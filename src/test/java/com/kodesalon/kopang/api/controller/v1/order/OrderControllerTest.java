package com.kodesalon.kopang.api.controller.v1.order;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import java.math.BigDecimal;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import com.kodesalon.kopang.api.support.AcceptanceTest;

import io.restassured.RestAssured;

@AcceptanceTest({
	"acceptance/order/product.json",
	"acceptance/order/warehouse.json",
	"acceptance/order/member_address.json",
	"acceptance/order/stock.json"
})
class OrderControllerTest {

	private static final String REDIS_STOCK_KEY = "stock:product:1:warehouse:1";
	private static final String REDIS_MEMBER_ADDRESS_CACHE_KEY = "member_address::1";

	@Autowired
	StringRedisTemplate redisTemplate;

	@AfterEach
	void cleanUpRedis() {
		redisTemplate.delete(REDIS_STOCK_KEY);
		redisTemplate.delete(REDIS_MEMBER_ADDRESS_CACHE_KEY);
	}

	@DisplayName("Redis 재고가 있는 상품을 주문하면, 201 응답과 함께 PENDING 상태의 주문과 남은 재고를 반환한다.")
	@Test
	void createReservationOrder_StockAvailable_ReturnsCreatedOrderWithRemainingStock() {
		// given
		redisTemplate.opsForValue().set(REDIS_STOCK_KEY, "100");

		// when
		Map<String, Object> response = RestAssured
			.given().log().all()
			.contentType(MediaType.APPLICATION_JSON_VALUE)
			.queryParam("memberNo", 1L)
			.body(Map.of("productNo", 1L, "count", 1))
			.when()
			.post("/api/v1/orders")
			.then().log().all()
			.statusCode(HttpStatus.CREATED.value())
			.extract()
			.jsonPath().getMap(".");

		// then
		assertAll(
			() -> assertThat(response).containsKey("orderNo"),
			() -> assertThat(response).containsEntry("quantity", 99),
			() -> assertThat(response).containsEntry("orderStatus", "PENDING"),
			() -> assertThat(new BigDecimal(response.get("totalPrice").toString()))
				.isEqualByComparingTo(BigDecimal.valueOf(10000))
		);
	}

	@DisplayName("Redis 재고가 없는 상품을 주문하면, SoldOutException이 발생하고 409 Conflict와 품절 메시지를 반환한다.")
	@Test
	void createReservationOrder_StockEmpty_ReturnsConflictWithSoldOutMessage() {
		// given — Redis 재고 키 미설정: Lua 스크립트가 -1 반환 → SoldOutException

		// when
		Map<String, Object> response = RestAssured
			.given().log().all()
			.contentType(MediaType.APPLICATION_JSON_VALUE)
			.queryParam("memberNo", 1L)
			.body(Map.of("productNo", 1L, "count", 1))
			.when()
			.post("/api/v1/orders")
			.then().log().all()
			.statusCode(HttpStatus.CONFLICT.value())
			.extract()
			.jsonPath().getMap(".");

		// then
		assertAll(
			() -> assertThat(response).containsEntry("code", HttpStatus.CONFLICT.value()),
			() -> assertThat(response.get("message").toString()).contains("품절")
		);
	}
}