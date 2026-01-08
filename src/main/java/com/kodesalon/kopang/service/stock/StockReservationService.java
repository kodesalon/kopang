package com.kodesalon.kopang.service.stock;

import java.util.Map;

import org.springframework.stereotype.Service;

import com.kodesalon.kopang.domain.stock.StockQuantity;
import com.kodesalon.kopang.domain.stock.StockReservationRepository;

@Service
public class StockReservationService {

	private final StockReservationRepository stockReservationRepository;

	public StockReservationService(StockReservationRepository stockReservationRepository) {
		this.stockReservationRepository = stockReservationRepository;
	}

	public StockQuantity decrease(Long productNo, Integer count) {
		return stockReservationRepository.decreaseStock(productNo, count);
	}

	public void increase(Long productNo, Integer count) {
		stockReservationRepository.increaseStock(productNo, count);
	}

	public void restoreInBatch(Map<Long, Integer> productRestoreInfo) {
		for (Map.Entry<Long, Integer> entry : productRestoreInfo.entrySet()) {
			stockReservationRepository.increaseStock(entry.getKey(), entry.getValue());
		}
	}
}
