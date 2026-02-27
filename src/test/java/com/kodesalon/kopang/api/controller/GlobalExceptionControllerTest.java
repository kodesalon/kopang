package com.kodesalon.kopang.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.kodesalon.kopang.service.exception.NotFoundException;
import com.kodesalon.kopang.service.exception.PaymentFailedException;
import com.kodesalon.kopang.service.exception.SoldOutException;

class GlobalExceptionControllerTest {

	private GlobalExceptionController globalExceptionController;

	@BeforeEach
	void setUp() {
		globalExceptionController = new GlobalExceptionController();
	}

	@Nested
	@DisplayName("IllegalArgumentException / IllegalStateException 처리 테스트")
	class BadRequestHandlerTest {

		@DisplayName("IllegalArgumentException 이 발생하면 400 Bad Request 응답을 반환한다")
		@Test
		void badRequest_returns400ForIllegalArgument() {
			IllegalArgumentException exception = new IllegalArgumentException("잘못된 파라미터입니다");

			ResponseEntity<KopangExceptionResponse> response = globalExceptionController.badRequest(exception);

			assertAll(
				() -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST),
				() -> assertThat(response.getBody()).isNotNull(),
				() -> assertThat(response.getBody().message()).isEqualTo("잘못된 파라미터입니다"),
				() -> assertThat(response.getBody().code()).isEqualTo(HttpStatus.BAD_REQUEST.value())
			);
		}

		@DisplayName("IllegalStateException 이 발생하면 400 Bad Request 응답을 반환한다")
		@Test
		void badRequest_returns400ForIllegalState() {
			IllegalStateException exception = new IllegalStateException("잘못된 상태입니다");

			ResponseEntity<KopangExceptionResponse> response = globalExceptionController.badRequest(exception);

			assertAll(
				() -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST),
				() -> assertThat(response.getBody()).isNotNull(),
				() -> assertThat(response.getBody().message()).isEqualTo("잘못된 상태입니다"),
				() -> assertThat(response.getBody().code()).isEqualTo(HttpStatus.BAD_REQUEST.value())
			);
		}
	}

	@Nested
	@DisplayName("NotFoundException 처리 테스트")
	class NotFoundHandlerTest {

		@DisplayName("NotFoundException 이 발생하면 404 Not Found 응답을 반환한다")
		@Test
		void notFound_returns404Status() {
			NotFoundException exception = NotFoundException.product(1L);

			ResponseEntity<KopangExceptionResponse> response = globalExceptionController.notFound(exception);

			assertAll(
				() -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND),
				() -> assertThat(response.getBody()).isNotNull(),
				() -> assertThat(response.getBody().message()).isEqualTo("상품 1 를 찾을 수 없습니다"),
				() -> assertThat(response.getBody().code()).isEqualTo(HttpStatus.NOT_FOUND.value())
			);
		}
	}

	@Nested
	@DisplayName("SoldOutException 처리 테스트")
	class SoldOutExceptionHandlerTest {

		@DisplayName("SoldOutException 이 발생하면 409 Conflict 응답을 반환한다")
		@Test
		void conflict_returns409ForSoldOut() {
			SoldOutException exception = SoldOutException.warehouse(1L);

			ResponseEntity<KopangExceptionResponse> response = globalExceptionController.conflict(exception);

			assertAll(
				() -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT),
				() -> assertThat(response.getBody()).isNotNull(),
				() -> assertThat(response.getBody().code()).isEqualTo(HttpStatus.CONFLICT.value())
			);
		}
	}

	@Nested
	@DisplayName("PaymentFailedException 처리 테스트")
	class PaymentFailedExceptionHandlerTest {

		@DisplayName("PaymentFailedException 이 발생하면 422 Unprocessable Entity 응답을 반환한다")
		@Test
		void unprocessableEntity_returns422ForPaymentFailed() {
			PaymentFailedException exception = PaymentFailedException.aborted("pay-key", 1L, "잔액 부족");

			ResponseEntity<KopangExceptionResponse> response = globalExceptionController.unprocessableEntity(exception);

			assertAll(
				() -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY),
				() -> assertThat(response.getBody()).isNotNull(),
				() -> assertThat(response.getBody().code()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY.value())
			);
		}
	}

	@Nested
	@DisplayName("처리되지 않은 예외 처리 테스트")
	class InternalServerErrorHandlerTest {

		@DisplayName("예상치 못한 Exception 이 발생하면 500 Internal Server Error 응답을 반환한다")
		@Test
		void internalServerError_returns500Status() {
			Exception exception = new RuntimeException("서버 내부 오류");

			ResponseEntity<KopangExceptionResponse> response = globalExceptionController.internalServerError(exception);

			assertAll(
				() -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR),
				() -> assertThat(response.getBody()).isNotNull(),
				() -> assertThat(response.getBody().message()).isEqualTo("서버 내부 오류"),
				() -> assertThat(response.getBody().code()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value())
			);
		}
	}
}
