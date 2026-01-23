package com.kodesalon.kopang.storage.order;

import static com.kodesalon.kopang.domain.order.OrderStatus.CANCELLED;
import static com.kodesalon.kopang.domain.order.OrderStatus.PAYMENT_IN_PROGRESS;
import static com.kodesalon.kopang.domain.order.OrderStatus.PENDING;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.kodesalon.kopang.domain.order.Order;
import com.kodesalon.kopang.domain.order.OrderRepository;

@Repository
public class OrderRepositoryImpl implements OrderRepository {

	private final OrderJpaRepository orderJpaRepository;

	public OrderRepositoryImpl(OrderJpaRepository orderJpaRepository) {
		this.orderJpaRepository = orderJpaRepository;
	}

	@Override
	public Order register(Order order) {
		return orderJpaRepository.save(OrderJpaEntity.from(order)).toDomain();
	}

	@Override
	public Optional<Order> findByOrderNo(Long orderNo) {
		return Optional.ofNullable(orderJpaRepository.findByNo(orderNo))
			.map(OrderJpaEntity::toDomain);
	}

	@Override
	public void updateOrder(Order order) {
		orderJpaRepository.updateOrder(order.getNo(), order.getStatus());
	}

	@Override
	public List<Order> findExpiredPendingOrders(LocalDateTime cutoffTime) {
		Pageable limit = PageRequest.of(0, 1000);
		return orderJpaRepository
			.findExpiredOrders(PENDING, cutoffTime, limit)
			.stream()
			.map(OrderJpaEntity::toDomain)
			.toList();
	}

	@Override
	public List<Order> findExpiredInProgressOrders(LocalDateTime cutoffTime) {
		Pageable limit = PageRequest.of(0, 100);
		return orderJpaRepository
			.findExpiredOrders(PAYMENT_IN_PROGRESS, cutoffTime, limit)
			.stream()
			.map(OrderJpaEntity::toDomain)
			.toList();
	}

	@Override
	public void updateStatusToCancelInBatch(List<Long> expiredNos) {
		orderJpaRepository.updateStatusToCancelInBatch(expiredNos, CANCELLED, List.of(PENDING, PAYMENT_IN_PROGRESS));
	}
}
