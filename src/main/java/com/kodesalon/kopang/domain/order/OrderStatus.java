package com.kodesalon.kopang.domain.order;

public enum OrderStatus {
	PENDING,
	CANCELLED,
	PAID,
	;

	public boolean isCanceled() {
		return this == CANCELLED;
	}

	public boolean isPaid() {
		return this == PAID;
	}
}
