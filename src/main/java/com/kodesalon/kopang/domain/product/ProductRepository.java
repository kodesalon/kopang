package com.kodesalon.kopang.domain.product;

import java.util.Optional;

public interface ProductRepository {
	Optional<Product> findByProductNo(Long productNo);
}
