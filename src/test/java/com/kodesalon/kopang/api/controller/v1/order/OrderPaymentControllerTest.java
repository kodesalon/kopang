package com.kodesalon.kopang.api.controller.v1.order;

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
import com.kodesalon.kopang.service.exception.NotFoundException;
import com.kodesalon.kopang.service.exception.PaymentFailedException;

import io.restassured.RestAssured;

@AcceptanceTest({
	"acceptance/warehouse.json",
	"acceptance/member_address.json",
	"acceptance/product.json",
	"acceptance/stock.json",
	"acceptance/order.json"})
class OrderPaymentControllerTest {

	private static final String IDEMPOTENCY_KEY = "test-idempotency-key-payment-001";
	private static final String REDIS_IDEMPOTENCY_KEY = "idempotent:" + IDEMPOTENCY_KEY;
	private static final Long MEMBER_NO = 1L;
	private static final Long ORDER_NO = 1L;
	private static final Long PRODUCT_NO = 1L;
	private static final BigDecimal AMOUNT = BigDecimal.valueOf(10000);
	private static final String PAYMENT_KEY = "test-payment-key-001";

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
		redisTemplate.delete(REDIS_IDEMPOTENCY_KEY);
		Cache cache = caffeineCacheManager.getCache(Caches.Name.IDEMPOTENCY);
		if (cache != null) {
			cache.evict(REDIS_IDEMPOTENCY_KEY);
		}
	}

	@DisplayName("유효한 PENDING 주문에 대해 결제 승인이 성공하면 결제번호, 주문번호, 결제상태, 결제금액, 결제시각을 반환한다")
	@Test
	void confirmPayment_success_done() {
		Map<String, Object> requestBody = new HashMap<>();
		requestBody.put("paymentKey", PAYMENT_KEY);
		requestBody.put("amount", AMOUNT);
		requestBody.put("productNo", PRODUCT_NO);
		requestBody.put("orderCount", 1);

		Map<String, Object> responseMap = RestAssured
			.given().log().all()
			.contentType(MediaType.APPLICATION_JSON_VALUE)
			.header("Idempotency-Key", IDEMPOTENCY_KEY)
			.queryParam("memberNo", MEMBER_NO)
			.body(requestBody)
			.when()
			.post("/api/v1/orders/{orderNo}/payment", ORDER_NO)
			.then().log().all()
			.statusCode(HttpStatus.OK.value())
			.extract()
			.jsonPath().getMap(".");

		assertAll(
			() -> assertThat(responseMap).containsKey("paymentNo"),
			() -> assertThat(responseMap).containsEntry("orderNo", ORDER_NO.intValue()),
			() -> assertThat(responseMap).containsEntry("paymentStatus", "DONE"),
			() -> {
				Object totalAmount = responseMap.get("totalAmount");
				assertThat(new BigDecimal(totalAmount.toString()))
					.isEqualByComparingTo(AMOUNT);
			},
			() -> assertThat(responseMap).containsKey("paidAt")
		);
	}

	@DisplayName("결제가 ABORTED(실패) 상태로 반환되면 주문은 PENDING 으로 롤백되고 422 예외가 발생한다")
	@Test
	void confirmPayment_fail_aborted() {
		String failureMessage = "잔액 부족";
		mockPaymentClient.setNextResult(new PaymentResult(
			PAYMENT_KEY,
			ORDER_NO,
			new Money(AMOUNT),
			LocalDateTime.now(),
			PaymentResult.Status.ABORTED,
			failureMessage
		));

		Map<String, Object> requestBody = new HashMap<>();
		requestBody.put("paymentKey", PAYMENT_KEY);
		requestBody.put("amount", AMOUNT);
		requestBody.put("productNo", PRODUCT_NO);
		requestBody.put("orderCount", 1);

		Map<String, Object> responseMap = RestAssured
			.given().log().all()
			.contentType(MediaType.APPLICATION_JSON_VALUE)
			.header("Idempotency-Key", IDEMPOTENCY_KEY)
			.queryParam("memberNo", MEMBER_NO)
			.body(requestBody)
			.when()
			.post("/api/v1/orders/{orderNo}/payment", ORDER_NO)
			.then().log().all()
			.statusCode(HttpStatus.UNPROCESSABLE_ENTITY.value())
			.extract()
			.jsonPath().getMap(".");

		String expectedMessage = PaymentFailedException.aborted(PAYMENT_KEY, ORDER_NO, failureMessage).getMessage();
		assertAll(
			() -> assertThat(responseMap).containsEntry("code", HttpStatus.UNPROCESSABLE_ENTITY.value()),
			() -> assertThat(responseMap).containsEntry("message", expectedMessage)
		);
	}

	@DisplayName("결제가 EXPIRED(만료) 상태로 반환되면 주문이 취소되고 재고가 복구된 후 422 예외가 발생한다")
	@Test
	void confirmPayment_fail_expired() {
		String failureMessage = "결제 유효시간 초과";
		mockPaymentClient.setNextResult(new PaymentResult(
			PAYMENT_KEY,
			ORDER_NO,
			new Money(AMOUNT),
			LocalDateTime.now(),
			PaymentResult.Status.EXPIRED,
			failureMessage
		));

		Map<String, Object> requestBody = new HashMap<>();
		requestBody.put("paymentKey", PAYMENT_KEY);
		requestBody.put("amount", AMOUNT);
		requestBody.put("productNo", PRODUCT_NO);
		requestBody.put("orderCount", 1);

		Map<String, Object> responseMap = RestAssured
			.given().log().all()
			.contentType(MediaType.APPLICATION_JSON_VALUE)
			.header("Idempotency-Key", IDEMPOTENCY_KEY)
			.queryParam("memberNo", MEMBER_NO)
			.body(requestBody)
			.when()
			.post("/api/v1/orders/{orderNo}/payment", ORDER_NO)
			.then().log().all()
			.statusCode(HttpStatus.UNPROCESSABLE_ENTITY.value())
			.extract()
			.jsonPath().getMap(".");

		String expectedMessage = PaymentFailedException.expired(PAYMENT_KEY, ORDER_NO, failureMessage).getMessage();
		assertAll(
			() -> assertThat(responseMap).containsEntry("code", HttpStatus.UNPROCESSABLE_ENTITY.value()),
			() -> assertThat(responseMap).containsEntry("message", expectedMessage)
		);
	}

	@DisplayName("존재하지 않는 주문번호로 결제를 요청하면 404 예외가 발생한다")
	@Test
	void confirmPayment_fail_orderNotFound() {
		Long nonExistentOrderNo = Long.MAX_VALUE;

		Map<String, Object> requestBody = new HashMap<>();
		requestBody.put("paymentKey", PAYMENT_KEY);
		requestBody.put("amount", AMOUNT);
		requestBody.put("productNo", PRODUCT_NO);
		requestBody.put("orderCount", 1);

		Map<String, Object> responseMap = RestAssured
			.given().log().all()
			.contentType(MediaType.APPLICATION_JSON_VALUE)
			.header("Idempotency-Key", IDEMPOTENCY_KEY)
			.queryParam("memberNo", MEMBER_NO)
			.body(requestBody)
			.when()
			.post("/api/v1/orders/{orderNo}/payment", nonExistentOrderNo)
			.then().log().all()
			.statusCode(HttpStatus.NOT_FOUND.value())
			.extract()
			.jsonPath().getMap(".");

		assertAll(
			() -> assertThat(responseMap).containsEntry("code", HttpStatus.NOT_FOUND.value()),
			() -> assertThat(responseMap).containsEntry("message", NotFoundException.order(nonExistentOrderNo).getMessage())
		);
	}

	@DisplayName("주문 금액과 결제 금액이 다르면 400 예외가 발생하고 주문은 PENDING 으로 롤백된다")
	@Test
	void confirmPayment_fail_amountMismatch() {
		BigDecimal wrongAmount = BigDecimal.valueOf(99999);

		Map<String, Object> requestBody = new HashMap<>();
		requestBody.put("paymentKey", PAYMENT_KEY);
		requestBody.put("amount", wrongAmount);
		requestBody.put("productNo", PRODUCT_NO);
		requestBody.put("orderCount", 1);

		Map<String, Object> responseMap = RestAssured
			.given().log().all()
			.contentType(MediaType.APPLICATION_JSON_VALUE)
			.header("Idempotency-Key", IDEMPOTENCY_KEY)
			.queryParam("memberNo", MEMBER_NO)
			.body(requestBody)
			.when()
			.post("/api/v1/orders/{orderNo}/payment", ORDER_NO)
			.then().log().all()
			.statusCode(HttpStatus.BAD_REQUEST.value())
			.extract()
			.jsonPath().getMap(".");

		assertAll(
			() -> assertThat(responseMap).containsEntry("code", HttpStatus.BAD_REQUEST.value()),
			() -> assertThat(responseMap).containsEntry("message", "주문 금액과 결제 금액이 일치하지 않습니다")
		);
	}

	@DisplayName("이미 처리 중인 멱등 키로 결제 요청이 들어오면 409 Conflict 가 발생한다")
	@Test
	void confirmPayment_fail_processingConflict() {
		redisTemplate.opsForHash().put(REDIS_IDEMPOTENCY_KEY, "status", "PROCESSING");

		Map<String, Object> requestBody = new HashMap<>();
		requestBody.put("paymentKey", PAYMENT_KEY);
		requestBody.put("amount", AMOUNT);
		requestBody.put("productNo", PRODUCT_NO);
		requestBody.put("orderCount", 1);

		Map<String, Object> responseMap = RestAssured
			.given().log().all()
			.contentType(MediaType.APPLICATION_JSON_VALUE)
			.header("Idempotency-Key", IDEMPOTENCY_KEY)
			.queryParam("memberNo", MEMBER_NO)
			.body(requestBody)
			.when()
			.post("/api/v1/orders/{orderNo}/payment", ORDER_NO)
			.then().log().all()
			.statusCode(HttpStatus.CONFLICT.value())
			.extract()
			.jsonPath().getMap(".");

		assertAll(
			() -> assertThat(responseMap).containsEntry("code", HttpStatus.CONFLICT.value()),
			() -> assertThat(responseMap).containsEntry("message", "처리 중인 요청이 있습니다")
		);
	}
}