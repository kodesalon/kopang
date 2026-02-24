package com.kodesalon.kopang.api.aop;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.never;
import static org.mockito.BDDMockito.verify;

import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.kodesalon.kopang.config.Caches;
import com.kodesalon.kopang.service.exception.DuplicateRequestException;
import com.kodesalon.kopang.service.exception.SoldOutException;

@ExtendWith(MockitoExtension.class)
class DuplicateRequestGuardAspectTest {

	@Mock
	private CacheManager caffeineCacheManager;

	@Mock
	private RedisTemplate<String, String> redisTemplate;

	@Mock
	private ProceedingJoinPoint joinPoint;

	@Mock
	private MethodSignature methodSignature;

	@Mock
	private Cache caffeineCache;

	@Mock
	private Cache.ValueWrapper valueWrapper;

	private DuplicateRequestGuardAspect aspect;

	/**
	 * 테스트 대상 메서드 - SpEL 키 표현식 파싱에 사용될 파라미터를 가진 샘플 메서드
	 */
	@SuppressWarnings("unused")
	public static void sampleMethod(Long memberNo, Long productNo) {
	}

	@BeforeEach
	void setUp() {
		aspect = new DuplicateRequestGuardAspect(caffeineCacheManager, redisTemplate);
	}

	private PreventDuplicateRequest buildAnnotation(String keyExpression, int ttlSeconds) {
		return new PreventDuplicateRequest() {
			@Override
			public Class<PreventDuplicateRequest> annotationType() {
				return PreventDuplicateRequest.class;
			}

			@Override
			public String keyExpression() {
				return keyExpression;
			}

			@Override
			public int ttlSeconds() {
				return ttlSeconds;
			}
		};
	}

	private void stubJoinPointWithArgs(Object target, String methodName, Object[] args) throws NoSuchMethodException {
		Method method = DuplicateRequestGuardAspectTest.class.getMethod(methodName, Long.class, Long.class);
		given(joinPoint.getSignature()).willReturn(methodSignature);
		given(methodSignature.getMethod()).willReturn(method);
		given(joinPoint.getArgs()).willReturn(args);
		given(joinPoint.getTarget()).willReturn(target);
	}

	@Nested
	@DisplayName("Caffeine 캐시 중복 감지 테스트")
	class CaffeineDuplicateDetectionTest {

		@DisplayName("동일 키로 재요청이 들어오면 Caffeine 캐시 단계에서 DuplicateRequestException 이 발생한다")
		@Test
		void guard_throwsDuplicateRequestException_whenCaffeineDetectsDuplicate() throws NoSuchMethodException {
			// given
			PreventDuplicateRequest annotation = buildAnnotation("'order:' + #memberNo + ':' + #productNo", 3);
			stubJoinPointWithArgs(new Object(), "sampleMethod", new Object[]{1L, 100L});

			given(caffeineCacheManager.getCache(Caches.Name.DUPLICATE_REQUEST_GUARD)).willReturn(caffeineCache);
			// Caffeine 캐시에 이미 값이 존재하는 상황 (ValueWrapper 반환)
			given(caffeineCache.putIfAbsent(anyString(), any())).willReturn(valueWrapper);

			// when & then
			assertThatThrownBy(() -> aspect.guard(joinPoint, annotation))
				.isInstanceOf(DuplicateRequestException.class)
				.hasMessage("이미 처리 중인 요청입니다");
		}

		@DisplayName("Caffeine 중복 감지 시 Redis 스크립트는 실행되지 않는다")
		@Test
		void guard_doesNotExecuteRedisScript_whenCaffeineDetectsDuplicate() throws NoSuchMethodException {
			// given
			PreventDuplicateRequest annotation = buildAnnotation("'order:' + #memberNo + ':' + #productNo", 3);
			stubJoinPointWithArgs(new Object(), "sampleMethod", new Object[]{1L, 100L});

			given(caffeineCacheManager.getCache(Caches.Name.DUPLICATE_REQUEST_GUARD)).willReturn(caffeineCache);
			given(caffeineCache.putIfAbsent(anyString(), any())).willReturn(valueWrapper);

			// when
			assertThatThrownBy(() -> aspect.guard(joinPoint, annotation))
				.isInstanceOf(DuplicateRequestException.class);

			// then: Redis 스크립트 미실행 검증
			verify(redisTemplate, never()).execute(any(DefaultRedisScript.class), anyList(), anyString());
		}
	}

	@Nested
	@DisplayName("Redis 중복 감지 테스트")
	class RedisDuplicateDetectionTest {

