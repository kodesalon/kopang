package com.kodesalon.kopang.service.payment;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kodesalon.kopang.domain.payment.Payment;
import com.kodesalon.kopang.domain.payment.PaymentRepository;
import com.kodesalon.kopang.domain.payment.PaymentResult;
import com.kodesalon.kopang.service.order.OrderService;

@Service
public class PaymentService {

	private final PaymentRepository paymentRepository;
	private final OrderService orderService;

	public PaymentService(PaymentRepository paymentRepository, OrderService orderService) {
		this.paymentRepository = paymentRepository;
		this.orderService = orderService;
	}

	@Transactional
	public Payment completePayment(Long orderNo, PaymentResult result) {
		orderService.pay(orderNo);
		return paymentRepository.register(Payment.createSuccess(orderNo, result));
	}

	@Transactional
	public void registerFailedPayment(Long orderNo, PaymentResult result) {
		paymentRepository.register(Payment.createFailure(orderNo, result));
	}
}
