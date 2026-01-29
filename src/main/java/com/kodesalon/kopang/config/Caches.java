package com.kodesalon.kopang.config;

public interface Caches {
	interface Manager {
		String CAFFEINE = "caffeineCacheManager";
		String REDIS = "redisCacheManager";
	}

	interface Name {
		String PRODUCT_WAREHOUSES = "product_warehouses";
		String PRODUCT = "product";
	}
}
