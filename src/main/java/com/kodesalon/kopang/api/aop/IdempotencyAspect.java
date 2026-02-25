package com.kodesalon.kopang.api.aop;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.time.Duration;
import java.util.Objects;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.expression.MethodBasedEvaluationContext;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kodesalon.kopang.api.controller.KopangExceptionResponse;
import com.kodesalon.kopang.config.Caches;
import com.kodesalon.kopang.service.exception.IdempotencyUnavailableException;
import com.kodesalon.kopang.service.exception.NotFoundException;
import com.kodesalon.kopang.service.exception.PaymentFailedException;
import com.kodesalon.kopang.service.exception.SoldOutException;

@Aspect
@Component
public class IdempotencyAspect {

	private static final Logger log = LoggerFactory.getLogger(IdempotencyAspect.class);
	private static final String KEY_PREFIX = "idempotent:";

	private final Cache idempotencyCache;
	private final RedisTemplate<String, String> redisTemplate;
	private final ObjectMapper objectMapper;
	private final ExpressionParser parser = new SpelExpressionParser();
	private final DefaultParameterNameDiscoverer nameDiscoverer = new DefaultParameterNameDiscoverer();

	public IdempotencyAspect(
		@Qualifier(Caches.Manager.CAFFEINE) CacheManager caffeineCacheManager,
		RedisTemplate<String, String> redisTemplate,
		ObjectMapper objectMapper
	) {
		this.idempotencyCache = Objects.requireNonNull(
			caffeineCacheManager.getCache(Caches.Name.IDEMPOTENCY),
			"idempotency cache가 등록되지 않았습니다."
		);
		this.redisTemplate = redisTemplate;
		this.objectMapper = objectMapper;
	}

	@Around("@annotation(idempotent)")
	public Object intercept(ProceedingJoinPoint joinPoint, Idempotent idempotent) throws Throwable {
		String key = KEY_PREFIX + resolveKey(joinPoint, idempotent.keyExpression());

		// --- L1: Caffeine 빠른 경로 ---
		Cache.ValueWrapper caffeineExisting = idempotencyCache.putIfAbsent(key, IdempotentResponse.processing());
		if (caffeineExisting != null) {
			IdempotentResponse cachedState = Objects.requireNonNullElseGet(
				(IdempotentResponse) caffeineExisting.get(), IdempotentResponse::processing
			);
			return buildResponseFromCachedState(joinPoint, cachedState);
		}

		// --- L2: Redis 원자적 획득 (SET NX EX) ---
		try {
			String processingJson = objectMapper.writeValueAsString(IdempotentResponse.processing());
			Boolean acquired = redisTemplate.opsForValue()
				.setIfAbsent(key, processingJson, Duration.ofSeconds(idempotent.processingTimeoutSeconds()));

			if (Boolean.FALSE.equals(acquired)) {
				// 키가 이미 존재 — Redis 상태로 Caffeine을 갱신하고 반환
				String existingJson = redisTemplate.opsForValue().get(key);
				if (existingJson != null) {
					IdempotentResponse redisState = objectMapper.readValue(existingJson, IdempotentResponse.class);
					idempotencyCache.put(key, redisState);
					return buildResponseFromCachedState(joinPoint, redisState);
				}
				// setIfAbsent와 get 사이에 키가 만료된 경우 — 신규 요청으로 처리
			}
		} catch (Exception redisException) {
			// Redis 장애 시 멱등성 보장 불가 — Fail-fast로 요청 거부 (중복 결제/재고 오차감 방지)
			log.error("[Idempotency] Redis 장애로 멱등성 보장 불가, 요청 거부. key={}, error={}",
				key, redisException.getMessage());
			idempotencyCache.evict(key);
			throw new IdempotencyUnavailableException();
		}

		// --- 비즈니스 로직 실행 ---
		try {
			Object result = joinPoint.proceed();

			ResponseEntity<?> responseEntity = (ResponseEntity<?>) result;
			String serializedBody = objectMapper.writeValueAsString(responseEntity.getBody());
			int httpStatus = responseEntity.getStatusCode().value();

			IdempotentResponse completedState = IdempotentResponse.completed(httpStatus, serializedBody);
			saveToRedis(key, completedState, idempotent.ttlHours());
			idempotencyCache.put(key, completedState);

			return result;
		} catch (Throwable e) {
			if (isSystemError(e)) {
				// 시스템 오류 — 키 삭제하여 재시도 허용
				redisTemplate.delete(key);
				idempotencyCache.evict(key);
			} else {
				// 비즈니스 예외 — FAILED 상태로 캐싱 (재시도 시 동일 에러 즉시 반환)
				int httpStatus = resolveHttpStatus(e);
				KopangExceptionResponse errorBody = new KopangExceptionResponse(e.getMessage(), httpStatus);
				String serializedBody = objectMapper.writeValueAsString(errorBody);

				IdempotentResponse failedState = IdempotentResponse.failed(httpStatus, serializedBody);
				saveToRedis(key, failedState, idempotent.ttlHours());
				idempotencyCache.put(key, failedState);
			}
			throw e;
		}
	}

