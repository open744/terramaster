import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Collection;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JProgressBar;
import javax.swing.JTextField;

public class MapFrame extends JFrame {

	// this Adapter is used by the child elements
	public class MFAdapter extends ComponentAdapter
	    implements ActionListener {

	  public void componentMoved(ComponentEvent e) {
	    updateGeom();
	  }

	  public void componentResized(ComponentEvent e) {
	    tileName.setLocation( 20, 10);
	    butSync.setLocation(  90-1, 7);
	    butDelete.setLocation(115-1, 7);
	    butSearch.setLocation(140-1, 7);
	    butStop.setLocation(165-1, 7);
            butModels.setLocation(190-1, 7);
	    butClear.setLocation(220-1, 7);
	    butReset.setLocation(245-1, 7);
	    butPrefs.setLocation(270-1, 7);
	    search.setLocation(300, 10);
	    searchBar.setLocation(345, 10);
	    progressBar.setLocation(470, 9);
	    map.setLocation(0, 40);
	    map.setSize(getWidth(), getHeight()-40);
	    updateGeom();
	  }
	  

	  public void actionPerformed(ActionEvent e) {
	    String	a = e.getActionCommand();

	    if (a.equals("SYNC")) {
	      Collection<TileName> set = map.getSelection();
	      TerraMaster.svn.sync(set);
	      progressBar.setMaximum(progressBar.getMaximum() + set.size() * 2);
	      progressBar.setVisible(true);
	      butStop.setEnabled(true);
	      map.clearSelection();
	      repaint();
	    } else

            if (a.equals("MODELS")) {
	      Collection<TileName> set = new ArrayList<TileName>();
              set.add(new TileName("MODELS"));
	      TerraMaster.svn.sync(set);
	      progressBar.setMaximum(progressBar.getMaximum() + set.size() * 1);
	      progressBar.setVisible(true);
	      butStop.setEnabled(true);
	    } else

	    if (a.equals("DELETE")) {
	      TerraMaster.svn.delete(map.getSelection());
	      map.clearSelection();
	      repaint();
	    } else

	    if (a.equals("RESET")) {
	      map.toggleProj();
	      //map.clearSelection();
	      repaint();
	    } else

	    if (a.equals("STOP")) {
	      TerraMaster.svn.cancel();
	      //repaint();
	    } else

	    if (a.equals("CLEAR")) {
	      TerraMaster.fgmap.clearAirports();
	      repaint();
	    } else

			if (a.equals("PREFS")) {
				SettingsDialog settingsDialog = new SettingsDialog();
				settingsDialog
						.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
				
				settingsDialog.setVisible(true);
				map.repaint();
			}

	    if (a.equals("SEARCH")) {
	      String str = searchBar.getText();
	      new WebWorker(str).execute();
	    }

	    if (a.equals("BROWSE")) {
	      Collection<TileName> sel = map.getSelection();
	      new WebWorker(sel).execute();
	    }

	  }
	}

  String	title;
  MapPanel	map;
  JTextField	searchBar;
  JLabel	tileName, search;
  JButton	butSync, butDelete, butStop, butModels, butReset, butClear, butPrefs, butSearch;
  JFileChooser	fc = new JFileChooser();
  JProgressBar	progressBar;

