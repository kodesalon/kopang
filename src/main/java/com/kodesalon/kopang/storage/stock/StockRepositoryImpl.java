package com.kodesalon.kopang.storage.stock;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.kodesalon.kopang.domain.stock.Stock;
import com.kodesalon.kopang.domain.stock.StockRepository;

@Repository
public class StockRepositoryImpl implements StockRepository {

	private final StockJpaRepository stockJpaRepository;

	public StockRepositoryImpl(StockJpaRepository stockJpaRepository) {
		this.stockJpaRepository = stockJpaRepository;
	}

	@Override
	public Optional<Stock> findByProductNo(Long productNo) {
		StockJpaEntity jpaEntity = stockJpaRepository.findByProductNoWithLock(productNo);
		return Optional.ofNullable(jpaEntity).map(StockJpaEntity::toDomain);
	}

	@Override
	public void updateStock(Stock stock) {
		stockJpaRepository.updateStock(stock.getProductNo(), stock.getQuantity());
	}
}
