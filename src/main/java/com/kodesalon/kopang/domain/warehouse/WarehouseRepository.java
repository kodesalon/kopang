package com.kodesalon.kopang.domain.warehouse;

import java.util.List;

public interface WarehouseRepository {
	List<Warehouse> findAllByProductNo(Long productNo);
}
