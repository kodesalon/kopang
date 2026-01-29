package com.kodesalon.kopang.service.warehouse;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.kodesalon.kopang.config.Caches;
import com.kodesalon.kopang.domain.warehouse.WarehouseRepository;
import com.kodesalon.kopang.domain.warehouse.Warehouses;

@Service
public class WarehouseCacheService {

	private final WarehouseRepository warehouseRepository;

	public WarehouseCacheService(WarehouseRepository warehouseRepository) {
		this.warehouseRepository = warehouseRepository;
	}

	@Cacheable(
		cacheManager = Caches.Manager.CAFFEINE,
		value = Caches.Name.PRODUCT_WAREHOUSES,
		key = "#productNo"
	)
	public Warehouses getWarehousesForProduct(Long productNo) {
		return new Warehouses(warehouseRepository.findAllByProductNo(productNo));
	}
}
