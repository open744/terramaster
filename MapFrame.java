import	java.io.*;
import	java.awt.*;
import	java.awt.image.*;
import	java.awt.geom.*;
import	java.util.*;
import	java.util.regex.*;
import	javax.swing.*;
import	javax.swing.border.*;
import	java.awt.event.*;
import	javax.swing.event.*;
import	com.jhlabs.map.proj.*;

public class MapFrame extends JFrame {

	public class MFAdapter extends ComponentAdapter
	    implements ActionListener {

	  public void componentResized(ComponentEvent e) {
	    tileName.setLocation(   0, 10);
	    butSync.setLocation(  120, 10);
	    butDelete.setLocation(220, 10);
	    butReset.setLocation(340, 0);
	    butPrefs.setLocation(380, 0);
	    map.setLocation(0, 40);
	    map.setSize(getWidth(), getHeight()-40);
	  }

	  public void actionPerformed(ActionEvent e) {
	    String	a = e.getActionCommand();

	    if (a.equals("SYNC")) {
	      TerraMaster.svn.sync(map.getSelection());
	      map.clearSelection();
	      repaint();
	    } else

	    if (a.equals("DELETE")) {
	      TerraMaster.svn.delete(map.getSelection());
	      map.clearSelection();
	      repaint();
	    } else

	    if (a.equals("RESET")) {
	      map.toggleProj();
	      repaint();
	    } else

	    if (a.equals("PREFS")) {
	      //JOptionPane.showInputDialog(butPrefs, "Path to scenery");
	      fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
	      if (fc.showOpenDialog(butPrefs) == JFileChooser.APPROVE_OPTION) {
		File	f = fc.getSelectedFile();
		fc.setCurrentDirectory(f);
		try {
		setTitle(f.getPath() + " - " + title);
		TerraMaster.mapScenery =
		    TerraMaster.newScnMap(f.getPath());
		repaint();
		TerraMaster.svn.setScnPath(f);
		TerraMaster.props.setProperty("SceneryPath", f.getPath());
		} catch (Exception x) {}
	      }
	    }
	  }
	}

  String	title;
  MapPanel	map;
  JTextField	tileName;
  JButton	butSync, butDelete, butReset, butPrefs;
  JFileChooser	fc = new JFileChooser();

  public MapFrame(String title) {
    MFAdapter	ad = new MFAdapter();

    this.title = title;
    setTitle(title);
    setLayout(null);
    getContentPane().addComponentListener(ad);

    tileName = new JTextField(8);
    tileName.setBounds(0, 20, 100, 20);
    add(tileName);

    butSync = new JButton("SYNC");
    butSync.setBounds(0, 80, 100, 20);
    butSync.addActionListener(ad);
    butSync.setActionCommand("SYNC");
    add(butSync);
    butDelete = new JButton("DELETE");
    butDelete.setBounds(0, 100, 100, 20);
    //butDelete.setEnabled(false);
    butDelete.addActionListener(ad);
    butDelete.setActionCommand("DELETE");
    add(butDelete);

    butReset = new JButton(new ImageIcon("globe.png"));
    butReset.setBounds(0, 400,  40, 40);
    butReset.addActionListener(ad);
    butReset.setActionCommand("RESET");
    add(butReset);

    butPrefs = new JButton(new ImageIcon("prefs.png"));
    butPrefs.setBounds(0, 400,  40, 40);
    butPrefs.addActionListener(ad);
    butPrefs.setActionCommand("PREFS");
    add(butPrefs);

    map = new MapPanel();
    add(map);

    map.passFrame(this);
  }


  public void passPolys(ArrayList<MapPoly> p) {
    map.passPolys(p);
  }

  // invoked from Svn thread
  public void doSvnUpdate(TileName n) {
    // XXX: paint just one 1x1
    map.repaint();
  }

}




class MapPanel extends JPanel {

