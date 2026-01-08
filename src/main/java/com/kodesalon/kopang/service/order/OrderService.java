package com.kodesalon.kopang.service.order;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kodesalon.kopang.domain.order.Money;
import com.kodesalon.kopang.domain.order.Order;
import com.kodesalon.kopang.domain.order.OrderRepository;
import com.kodesalon.kopang.domain.product.Product;
import com.kodesalon.kopang.domain.product.ProductRepository;
import com.kodesalon.kopang.service.exception.NotFoundException;

@Service
public class OrderService {

	private final ProductRepository productRepository;
	private final OrderRepository orderRepository;

	public OrderService(ProductRepository productRepository, OrderRepository orderRepository) {
		this.productRepository = productRepository;
		this.orderRepository = orderRepository;
	}

	@Transactional
	public Order createOrderPending(Long memberNo, Long productNo, Integer count) {
		Product product = productRepository.findByProductNo(productNo)
			.orElseThrow(() -> NotFoundException.product(productNo));
		Order pendingOrder = Order.createPending(memberNo, productNo, count, product.getPrice());
		return orderRepository.register(pendingOrder);
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
	public void cancelOrder(Long orderNo) {
		Order cancelledOrder = findOrder(orderNo).cancel();
		orderRepository.updateOrder(cancelledOrder);
	}

	@Transactional(readOnly = true)
	public List<Order> findExpiredOrders(LocalDateTime now) {
		LocalDateTime pendingCutoffTime = Order.calculatePendingCutoffTime(now);
		LocalDateTime inProgressCutoffTime = Order.calculateInProgressCutoffTime(now);
		return orderRepository.findExpiredOrders(pendingCutoffTime, inProgressCutoffTime);
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