		@DisplayName("Caffeine 통과 후 Redis 가 0 을 반환하면 DuplicateRequestException 이 발생한다")
		@Test
		void guard_throwsDuplicateRequestException_whenRedisReturnsZero() throws NoSuchMethodException {
			// given
			PreventDuplicateRequest annotation = buildAnnotation("'order:' + #memberNo + ':' + #productNo", 3);
			stubJoinPointWithArgs(new Object(), "sampleMethod", new Object[]{1L, 100L});

			given(caffeineCacheManager.getCache(Caches.Name.DUPLICATE_REQUEST_GUARD)).willReturn(caffeineCache);
			// Caffeine miss (null 반환 = 최초 삽입 성공)
			given(caffeineCache.putIfAbsent(anyString(), any())).willReturn(null);
			// Redis: 이미 키 존재(중복) → 0 반환
			given(redisTemplate.execute(any(DefaultRedisScript.class), anyList(), anyString())).willReturn(0L);

			// when & then
			assertThatThrownBy(() -> aspect.guard(joinPoint, annotation))
				.isInstanceOf(DuplicateRequestException.class)
				.hasMessage("이미 처리 중인 요청입니다");
		}

		@DisplayName("Caffeine 통과 후 Redis 가 null 을 반환하면 DuplicateRequestException 이 발생한다")
		@Test
		void guard_throwsDuplicateRequestException_whenRedisReturnsNull() throws NoSuchMethodException {
			// given
			PreventDuplicateRequest annotation = buildAnnotation("'order:' + #memberNo + ':' + #productNo", 3);
			stubJoinPointWithArgs(new Object(), "sampleMethod", new Object[]{1L, 100L});

			given(caffeineCacheManager.getCache(Caches.Name.DUPLICATE_REQUEST_GUARD)).willReturn(caffeineCache);
			given(caffeineCache.putIfAbsent(anyString(), any())).willReturn(null);
			// Redis: null 반환 (네트워크 오류나 스크립트 실패 상황)
			given(redisTemplate.execute(any(DefaultRedisScript.class), anyList(), anyString())).willReturn(null);

			// when & then
			assertThatThrownBy(() -> aspect.guard(joinPoint, annotation))
				.isInstanceOf(DuplicateRequestException.class)
				.hasMessage("이미 처리 중인 요청입니다");
		}

		@DisplayName("Redis 중복 감지 시 proceed() 는 호출되지 않는다")
		@Test
		void guard_doesNotProceed_whenRedisDetectsDuplicate() throws Throwable {
			// given
			PreventDuplicateRequest annotation = buildAnnotation("'order:' + #memberNo + ':' + #productNo", 3);
			stubJoinPointWithArgs(new Object(), "sampleMethod", new Object[]{1L, 100L});

			given(caffeineCacheManager.getCache(Caches.Name.DUPLICATE_REQUEST_GUARD)).willReturn(caffeineCache);
			given(caffeineCache.putIfAbsent(anyString(), any())).willReturn(null);
			given(redisTemplate.execute(any(DefaultRedisScript.class), anyList(), anyString())).willReturn(0L);

			// when
			assertThatThrownBy(() -> aspect.guard(joinPoint, annotation))
				.isInstanceOf(DuplicateRequestException.class);

			// then
			verify(joinPoint, never()).proceed();
		}
	}

	@Nested
	@DisplayName("최초 요청 성공 테스트")
	class FirstRequestSuccessTest {

		@DisplayName("Caffeine miss 후 Redis 가 1 을 반환하면 proceed() 가 호출되고 결과가 반환된다")
		@Test
		void guard_proceeds_whenBothCachesMiss() throws Throwable {
			// given
			PreventDuplicateRequest annotation = buildAnnotation("'order:' + #memberNo + ':' + #productNo", 3);
			stubJoinPointWithArgs(new Object(), "sampleMethod", new Object[]{1L, 100L});

			Object expectedResult = new Object();
			given(caffeineCacheManager.getCache(Caches.Name.DUPLICATE_REQUEST_GUARD)).willReturn(caffeineCache);
			given(caffeineCache.putIfAbsent(anyString(), any())).willReturn(null);
			given(redisTemplate.execute(any(DefaultRedisScript.class), anyList(), anyString())).willReturn(1L);
			given(joinPoint.proceed()).willReturn(expectedResult);

			// when
			Object result = aspect.guard(joinPoint, annotation);

			// then
			assertAll(
				() -> assertThat(result).isEqualTo(expectedResult),
				() -> verify(joinPoint).proceed()
			);
		}

