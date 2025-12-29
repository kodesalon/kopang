package com.kodesalon.kopang.domain.payment;

import java.time.LocalDateTime;

import com.kodesalon.kopang.domain.order.Money;

public record PaymentResult(
	String paymentKey,
	Long orderNo,
	Money amount,
	LocalDateTime approvedAt,
	String status,
	String failureMessage
) {
}
