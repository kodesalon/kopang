package com.kodesalon.kopang.domain.stock;

public class Stock {

	private static final Integer ZERO = 0;

	private final Long no;
	private final Long productNo;
	private final Integer quantity;

	public Stock(Long no, Long productNo, Integer quantity) {
		this.no = no;
		this.productNo = productNo;
		this.quantity = quantity;
	}

	public static Stock of(Long productNo, Integer quantity) {
		if (quantity < ZERO) {
			throw new IllegalStateException("재고 수량은 0보다 작을 수 없습니다");
		}
		return new Stock(null, productNo, quantity);
	}

	public Stock decrease(Integer count) {
		checkAvailable();
		int remainQuantity = quantity - count;
		if (remainQuantity < ZERO) {
			throw new IllegalStateException("재고 수량은 0보다 작을 수 없습니다");
		}
		return new Stock(no, productNo, remainQuantity);
	}

	public Stock increase(Integer count) {
		return new Stock(no, productNo, quantity + count);
	}

	public void checkAvailable() {
		if (quantity.equals(ZERO)) {
			throw new IllegalStateException("남아있는 재고 수량이 없습니다");
		}
	}

	public Long getProductNo() {
		return productNo;
	}

	public Integer getQuantity() {
		return quantity;
	}
}
