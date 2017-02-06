import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import net.sf.ivmaidns.dns.DNSConnection;
import net.sf.ivmaidns.dns.DNSMsgHeader;
import net.sf.ivmaidns.dns.DNSName;
import net.sf.ivmaidns.dns.DNSRecord;
import net.sf.ivmaidns.util.UnsignedInt;

/**
 * Implementation of the new TerraSync Version
 * 
 * @author keith.paterson
 */

public class HTTPTerraSync extends Thread implements TileService {

	Logger log = Logger.getLogger(this.getClass().getName());

	private static final int RESET = 1;
	private static final int UPDATE = 2;  
	private static final int EXTEND = 3;
	private static final String TERRASYNC_SERVERS = "nameservers.bin";
	private LinkedList<TileName> syncList = new LinkedList<TileName>();
	private boolean cancelFlag = false;
	private boolean noquit = true;

	private ArrayList<URL> urls = new ArrayList<URL>();
	Random rand = new Random();
	private File localBaseDir;

	public HTTPTerraSync() {
		super("HTTPTerraSync");
		
	}

	@Override
	public void setScnPath(File file) {
		localBaseDir = file;
	}

	@Override
	public void sync(Collection<TileName> set) {
		synchronized (syncList) {
			syncList.addAll(set);
			cancelFlag = false;
		}
		synchronized (this) {
			try {
				notify();
			} // wake up the main loop
			catch (IllegalMonitorStateException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public Collection<TileName> getSyncList() {
		return syncList;
	}

	@Override
	public void quit() {
		// TODO Auto-generated method stub

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
          //Expecting to throw error
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
		while (noquit) {
			synchronized (this) {
				try {
					wait();
				} catch (InterruptedException e) {
				}
			}
			HashSet<String> apt = new HashSet<String>();
			//update progressbar
		    invokeLater(EXTEND, syncList.size() * 400 + 3000); // update
			while (syncList.size() > 0) {
				queryDNSServer();
				final TileName n;
				synchronized (syncList) {
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
	}

	/**
	 * 
	 * @param path
	 * @return
	 * @throws IOException
	 */

	private HashSet<String> syncTile(String path) throws IOException {
		try {
		  if(terrain)
		  {
        int updates = syncDirectory(TerraSyncDirectoryTypes.TERRAIN.dirname + path, false, TerraSyncDirectoryTypes.TERRAIN);        
        invokeLater(UPDATE, 200 - updates); // update progressBar
		  }
      if(objects)
      {
        int updates = syncDirectory(TerraSyncDirectoryTypes.OBJECTS.dirname + path, false, TerraSyncDirectoryTypes.OBJECTS);        
        invokeLater(UPDATE, 200 - updates); // update progressBar
      }
      if(buildings)
      {
        int updates = syncDirectory(TerraSyncDirectoryTypes.BUILDINGS.dirname + path, false, TerraSyncDirectoryTypes.BUILDINGS);        
        invokeLater(UPDATE, 200 - updates); // update progressBar
      }
			HashSet<String> apt = findAirports(new File(localBaseDir, TerraSyncDirectoryTypes.TERRAIN + path));
			return apt;

		} catch (Exception e) {
		  log.log(Level.SEVERE, "Can't sync tile " + path, e);
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
		invokeLater(UPDATE, 3000 - nodes.size() * 100);
		for (String node : nodes) {
			int updates = syncDirectory(node, false, TerraSyncDirectoryTypes.AIRPORTS);
			invokeLater(UPDATE, 100 - updates);
		}
	}

	private URL getBaseUrl() {
		// TODO Auto-generated method stub
		return urls.get(rand.nextInt(urls.size()));
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
		log.info(fileURL.toExternalForm());
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

			log.info("Content-Type = " + contentType);
			log.info("Content-Disposition = " + disposition);
			log.info("Content-Length = " + contentLength);
			log.info("fileName = " + fileName);

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

			log.info("File downloaded");
			return outputStream.toByteArray();
		} else {
			log.warning("No file to download. Server replied HTTP code: " + responseCode);
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
		try {
			int updates = 0;
			if (cancelFlag)
				return updates;
			String remoteDirIndex = new String(getFile(new URL(getBaseUrl().toExternalForm() + path.replace("\\", "/") + "/.dirindex")));
			String localDirIndex = readDirIndex(path);
			String[] lines = remoteDirIndex.split("\r?\n");
			String[] localLines = localDirIndex.split("\r?\n");
			HashMap<String, String> lookup = new HashMap<String, String>();
			for (int i = 0; i < localLines.length; i++) {
				String line = localLines[i];
				String[] splitLine = line.split(":");
				if (splitLine.length > 2)
					lookup.put(splitLine[1], splitLine[2]);
			}
			for (int i = 0; i < lines.length; i++) {
				if (cancelFlag)
					return updates;
				String file = lines[i];
				String[] splitLine = file.split(":");
				if (file.startsWith("d:")) {
					// We've got a directory if force ignore what we know
					// otherwise check the SHA against
					// the one from the server
					if (force || !splitLine[2].equals(lookup.get(splitLine[1])))
						updates += syncDirectory(path + "/" + splitLine[1], force, models);
				} else if (file.startsWith("f:")) {
					// We've got a file
					File localFile = new File(localBaseDir, path + File.separator + splitLine[1]);
					boolean load = true;
					if (localFile.exists()) {
						log.info(localFile.getAbsolutePath());
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
						byte[] fileContent = getFile(
								new URL(getBaseUrl().toExternalForm() + path.replace("\\", "/") + "/" + splitLine[1]));
						FileOutputStream fos = new FileOutputStream(localFile);
						fos.write(fileContent);
						fos.flush();
						fos.close();
					}
					invokeLater(UPDATE, 1);
					updates++;
				}
				log.info(file);
			}
			if (models == TerraSyncDirectoryTypes.OBJECTS || models == TerraSyncDirectoryTypes.TERRAIN || models == TerraSyncDirectoryTypes.BUILDINGS)
				TerraMaster.addScnMapTile(TerraMaster.mapScenery, new File(localBaseDir, path), models);

			storeDirIndex(path, remoteDirIndex);
			return updates;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return 0;
	}

	private String readDirIndex(String path) throws NoSuchAlgorithmException, IOException {
		File file = new File(new File(localBaseDir, path), ".dirindex");
		return file.exists() ? new String(readFile(file)) : "";
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

	public String bytesToHex(byte[] bytes) {
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
				bos.write(buffer, off, n);
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
	 * Queries the DNS Server for the records pointing to the terrasync servers.
	 * Always queries 8.8.8.8 (Google). If nothing is received it uses the last
	 * ones
	 */

	private void queryDNSServer() {
		if (urls.size() > 0)
			return;
		int index, len = 0, rcode, count = 0;
		boolean isZone = false, isNS = false, isPlain = false;
		String queryName = null;
		String fileName = null;
		InetAddress server;
		//Get the system dns
		List<String> nameservers = sun.net.dns.ResolverConfiguration.open().nameservers();
		//Add google
		nameservers.add(0, "8.8.8.8");
		for (String serverName : nameservers) {
			try {
				server = InetAddress.getByName(serverName);
			} catch (UnknownHostException e) {
			  log.log(Level.WARNING, "Host unknown: " + serverName);
				continue;
			}
			DNSName qName = null;
			try {
				qName = new DNSName("terrasync.flightgear.org", null);
			} catch (Exception e) {
			  log.log(Level.WARNING, "DNS Name can't be created", e);
        continue;
			}

			log.info("Connecting to " + server + " to query " + qName.getDomain() + "...");
			DNSConnection connection = new DNSConnection();
			try {
				connection.open(server);
			} catch (IOException e) {
				log.log(Level.WARNING, "Could not establish connection to: " + serverName, e);
			}
			DNSMsgHeader qHeader, header = null;
			DNSRecord[] records = null;
			DNSRecord resRecord;
			byte[] msgBytes;
			DNSName[] servers = new DNSName[0];
			rcode = DNSMsgHeader.NOERROR;
			if (isNS || !isZone) {
				log.fine("Sending " + (qName.getLevel() > 0 ? qName.getRelativeAt(0) : "root") + (isNS ? " domain" : "")
						+ " query...");
				qHeader = DNSMsgHeader.construct(DNSMsgHeader.QUERY, true, 1, 0, 0, 0, false);
				records = new DNSRecord[1];
				records[0] = new DNSRecord(qName, isNS ? DNSRecord.NS : DNSRecord.ANY, DNSRecord.IN);
				msgBytes = DNSConnection.encode(qHeader, records);
				try {
					connection.send(msgBytes);
				} catch (IOException e) {
				  log.log(Level.WARNING, "Data transmission error!", e);
					continue;
				}
				log.fine("Receiving answer...");
				try {
					msgBytes = connection.receive(true);
				} catch (IOException e) {
					connection.close();
          log.log(Level.WARNING, "Data transmission error!", e);
					continue;
				}
				if ((records = DNSConnection.decode(msgBytes)) == null) {
					connection.close();
					System.err.println("Invalid protocol message received!");
					continue;
				}
				header = new DNSMsgHeader(msgBytes);
				if (!header.isResponse() || header.getId() != qHeader.getId()) {
					connection.close();
					log.warning("Bad protocol message header: " + header.toString());
					continue;
				}
				log.info("Authoritative answer: " + (header.isAuthoritativeAnswer() ? "Yes" : "No"));
				if ((rcode = header.getRCode()) != DNSMsgHeader.NOERROR)
					log.info(rcode == DNSMsgHeader.NXDOMAIN
							? (isNS ? "Domain does not exist!" : "Requested name does not exist!")
							: "Server returned error: "
									+ UnsignedInt.toAbbreviation(rcode, DNSMsgHeader.RCODE_ABBREVS));
				len = records.length;
				if ((index = header.getQdCount()) < len) {
					count = header.getAnCount();
					if (!isNS) {
						int section = 1;
						log.info("Answer:");
						urls.clear();
						do {

							while (count <= 0) {
								count = len;
								String str = "";
								if (++section == 2) {
									count = header.getNsCount();
									str = "Name servers:";
								} else if (section == 3) {
									count = header.getArCount();
									str = "Additional:";
								}
								log.info(str);
							}
							log.info(" ");
							Object[] rData = records[index].getRData();
							if( rData.length >= 4)
							{
              String regex = (String) rData[4];
							String[] tokens = regex.split("!");
							Pattern p = Pattern.compile(tokens[1]);
							Matcher m = p.matcher(qName.getAbsolute());
							if (m.find())
							{
								try {
									urls.add(new URL(m.replaceAll(tokens[2] + "/")));
								} catch (MalformedURLException e) {
									e.printStackTrace();
								}
							}
							}
							log.info(records[index].toString(null, null, false));
							index++;
							count--;
						} while (index < len);
						if( urls.size() > 0 )
						{
						  //We have some servers so we can return
						  return;
						}
					} else if (rcode == DNSMsgHeader.NOERROR) {
						boolean found = false;
						log.info("Found authoritative name servers:");
						servers = new DNSName[count];
						for (int index2 = 0; index2 < count && index < len; index2++)
							if ((resRecord = records[index++]).getRType() == DNSRecord.NS
									&& qName.equals(resRecord.getRName())) {
								if (!found) {
									found = true;
									log.info("");
								}
								log.info(" ");
								log.info((servers[index2] = (DNSName) resRecord.getRData()[0]).getAbsolute());
							}
						if (!found) {
							log.info(" none");
							log.info("Domain does not exist!");
							rcode = DNSMsgHeader.NXDOMAIN;
						}
					}
				}
				if (rcode != DNSMsgHeader.NOERROR) {
					connection.close();
					// return rcode == DNSMsgHeader.NXDOMAIN ? 5 : 6;
					return;
				}
			}
			if (isZone) {
				index = servers.length;
				DNSName rName;
				if (index != 0) {
					try {
						rName = new DNSName(serverName, null);
						while (!rName.equals(servers[index - 1]) && --index > 0)
							;
					} catch (NumberFormatException e) {
					}
					if (index > 0) {
						servers[index - 1] = null;
						index = 0;
					} else {
						connection.close();
						serverName = null;
					}
				}
				int size = -1, receivedBytesCount = 0, time;
				String errStr = null;
				Object[] soaRData = null;
				do {
					if (serverName == null) {
						do {
							if (index >= servers.length)
								return;
						} while (servers[index++] == null);
						serverName = servers[index - 1].getRelativeAt(0);
						log.info("Connecting to " + serverName + "...");
						try {
							server = InetAddress.getByName(serverName);
						} catch (UnknownHostException e) {
							System.err.println("Host unknown!");
							serverName = null;
							rcode = 9;
							continue;
						} catch (SecurityException e) {
							serverName = null;
							rcode = 9;
							continue;
						}
						try {
							connection.open(server);
						} catch (IOException e) {
							System.err.println("Could not establish connection to: " + serverName);
							serverName = null;
							rcode = 8;
							continue;
						}
					}
					log.info("Sending zone query for: " + qName.getRelativeAt(0));
					qHeader = DNSMsgHeader.construct(DNSMsgHeader.QUERY, false, 1, 0, 0, 0, false);
					records = new DNSRecord[1];
					records[0] = new DNSRecord(qName, DNSRecord.AXFR, DNSRecord.IN);
					msgBytes = DNSConnection.encode(qHeader, records);
					try {
						connection.send(msgBytes);
					} catch (IOException e) {
						connection.close();
						System.err.println("Data transmission error!");
						serverName = null;
						rcode = 7;
						continue;
					}
					log.info("Waiting for response...");
					receivedBytesCount = 0;
					errStr = null;
					time = (int) System.currentTimeMillis();
					try {
						if ((records = DNSConnection.decode(msgBytes = connection.receive(true))) == null) {
							connection.close();
							System.err.println("Invalid protocol message received!");
							serverName = null;
							rcode = 7;
							continue;
						}
						header = new DNSMsgHeader(msgBytes);
						if (!header.isResponse() || header.getId() != qHeader.getId() && header.getId() != 0) {
							connection.close();
							System.err.println("Bad protocol message header: " + header.toString());
							serverName = null;
							rcode = 7;
							continue;
						}
						if ((rcode = header.getRCode()) != DNSMsgHeader.NOERROR) {
							connection.close();
							System.err.println(rcode == DNSMsgHeader.REFUSED ? "Zone access denied by this server!"
									: "Server returned error: "
											+ UnsignedInt.toAbbreviation(rcode, DNSMsgHeader.RCODE_ABBREVS));
							rcode = rcode == DNSMsgHeader.REFUSED ? 4 : 6;
							serverName = null;
							continue;
						}
						if ((rcode = header.getAnCount()) <= 0
								|| (count = header.getQdCount()) > records.length - rcode) {
							connection.close();
							System.err.println("None answer records received!");
							serverName = null;
							rcode = 6;
							continue;
						}
						if ((resRecord = records[count]).getRType() != DNSRecord.SOA
								|| resRecord.getRClass() != DNSRecord.IN
								|| !(rName = resRecord.getRName()).equals(qName)) {
							connection.close();
							System.err.println(
									"Non-authoritative record received: " + resRecord.toString(null, null, false));
							serverName = null;
							rcode = 6;
							continue;
						}
						qName = rName;
						soaRData = resRecord.getRData();
						if (soaRData.length <= DNSRecord.SOA_MINTTL_INDEX) {
							connection.close();
							System.err.println("Invalid authority data received!");
							serverName = null;
							rcode = 6;
							continue;
						}
						size = 0;
						if (fileName != null) {
							DNSRecord[] curRecords = records;
							receivedBytesCount = msgBytes.length;
							log.info("Getting zone records ");
							records[0] = resRecord;
							rcode--;
							count++;
							size = 1;
							do {
								while (rcode-- > 0 && (resRecord = curRecords[count++]).getRType() != DNSRecord.SOA)
									if (resRecord.getRClass() == DNSRecord.IN
											|| resRecord.getRName().isInDomain(qName, false)) {
										if (size % 100 == 1) {
											log.info(".");
										}
										records[size++] = resRecord;
									}
								if (rcode >= 0)
									break;
								receivedBytesCount += (msgBytes = connection.receive(true)).length;
								if ((curRecords = DNSConnection.decode(msgBytes)) == null
										|| !(header = new DNSMsgHeader(msgBytes)).isResponse()
										|| (count = header.getQdCount()) > curRecords.length - header.getAnCount()) {
									errStr = "Invalid protocol message received!";
									break;
								}
								if ((rcode = header.getRCode()) != DNSMsgHeader.NOERROR) {
									errStr = "Server returned error: "
											+ UnsignedInt.toAbbreviation(rcode, DNSMsgHeader.RCODE_ABBREVS);
									break;
								}
								if (records.length - (rcode = header.getAnCount()) < size) {
									int newSize;
									DNSRecord[] newRecords;
									if ((newSize = (size >> 1) + rcode + size + 1) <= size)
										newSize = -1 >>> 1;
									System.arraycopy(records, 0, newRecords = new DNSRecord[newSize], 0, size);
									records = newRecords;
								}
							} while (true);
						}
					} catch (EOFException e) {
						errStr = "Connection terminated by server!";
					} catch (InterruptedIOException e) {
						errStr = "Connection time-out!";
					} catch (IOException e) {
						errStr = "Data transmission error!";
					}
					time = (int) System.currentTimeMillis() - time;
					connection.close();
					if (size > 0)
						log.info("");
					if (errStr != null)
						System.err.println(errStr);
					if (size < 0)
						serverName = null;
					else
						break;
				} while (true);
			}
		}
		if (!urls.isEmpty()) {
			try {
				ObjectOutputStream ois = new ObjectOutputStream(new FileOutputStream(TERRASYNC_SERVERS));
				ArrayList<String> s = new ArrayList<>();
				for (URL url : urls) {
          s.add(url.toString());
        }
				ois.writeObject(s);
			} catch (IOException e1) {
			  log.log(Level.WARNING, e1.getMessage(), e1);
			}
		} else {
			try {
				ObjectInputStream ois = new ObjectInputStream(new FileInputStream(TERRASYNC_SERVERS));
				urls = (ArrayList<URL>) ois.readObject();
			} catch (IOException e1) {
        log.log(Level.WARNING, e1.getMessage(), e1);
			} catch (ClassNotFoundException e) {
        log.log(Level.WARNING, e.getMessage(), e);
			}
		}
	}

	/**
	 * Does the Async notification of the GUI
	 * 
	 * @param action
	 */

	private void invokeLater(final int action, final int num) {
		if (num < 0)
			log.info("Update < 0");
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
    buildings = Boolean.parseBoolean(TerraMaster.props.getProperty(TerraSyncDirectoryTypes.BUILDINGS.name(), "false"));
  }

}
