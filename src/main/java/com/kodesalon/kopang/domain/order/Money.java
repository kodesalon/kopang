package com.kodesalon.kopang.domain.order;

import java.math.BigDecimal;

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

	public BigDecimal getAmount() {
		return amount;
	}

	@Override
	public boolean equals(Object o) {
		if (o == null || getClass() != o.getClass())
			return false;
		Money money = (Money)o;
		return amount.compareTo(money.amount) == 0;
	}

	@Override
	public int hashCode() {
		return amount.stripTrailingZeros().hashCode();
	}
}
