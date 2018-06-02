/*
 * @(#) src/net/sf/ivmaidns/dnslook.java --
 * Utility for looking up Internet domains.
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

import java.io.BufferedWriter;
import java.io.EOFException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.OutputStreamWriter;

import java.net.InetAddress;
import java.net.UnknownHostException;

import net.sf.ivmaidns.dns.DNSConnection;
import net.sf.ivmaidns.dns.DNSMsgHeader;
import net.sf.ivmaidns.dns.DNSName;
import net.sf.ivmaidns.dns.DNSRecord;

import net.sf.ivmaidns.util.GComparator;
import net.sf.ivmaidns.util.ObjectVector;
import net.sf.ivmaidns.util.UnsignedInt;

/**
 * Utility for looking up Internet domains.
 **
 * @version 3.2
 * @author Ivan Maidanski
 */
public final class dnslook
{

 public static final String NAME = "dnslook";

 public static final String VERSION = "3.2";

 public static final String DESCRIPTION =
  "Utility for looking up Internet domains";

 public static final String COPYRIGHT =
  "(c) 2001 Ivan Maidanski <ivmai@mail.ru> http://ivmaidns.sf.net";

 public static final String LICENSE =
  "This is free software. No warranties. All rights reserved.";

 public static final String ARGS_INFO =
  "[-z] [-n] [-p] [<dns_server>] <name> [[-d] <out_txt_file>]";

 protected static final String DEFAULT_SERVER = "8.8.8.8"; /* Google DNS */

 private dnslook() {}

