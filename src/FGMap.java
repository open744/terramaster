import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

/**
 *  this class handles webqueries and returns results
 *  it keeps a per-session HashMap of known airports
 *  It queries the multiplayer map. 
 *  {@link http://mpmap02.flightgear.org/fg_nav_xml_proxy.cgi?sstr=wbks&apt_code} 
 */

public class FGMap {
  Logger LOG = Logger.getLogger(this.getClass().getName());
  XMLInputFactory input;
  Airport apt;
  HashMap<String, Airport> map;

  public FGMap() {
    try {
      input = XMLInputFactory.newFactory();
    } catch (FactoryConfigurationError e) {
    }

    apt = new Airport();

    map = new HashMap<String, Airport>();
  }

  private void close_apt(List<Airport> list) {
    if (apt.code != null) {
      // first add the current airport to the HashMap
      map.put(apt.code, apt);

      // and to the current query's result
      list.add(apt);

      // now start a new airport
      apt = new Airport();
    }
  }

  private void work(XMLStreamReader reader, List<Airport> list) {
    if (reader.getName().toString().equals("airport")) {
      close_apt(list);
      apt.code = reader.getAttributeValue(null, "code");
      apt.name = reader.getAttributeValue(null, "name");
    } else if (reader.getName().toString().equals("runway")) {
      apt.lat = Float.parseFloat(reader.getAttributeValue(null, "lat"));
      apt.lon = Float.parseFloat(reader.getAttributeValue(null, "lng"));
    }
  }

  private List<Airport> webquery(URL url) {
    List<Airport> result = new LinkedList<Airport>();
    try {
      XMLStreamReader reader = input.createXMLStreamReader(url.openStream());
      while (reader.hasNext()) {
        reader.nextTag();
        if (reader.getEventType() == XMLStreamConstants.START_ELEMENT) {
          work(reader, result);
        }
        if (reader.getEventType() == XMLStreamConstants.END_ELEMENT) {
          close_apt(result);
        }
      }

    } catch (XMLStreamException e) {
      LOG.log(Level.WARNING, e.toString(), e);
    } catch (Exception e) {
      LOG.log(Level.WARNING, e.toString(), e);
      JOptionPane.showMessageDialog(TerraMaster.frame, "Can't query Airports " + e.toString(), "Error", JOptionPane.ERROR_MESSAGE);    }

    return result;
  }

  public List<Airport> search(String str) {
    str = str.trim();
    String url = String
        .format(
            "http://mpmap02.flightgear.org/fg_nav_xml_proxy.cgi?sstr=%s&apt_code&apt_name",
            str);

    try {
      return webquery(new URL(url));
    } catch (MalformedURLException e) {
      LOG.log(Level.WARNING, e.toString(), e);      
//      System.err.printf("Error: Malformed URL: %s\n", url);
    }
    return null;
  }

  public List<Airport> browse(Collection<TileName> list) {
    List<Airport> result = new LinkedList<Airport>();

    for (TileName t : list) {
      int lat = t.getLat(), lon = t.getLon();
      String sw = String.format("%d,%d", lat, lon);
      String ne = String.format("%d,%d", lat + 1, lon + 1);
      String url = String
          .format(
              "http://mpmap02.flightgear.org/fg_nav_xml_proxy.cgi?ne=%s&sw=%s&apt_code",
              ne, sw);
      try {
        result.addAll(webquery(new URL(url)));
        SwingUtilities.invokeLater(new Runnable() {
          public void run() {
            TerraMaster.frame.repaint();
          }
        });
      } catch (MalformedURLException e) {
        System.err.printf("Error: Malformed URL: %s\n", url);
      }
    }
    return result;
  }

  public HashMap<String, Airport> getAirportList() {
    return map;
  }

  public void clearAirports() {
    map.clear();
  }
}
