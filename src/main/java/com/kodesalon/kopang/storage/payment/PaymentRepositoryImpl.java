package com.kodesalon.kopang.storage.payment;

import org.springframework.stereotype.Repository;

import com.kodesalon.kopang.domain.payment.Payment;
import com.kodesalon.kopang.domain.payment.PaymentRepository;

@Repository
public class PaymentRepositoryImpl implements PaymentRepository {

	private final PaymentJpaRepository paymentJpaRepository;

	public PaymentRepositoryImpl(PaymentJpaRepository paymentJpaRepository) {
		this.paymentJpaRepository = paymentJpaRepository;
	}

	@Override
	public Payment register(Payment payment) {
		return paymentJpaRepository.save(PaymentJpaEntity.from(payment)).toDomain();
	}
}
