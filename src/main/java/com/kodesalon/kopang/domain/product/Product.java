package com.kodesalon.kopang.domain.product;

import java.math.BigDecimal;

public class Product {

	private final Long no;
	private final String name;
	private final String description;
	private final BigDecimal price;

	public Product(Long no, String name, String description, BigDecimal price) {
		this.no = no;
		this.name = name;
		this.description = description;
		this.price = price;
	}

	public BigDecimal getPrice() {
		return price;
	}
}