	private Point2D.Double screen2geo(Point n) {
	  Point	s = new Point(n);
	  s.y += getY();
	  Point	p = new Point();

	  try {
	  affine.createInverse().transform(s, p);
	  Point2D.Double	dp = new Point2D.Double(p.x, p.y),
				dd = new Point2D.Double();
	  pj.inverseTransform(dp, dd);
	  return dd;
	  } catch (Exception x) {
	  return null;
	  }
	}

	class SortPoint extends Object implements Comparable<SortPoint> {
	  Point		p;
	  long		d;

	  SortPoint(Point pt, long l) {
	    p = pt; d = l;
	  }

	  public int compareTo(SortPoint l) {
	    return (int)(d - l.d);
	  }

	  public String toString() {
	    return new String(d + " " + p);
	  }
	}

	class SimpleMouseHandler extends MouseAdapter {
	  public void mouseClicked(MouseEvent e) {
	    TileName t = TerraMaster.tilenameManager.getTile(screen2geo(e.getPoint()));
	    projectionLatitude = Math.toRadians(-t.getLat());
	    projectionLongitude = Math.toRadians(t.getLon());
	    setOrtho();
	    mapFrame.repaint();
	  }

	  public void mouseWheelMoved(MouseWheelEvent e) {
	    int	n = e.getWheelRotation();
	    fromMetres -= n;
	    pj.setFromMetres(Math.pow(2, fromMetres/4));
	    pj.initialize();
	    //repaint();
	    mapFrame.repaint();
	  }
	}

	class MouseHandler extends MouseAdapter {
	  Point		press, last;
	  int		mode = 0;

	  public void mousePressed(MouseEvent e) {
	    press = e.getPoint();
	    mode = e.getButton();
	  }

	  public void mouseReleased(MouseEvent e) {
	    switch (e.getButton()) {
	    case MouseEvent.BUTTON3:
	      mouseReleasedPanning(e);
	      break;
	    case MouseEvent.BUTTON1:
	      mouseReleasedSelection(e);
	      break;
	    }
	  }

	  public void mouseReleasedPanning(MouseEvent e) {
	    if (!e.getPoint().equals(press)) {
	      mapFrame.repaint();
	    }
	    press = null;
	  }

	  public void mouseReleasedSelection(MouseEvent e) {
	    last = null;
	    dragbox = null;
	    Point2D.Double	d1 = screen2geo(press),
				d2 = screen2geo(e.getPoint());
	    if (d1 == null || d2 == null) return;

	    if ((e.getModifiersEx() & InputEvent.CTRL_DOWN_MASK) == 0)
	      selectionSet.clear();

	    int		x1 = (int)Math.floor(d1.x),
			y1 = (int)-Math.ceil(d1.y),
			x2 = (int)Math.floor(d2.x),
			y2 = (int)-Math.ceil(d2.y);
	    int		inc_i = (x2 > x1 ? 1 : -1),
			inc_j = (y2 > y1 ? 1 : -1);

	    // build a list of Points
	    ArrayList<SortPoint> l = new ArrayList<SortPoint>();
	    x2 += inc_i; y2 += inc_j;
	    for (int i = x1; i != x2; i += inc_i) {
	      for (int j = y1; j != y2; j += inc_j) {
		l.add(new SortPoint(
		    new Point(i, j),
		    (i-x1)*(i-x1) + (j-y1)*(j-y1))
		);
	      }
	    }
	    // sort by distance from x1,y1
	    Object[]	arr = l.toArray();
	    Arrays.sort(arr);

	    // finally, add the sorted list to selectionSet
	    for (Object t : arr) {
	      SortPoint p = (SortPoint)t;
	      TileName n = TerraMaster.tilenameManager.getTile(p.p.x, p.p.y);
	      if (!selectionSet.add(n))
		selectionSet.remove(n);		// remove on reselect
	    }

	    mapFrame.butDelete.setEnabled(true);

	    mapFrame.repaint();
	  }

