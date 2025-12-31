package com.kodesalon.kopang.domain.stock;

public class StockQuantity {

	private static final int MIN_QUANTITY = 0;

	private final Integer value;

	public StockQuantity decrease(Integer count) {
		return StockQuantity.from(value - count);
	}

	public StockQuantity increase(Integer count) {
		return StockQuantity.from(value + count);
	}

	public boolean isSoldOut() {
		return value == MIN_QUANTITY;
	}

	public static StockQuantity from(Integer value) {
		if (value < MIN_QUANTITY) {
			throw new IllegalArgumentException("재고 수량은 0보다 작을 수 없습니다.");
		}
		return new StockQuantity(value);
	}

	private StockQuantity(Integer value) {
		this.value = value;
	}

	public Integer getValue() {
		return value;
	}

	@Override
	public boolean equals(Object o) {
		if (o == null || getClass() != o.getClass())
			return false;

		StockQuantity that = (StockQuantity)o;
		return value.equals(that.value);
	}

	@Override
	public int hashCode() {
		return value.hashCode();
	}
}
