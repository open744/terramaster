// WinkelTriple, Azimuthal Orthographic (globe)

// svn --force co http://terrascenery.googlecode.com/svn/trunk/data/Scenery/Terrain/e100n00/e104n00

// http://stackoverflow.com/questions/3727662/how-can-you-search-google-programmatically-java-api

// XXX TODO
// 1. on exit, check if still syncing; close Svn
// 2. on exit, write Properties DONE
// 3. keyboard actions
// 4. double-click for priority sync

import java.io.DataInput;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

public class TerraMaster {
	Logger log = Logger.getLogger(this.getClass().getName());
	public static ArrayList<MapPoly> polys, borders;

	static MapFrame frame;
	public GshhsHeader gshhsHeader;

	public static Map<TileName, TileData> mapScenery;

	public static TileName tilenameManager;
	/** The service getting the tiles */
	public static TileService svn;
	public static FGMap fgmap;
	public static Properties props;

	public static void addScnMapTile(Map<TileName, TileData> map, File i, TerraSyncDirectoryTypes type) {
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
    case BUILDINGS:
      t.buildings = true;
      t.dir_buildings = i;
      break;
		}
		map.put(n, t);
	}

	// given a 10x10 dir, add the 1x1 tiles within to the HashMap
	static void buildScnMap(File dir, Map<TileName, TileData> map, TerraSyncDirectoryTypes type) {
		File tiles[] = dir.listFiles();
		Pattern p = Pattern.compile("([ew])(\\p{Digit}{3})([ns])(\\p{Digit}{2})");

		for (File f : tiles) {
			Matcher m = p.matcher(f.getName());
			if (m.matches())
				addScnMapTile(map, f, type);
		}
	}

	// builds a HashMap of /Terrain and /Objects
	static Map<TileName, TileData> newScnMap(String path) {
		TerraSyncDirectoryTypes[] types = { TerraSyncDirectoryTypes.TERRAIN, TerraSyncDirectoryTypes.OBJECTS, TerraSyncDirectoryTypes.BUILDINGS };
		Pattern patt = Pattern.compile("([ew])(\\p{Digit}{3})([ns])(\\p{Digit}{2})");
		Map<TileName, TileData> map = new HashMap<TileName, TileData>(180 * 90);

		for (TerraSyncDirectoryTypes terraSyncDirectoryType : types) {
			File d = new File(path + File.separator + terraSyncDirectoryType.dirname);
			File list[] = d.listFiles();
			if (list != null) {
				// list of 10x10 dirs
				for (File f : list) {
					Matcher m = patt.matcher(f.getName());
					if (m.matches()) {
						// now look inside this dir
						buildScnMap(f, map, terraSyncDirectoryType);
					}
				}
			}
		}
		return map;
	}

	int readGshhsHeader(DataInput s, GshhsHeader h) {
		int fl;
		try {
			h.id = s.readInt();
			h.n = s.readInt(); // npoints
			fl = s.readInt();
			h.greenwich = (fl & 1 << 16) > 0 ? true : false;
			h.level = (byte) (fl & 0xff);
			h.west = s.readInt();
			h.east = s.readInt();
			h.south = s.readInt();
			h.north = s.readInt();
			h.area = s.readInt();
			h.areaFull = s.readInt();
			h.container = s.readInt();
			h.ancestor = s.readInt();
			return h.n;
		} catch (Exception e) {
			return -1;
		}
	}

	// reads in GSHHS and builds ArrayList of polys

	ArrayList<MapPoly> newPolyList(String filename) {

		ArrayList<MapPoly> poly = new ArrayList<MapPoly>();

		try {
			DataInput s = new DataInputStream(
					// new FileInputStream(filename));
					getClass().getClassLoader().getResourceAsStream(filename));
			int n = 0;
			do {
				GshhsHeader h = new GshhsHeader();
				n = readGshhsHeader(s, h);
				if (n > 0)
					poly.add(new MapPoly(s, h));
			} while (n > 0);
		} catch (Exception e) {
			log.log(Level.SEVERE, filename, e);
		}

		// System.out.format("%s: %d polys\n", filename, poly.size());
		return poly;
	}

	void createAndShowGUI() {
		// find our jar
		java.net.URL url = getClass().getClassLoader().getResource("gshhs_l.b");
		log.fine("getResource: " + url);

		String path = props.getProperty(TerraMasterProperties.SCENERY_PATH);
		if (path != null) {
			svn.setScnPath(new File(path));
			mapScenery = newScnMap(path);
		} else {
			mapScenery = new HashMap<TileName, TileData>();
		}

		frame = new MapFrame("TerraMaster");
		frame.restoreSettings();
		// frame.setLocationRelativeTo(null);
		frame.setVisible(true);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		/*
		 * worker_polys = new Worker<ArrayList<MapPoly>, Void>("gshhs_l.b");
		 * worker_polys.execute(); worker_borders = new
		 * Worker<ArrayList<MapPoly>, Void>("wdb_borders_l.b");
		 * worker_borders.execute();
		 */
		polys = newPolyList("gshhs_l.b");
		borders = newPolyList("wdb_borders_l.b");
		frame.passPolys(polys);
		frame.passBorders(borders);
	}

	public static void main(String args[]) {

		try {
			InputStream resourceAsStream = TerraMaster.class.getResourceAsStream("/terramaster.logging.properties");
			if (resourceAsStream != null) {
				LogManager.getLogManager().readConfiguration(resourceAsStream);
			}
		} catch (SecurityException | IOException e1) {
			e1.printStackTrace();
		}
		Logger LOG = Logger.getLogger(TerraMaster.class.getName());
		// Logger.getGlobal().setLevel(Level.ALL);

		tilenameManager = new TileName();

		fgmap = new FGMap(); // handles webqueries

		props = new Properties();
		try {
			props.load(new FileReader("terramaster.properties"));
		} catch (IOException e) {
			LOG.log(Level.WARNING, "Couldn't load properties : " + e.toString(), e);
		}
		LOG.info("Starting TerraMaster");

		setTileService();
		
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				new TerraMaster().createAndShowGUI();
			}
		});

	}

	public static void setTileService() {
		String server_type = props.getProperty(TerraMasterProperties.SERVER_TYPE);
		if (server_type == null || server_type.indexOf("SVN") >= 0) {
			svn = new Svn();
			svn.start(); // the Svn thread processes the tile queue
		} else {
			svn = new HTTPTerraSync();
			svn.start();
		}
		svn.restoreSettings();
	}

}
