package com.kodesalon.kopang.domain.warehouse.policy;

import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Component;

import com.kodesalon.kopang.domain.Address;
import com.kodesalon.kopang.domain.warehouse.Warehouse;

@Component
public class DistanceBasedRoutingPolicy implements WarehouseRoutingPolicy {

	@Override
	public List<Warehouse> route(List<Warehouse> warehouses, Address memberAddress) {
		return warehouses.stream()
			.sorted(Comparator.comparingDouble(warehouse ->
				memberAddress.distanceTo(warehouse.getAddress())
			))
			.toList();
	}
}
