package com.kodesalon.kopang.api.controller.v1.order;

import java.math.BigDecimal;

public record PaymentRequest(
	BigDecimal amount,
	String paymentKey,
	Long productNo,
	Integer orderCount
) {
}