	  public void mouseDragged(MouseEvent e) {
	    switch (mode) {
	    case MouseEvent.BUTTON3:
	      mouseDraggedPanning(e);
	      break;
	    case MouseEvent.BUTTON1:
	      mouseDraggedSelection(e);
	      break;
	    }
	  }

	  public void mouseDraggedPanning(MouseEvent e) {
	    Point2D.Double	d1 = screen2geo(press),
				d2 = screen2geo(e.getPoint());
	    if (d1 == null || d2 == null) return;

	    d2.x -= d1.x; d2.y -= d1.y;
	    projectionLatitude -= Math.toRadians(d2.y);
	    projectionLongitude -= Math.toRadians(d2.x);
	    press = e.getPoint();
	    pj.setProjectionLatitude(projectionLatitude);
	    pj.setProjectionLongitude(projectionLongitude);
	    pj.initialize();
	    mapFrame.repaint();
	  }

	  public void mouseDraggedSelection(MouseEvent e) {
	    last = e.getPoint();
	    if ((e.getModifiersEx() & InputEvent.CTRL_DOWN_MASK) == 0)
	      selectionSet.clear();
	    boxSelection(screen2geo(press), screen2geo(last));
	    mapFrame.repaint();
	  }

	  public void mouseClicked(MouseEvent e) {
	    Point2D.Double p2 = screen2geo(e.getPoint());

	    TileName tile = TerraMaster.tilenameManager.getTile(p2);
	    String txt = tile.getName();
	    mapFrame.tileName.setText(txt);
	    if (txt.equals("")) {
	      txt = "No selection";
	      mapFrame.butSync.setEnabled(false);
	    } else
	      mapFrame.butSync.setEnabled(true);

	    if (p2 == null) return;

	    mapFrame.butDelete.setEnabled(true);

	    /*
	    // not Ctrl-click, clear previous selection
	    if ((e.getModifiersEx() & InputEvent.CTRL_DOWN_MASK) == 0)
	      selectionSet.clear();
	    selectionSet.add(tile);
	    selection = true;
	    repaint();
	    */
	  }

	  public void mouseWheelMoved(MouseWheelEvent e) {
	    /*
	    // recentre
	    Point2D.Double d2 = screen2geo(e.getPoint());
	    if (d2 != null) {
	      projectionLatitude = Math.toRadians(d2.y);
	      projectionLongitude = Math.toRadians(d2.x);
	      pj.setProjectionLatitude(projectionLatitude);
	      pj.setProjectionLongitude(projectionLongitude);
	    }
	    */

	    int	n = e.getWheelRotation();
	    fromMetres -= n;
	    pj.setFromMetres(Math.pow(2, fromMetres/4));
	    pj.initialize();
	    //repaint(new Rectangle(0, 0, getWidth(), getHeight()));
	    mapFrame.repaint();
	  }
	}

	class MPAdapter extends ComponentAdapter {
	  public void componentResized(ComponentEvent e) {
	    int w = getWidth();
	    int h = getHeight();
	    double	r = pj.getEquatorRadius();
	    int i = (h < w ? h : w);	// the lesser dimension

	    sc = i / r / 2;
	    affine = new AffineTransform();
	    affine.translate(w/2, h/2);
	    affine.scale(sc, sc);
	    mapFrame.repaint();
	  }
	}

  private ArrayList<MapPoly>	poly;
  BufferedImage		map, grat;
  double		sc;
  MapFrame		mapFrame;
  AffineTransform	affine;
  MouseAdapter		mousehandler;
  boolean		isWinkel = true;

  Projection		pj;
  double	projectionLatitude = -Math.toRadians(-30),
		projectionLongitude = Math.toRadians(145),
		totalFalseEasting = 0,
		totalFalseNorthing = 0,
		fromMetres = 1,//3e-4,
		mapRadius = HALFPI;
  public final static int NORTH_POLE = 1;
  public final static int SOUTH_POLE = 2;
  public final static int EQUATOR = 3;
  public final static int OBLIQUE = 4;
  final static double EPS10 = 1e-10;
  final static double HALFPI = Math.PI / 2;
  final static double TWOPI = Math.PI*2.0;