		@DisplayName("최초 요청 성공 시 TTL 값이 Redis 스크립트에 정확히 전달된다")
		@Test
		void guard_passesTtlToRedisScript() throws Throwable {
			// given
			int expectedTtl = 5;
			PreventDuplicateRequest annotation = buildAnnotation("'key'", expectedTtl);
			Method method = DuplicateRequestGuardAspectTest.class.getMethod("sampleMethod", Long.class, Long.class);
			given(joinPoint.getSignature()).willReturn(methodSignature);
			given(methodSignature.getMethod()).willReturn(method);
			given(joinPoint.getArgs()).willReturn(new Object[]{1L, 100L});
			given(joinPoint.getTarget()).willReturn(new Object());

			given(caffeineCacheManager.getCache(Caches.Name.DUPLICATE_REQUEST_GUARD)).willReturn(caffeineCache);
			given(caffeineCache.putIfAbsent(anyString(), any())).willReturn(null);
			given(redisTemplate.execute(
				any(DefaultRedisScript.class),
				anyList(),
				anyString()
			)).willReturn(1L);
			given(joinPoint.proceed()).willReturn(null);

			// when
			aspect.guard(joinPoint, annotation);

			// then: TTL 값이 "5" 문자열로 전달되는지 검증
			verify(redisTemplate).execute(
				any(DefaultRedisScript.class),
				anyList(),
				org.mockito.ArgumentMatchers.eq(String.valueOf(expectedTtl))
			);
		}
	}

	@Nested
	@DisplayName("proceed() 예외 발생 시 캐시 처리 테스트")
	class ProceedExceptionHandlingTest {

		@DisplayName("proceed() 에서 인프라 RuntimeException 발생 시 Caffeine evict 와 Redis delete 가 호출된다")
		@Test
		void guard_evictsCacheAndDeletesRedisKey_whenSystemErrorOccurs() throws Throwable {
			// given
			PreventDuplicateRequest annotation = buildAnnotation("'order:' + #memberNo + ':' + #productNo", 3);
			stubJoinPointWithArgs(new Object(), "sampleMethod", new Object[]{1L, 100L});
			String expectedFullKey = "duplicate:order:1:100";

			given(caffeineCacheManager.getCache(Caches.Name.DUPLICATE_REQUEST_GUARD)).willReturn(caffeineCache);
			given(caffeineCache.putIfAbsent(anyString(), any())).willReturn(null);
			given(redisTemplate.execute(any(DefaultRedisScript.class), anyList(), anyString())).willReturn(1L);
			// 인프라 오류: 패키지가 kopang 이 아닌 일반 RuntimeException
			given(joinPoint.proceed()).willThrow(new RuntimeException("DB connection failed"));

			// when
			assertThatThrownBy(() -> aspect.guard(joinPoint, annotation))
				.isInstanceOf(RuntimeException.class)
				.hasMessage("DB connection failed");

			// then: 시스템 오류이므로 캐시 정리 호출 검증
			assertAll(
				() -> verify(caffeineCache).evict(expectedFullKey),
				() -> verify(redisTemplate).delete(expectedFullKey)
			);
		}

		@DisplayName("proceed() 에서 kopang 패키지 비즈니스 예외 발생 시 캐시는 정리되지 않는다")
		@Test
		void guard_doesNotEvictCache_whenBusinessExceptionFromKopangPackageOccurs() throws Throwable {
			// given
			PreventDuplicateRequest annotation = buildAnnotation("'order:' + #memberNo + ':' + #productNo", 3);
			stubJoinPointWithArgs(new Object(), "sampleMethod", new Object[]{1L, 100L});

			given(caffeineCacheManager.getCache(Caches.Name.DUPLICATE_REQUEST_GUARD)).willReturn(caffeineCache);
			given(caffeineCache.putIfAbsent(anyString(), any())).willReturn(null);
			given(redisTemplate.execute(any(DefaultRedisScript.class), anyList(), anyString())).willReturn(1L);
			// 비즈니스 예외: com.kodesalon.kopang 패키지 소속
			given(joinPoint.proceed()).willThrow(SoldOutException.warehouse(100L));

			// when
			assertThatThrownBy(() -> aspect.guard(joinPoint, annotation))
				.isInstanceOf(SoldOutException.class);

			// then: 비즈니스 예외이므로 캐시 정리 미호출 검증
			assertAll(
				() -> verify(caffeineCache, never()).evict(anyString()),
				() -> verify(redisTemplate, never()).delete(anyString())
			);
		}

