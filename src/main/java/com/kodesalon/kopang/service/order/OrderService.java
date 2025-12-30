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
		Order preparedOrder = findOrder(orderNo).preparePayment(new Money(amount));
		orderRepository.updateStatusToInProgress(preparedOrder); // 스케줄러와의 경합 시 처리 로직 필요
	}

	@Transactional
	public void rollbackToPending(Long orderNo) {
		Order order = findOrder(orderNo).rollbackToPending();
		orderRepository.updateStatusToPending(order); // 스케줄러와의 경합 시 처리 로직 필요
	}

	@Transactional
	public void pay(Long orderNo) {
		Order order = findOrder(orderNo).pay();
		orderRepository.updateOrder(order); // 스케줄러와의 경합 시 처리 로직 필요
	}

	@Transactional
	public void cancelOrder(Long orderNo) {
		Order cancelledOrder = findOrder(orderNo).cancel();
		orderRepository.updateStatusCancel(cancelledOrder); // 스케줄러와의 경합 시 처리 로직 필요
	}

	@Transactional(readOnly = true)
	public List<Order> findExpiredOrders(LocalDateTime now) {
		LocalDateTime cutoffTime = Order.calculateCutoffTime(now);
		return orderRepository.findExpiredOrders(cutoffTime);
	}

	private Order findOrder(Long orderNo) {
		return orderRepository.findByOrderNo(orderNo)
			.orElseThrow(() -> NotFoundException.order(orderNo));
	}
}
