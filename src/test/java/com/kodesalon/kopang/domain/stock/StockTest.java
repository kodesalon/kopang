package com.kodesalon.kopang.domain.stock;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class StockTest {

	@DisplayName("재고의 수량 차감 요청 시, count 만큼 차감된다")
	@Test
	void decrease() {
		Stock stock = new Stock(1L, 1L, StockQuantity.from(100));

		Stock decreased = stock.decrease(1);

		assertAll(
			() -> assertThat(decreased).isNotNull(),
			() -> assertThat(decreased.getQuantity()).isEqualTo(99)
		);
	}

	@DisplayName("재고의 수량 차감 요청 수행 시, 재고가 0이거나 0보다 작으면 예외를 발생시킨다")
	@ParameterizedTest
	@CsvSource({"0, 1", "1, 2"})
	void decrease_fail(int quantity, int count) {
		Stock stock = new Stock(1L, 1L, StockQuantity.from(quantity));
		assertThatThrownBy(() -> stock.decrease(count))
			.isInstanceOf(IllegalStateException.class);
	}

	@DisplayName("재고의 수량 증가 요청 시, count 만큼 증가된다")
	@Test
	void increase() {
		Stock stock = new Stock(1L, 1L, StockQuantity.from(99));

		Stock increased = stock.increase(1);

		assertAll(
			() -> assertThat(increased).isNotNull(),
			() -> assertThat(increased.getQuantity()).isEqualTo(100)
		);
	}
}