  public MapPanel() {
    MPAdapter	ad = new MPAdapter();
    addComponentListener(ad);

    poly = new ArrayList<MapPoly>();
    //map = new BufferedImage(1600, 800, BufferedImage.TYPE_INT_RGB);
    //grat = new BufferedImage(1600, 800, BufferedImage.TYPE_4BYTE_ABGR);

    //setOrtho();
    setWinkel();

    setToolTipText("Hover for tile info");
    ToolTipManager tm = ToolTipManager.sharedInstance();
    tm.setDismissDelay(999999);
    tm.setInitialDelay(0);
    tm.setReshowDelay(0);
  }

  private void setOrtho()
  {
    pj = new OrthographicAzimuthalProjection();
    System.out.println(pj.getPROJ4Description());
    mapRadius = HALFPI - 0.1;
    isWinkel = false;

    /* depends on click
    projectionLatitude = -Math.toRadians(0);
    projectionLongitude = Math.toRadians(0);
    */
    pj.setProjectionLatitude(projectionLatitude);
    pj.setProjectionLongitude(projectionLongitude);
    fromMetres = 1;
    pj.setFromMetres(Math.pow(2, fromMetres/4));
    pj.initialize();

    double r = pj.getEquatorRadius();
    int w = getWidth();
    int h = getHeight();
    int i = (h < w ? h : w);	// the lesser dimension
    sc = i / r / 2;
    affine = new AffineTransform();
    affine.translate(w/2, h/2);
    affine.scale(sc, sc);

    removeMouseListener(mousehandler);
    mousehandler = new MouseHandler();
    addMouseWheelListener(mousehandler);
    addMouseListener(mousehandler);
    addMouseMotionListener(mousehandler);
  }

  private void setWinkel()
  {
    pj = new WinkelTripelProjection();
    System.out.println(pj.getPROJ4Description());
    mapRadius = TWOPI;
    isWinkel = true;

    projectionLatitude = -Math.toRadians(0);
    projectionLongitude = Math.toRadians(0);
    pj.setProjectionLatitude(projectionLatitude);
    pj.setProjectionLongitude(projectionLongitude);
    fromMetres = -5;
    pj.setFromMetres(Math.pow(2, fromMetres/4));
    pj.initialize();

    double r = pj.getEquatorRadius();
    int w = getWidth();
    int h = getHeight();
    int i = (h < w ? h : w);	// the lesser dimension
    sc = i / r / 2;
    affine = new AffineTransform();
    affine.translate(w/2, h/2);
    affine.scale(sc, sc);

    removeMouseWheelListener(mousehandler);
    removeMouseListener(mousehandler);
    removeMouseMotionListener(mousehandler);
    mousehandler = new SimpleMouseHandler();
    addMouseListener(mousehandler);

    clearSelection();
  }

  public void toggleProj()
  {
    if (isWinkel)
      setOrtho();
    else
      setWinkel();
  }

  void reset() {
    projectionLatitude = -Math.toRadians(-30);
    projectionLongitude = Math.toRadians(145);
    fromMetres = 1;
    pj.setProjectionLatitude(projectionLatitude);
    pj.setProjectionLongitude(projectionLongitude);
    pj.setFromMetres(Math.pow(2, fromMetres/4));
    pj.initialize();
  }




