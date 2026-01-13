package com.kodesalon.kopang.domain;

public class Address {

	private final String zipCode;
	private final String defaultAddress;
	private final String detail;
	private final Coordinate coordinate;

	public Address(String zipCode, String defaultAddress, String detail, Coordinate coordinate) {
		this.zipCode = zipCode;
		this.defaultAddress = defaultAddress;
		this.detail = detail;
		this.coordinate = coordinate;
	}

	public Double distanceTo(Address otherAddress) {
		return coordinate.calculateDistance(otherAddress.coordinate);
	}
}
