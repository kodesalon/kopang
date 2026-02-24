package com.kodesalon.kopang.api.aop;

import java.lang.reflect.Method;
import java.util.List;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.expression.MethodBasedEvaluationContext;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
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

	private final CacheManager caffeineCacheManager;
	private final RedisTemplate<String, String> redisTemplate;
	private final DefaultRedisScript<Long> duplicateRequestGuardScript;
	private final ExpressionParser parser = new SpelExpressionParser();
	private final DefaultParameterNameDiscoverer nameDiscoverer = new DefaultParameterNameDiscoverer();

	public DuplicateRequestGuardAspect(
		@Qualifier(Caches.Manager.CAFFEINE) CacheManager caffeineCacheManager,
		RedisTemplate<String, String> redisTemplate
	) {
		this.caffeineCacheManager = caffeineCacheManager;
		this.redisTemplate = redisTemplate;
		this.duplicateRequestGuardScript = new DefaultRedisScript<>();
		this.duplicateRequestGuardScript.setLocation(new ClassPathResource("redis/duplicate_request_guard.lua"));
		this.duplicateRequestGuardScript.setResultType(Long.class);
	}

	@Around("@annotation(preventDuplicateRequest)")
	public Object guard(ProceedingJoinPoint joinPoint, PreventDuplicateRequest preventDuplicateRequest) throws Throwable {
		String key = KEY_PREFIX + resolveKey(joinPoint, preventDuplicateRequest.keyExpression());

		Cache cache = caffeineCacheManager.getCache(Caches.Name.DUPLICATE_REQUEST_GUARD);
		Cache.ValueWrapper existing = cache.putIfAbsent(key, Boolean.TRUE);
		if (existing != null) {
			throw DuplicateRequestException.detected();
		}

		Long result = redisTemplate.execute(
			duplicateRequestGuardScript,
			List.of(key),
			String.valueOf(preventDuplicateRequest.ttlSeconds())
		);
		if (result == null || result == 0L) {
			throw DuplicateRequestException.detected();
		}

		try {
			return joinPoint.proceed();
		} catch (Throwable e) {
			if (isSystemError(e)) {
				cache.evict(key);
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
