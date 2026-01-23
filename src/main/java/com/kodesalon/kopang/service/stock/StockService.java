package com.kodesalon.kopang.service.stock;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kodesalon.kopang.service.exception.NotFoundException;
import com.kodesalon.kopang.domain.stock.Stock;
import com.kodesalon.kopang.domain.stock.StockRepository;

@Service
public class StockService {

	private final StockRepository stockRepository;

	public StockService(StockRepository stockRepository) {
		this.stockRepository = stockRepository;
	}

	@Transactional
	public Stock decrease(Long productNo, Integer count) {
		Stock stock = stockRepository.findByProductNo(productNo)
			.orElseThrow(() -> NotFoundException.stock(productNo));
		Stock decreased = stock.decrease(count);
		stockRepository.updateStock(decreased);
		return decreased;
	}
}
