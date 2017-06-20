import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.border.MatteBorder;

public class MapFrame extends JFrame {

  // this Adapter is used by the child elements
  public class MFAdapter extends ComponentAdapter implements ActionListener {

    public void componentMoved(ComponentEvent e) {
      storeSettings();
    }

    public void componentResized(ComponentEvent e) {
      // tileName.setLocation( 20, 10);
      // butSync.setLocation( 90-1, 7);
      // butDelete.setLocation(115-1, 7);
      // butSearch.setLocation(140-1, 7);
      // butStop.setLocation(165-1, 7);
      // butModels.setLocation(190-1, 7);
      // butClear.setLocation(220-1, 7);
      // butReset.setLocation(245-1, 7);
      // butPrefs.setLocation(270-1, 7);
      // search.setLocation(300, 10);
      // searchBar.setLocation(345, 10);
      // progressBar.setLocation(470, 9);
      // map.setLocation(0, 40);
      map.setSize(getWidth(), getHeight() - 40);
      storeSettings();
    }

    public void actionPerformed(ActionEvent e) {
      String a = e.getActionCommand();

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
        // map.clearSelection();
        repaint();
      } else

      if (a.equals("STOP")) {
        TerraMaster.svn.cancel();
        // repaint();
      } else

      if (a.equals("CLEAR")) {
        TerraMaster.fgmap.clearAirports();
        repaint();
      } else

      if (a.equals("PREFS")) {
        SettingsDialog settingsDialog = new SettingsDialog();
        settingsDialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        settingsDialog.setVisible(true);
        map.repaint();
      } else

      if (a.equals("SEARCH")) {
        String str = searchBar.getText();
        new WebWorker(str).execute();
      } else

