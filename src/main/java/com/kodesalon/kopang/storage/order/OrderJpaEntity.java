package com.kodesalon.kopang.storage.order;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.kodesalon.kopang.domain.order.Order;
import com.kodesalon.kopang.domain.order.OrderStatus;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "order")
@EntityListeners(AuditingEntityListener.class)
@SQLDelete(sql = "UPDATE orders SET deleted_at = NOW() WHERE no = ?")
@SQLRestriction(value = "deleted_at IS NULL")
public class OrderJpaEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long no;

	@Column(nullable = false)
	private Long memberNo;

	@Column(nullable = false)
	private BigDecimal totalPrice;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private OrderStatus status;

	@CreatedDate
	private LocalDateTime orderedAt;

	@LastModifiedDate
	private LocalDateTime updatedAt;

	private LocalDateTime deletedAt;

	@OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
	private final List<OrderProductJpaEntity> orderProducts = new ArrayList<>();

	protected OrderJpaEntity() {
	}

	public OrderJpaEntity(Long memberNo, BigDecimal totalPrice, OrderStatus status) {
		this.memberNo = memberNo;
		this.totalPrice = totalPrice;
		this.status = status;
	}

	public static OrderJpaEntity from(Order order) {
		OrderJpaEntity orderJpaEntity = new OrderJpaEntity(
			order.getMemberNo(),
			order.getTotalPrice().getAmount(),
			order.getStatus()
		);
		List<OrderProductJpaEntity> orderProductJpaEntities = order.getProducts()
			.stream()
			.map(orderProduct -> OrderProductJpaEntity.of(orderJpaEntity, orderProduct))
			.toList();
		orderJpaEntity.addOrderProduct(orderProductJpaEntities);
		return orderJpaEntity;
	}

	public void addOrderProduct(List<OrderProductJpaEntity> orderProducts) {
		this.orderProducts.addAll(orderProducts);
	}
}