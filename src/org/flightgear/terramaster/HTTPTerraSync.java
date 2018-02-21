package org.flightgear.terramaster;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.flightgear.terramaster.dns.FlightgearNAPTRQuery;
import org.flightgear.terramaster.dns.WeightedUrl;

/**
 * Implementation of the new TerraSync Version
 * 
 * @author keith.paterson
 */

public class HTTPTerraSync extends Thread implements TileService {
	private static final int DIR_SIZE = 400;

	private static final int AIRPORT_MAX = 30000;

	Logger log = Logger.getLogger(TerraMaster.LOGGER_CATEGORY);

	private static final int RESET = 1;
	private static final int UPDATE = 2;
	private static final int EXTEND = 3;
	private LinkedList<TileName> syncList = new LinkedList<TileName>();
	private boolean cancelFlag = false;
	private boolean noquit = true;

	private List<WeightedUrl> urls = new ArrayList<>();
	Random rand = new Random();
	private File localBaseDir;

	private boolean ageCheck;

	public HTTPTerraSync() {
		super("HTTPTerraSync");
	}

	@Override
	public void setScnPath(File file) {
		localBaseDir = file;
	} 

	@Override
	public void sync(Collection<TileName> set, boolean ageCheck) {
		
		this.ageCheck = ageCheck;
		for (Iterator iterator = set.iterator(); iterator.hasNext();) {
			TileName tileName = (TileName) iterator.next();
			if(tileName==null)
				continue;
			synchronized (syncList) {
				syncList.add(tileName);
				cancelFlag = false;
				Collections.sort(syncList, new Comparator<TileName>() {

					@Override
					public int compare(TileName o1, TileName o2) {
						return o1.getName().compareTo(o2.getName());
					}
				});
			}			
			log.finest("Added " + tileName.getName() + " to queue");
		}
		synchronized (this) {
			try {
				notify();
			} // wake up the main loop
			catch (IllegalMonitorStateException e) {
				log.log(Level.WARNING, e.toString(), e);
			}
		}
	}


	@Override
	public Collection<TileName> getSyncList() {
		return syncList;
	}

	@Override
	public void quit() {
		cancelFlag = true;
		synchronized (syncList) {
			syncList.clear();
		}
		(new Thread() {
			@Override
			public void run() {
				try {
					if (httpConn != null && httpConn.getInputStream() != null) {
						httpConn.getInputStream().close();
					}
				} catch (IOException e) {
					// Expecting to throw error
				}
			}
		}).start();
	}

	@Override
	public void cancel() {
		cancelFlag = true;
		synchronized (syncList) {
			syncList.clear();
		}
		(new Thread() {
			@Override
			public void run() {
				try {
					if (httpConn != null && httpConn.getInputStream() != null) {
						httpConn.getInputStream().close();
					}
				} catch (IOException e) {
					// Expecting to throw error
				}
			}
		}).start();
	}

