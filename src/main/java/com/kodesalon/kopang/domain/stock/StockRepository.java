package com.kodesalon.kopang.domain.stock;

import java.util.Optional;

public interface StockRepository {

	Optional<Stock> findByProductNo(Long productNo);

	void updateStock(Stock stock);
}
