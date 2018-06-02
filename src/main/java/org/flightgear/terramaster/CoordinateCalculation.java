package org.flightgear.terramaster;

public class CoordinateCalculation {
	
	/**Radius in km*/
	public static final double R = 6372.8; 

	/**
	 * Old Method missing the Radius
	 * @deprecated
	 * @param lat1 in radians
	 * @param lon1 in radians
	 * @param lat2 in radians
	 * @param lon2 in radians
	 * @return
	 */
	static double oldHaversine(double lat1, double lon1, double lat2, double lon2) {
		double dlat = Math.sin((lat2 - lat1) / 2);
		double dlon = Math.sin((lon2 - lon1) / 2);
		double r = Math.sqrt(dlat * dlat + Math.cos(lat1) * Math.cos(lat2) * dlon * dlon);
		return 2.0 * Math.asin(r);
	}
	
	public static double greatCircleDistance(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        lat1 = Math.toRadians(lat1);
        lat2 = Math.toRadians(lat2);
 
        double a = Math.pow(Math.sin(dLat / 2),2) + Math.pow(Math.sin(dLon / 2),2) * Math.cos(lat1) * Math.cos(lat2);
        double c = 2 * Math.asin(Math.sqrt(a));
        return R * c;
    }
	
	/**
	 * Calculate the bearing
	 * @param lat1 
	 * @param lon1
	 * @param lat2
	 * @param lon2
	 * @return
	 */

	public static double greatCircleBearing(double lat1, double lon1, double lat2, double lon2) {
		double longitude1 = lon1;
		double longitude2 = lon2;
		double latitude1 = Math.toRadians(lat1);
		double latitude2 = Math.toRadians(lat2);
		double longDiff = Math.toRadians(longitude2 - longitude1);
		double y = Math.sin(longDiff) * Math.cos(latitude2);
		double x = Math.cos(latitude1) * Math.sin(latitude2)
				- Math.sin(latitude1) * Math.cos(latitude2) * Math.cos(longDiff);

		return (Math.toDegrees(Math.atan2(y, x)) + 360) % 360;
	}
}
