package com.kodesalon.kopang.domain.order;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import java.math.BigDecimal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class OrderProductTest {

	@DisplayName("주문 상품 create 를 호출하면 OrderProduct 를 성공적으로 생성하고, 상품 가격과 상품 갯수에 따른 주문 가격이 결정된다")
	@Test
	void create() {
		long productNo = 1L;
		int count = 2;
		BigDecimal productPrice = BigDecimal.valueOf(1000);

		OrderProduct orderProduct = OrderProduct.create(productNo, count, productPrice);

		Money expectedPrice = new Money(BigDecimal.valueOf(count * productPrice.doubleValue()));
		assertAll(
			() -> assertThat(orderProduct).isNotNull(),
			() -> assertThat(orderProduct.getProductNo()).isEqualTo(productNo),
			() -> assertThat(orderProduct.getCount()).isEqualTo(count),
			() -> assertThat(orderProduct.getOrderPrice()).isEqualTo(expectedPrice)
		);
	}
}