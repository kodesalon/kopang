package com.kodesalon.kopang.service.exception;

public class IdempotencyUnavailableException extends RuntimeException {

	public IdempotencyUnavailableException() {
		super("멱등성 처리를 보장할 수 없습니다. 잠시 후 다시 시도해 주세요.");
	}
}
