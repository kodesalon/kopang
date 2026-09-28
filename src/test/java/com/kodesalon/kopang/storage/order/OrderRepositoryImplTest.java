package com.kodesalon.kopang.storage.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

import com.kodesalon.kopang.config.JpaAuditingConfig;
import com.kodesalon.kopang.domain.order.Order;
import com.kodesalon.kopang.domain.order.OrderFixture;
import com.kodesalon.kopang.domain.order.OrderRepository;
import com.kodesalon.kopang.domain.order.OrderStatus;

@DataJpaTest
@Import({OrderRepositoryImpl.class, JpaAuditingConfig.class})
class OrderRepositoryImplTest {

	private @Autowired OrderRepository orderRepository;
	private @Autowired OrderJpaRepository orderJpaRepository;
	private @Autowired TestEntityManager entityManager;

	@DisplayName("Order 도메인 객체를 저장하면 JpaEntity로 변환되어 저장되고, 자식 엔티티까지 함께 저장된다")
	@Test
	void register_success() {
		// Order order = OrderFixture.PENDING_ORDER;
		//
		// orderRepository.register(order);
		//
		// List<OrderJpaEntity> savedOrders = orderJpaRepository.findAll();
		// assertThat(savedOrders).hasSize(1);
		// OrderJpaEntity orderJpaEntity = savedOrders.getFirst();
		// assertThat(orderJpaEntity.getOrderProducts()).hasSize(1);
	}

	@DisplayName("주문을 저장할 때, JPA Auditing 이 켜져 있으면, 주문 시각이 채워진다")
	@Test
	void register_JpaAuditingEnabled_SetsOrderedAt() {
		// given
		Order order = Order.createPending(1L, 1L, 1L, 1, BigDecimal.valueOf(1000));

		// when
		Order registered = orderRepository.register(order);

		// then
		Object orderedAt = entityManager.getEntityManager()
			.createNativeQuery("SELECT ordered_at FROM orders WHERE no = ?")
			.setParameter(1, registered.getNo())
			.getSingleResult();
		assertThat(orderedAt).isNotNull();
	}

	@DisplayName("상태 변경")
	@Nested
	class UpdateStatus {

		@DisplayName("결제 대기 주문이 주어질 때, 결제 대기일 때만 결제 진행 중으로 바꾸도록 요청하면, 상태가 바뀌고 true 를 돌려준다")
		@Test
		void updateStatus_ExpectedStatusMatches_ChangesStatusAndReturnsTrue() {
			// given
			Order order = orderRepository.register(Order.createPending(1L, 1L, 1L, 1, BigDecimal.valueOf(1000)));

			// when
			boolean updated = orderRepository.updateStatus(order.getNo(), OrderStatus.PENDING, OrderStatus.PAYMENT_IN_PROGRESS);

			// then
			assertAll(
				() -> assertThat(updated).isTrue(),
				() -> assertThat(statusOf(order)).isEqualTo(OrderStatus.PAYMENT_IN_PROGRESS)
			);
		}

		@DisplayName("만료 취소가 먼저 반영된 주문이 주어질 때, 결제 대기라고 읽어 둔 늦은 요청이 상태를 바꾸려 하면, 바뀌지 않고 false 를 돌려준다")
		@Test
		void updateStatus_StatusChangedByScheduler_KeepsCancelledAndReturnsFalse() {
			// given — 사용자 요청이 PENDING 을 읽은 뒤, 스케줄러가 먼저 취소했다
			Order order = orderRepository.register(Order.createPending(1L, 1L, 1L, 1, BigDecimal.valueOf(1000)));
			orderRepository.updateStatus(order.getNo(), OrderStatus.PENDING, OrderStatus.CANCELLED);

			// when
			boolean updated = orderRepository.updateStatus(order.getNo(), OrderStatus.PENDING, OrderStatus.PAYMENT_IN_PROGRESS);

			// then
			assertAll(
				() -> assertThat(updated).isFalse(),
				() -> assertThat(statusOf(order)).isEqualTo(OrderStatus.CANCELLED)
			);
		}

		private OrderStatus statusOf(Order order) {
			return orderRepository.findByOrderNo(order.getNo()).orElseThrow().getStatus();
		}
	}
}