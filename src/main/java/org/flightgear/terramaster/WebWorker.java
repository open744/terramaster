package org.flightgear.terramaster;

import java.io.StringBufferInputStream;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

public class WebWorker extends SwingWorker<List<Airport>, Void> {
	Logger LOG = Logger.getLogger(TerraMaster.LOGGER_CATEGORY);
	String url;
	Collection<TileName> selection;
	List<Airport> result;
	int jobType;
	XMLInputFactory input;
	AirportResult callback;

	private static final int SEARCH = 1, BROWSE = 2;

	/**
	 * Search for Airport.
	 * 
	 * @param str
	 */
	public WebWorker(String str, AirportResult callback) {
		init();
		jobType = SEARCH;
		url = str;
		this.callback = callback;
	}

	/**
	 * Search for all airports in tiles.
	 * 
	 * @param list
	 */
	public WebWorker(Collection<TileName> list, AirportResult callback) {
		init();
		jobType = BROWSE;
		selection = list;
		this.callback = callback;
	}

	private void init() {
		try {
			input = XMLInputFactory.newFactory();
		} catch (FactoryConfigurationError e) {
		}
	}

	public List<Airport> doInBackground() {
		switch (jobType) {
		case SEARCH:
			result = search(url);
			break;
		case BROWSE:
			result = browse(selection);
			break;
		}
		return result;
	}

	public void done() {
		/*
		 * for (Airport a : result) { if (a != null) System.out.println(a); }
		 */
		callback.done();
	}

	private List<Airport> webquery(URL url) {
		List<Airport> result = new LinkedList<Airport>();
		try {
			try (Scanner scanner = new Scanner(url.openStream(), StandardCharsets.UTF_8.toString())) {
				scanner.useDelimiter("\\A");
				String content = scanner.hasNext() ? scanner.next() : "";
				// if (reader.getName().toString().equals("airport")) {
				// apt.code = reader.getAttributeValue(null, "code");
				// apt.name = reader.getAttributeValue(null, "name");
				// } else if (reader.getName().toString().equals("runway")) {
				// apt.lat = Float.parseFloat(reader.getAttributeValue(null, "lat"));
				// apt.lon = Float.parseFloat(reader.getAttributeValue(null, "lng"));
				// }
				// addAirport(apt);
				DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
				factory.setNamespaceAware(true);
				DocumentBuilder builder;
				Document doc = null;
				builder = factory.newDocumentBuilder();
				doc = builder.parse(new InputSource(new StringReader(content)));

				// Create XPathFactory object
				XPathFactory xpathFactory = XPathFactory.newInstance();

				// Create XPath object
				XPath xpath = xpathFactory.newXPath();
				NodeList airportNodes = (NodeList) xpath.evaluate("/navaids/airport", doc, XPathConstants.NODESET);
				for (int i = 0; i < airportNodes.getLength(); i++) {
					Element n = (Element) airportNodes.item(i);

					Airport a = new Airport(n.getAttribute("code"), n.getAttribute("name"));
					NodeList runwayNodes = (NodeList) xpath.evaluate("runway", n, XPathConstants.NODESET);
					for (int j = 0; j < runwayNodes.getLength(); j++) {
						Element runwayNode = (Element) runwayNodes.item(j);
                        a.updatePosition(runwayNode.getAttribute("lat"), runwayNode.getAttribute("lng"));						
					}
					
					callback.addAirport(a);
				}				
			}

		} catch (Exception e) {
			LOG.log(Level.WARNING, e.toString(), e);
			JOptionPane.showMessageDialog(TerraMaster.frame, "Can't query Airports " + e.toString(), "Error",
					JOptionPane.ERROR_MESSAGE);
		}

		return result;
	}

	public List<Airport> search(String str) {
		str = URLEncoder.encode(str.trim());
		String url = String.format("http://mpmap02.flightgear.org/fg_nav_xml.cgi?sstr=%s&apt_code&apt_name", str);

		try {
			return webquery(new URL(url));
		} catch (MalformedURLException e) {
			LOG.log(Level.WARNING, e.toString(), e);
			// System.err.printf("Error: Malformed URL: %s\n", url);
		}
		return null;
	}

	public List<Airport> browse(Collection<TileName> list) {
		List<Airport> result = new LinkedList<Airport>();

		for (TileName t : list) {
			int lat = t.getLat(), lon = t.getLon();
			String sw = String.format("%d,%d", lat, lon);
			String ne = String.format("%d,%d", lat + 1, lon + 1);
			String url = String.format("http://mpmap02.flightgear.org/fg_nav_xml.cgi?ne=%s&sw=%s&apt_code", ne, sw);
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
}
