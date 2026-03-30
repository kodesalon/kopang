package com.kodesalon.kopang.service.exception;

public class DuplicateQueueEntryException extends RuntimeException {

	public DuplicateQueueEntryException(String message) {
		super(message);
	}

	public static DuplicateQueueEntryException of(Long eventId, Long memberNo) {
		return new DuplicateQueueEntryException(
			String.format("회원 {%d}은 이미 이벤트 {%d} 대기열에 등록되어 있습니다.", memberNo, eventId)
		);
	}
}
