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
	public void register(Order order) {
		orderJpaRepository.save(OrderJpaEntity.from(order));
	}
}
