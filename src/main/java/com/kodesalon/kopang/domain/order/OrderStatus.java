package com.kodesalon.kopang.domain.order;

public enum OrderStatus {

	PENDING,
	CANCELLED,
	PAID,
	PAYMENT_IN_PROGRESS,
	;

	public boolean isCanceled() {
		return this == CANCELLED;
	}

	public boolean isPaid() {
		return this == PAID;
	}

	public boolean isPending() {
		return this == PENDING;
	}
}