  public String getToolTipText(MouseEvent e) {
    Point s = e.getPoint();
    //return TerraMaster.tilenameManager.getTile(screen2geo(s)).getName();
    String txt = "";
    String str = "";

    TileName t = TerraMaster.tilenameManager.getTile(screen2geo(s));
    if (t != null) txt = t.getName();

    if (TerraMaster.mapScenery.containsKey(t)) {
      // list Terr, Obj, airports

      TileData d = TerraMaster.mapScenery.get(t);
      txt = "<html>" + txt;

      if (d.terrain) {
	txt += " +Terr";
	File f = d.dir_terr;
	int count = 0;
	for (String i : f.list()) {
	  if (i.endsWith(".btg.gz")) {
	    int n = i.indexOf('.');
	    if (n > 4) n = 4;
	    i = i.substring(0, n);
	    try {
	    Short.parseShort(i);
	    } catch (Exception x) {
	    str += i + " ";
	    if ((++count % 4) == 0)
	      str += "<br>";
	    }
	  }
	}
      }
      if (d.objects)
	txt += " +Obj";
      if (str.length() > 0)
	txt += "<br>" + str;

      txt += "</html>";
    }
    return txt;
  }

  public int polyCount() {
    return poly.size();
  }


  /*
    selection stuff
  */

  private boolean selection = false;
  private Collection<TileName> selectionSet = new LinkedHashSet<TileName>();
  private int[] dragbox;

  // capture all 1x1 boxes between press and last
  // (to be drawn by paint() later)
  private void boxSelection(Point2D.Double p1,
			    Point2D.Double p2) {
    if (p1 == null || p2 == null) {
      //selection = false;
      dragbox = null;
      return;
    }

    dragbox = new int[4];
    dragbox[0] = (int)Math.floor(p1.x);
    dragbox[1] = (int)-Math.ceil(p1.y);
    dragbox[2] = (int)Math.floor(p2.x);
    dragbox[3] = (int)-Math.ceil(p2.y);
    selection = true;
  }

  // returns union of selectionSet + dragbox
  Collection<TileName> getSelection() {
    Collection<TileName> selSet = new LinkedHashSet<TileName>(selectionSet);

    if (dragbox != null) {
      int l = dragbox[0];
      int b = dragbox[1];
      int r = dragbox[2];
      int t = dragbox[3];
      int inc_i = (r > l ? 1 : -1),
	  inc_j = (t > b ? 1 : -1);
      r += inc_i;
      t += inc_j;
      for (int i = l; i != r; i += inc_i) {
	for (int j = b; j != t; j += inc_j) {
	  TileName n = TerraMaster.tilenameManager.getTile(i, j);
	  if (!selSet.add(n))
	    selSet.remove(n);	// remove on reselect
	}
      }
    }
    return selSet;
  }

  void clearSelection() {
    selectionSet.clear();
    if (mapFrame != null) {
      mapFrame.butSync.setEnabled(false);
      mapFrame.butDelete.setEnabled(false);
    }
  }

  void showSelection(Graphics g) {
    Collection<TileName> a = getSelection();
    if (a == null) return;

    g.setColor(Color.red);
    for (TileName t : a) {
      Polygon p = box1x1(t.getLon(), t.getLat());
      if (p != null) g.drawPolygon(p);
    }
  }

  void showSyncList(Graphics g) {
    Collection<TileName> a = TerraMaster.svn.syncList;
    if (a == null) return;

    g.setColor(Color.cyan);
    for (TileName t : a) {
      Polygon p = box1x1(t.getLon(), t.getLat());
      if (p != null) g.drawPolygon(p);
    }
  }







  void drawGraticule(Graphics g, int sp) {
    int		x, y;
    Point2D.Double	p = new Point2D.Double();
    double	l, r, t, b;
    int		x4[] = new int[4],
		y4[] = new int[4];

    x = -180;
    while (x < 180) {
      y =  -70;
      while (y < 90) {
	l = Math.toRadians(x);
	b = Math.toRadians(y);
	r = Math.toRadians((double)x + sp);
	t = Math.toRadians((double)y - sp);

	if (inside(l, b)) {
	  project(l, b, p);
	  x4[0] = (int)p.x;
	  y4[0] = (int)p.y;
	  project(r, b, p);
	  x4[1] = (int)p.x;
	  y4[1] = (int)p.y;
	  project(r, t, p);
	  x4[2] = (int)p.x;
	  y4[2] = (int)p.y;
	  project(l, t, p);
	  x4[3] = (int)p.x;
	  y4[3] = (int)p.y;
	  g.drawPolygon(x4, y4, 4);
	}

	y += sp;
      }
      x += sp;
    }
  }

