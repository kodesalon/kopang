package com.kodesalon.kopang.service.exception;

public class DuplicateRequestException extends RuntimeException {

	public DuplicateRequestException(String message) {
		super(message);
	}

	public static DuplicateRequestException detected() {
		return new DuplicateRequestException("이미 처리 중인 요청입니다");
	}
}