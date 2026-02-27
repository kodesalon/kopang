package com.kodesalon.kopang.api.controller.v1.order;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import java.math.BigDecimal;
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

import com.kodesalon.kopang.api.aop.IdempotentResponse;
import com.kodesalon.kopang.api.support.AcceptanceTest;
import com.kodesalon.kopang.config.Caches;
import com.kodesalon.kopang.domain.order.OrderStatus;
import com.kodesalon.kopang.service.exception.SoldOutException;

import io.restassured.RestAssured;

@AcceptanceTest({
	"acceptance/warehouse.json",
	"acceptance/member_address.json",
	"acceptance/product.json",
	"acceptance/stock.json"})
class OrderControllerTest {

	private static final String IDEMPOTENCY_KEY = "test-idempotency-key-order-001";
	private static final String REDIS_IDEMPOTENCY_KEY = "idempotent:" + IDEMPOTENCY_KEY;
	private static final String STOCK_KEY_FORMAT = "stock:product:%d:warehouse:%d";
	private static final String DUPLICATE_KEY_FORMAT = "duplicate:order:%d:%d";

	@Autowired
	private RedisTemplate<String, String> redisTemplate;

	@Autowired
	@Qualifier(Caches.Manager.CAFFEINE)
	private CacheManager caffeineCacheManager;

	@BeforeEach
	void setUpRedis() {
		redisTemplate.opsForValue().set(String.format(STOCK_KEY_FORMAT, 1, 1), "100");
		redisTemplate.opsForValue().set(String.format(STOCK_KEY_FORMAT, 2, 1), "0");
		redisTemplate.opsForValue().set(String.format(STOCK_KEY_FORMAT, 3, 1), "1");
		redisTemplate.delete(String.format(DUPLICATE_KEY_FORMAT, 1, 1));
		redisTemplate.delete(String.format(DUPLICATE_KEY_FORMAT, 1, 2));
		redisTemplate.delete(String.format(DUPLICATE_KEY_FORMAT, 1, 3));
		redisTemplate.delete(String.format(DUPLICATE_KEY_FORMAT, 1, Long.MAX_VALUE));
		redisTemplate.delete(REDIS_IDEMPOTENCY_KEY);
		Cache cache = caffeineCacheManager.getCache(Caches.Name.IDEMPOTENCY);
		if (cache != null) {
			cache.evict(REDIS_IDEMPOTENCY_KEY);
		}
	}

	@DisplayName("선착순 재고 차감 주문 예약을 성공하면 주문번호, 총액, 재고, 주문 상태를 반환한다")
	@Test
	void createReservationOrder_success() {
		Map<String, Object> requestBody = new HashMap<>();
		requestBody.put("productNo", 1L);
		requestBody.put("count", 1);

		Map<String, Object> responseMap = RestAssured
			.given().log().all()
			.contentType(MediaType.APPLICATION_JSON_VALUE)
			.header("Idempotency-Key", IDEMPOTENCY_KEY)
			.queryParam("memberNo", 1L)
			.body(requestBody)
			.when()
			.post("/api/v1/orders")
			.then().log().all()
			.statusCode(HttpStatus.CREATED.value())
			.extract()
			.jsonPath().getMap(".");

		assertAll(
			() -> assertThat(responseMap).containsKey("orderNo"),
			() -> assertThat(responseMap).containsEntry("quantity", 99),
			() -> assertThat(responseMap).containsEntry("orderStatus", OrderStatus.PENDING.toString()),
			() -> {
				Object totalPrice = responseMap.get("totalPrice");
				BigDecimal actualPrice = new BigDecimal(totalPrice.toString());
				assertThat(actualPrice).isEqualByComparingTo(BigDecimal.valueOf(10000));
			}
		);
	}

