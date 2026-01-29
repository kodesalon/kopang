package com.kodesalon.kopang.storage.order;

import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "order_stock_event")
@EntityListeners(AuditingEntityListener.class)
public class OrderStockEventJpaEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "event_id")
	private String id;

	@Column(nullable = false)
	private Long orderNo;

	@Column(nullable = false)
	@Enumerated(EnumType.STRING)
	private EventType eventType; // 재고 증가 or 재고 감소

	@Column(nullable = false)
	private Integer count;

	@Column(nullable = false)
	private Long warehouseNo;

	@Column(nullable = false)
	private Long productNo;

	@Column(nullable = false)
	private String requestedBy; // 수행 주체: 사용자 요청 or 스케줄러

	@Column(nullable = false)
	private String reason; // 수행 이유: 실패 사유 디테일하게 기록하는데 주로 사용

	@Column(nullable = false)
	private Boolean published;

	@CreatedDate
	@Column(nullable = false)
	private LocalDateTime createdAt;

	private LocalDateTime publishedAt;

	protected OrderStockEventJpaEntity() {
	}

	private OrderStockEventJpaEntity(Builder builder) {
		this.id = builder.id;
		this.orderNo = builder.orderNo;
		this.eventType = builder.eventType;
		this.count = builder.count;
		this.warehouseNo = builder.warehouseNo;
		this.productNo = builder.productNo;
		this.requestedBy = builder.requestedBy;
		this.reason = builder.reason;
		this.published = Boolean.FALSE;
	}

	// 빌더 시작 정적 메서드
	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {

		private String id;
		private Long orderNo;
		private EventType eventType;
		private Integer count;
		private Long warehouseNo;
		private Long productNo;
		private String requestedBy;
		private String reason;

		public Builder id(String id) {
			this.id = id;
			return this;
		}

		public Builder orderNo(Long orderNo) {
			this.orderNo = orderNo;
			return this;
		}

		public Builder eventType(EventType eventType) {
			this.eventType = eventType;
			return this;
		}

		public Builder count(Integer count) {
			this.count = count;
			return this;
		}

		public Builder warehouseNo(Long warehouseNo) {
			this.warehouseNo = warehouseNo;
			return this;
		}

		public Builder productNo(Long productNo) {
			this.productNo = productNo;
			return this;
		}

		public Builder requestedBy(String requestedBy) {
			this.requestedBy = requestedBy;
			return this;
		}

		public Builder reason(String reason) {
			this.reason = reason;
			return this;
		}

		public OrderStockEventJpaEntity build() {
			return new OrderStockEventJpaEntity(this);
		}
	}

	public enum EventType {
		INCREASE, DECREASE;
	}
}
