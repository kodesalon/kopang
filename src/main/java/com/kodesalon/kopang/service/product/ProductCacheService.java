package com.kodesalon.kopang.service.product;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.kodesalon.kopang.config.Caches;
import com.kodesalon.kopang.domain.product.Product;
import com.kodesalon.kopang.domain.product.ProductRepository;
import com.kodesalon.kopang.service.exception.NotFoundException;

@Service
public class ProductCacheService {

	private final ProductRepository productRepository;

	public ProductCacheService(ProductRepository productRepository) {
		this.productRepository = productRepository;
	}

	@Cacheable(
		cacheManager = Caches.Manager.CAFFEINE,
		value = Caches.Name.PRODUCT,
		key = "#productNo"
	)
	public Product getProduct(Long productNo) {
		return productRepository.findByProductNo(productNo)
			.orElseThrow(() -> NotFoundException.product(productNo));
	}
}