	@DisplayName("존재하지 않는 상품을 주문하면 모든 창고에 재고가 없어 409 예외가 발생한다")
	@Test
	void createReservationOrder_fail_notFound() {
		Map<String, Object> requestBody = new HashMap<>();
		requestBody.put("productNo", Long.MAX_VALUE);
		requestBody.put("count", 1);

		Map<String, Object> responseMap = RestAssured
			.given().log().all()
			.contentType(MediaType.APPLICATION_JSON_VALUE)
			.header("Idempotency-Key", IDEMPOTENCY_KEY)
			.queryParam("memberNo", 1L)
			.body(requestBody)
			.when()
			.post("/api/v1/orders")
			.then().log().all()
			.statusCode(HttpStatus.CONFLICT.value())
			.extract()
			.jsonPath().getMap(".");

		assertAll(
			() -> assertThat(responseMap).containsEntry("code", HttpStatus.CONFLICT.value()),
			() -> assertThat(responseMap).containsEntry("message", SoldOutException.warehouse(Long.MAX_VALUE).getMessage())
		);
	}

	@DisplayName("재고가 0인 상품에 대해 주문하면 모든 창고 품절로 409 예외가 발생한다")
	@Test
	void createReservationOrder_fail_outOfStock() {
		Map<String, Object> requestBody = new HashMap<>();
		requestBody.put("productNo", 2L);
		requestBody.put("count", 1);

		Map<String, Object> responseMap = RestAssured
			.given().log().all()
			.contentType(MediaType.APPLICATION_JSON_VALUE)
			.header("Idempotency-Key", IDEMPOTENCY_KEY)
			.queryParam("memberNo", 1L)
			.body(requestBody)
			.when()
			.post("/api/v1/orders")
			.then().log().all()
			.statusCode(HttpStatus.CONFLICT.value())
			.extract()
			.jsonPath().getMap(".");

		assertAll(
			() -> assertThat(responseMap).containsEntry("code", HttpStatus.CONFLICT.value()),
			() -> assertThat(responseMap).containsEntry("message", SoldOutException.warehouse(2L).getMessage())
		);
	}

	@DisplayName("재고보다 많은 수량을 주문하면 모든 창고 품절로 409 예외가 발생한다")
	@Test
	void createReservationOrder_fail_exceedStock() {
		Map<String, Object> requestBody = new HashMap<>();
		requestBody.put("productNo", 3L);
		requestBody.put("count", 2);

		Map<String, Object> responseMap = RestAssured
			.given().log().all()
			.contentType(MediaType.APPLICATION_JSON_VALUE)
			.header("Idempotency-Key", IDEMPOTENCY_KEY)
			.queryParam("memberNo", 1L)
			.body(requestBody)
			.when()
			.post("/api/v1/orders")
			.then().log().all()
			.statusCode(HttpStatus.CONFLICT.value())
			.extract()
			.jsonPath().getMap(".");

		assertAll(
			() -> assertThat(responseMap).containsEntry("code", HttpStatus.CONFLICT.value()),
			() -> assertThat(responseMap).containsEntry("message", SoldOutException.warehouse(3L).getMessage())
		);
	}

	@DisplayName("이미 처리 중인 멱등 키로 주문 요청이 들어오면 409 Conflict 가 발생한다")
	@Test
	void createReservationOrder_idempotent_conflict_processingKey() {
		caffeineCacheManager.getCache(Caches.Name.IDEMPOTENCY).put(REDIS_IDEMPOTENCY_KEY, IdempotentResponse.processing());

		Map<String, Object> requestBody = new HashMap<>();
		requestBody.put("productNo", 1L);
		requestBody.put("count", 1);

		Map<String, Object> responseMap = RestAssured
			.given().log().all()
			.contentType(MediaType.APPLICATION_JSON_VALUE)
			.header("Idempotency-Key", IDEMPOTENCY_KEY)
			.queryParam("memberNo", 1L)
			.body(requestBody)
			.when()
			.post("/api/v1/orders")
			.then().log().all()
			.statusCode(HttpStatus.CONFLICT.value())
			.extract()
			.jsonPath().getMap(".");

		assertAll(
			() -> assertThat(responseMap).containsEntry("code", HttpStatus.CONFLICT.value()),
			() -> assertThat(responseMap).containsEntry("message", "처리 중인 요청이 있습니다")
		);
	}

