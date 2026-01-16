package com.kodesalon.kopang.scheduler;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.kodesalon.kopang.domain.order.Order;
import com.kodesalon.kopang.service.order.OrderService;
import com.kodesalon.kopang.service.purchase.PurchaseFacade;

@Component
public class OrderAutoCancelScheduler {

	private final OrderService orderService;
	private final PurchaseFacade purchaseFacade;

	public OrderAutoCancelScheduler(OrderService orderService, PurchaseFacade purchaseFacade) {
		this.orderService = orderService;
		this.purchaseFacade = purchaseFacade;
	}

	@Scheduled(fixedDelay = 30_000)
	public void autoCancelExpiredPendingOrders() {
		while (true) {
			List<Order> expiredOrders = orderService.findExpiredPendingOrders(LocalDateTime.now());
			if (expiredOrders.isEmpty()) {
				break;
			}
			purchaseFacade.cancelInBatch(expiredOrders);
		}
	}
}
