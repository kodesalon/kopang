package com.kodesalon.kopang.storage.warehouse;

import com.kodesalon.kopang.domain.warehouse.WarehouseRegion;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "warehouse")
public class WarehouseJpaEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long no;

	@Column(nullable = false)
	private String name;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private WarehouseRegion region;

	@Column(nullable = false)
	private String zipCode;

	@Column(nullable = false)
	private String address;

	private String detail;

	@Column(nullable = false)
	private Double latitude;

	@Column(nullable = false)
	private Double longitude;

	protected WarehouseJpaEntity() {
	}
}
