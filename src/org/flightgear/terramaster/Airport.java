package org.flightgear.terramaster;
import java.awt.geom.Point2D;

public class Airport
{
  String name, code, tilename = "";
  float lat, lon;

  public String toString()
  {
    return String.format("%s %s (%g,%g) %s", code, name, lat, lon, tilename);
  }

  public String getTileName()
  {
    return tilename;
  }
}
