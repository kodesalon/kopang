package com.kodesalon.kopang.scheduler;

import java.time.LocalDateTime;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.kodesalon.kopang.domain.order.Orders;
import com.kodesalon.kopang.service.order.OrderService;
import com.kodesalon.kopang.service.purchase.PurchaseOrchestrator;

@Component
public class OrderAutoCancelScheduler {

	private final OrderService orderService;
	private final PurchaseOrchestrator purchaseOrchestrator;

	public OrderAutoCancelScheduler(OrderService orderService, PurchaseOrchestrator purchaseOrchestrator) {
		this.orderService = orderService;
		this.purchaseOrchestrator = purchaseOrchestrator;
	}

	@Scheduled(fixedDelay = 30_000)
	public void autoCancelExpiredPendingOrders() {
		while (true) {
			Orders expiredOrders = orderService.findExpiredPendingOrders(LocalDateTime.now());
			if (expiredOrders.isEmpty()) {
				break;
			}
			purchaseOrchestrator.cancelInBatch(expiredOrders);
		}
	}
}
