package com.kodesalon.kopang.storage.stock;

import com.kodesalon.kopang.domain.stock.Stock;
import com.kodesalon.kopang.domain.stock.StockQuantity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "stock")
public class StockJpaEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long no;

	@Column(nullable = false)
	private Long productNo;

	@Column(nullable = false)
	private Integer quantity;

	protected StockJpaEntity() {
	}

	public StockJpaEntity(Long productNo, Integer quantity) {
		this.productNo = productNo;
		this.quantity = quantity;
	}

	public Stock toDomain() {
		return new Stock(no, productNo, StockQuantity.from(quantity));
	}

	public Long getNo() {
		return no;
	}

	public Long getProductNo() {
		return productNo;
	}

	public Integer getQuantity() {
		return quantity;
	}
}
