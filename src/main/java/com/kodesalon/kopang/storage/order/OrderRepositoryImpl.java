package com.kodesalon.kopang.storage.order;

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
import com.kodesalon.kopang.domain.order.OrderStatus;

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
	public boolean updateStatus(Long orderNo, OrderStatus expected, OrderStatus next) {
		return orderJpaRepository.updateStatus(orderNo, expected, next) == 1;
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
}
