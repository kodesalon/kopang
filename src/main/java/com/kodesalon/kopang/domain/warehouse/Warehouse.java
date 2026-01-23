package com.kodesalon.kopang.domain.warehouse;

import com.kodesalon.kopang.domain.Address;

public class Warehouse {

	private final Long no;
	private final String name;
	private final WarehouseRegion region;
	private final Address address;

	public Warehouse(Long no, String name, WarehouseRegion region, Address address) {
		this.no = no;
		this.name = name;
		this.region = region;
		this.address = address;
	}

	public Long getNo() {
		return no;
	}

	public String getRegionName() {
		return region.name();
	}

	public Address getAddress() {
		return address;
	}
}
