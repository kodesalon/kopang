package com.kodesalon.kopang.storage.order;

import java.util.Optional;

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
	public int updateStatusToInProgress(Order order) {
		return orderJpaRepository.updateStatusToInProgress(order.getNo(), order.getStatus());
	}

	@Override
	public int updateStatusToPending(Order order) {
		return orderJpaRepository.updateStatusToPending(order.getNo(), order.getStatus());
	}

	@Override
	public int updateStatusCancel(Order order) {
		return orderJpaRepository.updateStatusCancel(order.getNo(), order.getStatus());
	}
}
