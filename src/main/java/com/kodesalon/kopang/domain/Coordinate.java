package com.kodesalon.kopang.domain;

import java.awt.geom.Point2D;

public class Coordinate {

	private final Double latitude;
	private final Double longitude;

	public Coordinate(Double latitude, Double longitude) {
		this.latitude = latitude;
		this.longitude = longitude;
	}

	public Double calculateDistance(Coordinate other) {
		return new Point2D.Double(latitude, longitude)
			.distance(other.latitude, other.longitude);
	}

	public Double getLatitude() {
		return latitude;
	}

	public Double getLongitude() {
		return longitude;
	}
}
