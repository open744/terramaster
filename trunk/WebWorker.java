import	java.io.*;
import	java.util.*;
import	javax.swing.*;

public class WebWorker extends SwingWorker<List<Airport>, Void>
{
  String url;
  Collection<TileName> selection;
  List<Airport> result;
  int jobType;

  private static final int SEARCH = 1, BROWSE = 2;

  public WebWorker(String str)
  {
    jobType = SEARCH;
    url = str;
  }

  public WebWorker(Collection<TileName> list)
  {
    jobType = BROWSE;
    selection = list;
  }

  public List<Airport> doInBackground()
  {
    switch (jobType) {
    case SEARCH:
      result = TerraMaster.fgmap.search(url);
      break;
    case BROWSE:
      result = TerraMaster.fgmap.browse(selection);
      break;
    }
    return result;
  }

  public void done()
  {
    /*
    for (Airport a : result) {
      if (a != null) System.out.println(a);
    }
    */
    TerraMaster.frame.repaint();
  }
}

