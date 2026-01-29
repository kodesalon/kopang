package com.kodesalon.kopang.domain;

public record Address(
	String zipCode,
	String defaultAddress,
	String detail,
	Coordinate coordinate
) {
	public Double distanceTo(Address otherAddress) {
		return coordinate.calculateDistance(otherAddress.coordinate);
	}
}
