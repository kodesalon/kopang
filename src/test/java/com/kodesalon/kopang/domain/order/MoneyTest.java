package com.kodesalon.kopang.domain.order;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MoneyTest {

	@DisplayName("기존 금액이 0원일 때 합산 결과를 검증한다")
	@Test
	void plus() {
		Money zero = Money.ZERO;
		Money expected = new Money(1000L);

		Money actual = zero.plus(expected);

		assertEquals(expected, actual);
	}
}