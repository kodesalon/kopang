package com.kodesalon.kopang.domain.payment;

public enum PaymentStatus {

	DONE("결제 승인"),
	ABORTED("결제 실패"),
	CANCELED("결제 취소"),
	;

	private final String description;

	PaymentStatus(String description) {
		this.description = description;
	}
}
