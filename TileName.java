// convenience methods for converting between
// lat,lon and "e000s00" formats

import java.awt.geom.Point2D;
import java.util.*;
import java.util.regex.*;

public class TileName implements Comparable<TileName>
{
  private int lat, lon;
  private String name;

  private Hashtable<String, TileName> tilenameMap;

  // creates a hashtable of all possible 1x1 tiles in the world
  public TileName()
  {
    tilenameMap = new Hashtable<String, TileName>();

    for (int x = -180; x < 180; ++x) {
      for (int y = -90; y < 90; ++y) {
	TileName t = new TileName(y, x);
	tilenameMap.put(t.getName(), t);
      }
    }
  }

  public TileName(int lat, int lon)
  {
    this.lat = lat;
    this.lon = lon;
    name = computeTileName(lat, lon);
  }

  public TileName(String name)
  {
    this.name = name;
    Pattern p = Pattern.compile("([ew])(\\p{Digit}{3})([ns])(\\p{Digit}{2})");
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

  public String getName()
  {
    return name;
  }

  public int getLat()
  {
    return lat;
  }

  public int getLon()
  {
    return lon;
  }

  // W and S are negative
  public String computeTileName(Point2D.Double p)
  {
    if (p == null) return "";
    return computeTileName((int)-Math.ceil(p.y), (int)Math.floor(p.x));
  }

  // W and S are negative
  public String computeTileName(int lat, int lon)
  {
    char	ew = 'e', ns = 'n';

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




  public TileName getTile(String n)
  {
    return tilenameMap.get(n);
  }

  public TileName getTile(int x, int y)
  {
    return tilenameMap.get(computeTileName(y, x));
  }

  public TileName getTile(Point2D.Double p)
  {
    return tilenameMap.get(computeTileName(p));
  }
}
