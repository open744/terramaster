// WinkelTriple, Azimuthal Orthographic (globe)

// svn --force co http://terrascenery.googlecode.com/svn/trunk/data/Scenery/Terrain/e100n00/e104n00

// http://stackoverflow.com/questions/3727662/how-can-you-search-google-programmatically-java-api

import	java.io.*;
import	java.awt.*;
import	java.awt.image.*;
import	java.awt.geom.*;
import	java.util.*;
import	java.util.regex.*;
import	javax.swing.*;

public class TerraMaster {

  public static ArrayList<MapPoly>	polys;

  MapFrame	frame;
  public GshhsHeader	gshhsHeader;

  public static Map<String, TileData>	mapScenery;
  static final int	TERRAIN = 0,
			OBJECTS = 1;

  public static TileName tilenameManager;


  static void buildScnMap(File f, Map<String, TileData> map, int type) {
    TileData	t;
    File	x[] = f.listFiles();
    Pattern	p = Pattern.compile("([ew])(\\p{Digit}{3})([ns])(\\p{Digit}{2})");

    for (File i: x) {
      String	str = i.getName();
      Matcher	m = p.matcher(str);
      if (m.matches()) {
	t = map.get(str);
	if (t == null) {
	  // make a new TileData
	  t = new TileData();
	}
	switch (type) {
	case TERRAIN:
	  t.terrain = true;
	  t.dir_terr = i;
	  break;
	case OBJECTS:
	  t.objects = true;
	  t.dir_obj = i;
	  break;
	}
	map.put(str, t);
      }
    }
  }

  // builds a HashMap of /Terrain and /Objects
  static Map<String, TileData> newScnMap(String path)
  {
    String[]	types = { "/Terrain", "/Objects" };
    Pattern	patt = Pattern.compile("([ew])(\\p{Digit}{3})([ns])(\\p{Digit}{2})");
    Map<String, TileData>	map = new HashMap<String, TileData>(180*90);

    for (int i = 0; i < types.length; ++i) {
      File	list[] = new File(path + types[i]).listFiles();

      // list of 10x10 dirs
      for (File f: list) {
	Matcher	m = patt.matcher(f.getName());
	if (m.matches()) {
	  // now look inside this dir
	  buildScnMap(f, map, i);
	}
      }
    }
    return map;
  }




  int readGshhsHeader(DataInput s, GshhsHeader h) {
    int		fl;
    try {
      h.id = s.readInt();
      h.n  = s.readInt();	// npoints
      fl = s.readInt();
      h.greenwich = (fl & 1<<16) > 0 ? true : false;
      h.level = (byte)(fl & 0xff);
      h.west = s.readInt();
      h.east = s.readInt();
      h.south = s.readInt();
      h.north = s.readInt();
      h.area = s.readInt();
      h.areaFull  = s.readInt();
      h.container = s.readInt();
      h.ancestor  = s.readInt();
      return h.n;
    } catch (Exception e) {
      return -1;
    }
  }

  // reads in GSHHS and builds ArrayList of polys
  SwingWorker	worker = new SwingWorker<ArrayList<MapPoly>, Void>() {

    public ArrayList<MapPoly> doInBackground() {

      ArrayList<MapPoly>	poly = new ArrayList<MapPoly>();
      String			filename = "gshhs_l.b";

System.out.println("worker.doInBackground()");
      try {
	GshhsHeader	h = new GshhsHeader();
	DataInput	s = new DataInputStream(new
	    FileInputStream(new File(filename)));

	int n = 0;
	while ((n = readGshhsHeader(s, h)) > 0) {
	  poly.add(new MapPoly(s, h));
	}
      } catch (Exception e) {
	System.out.println(filename + ": " + e);
	System.exit(-1);
      }

      return poly;
    }

    public void done() {
      // called by Event Disp thread
      // waits for worker to finish
      try {
	polys = get();
	System.out.println("worker: polys " + polys.size());
	frame.passPolys(polys);
      } catch (Exception e) {
	System.out.println(e);
      }
    }

  };

  void createAndShowGUI() {

    mapScenery = new HashMap<String, TileData>();	// empty
    worker.execute();

    frame = new MapFrame("TerraMaster");
    frame.setSize(740, 640);
    frame.setLocationRelativeTo(null);
    frame.setVisible(true);
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

    if (worker.isDone()) {
      frame.initImage();
      frame.showTiles();
      frame.repaint();
    }

    System.out.println("isEventDispThread " +
	SwingUtilities.isEventDispatchThread());
  }

  public static void main(String args[]) {

    tilenameManager = new TileName();

    SwingUtilities.invokeLater(new Runnable() {
	public void run() {
	  new TerraMaster().createAndShowGUI();
	}
    });

  }

}
