package org.flightgear.terramaster;

import java.awt.Container;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;
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
 * this class handles webqueries and returns results it keeps a per-session
 * HashMap of known airports It queries the multiplayer map.
 * {@link http://mpmap02.flightgear.org/fg_nav_xml_proxy.cgi?sstr=wbks&apt_code}
 */

public class FGMap implements AirportResult {
	Logger LOG = Logger.getLogger(TerraMaster.LOGGER_CATEGORY);
	HashMap<String, Airport> map;
	private List<Airport> list = new ArrayList<>();

	public FGMap() {
		map = new HashMap<String, Airport>();
	}

	public void addAirport(Airport apt) {
		if (apt.code != null) {
			// first add the current airport to the HashMap
			map.put(apt.code, apt);

			// and to the current query's result
			list.add(apt);
		}
	}


	public HashMap<String, Airport> getAirportList() {
		return map;
	}

	public void clearAirports() {
		map.clear();
	}

	@Override
	public void done() {
		TerraMaster.frame.repaint();
	}
}
