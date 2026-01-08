package com.kodesalon.kopang.api.controller.v1.order;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.kodesalon.kopang.domain.payment.Payment;

public record PaymentResponse(
	Long paymentNo,
	Long orderNo,
	String paymentStatus,
	BigDecimal totalAmount,
	LocalDateTime paidAt
) {
	public static PaymentResponse from(Payment payment) {
		return new PaymentResponse(
			payment.getNo(),
			payment.getOrderNo(),
			payment.getStatus().name(),
			payment.getAmount().getAmount(),
			payment.getCreatedAt()
		);
	}
}
