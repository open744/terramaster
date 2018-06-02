/*
 * @(#) src/net/sf/ivmaidns/dnszcon.java --
 * Internet DNS zones multi-threaded retriever (robot).
 **
 * Copyright (c) 1999-2001 Ivan Maidanski <ivmai@mail.ru>
 * All rights reserved.
 */

/*
 * This is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 **
 * This software is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License (GPL) for more details.
 **
 * Linking this library statically or dynamically with other modules is
 * making a combined work based on this library. Thus, the terms and
 * conditions of the GNU General Public License cover the whole
 * combination.
 **
 * As a special exception, the copyright holders of this library give you
 * permission to link this library with independent modules to produce an
 * executable, regardless of the license terms of these independent
 * modules, and to copy and distribute the resulting executable under
 * terms of your choice, provided that you also meet, for each linked
 * independent module, the terms and conditions of the license of that
 * module. An independent module is a module which is not derived from
 * or based on this library. If you modify this library, you may extend
 * this exception to your version of the library, but you are not
 * obligated to do so. If you do not wish to do so, delete this
 * exception statement from your version.
 */

package net.sf.ivmaidns;

import java.io.*;

import java.net.InetAddress;
import java.net.UnknownHostException;

import net.sf.ivmaidns.dns.DNSConnection;
import net.sf.ivmaidns.dns.DNSMsgHeader;
import net.sf.ivmaidns.dns.DNSName;
import net.sf.ivmaidns.dns.DNSRecord;

import net.sf.ivmaidns.storage.HashedStorage;
import net.sf.ivmaidns.storage.ListStorage;
import net.sf.ivmaidns.storage.ObjectStorage;
import net.sf.ivmaidns.storage.SortedStorage;

import net.sf.ivmaidns.util.ActivityCore;
import net.sf.ivmaidns.util.ConstVector;
import net.sf.ivmaidns.util.IntVector;
import net.sf.ivmaidns.util.JavaConsts;
import net.sf.ivmaidns.util.Lockable;
import net.sf.ivmaidns.util.MultiObservable;
import net.sf.ivmaidns.util.Notifiable;
import net.sf.ivmaidns.util.ObservedCore;
import net.sf.ivmaidns.util.SafeRunnable;
import net.sf.ivmaidns.util.UnsignedInt;

/**
 * Internet DNS zones multi-threaded retriever.
 **
 * @version 3.2
 * @author Ivan Maidanski
 */
public final class dnszcon implements Notifiable
{

 public static final String NAME = "dnszcon";

 public static final String VERSION = "3.2";

 public static final String DESCRIPTION =
  "Internet DNS zones multi-threaded retriever";

 public static final String COPYRIGHT =
  "(c) 2001 Ivan Maidanski <ivmai@mail.ru> http://ivmaidns.sf.net";

 public static final String LICENSE =
  "This is free software. No warranties. All rights reserved.";

 public static final String ARGS_INFO = "<out_text_file>";

 protected static final int SCREEN_WIDTH = 80;

 protected static final int SCREEN_HEIGHT = 25;

 protected boolean recurse;

 protected final SortedStorage storage = new SortedStorage();

 private dnszcon() {}

 public static final void main(String[] args)
  throws NullPointerException
 {
  int exitCode;
  try
  {
   exitCode = (new dnszcon()).intMain(args);
  }
  catch (OutOfMemoryError e)
  {
   System.err.println("Out of memory!");
   exitCode = 255;
  }
  try
  {
   Runtime.getRuntime().exit(exitCode);
  }
  catch (SecurityException e) {}
  exitCode = 0;
 }

 public int intMain(String[] args)
  throws NullPointerException
 {
  if (args.length != 1)
  {
   System.out.println(NAME + " v" + VERSION + " - " + DESCRIPTION);
   System.out.println(COPYRIGHT);
   System.out.println(LICENSE);
   System.out.println("");
   System.out.println("Usage: " + NAME + " " + ARGS_INFO);
   System.out.println("");
   System.out.println("This tool allows the user to fetch" +
    " (retrieve) Internet DNS records");
   System.out.println("and entire DNS zones of a given name from" +
    " their authoritative name");
   System.out.println("servers. The tool is an interactive" +
    " console program which processes");
   System.out.println("user retrieval queries asynchronously in" +
    " the background (up to 30");
   System.out.println("queries to different name servers at a" +
    " time). All retrieved records");
   System.out.println("are immediately put into the built-in" +
    " sorted storage, which may be");
   System.out.println("listed by user at run-time, and which is" +
    " saved to the specified");
   System.out.println("file (in a textual tab-separated format)" +
    " on exit. An entire zone");
   System.out.println("retrieving (zone transferring) is possible" +
    " only if it is allowed at");
   System.out.println("least by one of its authoritative" +
    " name servers. Subzones for the");
   System.out.println("zone being transferred are retrieved" +
    " only in the recursive mode.");
   return args.length > 0 ? 2 : 0;
  }
  int location = 0;
  DNSName rName = null;
  DNSClientRobot robot = new DNSClientRobot();
  SortedStorage storage = this.storage;
  BufferedReader input =
   new BufferedReader(new InputStreamReader(System.in));
  System.out.println("");
  System.out.println("Interactive console commands:");
  System.out.println(" <zone> <server1> ... -" +
   " get and list name server records for <zone>");
  System.out.println(" ?<name> <server1> ... -" +
   " try to retrieve all records for <name>");
  System.out.println(
   " !<zone> <server1> ... - try to retrieve entire <zone>");
  System.out.println(" <zone> -" +
   " list all retrieved records for <zone> and its subzones");
  System.out.println(" + - enable recursive mode");
  System.out.println(" - - disable recursive mode (default)");
  System.out.println(
   " ? - view retriever activity and memory utilization");
  System.out.println(" & - pause/resume activity");
  System.out.println(
   " ! - show statistics, save retrieved records to file and exit");
  System.out.println("");
  do
  {
   System.out.print(":");
   System.out.flush();
   String cmdLine = null;
   try
   {
    cmdLine = input.readLine();
   }
   catch (IOException e) {}
   if (cmdLine == null)
    break;
   int pos = 0, nextPos;
   while (pos < cmdLine.length() && cmdLine.charAt(pos) <= ' ')
    pos++;
   boolean isDomain = false, isAny = false;
   if (pos < cmdLine.length())
   {
    location = 0;
    char ch = cmdLine.charAt(nextPos = pos);
    while (++nextPos < cmdLine.length() &&
           cmdLine.charAt(nextPos) <= ' ');
    if (nextPos >= cmdLine.length())
    {
     if (ch == '!')
      break;
     if (ch == '+')
     {
      this.recurse = true;
      System.out.println("Recursive mode: On (global)");
      continue;
     }
     if (ch == '-')
     {
      this.recurse = false;
      System.out.println("Recursive mode: Off");
      continue;
     }
     if (ch == '&')
     {
      if (!robot.isSuspended())
      {
       robot.suspend();
       System.out.println("Suspending all...");
       continue;
      }
      robot.resume();
      System.out.println("Resuming all...");
      continue;
     }
     if (ch == '?')
     {
      System.out.println(robot.toString(false, true, false));
      Runtime.getRuntime().gc();
      System.out.println("Free memory: " + Integer.toString(
       (int)(Runtime.getRuntime().freeMemory() >> 10)) + " Kbytes");
      continue;
     }
    }
     else
     {
      isDomain = ch == '!';
      if ((isAny = ch == '?') || isDomain)
       pos = nextPos;
     }
    if ((nextPos = cmdLine.indexOf(' ', pos)) < 0)
     nextPos = cmdLine.length();
    try
    {
     rName = new DNSName(cmdLine.substring(pos, nextPos), null);
    }
    catch (NumberFormatException e)
    {
     System.err.println("Illegal name!");
     continue;
    }
    pos = nextPos - 1;
    while (++pos < cmdLine.length() && cmdLine.charAt(pos) <= ' ');
    if (pos == cmdLine.length())
     synchronized (storage)
     {
      location = (location =
       storage.findLessGreater(new DNSRecord(rName,
       DNSRecord.SOA, DNSRecord.IN), false, 0, false)) > 0 ?
       storage.siblingLocation(location, true) :
       storage.childLocation(0, true);
     }
   }
   if (location > 0)
    synchronized (storage)
    {
     int count = SCREEN_HEIGHT - 2;
     String str = null;
     DNSRecord prevRecord = null, resRecord = null;
     do
     {
      if (--count > 0 || prevRecord == null)
      {
       if (!(resRecord = (DNSRecord)storage.getAt(location)).
           getRName().isInDomain(rName, false))
       {
        location = 0;
        break;
       }
       count -= (str = resRecord.toString(rName,
        prevRecord, false)).length() / SCREEN_WIDTH;
      }
      if (count <= 0 && prevRecord != null)
      {
       System.out.println("<More?>");
       break;
      }
      System.out.println(str);
      prevRecord = resRecord;
     } while ((location =
              storage.siblingLocation(location, true)) > 0);
    }
   if (pos < cmdLine.length())
   {
    InetAddress[] servers = new InetAddress[16];
    int count = 0;
    do
    {
     if ((nextPos = cmdLine.indexOf(' ', pos)) < 0)
      nextPos = cmdLine.length();
     try
     {
      servers[count] =
       InetAddress.getByName(cmdLine.substring(pos, nextPos));
      count++;
     }
     catch (UnknownHostException e)
     {
      System.err.println("Host unknown: " + e.getMessage());
     }
     catch (SecurityException e) {}
     pos = nextPos - 1;
     while (++pos < cmdLine.length() && cmdLine.charAt(pos) <= ' ');
    } while (pos < cmdLine.length() && count < servers.length);
    if (count > 0)
     robot.queryAll(servers, new DNSRecord(rName, isDomain ?
      DNSRecord.AXFR : isAny ? DNSRecord.ANY : DNSRecord.NS,
      DNSRecord.IN), !(isDomain || isAny), this);
   }
  } while (true);
  this.recurse = false;
  System.out.println("");
  System.out.println(robot.toString(false, true, false));
  robot.removeAgent(this);
  robot.stop();
  System.out.println("");
  System.out.println(robot.toString(false, false, true));
  if (storage.childLocation(0, true) > 0)
  {
   System.out.println("Writing records to text file...");
   BufferedWriter out;
   try
   {
    out = new BufferedWriter(
     new OutputStreamWriter(new FileOutputStream(args[0])));
   }
   catch (IOException e)
   {
    System.err.println("Cannot create text file: " + args[0]);
    return 10;
   }
   catch (SecurityException e)
   {
    return 10;
   }
   try
   {
    out.write("; DNS records file");
    out.newLine();
    out.write("; Generated by: " + NAME + " v" + VERSION);
    out.newLine();
    synchronized (storage)
    {
     for (location = storage.childLocation(0, true); location > 0;
          location = storage.siblingLocation(location, true),
          out.newLine())
      out.write(((DNSRecord)storage.getAt(location)).toString(null,
       null, false));
    }
    out.close();
   }
   catch (IOException e)
   {
    System.err.println("File write error!");
    return 10;
   }
  }
  System.out.println("Ok");
  return 0;
 }

