package com.kodesalon.kopang.domain.stock;

public interface StockReservationRepository {

	Boolean decreaseStock(Long productNo, Integer count);

	void increaseStock(Long productNo, Integer count);
}
