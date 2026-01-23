package com.kodesalon.kopang.domain;

import java.awt.geom.Point2D;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Coordinate {

	private final Double latitude;
	private final Double longitude;

	@JsonCreator
	public Coordinate(
		@JsonProperty("latitude") Double latitude,
		@JsonProperty("longitude") Double longitude) {
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
