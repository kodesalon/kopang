package com.kodesalon.kopang.domain.payment;

import java.math.BigDecimal;

public interface PaymentClient {
	PaymentResult approve(String paymentKey, Long orderNo, BigDecimal amount);
}
