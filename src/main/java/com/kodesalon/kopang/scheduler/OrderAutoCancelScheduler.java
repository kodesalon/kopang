package com.kodesalon.kopang.scheduler;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.kodesalon.kopang.domain.order.Order;
import com.kodesalon.kopang.service.order.OrderService;

@Component
public class OrderAutoCancelScheduler {

	private final OrderService orderService;

	public OrderAutoCancelScheduler(OrderService orderService) {
		this.orderService = orderService;
	}

	@Scheduled(fixedDelay = 60_000)
	public void autoCancelOrders() {
		List<Order> expiredOrders = orderService.findExpiredOrders(LocalDateTime.now());

		for (Order expiredOrder : expiredOrders) {
			orderService.cancelOrder(expiredOrder.getNo());
		}
	}
}
