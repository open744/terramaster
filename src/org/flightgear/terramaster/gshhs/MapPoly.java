package org.flightgear.terramaster.gshhs;

import java.awt.Polygon;
import java.io.DataInput;

public class MapPoly extends Polygon {

  public GshhsHeader gshhsHeader;
  public byte level;

  public MapPoly() {
  }

  /**
   * reads raw GSHHS format
   * 
   * @param s
   * @param h
   * @throws Exception
   */

  public MapPoly(DataInput s, GshhsHeader h) throws Exception {
    gshhsHeader = h;
    level = h.getLevel();

    for (int i = 0; i < h.getNumPoints(); ++i) {
      float x = s.readInt() / 10000;
      float y = -s.readInt() / 10000;
      if ((h.isGreenwich() && x > 27000) || h.getWest() > 180000000)
        x -= 36000;
      addPoint((int) x, (int) y);
    }
  }

}
