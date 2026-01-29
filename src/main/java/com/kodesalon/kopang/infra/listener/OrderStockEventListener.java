package com.kodesalon.kopang.infra.listener;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.kodesalon.kopang.domain.order.event.OrderStockEvent;
import com.kodesalon.kopang.storage.order.OrderStockEventJpaEntity;
import com.kodesalon.kopang.storage.order.OrderStockEventJpaRepository;

@Component
public class OrderStockEventListener {

	private final OrderStockEventJpaRepository orderStockEventJpaRepository;

	public OrderStockEventListener(OrderStockEventJpaRepository orderStockEventJpaRepository) {
		this.orderStockEventJpaRepository = orderStockEventJpaRepository;
	}

	@TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
	public void handleOrderStockEvent(OrderStockEvent event) {
		orderStockEventJpaRepository.save(
			OrderStockEventJpaEntity.builder()
				.id(event.id())
				.orderNo(event.orderNo())
				.productNo(event.productNo())
				.warehouseNo(event.warehouseNo())
				.count(event.count())
				.eventType(OrderStockEventJpaEntity.EventType.DECREASE)
				.reason("")
				.requestedBy("MEMBER")
				.build()
		);
	}
}