		@DisplayName("proceed() 에서 IllegalArgumentException 발생 시 캐시는 정리되지 않는다")
		@Test
		void guard_doesNotEvictCache_whenIllegalArgumentExceptionOccurs() throws Throwable {
			// given
			PreventDuplicateRequest annotation = buildAnnotation("'order:' + #memberNo + ':' + #productNo", 3);
			stubJoinPointWithArgs(new Object(), "sampleMethod", new Object[]{1L, 100L});

			given(caffeineCacheManager.getCache(Caches.Name.DUPLICATE_REQUEST_GUARD)).willReturn(caffeineCache);
			given(caffeineCache.putIfAbsent(anyString(), any())).willReturn(null);
			given(redisTemplate.execute(any(DefaultRedisScript.class), anyList(), anyString())).willReturn(1L);
			// 표준 검증 예외: IllegalArgumentException 은 비즈니스 예외로 취급
			given(joinPoint.proceed()).willThrow(new IllegalArgumentException("잘못된 파라미터"));

			// when
			assertThatThrownBy(() -> aspect.guard(joinPoint, annotation))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("잘못된 파라미터");

			// then: 비즈니스 예외이므로 캐시 정리 미호출 검증
			assertAll(
				() -> verify(caffeineCache, never()).evict(anyString()),
				() -> verify(redisTemplate, never()).delete(anyString())
			);
		}

		@DisplayName("proceed() 에서 IllegalStateException 발생 시 캐시는 정리되지 않는다")
		@Test
		void guard_doesNotEvictCache_whenIllegalStateExceptionOccurs() throws Throwable {
			// given
			PreventDuplicateRequest annotation = buildAnnotation("'order:' + #memberNo + ':' + #productNo", 3);
			stubJoinPointWithArgs(new Object(), "sampleMethod", new Object[]{1L, 100L});

			given(caffeineCacheManager.getCache(Caches.Name.DUPLICATE_REQUEST_GUARD)).willReturn(caffeineCache);
			given(caffeineCache.putIfAbsent(anyString(), any())).willReturn(null);
			given(redisTemplate.execute(any(DefaultRedisScript.class), anyList(), anyString())).willReturn(1L);
			// 표준 상태 예외: IllegalStateException 은 비즈니스 예외로 취급
			given(joinPoint.proceed()).willThrow(new IllegalStateException("잘못된 상태"));

			// when
			assertThatThrownBy(() -> aspect.guard(joinPoint, annotation))
				.isInstanceOf(IllegalStateException.class)
				.hasMessage("잘못된 상태");

			// then: 비즈니스 예외이므로 캐시 정리 미호출 검증
			assertAll(
				() -> verify(caffeineCache, never()).evict(anyString()),
				() -> verify(redisTemplate, never()).delete(anyString())
			);
		}
	}

	@Nested
	@DisplayName("SpEL 키 표현식 파싱 테스트")
	class SpelKeyExpressionTest {

		@DisplayName("'order:' + #memberNo + ':' + #productNo 표현식이 올바른 키로 파싱된다")
		@Test
		void guard_resolvesSpelKeyExpression() throws Throwable {
			// given
			Long memberNo = 42L;
			Long productNo = 99L;
			String expectedFullKey = "duplicate:order:" + memberNo + ":" + productNo;

			PreventDuplicateRequest annotation = buildAnnotation("'order:' + #memberNo + ':' + #productNo", 3);
			stubJoinPointWithArgs(new Object(), "sampleMethod", new Object[]{memberNo, productNo});

			given(caffeineCacheManager.getCache(Caches.Name.DUPLICATE_REQUEST_GUARD)).willReturn(caffeineCache);
			given(caffeineCache.putIfAbsent(anyString(), any())).willReturn(null);
			given(redisTemplate.execute(any(DefaultRedisScript.class), anyList(), anyString())).willReturn(1L);
			given(joinPoint.proceed()).willReturn(null);

			// when
			aspect.guard(joinPoint, annotation);

			// then: 올바른 전체 키(prefix 포함)로 캐시 조회 검증
			verify(caffeineCache).putIfAbsent(
				org.mockito.ArgumentMatchers.eq(expectedFullKey),
				any()
			);
		}

