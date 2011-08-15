// WinkelTriple, Azimuthal Orthographic (globe)

// svn --force co http://terrascenery.googlecode.com/svn/trunk/data/Scenery/Terrain/e100n00/e104n00

// http://stackoverflow.com/questions/3727662/how-can-you-search-google-programmatically-java-api

// XXX TODO
// 1. on exit, check if still syncing; close Svn
// 2. on exit, write Properties

import	java.io.*;
import	java.awt.*;
import	java.awt.image.*;
import	java.awt.geom.*;
import	java.util.*;
import	java.util.regex.*;
import	javax.swing.*;

public class TerraMaster
{
  public static ArrayList<MapPoly>	polys;

  static MapFrame	frame;
  public GshhsHeader	gshhsHeader;

  public static Map<TileName, TileData>	mapScenery;
  static final int	TERRAIN = 0,
			OBJECTS = 1;

  public static TileName tilenameManager;
  public static Svn svn;
  public static Properties props;

  public static void addScnMapTile(Map<TileName, TileData> map,
				   File i, int type)
  {
    TileName n = tilenameManager.getTile(i.getName());
    TileData t = map.get(n);
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
    map.put(n, t);
  }

  static void buildScnMap(File f, Map<TileName, TileData> map, int type)
  {
    File x[] = f.listFiles();
    Pattern p = Pattern.compile("([ew])(\\p{Digit}{3})([ns])(\\p{Digit}{2})");

    for (File i: x) {
      Matcher	m = p.matcher(i.getName());
      if (m.matches())
	addScnMapTile(map, i, type);
    }
  }

  // builds a HashMap of /Terrain and /Objects
  static Map<TileName, TileData> newScnMap(String path)
  {
    String[] types = { "/Terrain", "/Objects" };
    Pattern patt = Pattern.compile("([ew])(\\p{Digit}{3})([ns])(\\p{Digit}{2})");
    Map<TileName, TileData> map = new HashMap<TileName, TileData>(180*90);

    for (int i = 0; i < types.length; ++i) {
      File	list[] = new File(path + types[i]).listFiles();
      if (list == null) return map;

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




  int readGshhsHeader(DataInput s, GshhsHeader h)
  {
    int	 fl;
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

	try {
	  DataInput	s = new DataInputStream(new
	      FileInputStream(new File(filename)));

	  int n = 0;
	  do {
	    GshhsHeader	h = new GshhsHeader();
	    n = readGshhsHeader(s, h);
	    if (n > 0) poly.add(new MapPoly(s, h));
	  } while (n > 0);
	} catch (Exception e) {
	  System.out.println(filename + ": " + e);
	  System.exit(-1);
	}

	return poly;
      }

      public void done() {
	// called by Event Disp thread
	// waits for worker to finish
System.out.format("worker.done() ... "); System.out.flush();
	try {
	  while (!isDone())
	    Thread.sleep(100);
	  polys = get();
	  System.out.format("%d polys\n", polys.size());
	  frame.passPolys(polys);
	} catch (Exception e) {
	  System.out.println(e);
	}
      }

  };

  void createAndShowGUI()
  {
    String geom = props.getProperty("Geometry");
    Scanner s = new Scanner(geom);
    s.useDelimiter(Pattern.compile("[x+-]"));

    int w = s.nextInt();
    int h = s.nextInt();
    int x = s.nextInt();
    int y = s.nextInt();

    frame = new MapFrame("TerraMaster");
    frame.setSize(w, h);
    frame.setLocation(x, y);
    //frame.setLocationRelativeTo(null);
    frame.setVisible(true);
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

    worker.execute();

    frame.repaint();

  }

  public static void main(String args[]) {

    tilenameManager = new TileName();
    svn = new Svn();
    svn.start();	// the Svn thread processes the tile queue

    props = new Properties();
    try {
      //props.store(new FileWriter("terramaster.properties"), null);
      props.load(new FileReader("terramaster.properties"));
    } catch (IOException e) {
    }

    String path = props.getProperty("SceneryPath");
    svn.setScnPath(new File(path));
    mapScenery = newScnMap(path);

    SwingUtilities.invokeLater(new Runnable() {
	public void run() {
	  new TerraMaster().createAndShowGUI();
	}
    });

  }

}
