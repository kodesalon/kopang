package com.kodesalon.kopang.domain.queue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class QueueEntryTest {

	@Nested
	@DisplayName("QueueEntry.of()")
	class Of {

		@Test
		@DisplayName("정상 인자로 of()를 호출하면 유효한 UUID 형식의 토큰이 생성된다")
		void of_ValidArguments_GeneratesUuidFormatToken() {
			// given
			Long eventId = 1L;
			Long memberNo = 42L;
			Integer count = 2;

			// when
			QueueEntry entry = QueueEntry.of(eventId, memberNo, count);

			// then
			assertThat(entry.token()).isNotNull();
			UUID parsed = UUID.fromString(entry.token());
			assertThat(parsed).isNotNull();
		}

		@Test
		@DisplayName("of()를 호출하면 requestedAt이 현재 시각(1초 이내)으로 설정된다")
		void of_Called_SetsRequestedAtToCurrentTime() {
			// given
			long before = System.currentTimeMillis();

			// when
			QueueEntry entry = QueueEntry.of(1L, 1L, 1);

			// then
			long after = System.currentTimeMillis();
			assertThat(entry.requestedAt())
				.isGreaterThanOrEqualTo(before)
				.isLessThanOrEqualTo(after + 1000L);
		}

		@Test
		@DisplayName("of()를 호출하면 eventId, memberNo, count가 전달된 인자 그대로 설정된다")
		void of_ValidArguments_SetsEventIdMemberNoCountCorrectly() {
			// given
			Long eventId = 7L;
			Long memberNo = 99L;
			Integer count = 3;

			// when
			QueueEntry entry = QueueEntry.of(eventId, memberNo, count);

			// then
			assertAll(
				() -> assertThat(entry.eventId()).isEqualTo(eventId),
				() -> assertThat(entry.memberNo()).isEqualTo(memberNo),
				() -> assertThat(entry.count()).isEqualTo(count)
			);
		}

		@Test
		@DisplayName("of()를 두 번 호출하면 서로 다른 토큰이 생성된다")
		void of_CalledTwice_GeneratesDistinctTokens() {
			// given
			Long eventId = 1L;
			Long memberNo = 1L;
			Integer count = 1;

			// when
			QueueEntry first = QueueEntry.of(eventId, memberNo, count);
			QueueEntry second = QueueEntry.of(eventId, memberNo, count);

			// then
			assertThat(first.token()).isNotEqualTo(second.token());
		}
	}

	@Nested
	@DisplayName("QueueStatus enum")
	class QueueStatusEnum {

		@Test
		@DisplayName("QueueStatus enum은 WAITING, ACTIVE, EXPIRED 세 가지 값을 갖는다")
		void queueStatus_EnumValues_ContainsWaitingActiveExpired() {
			// when
			QueueStatus[] values = QueueStatus.values();

			// then
			assertAll(
				() -> assertThat(values).hasSize(3),
				() -> assertThat(values).contains(QueueStatus.WAITING),
				() -> assertThat(values).contains(QueueStatus.ACTIVE),
				() -> assertThat(values).contains(QueueStatus.EXPIRED)
			);
		}

		@Test
		@DisplayName("QueueStatus.valueOf()로 각 상태를 이름으로 조회할 수 있다")
		void queueStatus_ValueOf_ReturnsCorrectEnum() {
			assertAll(
				() -> assertThat(QueueStatus.valueOf("WAITING")).isEqualTo(QueueStatus.WAITING),
				() -> assertThat(QueueStatus.valueOf("ACTIVE")).isEqualTo(QueueStatus.ACTIVE),
				() -> assertThat(QueueStatus.valueOf("EXPIRED")).isEqualTo(QueueStatus.EXPIRED)
			);
		}
	}
}
