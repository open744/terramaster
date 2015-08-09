import	java.util.*;
import	java.io.File;
import	javax.swing.SwingUtilities;
import	org.tmatesoft.svn.core.wc.*;
import	org.tmatesoft.svn.core.ISVNCanceller;
import	org.tmatesoft.svn.core.ISVNDirEntryHandler;
import	org.tmatesoft.svn.core.SVNDirEntry;
import	org.tmatesoft.svn.core.SVNException;
import	org.tmatesoft.svn.core.SVNCancelException;
import	org.tmatesoft.svn.core.SVNErrorMessage;
import	org.tmatesoft.svn.core.SVNURL;
import	org.tmatesoft.svn.core.SVNDepth;
import	org.tmatesoft.svn.core.internal.io.dav.DAVRepositoryFactory;

// svn --force co http://terrascenery.googlecode.com/svn/trunk/data/Scenery/Terrain/e100n00/e104n00

class Svn extends Thread implements ISVNDirEntryHandler, ISVNCanceller, ISVNExternalsHandler
{
  SVNClientManager clientManager;
  SVNUpdateClient updateClient;
  SVNStatusClient statusClient;
  SVNLogClient logClient;
  String urlBase = "http://terrascenery.googlecode.com/svn/trunk/data/Scenery/";
  String pathBase;
  long syncsize, synccount;
  boolean cancelFlag = false;

  LinkedList<TileName> syncList;

  Svn() {
    DAVRepositoryFactory.setup();
    clientManager = SVNClientManager.newInstance();
    clientManager.setCanceller(this);
    updateClient = clientManager.getUpdateClient();
    statusClient = clientManager.getStatusClient();
    logClient = clientManager.getLogClient();
    updateClient.setIgnoreExternals(false);

    syncList = new LinkedList<TileName>();

    // externals stuff
    updateClient.setExternalsHandler(this);
  }

  // this implements ISVNExternalsHandler
  public SVNRevision[] handleExternal(java.io.File externalPath,
    SVNURL externalURL, SVNRevision externalRevision,
    SVNRevision externalPegRevision, java.lang.String externalsDefinition,
    SVNRevision externalsWorkingRevision)
  {
    System.out.printf("Externals: %d %d -> ", externalRevision, externalPegRevision);
    SVNRevision[] ret = ISVNExternalsHandler.DEFAULT.handleExternal(externalPath, externalURL, externalRevision, externalPegRevision, externalsDefinition, externalsWorkingRevision);
    System.out.println(ret);
    return ret;
  }

  // given a 1x1 tile, figure out the parent 10x10 container
  // return the 10/1 path
  private String buildPath(String tile) {
    if (tile.length() < 7) return null;

    // XXX throw an exception
    int lon = Integer.parseInt(tile.substring(1, 4));
    int lat = Integer.parseInt(tile.substring(5));
    char ew = tile.charAt(0);
    char ns = tile.charAt(4);

    int modlon = lon % 10;
    lon -= ew == 'w' && modlon != 0 ? modlon - 10 : modlon;

    int modlat = lat % 10;
    lat -= ns == 's' && modlat != 0 ? modlat - 10 : modlat;

    return String.format("%s%03d%s%02d/%s", ew, lon, ns, lat, tile);
  }

  // this implements ISVNCanceller
  public void checkCancelled() throws SVNCancelException
  {
    if (cancelFlag) {
System.out.println("I SAID STOP!!");
      cancelFlag = false;
      invokeLater(1);		// reset progressBar
      throw new SVNCancelException();
    }
  }

  // this implements ISVNDirEntryHandler
  public void handleDirEntry(SVNDirEntry e)
  {
    syncsize += e.getSize();
    ++synccount;
  }

  /*
  SVNStatus stat = statusClient.doStatus(pathBase +
      "Terrain/e140s40/e144s38/"), true);
  System.out.println(stat.getURL());
  */

  private void updateOrCheckout(SVNURL url, File f) throws SVNException
  {
    long rev;

    if (f.exists()) {
      rev = updateClient.doUpdate(f, SVNRevision.HEAD, SVNDepth.INFINITY, true, true);
      System.out.printf("updated to r%d... ", rev);
    } else {
      //f.mkdir();
      rev = updateClient.doCheckout(url, f, SVNRevision.HEAD,
          SVNRevision.HEAD, SVNDepth.INFINITY, true);
      System.out.printf("checked out r%d... ", rev);
    }
  }

