package com.kodesalon.kopang.service.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DuplicateRequestExceptionTest {

	@DisplayName("detected() 팩토리 메서드는 '이미 처리 중인 요청입니다' 메시지를 가진 예외를 생성한다")
	@Test
	void detected_hasExpectedMessage() {
		DuplicateRequestException exception = DuplicateRequestException.detected();

		assertThat(exception.getMessage()).isEqualTo("이미 처리 중인 요청입니다");
	}

	@DisplayName("DuplicateRequestException 은 RuntimeException 을 상속한다")
	@Test
	void detected_isRuntimeException() {
		DuplicateRequestException exception = DuplicateRequestException.detected();

		assertThat(exception).isInstanceOf(RuntimeException.class);
	}

	@DisplayName("생성자로 직접 메시지를 지정하면 해당 메시지를 가진 예외가 생성된다")
	@Test
	void constructor_withCustomMessage() {
		String customMessage = "사용자 정의 중복 요청 메시지";

		DuplicateRequestException exception = new DuplicateRequestException(customMessage);

		assertAll(
			() -> assertThat(exception.getMessage()).isEqualTo(customMessage),
			() -> assertThat(exception).isInstanceOf(RuntimeException.class)
		);
	}

	@DisplayName("detected() 를 여러 번 호출하면 매번 독립적인 예외 인스턴스를 반환한다")
	@Test
	void detected_returnsDifferentInstancesOnEachCall() {
		DuplicateRequestException first = DuplicateRequestException.detected();
		DuplicateRequestException second = DuplicateRequestException.detected();

		assertAll(
			() -> assertThat(first).isNotSameAs(second),
			() -> assertThat(first.getMessage()).isEqualTo(second.getMessage())
		);
	}
}