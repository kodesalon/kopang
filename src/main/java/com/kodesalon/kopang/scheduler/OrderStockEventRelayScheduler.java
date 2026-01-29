package com.kodesalon.kopang.scheduler;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.kodesalon.kopang.infra.messaging.kafka.MockKafkaMessageProducer;
import com.kodesalon.kopang.storage.order.OrderStockEventJpaEntity;
import com.kodesalon.kopang.storage.order.OrderStockEventJpaRepository;

@Component
public class OrderStockEventRelayScheduler {

	private final OrderStockEventJpaRepository orderStockEventJpaRepository;
	private final MockKafkaMessageProducer mockKafkaMessageProducer;

	public OrderStockEventRelayScheduler(
		OrderStockEventJpaRepository orderStockEventJpaRepository,
		MockKafkaMessageProducer mockKafkaMessageProducer
	) {
		this.orderStockEventJpaRepository = orderStockEventJpaRepository;
		this.mockKafkaMessageProducer = mockKafkaMessageProducer;
	}

	@Scheduled(fixedDelay = 60_000, initialDelay = 300_000)
	public void replayFailedEvents() {
		List<OrderStockEventJpaEntity> eventJpaEntities = orderStockEventJpaRepository
			.findAllByPublishedFalseAndCreatedAtBefore(LocalDateTime.now().minusMinutes(5));
		for (OrderStockEventJpaEntity eventJpaEntity : eventJpaEntities) {
			// eventJpaEntity -> payload
			mockKafkaMessageProducer.produce();
		}
	}
}
