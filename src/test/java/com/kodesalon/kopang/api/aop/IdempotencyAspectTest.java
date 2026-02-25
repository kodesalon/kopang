package com.kodesalon.kopang.api.aop;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import com.kodesalon.kopang.api.support.AcceptanceTest;
import com.kodesalon.kopang.config.Caches;
import com.kodesalon.kopang.domain.order.Money;
import com.kodesalon.kopang.domain.payment.PaymentResult;
import com.kodesalon.kopang.external.MockPaymentClient;
import com.kodesalon.kopang.service.exception.PaymentFailedException;

import io.restassured.RestAssured;
import io.restassured.response.ValidatableResponse;

@AcceptanceTest({
	"acceptance/warehouse.json",
	"acceptance/member_address.json",
	"acceptance/product.json",
	"acceptance/stock.json",
	"acceptance/order.json"})
class IdempotencyAspectTest {

	private static final String IDEMPOTENCY_KEY = "idempotency-aspect-test-key-001";
	private static final String REDIS_KEY = "idempotent:" + IDEMPOTENCY_KEY;
	private static final Long MEMBER_NO = 1L;
	private static final Long ORDER_NO = 1L;
	private static final Long PRODUCT_NO = 1L;
	private static final BigDecimal AMOUNT = BigDecimal.valueOf(10000);
	private static final String PAYMENT_KEY = "payment-key-aspect-001";

	@Autowired
	private MockPaymentClient mockPaymentClient;

	@Autowired
	private RedisTemplate<String, String> redisTemplate;

	@Autowired
	@Qualifier(Caches.Manager.CAFFEINE)
	private CacheManager caffeineCacheManager;

	@BeforeEach
	void setUp() {
		mockPaymentClient.reset();
		redisTemplate.delete(REDIS_KEY);
		Cache cache = caffeineCacheManager.getCache(Caches.Name.IDEMPOTENCY);
		if (cache != null) {
			cache.evict(REDIS_KEY);
		}
	}

	@DisplayName("성공한 결제 요청에 동일한 멱등 키로 재요청하면 비즈니스 로직을 재실행하지 않고 캐시된 성공 응답을 반환한다")
	@Test
	void idempotentRetry_afterSuccess_returnsCachedCompletedResponse() {
		Map<String, Object> requestBody = createRequestBody();

		Map<String, Object> firstResponse = callPaymentApi(requestBody)
			.statusCode(HttpStatus.OK.value())
			.extract().jsonPath().getMap(".");

		// 결제 클라이언트를 실패 상태로 재설정 — 재요청이 비즈니스 로직을 실행한다면 422가 반환됨
		mockPaymentClient.setNextResult(new PaymentResult(
			PAYMENT_KEY, ORDER_NO, new Money(AMOUNT), LocalDateTime.now(),
			PaymentResult.Status.ABORTED, "비즈니스 로직이 재실행된 경우 이 결과가 반환됨"
		));

		Map<String, Object> secondResponse = callPaymentApi(requestBody)
			.statusCode(HttpStatus.OK.value())
			.extract().jsonPath().getMap(".");

		assertAll(
			() -> assertThat(secondResponse).containsEntry("orderNo", ORDER_NO.intValue()),
			() -> assertThat(secondResponse).containsEntry("paymentStatus", "DONE"),
			() -> assertThat(secondResponse.get("paymentNo")).isEqualTo(firstResponse.get("paymentNo"))
		);
	}

	@DisplayName("실패한 결제 요청에 동일한 멱등 키로 재요청하면 비즈니스 로직을 재실행하지 않고 캐시된 오류 응답을 반환한다")
	@Test
	void idempotentRetry_afterFailure_returnsCachedFailedResponse() {
		String failureMessage = "잔액 부족";
		mockPaymentClient.setNextResult(new PaymentResult(
			PAYMENT_KEY, ORDER_NO, new Money(AMOUNT), LocalDateTime.now(),
			PaymentResult.Status.ABORTED, failureMessage
		));
		Map<String, Object> requestBody = createRequestBody();

		Map<String, Object> firstError = callPaymentApi(requestBody)
			.statusCode(HttpStatus.UNPROCESSABLE_ENTITY.value())
			.extract().jsonPath().getMap(".");

		// 결제 클라이언트를 성공 상태로 복구 — 재요청이 비즈니스 로직을 실행한다면 200이 반환됨
		mockPaymentClient.reset();

		Map<String, Object> secondError = callPaymentApi(requestBody)
			.statusCode(HttpStatus.UNPROCESSABLE_ENTITY.value())
			.extract().jsonPath().getMap(".");

		String expectedMessage = PaymentFailedException.aborted(PAYMENT_KEY, ORDER_NO, failureMessage).getMessage();
		assertAll(
			() -> assertThat(secondError).containsEntry("code", HttpStatus.UNPROCESSABLE_ENTITY.value()),
			() -> assertThat(secondError).containsEntry("message", expectedMessage)
		);
	}

	@DisplayName("Caffeine 캐시가 비어있고 Redis에 완료 상태가 있으면 Redis에서 응답을 복원하고 Caffeine에 동기화한다")
	@Test
	void idempotentRetry_caffeineMiss_redisHit_restoresResponseAndSyncsCache() {
		Map<String, Object> requestBody = createRequestBody();

		Map<String, Object> firstResponse = callPaymentApi(requestBody)
			.statusCode(HttpStatus.OK.value())
			.extract().jsonPath().getMap(".");

		// Caffeine만 비워 L2(Redis) 조회 경로를 강제로 활성화
		Cache caffeineCache = caffeineCacheManager.getCache(Caches.Name.IDEMPOTENCY);
		if (caffeineCache != null) {
			caffeineCache.evict(REDIS_KEY);
		}

		// 두 번째 요청: Redis 값 기반으로 응답 복원
		Map<String, Object> secondResponse = callPaymentApi(requestBody)
			.statusCode(HttpStatus.OK.value())
			.extract().jsonPath().getMap(".");

		assertAll(
			() -> assertThat(secondResponse).containsEntry("paymentNo", firstResponse.get("paymentNo")),
			() -> assertThat(secondResponse).containsEntry("orderNo", ORDER_NO.intValue()),
			() -> assertThat(secondResponse).containsEntry("paymentStatus", "DONE")
		);

		// Caffeine에 동기화 확인
		Cache syncedCache = caffeineCacheManager.getCache(Caches.Name.IDEMPOTENCY);
		assertThat(syncedCache).isNotNull();
		Cache.ValueWrapper wrapper = syncedCache.get(REDIS_KEY);
		assertThat(wrapper).isNotNull();
		IdempotentResponse syncedState = (IdempotentResponse) wrapper.get();
		assertThat(syncedState).isNotNull();
		assertThat(syncedState.status()).isEqualTo(IdempotencyStatus.COMPLETED);
	}

	private ValidatableResponse callPaymentApi(Map<String, Object> requestBody) {
		return RestAssured
			.given().log().all()
			.contentType(MediaType.APPLICATION_JSON_VALUE)
			.header("Idempotency-Key", IDEMPOTENCY_KEY)
			.queryParam("memberNo", MEMBER_NO)
			.body(requestBody)
			.when()
			.post("/api/v1/orders/{orderNo}/payment", ORDER_NO)
			.then().log().all();
	}

	private Map<String, Object> createRequestBody() {
		Map<String, Object> requestBody = new HashMap<>();
		requestBody.put("paymentKey", PAYMENT_KEY);
		requestBody.put("amount", AMOUNT);
		requestBody.put("productNo", PRODUCT_NO);
		requestBody.put("orderCount", 1);
		return requestBody;
	}
}