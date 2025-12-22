package com.kodesalon.kopang.service.stock;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kodesalon.kopang.service.exception.NotFoundException;
import com.kodesalon.kopang.domain.stock.Stock;
import com.kodesalon.kopang.domain.stock.StockRepository;
import com.kodesalon.kopang.storage.stock.StockJpaRepository;

@Service
public class StockService {

	private final StockRepository stockRepository;
	private final StockJpaRepository stockJpaRepository;

	public StockService(StockRepository stockRepository, StockJpaRepository stockJpaRepository) {
		this.stockRepository = stockRepository;
		this.stockJpaRepository = stockJpaRepository;
	}

	@Transactional
	public Stock decrease(Long productNo, Integer count) {
		// Stock stock = stockRepository.findByProductNo(productNo)
		// 	.orElseThrow(() -> NotFoundException.stock(productNo));
		// Stock decreased = stock.decrease(count);
		// stockRepository.updateStock(decreased);
		stockJpaRepository.updateDecreaseStock(productNo, count);
		return new Stock(null, productNo, count);
	}
}
