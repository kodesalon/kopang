package com.kodesalon.kopang.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Address {

	private final String zipCode;
	private final String defaultAddress;
	private final String detail;
	private final Coordinate coordinate;

	@JsonCreator
	public Address(
		@JsonProperty("zipCode") String zipCode,
		@JsonProperty("defaultAddress") String defaultAddress,
		@JsonProperty("detail") String detail,
		@JsonProperty("coordinate") Coordinate coordinate
	) {
		this.zipCode = zipCode;
		this.defaultAddress = defaultAddress;
		this.detail = detail;
		this.coordinate = coordinate;
	}

	public Double distanceTo(Address otherAddress) {
		return coordinate.calculateDistance(otherAddress.coordinate);
	}

	public String getZipCode() {
		return zipCode;
	}

	public String getDefaultAddress() {
		return defaultAddress;
	}

	public String getDetail() {
		return detail;
	}

	public Coordinate getCoordinate() {
		return coordinate;
	}
}