  public MapFrame(String title) {
    MFAdapter ad = new MFAdapter();

    this.title = title;
    setTitle(title);
    setLayout(null);
    getContentPane().addComponentListener(ad);

    addWindowListener(new WindowAdapter() {
	public void windowClosing(WindowEvent e) {
	  TerraMaster.svn.quit();
	  updateGeom();
	  try {
	  TerraMaster.props.store(new FileWriter("terramaster.properties"),
	      null);
	  } catch (Exception x) { }
	}
      });

    tileName = new JLabel();
    tileName.setBounds(0, 0, 60, 20);
    tileName.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 10));
    add(tileName);

    butSync = new JButton(new ImageIcon(getClass().getClassLoader().getResource("Sync.png")));
    butSync.setBounds(0, 0, 26, 26);
    butSync.setEnabled(false);
    butSync.addActionListener(ad);
    butSync.setActionCommand("SYNC");
    butSync.setToolTipText("Synchronise selected tiles");
    add(butSync);

    butDelete = new JButton(new ImageIcon(getClass().getClassLoader().getResource("Trash.png")));
    butDelete.setBounds(0, 0, 26, 26);
    butDelete.setEnabled(false);
    butDelete.addActionListener(ad);
    butDelete.setActionCommand("DELETE");
    butDelete.setToolTipText("Delete selected tiles from disk");
    add(butDelete);

    butSearch = new JButton(new ImageIcon("Eye.png"));
    butSearch.setBounds(0, 0,  26, 26);
    butSearch.setEnabled(false);
    butSearch.addActionListener(ad);
    butSearch.setActionCommand("BROWSE");
    butSearch.setToolTipText("Show airports within selected tiles");
    add(butSearch);

    butStop = new JButton(new ImageIcon(getClass().getClassLoader().getResource("Stop sign.png")));
    butStop.setBounds(0, 0, 26, 26);
    butStop.setEnabled(false);
    butStop.addActionListener(ad);
    butStop.setActionCommand("STOP");
    butStop.setToolTipText("Stop all queued syncs");
    add(butStop);

    butModels = new JButton(new ImageIcon(getClass().getClassLoader().getResource("Company.png")));
    butModels.setBounds(0, 0, 26, 26);
    butModels.setEnabled(true);
    butModels.addActionListener(ad);
    butModels.setActionCommand("MODELS");
    butModels.setToolTipText("Synchronise shared models");
    add(butModels);

    butClear = new JButton(new ImageIcon(getClass().getClassLoader().getResource("New document.png")));
    butClear.setBounds(0, 0,  26, 26);
    butClear.addActionListener(ad);
    butClear.setActionCommand("CLEAR");
    butClear.setToolTipText("Clear all airports from map");
    add(butClear);

    butReset = new JButton(new ImageIcon(getClass().getClassLoader().getResource("globe.png")));
    butReset.setBounds(0, 0,  26, 26);
    butReset.addActionListener(ad);
    butReset.setActionCommand("RESET");
    butReset.setToolTipText("Toggle between projections");
    add(butReset);

    butPrefs = new JButton(new ImageIcon(getClass().getClassLoader().getResource("application.png")));
    butPrefs.setBounds(0, 0,  26, 26);
    butPrefs.addActionListener(ad);
    butPrefs.setActionCommand("PREFS");
    butPrefs.setToolTipText("Select scenery folder");
    add(butPrefs);

    search = new JLabel("Search:");
    search.setBounds(0, 0, 60, 20);
    search.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 10));
    add(search);

    searchBar = new JTextField();
    searchBar.setBounds(0, 0, 80, 20);
    searchBar.addActionListener(ad);
    searchBar.setActionCommand("SEARCH");
    searchBar.setToolTipText("Search for airport by name or code");
    add(searchBar);

    progressBar = new JProgressBar();
    progressBar.setBounds(0, 0, 120, 20);
    progressBar.setVisible(false);
    progressBar.setStringPainted(true);
    progressBar.setMaximum(0);
    add(progressBar);

    map = new MapPanel();
    add(map);

    map.passFrame(this);

    /*
    // keyboard accels
    getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(^S), "SYNC");
    getActionMap().put("
    */
  }

  private void updateGeom() {
    TerraMaster.props.setProperty("Geometry",
	String.format("%dx%d%+d%+d", getWidth(), getHeight(),
	getX(), getY()));
  }

  public void passPolys(ArrayList<MapPoly> p) {
    map.passPolys(p);
    repaint();
  }

  public void passBorders(ArrayList<MapPoly> p) {
    map.passBorders(p);
    repaint();
  }

  // called from Svn thread
  public void progressUpdate(int n) {
    progressBar.setValue(progressBar.getValue() + n);
    progressBar.setToolTipText("" + progressBar.getValue() + " / " + progressBar.getMaximum());
    repaint();
  }
  
  @Override
  public void setVisible(boolean b) {
    super.setVisible(b);
    if (b && TerraMaster.mapScenery == null)
    {
      JOptionPane.showMessageDialog(this, "Scenery folder not found. Click the gear icon and select the folder containing your scenery files.", "Warning", JOptionPane.WARNING_MESSAGE);
    } else
    if (b && TerraMaster.mapScenery.isEmpty() )
    {
      JOptionPane.showMessageDialog(this, "Scenery folder is empty.", "Warning", JOptionPane.WARNING_MESSAGE);
    }
  }

}
