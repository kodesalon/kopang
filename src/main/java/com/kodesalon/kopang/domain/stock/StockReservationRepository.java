package com.kodesalon.kopang.domain.stock;

import java.util.Optional;

public interface StockReservationRepository {

	Optional<StockQuantity> decreaseStock(String warehouseName, Long productNo, Integer count);

	void increaseStock(Long productNo, Integer count);
}
