package org.flightgear.terramaster;
// WinkelTriple, Azimuthal Orthographic (globe)

// svn --force co http://terrascenery.googlecode.com/svn/trunk/data/Scenery/Terrain/e100n00/e104n00

// http://stackoverflow.com/questions/3727662/how-can-you-search-google-programmatically-java-api

// XXX TODO
// 1. on exit, check if still syncing; close Svn
// 2. on exit, write Properties DONE
// 3. keyboard actions
// 4. double-click for priority sync

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.jar.Manifest;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import org.flightgear.terramaster.gshhs.GshhsReader;
import org.flightgear.terramaster.gshhs.MapPoly;

public class TerraMaster {
  public static final String LOGGER_CATEGORY = "org.flightgear";
  Logger log = Logger.getLogger(LOGGER_CATEGORY);
  public static ArrayList<MapPoly> polys, borders;

  static MapFrame frame;

  public static Map<TileName, TileData> mapScenery;

  /** The service getting the tiles */
  public static TileService svn;
  public static FGMap fgmap;
  public static Properties props;

  public static void addScnMapTile(Map<TileName, TileData> map, File i, TerraSyncDirectoryTypes type) {
    TileName n = TileName.getTile(i.getName());
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
    TerraSyncDirectoryTypes[] types = { TerraSyncDirectoryTypes.TERRAIN, TerraSyncDirectoryTypes.OBJECTS,
        TerraSyncDirectoryTypes.BUILDINGS };
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
     * worker_polys.execute(); worker_borders = new Worker<ArrayList<MapPoly>,
     * Void>("wdb_borders_l.b"); worker_borders.execute();
     */
    polys = new GshhsReader().newPolyList("maps/gshhs_l.b");
    borders = new GshhsReader().newPolyList("maps/wdb_borders_l.b");
    frame.passPolys(polys);
    frame.passBorders(borders);
    frame.addWindowListener(new WindowAdapter() {
      public void windowClosing(WindowEvent e) {
        svn.quit();
        frame.storeSettings();
        props.setProperty("LogLevel", log.getParent().getLevel().getName());
        try {
          props.store(new FileWriter("terramaster.properties"), null);
        } catch (Exception x) {
          log.log(Level.WARNING, "Couldn't store settings", e);
        }
        log.info("Shut down Terramaster");
      }
    });
  }

  public static void main(String args[]) {
    Properties p = new Properties();
    try {
      InputStream resourceAsStream = TerraMaster.class.getClassLoader()
          .getResourceAsStream("terramaster.logging.properties");
      if (resourceAsStream != null) {
        LogManager.getLogManager().readConfiguration(resourceAsStream);
        Logger.getLogger("java.awt").setLevel(Level.OFF);
        Logger.getLogger("sun.awt").setLevel(Level.OFF);
        Logger.getLogger("javax.swing").setLevel(Level.OFF);
        Logger.getGlobal().info("Successfully configured logging");
      }
    } catch (SecurityException | IOException e1) {
      e1.printStackTrace();
    }
    Logger LOG = Logger.getLogger(TerraMaster.class.getCanonicalName());
    try {
      InputStream resourceAsStream = TerraMaster.class.getResourceAsStream("/build_info.properties");
      if (resourceAsStream != null)
        p.load(resourceAsStream);
    } catch (Exception e1) {
      e1.printStackTrace();
    }
    readMetaINF();
    // Logger.getGlobal().setLevel(Level.ALL);

    fgmap = new FGMap(); // handles webqueries

    props = new Properties();
    try {
      props.load(new FileReader("terramaster.properties"));
      if (props.getProperty("LogLevel") != null) {
        Logger.getGlobal().getParent().setLevel(Level.INFO);
        Logger.getLogger(TerraMaster.LOGGER_CATEGORY).setLevel(Level.parse(props.getProperty("LogLevel")));
        Logger.getGlobal().getParent().setLevel(Level.INFO);
      }
    } catch (IOException e) {
      LOG.log(Level.WARNING, "Couldn't load properties : " + e.toString(), e);
    }
    LOG.info("Starting TerraMaster " + p.getProperty("build.major.number") + "." + p.getProperty("build.minor.number")
        + "_" + p.getProperty("build.number") + " " + p.getProperty("build.time"));

    setTileService();

    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        new TerraMaster().createAndShowGUI();
      }
    });
    if (props.getProperty("LogLevel") != null) {

      Level newLevel = Level.parse(props.getProperty("LogLevel"));
      LOG.getParent().setLevel(newLevel);
      LogManager manager = LogManager.getLogManager();
      Enumeration<String> loggers = manager.getLoggerNames();
      while (loggers.hasMoreElements()) {
        String logger = (String) loggers.nextElement();
        Logger logger2 = manager.getLogger(logger);
        if (logger2 != null && logger2.getLevel() != null) {
          logger2.setLevel(newLevel);
        }
      }

    }

  }

  private static void readMetaINF() {
    Logger LOG = Logger.getLogger(TerraMaster.class.getCanonicalName());
    try {
      Enumeration<URL> resources = TerraMaster.class.getClassLoader().getResources("META-INF/MANIFEST.MF");
      while (resources.hasMoreElements()) {
        try {
          Manifest manifest = new Manifest(resources.nextElement().openStream());
          // check that this is your manifest and do what you need or
          // get
          // the next one
          if ("TerraMasterLauncher".equals(manifest.getMainAttributes().getValue("Main-Class"))) {
            for (Entry<Object, Object> entry : manifest.getMainAttributes().entrySet()) {
              LOG.finest(entry.getKey() + "\t:\t" + entry.getValue());
            }
          }
        } catch (IOException E) {
          // handle
        }
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public static void setTileService() {
    String server_type = props.getProperty(TerraMasterProperties.SERVER_TYPE);
    if (svn == null) {
      svn = new HTTPTerraSync();
      svn.start();
    }
    svn.restoreSettings();
  }

}
