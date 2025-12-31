package com.kodesalon.kopang.storage.stock;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

import com.kodesalon.kopang.domain.stock.Stock;
import com.kodesalon.kopang.domain.stock.StockQuantity;
import com.kodesalon.kopang.domain.stock.StockRepository;

@DataJpaTest
@Import(StockRepositoryImpl.class)
class StockRepositoryImplTest {

	private @Autowired StockRepository stockRepository;
	private @Autowired StockJpaRepository stockJpaRepository;
	private @Autowired TestEntityManager tem;

	@DisplayName("상품 번호로 재고를 조회하면 Stock 도메인 객체로 변환되어 반환된다")
	@Test
	void findByProductNo_success() {
		Long productNo = 1L;
		StockJpaEntity entity = new StockJpaEntity(productNo, 100);
		stockJpaRepository.save(entity);

		Optional<Stock> result = stockRepository.findByProductNo(productNo);

		assertThat(result)
			.isPresent()
			.get()
			.isInstanceOf(Stock.class)
			.extracting(Stock::getProductNo, Stock::getQuantity)
			.containsExactly(1L, 100);
	}

	@DisplayName("존재하지 않는 상품 번호 조회 시 Optional Empty 값이 반환된다")
	@Test
	void findByProductNo_notFound() {
		Optional<Stock> result = stockRepository.findByProductNo(Long.MAX_VALUE);
		assertThat(result).isEmpty();
	}

	@DisplayName("updateStock 호출 시 DB의 값이 변경되어야 한다")
	@Test
	void updateStock_success() {
		// given
		StockJpaEntity entity = new StockJpaEntity(1L, 100);
		tem.persist(entity);
		tem.flush();
		tem.clear();

		// when
		Stock stock = new Stock(entity.getNo(), 1L, StockQuantity.from(99));
		stockRepository.updateStock(stock);

		// then
		tem.flush();
		tem.clear();
		StockJpaEntity saved = tem.find(StockJpaEntity.class, entity.getNo());
		assertThat(saved.getQuantity()).isEqualTo(99);
	}
}