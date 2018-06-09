package org.flightgear.terramaster;
import java.io.File;
import java.awt.Polygon;

/**
 * The data associated with a tile. 
 * @author keith.paterson
 *
 */

public class TileData {
  /**The square drawn on the map.*/
  public Polygon poly;
  /**Flags indicating what the tiles contain. Used for the mouse over.*/
  public boolean terrain, objects, buildings;
  public File dir_terr, dir_obj, dir_buildings;

  public TileData() {
    terrain = false;
    objects = false;
  }
}