 public void update(MultiObservable observed, Object argument)
 {
  if (observed instanceof DNSClientRobot &&
      argument instanceof Integer)
  {
   DNSClientRobot robot = (DNSClientRobot)observed;
   int activeLoc = ((Integer)argument).intValue();
   InetAddress server = robot.getActiveServerAt(activeLoc);
   DNSRecord qdRecord = robot.getActiveQdRecordAt(activeLoc);
   DNSRecord resRecord = robot.getResRecordAt(activeLoc);
   int status = robot.getActiveStatusAt(activeLoc), qType;
   DNSName qName = qdRecord.getRName();
   if ((qType = qdRecord.getRType()) == DNSRecord.ANY ||
       qType == DNSRecord.AXFR)
    if (resRecord != null)
    {
     try
     {
      SortedStorage storage;
      synchronized (storage = this.storage)
      {
       storage.add(resRecord, true);
      }
     }
     catch (OutOfMemoryError e)
     {
      System.err.println("Thread: Out of memory!");
      robot.removeAgent(this);
     }
     Object[] rData;
     if (qType == DNSRecord.AXFR)
      if (status == 1)
       System.out.println(" " + qName.getAbsolute() +
        " [" + server.getHostName() + "]: transfering zone...");
       else if (this.recurse &&
                resRecord.getRType() == DNSRecord.NS &&
                resRecord.getRName().isInDomain(qName, true) &&
                (rData = resRecord.getRData()).length > 0)
       {
        String name = ((DNSName)rData[0]).getRelativeAt(0);
        InetAddress[] addresses = null;
        try
        {
         addresses = InetAddress.getAllByName(name);
        }
        catch (UnknownHostException e)
        {
         String ipName = ((DNSName)rData[0]).getRelative(qName);
         if (DNSRecord.isIPAddress(ipName, 0, ipName.length()))
         {
          try
          {
           addresses = InetAddress.getAllByName(ipName);
          }
          catch (UnknownHostException e1) {}
          catch (SecurityException e1) {}
         }
        }
        catch (SecurityException e) {}
        if (addresses == null)
         System.err.println(" Host unknown: " + name);
         else if (this.recurse)
          robot.queryAll(addresses,
           new DNSRecord(resRecord.getRName(),
           DNSRecord.AXFR, DNSRecord.IN), false, this);
       }
    }
     else System.out.println(" " + qName.getAbsolute() +
           " [" + server.getHostName() + "]: " + (status > 0 ?
           "finished(" + Integer.toString(status - 1) + ")" :
           status == 0 ? "could not connect!" : "query refused!"));
    else if (resRecord != null)
     System.out.println(" " + resRecord.toString(null,
      null, false));
     else if (status <= 1)
      System.out.println(status == 0 ?
       "Could not connect to: " + server.getHostName() :
       (status > 0 ? " Name server not found: " :
       " Name server request failed: ") + qName.getAbsolute());
  }
 }

 public void integrityCheck() {}
}

/**
 * Class for DNS client side robot (multi-threaded).
 **
 * @version 3.0
 * @author Ivan Maidanski
 **
 * @since 1.1
 */
