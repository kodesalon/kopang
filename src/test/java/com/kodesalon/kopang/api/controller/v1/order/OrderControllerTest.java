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
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import com.kodesalon.kopang.api.support.AcceptanceTest;
import com.kodesalon.kopang.domain.order.OrderStatus;
import com.kodesalon.kopang.service.exception.SoldOutException;

import io.restassured.RestAssured;

@AcceptanceTest({
	"acceptance/warehouse.json",
	"acceptance/member_address.json",
	"acceptance/product.json",
	"acceptance/stock.json"})
class OrderControllerTest {

	private static final String STOCK_KEY_FORMAT = "stock:product:%d:warehouse:%d";
	private static final String DUPLICATE_KEY_FORMAT = "duplicate:order:%d:%d";

	@Autowired
	private RedisTemplate<String, String> redisTemplate;

	@BeforeEach
	void setUpRedis() {
		redisTemplate.opsForValue().set(String.format(STOCK_KEY_FORMAT, 1, 1), "100");
		redisTemplate.opsForValue().set(String.format(STOCK_KEY_FORMAT, 2, 1), "0");
		redisTemplate.opsForValue().set(String.format(STOCK_KEY_FORMAT, 3, 1), "1");
		redisTemplate.delete(String.format(DUPLICATE_KEY_FORMAT, 1, 1));
		redisTemplate.delete(String.format(DUPLICATE_KEY_FORMAT, 1, 2));
		redisTemplate.delete(String.format(DUPLICATE_KEY_FORMAT, 1, 3));
		redisTemplate.delete(String.format(DUPLICATE_KEY_FORMAT, 1, Long.MAX_VALUE));
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

	@DisplayName("존재하지 않는 상품을 주문하면 모든 창고에 재고가 없어 500 예외가 발생한다")
	@Test
	void createReservationOrder_fail_notFound() {
		Map<String, Object> requestBody = new HashMap<>();
		requestBody.put("productNo", Long.MAX_VALUE);
		requestBody.put("count", 1);

		Map<String, Object> responseMap = RestAssured
			.given().log().all()
			.contentType(MediaType.APPLICATION_JSON_VALUE)
			.queryParam("memberNo", 1L)
			.body(requestBody)
			.when()
			.post("/api/v1/orders")
			.then().log().all()
			.statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
			.extract()
			.jsonPath().getMap(".");

		assertAll(
			() -> assertThat(responseMap).containsEntry("code", HttpStatus.INTERNAL_SERVER_ERROR.value()),
			() -> assertThat(responseMap).containsEntry("message", SoldOutException.warehouse(Long.MAX_VALUE).getMessage())
		);
	}

	@DisplayName("재고가 0인 상품에 대해 주문하면 모든 창고 품절로 500 예외가 발생한다")
	@Test
	void createReservationOrder_fail_outOfStock() {
		Map<String, Object> requestBody = new HashMap<>();
		requestBody.put("productNo", 2L);
		requestBody.put("count", 1);

		Map<String, Object> responseMap = RestAssured
			.given().log().all()
			.contentType(MediaType.APPLICATION_JSON_VALUE)
			.queryParam("memberNo", 1L)
			.body(requestBody)
			.when()
			.post("/api/v1/orders")
			.then().log().all()
			.statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
			.extract()
			.jsonPath().getMap(".");

		assertAll(
			() -> assertThat(responseMap).containsEntry("code", HttpStatus.INTERNAL_SERVER_ERROR.value()),
			() -> assertThat(responseMap).containsEntry("message", SoldOutException.warehouse(2L).getMessage())
		);
	}

	@DisplayName("재고보다 많은 수량을 주문하면 모든 창고 품절로 500 예외가 발생한다")
	@Test
	void createReservationOrder_fail_exceedStock() {
		Map<String, Object> requestBody = new HashMap<>();
		requestBody.put("productNo", 3L);
		requestBody.put("count", 2);

		Map<String, Object> responseMap = RestAssured
			.given().log().all()
			.contentType(MediaType.APPLICATION_JSON_VALUE)
			.queryParam("memberNo", 1L)
			.body(requestBody)
			.when()
			.post("/api/v1/orders")
			.then().log().all()
			.statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
			.extract()
			.jsonPath().getMap(".");

		assertAll(
			() -> assertThat(responseMap).containsEntry("code", HttpStatus.INTERNAL_SERVER_ERROR.value()),
			() -> assertThat(responseMap).containsEntry("message", SoldOutException.warehouse(3L).getMessage())
		);
	}
}