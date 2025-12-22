package com.kodesalon.kopang.service.stock;

import org.springframework.stereotype.Service;

import com.kodesalon.kopang.domain.stock.StockReservationRepository;

@Service
public class StockReservationService {

	private final StockReservationRepository stockReservationRepository;

	public StockReservationService(StockReservationRepository stockReservationRepository) {
		this.stockReservationRepository = stockReservationRepository;
	}

	public void decrease(Long productNo, Integer count) {
		if (!stockReservationRepository.decreaseStock(productNo, count)) {
			throw new IllegalStateException("남아있는 재고 수량이 없습니다");
		}
	}

	public void increase(Long productNo, Integer count) {
		stockReservationRepository.increaseStock(productNo, count);
	}
}