	@Override
	public void delete(Collection<TileName> selection) {
		for (TileName n : selection) {
			TileData d = TerraMaster.mapScenery.remove(n);
			if (d == null)
				continue;
			if (d.terrain) {
				deltree(d.dir_terr);
			}

			if (d.objects) {
				deltree(d.dir_obj);
			}
			if (d.buildings) {
				deltree(d.dir_buildings);
			}

			synchronized (syncList) {
				syncList.remove(n);
			}
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

	@Override
	public void run() {
		try {
			while (noquit) {
				synchronized (this) {
					try {
						wait();
					} catch (InterruptedException e) {
					}
				}
				HashSet<String> apt = new HashSet<String>();
				int tilesize = (terrain?DIR_SIZE:0) + (objects?DIR_SIZE:0) + (buildings?2000:0);
				// update progressbar
				invokeLater(EXTEND, syncList.size() * tilesize  + AIRPORT_MAX); // update
				while (syncList.size() > 0) {
					urls = new FlightgearNAPTRQuery().queryDNSServer("ws20");
					final TileName n;
					synchronized (syncList) {
						if (syncList.size() == 0)
							continue;
						n = syncList.getFirst();
					}

					String name = n.getName();
					if (name.startsWith("MODELS")) {
						int i = name.indexOf('-');
						if (i > -1)
							syncDirectory(name.substring(i + 1), false, TerraSyncDirectoryTypes.MODELS);
						else
							syncModels();
					} else {
						// Updating Terrain/Objects
						String path = n.buildPath();
						if (path != null)
							try {
								HashSet<String> apt2 = syncTile(path);
								apt.addAll(apt2);
							} catch (IOException e) {
								log.log(Level.WARNING, "Couldn't sync tile " + path, e);
							}
					}

					synchronized (syncList) {
						syncList.remove(n);
					}
				}
				if (apt != null) {
					try {
						syncAirports(apt.toArray(new String[0]));
					} catch (IOException e) {
						log.log(Level.WARNING, "Couldn't get airports ", e);
					}
				}

				// syncList is now empty
				invokeLater(RESET, 0); // reset progressBar
			}
			log.fine("HTTP TerraSync ended gracefully");
		} catch (Exception e) {
			log.log(Level.SEVERE, "HTTP Crashed ", e);
			e.printStackTrace();
		}
	}

	/**
	 * 
	 * @param path
	 * @return
	 * @throws IOException
	 */

	private HashSet<String> syncTile(String path) throws IOException {
		try {
			log.fine("Syncing " + path);
			if (terrain) {
				int updates = syncDirectory(TerraSyncDirectoryTypes.TERRAIN.dirname + path, false,
						TerraSyncDirectoryTypes.TERRAIN);
				invokeLater(UPDATE, DIR_SIZE - updates); // update progressBar
			}
			if (objects) {
				int updates = syncDirectory(TerraSyncDirectoryTypes.OBJECTS.dirname + path, false,
						TerraSyncDirectoryTypes.OBJECTS);
				invokeLater(UPDATE, DIR_SIZE - updates); // update progressBar
			}
			if (buildings) {
				int updates = syncDirectory(TerraSyncDirectoryTypes.BUILDINGS.dirname + path, false,
						TerraSyncDirectoryTypes.BUILDINGS);
				invokeLater(UPDATE, 2000 - updates); // update progressBar
			}
			HashSet<String> apt = findAirports(
					new File(localBaseDir, TerraSyncDirectoryTypes.TERRAIN + File.separator + path));
			return apt;

		} catch (Exception e) {
			log.log(Level.SEVERE, "Can't sync tile " + path, e);
			JOptionPane.showMessageDialog(TerraMaster.frame,
					"Can't sync tile " + path + System.lineSeparator() + e.toString(), "Error",
					JOptionPane.ERROR_MESSAGE);
		}
		return new HashSet<String>();
	}

	/**
	 * returns an array of unique 3-char prefixes
	 * 
	 * @param d
	 * @return
	 */
	private HashSet<String> findAirports(File d) {
		HashSet<String> set = new HashSet<String>();

		if (!d.exists())
			return set;
		for (File f : d.listFiles()) {
			String n = TileName.getAirportCode(f.getName());
			if (n != null) {
				set.add(n.substring(0, 3));
			}
		}
		return set;
	}

	/**
	 * sync "Airports/W/A/T"
	 * 
	 * @param names
	 * @throws IOException
	 */
	private void syncAirports(String[] names) throws IOException {
		long rev;

		HashSet<String> nodes = new HashSet<String>();
		for (String i : names) {
			String node = String.format("Airports/%c/%c/%c", i.charAt(0), i.charAt(1), i.charAt(2));
			nodes.add(node);
		}
		invokeLater(UPDATE, AIRPORT_MAX - nodes.size() * 100);
		for (String node : nodes) {
			int updates = syncDirectory(node, false, TerraSyncDirectoryTypes.AIRPORTS);
			invokeLater(UPDATE, 100 - updates);
		}
	}

	/**
	 * Get a weighted random URL
	 * 
	 * @return
	 */

	private URL getBaseUrl() {
		if (urls.size() == 0) {
			log.warning("No URLs to sync with");
		}

		// Compute the total weight of all items together
		double totalWeight = 0.0d;
		for (WeightedUrl i : urls) {
			totalWeight += i.getWeight();
		}
		// Now choose a random item
		int randomIndex = -1;
		double random = Math.random() * totalWeight;
		for (int i = 0; i < urls.size(); ++i) {
			random -= urls.get(i).getWeight();
			if (random <= 0.0d) {
				randomIndex = i;
				break;
			}
		}
		return urls.get(randomIndex).getUrl();
	}

	/**
	 * Downloads a File into a byte[]
	 * 
	 * @param fileURL
	 * @return
	 * @throws IOException
	 * @throws FileNotFoundException
	 */

	private byte[] getFile(URL fileURL) throws IOException, FileNotFoundException {
		log.fine("Getting " + fileURL.toExternalForm());
		httpConn = (HttpURLConnection) fileURL.openConnection();
		int responseCode = (httpConn).getResponseCode();

		if (responseCode == HttpURLConnection.HTTP_OK) {
			String fileName = "";
			String disposition = httpConn.getHeaderField("Content-Disposition");
			String contentType = httpConn.getContentType();
			int contentLength = httpConn.getContentLength();

			if (disposition != null) {
				// extracts file name from header field
				int index = disposition.indexOf("filename=");
				if (index > 0) {
					fileName = disposition.substring(index + 10, disposition.length() - 1);
				}
			} else {
				fileName = fileURL.getFile();
			}

			log.finest("Content-Type = " + contentType);
			log.finest("Content-Disposition = " + disposition);
			log.finest("Content-Length = " + contentLength);
			log.finest("fileName = " + fileName);

			httpConn.setConnectTimeout(10000);
			httpConn.setReadTimeout(20000);
			// opens input stream from the HTTP connection
			InputStream inputStream = httpConn.getInputStream();

			// opens an output stream to save into file
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

			int bytesRead = -1;
			byte[] buffer = new byte[1024];
			while ((bytesRead = inputStream.read(buffer)) != -1) {
				outputStream.write(buffer, 0, bytesRead);
			}

			outputStream.close();
			inputStream.close();

			log.fine("File downloaded");
			return outputStream.toByteArray();
		} else {
			log.warning("No file to download. Server replied HTTP code: " + responseCode + " for "
					+ fileURL.toExternalForm());
		}
		httpConn.disconnect();
		return "".getBytes();
	}

	private void syncModels() {
		if (localBaseDir == null) {
			JOptionPane.showMessageDialog(TerraMaster.frame, "TerraSync path not set");
		}

		try {
			syncDirectory("Models", false, TerraSyncDirectoryTypes.MODELS);
		} catch (Exception e) {
			log.log(Level.SEVERE, e.toString(), e);
		}
	}

	/**
	 * Syncs the given directory.
	 * 
	 * @param path
	 * @param force
	 * @param models
	 * @return
	 */

	private int syncDirectory(String path, boolean force, TerraSyncDirectoryTypes models) {
		while (urls.size() > 0) {
			URL baseUrl = getBaseUrl();
			try {
				
				int updates = 0;
				if (cancelFlag)
					return updates;
				String localDirIndex = readDirIndex(path);
				String[] localLines = localDirIndex.split("\r?\n");
				if( !force && ageCheck && getDirIndexAge(path) > maxAge )
					return localLines.length;
				URL dirIndexFileURL = new URL(baseUrl.toExternalForm() + path.replace("\\", "/") + "/.dirindex");
				log.finest(dirIndexFileURL.toExternalForm());
				String remoteDirIndex = new String(getFile(dirIndexFileURL));
				String[] lines = remoteDirIndex.split("\r?\n");
				HashMap<String, String> lookup = new HashMap<String, String>();
				for (int i = 0; i < localLines.length; i++) {
					String line = localLines[i];
					String[] splitLine = line.split(":");
					if (splitLine.length > 2)
						lookup.put(splitLine[1], splitLine[2]);				}
				for (int i = 0; i < lines.length; i++) {
					if (cancelFlag)
						return updates;
					String file = lines[i];
					String[] splitLine = file.split(":");
					if (file.startsWith("d:")) {
						// We've got a directory if force ignore what we know
						// otherwise check the SHA against
						// the one from the server
						String dirname = path + "/" + splitLine[1];
						if (force || !(new File(dirname).exists()) || !splitLine[2].equals(lookup.get(splitLine[1])))
							updates += syncDirectory(dirname, force, models);
					} else if (file.startsWith("f:")) {
						// We've got a file
						File localFile = new File(localBaseDir, path + File.separator + splitLine[1]);
						log.finest(localFile.getAbsolutePath());
						boolean load = true;
						if (localFile.exists()) {
							log.finest(localFile.getAbsolutePath());
							byte[] b = calcSHA1(localFile);
							String bytesToHex = bytesToHex(b);
							// LOG.info(bytesToHex);
							load = !splitLine[2].equals(bytesToHex);
						} else {
							if (!localFile.getParentFile().exists()) {
								localFile.getParentFile().mkdirs();
							}
						}
						if (load) {
							URL fileURL = new URL(
									baseUrl.toExternalForm() + path.replace("\\", "/") + "/" + splitLine[1]);
							byte[] fileContent = getFile(fileURL);
							log.finest(fileURL.toExternalForm());
							FileOutputStream fos = new FileOutputStream(localFile);
							fos.write(fileContent);
							fos.flush();
							fos.close();
						}
						invokeLater(UPDATE, 1);
						updates++;
					}
					log.finest(file);
				}
				if (models == TerraSyncDirectoryTypes.OBJECTS || models == TerraSyncDirectoryTypes.TERRAIN
						|| models == TerraSyncDirectoryTypes.BUILDINGS)
					TerraMaster.addScnMapTile(TerraMaster.mapScenery, new File(localBaseDir, path), models);

				storeDirIndex(path, remoteDirIndex);
				return updates;
			} catch (javax.net.ssl.SSLHandshakeException e) {
				log.log(Level.WARNING, "Handshake Error " + e.toString() + " syncing " + path, e);
				JOptionPane.showMessageDialog(TerraMaster.frame, "Sync can fail if Java older than 8u101 and 7u111",
						"SSL Error", JOptionPane.ERROR_MESSAGE);
				urls.remove(baseUrl);
			} catch (Exception e) {
				log.log(Level.WARNING, "General Error " + e.toString() + " syncing with " + baseUrl.toExternalForm() + path.replace("\\", "/") , e);
				return 0;
			}
		}
		return 0;
	}

	private String readDirIndex(String path) throws NoSuchAlgorithmException, IOException {
		File file = new File(new File(localBaseDir, path), ".dirindex");
		return file.exists() ? new String(readFile(file)) : "";
	}

	private long getDirIndexAge(String path) throws NoSuchAlgorithmException, IOException {
		File file = new File(new File(localBaseDir, path), ".dirindex");
		return file.exists() ? (System.currentTimeMillis() - file.lastModified()) : (Long.MAX_VALUE);
	}

	private void storeDirIndex(String path, String remoteDirIndex) throws IOException {
		File file = new File(new File(localBaseDir, path), ".dirindex");
		writeFile(file, remoteDirIndex);
	}

	final protected static char[] hexArray = "0123456789abcdef".toCharArray();
	private HttpURLConnection httpConn;

	private boolean terrain;

	private boolean objects;

	private boolean buildings;

	private long maxAge;

	private String bytesToHex(byte[] bytes) {
		char[] hexChars = new char[bytes.length * 2];
		for (int j = 0; j < bytes.length; j++) {
			int v = bytes[j] & 0xFF;
			hexChars[j * 2] = hexArray[v >>> 4];
			hexChars[j * 2 + 1] = hexArray[v & 0x0F];
		}
		return new String(hexChars);
	}

	/**
	 * Calculates the SHA1 Hash for the given File
	 * 
	 * @param file
	 * @return
	 * @throws NoSuchAlgorithmException
	 * @throws IOException
	 */

	private byte[] calcSHA1(File file) throws NoSuchAlgorithmException, IOException {
		MessageDigest digest = MessageDigest.getInstance("SHA-1");
		InputStream fis = new FileInputStream(file);
		int n = 0;
		byte[] buffer = new byte[8192];
		while (n != -1) {
			n = fis.read(buffer);
			if (n > 0) {
				digest.update(buffer, 0, n);
			}
		}
		return digest.digest();
	}

	/**
	 * Reads the given File.
	 * 
	 * @param file
	 * @return
	 * @throws NoSuchAlgorithmException
	 * @throws IOException
	 */

	private byte[] readFile(File file) throws IOException {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		InputStream fis = new FileInputStream(file);
		int n = 0;
		byte[] buffer = new byte[8192];
		int off = 0;
		while (n != -1) {
			n = fis.read(buffer);
			if (n > 0) {
				bos.write(buffer, 0, n);
				off += n;
			}
		}
		return bos.toByteArray();
	}

	private void writeFile(File file, String remoteDirIndex) throws IOException {
		file.getParentFile().mkdirs();
		FileOutputStream fos = new FileOutputStream(file);
		fos.write(remoteDirIndex.getBytes());
		fos.flush();
		fos.close();
	}

	/**
	 * Does the Async notification of the GUI
	 * 
	 * @param action
	 */

	private void invokeLater(final int action, final int num) {
		if (num < 0)
			log.warning("Update < 0 (" + action + ")");
		// invoke this on the Event Disp Thread
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				switch (action) {
				case RESET: // reset progressBar
					TerraMaster.frame.butStop.setEnabled(false);
					try {
						Thread.sleep(1200);
					} catch (InterruptedException e) {
					}
					TerraMaster.frame.progressBar.setMaximum(0);
					TerraMaster.frame.progressBar.setVisible(false);
					break;
				case UPDATE: // update progressBar
					TerraMaster.frame.progressUpdate(num);
					break;
				case EXTEND: // progressBar maximum++
					TerraMaster.frame.progressBar.setMaximum(TerraMaster.frame.progressBar.getMaximum() + num);
					break;
				}
			}
		});
	}

	@Override
	public void setTypes(boolean t, boolean o, boolean b) {
		terrain = t;
		objects = o;
		buildings = b;
	}

	@Override
	public void restoreSettings() {
		terrain = Boolean.parseBoolean(TerraMaster.props.getProperty(TerraSyncDirectoryTypes.TERRAIN.name(), "true"));
		objects = Boolean.parseBoolean(TerraMaster.props.getProperty(TerraSyncDirectoryTypes.OBJECTS.name(), "true"));
		buildings = Boolean
				.parseBoolean(TerraMaster.props.getProperty(TerraSyncDirectoryTypes.BUILDINGS.name(), "false"));
		maxAge = Long.parseLong(TerraMaster.props.getProperty("MaxTileAge", "" + 0));
		
	}

}
