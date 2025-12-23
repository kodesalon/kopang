package com.kodesalon.kopang.domain.payment;

public interface PaymentClient {
	PaymentResult approve(Payment payment);
}
