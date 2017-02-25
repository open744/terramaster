import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.io.File;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.tmatesoft.svn.core.wc.*;
import org.tmatesoft.svn.core.ISVNCanceller;
import org.tmatesoft.svn.core.ISVNDirEntryHandler;
import org.tmatesoft.svn.core.SVNNodeKind;
import org.tmatesoft.svn.core.SVNDirEntry;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNCancelException;
import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.internal.io.dav.DAVRepositoryFactory;

// svn --force co http://terrascenery.googlecode.com/svn/trunk/data/Scenery/Terrain/e100n00/e104n00

/**
 * @deprecated SVN will be discontinued
 */

class Svn extends Thread implements ISVNDirEntryHandler, ISVNCanceller,
    ISVNExternalsHandler, TileService {
  Logger LOG = Logger.getLogger(this.getClass().getName());

  SVNClientManager clientManager;
  SVNUpdateClient updateClient;
  SVNStatusClient statusClient;
  SVNLogClient logClient;
  SVNWCClient wcClient;
  String urlBase;
  String pathBase;
  long syncsize, synccount;
  boolean cancelFlag = false;

  LinkedList<TileName> syncList;

  Svn() {
    super("SVN-Thread");
    DAVRepositoryFactory.setup();
    clientManager = SVNClientManager.newInstance();
    clientManager.setCanceller(this);
    updateClient = clientManager.getUpdateClient();
    statusClient = clientManager.getStatusClient();
    logClient = clientManager.getLogClient();
    wcClient = clientManager.getWCClient();
    updateClient.setIgnoreExternals(false);

    syncList = new LinkedList<TileName>();

    // externals stuff
    updateClient.setExternalsHandler(this);

    checkURL();
  }

  private synchronized void checkURL() {
    if (urlBase != null)
      return;
    // 2015-12-28 use FG scenery server redirector
    try {
      char[] buf = new char[256];
      new BufferedReader(new InputStreamReader(new java.net.URL(
          "http://scenery.flightgear.org/svn-server").openStream())).read(buf,
          0, 256);
      urlBase = new String(buf).trim();
      LOG.info("SVN server is " + urlBase);
    } catch (IOException e) {
      LOG.log(Level.WARNING, e.toString());
    }
  }

  // this implements ISVNExternalsHandler
  public SVNRevision[] handleExternal(java.io.File externalPath,
      SVNURL externalURL, SVNRevision externalRevision,
      SVNRevision externalPegRevision, java.lang.String externalsDefinition,
      SVNRevision externalsWorkingRevision) {
    String
        .format("Externals: %d %d -> ", externalRevision, externalPegRevision);
    LOG.info(String.format("Externals: %d %d -> ", externalRevision,
        externalPegRevision));
    SVNRevision[] ret = ISVNExternalsHandler.DEFAULT.handleExternal(
        externalPath, externalURL, externalRevision, externalPegRevision,
        externalsDefinition, externalsWorkingRevision);
    LOG.info(ret.toString());
    return ret;
  }

  // this implements ISVNCanceller
  public void checkCancelled() throws SVNCancelException {
    if (cancelFlag) {
      cancelFlag = false;
      invokeLater(1); // reset progressBar
      throw new SVNCancelException();
    }
  }

  // checks that f exists and is versioned, otherwise `svn checkout --depth
  // empty url`
  private void verifyRoot(File f, SVNURL url) throws SVNException {
    try {
      boolean stat = statusClient.doStatus(f, false).isVersioned();
      // LOG.info(String.format(("Info: %s exists and is versioned.\n",
      // f.toString());
    } catch (SVNException e) {
      // allowUnversionedObstructions = true
      long rev = updateClient.doCheckout(url, f, SVNRevision.HEAD,
          SVNRevision.HEAD, SVNDepth.EMPTY, true);
      LOG.info(String.format("Created working copy %s.\n", f.toString()));
    }
  }

  // `svn update --parents f`
  private long updateNode(File f) throws SVNException {
    File[] list = new File[1];
    list[0] = f;
    // allowUnversionedObstructions = true, depthIsSticky = T, makeParents = T
    long[] rev = updateClient.doUpdate(list, SVNRevision.HEAD,
        SVNDepth.INFINITY, true, true, true);
    return rev[0];
  }

  // this implements ISVNDirEntryHandler
  public void handleDirEntry(SVNDirEntry e) {
    syncsize += e.getSize();
    ++synccount;
    // SVNNodeKind typ = e.getKind();
    // e.DIRENT_KIND.equals(SVNNodeKind.DIR)
  }

  // svn list --verbose --> fills up syncsize/synccount
  private void printStats(String path) {
    try {
      SVNURL url = SVNURL.parseURIEncoded(urlBase + path);
      synccount = syncsize = 0;
      logClient.doList(url, SVNRevision.HEAD, SVNRevision.HEAD, false, true,
          this);
      LOG.info(String.format("%s/%d items (%d bytes)... ", path, synccount,
          syncsize));
    } catch (SVNException e) {
      SVNErrorMessage em = e.getErrorMessage();
      if (em.getErrorCode().getCode() == 160013)
        LOG.warning(String.format("%s not found\n", path));
      else
        LOG.warning(String.format("doList: %s\n", e.getMessage()));
    }
  }

  /*
   * SVNStatus stat = statusClient.doStatus(pathBase +
   * "Terrain/e140s40/e144s38/"), true); LOG.info(stat.getURL());
   */

  // syncs the tile's Terrain then Objects, then any Airports within
  // name : "e120s20/e123s11"
  private void checkout(String name) {
    String[] types = { "Terrain/", "Objects/" };
    TerraSyncDirectoryTypes[] ntype = { TerraSyncDirectoryTypes.TERRAIN, TerraSyncDirectoryTypes.OBJECTS };
    if (pathBase == null) {
      JOptionPane
          .showMessageDialog(TerraMaster.frame, "TerraSync path not set");
      return;
    }

    // make sure the working copy root is valid
    try {
      verifyRoot(new File(pathBase), SVNURL.parseURIEncoded(urlBase));
    } catch (SVNException e) {
      LOG.log(Level.WARNING, e.toString(), e);
    }

    LOG.info("sync " + name + "... ");

    for (int i = 0; i < types.length; ++i) {
      String node = types[i] + name;

      try {

        File f = new File(pathBase + node);

        printStats(node);
        long rev = updateNode(f);
        invokeLater(2); // update progressBar

        if (rev > 0) {
          LOG.info(String.format("updated to r%d.\n", rev));
          TerraMaster.addScnMapTile(TerraMaster.mapScenery, f, ntype[i]);

          // look for airport codes among the newly sync'd Terrain files
          if (i == 0 & f.exists()) {
            String[] apt = findAirports(f);
            if (apt != null) {
              for (int j = 0; j < apt.length; ++j)
                invokeLater(3); // extend progressBar
              syncAirports(apt);
            }
          }
        }

      } catch (SVNException x) {
        invokeLater(2); // update progressBar
        SVNErrorMessage em = x.getErrorMessage();
        // E160013 = URL not found
        // if (em.getErrorCode().getCode() != 160013)
        LOG.info(x.getMessage());
        // 155004 unfinished work items, need svn cleanup
      } catch (Exception x) {
        LOG.log(Level.WARNING, x.toString(), x);
      }
    }
  }

  // returns an array of unique 3-char prefixes
  private String[] findAirports(File d) {
    HashSet<String> set = new HashSet<String>();

    for (File f : d.listFiles()) {
      String n = TileName.getAirportCode(f.getName());
      if (n != null) {
        set.add(n.substring(0, 3));
      }
    }
    return set.toArray(new String[0]);
  }

  // sync "Airports/W/A/T"
  private void syncAirports(String[] names) {
    long rev;

    for (String i : names) {
      String node = String.format("Airports/%c/%c/%c", i.charAt(0),
          i.charAt(1), i.charAt(2));
      File f = new File(pathBase + node);

      try {
        printStats(node);
        rev = updateNode(f);
        invokeLater(2); // update progressBar
        if (rev > 0)
          LOG.info(String.format("updated to r%d.\n", rev));
      } catch (SVNException x) {
        LOG.info(x.getMessage());
      }
    }
  }

  // svn update Models/*
  private void syncModels(String name) {
    try {
      printStats("Models/" + name);

      File f = new File(pathBase + "Models/" + name);
      long rev = updateNode(f);
      invokeLater(2); // update progressBar
      if (rev > 0)
        LOG.info(String.format("updated to r%d.\n", rev));
    } catch (SVNException x) {
      LOG.info(x.getMessage());
    }
  }

  // svn update --depth immediate Models
  private void syncModels() {
    File d;
    if (pathBase == null) {
      JOptionPane
          .showMessageDialog(TerraMaster.frame, "TerraSync path not set");
      return;
    }

    try {
      verifyRoot(new File(pathBase), SVNURL.parseURIEncoded(urlBase));

      d = new File(pathBase + "Models/");

      // first update the top level
      File[] list = new File[1];
      list[0] = d;
      long[] rev = updateClient.doUpdate(list, SVNRevision.HEAD,
          SVNDepth.IMMEDIATES, true, true, true);
    } catch (SVNException x) {
      LOG.info(x.getMessage());
      return;
    } finally {
      invokeLater(2); // update progressBar
    }

    // queue each subdir in syncList
    synchronized (syncList) {
      for (File f : d.listFiles()) { // XXX should filter for dirs only
        syncList.add(new TileName("MODELS-" + f.getName()));
        invokeLater(3);
      }
    }
  }

  public void sync(Collection<TileName> set) {
    int res = JOptionPane.showConfirmDialog(TerraMaster.frame,
        "SVN is deprecated and you won't receive the most recent data." + System.getProperty("line.separator") + "Sync?",
        "Warning", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
    if (res == JOptionPane.OK_OPTION) {
      synchronized (syncList) {
        syncList.addAll(set);
      }
    }
    synchronized (this) {
      try {
        notify();
      } // wake up the main loop
      catch (IllegalMonitorStateException e) {
      }
    }
  }

  // cancel current op and clear the queue
  public void cancel() {
    cancelFlag = true;
    synchronized (syncList) {
      syncList.clear();
    }
  }

  private void deltree(File d) {
    if (!d.exists())
      return;
    for (File f : d.listFiles()) {
      if (f.isDirectory())
        deltree(f);
      try {
        f.delete();
      } catch (SecurityException x) {
      }
    }
    try {
      d.delete();
    } catch (SecurityException x) {
    }
  }

  public void delete(Collection<TileName> set) {
    for (TileName n : set) {
      TileData d = TerraMaster.mapScenery.remove(n);
      if (d == null)
        continue;
      if (d.terrain) {
        deltree(d.dir_terr);
      }

      if (d.objects) {
        deltree(d.dir_obj);
      }

      synchronized (syncList) {
        syncList.remove(n);
      }
    }
  }

  public void quit() {
    clientManager.dispose();
  }

  public void setScnPath(File f) {
    pathBase = f.getPath() + "/";

    try {
      wcClient.doCleanup(f);
    } catch (SVNException e) {
    }
  }

  private boolean noquit = true;

  public void run() {
    while (noquit) {
      try {
        synchronized (this) {
          try {
            wait();
          } catch (InterruptedException e) {
          }
        }
        if (pathBase == null) {
          JOptionPane
              .showMessageDialog(TerraMaster.frame, "TerraSync path not set");
          continue;
        }
        while (syncList.size() > 0) {
          checkURL();

          final TileName n;
          synchronized (syncList) {
            n = syncList.getFirst();
          }

          String name = n.getName();
          if (name.startsWith("MODELS")) {
            int i = name.indexOf('-');
            if (i > -1)
              syncModels(name.substring(i + 1));
            else
              syncModels();
          } else {
            String path = n.buildPath();
            if (path != null)
              checkout(path);
          }

          synchronized (syncList) {
            syncList.remove(n);
          }
        }

        // syncList is now empty
      } catch (Exception e) {
        LOG.log(Level.WARNING, e.toString(), e);
      }
      invokeLater(1); // reset progressBar
    }
  }

  private void invokeLater(final int n) {
    // invoke this on the Event Disp Thread
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        switch (n) {
        case 1: // reset progressBar
          TerraMaster.frame.butStop.setEnabled(false);
          try {
            Thread.sleep(1200);
          } catch (InterruptedException e) {
          }
          TerraMaster.frame.progressBar.setMaximum(0);
          TerraMaster.frame.progressBar.setVisible(false);
          break;
        case 2: // update progressBar
          TerraMaster.frame.progressUpdate(1);
          break;
        case 3: // progressBar maximum++
          TerraMaster.frame.progressBar
              .setMaximum(TerraMaster.frame.progressBar.getMaximum() + 1);
          break;
        }
      }
    });
  }

  @Override
  public Collection<TileName> getSyncList() {
    return syncList;
  }

  /**
   * 
   */
  @Override
  public void setTypes(boolean selected, boolean selected2, boolean selected3) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void restoreSettings() {
    // TODO Auto-generated method stub
    
  }

}
