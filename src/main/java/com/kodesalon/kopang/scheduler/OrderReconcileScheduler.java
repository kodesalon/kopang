package com.kodesalon.kopang.scheduler;

import java.time.LocalDateTime;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.kodesalon.kopang.domain.order.Orders;
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

	/**
	 * 한 번 실행에 한 번만 조회한다. PG 조회에 실패한 주문은 결제 중 상태로 남으므로,
	 * 다시 조회하면 같은 주문을 끝없이 조회하며 스케줄러 스레드를 붙잡는다. 실패한 주문은 다음 주기에 다시 시도한다.
	 */
	@Scheduled(fixedDelay = 60_000)
	public void reconcileStuckPaymentOrders() {
		Orders expiredOrders = orderService.findExpiredInProgressOrders(LocalDateTime.now());
		expiredOrders.forEach(paymentRecoveryOrchestrator::recover);
	}
}
