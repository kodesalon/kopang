package com.kodesalon.kopang.api.controller.v1.order;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import com.kodesalon.kopang.api.support.AcceptanceTest;
import com.kodesalon.kopang.domain.order.OrderStatus;
import com.kodesalon.kopang.service.exception.NotFoundException;

import io.restassured.RestAssured;

@AcceptanceTest({"acceptance/stock.json", "acceptance/product.json"})
class OrderControllerTest {

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
			() -> assertThat(responseMap).containsEntry("orderNo", 1),
			() -> assertThat(responseMap).containsEntry("quantity", 99),
			() -> assertThat(responseMap).containsEntry("orderStatus", OrderStatus.PENDING.toString()),
			() -> {
				Object totalPrice = responseMap.get("totalPrice");
				BigDecimal actualPrice = new BigDecimal(totalPrice.toString());
				assertThat(actualPrice).isEqualByComparingTo(BigDecimal.valueOf(10000));
			}
		);
	}

	@DisplayName("존재하지 않는 상품을 주문하면 404 예외가 발생한다")
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
			.extract()
			.jsonPath().getMap(".");

		assertAll(
			() -> assertThat(responseMap).containsEntry("code", HttpStatus.NOT_FOUND.value()),
			() -> assertThat(responseMap).containsEntry("message", NotFoundException.stock(Long.MAX_VALUE).getMessage())
		);
	}

	@DisplayName("재고가 0인 상품에 대해 주문하면 400 예외가 발생한다")
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
			.extract()
			.jsonPath().getMap(".");

		assertAll(
			() -> assertThat(responseMap).containsEntry("code", HttpStatus.BAD_REQUEST.value()),
			() -> assertThat(responseMap).containsEntry("message", "남아있는 재고 수량이 없습니다")
		);
	}

	@DisplayName("재고보다 많은 수량을 주문하면 400 예외가 발생한다")
	@Test
	void createReservationOrder_fail() {
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
			.extract()
			.jsonPath().getMap(".");

		assertAll(
			() -> assertThat(responseMap).containsEntry("code", HttpStatus.BAD_REQUEST.value()),
			() -> assertThat(responseMap).containsEntry("message", "재고 수량은 0보다 작을 수 없습니다")
		);
	}
}