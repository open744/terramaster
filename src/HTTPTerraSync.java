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
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
			while (syncList.size() > 0) {
				queryServer();
				final TileName n;
				synchronized (syncList) {
					n = syncList.getFirst();
				}

				String name = n.getName();
				if (name.startsWith("MODELS")) {
					int i = name.indexOf('-');
					if (i > -1)
						syncDirectory(name.substring(i + 1));
					else
						syncModels();
				} else {
					String path = n.buildPath();
					if (path != null)
						try {
							sync(path);

						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
				}

				synchronized (syncList) {
					syncList.remove(n);
				}
			}

			// syncList is now empty
			invokeLater(1); // reset progressBar
		}
	}

	/**
	 * 
	 * @param path
	 * @throws IOException
	 */

	private void sync(String path) throws IOException {
		try {
			syncDirectory("Terrain/" + path);
			invokeLater(2); // update progressBar
			syncDirectory("Objects/" + path);
			invokeLater(2); // update progressBar
			String[] apt = findAirports(new File(localBaseDir, "Terrain/"
					+ path));
			if (apt != null) {
				for (int j = 0; j < apt.length; ++j)
					invokeLater(3); // extend progressBar
				syncAirports(apt);
			}

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
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
		return set.toArray(new String[1]);
	}

	// sync "Airports/W/A/T"
	private void syncAirports(String[] names) throws IOException {
		long rev;

		for (String i : names) {
			String node = String.format("Airports/%c/%c/%c", i.charAt(0),
					i.charAt(1), i.charAt(2));
			syncDirectory(node);
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

	private byte[] getFile(URL fileURL) throws IOException,
			FileNotFoundException {
		System.out.println(fileURL.toExternalForm());
		HttpURLConnection httpConn = (HttpURLConnection) fileURL
				.openConnection();
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
					fileName = disposition.substring(index + 10,
							disposition.length() - 1);
				}
			} else {
				fileName = fileURL.getFile();
			}

			System.out.println("Content-Type = " + contentType);
			System.out.println("Content-Disposition = " + disposition);
			System.out.println("Content-Length = " + contentLength);
			System.out.println("fileName = " + fileName);

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

			System.out.println("File downloaded");
			return outputStream.toByteArray();
		} else {
			System.out
					.println("No file to download. Server replied HTTP code: "
							+ responseCode);
		}
		httpConn.disconnect();
		return null;
	}

	private void syncModels() {
		try {
			syncDirectory("Models");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void syncDirectory(String path) {
		try {
			if (cancelFlag)
				return;
			String dirIndex = new String(getFile(new URL(getBaseUrl()
					.toExternalForm() + path + "/.dirindex")));
			String[] lines = dirIndex.split("\r?\n");
			for (int i = 0; i < lines.length; i++) {
				if (cancelFlag)
					return;
				String file = lines[i];
				String[] splitLine = file.split(":");
				if (file.startsWith("d:")) {
					syncDirectory(path + "/" + splitLine[1]);
				} else if (file.startsWith("f:")) {
					invokeLater(3);
					File localFile = new File(localBaseDir, path
							+ File.separator + splitLine[1]);
					boolean load = true;
					if (localFile.exists()) {
						System.out.println(localFile.getAbsolutePath());
						byte[] b = calcSHA1(localFile);
						String bytesToHex = bytesToHex(b);
						// System.out.println(bytesToHex);
						load = !splitLine[2].equals(bytesToHex);
					} else {
						if (!localFile.getParentFile().exists()) {
							localFile.getParentFile().mkdirs();
						}
					}
					if (load) {
						byte[] fileContent = getFile(new URL(getBaseUrl()
								.toExternalForm() + path + "/" + splitLine[1]));
						FileOutputStream fos = new FileOutputStream(localFile);
						fos.write(fileContent);
						fos.flush();
						fos.close();
					}
					invokeLater(2);
				}
				System.out.println(file);

			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	final protected static char[] hexArray = "0123456789abcdef".toCharArray();

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

	private byte[] calcSHA1(File file) throws NoSuchAlgorithmException,
			IOException {
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
	 * Queries the DNS Server for the records pointing to the terrasync servers. 
	 * Always queries 8.8.8.8 (Google). If nothing is received it uses the last ones
	 */

	private void queryServer() {
		if (urls.size() > 0)
			return;
		int index, len = 0, rcode, count = 0;
		boolean isZone = false, isNS = false, isPlain = false;
		String queryName = null;
		String fileName = null;
		InetAddress server;
		// The default Google DNS which should work everywhere
		String serverName = "8.8.8.8";
		try {
			server = InetAddress.getByName(serverName);
		} catch (UnknownHostException e) {
			System.err.println("Host unknown: " + serverName);
			return;
		}
		DNSName qName = null;
		try {
			qName = new DNSName("terrasync.flightgear.org", null);
		} catch (NumberFormatException e) {
			e.printStackTrace();
		}

		System.out.println("Connecting to " + qName.getDomain() + "...");
		DNSConnection connection = new DNSConnection();
		try {
			connection.open(server);
		} catch (IOException e) {
			System.err.println("Could not establish connection to: "
					+ serverName);
		}
		DNSMsgHeader qHeader, header = null;
		DNSRecord[] records = null;
		DNSRecord resRecord;
		byte[] msgBytes;
		DNSName[] servers = new DNSName[0];
		rcode = DNSMsgHeader.NOERROR;
		if (isNS || !isZone) {
			System.out.println("Sending "
					+ (qName.getLevel() > 0 ? qName.getRelativeAt(0) : "root")
					+ (isNS ? " domain" : "") + " query...");
			qHeader = DNSMsgHeader.construct(DNSMsgHeader.QUERY, true, 1, 0, 0,
					0, false);
			records = new DNSRecord[1];
			records[0] = new DNSRecord(qName, isNS ? DNSRecord.NS
					: DNSRecord.ANY, DNSRecord.IN);
			msgBytes = DNSConnection.encode(qHeader, records);
			try {
				connection.send(msgBytes);
			} catch (IOException e) {
				System.err.println("Data transmission error!");
				e.printStackTrace();
				return;
			}
			System.out.println("Receiving answer...");
			try {
				msgBytes = connection.receive(true);
			} catch (IOException e) {
				connection.close();
				System.err.println("Data transmission error!");
				e.printStackTrace();
				return;
			}
			if ((records = DNSConnection.decode(msgBytes)) == null) {
				connection.close();
				System.err.println("Invalid protocol message received!");
				return;
			}
			header = new DNSMsgHeader(msgBytes);
			if (!header.isResponse() || header.getId() != qHeader.getId()) {
				connection.close();
				System.err.println("Bad protocol message header: "
						+ header.toString());
				return;
			}
			System.out.println("Authoritative answer: "
					+ (header.isAuthoritativeAnswer() ? "Yes" : "No"));
			if (header.isAuthenticData())
				System.out.println("Authentic data received");
			if (header.isTruncated())
				System.out.println("Response message truncated!");
			if ((rcode = header.getRCode()) != DNSMsgHeader.NOERROR)
				System.out
						.println(rcode == DNSMsgHeader.NXDOMAIN ? (isNS ? "Domain does not exist!"
								: "Requested name does not exist!")
								: "Server returned error: "
										+ UnsignedInt.toAbbreviation(rcode,
												DNSMsgHeader.RCODE_ABBREVS));
			len = records.length;
			if ((index = header.getQdCount()) < len) {
				count = header.getAnCount();
				if (!isNS) {
					int section = 1;
					System.out.println("Answer:");
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
							System.out.println(str);
						}
						System.out.print(" ");
						String regex = (String) (records[index].getRData())[4];
						String[] tokens = regex.split("!");
						Pattern p = Pattern.compile(tokens[1]);
						Matcher m = p.matcher(qName.getAbsolute());
						if (m.find())
							try {
								urls.add(new URL(m.replaceAll(tokens[2] + "/")));
							} catch (MalformedURLException e) {
								e.printStackTrace();
							}
						System.out.println(records[index].toString(null, null,
								false));
						index++;
						count--;
					} while (index < len);
				} else if (rcode == DNSMsgHeader.NOERROR) {
					boolean found = false;
					System.out.print("Found authoritative name servers:");
					servers = new DNSName[count];
					for (int index2 = 0; index2 < count && index < len; index2++)
						if ((resRecord = records[index++]).getRType() == DNSRecord.NS
								&& qName.equals(resRecord.getRName())) {
							if (!found) {
								found = true;
								System.out.println("");
							}
							System.out.print(" ");
							System.out
									.println((servers[index2] = (DNSName) resRecord
											.getRData()[0]).getAbsolute());
						}
					if (!found) {
						System.out.println(" none");
						System.out.println("Domain does not exist!");
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
					System.out.println("Connecting to " + serverName + "...");
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
						System.err
								.println("Could not establish connection to: "
										+ serverName);
						serverName = null;
						rcode = 8;
						continue;
					}
				}
				System.out.println("Sending zone query for: "
						+ qName.getRelativeAt(0));
				qHeader = DNSMsgHeader.construct(DNSMsgHeader.QUERY, false, 1,
						0, 0, 0, false);
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
				System.out.println("Waiting for response...");
				receivedBytesCount = 0;
				errStr = null;
				time = (int) System.currentTimeMillis();
				try {
					if ((records = DNSConnection.decode(msgBytes = connection
							.receive(true))) == null) {
						connection.close();
						System.err
								.println("Invalid protocol message received!");
						serverName = null;
						rcode = 7;
						continue;
					}
					header = new DNSMsgHeader(msgBytes);
					if (!header.isResponse()
							|| header.getId() != qHeader.getId()
							&& header.getId() != 0) {
						connection.close();
						System.err.println("Bad protocol message header: "
								+ header.toString());
						serverName = null;
						rcode = 7;
						continue;
					}
					if ((rcode = header.getRCode()) != DNSMsgHeader.NOERROR) {
						connection.close();
						System.err
								.println(rcode == DNSMsgHeader.REFUSED ? "Zone access denied by this server!"
										: "Server returned error: "
												+ UnsignedInt
														.toAbbreviation(
																rcode,
																DNSMsgHeader.RCODE_ABBREVS));
						rcode = rcode == DNSMsgHeader.REFUSED ? 4 : 6;
						serverName = null;
						continue;
					}
					if ((rcode = header.getAnCount()) <= 0
							|| (count = header.getQdCount()) > records.length
									- rcode) {
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
						System.err
								.println("Non-authoritative record received: "
										+ resRecord.toString(null, null, false));
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
						System.out.print("Getting zone records ");
						records[0] = resRecord;
						rcode--;
						count++;
						size = 1;
						do {
							while (rcode-- > 0
									&& (resRecord = curRecords[count++])
											.getRType() != DNSRecord.SOA)
								if (resRecord.getRClass() == DNSRecord.IN
										|| resRecord.getRName().isInDomain(
												qName, false)) {
									if (size % 100 == 1) {
										System.out.print(".");
										System.out.flush();
									}
									records[size++] = resRecord;
								}
							if (rcode >= 0)
								break;
							receivedBytesCount += (msgBytes = connection
									.receive(true)).length;
							if ((curRecords = DNSConnection.decode(msgBytes)) == null
									|| !(header = new DNSMsgHeader(msgBytes))
											.isResponse()
									|| (count = header.getQdCount()) > curRecords.length
											- header.getAnCount()) {
								errStr = "Invalid protocol message received!";
								break;
							}
							if ((rcode = header.getRCode()) != DNSMsgHeader.NOERROR) {
								errStr = "Server returned error: "
										+ UnsignedInt.toAbbreviation(rcode,
												DNSMsgHeader.RCODE_ABBREVS);
								break;
							}
							if (records.length - (rcode = header.getAnCount()) < size) {
								int newSize;
								DNSRecord[] newRecords;
								if ((newSize = (size >> 1) + rcode + size + 1) <= size)
									newSize = -1 >>> 1;
								System.arraycopy(records, 0,
										newRecords = new DNSRecord[newSize], 0,
										size);
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
					System.out.println("");
				if (errStr != null)
					System.err.println(errStr);
				if (size < 0)
					serverName = null;
				else
					break;
			} while (true);
		}
		if (!urls.isEmpty()) {
			try {
				ObjectOutputStream ois = new ObjectOutputStream(new FileOutputStream(TERRASYNC_SERVERS));
				ois.writeObject(urls);
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		} else {
			try {
				ObjectInputStream ois = new ObjectInputStream(new FileInputStream(TERRASYNC_SERVERS));
				urls = (ArrayList<URL>) ois.readObject();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} catch (ClassNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
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
							.setMaximum(TerraMaster.frame.progressBar
									.getMaximum() + 1);
					break;
				}
			}
		});
	}

}
