package org.flightgear.terramaster;
/** 
 * convenience methods for converting between
 * lat,lon and "e000s00" formats
 */

import java.awt.geom.Point2D;
import java.util.*;
import java.util.regex.*;

public class TileName implements Comparable<TileName> {
	private int lat, lon;
	private String name;

	private static Hashtable<String, TileName> tilenameMap;
	
	/** creates a hashtable of all possible 1x1 tiles in the world*/
	static{
		tilenameMap = new Hashtable<String, TileName>();

		for (int x = -180; x < 180; ++x) {
			for (int y = -90; y < 90; ++y) {
				TileName t = new TileName(y, x);
				tilenameMap.put(t.getName(), t);
			}
		}		
	}

	public TileName(int lat, int lon) {
		this.lat = lat;
		this.lon = lon;
		name = computeTileName(lat, lon);
	}

	public TileName(String name) {
		this.name = name;
		Pattern p = Pattern
				.compile("([ew])(\\p{Digit}{3})([ns])(\\p{Digit}{2})");
		Matcher m = p.matcher(name);
		if (m.matches()) {
			int lon = Integer.parseInt(m.group(2));
			int lat = Integer.parseInt(m.group(4));
			this.lon = m.group(1).equals("w") ? -lon : lon;
			this.lat = m.group(3).equals("s") ? -lat : lat;
		} else
			lat = lon = 0;
	}

	public int compareTo(TileName l) {
		return name.compareTo(l.getName());
	}

	public String getName() {
		return name;
	}

	public int getLat() {
		return lat;
	}

	public int getLon() {
		return lon;
	}

	// W and S are negative
	public static String computeTileName(Point2D.Double p) {
		if (p == null)
			return "";
		return computeTileName((int) -Math.ceil(p.y), (int) Math.floor(p.x));
	}

	/** W and S are negative*/
	public static String computeTileName(int lat, int lon) {
		char ew = 'e', ns = 'n';

		if (lon < 0) {
			lon = -lon;
			ew = 'w';
		}
		if (lat < 0) {
			lat = -lat;
			ns = 's';
		}
		// XXX check sanity
		return String.format("%c%03d%c%02d", ew, lon, ns, lat);
	}

	// returns ICAO code from "ICAO.btg.gz", or null
	public static String getAirportCode(String n) {
		Pattern p = Pattern.compile("([A-Z0-9]{1,4}).btg.gz");
		Matcher m = p.matcher(n);
		if (m.matches()) {
			return m.group(1);
		}
		return null;
	}

	public static TileName getTile(String n) {
		return tilenameMap.get(n);
	}

	public static TileName getTile(int x, int y) {
		return tilenameMap.get(computeTileName(y, x));
	}

	public static TileName getTile(Point2D.Double p) {
		return tilenameMap.get(computeTileName(p));
	}

	/**
	 * given a 1x1 tile, figure out the parent 10x10 container return the 10/1
	 * path
	 * 
	 * @return
	 */
	public String buildPath() {
		if (name.length() < 7)
			return null;

		// XXX throw an exception
		int lon = Integer.parseInt(name.substring(1, 4));
		int lat = Integer.parseInt(name.substring(5));
		char ew = name.charAt(0);
		char ns = name.charAt(4);

		int modlon = lon % 10;
		lon -= ew == 'w' && modlon != 0 ? modlon - 10 : modlon;

		int modlat = lat % 10;
		lat -= ns == 's' && modlat != 0 ? modlat - 10 : modlat;

		return String.format("%s%03d%s%02d/%s", ew, lon, ns, lat, name);
	}

	public TileName getNeighbour(int i, int j) {		
		TileName tile = TileName.getTile(lon + i, lat + j);
		return tile;
	}
	
	@Override
	public String toString() {
		return getName();
	}
}
