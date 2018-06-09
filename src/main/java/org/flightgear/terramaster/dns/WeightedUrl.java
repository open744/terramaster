package org.flightgear.terramaster.dns;

import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.flightgear.terramaster.TerraMaster;

import net.sf.ivmaidns.dns.DNSName;

public class WeightedUrl implements Serializable {
  transient Logger log;

  public synchronized Logger getLog() {
    if (log == null)
      log = Logger.getLogger(TerraMaster.LOGGER_CATEGORY);
    return log;
  }

  private URL url;
  private int weight;

  /**
   * @return the weight
   */
  public int getWeight() {
    return weight;
  }

  /**
   * @param weight
   *          the weight to set
   */
  public void setWeight(int weight) {
    this.weight = weight;
  }

  /**
   * @param url
   *          the url to set
   */
  public void setUrl(URL url) {
    this.url = url;
  }

  public WeightedUrl() {
  }

  /**
   * 
   * @param weight
   * @param regex
   *          The Regex to be used on the QName to get the URL
   * @param qName
   *          the QName to be regexed
   */

  public WeightedUrl(String weight, String regex, DNSName qName) {
    this();
    String[] tokens = regex.split("!");
    Pattern p = Pattern.compile(tokens[1]);
    Matcher m = p.matcher(qName.getAbsolute());
    if (m.find()) {
      String urlString = "";
      try {
        urlString = m.replaceAll(tokens[2] + "/");
        url = new URL(urlString);
      } catch (MalformedURLException e) {
        log.log(Level.WARNING, "URL incorrect " + urlString, e);
      }
    }
    this.weight = Integer.parseInt(weight);
  }

  public URL getUrl() {
    // TODO Auto-generated method stub
    return url;
  }

}
