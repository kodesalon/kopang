package com.kodesalon.kopang.storage.product;

import static org.junit.jupiter.api.Assertions.*;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import com.kodesalon.kopang.domain.order.Money;
import com.kodesalon.kopang.domain.product.Product;
import com.kodesalon.kopang.domain.product.ProductRepository;

@DataJpaTest
@Import(ProductRepositoryImpl.class)
class ProductRepositoryImplTest {

	private @Autowired ProductRepository productRepository;
	private @Autowired ProductJpaRepository productJpaRepository;

	@DisplayName("존재하는 상품 번호로 조회하면 Optional 로 감싸진 도메인 객체를 반환한다")
	@Test
	void findByProductNo_success() {
		ProductJpaEntity savedProduct = productJpaRepository.save(
			new ProductJpaEntity("테스트 상품", "테스트 상품 설명", new BigDecimal(1000))
		);
		Long productNo = savedProduct.getNo();

		Optional<Product> result = productRepository.findByProductNo(productNo);

		assertThat(result).isPresent()
			.get()
			.extracting(Product::getPrice)
			.isEqualTo(new BigDecimal(1000));
	}

	@DisplayName("존재하지 않는 상품 번호로 조회하면 빈 Optional을 반환한다")
	@Test
	void findByProductNo_empty() {
		Optional<Product> result = productRepository.findByProductNo(Long.MAX_VALUE);
		assertThat(result).isEmpty();
	}
}