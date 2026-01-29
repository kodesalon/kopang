package com.kodesalon.kopang.config;

public interface Caches {
	interface Manager {
		String CAFFEINE = "caffeineCacheManager";
		String REDIS = "redisCacheManager";
	}

	interface Name {
		String MEMBER_ADDRESS = "member_address";
		String PRODUCT_WAREHOUSES = "product_warehouses";
		String PRODUCT = "product";
	}
}
