package com.kodesalon.kopang.storage.order;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.kodesalon.kopang.domain.order.Order;
import com.kodesalon.kopang.domain.order.OrderProduct;
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
@Table(name = "orders")
@EntityListeners(AuditingEntityListener.class)
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

	@CreationTimestamp
	private LocalDateTime orderedAt;

	@UpdateTimestamp
	private LocalDateTime updatedAt;

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

	public Order toDomain() {
		List<OrderProduct> products = orderProducts.stream()
			.map(OrderProductJpaEntity::toDomain)
			.toList();
		return Order.of(no, memberNo, status, products);
	}
}