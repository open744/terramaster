import	java.util.*;
import	java.io.File;
import	javax.swing.SwingUtilities;
import	org.tmatesoft.svn.core.wc.*;
import	org.tmatesoft.svn.core.ISVNCanceller;
import	org.tmatesoft.svn.core.ISVNDirEntryHandler;
import	org.tmatesoft.svn.core.SVNNodeKind;
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
  SVNWCClient wcClient;
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
    wcClient = clientManager.getWCClient();
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
      cancelFlag = false;
      invokeLater(1);		// reset progressBar
      throw new SVNCancelException();
    }
  }

  // checks that f exists and is versioned, otherwise `svn checkout --depth empty url`
  private void verifyRoot(File f, SVNURL url) throws SVNException
  {
    try {
      boolean stat = statusClient.doStatus(f, false).isVersioned();
      //System.out.printf("Info: %s exists and is versioned.\n", f.toString());
    } catch (SVNException e) {
      // allowUnversionedObstructions = true
      long rev = updateClient.doCheckout(url, f, SVNRevision.HEAD,
					 SVNRevision.HEAD, SVNDepth.EMPTY, true);
      System.out.printf("Created working copy %s.\n", f.toString());
    }
  }

  // `svn update --parents f`
  private long updateNode(File f) throws SVNException
  {
    File[] list = new File[1];
    list[0] = f;
    // allowUnversionedObstructions = true, depthIsSticky = T, makeParents = T
    long[] rev = updateClient.doUpdate(list, SVNRevision.HEAD, SVNDepth.INFINITY, true, true, true);
    return rev[0];
  }

  // this implements ISVNDirEntryHandler
  public void handleDirEntry(SVNDirEntry e)
  {
    syncsize += e.getSize();
    ++synccount;
    //SVNNodeKind typ = e.getKind();
    //e.DIRENT_KIND.equals(SVNNodeKind.DIR)
  }

  // svn list --verbose --> fills up syncsize/synccount
  private void printStats(String path)
  {
    try {
      SVNURL url = SVNURL.parseURIDecoded(urlBase + path);
      synccount = syncsize = 0;
      logClient.doList(url, SVNRevision.HEAD, SVNRevision.HEAD, false, true, this);
      System.out.printf("%s/%d items (%d bytes)... ", path, synccount, syncsize);
    } catch (SVNException e) { }
  }

  /*
  SVNStatus stat = statusClient.doStatus(pathBase +
      "Terrain/e140s40/e144s38/"), true);
  System.out.println(stat.getURL());
  */

  // syncs the tile's Terrain then Objects, then any Airports within
  // name : "e120s20/e123s11"
  private void checkout(String name) {
    String[] types = { "Terrain/", "Objects/" };
    int[] ntype = { TerraMaster.TERRAIN, TerraMaster.OBJECTS };

    // make sure the working copy root is valid
    try {
      verifyRoot(new File(pathBase), SVNURL.parseURIDecoded(urlBase));
    } catch (SVNException e) {
    }

    System.out.print("sync "+name+"... ");

    for (int i = 0; i < types.length; ++i) {
      String node = types[i]+name;

      try {

      File f = new File(pathBase + node);

      printStats(node);
      long rev = updateNode(f);
      if (rev > 0)
	System.out.printf("updated to r%d.\n", rev);

      TerraMaster.addScnMapTile(TerraMaster.mapScenery, f, ntype[i]);

      invokeLater(2);		// update progressBar

      // look for airport codes among the newly sync'd Terrain files
      if (i == 0) {
	String[] apt = findAirports(f);
	for (int j = 0; j < apt.length; ++j)
	  invokeLater(3);	// extend progressBar

	syncAirports(apt);
      }

      } catch (SVNException x) {
	invokeLater(2);		// update progressBar
        SVNErrorMessage em = x.getErrorMessage();
        // E160013 = URL not found
        //if (em.getErrorCode().getCode() != 160013)
          System.out.println(x.getMessage());
	// 155004 unfinished work items, need svn cleanup
      } //catch (Exception x) { System.out.println(x); }

    }
  }

  // returns an array of unique 3-char prefixes
  private String[] findAirports(File d)
  {
    HashSet<String> set = new HashSet<String>();

    for (File f : d.listFiles()) {
      String n = TileName.getAirportCode(f.getName());
      if (n != null) {
        set.add(n.substring(0, 3));
      }
    }
    return set.toArray(new String[1]);
  }

  // sync "Airports/W/A/T"
  private void syncAirports(String[] names)
  {
    long rev;

    if (names == null) return;

    for (String i : names) {
      String node = String.format("Airports/%c/%c/%c/",
          i.charAt(0), i.charAt(1), i.charAt(2));
      File f = new File(pathBase + node);

      try {
	printStats(node);
	rev = updateNode(f);
	if (rev > 0) System.out.printf("updated to r%d.\n", rev);

	invokeLater(2);		// update progressBar

      } catch (SVNException x) {
        System.out.println(x.getMessage());
      }
    }
  }

  // svn update Models/*
  private void syncModels(String name)
  {
    try {
      printStats("Models/" + name);

      File f = new File(pathBase + "Models/" + name);
      long rev = updateNode(f);
      if (rev > 0) System.out.printf("updated to r%d.\n", rev);
	
      invokeLater(2);		// update progressBar
    } catch (SVNException x) {
      System.out.println(x.getMessage());
    } 
  }

  // svn update --depth immediate Models
  private void syncModels()
  {
    File d;

    try {
      verifyRoot(new File(pathBase), SVNURL.parseURIDecoded(urlBase));

      d = new File(pathBase + "Models/");

      // first update the top level
      File[] list = new File[1];
      list[0] = d;
      long[] rev = updateClient.doUpdate(list, SVNRevision.HEAD, SVNDepth.IMMEDIATES, true, true, true);
    } catch (SVNException x) {
      System.out.println(x.getMessage());
      return;
    } finally {
      invokeLater(2);		// update progressBar
    }

    // queue each subdir in syncList 
    synchronized(syncList) {
      for (File f : d.listFiles()) {	// XXX should filter for dirs only
	syncList.add(new TileName("MODELS-" + f.getName()));
	invokeLater(3);
      }
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
  }



  public void quit() {
    clientManager.dispose();
  }

  public void setScnPath(File f) {
    pathBase = f.getPath() + "/";

    try {
      wcClient.doCleanup(f);
    } catch (SVNException e) { }
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
        if (name.startsWith("MODELS")) {
	  int i = name.indexOf('-');
	  if (i > -1)
	    syncModels(name.substring(i+1));
	  else
	    syncModels();
        } else {
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
	  case 3:	// progressBar maximum++
	    TerraMaster.frame.progressBar.setMaximum(TerraMaster.frame.progressBar.getMaximum() + 1);
	    break;
	  }
	}   
    });
  }

}
