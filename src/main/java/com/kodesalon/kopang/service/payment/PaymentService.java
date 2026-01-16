package com.kodesalon.kopang.service.payment;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kodesalon.kopang.domain.payment.Payment;
import com.kodesalon.kopang.domain.payment.PaymentRepository;
import com.kodesalon.kopang.domain.payment.PaymentResult;

@Service
public class PaymentService {

	private final PaymentRepository paymentRepository;

	public PaymentService(PaymentRepository paymentRepository) {
		this.paymentRepository = paymentRepository;
	}

	@Transactional
	public Payment completePayment(Long orderNo, PaymentResult result) {
		return paymentRepository.register(Payment.createSuccess(orderNo, result));
	}

	@Transactional
	public void registerFailedPayment(Long orderNo, PaymentResult result) {
		paymentRepository.register(Payment.createFailure(orderNo, result));
	}
}