	@DisplayName("성공한 주문 예약에 동일한 멱등 키로 재요청하면 비즈니스 로직을 재실행하지 않고 캐시된 성공 응답을 반환한다")
	@Test
	void createReservationOrder_idempotent_retryAfterSuccess_returnsCachedResponse() {
		Map<String, Object> requestBody = new HashMap<>();
		requestBody.put("productNo", 1L);
		requestBody.put("count", 1);

		Map<String, Object> firstResponse = RestAssured
			.given().log().all()
			.contentType(MediaType.APPLICATION_JSON_VALUE)
			.header("Idempotency-Key", IDEMPOTENCY_KEY)
			.queryParam("memberNo", 1L)
			.body(requestBody)
			.when()
			.post("/api/v1/orders")
			.then().log().all()
			.statusCode(HttpStatus.CREATED.value())
			.extract().jsonPath().getMap(".");

		// 재고를 0으로 변경 — 비즈니스 로직이 재실행된다면 품절 오류가 반환됨
		redisTemplate.opsForValue().set(String.format(STOCK_KEY_FORMAT, 1, 1), "0");

		Map<String, Object> secondResponse = RestAssured
			.given().log().all()
			.contentType(MediaType.APPLICATION_JSON_VALUE)
			.header("Idempotency-Key", IDEMPOTENCY_KEY)
			.queryParam("memberNo", 1L)
			.body(requestBody)
			.when()
			.post("/api/v1/orders")
			.then().log().all()
			.statusCode(HttpStatus.CREATED.value())
			.extract().jsonPath().getMap(".");

		assertAll(
			() -> assertThat(secondResponse.get("orderNo")).isEqualTo(firstResponse.get("orderNo")),
			() -> assertThat(secondResponse).containsEntry("orderStatus", OrderStatus.PENDING.toString())
		);
	}

	@DisplayName("실패한 주문 예약에 동일한 멱등 키로 재요청하면 비즈니스 로직을 재실행하지 않고 캐시된 오류 응답을 반환한다")
	@Test
	void createReservationOrder_idempotent_retryAfterFailure_returnsCachedErrorResponse() {
		Map<String, Object> requestBody = new HashMap<>();
		requestBody.put("productNo", 2L); // 재고 0
		requestBody.put("count", 1);

		Map<String, Object> firstError = RestAssured
			.given().log().all()
			.contentType(MediaType.APPLICATION_JSON_VALUE)
			.header("Idempotency-Key", IDEMPOTENCY_KEY)
			.queryParam("memberNo", 1L)
			.body(requestBody)
			.when()
			.post("/api/v1/orders")
			.then().log().all()
			.statusCode(HttpStatus.CONFLICT.value())
			.extract().jsonPath().getMap(".");

		// 재고를 복구 — 비즈니스 로직이 재실행된다면 성공 응답이 반환됨
		redisTemplate.opsForValue().set(String.format(STOCK_KEY_FORMAT, 2, 1), "100");

		Map<String, Object> secondError = RestAssured
			.given().log().all()
			.contentType(MediaType.APPLICATION_JSON_VALUE)
			.header("Idempotency-Key", IDEMPOTENCY_KEY)
			.queryParam("memberNo", 1L)
			.body(requestBody)
			.when()
			.post("/api/v1/orders")
			.then().log().all()
			.statusCode(HttpStatus.CONFLICT.value())
			.extract().jsonPath().getMap(".");

		assertAll(
			() -> assertThat(secondError).containsEntry("code", firstError.get("code")),
			() -> assertThat(secondError).containsEntry("message", firstError.get("message"))
		);
	}
}