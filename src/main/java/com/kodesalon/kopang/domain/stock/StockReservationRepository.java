package com.kodesalon.kopang.domain.stock;

public interface StockReservationRepository {

	boolean decreaseStock(Long productNo, Integer count);

	void increaseStock(Long productNo, Integer count);
}
