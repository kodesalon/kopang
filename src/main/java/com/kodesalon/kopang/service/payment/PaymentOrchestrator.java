package com.kodesalon.kopang.service.payment;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;

import com.kodesalon.kopang.domain.payment.Payment;
import com.kodesalon.kopang.domain.payment.PaymentClient;
import com.kodesalon.kopang.domain.payment.PaymentResult;
import com.kodesalon.kopang.service.exception.PaymentFailedException;
import com.kodesalon.kopang.service.order.OrderService;
import com.kodesalon.kopang.service.purchase.PurchaseOrchestrator;

@Component
public class PaymentOrchestrator {

	private final PaymentService paymentService;
	private final OrderService orderService;
	private final PurchaseOrchestrator purchaseOrchestrator;
	private final PaymentClient paymentClient;

	public PaymentOrchestrator(
		PaymentService paymentService,
		OrderService orderService,
		PurchaseOrchestrator purchaseOrchestrator,
		PaymentClient paymentClient
	) {
		this.paymentService = paymentService;
		this.orderService = orderService;
		this.purchaseOrchestrator = purchaseOrchestrator;
		this.paymentClient = paymentClient;
	}

	public Payment executePayment(String paymentKey, Long orderNo, BigDecimal amount, Long productNo, Integer count) {
		orderService.prepareOrderForPayment(orderNo, amount);

		PaymentResult paymentResult;
		try {
			paymentResult = paymentClient.approve(paymentKey, orderNo, amount);
		} catch (Exception e) {
			orderService.rollbackToPending(orderNo);
			throw e;
		}

		switch (paymentResult.status()) {
			case DONE -> {
				return paymentService.completePayment(orderNo, paymentResult);
			}
			case ABORTED -> {
				paymentService.registerFailedPayment(orderNo, paymentResult);
				orderService.rollbackToPending(orderNo);
				throw PaymentFailedException.aborted(paymentKey, orderNo, paymentResult.failureMessage());
			}
			case EXPIRED -> {
				paymentService.registerFailedPayment(orderNo, paymentResult);
				purchaseOrchestrator.cancel(orderNo, productNo, count);
				throw PaymentFailedException.expired(paymentKey, orderNo, paymentResult.failureMessage());
			}
			default -> throw PaymentFailedException.invalid(paymentKey, orderNo, paymentResult.status().name());
		}
	}
}
