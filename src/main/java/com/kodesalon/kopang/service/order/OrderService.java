package com.kodesalon.kopang.service.order;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kodesalon.kopang.domain.order.Money;
import com.kodesalon.kopang.domain.order.Order;
import com.kodesalon.kopang.domain.order.OrderRepository;
import com.kodesalon.kopang.domain.order.OrderStatus;
import com.kodesalon.kopang.domain.order.event.OrderStockEvent;
import com.kodesalon.kopang.domain.order.event.OrderStockEventPublisher;
import com.kodesalon.kopang.domain.order.Orders;
import com.kodesalon.kopang.service.exception.NotFoundException;
import com.kodesalon.kopang.service.exception.OrderStatusConflictException;

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
		Order order = findOrder(orderNo);
		changeStatus(order, order.preparePayment(new Money(amount), LocalDateTime.now()));
	}

	@Transactional
	public void rollbackToPending(Long orderNo) {
		Order order = findOrder(orderNo);
		changeStatus(order, order.rollbackToPending());
	}

	@Transactional
	public void pay(Long orderNo) {
		Order order = findOrder(orderNo);
		changeStatus(order, order.pay());
	}

	@Transactional
	public Order cancelOrder(Long orderNo) {
		Order order = findOrder(orderNo);
		Order cancelledOrder = order.cancel();
		changeStatus(order, cancelledOrder);
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

	/**
	 * 조회 뒤 그사이 결제를 시작하지 않아 아직 PENDING 인 주문만 취소하고, 실제로 취소한 주문을 돌려준다.
	 */
	@Transactional
	public Orders cancelExpiredPendingOrders(Orders expiredOrders) {
		List<Order> cancelled = new ArrayList<>();
		for (Order order : expiredOrders) {
			if (orderRepository.updateStatus(order.getNo(), OrderStatus.PENDING, OrderStatus.CANCELLED)) {
				cancelled.add(order);
			}
		}
		return new Orders(cancelled);
	}

	private Order findOrder(Long orderNo) {
		return orderRepository.findByOrderNo(orderNo)
			.orElseThrow(() -> NotFoundException.order(orderNo));
	}

	private void changeStatus(Order before, Order after) {
		if (!orderRepository.updateStatus(before.getNo(), before.getStatus(), after.getStatus())) {
			throw OrderStatusConflictException.of(before.getNo(), before.getStatus(), after.getStatus());
		}
	}
}
