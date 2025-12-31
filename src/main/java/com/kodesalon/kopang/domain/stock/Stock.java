package com.kodesalon.kopang.domain.stock;

public class Stock {

	private final Long no;
	private final Long productNo;
	private final StockQuantity quantity;

	public Stock(Long no, Long productNo, StockQuantity quantity) {
		this.no = no;
		this.productNo = productNo;
		this.quantity = quantity;
	}

	public Stock decrease(Integer count) {
		checkAvailable();
		return new Stock(no, productNo, quantity.decrease(count));
	}

	public Stock increase(Integer count) {
		return new Stock(no, productNo, quantity.increase(count));
	}

	public void checkAvailable() {
		if (quantity.isSoldOut()) {
			throw new IllegalStateException("남아있는 재고 수량이 없습니다");
		}
	}

	public Long getProductNo() {
		return productNo;
	}

	public Integer getQuantity() {
		return quantity.getValue();
	}
}
