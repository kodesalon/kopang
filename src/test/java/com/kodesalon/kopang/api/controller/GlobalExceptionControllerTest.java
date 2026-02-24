package com.kodesalon.kopang.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.kodesalon.kopang.service.exception.DuplicateRequestException;
import com.kodesalon.kopang.service.exception.NotFoundException;

class GlobalExceptionControllerTest {

	private GlobalExceptionController globalExceptionController;

	@BeforeEach
	void setUp() {
		globalExceptionController = new GlobalExceptionController();
	}

	@Nested
	@DisplayName("DuplicateRequestException 처리 테스트")
	class DuplicateRequestExceptionHandlerTest {

		@DisplayName("DuplicateRequestException 이 발생하면 409 Conflict 응답을 반환한다")
		@Test
		void conflict_returns409Status() {
			DuplicateRequestException exception = DuplicateRequestException.detected();

			ResponseEntity<KopangExceptionResponse> response = globalExceptionController.conflict(exception);

			assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
		}

		@DisplayName("DuplicateRequestException 응답 바디에 예외 메시지와 409 코드가 포함된다")
		@Test
		void conflict_includesMessageAndCodeInBody() {
			DuplicateRequestException exception = DuplicateRequestException.detected();

			ResponseEntity<KopangExceptionResponse> response = globalExceptionController.conflict(exception);

			KopangExceptionResponse body = response.getBody();
			assertAll(
				() -> assertThat(body).isNotNull(),
				() -> assertThat(body.message()).isEqualTo("이미 처리 중인 요청입니다"),
				() -> assertThat(body.code()).isEqualTo(HttpStatus.CONFLICT.value())
			);
		}

		@DisplayName("사용자 정의 메시지를 가진 DuplicateRequestException 도 409 응답으로 처리된다")
		@Test
		void conflict_handlesCustomMessageException() {
			String customMessage = "결제 요청이 이미 처리 중입니다";
			DuplicateRequestException exception = new DuplicateRequestException(customMessage);

			ResponseEntity<KopangExceptionResponse> response = globalExceptionController.conflict(exception);

			assertAll(
				() -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT),
				() -> assertThat(response.getBody()).isNotNull(),
				() -> assertThat(response.getBody().message()).isEqualTo(customMessage),
				() -> assertThat(response.getBody().code()).isEqualTo(409)
			);
		}
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