  private void checkout(String name) {
    String[] types = { "Terrain/", "Objects/" };
    int[] ntype = { TerraMaster.TERRAIN, TerraMaster.OBJECTS };

    System.out.print("sync "+name+"... ");

    for (int i = 0; i < types.length; ++i) {
      String node = types[i]+name;

      try {

      SVNURL url = SVNURL.parseURIDecoded(urlBase + node);
      File f = new File(pathBase + node);

      // create parent directory if !exist
      File parent = f.getParentFile();
      if (!parent.exists())
        parent.mkdir();

      synccount = syncsize = 0;
      logClient.doList(url, SVNRevision.HEAD, SVNRevision.HEAD, false, true, this);
      System.out.printf("%s%d items (%d bytes)... ", types[i], synccount, syncsize);

      // try update, if fails, do checkout (export?)
      updateOrCheckout(url, f);

      TerraMaster.addScnMapTile(TerraMaster.mapScenery, f, ntype[i]);

      } catch (SVNException x) {
        SVNErrorMessage em = x.getErrorMessage();
        // E160013 = URL not found
        if (em.getErrorCode().getCode() != 160013)
          System.out.println(x.getMessage());
      } catch (Exception x) { System.out.println(x); }

      invokeLater(2);		// update progressBar
    }

    System.out.println();
  }


  private void syncModels()
  {
    long rev;

    try {

    File f = new File(pathBase + "Models/");
    SVNURL url = SVNURL.parseURIDecoded(urlBase + "Models/");

    if (f.exists()) {
      rev = updateClient.doUpdate(f, SVNRevision.HEAD, SVNDepth.INFINITY, true, true);
      //System.out.printf("updated to r%d... ", rev);
    } else {
      rev = updateClient.doCheckout(url, f, SVNRevision.HEAD,
          SVNRevision.HEAD, SVNDepth.INFINITY, true);
      //System.out.printf("checked out r%d... ", rev);
    }

    } catch (SVNException x) {
      System.out.println(x.getMessage());
    }
  }

  public void sync(Collection<TileName> set)
  {
    synchronized(syncList) {
      syncList.addAll(set);
    }
    synchronized(this) {
      try { notify(); }			// wake up the main loop
      catch (IllegalMonitorStateException e) { }
    }
  }

  // cancel current op and clear the queue
  public void cancel()
  {
    cancelFlag = true;
    synchronized(syncList) {
      syncList.clear();
    }
  }

  private void deltree(File d)
  {
    for (File f : d.listFiles()) {
      if (f.isDirectory())
	deltree(f);
      try { f.delete(); }
      catch(SecurityException x) { }
    }
    try { d.delete(); } catch(SecurityException x) { }
  }

  public void delete(Collection<TileName> set)
  {
    for (TileName n : set) {
      TileData d = TerraMaster.mapScenery.remove(n);
      if (d == null) continue;
      if (d.terrain) {
	deltree(d.dir_terr);
      }

      if (d.objects) {
	deltree(d.dir_obj);
      }

      synchronized(syncList) {
	syncList.remove(n);
      }
    }

      /*
      TileData	t = TerraMaster.mapScenery.get(tileName.getText());
      //Collection<TileName> t = map.getSelection();
      try {
      System.out.println("rm -r "
	  + (t.terrain ? t.dir_terr.getCanonicalFile() : "") + " "
	  + (t.objects ? t.dir_obj.getCanonicalFile()  : "") );
      // showTiles();
      } catch (Exception x) {}
      */
  }



  public void quit() {
    clientManager.dispose();
  }

  public void setScnPath(File f) {
    pathBase = f.getPath() + "/";
  }

  private boolean noquit = true;

  public void run()
  {
    while (noquit) {
      synchronized(this) {
	try { wait(); }
	catch (InterruptedException e) { }
      }
      while (syncList.size() > 0) {
	final TileName n;
	synchronized(syncList) {
	  n = syncList.getFirst();
	}
        String name = n.getName();
        if (name.equals("MODELS"))
          syncModels();
        else {
          String path = buildPath(name);
          if (path != null)
            checkout(path);
        }

	synchronized(syncList) {
	  syncList.remove(n);
	}
      }

      // syncList is now empty
      invokeLater(1);		// reset progressBar
    }
  }

  private void invokeLater(final int n)
  {
    // invoke this on the Event Disp Thread
    SwingUtilities.invokeLater(new Runnable() {
	public void run() {
	  switch (n) {
	  case 1:	// reset progressBar
	    TerraMaster.frame.butStop.setEnabled(false);
	    try { Thread.sleep(1200); }
	    catch (InterruptedException e) { }
	    TerraMaster.frame.progressBar.setMaximum(0);
	    TerraMaster.frame.progressBar.setVisible(false);
	    break;
	  case 2:	// update progressBar
	    TerraMaster.frame.progressUpdate(1);
	    break;
	  }
	}   
    });
  }

  // XXX
  private void doSvnUpdate(final TileName n)
  {
    // invoke this on the Event Disp Thread
    SwingUtilities.invokeLater(new Runnable() {
	public void run() {
	  TerraMaster.frame.doSvnUpdate(n);
	}   
    });
  }

}
