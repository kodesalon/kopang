package com.kodesalon.kopang.service.payment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.kodesalon.kopang.domain.order.Order;
import com.kodesalon.kopang.domain.order.OrderProduct;
import com.kodesalon.kopang.domain.payment.PaymentClient;
import com.kodesalon.kopang.domain.payment.PaymentResult;
import com.kodesalon.kopang.service.purchase.PurchaseOrchestrator;

@Component
public class PaymentRecoveryOrchestrator {

	private static final Logger log = LoggerFactory.getLogger(PaymentRecoveryOrchestrator.class);

	private final PaymentService paymentService;
	private final PurchaseOrchestrator purchaseOrchestrator;
	private final PaymentClient paymentClient;

	public PaymentRecoveryOrchestrator(
		PaymentService paymentService,
		PurchaseOrchestrator purchaseOrchestrator,
		PaymentClient paymentClient
	) {
		this.paymentService = paymentService;
		this.purchaseOrchestrator = purchaseOrchestrator;
		this.paymentClient = paymentClient;
	}

	public void recover(Order order) {
		Long orderNo = order.getNo();
		try {
			PaymentResult paymentResult = paymentClient.retrieveByOrder(orderNo);

			switch (paymentResult.status()) {
				case DONE -> paymentService.completePayment(orderNo, paymentResult);
				case ABORTED, EXPIRED -> {
					OrderProduct eventProduct = order.getEventProduct();
					paymentService.registerFailedPayment(orderNo, paymentResult);
					purchaseOrchestrator.cancel(orderNo, eventProduct.getProductNo(), eventProduct.getCount());
				}
				default -> log.error("Order [{}]: Unknown Status ({}) from PG", orderNo, paymentResult.status());
			}
		} catch (Exception e) {
			log.warn("Reconcile order [{}] 실패. 다음 cycle 에 재시도됩니다. Error: {}", orderNo, e.getMessage());
		}
	}
}
