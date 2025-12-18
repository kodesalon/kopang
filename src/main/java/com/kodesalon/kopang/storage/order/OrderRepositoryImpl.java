package com.kodesalon.kopang.storage.order;

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
}
