package com.kodesalon.kopang.domain.order.event;

public interface OrderStockEventPublisher {
	void createOrderPending(OrderStockEvent event);
}
