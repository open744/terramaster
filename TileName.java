// convenience methods for converting between
// lat,lon and "e000s00" formats

import java.awt.geom.Point2D;
import java.util.regex.*;

public class TileName implements Comparable<TileName> 
{
  private int lat, lon;
  private String name;
  private String [][]table = new String[360][180];

  // probably not needed
  public TileName()
  {
    for (int x = -180; x < 180; ++x) {
      for (int y = -90; y < 90; ++y) {
	table[x][y] = computeTileName(y, x);
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

  public TileName(Point2D.Double p)
  {
    this((int)-Math.ceil(p.y), (int)Math.floor(p.x));
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
    return computeTileName((int)Math.floor(p.x), (int)-Math.ceil(p.y));
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
}
