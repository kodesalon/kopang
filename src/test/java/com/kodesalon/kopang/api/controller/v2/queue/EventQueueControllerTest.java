package com.kodesalon.kopang.api.controller.v2.queue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import com.kodesalon.kopang.api.support.AcceptanceTest;

import io.restassured.RestAssured;

@AcceptanceTest({
	"acceptance/queue/product.json",
	"acceptance/queue/warehouse.json",
	"acceptance/queue/member_address.json",
	"acceptance/queue/stock.json"
})
class EventQueueControllerTest {

	private static final Long EVENT_ID = 1L;
	private static final Long MEMBER_NO = 1L;
	private static final String REDIS_STOCK_KEY = "stock:product:1:warehouse:1";
	private static final String REDIS_MEMBER_ADDRESS_CACHE_KEY = "member_address::1";
	private static final String QUEUE_EVENT_KEY = "queue:event:1";
	private static final String QUEUE_ACTIVE_EVENTS_KEY = "queue:active_events";
	private static final String QUEUE_ACTIVE_KEY = "queue:active:1";
	private static final String QUEUE_LOCK_KEY = "queue:lock:1";

	@Autowired
	StringRedisTemplate redisTemplate;

	@AfterEach
	void cleanUpRedis() {
		redisTemplate.delete(REDIS_STOCK_KEY);
		redisTemplate.delete(REDIS_MEMBER_ADDRESS_CACHE_KEY);
		redisTemplate.delete(QUEUE_EVENT_KEY);
		redisTemplate.delete(QUEUE_ACTIVE_EVENTS_KEY);
		redisTemplate.delete(QUEUE_ACTIVE_KEY);
		redisTemplate.delete(QUEUE_LOCK_KEY);

		Set<String> entryKeys = redisTemplate.keys("queue:entry:*");
		if (entryKeys != null && !entryKeys.isEmpty()) {
			redisTemplate.delete(entryKeys);
		}
	}

	@Nested
	@DisplayName("POST /api/v2/events/{eventId}/queue — 대기열 진입")
	class EnterQueue {

		@Test
		@DisplayName("유효한 요청으로 대기열에 진입하면, 202 응답과 함께 token, position, estimatedWaitMs를 반환한다")
		void enterQueue_ValidRequest_Returns202WithTokenAndPosition() {
			// given
			redisTemplate.opsForValue().set(REDIS_STOCK_KEY, "100");

			// when
			Map<String, Object> response = RestAssured
				.given().log().all()
				.contentType(MediaType.APPLICATION_JSON_VALUE)
				.queryParam("memberNo", MEMBER_NO)
				.body(Map.of("count", 1))
				.when()
				.post("/api/v2/events/{eventId}/queue", EVENT_ID)
				.then().log().all()
				.statusCode(HttpStatus.ACCEPTED.value())
				.extract()
				.jsonPath().getMap(".");

			// then
			assertAll(
				() -> assertThat(response.get("token")).isNotNull(),
				() -> assertThat(response.get("token").toString()).isNotBlank(),
				() -> assertThat((Integer) response.get("position")).isGreaterThanOrEqualTo(0),
				() -> assertThat(((Number) response.get("estimatedWaitMs")).longValue()).isGreaterThanOrEqualTo(0L)
			);
		}
	}

	@Nested
	@DisplayName("GET /api/v2/events/{eventId}/queue/{token}/status — 대기열 상태 조회")
	class GetQueueStatus {

		@Test
		@DisplayName("대기열에 진입한 토큰의 상태를 조회하면, WAITING 상태와 position을 반환한다")
		void getQueueStatus_WaitingToken_ReturnsWaitingWithPosition() {
			// given
			redisTemplate.opsForValue().set(REDIS_STOCK_KEY, "100");

			Map<String, Object> enterResponse = RestAssured
				.given()
				.contentType(MediaType.APPLICATION_JSON_VALUE)
				.queryParam("memberNo", MEMBER_NO)
				.body(Map.of("count", 1))
				.when()
				.post("/api/v2/events/{eventId}/queue", EVENT_ID)
				.then()
				.statusCode(HttpStatus.ACCEPTED.value())
				.extract()
				.jsonPath().getMap(".");
			String token = enterResponse.get("token").toString();

			// when
			Map<String, Object> statusResponse = RestAssured
				.given().log().all()
				.when()
				.get("/api/v2/events/{eventId}/queue/{token}/status", EVENT_ID, token)
				.then().log().all()
				.statusCode(HttpStatus.OK.value())
				.extract()
				.jsonPath().getMap(".");

			// then
			assertAll(
				() -> assertThat(statusResponse.get("status")).isEqualTo("WAITING"),
				() -> assertThat((Integer) statusResponse.get("position")).isGreaterThanOrEqualTo(0)
			);
		}

