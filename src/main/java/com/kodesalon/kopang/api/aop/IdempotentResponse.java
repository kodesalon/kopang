package com.kodesalon.kopang.api.aop;

public record IdempotentResponse(
	IdempotencyStatus status,
	int httpStatus,
	String body
) {
	public static IdempotentResponse processing() {
		return new IdempotentResponse(IdempotencyStatus.PROCESSING, 0, null);
	}

	public static IdempotentResponse completed(int httpStatus, String body) {
		return new IdempotentResponse(IdempotencyStatus.COMPLETED, httpStatus, body);
	}

	public static IdempotentResponse failed(int httpStatus, String body) {
		return new IdempotentResponse(IdempotencyStatus.FAILED, httpStatus, body);
	}

	public boolean isTerminal() {
		return status == IdempotencyStatus.COMPLETED || status == IdempotencyStatus.FAILED;
	}
}