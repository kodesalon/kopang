package com.kodesalon.kopang.domain.order;

import java.math.BigDecimal;
import java.util.Objects;

public class Money {

	static final Money ZERO = new Money(BigDecimal.ZERO);

	private final BigDecimal amount;

	public Money(BigDecimal amount) {
		this.amount = amount;
	}

	public Money(Long amount) {
		this.amount = new BigDecimal(amount);
	}

	public Money plus(Money money) {
		return new Money(amount.add(money.amount));
	}

	@Override
	public boolean equals(Object o) {
		if (o == null || getClass() != o.getClass())
			return false;
		Money money = (Money)o;
		return Objects.equals(amount, money.amount);
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(amount);
	}
}
