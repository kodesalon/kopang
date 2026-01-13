package com.kodesalon.kopang.domain.warehouse.policy;

import java.util.List;

import com.kodesalon.kopang.domain.Address;
import com.kodesalon.kopang.domain.warehouse.Warehouse;

public interface WarehouseRoutingPolicy {
	List<Warehouse> route(List<Warehouse> warehouses, Address memberAddress);
}