final class DNSClientRobot extends ObservedCore
 implements SafeRunnable, Lockable
{

/**
 * NOTE: These are default connections limits.
 */
 public static final int MAX_CONNECTIONS = 30;

 public static final int MAX_SERVER_CONNECTIONS = 4;

/**
 * NOTE: These are default connection timeouts.
 */
 public static final int SWITCH_DELAY = 5;

 public static final int DISCONNECT_DELAY = 30;

/**
 * NOTE: lock must be != null. All synchronization must be done on
 * lock.
 */
 protected Object lock = this;

 protected boolean suspended = false;

/**
 * NOTE: Values must be > 0.
 */
 protected int maxConnections = MAX_CONNECTIONS;

 protected int maxServerConnections = MAX_SERVER_CONNECTIONS;

/**
 * NOTE: Values (in seconds) must be >= 0.
 */
 protected int switchDelay = SWITCH_DELAY;

 protected int disconnectDelay = DISCONNECT_DELAY;

 protected int queueAgentsSize;

 protected int activeAgentsSize;

 protected int activeThreadsSize;

/**
 * NOTE: queueAgents must only contain Notifiable values of the
 * ordered queries at the locations consistent with queueQdRecords
 * locations. These values should not be accessible outside.
 */
 protected final ListStorage queueAgents = new ListStorage();

/**
 * NOTE: queueQdRecords must only contain DNSRecord values of
 * queries.
 */
 protected final HashedStorage queueQdRecords = new HashedStorage();

/**
 * NOTE: queueServers contains triples of the location in
 * uniqueServers, the next and the previous locations in server
 * queries queue at the indices consistent with queueQdRecords
 * locations minus 1 (all values > 0).
 */
 protected final IntVector queueServers = new IntVector();

/**
 * NOTE: uniqueServers must only contain unique InetAddress values
 * of queries and/or active connection threads.
 */
 protected final HashedStorage uniqueServers = new HashedStorage();

/**
 * NOTE: firstServers contains groups of the first location in
 * server queries queue, server current zone transfers count, server
 * current connections count and server connections limit (0 means
 * server inaccessible) at the indices consistent with uniqueServers
 * locations minus 1 (all values >= 0 and at least one of the first
 * and third values in each group > 0).
 */
 protected final IntVector firstServers = new IntVector();

/**
 * NOTE: activeThreads must only contain ActivityCore values which
 * are current active connection threads. This values should not be
 * accessible outside.
 */
 protected final ObjectStorage activeThreads = new ObjectStorage();

/**
 * NOTE: activeQdRecords must only contain DNSRecord values of being
 * processed queries at the locations consistent with activeThreads
 * locations.
 */
 protected final HashedStorage activeQdRecords =
  new HashedStorage();

/**
 * NOTE: activeAgents must only contain Notifiable values of being
 * processed queries at the locations consistent with activeThreads
 * locations (null means no query is being processed now). These
 * values should not be accessible outside.
 */
 protected final ObjectStorage activeAgents = new ObjectStorage();

/**
 * NOTE: activeServers contains pairs of the location in
 * uniqueServers of being processed queries (0 means no server
 * connected and no query is being processed now) and the status of
 * current query (0 means no connection, -1 means connection is
 * established, less than -1 means query is sent, >= 1 means answer
 * received (number of answer record for a single query)) at the
 * indices consistent with activeThreads locations minus 1.
 */
 protected final IntVector activeServers = new IntVector();

/**
 * NOTE: resRecords must only contain DNSRecord answer values of
 * being processed queries at the locations consistent with
 * activeThreads locations.
 */
 protected final ObjectStorage resRecords = new ObjectStorage();

/**
 * NOTE: Statistics. All the values are unsigned (and wrapped to
 * zero).
 */
 protected int totalConnections;

 protected int totalConnectFails;

 protected int totalBytesSent;

 protected int totalBytesReceived;

 protected int totalQueries;

 protected int totalAnswers;

 protected int totalRecordsReceived;

 protected int totalZoneRequests;

 protected int totalZoneTransfers;

 protected int totalDeniedTransfers;

 public DNSClientRobot() {}

 public final void setLock(Object newLock)
  throws NullPointerException
 {
  newLock.equals(newLock);
  Object lock;
  while (true)
   synchronized (lock = this.lock)
   {
    if (lock == this.lock)
    {
     this.lock = newLock;
     break;
    }
   }
 }

/**
 * NOTE: All synchronization must be done on getLock().
 */
 public final Object getLock()
 {
  return lock;
 }

/**
 * NOTE: Result is the current count of connection threads.
 */
 public final int getThreadsCount()
 {
  return activeThreadsSize;
 }

 public final boolean isSuspended()
 {
  return suspended;
 }

/**
 * NOTE: If there is at least one alive connection thread then
 * result is true else false.
 */
 public boolean isAlive()
 {
  Object lock;
  ObjectStorage activeThreads = this.activeThreads;
  while (true)
   synchronized (lock = this.lock)
   {
    if (lock == this.lock)
    {
     for (int location = activeThreads.childLocation(0, true);
          location > 0;
          location = activeThreads.siblingLocation(location, true))
      if (((ActivityCore)activeThreads.getAt(location)).isAlive())
       return true;
     break;
    }
   }
  return false;
 }

/**
 * NOTE: Result is the count of current died connection threads (due
 * to exception). Normally 0 is returned.
 */
 public int getDiedCount()
 {
  int count = 0;
  Object lock;
  ObjectStorage activeThreads = this.activeThreads;
  while (true)
   synchronized (lock = this.lock)
   {
    if (lock == this.lock)
    {
     for (int location = activeThreads.childLocation(0, true);
          location > 0;
          location = activeThreads.siblingLocation(location, true))
      if (!((ActivityCore)activeThreads.getAt(location)).isAlive())
       count++;
     break;
    }
   }
  return count;
 }

/**
 * NOTE: Result is the count of current connections.
 */
 public int getConnectionsCount()
 {
  int count = 0;
  Object lock;
  IntVector activeServers = this.activeServers;
  while (true)
   synchronized (lock = this.lock)
   {
    if (lock == this.lock)
    {
     int activeLocation = activeServers.length() >> 1;
     while (activeLocation > 0)
      if (activeServers.getIntAt((activeLocation--) * 2 - 1) != 0)
       count++;
     break;
    }
   }
  return count;
 }

/**
 * NOTE: maxConnections must be > 0. Observers notification is
 * performed.
 */
 public void setMaxConnections(int maxConnections)
 {
  if (maxConnections <= 0)
   maxConnections = 1;
  this.maxConnections = maxConnections;
  notifyObservers(this, null);
 }

 public final int getMaxConnections()
 {
  return maxConnections;
 }

/**
 * NOTE: maxServerConnections must be > 0. Affects only newly added
 * servers. Observers notification is performed.
 */
 public void setMaxServerConnections(int maxServerConnections)
 {
  if (maxServerConnections <= 0)
   maxServerConnections = 1;
  this.maxServerConnections = maxServerConnections;
  notifyObservers(this, null);
 }

 public final int getMaxServerConnections()
 {
  return maxServerConnections;
 }

/**
 * NOTE: switchDelay (in seconds) must be >= 0. Observers
 * notification is performed.
 */
 public void setSwitchDelay(int switchDelay)
 {
  if (switchDelay <= 0)
   switchDelay = 0;
  this.switchDelay = switchDelay;
  notifyObservers(this, null);
 }

/**
 * NOTE: Result is in seconds.
 */
 public final int getSwitchDelay()
 {
  return switchDelay;
 }

/**
 * NOTE: disconnectDelay (in seconds) must be >= 0. Observers
 * notification is performed.
 */
 public void setDisconnectDelay(int disconnectDelay)
 {
  if (disconnectDelay <= 0)
   disconnectDelay = 0;
  this.disconnectDelay = disconnectDelay;
  notifyObservers(this, null);
 }

/**
 * NOTE: Result is in seconds.
 */
 public final int getDisconnectDelay()
 {
  return disconnectDelay;
 }

/**
 * NOTE: For statistics. Result is unsigned (wrapped to zero).
 */
 public final int getTotalConnections()
 {
  return totalConnections;
 }

/**
 * NOTE: For statistics. Result is unsigned (wrapped to zero).
 */
 public final int getTotalConnectFails()
 {
  return totalConnectFails;
 }

/**
 * NOTE: For statistics. Result is unsigned (wrapped to zero).
 */
 public final int getTotalBytesSent()
 {
  return totalBytesSent;
 }

/**
 * NOTE: For statistics. Result is unsigned (wrapped to zero).
 */
 public final int getTotalBytesReceived()
 {
  return totalBytesReceived;
 }

/**
 * NOTE: For statistics. Result is unsigned (wrapped to zero).
 */
 public final int getTotalQueries()
 {
  return totalQueries;
 }

/**
 * NOTE: For statistics. Result is unsigned (wrapped to zero).
 */
 public final int getTotalAnswers()
 {
  return totalAnswers;
 }

/**
 * NOTE: For statistics. Result is unsigned (wrapped to zero).
 */
 public final int getTotalRecordsReceived()
 {
  return totalRecordsReceived;
 }

/**
 * NOTE: For statistics. Result is unsigned (wrapped to zero).
 */
 public final int getTotalZoneRequests()
 {
  return totalZoneRequests;
 }

/**
 * NOTE: For statistics. Result is unsigned (wrapped to zero).
 */
 public final int getTotalZoneTransfers()
 {
  return totalZoneTransfers;
 }

/**
 * NOTE: For statistics. Result is unsigned (wrapped to zero).
 */
 public final int getTotalDeniedTransfers()
 {
  return totalDeniedTransfers;
 }

 /*public final int queueCapacity()
 {
  return queueQdRecords.capacity();
 }*/

/**
 * NOTE: Result is the count of queries in queue (not processed
 * yet).
 */
 public final int getPassiveCount()
 {
  return queueAgentsSize;
 }

/**
 * NOTE: Result is the count of records in queue, which may be not
 * processed yet and which may be already processed (hidden,
 * temporarily put back to queue).
 */
 public final int getQueueSize()
 {
  return queueQdRecords.size();
 }

/**
 * NOTE: prevLocation may be == 0. Result > 0 unless no next
 * location found.
 */
 public final int nextQueueLocation(int prevLocation,
         boolean forward)
  throws IllegalArgumentException
 {
  Object lock;
  ListStorage queueAgents = this.queueAgents;
  while (true)
   synchronized (lock = this.lock)
   {
    if (lock == this.lock)
     return prevLocation == 0 ?
      queueAgents.childLocation(0, forward) :
      queueAgents.siblingLocation(prevLocation, forward);
   }
 }

 public final boolean hasQueuedAt(int location)
 {
  return queueAgents.getAt(location) != null;
 }

/**
 * NOTE: Result != null only if hasQueuedAt(location).
 */
 public final DNSRecord getQueueQdRecordAt(int location)
 {
  Object lock;
  while (true)
   synchronized (lock = this.lock)
   {
    if (lock == this.lock)
     return queueAgents.getAt(location) != null ?
      (DNSRecord)queueQdRecords.getAt(location) : null;
   }
 }

/**
 * NOTE: Result != null only if hasQueuedAt(location).
 */
 public final InetAddress getQueueServerAt(int location)
 {
  Object lock;
  while (true)
   synchronized (lock = this.lock)
   {
    if (lock == this.lock)
     return queueAgents.getAt(location) != null ?
      (InetAddress)uniqueServers.getAt(
      queueServers.getIntAt((location - 1) * 3)) : null;
   }
 }

/**
 * NOTE: server may be == null. Result >= 0.
 */
 public final int getServerConnectionsCount(InetAddress server)
 {
  Object lock;
  int location;
  while (true)
   synchronized (lock = this.lock)
   {
    if (lock == this.lock)
     return (location = uniqueServers.locationOf(server,
      0, true)) > 0 ? firstServers.getIntAt(location * 4 - 2) : 0;
   }
 }

/**
 * NOTE: server may be == null. Result >= 0 (0 means server
 * inaccessible).
 */
 public final int getMaxServerConnections(InetAddress server)
 {
  Object lock;
  int location;
  while (true)
   synchronized (lock = this.lock)
   {
    if (lock == this.lock)
     return (location = uniqueServers.locationOf(server,
      0, true)) > 0 ? firstServers.getIntAt(location * 4 - 1) :
      maxServerConnections;
   }
 }

 /*public final int activeCapacity()
 {
  return activeThreads.capacity();
 }*/

/**
 * NOTE: Result is the count of queries being processed.
 */
 public final int getActiveCount()
 {
  return activeAgentsSize;
 }

/**
 * NOTE: prevActiveLocation may be == 0. Result > 0 unless no next
 * activeLocation found.
 */
 public final int nextActiveLocation(int prevActiveLocation,
         boolean forward)
  throws IllegalArgumentException
 {
  Object lock;
  ObjectStorage activeThreads = this.activeThreads;
  while (true)
   synchronized (lock = this.lock)
   {
    if (lock == this.lock)
     return prevActiveLocation == 0 ?
      activeThreads.childLocation(0, forward) :
      activeThreads.siblingLocation(prevActiveLocation, forward);
   }
 }

 public final boolean isAliveAt(int activeLocation)
 {
  ActivityCore thread;
  return (thread =
   (ActivityCore)activeThreads.getAt(activeLocation)) != null &&
   thread.isAlive();
 }

 public final boolean hasActiveAt(int activeLocation)
 {
  return activeAgents.getAt(activeLocation) != null;
 }

/**
 * NOTE: Result != null unless no query at activeLocation or
 * activeLocation is invalid.
 */
 public final DNSRecord getActiveQdRecordAt(int activeLocation)
 {
  return (DNSRecord)activeQdRecords.getAt(activeLocation);
 }

/**
 * NOTE: Result != null unless no server connected at activeLocation
 * or activeLocation is invalid.
 */
 public final InetAddress getActiveServerAt(int activeLocation)
 {
  Object lock;
  IntVector activeServers = this.activeServers;
  while (true)
   synchronized (lock = this.lock)
   {
    if (lock == this.lock)
     return activeLocation > 0 && (activeServers.length() >> 1) >=
      activeLocation ? (InetAddress)uniqueServers.getAt(
      activeServers.getIntAt((activeLocation - 1) * 2)) : null;
   }
 }

/**
 * NOTE: If qdRecord == null or agent == null then result == 0
 * (unless prevActiveLocation is invalid).
 */
 public int activeLocationOf(DNSRecord qdRecord, Notifiable agent,
         int prevActiveLocation, boolean forward)
  throws IllegalArgumentException
 {
  HashedStorage activeQdRecords = this.activeQdRecords;
  ObjectStorage activeAgents = this.activeAgents;
  Object lock;
  while (true)
   synchronized (lock = this.lock)
   {
    if (lock == this.lock)
    {
     if (agent == null)
      return activeQdRecords.locationOf(null,
       prevActiveLocation, forward);
     while ((prevActiveLocation =
            activeQdRecords.locationOf(qdRecord,
            prevActiveLocation, forward)) > 0 &&
            !agent.equals(activeAgents.getAt(prevActiveLocation)));
     return prevActiveLocation;
    }
   }
 }

/**
 * NOTE: Result is the status of active connection thread/query
 * (0 means no connection or invalid activeLocation, -1 means
 * connection is established, less than -1 means query is sent,
 * >= 1 means answer received (number of answer record for a single
 * query)).
 */
 public final int getActiveStatusAt(int activeLocation)
 {
  Object lock;
  IntVector activeServers = this.activeServers;
  while (true)
   synchronized (lock = this.lock)
   {
    if (lock == this.lock)
     return activeLocation > 0 &&
      (activeServers.length() >> 1) >= activeLocation ?
      activeServers.getIntAt(activeLocation * 2 - 1) : 0;
   }
 }

/**
 * NOTE: Result != null unless no (more) answers at activeLocation
 * for being processed query or activeLocation is invalid.
 */
 public final DNSRecord getResRecordAt(int activeLocation)
 {
  return (DNSRecord)resRecords.getAt(activeLocation);
 }

/**
 * NOTE: If qdRecord == null or agent == null then result is false.
 */
 public boolean contains(DNSRecord qdRecord, Notifiable agent,
         boolean includingAccepted)
 {
  if (agent != null)
  {
   HashedStorage activeQdRecords = this.activeQdRecords;
   ObjectStorage activeAgents = this.activeAgents;
   IntVector activeServers = this.activeServers;
   Object lock;
   while (true)
    synchronized (lock = this.lock)
    {
     if (lock == this.lock)
     {
      int location = 0;
      while ((location = activeQdRecords.locationOf(qdRecord,
             location, true)) > 0)
       if (agent.equals(activeAgents.getAt(location)) &&
           (includingAccepted ||
           activeServers.getIntAt(location * 2 - 1) <= 0))
        return true;
      HashedStorage queueQdRecords = this.queueQdRecords;
      ListStorage queueAgents = this.queueAgents;
      while ((location = queueQdRecords.locationOf(qdRecord,
             location, true)) > 0)
       if (agent.equals(queueAgents.getAt(location)))
        return true;
      break;
     }
    }
  }
  return false;
 }

/**
 * NOTE: Observers notification for modification is done when query
 * is added (at location > 0, removedServer == null, removedQdRecord
 * == null, activeLocation == 0), discarded in queue (at location
 * > 0 after prevLoc >= 0, removedServer != null, removedQdRecord
 * != null, activeLocation == 0) or activated (at location > 0 after
 * prevLoc >= 0, removedServer == null, removedQdRecord == null,
 * activeLocation > 0), when thread status is changed (at
 * activeLocation > 0, location = 0, removedServer == null,
 * removedQdRecord == null) or activated query is removed (at
 * activeLocation > 0, location = 0, removedServer != null,
 * removedQdRecord != null). Notification argument in this case is
 * of ConstVector type with the following fields: Integer(prevLoc),
 * Integer(location), removedServer, removedQdRecord,
 * Integer(activeLocation). In addition, observers are notified with
 * the argument of null when any of robot parameters is changed.
 * Observers are also notified after any agent is notified with the
 * argument of Integer(activeLocation).
 */
 protected final void notifyObservers(int prevLoc, int location,
            InetAddress removedServer, DNSRecord removedQdRecord,
            int activeLocation)
 {
  if (hasObservers())
  {
   Object[] array = new Object[5];
   array[0] = new Integer(prevLoc);
   array[1] = new Integer(location);
   array[2] = removedServer;
   array[3] = removedQdRecord;
   array[4] = new Integer(activeLocation);
   notifyObservers(this, new ConstVector(array));
  }
 }

/**
 * NOTE: If agent == null then empty array returned. Result != null,
 * result[i] != null for any i.
 */
 public DNSRecord[] getAgentQueries(Notifiable agent)
 {
  int location = 0, count = 0;
  DNSRecord[] qdRecords = new DNSRecord[0], newRecords;
  ListStorage queueAgents = this.queueAgents;
  ObjectStorage activeAgents = this.activeAgents;
  HashedStorage queueQdRecords = this.queueQdRecords;
  HashedStorage activeQdRecords = this.activeQdRecords;
  Object lock;
  while (true)
   synchronized (lock = this.lock)
   {
    if (lock == this.lock)
    {
     while ((location = activeAgents.locationOf(agent,
            location, true)) > 0)
     {
      if (qdRecords.length <= count)
      {
       System.arraycopy(qdRecords, 0, newRecords =
        new DNSRecord[(count >> 1) + count + 1], 0, count);
       qdRecords = newRecords;
      }
      qdRecords[count++] =
       (DNSRecord)activeQdRecords.getAt(location);
     }
     while ((location = queueAgents.locationOf(agent,
            location, true)) > 0)
     {
      if (qdRecords.length <= count)
      {
       System.arraycopy(qdRecords, 0, newRecords =
        new DNSRecord[(count >> 1) + count + 1], 0, count);
       qdRecords = newRecords;
      }
      qdRecords[count++] =
       (DNSRecord)queueQdRecords.getAt(location);
     }
     break;
    }
   }
  if (qdRecords.length > count)
  {
   System.arraycopy(qdRecords, 0,
    newRecords = new DNSRecord[count], 0, count);
   qdRecords = newRecords;
  }
  return qdRecords;
 }

/**
 * NOTE: If agent == null then nothing is performed.
 */
 public void removeAgent(Notifiable agent)
 {
  Object lock;
  ListStorage queueAgents = this.queueAgents;
  ObjectStorage activeAgents = this.activeAgents;
  if (agent != null)
   while (true)
    synchronized (lock = this.lock)
    {
     if (lock == this.lock)
     {
      int location, nextLoc;
      for (location = queueAgents.childLocation(0, true);
           location > 0; location = nextLoc)
      {
       nextLoc = queueAgents.siblingLocation(location, true);
       if (queueAgents.getAt(location).equals(agent))
       {
        purgeInQueue(location);
        if (nextLoc > 0 && queueAgents.getAt(nextLoc) == null)
         nextLoc = queueAgents.childLocation(0, true);
       }
      }
      for (location = activeAgents.locationOf(agent, 0, true);
           location > 0; location = nextLoc)
      {
       nextLoc = activeAgents.locationOf(agent, location, true);
       activeAgents.setAt(location, null);
       activeAgentsSize--;
      }
      nextLoc = 0;
      break;
     }
    }
 }

/**
 * NOTE: If agent == null then nothing is performed. agent is used
 * for verification of remove operation correctness.
 */
 public boolean removeQueuedAt(int location, Notifiable agent)
 {
  Object lock;
  while (true)
   synchronized (lock = this.lock)
   {
    if (lock == this.lock)
    {
     Object curAgent;
     if (agent == null ||
         (curAgent = queueAgents.getAt(location)) == null ||
         !curAgent.equals(agent))
      return false;
     purgeInQueue(location);
     return true;
    }
   }
 }

/**
 * NOTE: If agent == null then nothing is performed. agent is used
 * for validation of remove operation correctness.
 */
 public boolean removeActiveAt(int activeLocation, Notifiable agent)
 {
  ObjectStorage activeAgents = this.activeAgents;
  Object lock;
  while (true)
   synchronized (lock = this.lock)
   {
    if (lock == this.lock)
    {
     Object curAgent;
     if (agent == null ||
         (curAgent = activeAgents.getAt(activeLocation)) == null ||
         !curAgent.equals(agent))
      return false;
     activeAgents.setAt(activeLocation, null);
     activeAgentsSize--;
     return true;
    }
   }
 }

/**
 * NOTE: servers must be != null, qdRecord != null, agent != null.
 * servers[i] may be == null for any i. Result is the count of
 * non-null values of servers.
 */
 public final int queryAll(InetAddress[] servers,
         DNSRecord qdRecord, boolean highPriority, Notifiable agent)
  throws NullPointerException
 {
  int count = 0, index = 0, len = servers.length;
  Object lock;
  while (true)
   synchronized (lock = this.lock)
   {
    if (lock == this.lock)
    {
     InetAddress server;
     while (len-- > 0)
      if ((server = servers[highPriority ? len : index++]) != null)
      {
       query(server, qdRecord, highPriority, agent);
       count++;
      }
     return count;
    }
   }
 }

/**
 * NOTE: server, qdRecord and agent must be != null. agent should
 * not be accessible outside. Duplicate queries are removed. agent
 * update() method is called when answer successfully received or
 * when all queries for qdRecord (may be for multiple servers) have
 * failed. The argument parameter of update() method is
 * Integer(activeLocation), the result resRecord may be obtained (by
 * the agent) through getResRecordAt(activeLocation), resRecord of
 * null means end-of-answer. Observers are notified with the same
 * argument too (after agent). Query recursion may be performed for
 * other than SOA, IXFR, AXFR, MAILB, MAILA and ANY qTypes. On
 * success other queries with the same qdRecord and agent are
 * discarded.
 */
 public void query(InetAddress server, DNSRecord qdRecord,
         boolean highPriority, Notifiable agent)
  throws NullPointerException
 {
  server.equals(server);
  qdRecord.equals(qdRecord);
  agent.equals(agent);
  ListStorage queueAgents = this.queueAgents;
  HashedStorage queueQdRecords = this.queueQdRecords;
  IntVector queueServers = this.queueServers;
  IntVector firstServers = this.firstServers;
  HashedStorage uniqueServers = this.uniqueServers;
  ObjectStorage activeAgents = this.activeAgents;
  HashedStorage activeQdRecords = this.activeQdRecords;
  IntVector activeServers = this.activeServers;
  Object lock;
  while (true)
   synchronized (lock = this.lock)
   {
    if (lock == this.lock)
    {
     int uniqLoc, location = 0;
     if ((uniqLoc = uniqueServers.locationOf(server, 0, true)) > 0)
     {
      Object queuedAgent = null;
      if (firstServers.getIntAt((uniqLoc - 1) * 4) > 0)
       while ((location = queueQdRecords.locationOf(qdRecord,
              location, true)) > 0 &&
              (queueServers.getIntAt((location - 1) * 3) !=
              uniqLoc ||
              (queuedAgent = queueAgents.getAt(location)) != null &&
              !queuedAgent.equals(agent)));
      if (location > 0)
      {
       if (queuedAgent != null && (!highPriority ||
           queueAgents.siblingLocation(location, false) <= 0))
        return;
      }
       else if (firstServers.getIntAt(uniqLoc * 4 - 2) > 0)
        while ((location = activeQdRecords.locationOf(qdRecord,
               location, true)) > 0)
         if (activeServers.getIntAt((location - 1) * 2) ==
             uniqLoc &&
             activeServers.getIntAt(location * 2 - 1) <= 0 &&
             agent.equals(activeAgents.getAt(location)))
          return;
     }
      else
      {
       uniqueServers.setAt(uniqLoc =
        uniqueServers.emptyLocation(), null);
       firstServers.ensureSize(uniqLoc * 4);
      }
     queryHelper(uniqLoc, server, qdRecord, highPriority, agent);
     ObjectStorage activeThreads = this.activeThreads;
     if (location > 0)
      purgeInQueue(location);
      else if ((location = activeThreadsSize) < maxConnections &&
               location - activeAgentsSize < queueAgentsSize)
      {
       if ((location =
           firstServers.getIntAt(uniqLoc * 4 - 2)) <= 0 ||
           firstServers.getIntAt(uniqLoc * 4 - 3) == location &&
           firstServers.getIntAt(uniqLoc * 4 - 1) > location)
       {
        activeAgents.setAt(location =
         activeThreads.emptyLocation(), null);
        activeQdRecords.setAt(location, null);
        resRecords.setAt(location, null);
        activeThreads.setAt(location, null);
        activeServers.ensureSize(location * 2);
        activeServers.setAt((location - 1) * 2, 0);
        activeServers.setAt(location * 2 - 1, 0);
        ActivityCore thread =
         new DNSClientRobotThread(this, location);
        if (suspended)
         thread.suspend();
        activeThreads.setAt(location, thread);
        activeThreadsSize++;
        notifyObservers(0, 0, null, null, location);
       }
      }
       else if (!suspended && activeAgentsSize < location)
       {
        int servLoc;
        boolean forward = (queueQdRecords.size() ^
         totalBytesReceived) * JavaConsts.GOLD_MEDIAN < 0;
        for (location = activeThreads.childLocation(0, forward);
             location > 0; location =
             activeThreads.siblingLocation(location, forward))
         if ((servLoc =
             activeServers.getIntAt((location - 1) * 2)) <= 0 ||
             servLoc == uniqLoc &&
             activeAgents.getAt(location) == null)
         {
          ((ActivityCore)activeThreads.getAt(location)).interrupt();
          break;
         }
       }
     break;
    }
   }
 }

/**
 * NOTE: uniqLoc must be > 0, server must be != null (unless
 * uniqueServers getAt(uniqLoc) != null), qdRecord must be != null,
 * agent may be == null. Specified query is added to queue.
 * Observers notification is performed (only if agent != null). Must
 * be synchronized outside.
 */
 protected void queryHelper(int uniqLoc, InetAddress server,
            DNSRecord qdRecord, boolean highPriority,
            Notifiable agent)
 {
  ListStorage queueAgents = this.queueAgents;
  HashedStorage queueQdRecords = this.queueQdRecords;
  IntVector queueServers = this.queueServers;
  IntVector firstServers = this.firstServers;
  int location = queueQdRecords.emptyLocation();
  queueServers.ensureSize(location * 3);
  if (agent != null)
  {
   queueQdRecords.setAt(location, null);
   if (highPriority)
    queueAgents.insertAt(0, location, agent);
    else queueAgents.setAt(location, agent);
   queueAgentsSize++;
  }
  queueQdRecords.setAt(location, qdRecord);
  int firstLoc = 0;
  HashedStorage uniqueServers = this.uniqueServers;
  if (uniqueServers.getAt(uniqLoc) == null)
  {
   uniqueServers.setAt(uniqLoc, server);
   firstServers.setAt(uniqLoc * 4 - 1, maxServerConnections);
  }
   else firstLoc = firstServers.getIntAt((uniqLoc - 1) * 4);
  queueServers.setAt((location - 1) * 3, uniqLoc);
  if (firstLoc > 0)
  {
   int nextLoc = queueServers.getIntAt(firstLoc * 3 - 1);
   queueServers.setAt(location * 3 - 1, nextLoc);
   queueServers.setAt(nextLoc * 3 - 2, location);
  }
   else firstLoc = location;
  queueServers.setAt(location * 3 - 2, firstLoc);
  queueServers.setAt(firstLoc * 3 - 1, location);
  if (firstLoc == location || highPriority)
   firstServers.setAt((uniqLoc - 1) * 4, location);
  if (agent != null)
   notifyObservers(0, location, null, null, 0);
 }

/**
 * NOTE: Must be called only from active thread when no query is
 * being processed and status of current connection is 0 or -1.
 * Appropriate query from queue is moved to the active thread. If
 * fellAsleepTime == 0 then time is current. Result is true only if
 * disconnection must be performed.
 */
 protected boolean activate(int activeLocation, int fellAsleepTime)
 {
  ListStorage queueAgents = this.queueAgents;
  HashedStorage queueQdRecords = this.queueQdRecords;
  IntVector queueServers = this.queueServers;
  IntVector firstServers = this.firstServers;
  ObjectStorage activeAgents = this.activeAgents;
  HashedStorage activeQdRecords = this.activeQdRecords;
  IntVector activeServers = this.activeServers;
  Object lock;
  while (true)
   synchronized (lock = this.lock)
   {
    if (lock == this.lock)
    {
     DNSRecord qdRecord;
     int status, location = 0, uniqLoc = 0, firstLoc, prevLoc;
     if ((status =
         activeServers.getIntAt(activeLocation * 2 - 1)) != 0 &&
         (firstLoc = firstServers.getIntAt((uniqLoc =
         activeServers.getIntAt((activeLocation -
         1) * 2)) * 4 - 1)) > 0)
     {
      boolean limit =
       (location = firstServers.getIntAt(uniqLoc * 4 - 3)) > 0 &&
       firstLoc - 1 <= location;
      location = firstLoc =
       firstServers.getIntAt((uniqLoc - 1) * 4);
      while (location > 0)
      {
       Object agent;
       if ((agent = queueAgents.getAt(location)) != null)
       {
        qdRecord = (DNSRecord)queueQdRecords.getAt(location);
        int qType;
        if ((qType = qdRecord.getRType()) != DNSRecord.AXFR &&
            qType != DNSRecord.IXFR)
        {
         qType = 0;
         while ((qType = activeQdRecords.locationOf(qdRecord,
                qType, true)) > 0 &&
                (activeServers.getIntAt(qType * 2 - 1) == 0 ||
                !agent.equals(activeAgents.getAt(qType))));
         if (qType <= 0)
          break;
        }
         else if (!limit)
         {
          firstLoc = 0;
          break;
         }
       }
       if ((location =
           queueServers.getIntAt(location * 3 - 2)) == firstLoc)
        location = 0;
      }
      if (location > 0)
      {
       prevLoc = -1;
       firstLoc = firstLoc > 0 ?
        maxConnections * 10 : activeThreadsSize + 1;
       do
       {
        prevLoc++;
       } while ((firstLoc >>>= 1) != 0);
       if (((((fellAsleepTime - queueQdRecords.size()) ^
           (totalBytesReceived + location))) *
           JavaConsts.GOLD_MEDIAN) >>>
           ((JavaConsts.INT_SIZE - 1) - prevLoc) == 0)
        location = 0;
      }
       else if (fellAsleepTime == 0 || switchDelay >
                (int)(System.currentTimeMillis() / 1000L) -
                fellAsleepTime)
        return false;
     }
     if (location <= 0)
     {
      for (location = queueAgents.childLocation(0, true);
           location > 0;
           location = queueAgents.siblingLocation(location, true))
       if ((firstLoc = firstServers.getIntAt((uniqLoc =
           queueServers.getIntAt((location - 1) * 3)) * 4 - 2)) <
           (prevLoc = firstServers.getIntAt(uniqLoc * 4 - 1)))
       {
        if (firstLoc <= 0 ||
            firstServers.getIntAt(uniqLoc * 4 - 3) == firstLoc)
        {
         qdRecord = (DNSRecord)queueQdRecords.getAt(location);
         int qType;
         if ((qType = qdRecord.getRType()) != DNSRecord.AXFR &&
             qType != DNSRecord.IXFR)
         {
          qType = 0;
          Object agent = queueAgents.getAt(location);
          while ((qType = activeQdRecords.locationOf(qdRecord,
                 qType, true)) > 0 &&
                 (activeServers.getIntAt(qType * 2 - 1) == 0 ||
                 !agent.equals(activeAgents.getAt(qType))));
          if (qType <= 0)
           break;
         }
          else if (firstLoc <= 0 || prevLoc - 1 > firstLoc)
           break;
        }
       }
        else if (firstLoc == 0)
        {
         Object agent = queueAgents.getAt(location);
         qdRecord = (DNSRecord)queueQdRecords.getAt(location);
         while ((firstLoc = activeQdRecords.locationOf(qdRecord,
                firstLoc, true)) > 0 && (firstServers.getIntAt(
                activeServers.getIntAt((firstLoc -
                1) * 2) * 4 - 1) <= 0 ||
                !agent.equals(activeAgents.getAt(firstLoc))));
         if (firstLoc == 0)
         {
          int servLoc;
          while ((firstLoc = queueQdRecords.locationOf(qdRecord,
                 firstLoc, true)) > 0)
           if (agent.equals(queueAgents.getAt(firstLoc)) &&
               firstServers.getIntAt((servLoc =
               queueServers.getIntAt((firstLoc - 1) * 3)) * 4 - 2) <
               firstServers.getIntAt(servLoc * 4 - 1))
           {
            location = firstLoc;
            uniqLoc = servLoc;
            break;
           }
          break;
         }
        }
      if (status != 0)
       if (location <= 0 && (fellAsleepTime == 0 ||
           disconnectDelay > (int)(System.currentTimeMillis() /
           1000L) - fellAsleepTime))
        return false;
        else activeServers.setAt(activeLocation * 2 - 1, 0);
      purgeConnection(activeLocation);
      if (location <= 0)
       return status != 0;
      activeServers.setAt((activeLocation - 1) * 2, uniqLoc);
      firstServers.setAt(uniqLoc * 4 - 2,
       firstServers.getIntAt(uniqLoc * 4 - 2) + 1);
     }
      else status = 0;
     prevLoc = queueAgents.siblingLocation(location, false);
     queueAgentsSize--;
     activeAgents.setAt(activeLocation,
      queueAgents.setAt(location, null));
     activeAgentsSize++;
     qdRecord = (DNSRecord)queueQdRecords.setAt(location, null);
     activeQdRecords.setAt(activeLocation, qdRecord);
     firstLoc = queueServers.getIntAt(location * 3 - 2);
     int lastLoc = queueServers.getIntAt(location * 3 - 1);
     queueServers.setAt(firstLoc * 3 - 1, lastLoc);
     queueServers.setAt(lastLoc * 3 - 2, firstLoc);
     if (firstServers.getIntAt((uniqLoc - 1) * 4) == location)
      firstServers.setAt((uniqLoc - 1) * 4,
       location != firstLoc ? firstLoc : 0);
     queueServers.setAt((location - 1) * 3, 0);
     if ((firstLoc = qdRecord.getRType()) == DNSRecord.AXFR ||
         firstLoc == DNSRecord.IXFR)
      firstServers.setAt(uniqLoc * 4 - 3,
       firstServers.getIntAt(uniqLoc * 4 - 3) + 1);
     notifyObservers(prevLoc, location, null, null, activeLocation);
     return status != 0;
    }
   }
 }

/**
 * NOTE: Must be called only inside connection thread and query must
 * exist at activeLocation (agent may be == null). If resRecord
 * == null then no more records in the answer. If current status at
 * activeLocation is 0 then information about previous connection is
 * removed (if existed). Otherwise if resRecord == null then stored
 * information about query agent is erased. If current status at
 * activeLocation is (0 or -1) and there is no more same queries
 * then agent notification is done (notify on fail). Otherwise if
 * current status at activeLocation is less than -1 or greater than
 * 0 then not processed (no answer received yet) same queries are
 * removed. Answer counter for activeLocation is increased and agent
 * notification is performed (notify on success). After all
 * information about failed/processed query is erased. Note that
 * answer() should not be synchronized outside.
 */
 protected void answer(int activeLocation, DNSRecord resRecord)
 {
  Object agent;
  HashedStorage activeQdRecords = this.activeQdRecords;
  IntVector activeServers = this.activeServers;
  HashedStorage queueQdRecords = this.queueQdRecords;
  HashedStorage uniqueServers = this.uniqueServers;
  ListStorage queueAgents = this.queueAgents;
  ObjectStorage activeAgents = this.activeAgents;
  Object lock;
  while (true)
   synchronized (lock = this.lock)
   {
    if (lock == this.lock)
    {
     if ((agent = activeAgents.getAt(activeLocation)) != null)
     {
      DNSRecord qdRecord =
       (DNSRecord)activeQdRecords.getAt(activeLocation);
      int location;
      if ((location =
          activeServers.getIntAt(activeLocation * 2 - 1)) < -1)
      {
       activeServers.setAt(activeLocation * 2 - 1, 1);
       IntVector queueServers = this.queueServers;
       location = queueQdRecords.locationOf(qdRecord, 0, true);
       while (location > 0)
       {
        int nextLoc =
         queueQdRecords.locationOf(qdRecord, location, true);
        if (agent.equals(queueAgents.getAt(location)))
        {
         int prevLoc = queueAgents.siblingLocation(location, false);
         queueAgentsSize--;
         queueAgents.setAt(location, null);
         notifyObservers(prevLoc, location,
          (InetAddress)uniqueServers.getAt(
          queueServers.getIntAt((location - 1) * 3)),
          (DNSRecord)queueQdRecords.getAt(location), 0);
         if (nextLoc > 0 &&
             !qdRecord.equals(queueQdRecords.getAt(nextLoc)))
          nextLoc = queueQdRecords.locationOf(qdRecord, 0, true);
        }
        location = nextLoc;
       }
       while ((location = activeQdRecords.locationOf(qdRecord,
              location, true)) > 0)
        if (location != activeLocation &&
            activeServers.getIntAt(location * 2 - 1) <= 0 &&
            agent.equals(activeAgents.getAt(location)))
        {
         activeAgentsSize--;
         activeAgents.setAt(location, null);
         queryHelper(activeServers.getIntAt((location - 1) * 2),
          null, qdRecord, false, null);
        }
      }
       else if (location <= 0)
       {
        location = 0;
        while ((location = activeQdRecords.locationOf(qdRecord,
               location, true)) > 0 &&
               (location == activeLocation ||
               activeServers.getIntAt(location * 2 - 1) > 0 ||
               !agent.equals(activeAgents.getAt(location))));
        if (location == 0)
         while ((location = queueQdRecords.locationOf(qdRecord,
                location, true)) > 0 &&
                !agent.equals(queueAgents.getAt(location)));
        if (location > 0)
        {
         agent = null;
         queryHelper(activeServers.getIntAt(
          (activeLocation - 1) * 2), null, qdRecord, false, null);
        }
       }
        else activeServers.setAt(activeLocation * 2 - 1,
              location + 1);
     }
     break;
    }
   }
  resRecords.setAt(activeLocation, resRecord);
  Object obsArgument = new Integer(activeLocation);
  if (agent != null)
   ((Notifiable)agent).update(this, obsArgument);
  notifyObservers(this, obsArgument);
  if (resRecord == null)
   while (true)
    synchronized (lock = this.lock)
    {
     if (lock == this.lock)
     {
      if (activeAgents.setAt(activeLocation, null) != null)
       activeAgentsSize--;
      DNSRecord qdRecord =
       (DNSRecord)activeQdRecords.setAt(activeLocation, null);
      int qType, uniqLoc =
       activeServers.getIntAt((activeLocation - 1) * 2);
      if ((qType = qdRecord.getRType()) == DNSRecord.AXFR ||
          qType == DNSRecord.IXFR)
      {
       IntVector firstServers = this.firstServers;
       firstServers.setAt(uniqLoc * 4 - 3,
        firstServers.getIntAt(uniqLoc * 4 - 3) - 1);
      }
      InetAddress server =
       (InetAddress)uniqueServers.getAt(uniqLoc);
      if ((qType =
          activeServers.getIntAt(activeLocation * 2 - 1)) == 0)
       purgeConnection(activeLocation);
       else activeServers.setAt(activeLocation * 2 - 1, -1);
      notifyObservers(0, 0, server, qdRecord, activeLocation);
      if (activeQdRecords.locationOf(qdRecord, 0, true) <= 0 &&
          (qType = queueQdRecords.locationOf(qdRecord,
          0, true)) > 0)
      {
       int location = qType;
       while (queueAgents.getAt(qType) == null &&
              (qType = queueQdRecords.locationOf(qdRecord,
              qType, true)) > 0);
       if (qType <= 0)
        do
        {
         qType =
          queueQdRecords.locationOf(qdRecord, location, true);
         purgeInQueue(location);
        } while ((location = qType) > 0);
      }
      break;
     }
    }
 }

/**
 * NOTE: Must be called only if query in queue at location exists.
 * Information about the specified query is erased. Observers
 * notification is performed (only if agent != null). Must be
 * synchronized outside.
 */
 protected void purgeInQueue(int location)
 {
  ListStorage queueAgents = this.queueAgents;
  HashedStorage uniqueServers = this.uniqueServers;
  IntVector queueServers = this.queueServers;
  IntVector firstServers = this.firstServers;
  int prevLoc = -1;
  if (queueAgents.getAt(location) != null)
  {
   prevLoc = queueAgents.siblingLocation(location, false);
   queueAgentsSize--;
   queueAgents.setAt(location, null);
  }
  Object qdRecord = queueQdRecords.setAt(location, null);
  int nextLoc = queueServers.getIntAt(location * 3 - 2);
  int uniqLoc = queueServers.getIntAt(location * 3 - 1);
  queueServers.setAt(uniqLoc * 3 - 2, nextLoc);
  queueServers.setAt(nextLoc * 3 - 1, uniqLoc);
  InetAddress server = (InetAddress)uniqueServers.getAt(uniqLoc =
   queueServers.getIntAt((location - 1) * 3));
  if (firstServers.getIntAt((uniqLoc - 1) * 4) == location)
  {
   if (location == nextLoc &&
       (nextLoc = 0) >= firstServers.getIntAt(uniqLoc * 4 - 2))
   {
    firstServers.setAt(uniqLoc * 4 - 1, 0);
    uniqueServers.setAt(uniqLoc, null);
   }
   firstServers.setAt((uniqLoc - 1) * 4, nextLoc);
  }
  queueServers.setAt((location - 1) * 3, 0);
  if (prevLoc >= 0)
   notifyObservers(prevLoc, location,
    server, (DNSRecord)qdRecord, 0);
 }

/**
 * NOTE: Must be called only if current status at activeLocation is
 * 0. Must be synchronized outside. Information about previous
 * connection is erased (if existed).
 */
 protected void purgeConnection(int activeLocation)
 {
  IntVector activeServers = this.activeServers;
  IntVector firstServers = this.firstServers;
  int uniqLoc, count;
  if ((uniqLoc =
      activeServers.getIntAt((activeLocation - 1) * 2)) > 0)
  {
   activeServers.setAt((activeLocation - 1) * 2, 0);
   firstServers.setAt(uniqLoc * 4 - 2,
    count = firstServers.getIntAt(uniqLoc * 4 - 2) - 1);
   if (count <= 0 && firstServers.getIntAt((uniqLoc - 1) * 4) <= 0)
   {
    firstServers.setAt(uniqLoc * 4 - 1, 0);
    uniqueServers.setAt(uniqLoc, null);
   }
  }
 }

/**
 * NOTE: Perform interruption of all sleeping threads.
 */
 public void interrupt()
 {
  ObjectStorage activeThreads = this.activeThreads;
  HashedStorage activeQdRecords = this.activeQdRecords;
  Object lock;
  while (true)
   synchronized (lock = this.lock)
   {
    if (lock == this.lock)
    {
     for (int location = activeThreads.childLocation(0, true);
          location > 0;
          location = activeThreads.siblingLocation(location, true))
      if (activeQdRecords.getAt(location) == null)
       ((ActivityCore)activeThreads.getAt(location)).interrupt();
     break;
    }
   }
 }

/**
 * NOTE: Observers notification is performed.
 */
 public void suspend()
 {
  ObjectStorage activeThreads = this.activeThreads;
  Object lock;
  while (true)
   synchronized (lock = this.lock)
   {
    if (lock == this.lock)
    {
     if (suspended)
      return;
     for (int location = activeThreads.childLocation(0, true);
          location > 0;
          location = activeThreads.siblingLocation(location, true))
      ((ActivityCore)activeThreads.getAt(location)).suspend();
     suspended = true;
     break;
    }
   }
  notifyObservers(this, null);
 }

/**
 * NOTE: Perform suspending and wait for the completion of
 * operation.
 */
 public void waitSuspend()
 {
 }

/**
 * NOTE: Aborted threads are restarted, others are resumed.
 * Observers notification is performed.
 */
 public void resume()
 {
  ObjectStorage activeThreads = this.activeThreads;
  IntVector activeServers = this.activeServers;
  Object lock;
  while (true)
   synchronized (lock = this.lock)
   {
    if (lock == this.lock)
    {
     ActivityCore thread;
     for (int location = activeThreads.childLocation(0, true);
          location > 0;
          location = activeThreads.siblingLocation(location, true))
      if (!(thread =
          (ActivityCore)activeThreads.getAt(location)).isAlive())
      {
       activeServers.setAt(location * 2 - 1, 0);
       activeThreads.setAt(location,
        new DNSClientRobotThread(this, location));
       notifyObservers(0, 0, null, null, location);
      }
       else if (thread.isSuspended())
       {
        if (activeServers.getIntAt(location * 2 - 1) < 0)
         activeServers.setAt(location * 2 - 1, 0);
        thread.resume();
       }
     suspended = false;
     break;
    }
   }
  notifyObservers(this, null);
 }

/**
 * NOTE: Terminate sleeping threads.
 */
 public void stop()
 {
  ObjectStorage activeThreads = this.activeThreads;
  ObjectStorage activeAgents = this.activeAgents;
  Object lock;
  while (true)
   synchronized (lock = this.lock)
   {
    if (lock == this.lock)
    {
     int location, count;
     if ((count = activeThreadsSize - queueAgentsSize) > 0)
      for (location = activeThreads.childLocation(0, true);
           location > 0;
           location = activeThreads.siblingLocation(location, true))
       if (activeAgents.getAt(location) == null)
       {
        ActivityCore thread;
        (thread =
         (ActivityCore)activeThreads.getAt(location)).stop();
        thread.interrupt();
        if (--count <= 0)
         break;
       }
     break;
    }
   }
 }

/**
 * NOTE: Waiting while alive.
 */
 public void join()
 {
 }

/**
 * NOTE: In addition all sleeping connection threads are terminated.
 */
 public void trimToSize()
 {
  Object lock;
  while (true)
   synchronized (lock = this.lock)
   {
    if (lock == this.lock)
    {
     super.trimToSize();
     stop();
     queueAgents.trimToSize();
     HashedStorage queueQdRecords = this.queueQdRecords;
     queueQdRecords.trimToSize();
     HashedStorage uniqueServers = this.uniqueServers;
     uniqueServers.trimToSize();
     activeQdRecords.trimToSize();
     activeAgents.trimToSize();
     resRecords.trimToSize();
     ObjectStorage activeThreads = this.activeThreads;
     activeThreads.trimToSize();
     break;
    }
   }
 }

/**
 * NOTE: This is not a real clone. New instance is created with the
 * same parameters.
 */
 public Object clone()
 {
  DNSClientRobot robot = new DNSClientRobot();
  Object lock;
  while (true)
   synchronized (lock = this.lock)
   {
    if (lock == this.lock)
    {
     robot.suspended = suspended;
     robot.maxConnections = maxConnections;
     robot.maxServerConnections = maxServerConnections;
     robot.switchDelay = switchDelay;
     robot.disconnectDelay = disconnectDelay;
     return robot;
    }
   }
 }

/**
 * NOTE: Result != null, result is 'in-line' only if (!params ||
 * !status) and !statistics.
 */
 public String toString(boolean params, boolean status,
         boolean statistics)
 {
  StringBuffer sBuf = new StringBuffer(506);
  Object lock;
  while (true)
   synchronized (lock = this.lock)
   {
    if (lock == this.lock)
    {
     if (params)
      sBuf.append(
       "Connections(server)/switch/disconnect parameters: ").
       append(UnsignedInt.toString(maxConnections, false)).
       append('(').
       append(UnsignedInt.toString(maxServerConnections, false)).
       append(')').append('/').
       append(UnsignedInt.toString(switchDelay, false)).append('/').
       append(UnsignedInt.toString(disconnectDelay, false));
     if (status)
     {
      if (params)
       sBuf.append('\n');
      int count = queueAgentsSize;
      sBuf.append("Queries(hidden)/threads(active)/connections: ").
       append(UnsignedInt.toString(count, false)).append('(').
       append(UnsignedInt.toString(queueQdRecords.size() -
       count, false)).append(')').append('/').
       append(UnsignedInt.toString(activeThreadsSize, false)).
       append('(').
       append(UnsignedInt.toString(activeAgentsSize, false)).
       append(')').append('/').
       append(UnsignedInt.toString(getConnectionsCount(), false));
      if ((count = getDiedCount()) > 0)
       sBuf.append(' ').append('(').
        append(UnsignedInt.toString(count, false)).append(" died)");
      if (suspended)
       sBuf.append(" (suspended)");
     }
     if (statistics)
     {
      if (params || status)
       sBuf.append('\n');
      sBuf.append("Total connections/failures: ").
       append(UnsignedInt.toString(totalConnections, true)).
       append('/').append(UnsignedInt.toString(totalConnectFails,
       true)).append('\n').append("Total bytes sent/received: ").
       append(UnsignedInt.toString(totalBytesSent, true)).
       append('/').
       append(UnsignedInt.toString(totalBytesReceived, true)).
       append('\n').append("Total queries/answers/records: ").
       append(UnsignedInt.toString(totalQueries, true)).append('/').
       append(UnsignedInt.toString(totalAnswers, true)).append('/').
       append(UnsignedInt.toString(totalRecordsReceived, true)).
       append('\n').append("Zone requests/transfers/denials: ").
       append(UnsignedInt.toString(totalZoneRequests, true)).
       append('/').
       append(UnsignedInt.toString(totalZoneTransfers, true)).
       append('/').
       append(UnsignedInt.toString(totalDeniedTransfers, true)).
       append('\n');
     }
     break;
    }
   }
  return new String(sBuf);
 }

/**
 * NOTE: Result != null, result is 'out-line'.
 */
 public String toString()
 {
  return toString(true, true, true);
 }

/**
 * NOTE: Deep check for integrity of this object. Must be
 * synchronized outside. For debug purpose only.
 */
 public void integrityCheck()
 {
  super.integrityCheck();
 }
}

