package com.kodesalon.kopang.storage.product;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.kodesalon.kopang.domain.product.Product;
import com.kodesalon.kopang.domain.product.ProductRepository;

@Repository
public class ProductRepositoryImpl implements ProductRepository {

	private final ProductJpaRepository productJpaRepository;

	public ProductRepositoryImpl(ProductJpaRepository productJpaRepository) {
		this.productJpaRepository = productJpaRepository;
	}

	@Override
	public Optional<Product> findByProductNo(Long productNo) {
		return Optional.ofNullable(productJpaRepository.findByNo(productNo).toDomain());
	}
}
