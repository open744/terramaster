package org.flightgear.terramaster;

public class Airport {
	String name, code, tilename = "";
	double lat;
	double lon;
	private double maxLat = 0;
	private double maxLon = 0;
	private double minLon = 0;
	private double minLat = 0;

	public Airport(String code, String name) {
		this.code = code;
		this.name = name;
	}

	public String toString() {
		return String.format("%s %s (%g,%g) %s", code, name, lat, lon, tilename);
	}

	public String getTileName() {
		return tilename;
	}

	public void updatePosition(String sLat, String sLon) {
		double lat = Double.parseDouble(sLat);
		double lon = Double.parseDouble(sLon);

		maxLat = maxLat!=0?Math.max(maxLat, lat):lat;
		maxLon = maxLon!=0?Math.max(maxLon, lon):lon;

		minLat = Math.min(minLat, lat);
		minLon = Math.min(minLon, lon);
		this.lat = (maxLat - minLat) + minLat; 
		this.lon = (maxLon - minLon) + minLon; 
	}
}
