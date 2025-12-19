package com.kodesalon.kopang.domain.order;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import java.math.BigDecimal;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class OrderTest {

	@DisplayName("주문 createPending 을 호출하면 결제 대기 상태의 Order 를 생성하고, 상품의 가격과 개수에 맞는 주문 상품 및 최종 가격이 결정된다")
	@Test
	void createPending() {
		Integer count = 1;
		BigDecimal productPrice = BigDecimal.valueOf(1000);

		Order order = Order.createPending(1L, 1L, count, productPrice);

		Money totalPrice = new Money(count.longValue() * productPrice.longValue());
		assertAll(
			() -> assertThat(order).isNotNull(),
			() -> assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING),
			() -> assertThat(order.getTotalPrice()).isEqualTo(totalPrice),
			() -> assertThat(order.getProducts()).hasSize(1)
		);
	}

	@DisplayName("주문 결제 테스트")
	@Nested
	class OrderPay {

		@DisplayName("주문 결제 호출 시 주문 상태가 PENDING 일 경우 결제가 완료된다")
		@Test
		void pay() {
			Order order = OrderFixture.PENDING_ORDER;

			Order paid = order.pay();

			assertAll(
				() -> assertThat(paid).isNotNull(),
				() -> assertThat(paid.getStatus()).isNotNull(),
				() -> assertThat(paid.getStatus()).isEqualTo(OrderStatus.PAID)
			);
		}

		@DisplayName("주문 결제 호출 시 주문 상태가 CANCELED 거나 PAID 이면 예외가 발생한다")
		@ParameterizedTest
		@MethodSource("invalidOrdersForPayment")
		void pay_fail(Order order) {
			assertThatThrownBy(order::pay)
				.isInstanceOf(IllegalStateException.class);
		}

		private static Stream<Order> invalidOrdersForPayment() {
			return Stream.of(
				OrderFixture.CANCELLED_ORDER,
				OrderFixture.PAID_ORDER
			);
		}
	}

	@DisplayName("주문 취소 테스트")
	@Nested
	class OrderCancel {

		@DisplayName("주문 취소 호출 시 주문 상태가 PENDING 일 경우 주문 취소가 완료된다")
		@Test
		void cancel() {
			Order order = OrderFixture.PENDING_ORDER;

			Order cancelled = order.cancel();

			assertAll(
				() -> assertThat(cancelled).isNotNull(),
				() -> assertThat(cancelled.getStatus()).isNotNull(),
				() -> assertThat(cancelled.getStatus()).isEqualTo(OrderStatus.CANCELLED)
			);
		}

		@DisplayName("주문 취소 호출 시 주문 상태가 CANCELED 거나 PAID 이면 예외가 발생한다")
		@ParameterizedTest
		@MethodSource("invalidOrdersForCancel")
		void cancel_fail(Order order) {
			assertThatThrownBy(order::pay)
				.isInstanceOf(IllegalStateException.class);
		}

		private static Stream<Order> invalidOrdersForCancel() {
			return Stream.of(
				OrderFixture.CANCELLED_ORDER,
				OrderFixture.PAID_ORDER
			);
		}
	}
}