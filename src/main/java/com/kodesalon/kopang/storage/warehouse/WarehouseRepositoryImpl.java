package com.kodesalon.kopang.storage.warehouse;

import java.util.List;

import org.springframework.stereotype.Repository;

import com.kodesalon.kopang.domain.warehouse.Warehouse;
import com.kodesalon.kopang.domain.warehouse.WarehouseRepository;

@Repository
public class WarehouseRepositoryImpl implements WarehouseRepository {

	private final WarehouseJpaRepository warehouseJpaRepository;

	public WarehouseRepositoryImpl(WarehouseJpaRepository warehouseJpaRepository) {
		this.warehouseJpaRepository = warehouseJpaRepository;
	}

	@Override
	public List<Warehouse> findAllByProductNo(Long productNo) {
		return warehouseJpaRepository.findAllByProductNo(productNo)
			.stream()
			.map(WarehouseJpaEntity::toDomain)
			.toList();
	}
}
