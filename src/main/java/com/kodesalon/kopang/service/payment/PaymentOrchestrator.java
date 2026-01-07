package com.kodesalon.kopang.service.payment;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;

import com.kodesalon.kopang.domain.payment.Payment;
import com.kodesalon.kopang.domain.payment.PaymentClient;
import com.kodesalon.kopang.domain.payment.PaymentResult;
import com.kodesalon.kopang.service.exception.PaymentFailedException;
import com.kodesalon.kopang.service.order.OrderService;

@Component
public class PaymentOrchestrator {

	private final PaymentService paymentService;
	private final OrderService orderService;
	private final PaymentClient paymentClient;

	public PaymentOrchestrator(PaymentService paymentService, OrderService orderService, PaymentClient paymentClient) {
		this.paymentService = paymentService;
		this.orderService = orderService;
		this.paymentClient = paymentClient;
	}

	public Payment executePayment(String paymentKey, Long orderNo, BigDecimal amount) {
		orderService.prepareOrderForPayment(orderNo, amount);

		PaymentResult result;
		try {
			result = paymentClient.approve(paymentKey, orderNo, amount);
		} catch (Exception e) {
			orderService.rollbackToPending(orderNo);
			throw e;
		}

		switch (result.status()) {
			case "DONE" -> {
				return paymentService.completePayment(orderNo, result);
			}
			case "ABORTED" -> {
				paymentService.registerFailedPayment(orderNo, result);
				orderService.rollbackToPending(orderNo);
				throw PaymentFailedException.aborted(paymentKey, orderNo, result.failureMessage());
			}
			case "EXPIRED" -> {
				paymentService.registerFailedPayment(orderNo, result);
				orderService.cancelOrder(orderNo);
				throw PaymentFailedException.expired(paymentKey, orderNo, result.failureMessage());
			}
			default -> throw PaymentFailedException.invalid(paymentKey, orderNo, result.status());
		}
	}
}
