package org.flightgear.terramaster.dns;

import java.io.EOFException;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.flightgear.terramaster.TerraMaster;

import net.sf.ivmaidns.dns.DNSConnection;
import net.sf.ivmaidns.dns.DNSMsgHeader;
import net.sf.ivmaidns.dns.DNSName;
import net.sf.ivmaidns.dns.DNSRecord;
import net.sf.ivmaidns.util.UnsignedInt;
import sun.net.dns.ResolverConfiguration;

public class FlightgearNAPTRQuery {

	public class NAPTRComparator implements Comparator<Object[]> {
	
		@Override
		public int compare(Object[] o1, Object[] o2) {
			
			if(o1[2].equals(o1[2]))
				return ((Comparable)o1[3]).compareTo(o1[3]);
			else
				return ((Comparable)o1[2]).compareTo(o1[2]);
		}
	
	}

	Logger log = Logger.getLogger(TerraMaster.LOGGER_CATEGORY);

	private List<WeightedUrl> urls = new ArrayList<>();
	private static final String TERRASYNC_SERVERS = "nameservers.bin";


	/**
	 * Queries the DNS Server for the records pointing to the terrasync servers.
	 * Always queries 8.8.8.8 (Google). If nothing is received it uses the last
	 * ones
	 * @param sceneryType TODO
	 * @return 
	 */
	
	public List<WeightedUrl> queryDNSServer(String sceneryType) {
		refresh();
		if (urls.size() > 0)
			return urls;
		int index, len = 0, rcode, count = 0;
		boolean isZone = false, isNS = false, isPlain = false;
		String queryName = null;
		String fileName = null;
		InetAddress server;
		// Get the system dns
		 		
		ResolverConfiguration config = sun.net.dns.ResolverConfiguration.open();
		List<String> nameservers = config.nameservers();
		
		// Add google
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
	
			log.fine("Connecting to " + server + " to query " + qName.getDomain() + "...");
			DNSConnection connection = new DNSConnection();
			try {
				connection.open(server);
			} catch (IOException e) {
				log.log(Level.WARNING, "Could not establish connection to: " + serverName, e);
				continue;
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
					log.log(Level.WARNING, "Data transmission error Receiving Answer!", e);
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
				log.finer("Authoritative answer: " + (header.isAuthoritativeAnswer() ? "Yes" : "No"));
				if ((rcode = header.getRCode()) != DNSMsgHeader.NOERROR)
					log.finer(rcode == DNSMsgHeader.NXDOMAIN
							? (isNS ? "Domain does not exist!" : "Requested name does not exist!")
							: "Server returned error: "
									+ UnsignedInt.toAbbreviation(rcode, DNSMsgHeader.RCODE_ABBREVS));
				len = records.length;
				if ((index = header.getQdCount()) < len) {
					count = header.getAnCount();
					if (!isNS) {
						int section = 1;
						log.finer("Answer:");
						urls.clear();
						ArrayList<Object[]> hostRecords = new ArrayList<>();
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
								log.finer(str);
							}
							Object[] rData = records[index].getRData();
							if(rData.length == 6)
							{
							  hostRecords.add(rData);
							}
							log.finer(records[index].toString(null, null, false));
							index++;
							count--;
						} while (index < len);
						urls = getUrls(hostRecords, sceneryType, qName);
						if (urls.size() > 0) {
							// We have some servers so we can return
							return urls;
						}
					} else if (rcode == DNSMsgHeader.NOERROR) {
						boolean found = false;
						log.finer("Found authoritative name servers:");
						servers = new DNSName[count];
						for (int index2 = 0; index2 < count && index < len; index2++)
							if ((resRecord = records[index++]).getRType() == DNSRecord.NS
									&& qName.equals(resRecord.getRName())) {
								if (!found) {
									found = true;
								}
								log.info((servers[index2] = (DNSName) resRecord.getRData()[0]).getAbsolute());
							}
						if (!found) {
							log.finer(" none");
							log.finer("Domain does not exist!");
							rcode = DNSMsgHeader.NXDOMAIN;
						}
					}
				}
				if (rcode != DNSMsgHeader.NOERROR) {
					connection.close();
					// return rcode == DNSMsgHeader.NXDOMAIN ? 5 : 6;
					return urls;
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
								return urls;
						} while (servers[index++] == null);
						serverName = servers[index - 1].getRelativeAt(0);
						log.finer("Connecting to " + serverName + "...");
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
					log.finer("Sending zone query for: " + qName.getRelativeAt(0));
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
					log.finer("Waiting for response...");
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
							log.finer("Getting zone records ");
							records[0] = resRecord;
							rcode--;
							count++;
							size = 1;
							do {
								while (rcode-- > 0 && (resRecord = curRecords[count++]).getRType() != DNSRecord.SOA)
									if (resRecord.getRClass() == DNSRecord.IN
											|| resRecord.getRName().isInDomain(qName, false)) {
										if (size % 100 == 1) {
											log.finer(".");
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
						log.finer("");
					if (errStr != null)
						log.log(Level.WARNING, errStr);
					if (size < 0)
						serverName = null;
					else
						break;
				} while (true);
			}
		}
		if (!urls.isEmpty()) {
			storeURLs();
		} else {
			readURLs();
		}
		return urls;
	}
	
	private void refresh() {
		try {
			Method declaredMethod = sun.net.dns.ResolverConfigurationImpl.class.getDeclaredMethod("loadDNSconfig0");
			declaredMethod.setAccessible(true);
			declaredMethod.invoke(null);
		} catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
	}

	/**
	 * Gets the URLs from the list of NAPTR Records
	 * @param hostRecords
	 * @return
	 */


	private ArrayList<WeightedUrl> getUrls(ArrayList<Object[]> hostRecords, String protocol, DNSName qName) {
		ArrayList<WeightedUrl> urls = new ArrayList<>();
		Collections.sort(hostRecords, new NAPTRComparator());
		long order = -1;
		for (Object[] objects : hostRecords) {
			if(order < 0)
				order = Long.parseLong(objects[0].toString());
			if(Long.parseLong(objects[0].toString()) == order && objects[3].toString().equals(protocol))
				urls.add(new WeightedUrl(objects[1].toString(), (String)objects[4], qName ));
				
				
		}
		return urls;
	}


	private void readURLs() {
		try {
			ObjectInputStream ois = new ObjectInputStream(new FileInputStream(TERRASYNC_SERVERS));
			urls = (List<WeightedUrl>) ois.readObject();
		} catch (IOException e1) {
			log.log(Level.WARNING, e1.getMessage(), e1);
		} catch (ClassNotFoundException e) {
			log.log(Level.WARNING, e.getMessage(), e);
		}
	}


	private void storeURLs() {
		try {
			ObjectOutputStream ois = new ObjectOutputStream(new FileOutputStream(TERRASYNC_SERVERS));
			ArrayList<String> s = new ArrayList<>();
			for (WeightedUrl url : urls) {
				s.add(url.toString());
			}
			ois.writeObject(s);
		} catch (IOException e1) {
			log.log(Level.WARNING, e1.getMessage(), e1);
		}
	}

}