/**
 * NOTE: This helper class is used only inside DNSClientRobot.
 */
final class DNSClientRobotThread extends ActivityCore
{

/**
 * NOTE: robot must be != null (when initialized).
 */
 protected final DNSClientRobot robot;

/**
 * NOTE: activeLocation must be > 0 (when initialized).
 */
 protected final int activeLocation;

 protected final DNSConnection connection = new DNSConnection();

 protected boolean answered;

 protected int fellAsleepTime;

/**
 * NOTE: robot must be != null, activeLocation must be valid.
 */
 protected DNSClientRobotThread(DNSClientRobot robot,
            int activeLocation)
  throws NullPointerException
 {
  super("DNSClientRobot thread-" +
   UnsignedInt.toString(activeLocation, !robot.equals(robot)));
  this.robot = robot;
  this.activeLocation = activeLocation;
 }

 protected boolean loop()
 {
  DNSClientRobot robot;
  int activeLocation;
  Object lock;
  DNSConnection connection;
  if ((robot = this.robot) != null &&
      (activeLocation = this.activeLocation) > 0 &&
      (connection = this.connection) != null &&
      robot.activeThreads.getAt(activeLocation) != null)
  {
   IntVector activeServers = robot.activeServers;
   ObjectStorage activeAgents = robot.activeAgents;
   ObjectStorage activeQdRecords = robot.activeQdRecords;
   if (((activeServers.getIntAt(activeLocation * 2 -
       1) + 1) >> 1) == 0 &&
       activeAgents.getAt(activeLocation) == null)
   {
    if (robot.activeThreadsSize > robot.maxConnections)
     return false;
    if (activeQdRecords.getAt(activeLocation) != null)
     robot.answer(activeLocation, null);
    if (robot.activate(activeLocation, fellAsleepTime))
    {
     connection.close();
     robot.notifyObservers(0, 0, null, null, activeLocation);
    }
    int millis = 0;
    if (activeAgents.getAt(activeLocation) == null)
    {
     if (activeServers.getIntAt((activeLocation - 1) * 2) <= 0)
      while (true)
       synchronized (lock = robot.lock)
       {
        if (lock == robot.lock)
        {
         if (robot.queueQdRecords.size() <= 0)
          return false;
         break;
        }
       }
     if (fellAsleepTime == 0)
      fellAsleepTime = (int)(System.currentTimeMillis() / 1000L);
     millis = activeLocation + (IDLE_SLEEP_MILLIS + 1);
    }
    try
    {
     Thread.sleep(millis);
    }
    catch (InterruptedException e) {}
    if (millis > 0)
     return true;
    fellAsleepTime = 0;
   }
   boolean isNext = false;
   IntVector firstServers = robot.firstServers;
   DNSRecord qdRecord =
    (DNSRecord)activeQdRecords.getAt(activeLocation);
   if (activeServers.getIntAt(activeLocation * 2 - 1) == 0)
   {
    int uniqLoc = activeServers.getIntAt((activeLocation - 1) * 2);
    InetAddress server =
     (InetAddress)robot.uniqueServers.getAt(uniqLoc);
    try
    {
     connection.open(server);
    }
    catch (IOException e)
    {
     while (true)
      synchronized (lock = robot.lock)
      {
       if (lock == robot.lock)
       {
        robot.totalConnectFails++;
        break;
       }
      }
     int count;
     if ((count = firstServers.getIntAt(uniqLoc * 4 - 2) - 1) >= 0)
     {
      while (true)
       synchronized (lock = robot.lock)
       {
        if (lock == robot.lock)
        {
         firstServers.setAt(uniqLoc * 4 - 1, count);
         break;
        }
       }
      robot.notifyObservers(0, 0, null, null, activeLocation);
      if (count > 0)
       while (true)
        synchronized (lock = robot.lock)
        {
         if (lock == robot.lock)
         {
          Object agent;
          if ((agent =
              activeAgents.setAt(activeLocation, null)) != null)
          {
           robot.activeAgentsSize--;
           robot.queryHelper(uniqLoc, null, qdRecord,
            true, (Notifiable)agent);
          }
          break;
         }
        }
     }
     robot.answer(activeLocation, null);
     return true;
    }
    answered = false;
    while (true)
     synchronized (lock = robot.lock)
     {
      if (lock == robot.lock)
      {
       activeServers.setAt(activeLocation * 2 - 1, -1);
       if (firstServers.getIntAt(uniqLoc * 4 - 1) <= 0)
        firstServers.setAt(uniqLoc * 4 - 1,
         robot.maxServerConnections);
       robot.totalConnections++;
       break;
      }
     }
    robot.notifyObservers(0, 0, null, null, activeLocation);
    isNext = true;
    if (activeAgents.getAt(activeLocation) == null)
     return true;
   }
   int qType = qdRecord.getRType();
   DNSRecord[] records = null;
   byte[] msgBytes = null;
   if (activeServers.getIntAt(activeLocation * 2 - 1) == -1)
   {
    int queryId = 0;
    DNSMsgHeader qHeader =
     DNSMsgHeader.construct(DNSMsgHeader.QUERY, qType !=
     DNSRecord.SOA && qType < DNSRecord.IXFR, 1, 0, 0, 0, false);
    (records = new DNSRecord[1])[0] = qdRecord;
    msgBytes = DNSConnection.encode(qHeader, records);
    try
    {
     connection.send(msgBytes);
     queryId = -qHeader.getId() - 2;
     while (true)
      synchronized (lock = robot.lock)
      {
       if (lock == robot.lock)
       {
        robot.totalBytesSent += msgBytes.length;
        robot.totalQueries++;
        if (qType == DNSRecord.AXFR)
         robot.totalZoneRequests++;
        break;
       }
      }
     isNext = false;
    }
    catch (IOException e)
    {
     connection.close();
    }
    while (true)
     synchronized (lock = robot.lock)
     {
      if (lock == robot.lock)
      {
       activeServers.setAt(activeLocation * 2 - 1, queryId);
       break;
      }
     }
    robot.notifyObservers(0, 0, null, null, activeLocation);
    if (isNext)
     robot.answer(activeLocation, null);
    return true;
   }
   if (activeServers.getIntAt(activeLocation * 2 - 1) > 0)
    isNext = true;
   int qdCount = 0, anCount = 0, nsCount = 0, rCode = 0;
   if (qType != DNSRecord.AXFR ||
       activeAgents.getAt(activeLocation) != null)
   {
    try
    {
     msgBytes = connection.receive(true);
    }
    catch (IOException e) {}
    if (msgBytes != null)
    {
     while (true)
      synchronized (lock = robot.lock)
      {
       if (lock == robot.lock)
       {
        robot.totalBytesReceived += msgBytes.length;
        break;
       }
      }
     DNSMsgHeader header;
     if ((records = DNSConnection.decode(msgBytes)) == null ||
         !(header = new DNSMsgHeader(msgBytes)).isResponse() ||
         (!isNext && -header.getId() - 2 !=
         activeServers.getIntAt(activeLocation * 2 - 1)) ||
         (qdCount = header.getQdCount()) > 1 ||
         (anCount = header.getAnCount()) >
         records.length - qdCount ||
         (rCode = header.getRCode()) == DNSMsgHeader.NOTIMP ||
         (answered && rCode == DNSMsgHeader.FORMERR) ||
         (nsCount = header.getNsCount()) > records.length ||
         qType != DNSRecord.AXFR && qdCount <= 0)
      msgBytes = null;
    }
   }
   if (msgBytes == null)
   {
    if (isNext)
    {
     while (true)
      synchronized (lock = robot.lock)
      {
       if (lock == robot.lock)
       {
        robot.totalZoneTransfers++;
        break;
       }
      }
     robot.answer(activeLocation, null);
    }
    connection.close();
    while (true)
     synchronized (lock = robot.lock)
     {
      if (lock == robot.lock)
      {
       activeServers.setAt(activeLocation * 2 - 1, 0);
       break;
      }
     }
    robot.notifyObservers(0, 0, null, null, activeLocation);
    if (!answered)
     robot.answer(activeLocation, null);
    return true;
   }
   answered = true;
   while (true)
    synchronized (lock = robot.lock)
    {
     if (lock == robot.lock)
     {
      robot.totalRecordsReceived += records.length - qdCount;
      break;
     }
    }
   if (rCode == DNSMsgHeader.NOERROR)
   {
    if (!isNext)
     while (true)
      synchronized (lock = robot.lock)
      {
       if (lock == robot.lock)
       {
        int uniqLoc =
         activeServers.getIntAt((activeLocation - 1) * 2);
        if (firstServers.getIntAt(uniqLoc * 4 - 1) <= 0)
         firstServers.setAt(uniqLoc * 4 - 1, 1);
        robot.totalAnswers++;
        break;
       }
      }
    if (qType != DNSRecord.AXFR)
     if (anCount + nsCount > 0)
      anCount = records.length;
      else rCode = DNSMsgHeader.FORMERR;
     else if (anCount > 0)
     {
      if (anCount > 1)
       isNext = true;
      anCount += qdCount;
      if (!isNext)
       isNext = true;
       else if (records[anCount - 1].getRType() == DNSRecord.SOA)
       {
        isNext = false;
        while (true)
         synchronized (lock = robot.lock)
         {
          if (lock == robot.lock)
          {
           robot.totalZoneTransfers++;
           break;
          }
         }
       }
     }
   }
   if (rCode != DNSMsgHeader.NOERROR)
   {
    anCount = 0;
    if (!isNext)
     while (true)
      synchronized (lock = robot.lock)
      {
       if (lock == robot.lock)
       {
        if (qType != DNSRecord.AXFR &&
            rCode == DNSMsgHeader.NXDOMAIN)
        {
         robot.totalAnswers++;
         break;
        }
        activeServers.setAt(activeLocation * 2 - 1, -1);
        anCount = -1;
        if (qType == DNSRecord.AXFR)
        {
         robot.totalDeniedTransfers++;
         break;
        }
        if (rCode != DNSMsgHeader.REFUSED)
         break;
        int uniqLoc =
         activeServers.getIntAt((activeLocation - 1) * 2);
        if ((nsCount = firstServers.getIntAt(uniqLoc * 4 - 1)) <= 0)
         break;
        firstServers.setAt(uniqLoc * 4 - 1, nsCount - 1);
        break;
       }
      }
    isNext = false;
   }
   while (qdCount < anCount &&
          activeAgents.getAt(activeLocation) != null)
    robot.answer(activeLocation, records[qdCount++]);
   if (!isNext)
    robot.answer(activeLocation, null);
  }
  return true;
 }

