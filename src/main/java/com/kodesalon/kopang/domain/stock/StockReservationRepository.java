package com.kodesalon.kopang.domain.stock;

public interface StockReservationRepository {

	Stock decreaseStock(Long productNo, Integer count);

	void increaseStock(Long productNo, Integer count);
}