      if (a.equals("BROWSE")) {
        Collection<TileName> sel = map.getSelection();
        new WebWorker(sel).execute();
      } else {

      }

    }
  }

  String title;
  MapPanel map;
  JTextField searchBar;
  JLabel tileName, search;
  JButton butSync, butDelete, butStop, butModels, butReset, butClear, butPrefs,
      butSearch;
  JFileChooser fc = new JFileChooser();
  JProgressBar progressBar;
  private JPanel panel;
  Logger log = Logger.getLogger(this.getClass().getName());
  private JButton butInfo;

  public MapFrame(String title) {
    try {
      MFAdapter ad = new MFAdapter();

      this.title = title;
      setTitle(title);
      getContentPane().addComponentListener(ad);

      getContentPane().setLayout(new BorderLayout(0, 0));

      panel = new JPanel();

      panel.setBorder(new MatteBorder(2, 2, 2, 2, (Color) UIManager
          .getColor("Panel.background")));
      getContentPane().add(panel, BorderLayout.NORTH);
      GridBagLayout gbl_panel = new GridBagLayout();
      gbl_panel.columnWidths = new int[] { 63, 57, 57, 57, 57, 57, 57, 57, 10,
          30, 30, 0 };
      gbl_panel.rowHeights = new int[] { 33, 19, 0 };
      gbl_panel.columnWeights = new double[] { 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
          0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0 };
      gbl_panel.rowWeights = new double[] { 0.0, 0.0, Double.MIN_VALUE };
      panel.setLayout(gbl_panel);

      tileName = new JLabel();
      tileName.setHorizontalAlignment(SwingConstants.CENTER);
      tileName.setText("         ");
      GridBagConstraints gbc_tileName = new GridBagConstraints();
      gbc_tileName.anchor = GridBagConstraints.WEST;
      gbc_tileName.insets = new Insets(0, 0, 5, 5);
      gbc_tileName.gridx = 0;
      gbc_tileName.gridy = 0;
      panel.add(tileName, gbc_tileName);
      tileName.setFont(new Font("SansSerif", Font.BOLD, 16));

      butSync = new JButton(new ImageIcon(getClass().getClassLoader()
          .getResource("Sync.png")));
      GridBagConstraints gbc_butSync = new GridBagConstraints();
      gbc_butSync.anchor = GridBagConstraints.NORTHWEST;
      gbc_butSync.insets = new Insets(0, 0, 5, 5);
      gbc_butSync.gridx = 1;
      gbc_butSync.gridy = 0;
      panel.add(butSync, gbc_butSync);
      butSync.setEnabled(false);
      butSync.addActionListener(ad);
      butSync.setActionCommand("SYNC");
      butSync.setToolTipText("Synchronise selected tiles");

      butDelete = new JButton(new ImageIcon(getClass().getClassLoader()
          .getResource("Trash.png")));
      GridBagConstraints gbc_butDelete = new GridBagConstraints();
      gbc_butDelete.anchor = GridBagConstraints.NORTHWEST;
      gbc_butDelete.insets = new Insets(0, 0, 5, 5);
      gbc_butDelete.gridx = 2;
      gbc_butDelete.gridy = 0;
      panel.add(butDelete, gbc_butDelete);
      butDelete.setEnabled(false);
      butDelete.addActionListener(ad);
      butDelete.setActionCommand("DELETE");
      butDelete.setToolTipText("Delete selected tiles from disk");

      butSearch = new JButton(new ImageIcon(
          MapFrame.class.getResource("Eye.png")));
      GridBagConstraints gbc_butSearch = new GridBagConstraints();
      gbc_butSearch.anchor = GridBagConstraints.NORTHWEST;
      gbc_butSearch.insets = new Insets(0, 0, 5, 5);
      gbc_butSearch.gridx = 3;
      gbc_butSearch.gridy = 0;
      panel.add(butSearch, gbc_butSearch);
      butSearch.setEnabled(false);
      butSearch.addActionListener(ad);
      butSearch.setActionCommand("BROWSE");
      butSearch.setToolTipText("Show airports within selected tiles");

      butStop = new JButton(new ImageIcon(getClass().getClassLoader()
          .getResource("Stop sign.png")));
      GridBagConstraints gbc_butStop = new GridBagConstraints();
      gbc_butStop.anchor = GridBagConstraints.NORTHWEST;
      gbc_butStop.insets = new Insets(0, 0, 5, 5);
      gbc_butStop.gridx = 4;
      gbc_butStop.gridy = 0;
      panel.add(butStop, gbc_butStop);
      butStop.setEnabled(false);
      butStop.addActionListener(ad);
      butStop.setActionCommand("STOP");
      butStop.setToolTipText("Stop all queued syncs");

      butModels = new JButton(new ImageIcon(getClass().getClassLoader()
          .getResource("Company.png")));
      GridBagConstraints gbc_butModels = new GridBagConstraints();
      gbc_butModels.anchor = GridBagConstraints.NORTHWEST;
      gbc_butModels.insets = new Insets(0, 0, 5, 5);
      gbc_butModels.gridx = 5;
      gbc_butModels.gridy = 0;
      panel.add(butModels, gbc_butModels);
      butModels.setEnabled(true);
      butModels.addActionListener(ad);
      butModels.setActionCommand("MODELS");
      butModels.setToolTipText("Synchronise shared models");

      butClear = new JButton(new ImageIcon(getClass().getClassLoader()
          .getResource("New document.png")));
      GridBagConstraints gbc_butClear = new GridBagConstraints();
      gbc_butClear.anchor = GridBagConstraints.NORTHWEST;
      gbc_butClear.insets = new Insets(0, 0, 5, 5);
      gbc_butClear.gridx = 6;
      gbc_butClear.gridy = 0;
      panel.add(butClear, gbc_butClear);
      butClear.addActionListener(ad);
      butClear.setActionCommand("CLEAR");
      butClear.setToolTipText("Clear all airports from map");

      butReset = new JButton(new ImageIcon(getClass().getClassLoader()
          .getResource("globe.png")));
      
      butReset.setPreferredSize(butClear.getPreferredSize());
      GridBagConstraints gbc_butReset = new GridBagConstraints();
      gbc_butReset.anchor = GridBagConstraints.NORTHWEST;
      gbc_butReset.insets = new Insets(0, 0, 5, 5);
      gbc_butReset.gridx = 7;
      gbc_butReset.gridy = 0;
      panel.add(butReset, gbc_butReset);
      butReset.addActionListener(ad);
      butReset.setActionCommand("RESET");
      butReset.setToolTipText("Toggle between projections");

      butPrefs = new JButton(new ImageIcon(getClass().getClassLoader()
          .getResource("application.png")));
      GridBagConstraints gbc_butPrefs = new GridBagConstraints();
      gbc_butPrefs.anchor = GridBagConstraints.NORTH;
      gbc_butPrefs.insets = new Insets(0, 0, 5, 5);
      gbc_butPrefs.gridx = 8;
      gbc_butPrefs.gridy = 0;
      panel.add(butPrefs, gbc_butPrefs);
      butPrefs.addActionListener(ad);
      butPrefs.setActionCommand("PREFS");
      butPrefs.setToolTipText("Properties");

      search = new JLabel("Search:");
      GridBagConstraints gbc_search = new GridBagConstraints();
      gbc_search.anchor = GridBagConstraints.WEST;
      gbc_search.insets = new Insets(0, 0, 5, 5);
      gbc_search.gridx = 9;
      gbc_search.gridy = 0;
      panel.add(search, gbc_search);
      search.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 10));

      searchBar = new JTextField();
      searchBar.setText("");
      GridBagConstraints gbc_searchBar = new GridBagConstraints();
      gbc_searchBar.fill = GridBagConstraints.HORIZONTAL;
      gbc_searchBar.gridwidth = 2;
      gbc_searchBar.insets = new Insets(0, 0, 5, 5);
      gbc_searchBar.gridx = 10;
      gbc_searchBar.gridy = 0;
      panel.add(searchBar, gbc_searchBar);
      searchBar.addActionListener(ad);
      searchBar.setActionCommand("SEARCH");
      searchBar.setToolTipText("Search for airport by name or code");
      
      butInfo = new JButton(new ImageIcon(getClass().getClassLoader()
              .getResource("Question.png")));
      butInfo.addActionListener(new ActionListener() {
      	public void actionPerformed(ActionEvent e) {
      		AboutDialog dialog = new AboutDialog();
      		dialog.setVisible(true);
      	}
      });
      butInfo.setToolTipText("About");
      butInfo.setActionCommand("PREFS");
      GridBagConstraints gbc_butInfo = new GridBagConstraints();
      gbc_butInfo.anchor = GridBagConstraints.NORTH;
      gbc_butInfo.insets = new Insets(0, 0, 5, 0);
      gbc_butInfo.gridx = 12;
      gbc_butInfo.gridy = 0;
      panel.add(butInfo, gbc_butInfo);

      progressBar = new JProgressBar();
      GridBagConstraints gbc_progressBar = new GridBagConstraints();
      gbc_progressBar.fill = GridBagConstraints.HORIZONTAL;
      gbc_progressBar.anchor = GridBagConstraints.NORTH;
      gbc_progressBar.gridwidth = 13;
      gbc_progressBar.gridx = 0;
      gbc_progressBar.gridy = 1;
      panel.add(progressBar, gbc_progressBar);
      progressBar.setVisible(false);
      progressBar.setStringPainted(true);
      progressBar.setMaximum(0);

      map = new MapPanel();
      addKeyListener(new KeyAdapter() {
    	    
    	    @Override
    	    public void keyReleased(KeyEvent e) {
    	    	System.out.println(e.getKeyChar());
    	    }
    	    
        	@Override
        	public void keyPressed(KeyEvent e) {
        		if(e.getKeyCode() == KeyEvent.VK_SHIFT	)
        			System.out.println("DD");
        		map.keyEvent(e);
        	}
        });
      getContentPane().add(map, BorderLayout.CENTER);

      map.passFrame(this);
    } catch (Throwable e) {
      log.log(Level.SEVERE, "Couldn't show MapFrame", e);
    }

    /*
     * // keyboard accels
     * getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke
     * .getKeyStroke(^S), "SYNC"); getActionMap().put("
     */
  }
  
  public void restoreSettings(){
		String geom = TerraMaster.props.getProperty(TerraMasterProperties.GEOMETRY);
		int w = 800;
		int h = 600;
		int x = 0;
		int y = 0;
		Pattern pattern = Pattern.compile("([0-9]+)x([0-9]+)([+-][0-9]+)([+-][0-9]+)");
		if (geom != null) {
			Matcher matcher = pattern.matcher(geom);
			if (matcher.find()) {
				w = Integer.parseInt(matcher.group(1));
				h = Integer.parseInt(matcher.group(2));
				x = Integer.parseInt(matcher.group(3).replaceFirst("\\+", ""));
				y = Integer.parseInt(matcher.group(4).replaceFirst("\\+", ""));
			}
		}
		setSize(w, h);
		setLocation(x, y);	  
		
	    map.projectionLatitude = Double.parseDouble(TerraMaster.props.getProperty(TerraMasterProperties.PROJECTION_LAT, "0"));
	    map.projectionLongitude = Double.parseDouble(TerraMaster.props.getProperty(TerraMasterProperties.PROJECTION_LON, "0"));
        map.setProjection(Boolean.parseBoolean(TerraMaster.props.getProperty(TerraMasterProperties.PROJECTION, "false")));
	    map.fromMetres = Double.parseDouble(TerraMaster.props.getProperty(TerraMasterProperties.FROM_METRES, "1"));
	    map.setFromMetres();
  }

  public void storeSettings() {
    TerraMaster.props.setProperty(TerraMasterProperties.GEOMETRY,
        String.format("%dx%d%+d%+d", getWidth(), getHeight(), getX(), getY()));
    TerraMaster.props.setProperty(TerraMasterProperties.PROJECTION, Boolean.toString(map.isWinkel));
    TerraMaster.props.setProperty(TerraMasterProperties.PROJECTION_LAT, Double.toString(map.projectionLatitude));
    TerraMaster.props.setProperty(TerraMasterProperties.PROJECTION_LON, Double.toString(map.projectionLongitude));
    TerraMaster.props.setProperty(TerraMasterProperties.FROM_METRES, Double.toString(map.fromMetres));
  }

  public void passPolys(ArrayList<MapPoly> p) {
    map.passPolys(p);
    repaint();
  }

  public void passBorders(ArrayList<MapPoly> p) {
    map.passBorders(p);
    repaint();
  }

  /**
   * called from Svn thread
   * @param n
   */
  public void progressUpdate(int n) {
    progressBar.setValue(progressBar.getValue() + n);
    progressBar.setToolTipText("" + progressBar.getValue() + " / "
        + progressBar.getMaximum());
    repaint();
  }

  @Override
  public void setVisible(boolean b) {
    super.setVisible(b);
    if (b && TerraMaster.mapScenery == null) {
      JOptionPane
          .showMessageDialog(
              this,
              "Scenery folder not found. Click the gear icon and select the folder containing your scenery files.",
              "Warning", JOptionPane.WARNING_MESSAGE);
    } else if (b && TerraMaster.mapScenery.isEmpty()) {
      JOptionPane.showMessageDialog(this, "Scenery folder is empty.",
          "Warning", JOptionPane.WARNING_MESSAGE);
    }
  }

}
