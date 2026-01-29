package com.kodesalon.kopang.service.order;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kodesalon.kopang.domain.order.Money;
import com.kodesalon.kopang.domain.order.Order;
import com.kodesalon.kopang.domain.order.OrderRepository;
import com.kodesalon.kopang.domain.order.event.OrderStockEvent;
import com.kodesalon.kopang.domain.order.event.OrderStockEventPublisher;
import com.kodesalon.kopang.domain.order.Orders;
import com.kodesalon.kopang.service.exception.NotFoundException;

@Service
public class OrderService {

	private final OrderRepository orderRepository;
	private final OrderStockEventPublisher eventPublisher;

	public OrderService(OrderRepository orderRepository, OrderStockEventPublisher eventPublisher) {
		this.orderRepository = orderRepository;
		this.eventPublisher = eventPublisher;
	}

	@Transactional
	public Order createOrderPending(Long memberNo, Long productNo, Long warehouseNo, Integer count, BigDecimal productPrice) {
		Order order = orderRepository.register(Order.createPending(memberNo, productNo, warehouseNo, count, productPrice));
		eventPublisher.createOrderPending(OrderStockEvent.create(order.getNo(), productNo, warehouseNo, count));
		return order;
	}

	@Transactional
	public void prepareOrderForPayment(Long orderNo, BigDecimal amount) {
		Order preparedOrder = findOrder(orderNo).preparePayment(new Money(amount), LocalDateTime.now());
		orderRepository.updateOrder(preparedOrder);
	}

	@Transactional
	public void rollbackToPending(Long orderNo) {
		Order order = findOrder(orderNo).rollbackToPending();
		orderRepository.updateOrder(order);
	}

	@Transactional
	public void pay(Long orderNo) {
		Order order = findOrder(orderNo).pay();
		orderRepository.updateOrder(order);
	}

	@Transactional
	public Order cancelOrder(Long orderNo) {
		Order cancelledOrder = findOrder(orderNo).cancel();
		orderRepository.updateOrder(cancelledOrder);
		return cancelledOrder;
	}

	@Transactional(readOnly = true)
	public Orders findExpiredPendingOrders(LocalDateTime now) {
		LocalDateTime pendingCutoffTime = Order.calculatePendingCutoffTime(now);
		return new Orders(orderRepository.findExpiredPendingOrders(pendingCutoffTime));
	}

	@Transactional(readOnly = true)
	public Orders findExpiredInProgressOrders(LocalDateTime now) {
		LocalDateTime inProgressCutoffTime = Order.calculateInProgressCutoffTime(now);
		return new Orders(orderRepository.findExpiredInProgressOrders(inProgressCutoffTime));
	}

	@Transactional
	public void cancelExpiredOrders(List<Long> expiredNos) {
		orderRepository.updateStatusToCancelInBatch(expiredNos);
	}

	private Order findOrder(Long orderNo) {
		return orderRepository.findByOrderNo(orderNo)
			.orElseThrow(() -> NotFoundException.order(orderNo));
	}
}
