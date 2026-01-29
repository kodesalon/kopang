package com.kodesalon.kopang.infra.listener;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.kodesalon.kopang.domain.order.event.OrderStockEvent;
import com.kodesalon.kopang.infra.messaging.kafka.MockKafkaMessageProducer;

@Component
public class OrderStockEventMessageListener {

	private final MockKafkaMessageProducer kafkaMessageProducer;

	public OrderStockEventMessageListener(MockKafkaMessageProducer kafkaMessageProducer) {
		this.kafkaMessageProducer = kafkaMessageProducer;
	}

	@Async("eventTaskExecutor")
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void handleOrderStockEvent(OrderStockEvent event) {
		// event -> kafka message payload
		kafkaMessageProducer.produce(); // payload
	}
}
