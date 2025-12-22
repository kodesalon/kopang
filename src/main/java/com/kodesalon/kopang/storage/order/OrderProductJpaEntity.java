package com.kodesalon.kopang.storage.order;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import com.kodesalon.kopang.domain.order.OrderProduct;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "order_product")
@SQLDelete(sql = "UPDATE order_product SET deleted_at = NOW() WHERE no = ?")
@SQLRestriction(value = "deleted_at IS NULL")
public class OrderProductJpaEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long no;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "order_no", nullable = false)
	private OrderJpaEntity order;

	@Column(nullable = false)
	private Long productNo;

	@Column(nullable = false)
	private BigDecimal orderPrice;

	@Column(nullable = false)
	private Integer count;

	private LocalDateTime deletedAt;

	protected OrderProductJpaEntity() {
	}

	public OrderProductJpaEntity(OrderJpaEntity order, Long productNo, BigDecimal orderPrice, Integer count) {
		this.order = order;
		this.productNo = productNo;
		this.orderPrice = orderPrice;
		this.count = count;
	}

	public static OrderProductJpaEntity of(OrderJpaEntity orderJpaEntity, OrderProduct orderProduct) {
		return new OrderProductJpaEntity(
			orderJpaEntity,
			orderProduct.getProductNo(),
			orderProduct.getOrderPrice().getAmount(),
			orderProduct.getCount()
		);
	}

	public OrderProduct toDomain() {
		return OrderProduct.of(no, productNo, count, orderPrice);
	}
}