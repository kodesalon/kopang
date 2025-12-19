package com.kodesalon.kopang.storage.product;

import java.math.BigDecimal;

import com.kodesalon.kopang.domain.product.Product;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "product")
public class ProductJpaEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long no;

	@Column(nullable = false)
	private String name;

	@Column(nullable = false)
	private String description;

	@Column(nullable = false)
	private BigDecimal price;

	protected ProductJpaEntity() {
	}

	public ProductJpaEntity(String name, String description, BigDecimal price) {
		this.name = name;
		this.description = description;
		this.price = price;
	}

	public Product toDomain() {
		return new Product(no, name, description, price);
	}

	public Long getNo() {
		return no;
	}

	public String getName() {
		return name;
	}

	public String getDescription() {
		return description;
	}

	public BigDecimal getPrice() {
		return price;
	}
}
