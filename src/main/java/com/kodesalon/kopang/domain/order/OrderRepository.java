package com.kodesalon.kopang.domain.order;

import java.util.Optional;

public interface OrderRepository {

	Order register(Order order);

	Optional<Order> findByOrderNo(Long orderNo);

	void updateOrder(Order order);

	int updateStatusToInProgress(Order order);

	int updateStatusToPending(Order order);

	int updateStatusCancel(Order order);
}
