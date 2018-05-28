package org.flightgear.terramaster;
import java.io.File;
import java.awt.Polygon;

public class TileData {
  public Polygon poly;
  public boolean terrain, objects, buildings;
  public File dir_terr, dir_obj, dir_buildings;

  public TileData() {
    terrain = false;
    objects = false;
  }
}