  // w and s are negative
  Polygon box1x1(int x, int y) {
    double	l, r, t, b;
    int		x4[] = new int[4],
		y4[] = new int[4];
    Point2D.Double	p = new Point2D.Double();

    l = Math.toRadians( x);
    b = Math.toRadians(-y);
    r = Math.toRadians((double) x + 1);
    t = Math.toRadians((double)-y - 1);

    if (!inside(l, b))
      return null;

    project(l, b, p);
    x4[0] = (int)p.x;
    y4[0] = (int)p.y;
    project(r, b, p);
    x4[1] = (int)p.x;
    y4[1] = (int)p.y;
    project(r, t, p);
    x4[2] = (int)p.x;
    y4[2] = (int)p.y;
    project(l, t, p);
    x4[3] = (int)p.x;
    y4[3] = (int)p.y;
    return new Polygon(x4, y4, 4);
  }

  void showTiles(Graphics g0) {
    Graphics2D g = (Graphics2D)g0;
    Color	color, bg = new Color(0, 0, 0, 0),
		grey  = new Color(128, 128, 128, 224),
		green = new Color( 64, 224,   0, 128),
		amber = new Color(192, 192,   0, 128);

    g.setBackground(bg);
    //g.clearRect(0, 0, 1600, 800);
    g.setTransform(affine);

    //g.setColor(grey);
    g.setColor(Color.gray);
    drawGraticule(g, 10);

    Set<TileName> keys = TerraMaster.mapScenery.keySet();
    Pattern	p = Pattern.compile("([ew])(\\p{Digit}{3})([ns])(\\p{Digit}{2})");

    for (TileName n: keys) {
      Matcher	m = p.matcher(n.getName());
      if (m.matches()) {
	int	lon = Integer.parseInt(m.group(2));
	int	lat = Integer.parseInt(m.group(4));
	lon = m.group(1).equals("w") ? -lon : lon;
	lat = m.group(3).equals("s") ? -lat : lat;

	Polygon	poly = box1x1(lon, lat);
	TileData t = TerraMaster.mapScenery.get(n);
	t.poly = poly;
	if (poly != null) {
	  if (t.terrain && t.objects)
	    //g.setColor(green);
	    g.setColor(Color.green);
	  else
	    //g.setColor(amber);
	    g.setColor(Color.yellow);
	  g.drawPolygon(poly);
	  //g.fillPolygon(poly);
	}
      }
    }
  }

  int abrl(int west, int north, Point2D p1, Point2D p2)
  {
    int a = 0;
    double x = west / 1000000.;
    double y = -north / 1000000.;
    if (x < p1.getX()) a |= 0x0001;
    if (x > p2.getX()) a |= 0x0010;
    if (y < p1.getY()) a |= 0x0100;
    if (y > p2.getY()) a |= 0x1000;
    return a;
  }

  // draws the landmass
  // filter by rect region
  void showLandmass_rect(Graphics g) {
    Graphics2D g2 = (Graphics2D)g;
    Color	sea  = new Color(0, 0,  64),
		land = new Color(64, 128, 0);
    Rectangle	r = g2.getClipBounds();
//System.out.format("showLandmass %s\n", r);
    g2.setColor(land);
    g2.setBackground(sea);
    g2.clearRect(r.x, r.y, r.width, r.height);
    g2.setTransform(affine);

    Point2D.Double p1 = screen2geo(new Point(r.x, r.y));
    Point2D.Double p2 = screen2geo(new Point(r.x+r.width, r.y+r.height));

    for (MapPoly s : poly) {
      int a1, a2;
      if (p1 != null && p2 != null) {
	a1 = abrl(s.gshhsHeader.west, s.gshhsHeader.north, p1, p2);
	a2 = abrl(s.gshhsHeader.east, s.gshhsHeader.south, p1, p2);
	if (a1 != a2 || (a1 & a2) == 0) {
	  MapPoly d = convertPoly(s);
	  g2.setColor(s.level % 2 == 1 ? land : sea);
	  if (d.npoints != 0)
	    g2.fillPolygon(d);
	}
      }
    }
  }

