package com.kodesalon.kopang.scheduler;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.kodesalon.kopang.domain.order.Order;
import com.kodesalon.kopang.service.order.OrderService;
import com.kodesalon.kopang.service.payment.PaymentRecoveryOrchestrator;

@Component
public class OrderReconcileScheduler {

	private final OrderService orderService;
	private final PaymentRecoveryOrchestrator paymentRecoveryOrchestrator;

	public OrderReconcileScheduler(OrderService orderService, PaymentRecoveryOrchestrator paymentRecoveryOrchestrator) {
		this.orderService = orderService;
		this.paymentRecoveryOrchestrator = paymentRecoveryOrchestrator;
	}

	@Scheduled(fixedDelay = 60_000)
	public void reconcileStuckPaymentOrders() {
		while (true) {
			List<Order> expiredOrders = orderService.findExpiredInProgressOrders(LocalDateTime.now());
			if (expiredOrders.isEmpty()) {
				break;
			}
			expiredOrders.forEach(paymentRecoveryOrchestrator::recover);
		}
	}
}
