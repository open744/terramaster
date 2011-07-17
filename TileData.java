import	java.io.File;
import	java.awt.Polygon;

public class TileData {
  public Polygon	poly;
  public boolean	terrain, objects;
  public File		dir_terr, dir_obj;

  public TileData() {
    terrain = false;
    objects = false;
  }
}