  // draws the landmass
  // filter by polygon size and zoom
  void showLandmass(Graphics g) {
    Graphics2D g2 = (Graphics2D)g;
    Color	sea  = new Color(0, 0,  64),
		land = new Color(64, 128, 0);
    Rectangle	r = g2.getClipBounds();
    g2.setColor(land);
    g2.setBackground(sea);
    g2.clearRect(r.x, r.y, r.width, r.height);
    g2.setTransform(affine);
    for (MapPoly s : poly) {
      if (s.gshhsHeader.n > 20/Math.pow(2, fromMetres/4)) {
	MapPoly d = convertPoly(s);
	g2.setColor(s.level % 2 == 1 ? land : sea);
	if (d.npoints != 0)
	  g2.fillPolygon(d);
      }
    }
  }

  // in: MapPoly
  // out: transformed new MapPoly
  MapPoly convertPoly(MapPoly s) {
    int			i;
    Point2D.Double	p = new Point2D.Double();
    MapPoly		d = new MapPoly();

    for (i = 0; i < s.npoints; ++i) {
      double	x = s.xpoints[i],
		y = s.ypoints[i];
      x = Math.toRadians(x / 100.0);
      y = Math.toRadians(y / 100.0);
      if (inside(x, y)) {
	project(x, y, p);
	d.addPoint((int)p.x, (int)p.y);
      } else {
	// XXX
      }
    }
    return d;
  }

  double greatCircleDistance(double lon1, double lat1,
			     double lon2, double lat2 ) {
    double dlat = Math.sin((lat2-lat1)/2);
    double dlon = Math.sin((lon2-lon1)/2);
    double r = Math.sqrt(dlat*dlat + Math.cos(lat1)*Math.cos(lat2)*dlon*dlon);
    return 2.0 * Math.asin(r);
  }

  boolean inside(double lon, double lat) {
    return greatCircleDistance(lon, lat,
	projectionLongitude, projectionLatitude) < mapRadius;
  }

  void project(double lam, double phi, Point2D.Double d) {
    Point2D.Double	s = new Point2D.Double(lam, phi);
    pj.transformRadians(s, d);
  }

  void passFrame(MapFrame f) {
    mapFrame = f;
  }

  void passPolys(ArrayList<MapPoly> p) {
    poly = p;
    mapFrame.repaint();
  }

  public void paintComponent(Graphics g) {
    super.paintComponent(g);
    showLandmass(g);
    showTiles(g);
    showSelection(g);
    showSyncList(g);

// crosshair
{Graphics2D g2 = (Graphics2D)g;
g2.setTransform(new AffineTransform());
g2.setColor(Color.white);
g2.drawLine(getWidth()/2-50, getHeight()/2, getWidth()/2+50, getHeight()/2);
g2.drawLine(getWidth()/2, getHeight()/2-50, getWidth()/2, getHeight()/2+50);
}

    /*
    Graphics2D	g2 = (Graphics2D)g;
    Dimension	d = getSize(null);
    showLandmass(map.createGraphics());
    showTiles(grat.createGraphics());
    g.drawImage( map, 0, 0, d.width, d.height, 0, 0, d.width, d.height, this);
    g.drawImage(grat, 0, 0, d.width, d.height, 0, 0, d.width, d.height, this);
    */
  }
}
