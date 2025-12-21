package com.kodesalon.kopang.storage.order;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import com.kodesalon.kopang.domain.order.Order;
import com.kodesalon.kopang.domain.order.OrderFixture;
import com.kodesalon.kopang.domain.order.OrderRepository;

@DataJpaTest
@Import(OrderRepositoryImpl.class)
class OrderRepositoryImplTest {

	private @Autowired OrderRepository orderRepository;
	private @Autowired OrderJpaRepository orderJpaRepository;

	@DisplayName("Order 도메인 객체를 저장하면 JpaEntity로 변환되어 저장되고, 자식 엔티티까지 함께 저장된다")
	@Test
	void register_success() {
		Order order = OrderFixture.PENDING_ORDER;

		orderRepository.register(order);

		List<OrderJpaEntity> savedOrders = orderJpaRepository.findAll();
		assertThat(savedOrders).hasSize(1);
		OrderJpaEntity orderJpaEntity = savedOrders.getFirst();
		assertThat(orderJpaEntity.getOrderProducts()).hasSize(1);
	}
}