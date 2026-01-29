package com.kodesalon.kopang.domain;

import java.awt.geom.Point2D;

public record Coordinate(
	Double latitude,
	Double longitude
) {
	public Double calculateDistance(Coordinate other) {
		return new Point2D.Double(latitude, longitude)
			.distance(other.latitude, other.longitude);
	}
}