 public static final void main(String[] args)
  throws NullPointerException
 {
  int exitCode;
  try
  {
   exitCode = intMain(args);
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

 public static int intMain(String[] args)
  throws NullPointerException
 {
  int index, len = args.length, rcode, count = 0;
  boolean isZone = false, isNS = false, isPlain = false;
  String serverName = null, queryName = null, fileName = null;
  for (index = 0; index < len; index++)
  {
   String arg = args[index];
   if ((count = arg.length()) > 0)
    if (arg.charAt(0) != '-')
     if (queryName == null)
      queryName = arg;
      else if (serverName == null)
      {
       serverName = queryName;
       queryName = arg;
      }
       else if (fileName == null)
        fileName = arg;
        else
        {
         queryName = null;
         break;
        }
     else for (rcode = 1; rcode < count; rcode++)
      switch (arg.charAt(rcode))
      {
      case 'Z':
      case 'z':
       if (isZone)
       {
        queryName = null;
        count = len = 0;
        break;
       }
       isZone = true;
       break;
      case 'N':
      case 'n':
       if (isNS)
       {
        queryName = null;
        count = len = 0;
        break;
       }
       isNS = true;
       break;
      case 'P':
      case 'p':
       if (isPlain)
       {
        queryName = null;
        count = len = 0;
        break;
       }
       isPlain = true;
       break;
      case 'D':
      case 'd':
       if (index == len - 1 || fileName != null)
       {
        queryName = null;
        count = len = 0;
        break;
       }
       fileName = args[++index];
       break;
      default:
       queryName = null;
       count = len = 0;
      }
  }
  if (queryName == null)
  {
   System.out.println(NAME + " v" + VERSION + " - " + DESCRIPTION);
   System.out.println(COPYRIGHT);
   System.out.println(LICENSE);
   System.out.println("Usage: " + NAME + " " + ARGS_INFO);
   System.out.println(" This utility allows the user to look up" +
    " Internet DNS records and");
   System.out.println("entire domains of a given name from the" +
    " specified server. Every such");
   System.out.println("record consists of its name, the class" +
    " (IN), time-to-live value, its");
   System.out.println("type (mostly A, NS, MX, CNAME, PTR or" +
    " SOA), and its value according");
   System.out.println("to its type. Refer to RFC1035 document for" +
    " more information on DNS.");
   System.out.println(" Here, if <dns_server> parameter is" +
    " omitted then the default server");
   System.out.println("is used. If an optional <out_txt_file>" +
    " name is specified then the");
   System.out.println("looked up records are all saved to it in" +
    " the standard DNS zone file");
   System.out.println("format, which is either plain (the fields" +
    " of the records are");
   System.out.println("tab-separated) if -p option is set or" +
    " blank-padded (with the records");
   System.out.println("sorted ascending and duplicated records" +
    " removed). By default, all");
   System.out.println("the records of a given name are looked up," +
    " but setting of -n option");
   System.out.println("causes the utility to look up only name" +
    " server records.");
   System.out.println(" To look up an entire domain (zone) -z" +
    " option must be set (but, this");
   System.out.println("operation succeeds only if zone" +
    " transferring is allowed by the");
   System.out.println("server). If -n option is set in addition" +
    " to -z option then all of");
   System.out.println("the authoritative name servers for a given" +
    " domain are sequentially");
   System.out.println("tried to look up the entire domain from" +
    " until it succeeds.");
   return args.length > 0 ? 2 : 0;
  }
  DNSName qName;
  try
  {
   qName = new DNSName(queryName, null);
  }
  catch (NumberFormatException e)
  {
   System.err.println("Illegal name: " + queryName);
   return 3;
  }
  if (serverName == null)
  {
   serverName = DEFAULT_SERVER;
   if (isZone)
    isNS = true;
  }
  InetAddress server;
  try
  {
   server = InetAddress.getByName(serverName);
  }
  catch (UnknownHostException e)
  {
   System.err.println("Host unknown: " + serverName);
   return 9;
  }
  catch (SecurityException e)
  {
   return 9;
  }
  System.out.println("Connecting to " + serverName + "...");
  DNSConnection connection = new DNSConnection();
  try
  {
   connection.open(server);
  }
  catch (IOException e)
  {
   System.err.println("Could not establish connection to: " +
    serverName);
   return 8;
  }
  DNSMsgHeader qHeader, header = null;
  DNSRecord[] records = null;
  DNSRecord resRecord;
  byte[] msgBytes;
  DNSName[] servers = new DNSName[0];
  rcode = DNSMsgHeader.NOERROR;
  if (isNS || !isZone)
  {
   System.out.println("Sending " +
    (qName.getLevel() > 0 ? qName.getRelativeAt(0) : "root") +
    (isNS ? " domain" : "") + " query...");
   qHeader = DNSMsgHeader.construct(DNSMsgHeader.QUERY, true,
    1, 0, 0, 0, false);
   records = new DNSRecord[1];
   records[0] = new DNSRecord(qName, isNS ? DNSRecord.NS :
    DNSRecord.ANY, DNSRecord.IN);
   msgBytes = DNSConnection.encode(qHeader, records);
   try
   {
    connection.send(msgBytes);
   }
   catch (IOException e)
   {
    System.err.println("Data transmission error!");
    return 7;
   }
   System.out.println("Receiving answer...");
   try
   {
    msgBytes = connection.receive(true);
   }
   catch (IOException e)
   {
    connection.close();
    System.err.println("Data transmission error!");
    return 7;
   }
   if ((records = DNSConnection.decode(msgBytes)) == null)
   {
    connection.close();
    System.err.println("Invalid protocol message received!");
    return 7;
   }
   header = new DNSMsgHeader(msgBytes);
   if (!header.isResponse() || header.getId() != qHeader.getId())
   {
    connection.close();
    System.err.println("Bad protocol message header: " +
     header.toString());
    return 7;
   }
   System.out.println("Authoritative answer: " +
    (header.isAuthoritativeAnswer() ? "Yes" : "No"));
   if (header.isAuthenticData())
    System.out.println("Authentic data received");
   if (header.isTruncated())
    System.out.println("Response message truncated!");
   if ((rcode = header.getRCode()) != DNSMsgHeader.NOERROR)
    System.out.println(rcode == DNSMsgHeader.NXDOMAIN ? (isNS ?
     "Domain does not exist!" : "Requested name does not exist!") :
     "Server returned error: " +
     UnsignedInt.toAbbreviation(rcode, DNSMsgHeader.RCODE_ABBREVS));
   len = records.length;
   if ((index = header.getQdCount()) < len)
   {
    count = header.getAnCount();
    if (!isNS)
    {
     int section = 1;
     System.out.println("Answer:");
     do
     {
      while (count <= 0)
      {
       count = len;
       String str = "";
       if (++section == 2)
       {
        count = header.getNsCount();
        str = "Name servers:";
       }
        else if (section == 3)
        {
         count = header.getArCount();
         str = "Additional:";
        }
       System.out.println(str);
      }
      System.out.print(" ");
      System.out.println(records[index++].toString(null,
       null, false));
      count--;
     } while (index < len);
    }
     else if (rcode == DNSMsgHeader.NOERROR)
     {
      boolean found = false;
      System.out.print("Found authoritative name servers:");
      servers = new DNSName[count];
      for (int index2 = 0; index2 < count && index < len; index2++)
       if ((resRecord = records[index++]).getRType() ==
           DNSRecord.NS && qName.equals(resRecord.getRName()))
       {
        if (!found)
        {
         found = true;
         System.out.println("");
        }
        System.out.print(" ");
        System.out.println((servers[index2] =
         (DNSName)resRecord.getRData()[0]).getAbsolute());
       }
      if (!found)
      {
       System.out.println(" none");
       System.out.println("Domain does not exist!");
       rcode = DNSMsgHeader.NXDOMAIN;
      }
     }
   }
   if (rcode != DNSMsgHeader.NOERROR)
   {
    connection.close();
    return rcode == DNSMsgHeader.NXDOMAIN ? 5 : 6;
   }
  }
  if (isZone)
  {
   index = servers.length;
   DNSName rName;
   if (index != 0)
   {
    try
    {
     rName = new DNSName(serverName, null);
     while (!rName.equals(servers[index - 1]) && --index > 0);
    }
    catch (NumberFormatException e) {}
    if (index > 0)
    {
     servers[index - 1] = null;
     index = 0;
    }
     else
     {
      connection.close();
      serverName = null;
     }
   }
   int size = -1, receivedBytesCount = 0, time;
   String errStr = null;
   Object[] soaRData = null;
   do
   {
    if (serverName == null)
    {
     do
     {
      if (index >= servers.length)
       return rcode;
     } while (servers[index++] == null);
     serverName = servers[index - 1].getRelativeAt(0);
     System.out.println("Connecting to " + serverName + "...");
     try
     {
      server = InetAddress.getByName(serverName);
     }
     catch (UnknownHostException e)
     {
      System.err.println("Host unknown!");
      serverName = null;
      rcode = 9;
      continue;
     }
     catch (SecurityException e)
     {
      serverName = null;
      rcode = 9;
      continue;
     }
     try
     {
      connection.open(server);
     }
     catch (IOException e)
     {
      System.err.println("Could not establish connection to: " +
       serverName);
      serverName = null;
      rcode = 8;
      continue;
     }
    }
    System.out.println("Sending zone query for: " +
     qName.getRelativeAt(0));
    qHeader = DNSMsgHeader.construct(DNSMsgHeader.QUERY, false,
     1, 0, 0, 0, false);
    records = new DNSRecord[1];
    records[0] = new DNSRecord(qName, DNSRecord.AXFR, DNSRecord.IN);
    msgBytes = DNSConnection.encode(qHeader, records);
    try
    {
     connection.send(msgBytes);
    }
    catch (IOException e)
    {
     connection.close();
     System.err.println("Data transmission error!");
     serverName = null;
     rcode = 7;
     continue;
    }
    System.out.println("Waiting for response...");
    receivedBytesCount = 0;
    errStr = null;
    time = (int)System.currentTimeMillis();
    try
    {
     if ((records = DNSConnection.decode(msgBytes =
         connection.receive(true))) == null)
     {
      connection.close();
      System.err.println("Invalid protocol message received!");
      serverName = null;
      rcode = 7;
      continue;
     }
     header = new DNSMsgHeader(msgBytes);
     if (!header.isResponse() ||
         header.getId() != qHeader.getId() && header.getId() != 0)
     {
      connection.close();
      System.err.println("Bad protocol message header: " +
       header.toString());
      serverName = null;
      rcode = 7;
      continue;
     }
     if ((rcode = header.getRCode()) != DNSMsgHeader.NOERROR)
     {
      connection.close();
      System.err.println(rcode == DNSMsgHeader.REFUSED ?
       "Zone access denied by this server!" :
       "Server returned error: " + UnsignedInt.toAbbreviation(rcode,
       DNSMsgHeader.RCODE_ABBREVS));
      rcode = rcode == DNSMsgHeader.REFUSED ? 4 : 6;
      serverName = null;
      continue;
     }
     if ((rcode = header.getAnCount()) <= 0 ||
         (count = header.getQdCount()) > records.length - rcode)
     {
      connection.close();
      System.err.println("None answer records received!");
      serverName = null;
      rcode = 6;
      continue;
     }
     if ((resRecord = records[count]).getRType() != DNSRecord.SOA ||
         resRecord.getRClass() != DNSRecord.IN ||
         !(rName = resRecord.getRName()).equals(qName))
     {
      connection.close();
      System.err.println("Non-authoritative record received: " +
       resRecord.toString(null, null, false));
      serverName = null;
      rcode = 6;
      continue;
     }
     qName = rName;
     soaRData = resRecord.getRData();
     if (soaRData.length <= DNSRecord.SOA_MINTTL_INDEX)
     {
      connection.close();
      System.err.println("Invalid authority data received!");
      serverName = null;
      rcode = 6;
      continue;
     }
     size = 0;
     if (fileName != null)
     {
      DNSRecord[] curRecords = records;
      receivedBytesCount = msgBytes.length;
      System.out.print("Getting zone records ");
      records[0] = resRecord;
      rcode--;
      count++;
      size = 1;
      do
      {
       while (rcode-- > 0 && (resRecord =
              curRecords[count++]).getRType() != DNSRecord.SOA)
        if (resRecord.getRClass() == DNSRecord.IN ||
            resRecord.getRName().isInDomain(qName, false))
        {
         if (size % 100 == 1)
         {
          System.out.print(".");
          System.out.flush();
         }
         records[size++] = resRecord;
        }
       if (rcode >= 0)
        break;
       receivedBytesCount +=
        (msgBytes = connection.receive(true)).length;
       if ((curRecords = DNSConnection.decode(msgBytes)) == null ||
           !(header = new DNSMsgHeader(msgBytes)).isResponse() ||
           (count = header.getQdCount()) >
           curRecords.length - header.getAnCount())
       {
        errStr = "Invalid protocol message received!";
        break;
       }
       if ((rcode = header.getRCode()) != DNSMsgHeader.NOERROR)
       {
        errStr = "Server returned error: " +
         UnsignedInt.toAbbreviation(rcode,
         DNSMsgHeader.RCODE_ABBREVS);
        break;
       }
       if (records.length - (rcode = header.getAnCount()) < size)
       {
        int newSize;
        DNSRecord[] newRecords;
        if ((newSize = (size >> 1) + rcode + size + 1) <= size)
         newSize = -1 >>> 1;
        System.arraycopy(records, 0,
         newRecords = new DNSRecord[newSize], 0, size);
        records = newRecords;
       }
      } while (true);
     }
    }
    catch (EOFException e)
    {
     errStr = "Connection terminated by server!";
    }
    catch (InterruptedIOException e)
    {
     errStr = "Connection time-out!";
    }
    catch (IOException e)
    {
     errStr = "Data transmission error!";
    }
    time = (int)System.currentTimeMillis() - time;
    connection.close();
    if (size > 0)
     System.out.println("");
    if (errStr != null)
     System.err.println(errStr);
    if (size < 0)
     serverName = null;
     else break;
   } while (true);
   if (fileName != null)
   {
    System.out.println("Received: " +
     UnsignedInt.toString(size, false) + " records (" +
     UnsignedInt.toString(receivedBytesCount, false) +
     " bytes) [done in " + UnsignedInt.toString(time, false) +
     " ms]");
    if (size != 0)
    {
     int groups = 1;
     count = size;
     DNSName groupRName;
     DNSRecord prevRecord;
     if (!isPlain)
     {
      System.out.print("Sorting zone records...");
      ObjectVector.sort(records, 1, size - 1, GComparator.INSTANCE);
      System.out.println("");
      System.out.print("Discarding duplicates...");
      groupRName = (prevRecord = records[0]).getRName();
      for (index = 0; ++index < size; prevRecord = resRecord)
       if (!(rName = (resRecord =
           records[index]).getRName()).equals(groupRName))
       {
        groupRName = rName;
        groups++;
       }
        else if (resRecord.equals(prevRecord))
        {
         records[index] = null;
         count--;
        }
      System.out.println(" (" + UnsignedInt.toString(size - count,
       false) + " records removed)");
      System.out.print("Saving formatted zone file [" +
       fileName + "] ");
     }
      else System.out.println("Saving plain file [" +
       fileName + "]...");
     BufferedWriter out;
     try
     {
      out = new BufferedWriter(
       new OutputStreamWriter(new FileOutputStream(fileName)));
     }
     catch (IOException e)
     {
      if (!isPlain)
       System.out.println("");
      System.err.println("Cannot create this file!");
      return 10;
     }
     catch (SecurityException e)
     {
      if (!isPlain)
       System.out.println("");
      return 10;
     }
     time = (int)System.currentTimeMillis();
     try
     {
      if (!isPlain)
      {
       out.write("; DNS zone file");
       out.newLine();
       out.write("; Generated by: " + NAME + " v" + VERSION);
       out.newLine();
       out.newLine();
       out.write("$ORIGIN ");
       out.write(qName.getAbsolute());
       out.newLine();
       out.write("; TTL ");
       out.write(UnsignedInt.toString(((Number)
        soaRData[DNSRecord.SOA_MINTTL_INDEX]).intValue(), true));
       out.newLine();
       out.write("; Obtained from: ");
       out.write(serverName);
       out.newLine();
       out.write("; Zone version: ");
       out.write(UnsignedInt.toString(((Number)
        soaRData[DNSRecord.SOA_SERIAL_INDEX]).intValue(), true));
       out.newLine();
       out.write("; Contains: ");
       out.write(UnsignedInt.toString(count, false));
       out.write(" records for ");
       out.write(UnsignedInt.toString(groups, false));
       out.write(" resources");
       if (errStr != null)
        out.write(" (incomplete transfer)");
       out.newLine();
       out.newLine();
      }
       else qName = null;
      groupRName = null;
      prevRecord = null;
      index = -1;
      while (++index < size)
       if ((resRecord = records[index]) != null)
       {
        if (!isPlain)
        {
         if (index % 100 == 0)
         {
          System.out.print(".");
          System.out.flush();
         }
         rName = resRecord.getRName();
         if (groupRName != null &&
             !rName.isInDomain(groupRName, false) ||
             groupRName == null && !rName.equals(qName))
         {
          out.newLine();
          groupRName = rName;
         }
        }
        out.write(resRecord.toString(qName, prevRecord, isPlain));
        out.newLine();
        if (!isPlain)
         prevRecord = resRecord;
       }
      out.close();
     }
     catch (IOException e)
     {
      if (!isPlain)
       System.out.println("");
      System.err.println("File write error!");
      return 10;
     }
     time = (int)System.currentTimeMillis() - time;
     if (!isPlain)
      System.out.println("");
     System.out.println("[saved in " +
      UnsignedInt.toString(time, false) + " ms]");
    }
   }
   if (errStr != null)
    return 1;
  }
   else
   {
    connection.close();
    if (fileName != null && header != null && records != null)
    {
     if (isPlain)
      System.out.println("Saving plain file [" + fileName + "]...");
      else System.out.println("Saving formatted file [" +
       fileName + "] ");
     BufferedWriter out;
     try
     {
      out = new BufferedWriter(
       new OutputStreamWriter(new FileOutputStream(fileName)));
     }
     catch (IOException e)
     {
      System.err.println("Cannot create this file!");
      return 10;
     }
     catch (SecurityException e)
     {
      return 10;
     }
     try
     {
      if (!isPlain)
      {
       out.write("; DNS answer file");
       if (isNS)
        out.write(" (name servers only)");
       out.newLine();
       out.write("; Generated by: " + NAME + " v" + VERSION);
       out.newLine();
       out.write("; Obtained from: ");
       out.write(serverName);
       if (header.isAuthoritativeAnswer())
        out.write(" (authoritative server)");
       out.newLine();
       out.newLine();
      }
      DNSRecord prevRecord = null;
      count = header.getAnCount();
      for (index = header.getQdCount();
           count-- > 0 && index < len; index++)
       if ((resRecord = records[index]) != null)
       {
        out.write(resRecord.toString(null, prevRecord, isPlain));
        out.newLine();
        if (!isPlain)
         prevRecord = resRecord;
       }
      out.close();
     }
     catch (IOException e)
     {
      System.err.println("File write error!");
      return 10;
     }
    }
   }
  System.out.println("Ok");
  return 0;
 }
}
