package com.kodesalon.kopang.infra.event;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import com.kodesalon.kopang.domain.order.event.OrderStockEvent;
import com.kodesalon.kopang.domain.order.event.OrderStockEventPublisher;

@Component
public class OrderStockEventPublisherAdapter implements OrderStockEventPublisher {

	private final ApplicationEventPublisher applicationEventPublisher;

	public OrderStockEventPublisherAdapter(ApplicationEventPublisher applicationEventPublisher) {
		this.applicationEventPublisher = applicationEventPublisher;
	}

	@Override
	public void createOrderPending(OrderStockEvent event) {
		applicationEventPublisher.publishEvent(event);
	}
}
