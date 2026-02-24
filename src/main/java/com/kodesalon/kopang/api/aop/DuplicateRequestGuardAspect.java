package com.kodesalon.kopang.api.aop;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.Objects;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.expression.MethodBasedEvaluationContext;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.stereotype.Component;

import com.kodesalon.kopang.config.Caches;
import com.kodesalon.kopang.service.exception.DuplicateRequestException;

@Aspect
@Component
public class DuplicateRequestGuardAspect {

	private static final String KEY_PREFIX = "duplicate:";

	private final Cache duplicateRequestGuardCache;
	private final RedisTemplate<String, String> redisTemplate;
	private final ExpressionParser parser = new SpelExpressionParser();
	private final DefaultParameterNameDiscoverer nameDiscoverer = new DefaultParameterNameDiscoverer();

	public DuplicateRequestGuardAspect(
		@Qualifier(Caches.Manager.CAFFEINE) CacheManager caffeineCacheManager,
		RedisTemplate<String, String> redisTemplate
	) {
		this.duplicateRequestGuardCache = Objects.requireNonNull(
			caffeineCacheManager.getCache(Caches.Name.DUPLICATE_REQUEST_GUARD),
			"duplicate_request_guard cache가 등록되지 않았습니다."
		);
		this.redisTemplate = redisTemplate;
	}

	@Around("@annotation(preventDuplicateRequest)")
	public Object guard(ProceedingJoinPoint joinPoint, PreventDuplicateRequest preventDuplicateRequest) throws Throwable {
		String key = KEY_PREFIX + resolveKey(joinPoint, preventDuplicateRequest.keyExpression());

		Cache.ValueWrapper existing = duplicateRequestGuardCache.putIfAbsent(key, Boolean.TRUE);
		if (existing != null) {
			throw DuplicateRequestException.detected();
		}

		Boolean isNew = redisTemplate.opsForValue()
			.setIfAbsent(key, "1", Duration.ofSeconds(preventDuplicateRequest.ttlSeconds()));
		if (!Boolean.TRUE.equals(isNew)) {
			throw DuplicateRequestException.detected();
		}

		try {
			return joinPoint.proceed();
		} catch (Throwable e) {
			if (isSystemError(e)) {
				duplicateRequestGuardCache.evict(key);
				redisTemplate.delete(key);
			}
			throw e;
		}
	}

	/**
	 * 시스템 오류 여부를 판별합니다.
	 * 비즈니스 예외(도메인 예외, 표준 검증 예외)는 false를 반환하며, 캐시를 정리하지 않습니다.
	 * 인프라 오류(DB, 네트워크 등)는 true를 반환하며, 캐시를 정리하여 재시도를 허용합니다.
	 */
	private boolean isSystemError(Throwable e) {
		boolean isBusinessException =
			e.getClass().getPackageName().startsWith("com.kodesalon.kopang")
				|| e instanceof IllegalArgumentException
				|| e instanceof IllegalStateException;
		return !isBusinessException;
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