 protected void done()
 {
  DNSClientRobot robot;
  int activeLocation;
  if ((robot = this.robot) != null &&
      (activeLocation = this.activeLocation) > 0)
  {
   DNSConnection connection;
   if ((connection = this.connection) != null)
    connection.close();
   Object lock;
   while (true)
    synchronized (lock = robot.lock)
    {
     if (lock == robot.lock)
     {
      robot.activeServers.setAt(activeLocation * 2 - 1, 0);
      break;
     }
    }
   if (robot.activeQdRecords.getAt(activeLocation) != null)
    robot.answer(activeLocation, null);
   while (true)
    synchronized (lock = robot.lock)
    {
     if (lock == robot.lock)
     {
      robot.purgeConnection(activeLocation);
      robot.activeThreadsSize--;
      robot.activeThreads.setAt(activeLocation, null);
      robot.notifyObservers(0, 0, null, null, activeLocation);
      break;
     }
    }
  }
  activeLocation = 0;
 }

/**
 * NOTE: Check for integrity of this object. client robot fields are
 * not used here. client robot is not checked. For debug purpose
 * only.
 */
 public void integrityCheck()
 {
  super.integrityCheck();
  if (robot == null)
   throw new InternalError("robot: null");
  int activeLocation;
  if ((activeLocation = this.activeLocation) <= 0)
   throw new InternalError("activeLocation: " +
              UnsignedInt.toString(activeLocation, false));
  if (connection == null)
   throw new InternalError("connection: null");
 }
}
