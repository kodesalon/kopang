package com.kodesalon.kopang.domain.stock;

import java.util.Optional;

public interface StockReservationRepository {

	Optional<StockQuantity> decreaseStock(Long warehouseNo, Long productNo, Integer count);

	void increaseStock(Long warehouseNo, Long productNo, Integer count);
}
