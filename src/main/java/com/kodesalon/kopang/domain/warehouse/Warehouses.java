package com.kodesalon.kopang.domain.warehouse;

import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.kodesalon.kopang.domain.Address;

public class Warehouses implements Iterable<Warehouse> {

	private final List<Warehouse> values;

	@JsonCreator(mode = JsonCreator.Mode.DELEGATING)
	public Warehouses(List<Warehouse> values) {
		this.values = values;
	}

	public Warehouses sortedByDistance(Address memberAddress) {
		List<Warehouse> sorted = values.stream()
			.sorted(Comparator.comparingDouble(warehouse ->
				memberAddress.distanceTo(warehouse.getAddress())
			))
			.toList();
		return new Warehouses(sorted);
	}

	@Override
	public Iterator<Warehouse> iterator() {
		return values.iterator();
	}

	public List<Warehouse> getValues() {
		return values;
	}
}