	private Object buildResponseFromCachedState(ProceedingJoinPoint joinPoint, IdempotentResponse state) throws Exception {
		if (state.status() == IdempotencyStatus.PROCESSING) {
			return ResponseEntity.status(HttpStatus.CONFLICT)
				.body(new KopangExceptionResponse("처리 중인 요청이 있습니다", HttpStatus.CONFLICT.value()));
		}
		if (state.status() == IdempotencyStatus.COMPLETED) {
			MethodSignature signature = (MethodSignature) joinPoint.getSignature();
			Type genericReturnType = signature.getMethod().getGenericReturnType();
			ParameterizedType paramType = (ParameterizedType) genericReturnType;
			Type bodyType = paramType.getActualTypeArguments()[0];
			JavaType javaType = objectMapper.constructType(bodyType);
			Object body = objectMapper.readValue(state.body(), javaType);
			return ResponseEntity.status(state.httpStatus()).body(body);
		}
		// FAILED
		KopangExceptionResponse errorBody = objectMapper.readValue(state.body(), KopangExceptionResponse.class);
		return ResponseEntity.status(state.httpStatus()).body(errorBody);
	}

	private void saveToRedis(String key, IdempotentResponse state, int ttlHours) {
		try {
			String json = objectMapper.writeValueAsString(state);
			redisTemplate.opsForValue().set(key, json, Duration.ofHours(ttlHours));
		} catch (Exception e) {
			log.warn("[Idempotency] Redis 응답 캐싱 실패. key={}, status={}, error={}",
				key, state.status(), e.getMessage());
		}
	}

	/**
	 * 시스템 오류 여부를 판별합니다.
	 * 서비스/도메인 예외와 표준 Java 검증 예외는 비즈니스 예외로 분류합니다.
	 * 인프라 오류(DB 연결, 네트워크 등)는 시스템 오류로 분류하여 재시도를 허용합니다.
	 */
	private boolean isSystemError(Throwable e) {
		boolean isBusinessException =
			e.getClass().getPackageName().startsWith("com.kodesalon.kopang")
				|| e instanceof IllegalArgumentException
				|| e instanceof IllegalStateException;
		return !isBusinessException;
	}

	/**
	 * 예외 타입에 따라 HTTP 상태코드를 결정합니다.
	 * GlobalExceptionController의 예외 핸들러 매핑과 반드시 일치해야 합니다.
	 */
	private int resolveHttpStatus(Throwable e) {
		if (e instanceof NotFoundException) {
			return HttpStatus.NOT_FOUND.value();
		}
		if (e instanceof SoldOutException) {
			return HttpStatus.CONFLICT.value();
		}
		if (e instanceof PaymentFailedException) {
			return HttpStatus.UNPROCESSABLE_ENTITY.value();
		}
		return HttpStatus.BAD_REQUEST.value();
	}

	private String resolveKey(ProceedingJoinPoint joinPoint, String keyExpression) {
		MethodSignature signature = (MethodSignature) joinPoint.getSignature();
		Method method = signature.getMethod();
		Object[] args = joinPoint.getArgs();

		MethodBasedEvaluationContext context = new MethodBasedEvaluationContext(
			joinPoint.getTarget(), method, args, nameDiscoverer
		);

		Expression expression = parser.parseExpression(keyExpression);
		return expression.getValue(context, String.class);
	}
}