		@Test
		@DisplayName("ACTIVE Set에 등록된 토큰의 상태를 조회하면, ACTIVE 상태를 반환한다")
		void getQueueStatus_ActiveToken_ReturnsActive() {
			// given
			redisTemplate.opsForValue().set(REDIS_STOCK_KEY, "100");

			Map<String, Object> enterResponse = RestAssured
				.given()
				.contentType(MediaType.APPLICATION_JSON_VALUE)
				.queryParam("memberNo", MEMBER_NO)
				.body(Map.of("count", 1))
				.when()
				.post("/api/v2/events/{eventId}/queue", EVENT_ID)
				.then()
				.statusCode(HttpStatus.ACCEPTED.value())
				.extract()
				.jsonPath().getMap(".");
			String token = enterResponse.get("token").toString();

			// manually activate token
			redisTemplate.opsForSet().add(QUEUE_ACTIVE_KEY, token);

			// when
			Map<String, Object> statusResponse = RestAssured
				.given().log().all()
				.when()
				.get("/api/v2/events/{eventId}/queue/{token}/status", EVENT_ID, token)
				.then().log().all()
				.statusCode(HttpStatus.OK.value())
				.extract()
				.jsonPath().getMap(".");

			// then
			assertThat(statusResponse.get("status")).isEqualTo("ACTIVE");
		}

		@Test
		@DisplayName("어떤 Redis 구조에도 존재하지 않는 토큰의 상태를 조회하면, EXPIRED 상태를 반환한다")
		void getQueueStatus_ExpiredToken_ReturnsExpired() {
			// given
			String randomToken = UUID.randomUUID().toString();

			// when
			Map<String, Object> statusResponse = RestAssured
				.given().log().all()
				.when()
				.get("/api/v2/events/{eventId}/queue/{token}/status", EVENT_ID, randomToken)
				.then().log().all()
				.statusCode(HttpStatus.OK.value())
				.extract()
				.jsonPath().getMap(".");

			// then
			assertThat(statusResponse.get("status")).isEqualTo("EXPIRED");
		}
	}

	@Nested
	@DisplayName("POST /api/v1/orders + X-Queue-Token 헤더 — 인터셉터 검증")
	class OrderWithQueueToken {

		@Test
		@DisplayName("ACTIVE 상태의 유효한 Queue Token으로 주문하면, 201 응답을 반환한다")
		void v1Order_WithActiveQueueToken_Returns201() {
			// given
			redisTemplate.opsForValue().set(REDIS_STOCK_KEY, "100");

			Map<String, Object> enterResponse = RestAssured
				.given()
				.contentType(MediaType.APPLICATION_JSON_VALUE)
				.queryParam("memberNo", MEMBER_NO)
				.body(Map.of("count", 1))
				.when()
				.post("/api/v2/events/{eventId}/queue", EVENT_ID)
				.then()
				.statusCode(HttpStatus.ACCEPTED.value())
				.extract()
				.jsonPath().getMap(".");
			String token = enterResponse.get("token").toString();

			// manually activate token
			redisTemplate.opsForSet().add(QUEUE_ACTIVE_KEY, token);

			// when
			Map<String, Object> orderResponse = RestAssured
				.given().log().all()
				.contentType(MediaType.APPLICATION_JSON_VALUE)
				.header("X-Queue-Token", token)
				.queryParam("memberNo", MEMBER_NO)
				.body(Map.of("productNo", 1L, "count", 1))
				.when()
				.post("/api/v1/orders")
				.then().log().all()
				.statusCode(HttpStatus.CREATED.value())
				.extract()
				.jsonPath().getMap(".");

			// then
			assertThat(orderResponse).containsKey("orderNo");
		}

		@Test
		@DisplayName("존재하지 않는 Queue Token으로 주문하면, 401 응답을 반환한다")
		void v1Order_WithExpiredQueueToken_Returns401() {
			// given
			redisTemplate.opsForValue().set(REDIS_STOCK_KEY, "100");
			String randomToken = UUID.randomUUID().toString();

			// when & then
			RestAssured
				.given().log().all()
				.contentType(MediaType.APPLICATION_JSON_VALUE)
				.header("X-Queue-Token", randomToken)
				.queryParam("memberNo", MEMBER_NO)
				.body(Map.of("productNo", 1L, "count", 1))
				.when()
				.post("/api/v1/orders")
				.then().log().all()
				.statusCode(HttpStatus.UNAUTHORIZED.value());
		}
	}
}
