package com.kodesalon.kopang.domain.warehouse;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.kodesalon.kopang.domain.Address;

public class Warehouse {

	private final Long no;
	private final String name;
	private final WarehouseRegion region;
	private final Address address;

	@JsonCreator
	public Warehouse(
		@JsonProperty("no") Long no,
		@JsonProperty("name") String name,
		@JsonProperty("region") WarehouseRegion region,
		@JsonProperty("address") Address address
	) {
		this.no = no;
		this.name = name;
		this.region = region;
		this.address = address;
	}

	public Long getNo() {
		return no;
	}

	public String getName() {
		return name;
	}

	public WarehouseRegion getRegion() {
		return region;
	}

	public String getRegionName() {
		return region.name();
	}

	public Address getAddress() {
		return address;
	}
}