		@DisplayName("리터럴 문자열 SpEL 표현식도 올바르게 파싱된다")
		@Test
		void guard_resolvesLiteralSpelKey() throws Throwable {
			// given
			String expectedFullKey = "duplicate:static-key";
			PreventDuplicateRequest annotation = buildAnnotation("'static-key'", 3);
			stubJoinPointWithArgs(new Object(), "sampleMethod", new Object[]{1L, 1L});

			given(caffeineCacheManager.getCache(Caches.Name.DUPLICATE_REQUEST_GUARD)).willReturn(caffeineCache);
			given(caffeineCache.putIfAbsent(anyString(), any())).willReturn(null);
			given(redisTemplate.execute(any(DefaultRedisScript.class), anyList(), anyString())).willReturn(1L);
			given(joinPoint.proceed()).willReturn(null);

			// when
			aspect.guard(joinPoint, annotation);

			// then
			verify(caffeineCache).putIfAbsent(
				org.mockito.ArgumentMatchers.eq(expectedFullKey),
				any()
			);
		}
	}

	@Nested
	@DisplayName("TTL 만료 후 재요청 허용 테스트")
	class TtlExpiryTest {

		@DisplayName("Caffeine TTL 만료 후 동일 키로 재요청하면 putIfAbsent 가 null 을 반환하여 통과된다")
		@Test
		void guard_allowsRequest_afterCaffeineTtlExpiry() throws Throwable {
			// given: 실제 Caffeine 캐시를 1초 TTL 로 생성
			CaffeineCacheManager realCacheManager = new CaffeineCacheManager();
			realCacheManager.registerCustomCache(
				Caches.Name.DUPLICATE_REQUEST_GUARD,
				Caffeine.newBuilder()
					.expireAfterWrite(1, TimeUnit.SECONDS)
					.build()
			);

			DuplicateRequestGuardAspect aspectWithRealCache =
				new DuplicateRequestGuardAspect(realCacheManager, redisTemplate);

			PreventDuplicateRequest annotation = buildAnnotation("'order:' + #memberNo + ':' + #productNo", 3);
			stubJoinPointWithArgs(new Object(), "sampleMethod", new Object[]{1L, 100L});
			given(redisTemplate.execute(any(DefaultRedisScript.class), anyList(), anyString())).willReturn(1L);
			given(joinPoint.proceed()).willReturn("success");

			// when: 첫 번째 요청 성공
			Object firstResult = aspectWithRealCache.guard(joinPoint, annotation);

			// TTL 1초 만료 대기
			Thread.sleep(1500);

			// 두 번째 요청: TTL 만료로 Caffeine miss → Redis 도 1 반환 → 통과
			given(redisTemplate.execute(any(DefaultRedisScript.class), anyList(), anyString())).willReturn(1L);
			given(joinPoint.proceed()).willReturn("success-after-expiry");
			Object secondResult = aspectWithRealCache.guard(joinPoint, annotation);

			// then: 두 요청 모두 성공적으로 proceed 호출
			assertAll(
				() -> assertThat(firstResult).isEqualTo("success"),
				() -> assertThat(secondResult).isEqualTo("success-after-expiry")
			);
		}

		@DisplayName("TTL 이 만료되기 전 동일 키로 재요청하면 Caffeine 이 중복을 감지한다")
		@Test
		void guard_detectsDuplicate_beforeCaffeineTtlExpiry() throws Throwable {
			// given: 실제 Caffeine 캐시를 5초 TTL 로 생성
			CaffeineCacheManager realCacheManager = new CaffeineCacheManager();
			realCacheManager.registerCustomCache(
				Caches.Name.DUPLICATE_REQUEST_GUARD,
				Caffeine.newBuilder()
					.expireAfterWrite(5, TimeUnit.SECONDS)
					.build()
			);

			DuplicateRequestGuardAspect aspectWithRealCache =
				new DuplicateRequestGuardAspect(realCacheManager, redisTemplate);

			PreventDuplicateRequest annotation = buildAnnotation("'order:' + #memberNo + ':' + #productNo", 3);
			stubJoinPointWithArgs(new Object(), "sampleMethod", new Object[]{1L, 100L});
			given(redisTemplate.execute(any(DefaultRedisScript.class), anyList(), anyString())).willReturn(1L);
			given(joinPoint.proceed()).willReturn("success");

			// when: 첫 번째 요청 성공
			aspectWithRealCache.guard(joinPoint, annotation);

			// then: TTL 만료 전 동일 키 재요청 → Caffeine 중복 감지
			assertThatThrownBy(() -> aspectWithRealCache.guard(joinPoint, annotation))
				.isInstanceOf(DuplicateRequestException.class)
				.hasMessage("이미 처리 중인 요청입니다");
		}
	}
}
