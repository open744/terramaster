import	java.io.*;
import	java.awt.*;
import	java.util.*;

public class MapPoly extends Polygon {

  public GshhsHeader	gshhsHeader;
  public byte		level;

  public MapPoly() {
  }

  // reads raw GSHHS format
  public MapPoly(DataInput s, GshhsHeader h) throws Exception {
    gshhsHeader = h;
    level = h.level;

    for (int i = 0; i < h.n; ++i) {
      float	x =  s.readInt() / 10000;
      float	y = -s.readInt() / 10000;
      if ((h.greenwich && x > 27000) || h.west > 180000000) x -= 36000;
      addPoint((int)x, (int)y);
    }
  }

}
