/*
 * @(#) src/net/sf/ivmaidns/storage/ListStorage.java --
 * Class for list storage.
 **
 * Copyright (c) 2000-2001 Ivan Maidanski <ivmai@mail.ru>
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

package net.sf.ivmaidns.storage;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import net.sf.ivmaidns.util.JavaConsts;

/**
 * Class for list storage.
 **
 * @version 2.0
 * @author Ivan Maidanski
 */
public class ListStorage extends ObjectStorage
{

/**
 * The class version unique identifier for serialization
 * interoperability.
 **
 * @since 2.0
 */
 private static final long serialVersionUID = 8729441242300226023L;

/**
 * NOTE: links must be != null, links length == 2 +
 * elements length * 2, links[0] = nextLocation(0, true), links[1] =
 * emptyLocation() - 1, links[loc * 2] = elements[loc - 1] == null ?
 * -nextEmptyLocation(loc) : nextLocation(loc !=
 * links[links[0] * 2 + 1] ? loc : 0, true), links[loc * 2 + 1] =
 * elements[loc - 1] == null ? -prevEmptyLocation(loc) :
 * nextLocation(loc != links[loc] ? loc : 0, false). The order of
 * empty locations is not serialized.
 */
 protected transient int[] links = new int[2];

 public ListStorage() {}

/**
 * NOTE: Must be synchronized outside.
 */
 protected void minimizeCapacity()
 {
  int[] links = this.links;
  int capacity, oldCapacity;
  if (links[(capacity = oldCapacity =
      (links.length >> 1) - 1) << 1] < 0)
  {
   while (links[--capacity << 1] < 0);
   Object[] newElements = new Object[capacity];
   int[] newLinks = new int[(capacity + 1) << 1];
   if (capacity > 0)
   {
    do
    {
     int next = -links[oldCapacity << 1];
     links[-(links[(next << 1) + 1] =
      links[(oldCapacity << 1) + 1]) << 1] = -next;
     if (--oldCapacity == links[1])
      links[1] = next - 1;
    } while (oldCapacity > capacity);
    System.arraycopy(this.elements, 0, newElements, 0, capacity);
    System.arraycopy(links, 0, newLinks, 0, (capacity + 1) << 1);
   }
   this.elements = newElements;
   this.links = newLinks;
  }
 }

 public final int emptyLocation()
 {
  return this.links[1] + 1;
 }

/**
 * NOTE: prevLoc must be >= 0 (according to the semantics of list),
 * otherwise ArrayStoreException is thrown. The effectiveness is
 * constant. Observers notification is performed. Must be
 * synchronized outside.
 */
 public int insertAt(int prevLoc, int emptyLocation, Object value)
  throws IllegalArgumentException, ArrayStoreException
 {
  int[] links = this.links;
  int next = (links.length >> 1) - 1, last;
  if (prevLoc != 0)
  {
   if ((last = prevLoc) < 0)
    last = -prevLoc;
   if (last - 1 >= next || links[last << 1] < 0)
    throw new IllegalArgumentException("prevLoc: " +
               Integer.toString(prevLoc));
  }
  if (emptyLocation != 0 && (emptyLocation < 0 ||
      emptyLocation <= next && links[emptyLocation << 1] >= 0))
   throw new IllegalArgumentException("emptyLocation: " +
              Integer.toString(emptyLocation));
  if (value == null || prevLoc < 0)
   throw new ArrayStoreException("prevLoc: " +
              Integer.toString(prevLoc) + ", emptyLocation: " +
              Integer.toString(emptyLocation) + ", value: " +
              (value != null ? value.toString() : "null"));
  if (emptyLocation == 0)
   emptyLocation = (prevLoc < next &&
    links[(prevLoc + 1) << 1] < 0 ? prevLoc : links[1]) + 1;
  Object[] elements = this.elements;
  if (next < emptyLocation)
  {
   int oldCapacity = next;
   if ((next += (next >> 1) + 7) <= emptyLocation)
    next = emptyLocation;
   if (next >= (-1 >>> 2))
    next = -1 >>> 1;
   Object[] newElements = new Object[last = next];
   int[] newLinks = new int[(next + 1) << 1];
   System.arraycopy(elements, 0, newElements, 0, oldCapacity);
   System.arraycopy(links, 0, newLinks, 0, (oldCapacity + 1) << 1);
   links = newLinks;
   while (--last > oldCapacity)
    links[last << 1] = (links[(last << 1) + 3] = -last) - 1;
   if (links[1] < oldCapacity)
    links[-(links[(oldCapacity << 1) + 3] =
     links[(links[1] << 1) + 3]) << 1] = -oldCapacity - 1;
   links[-(links[(links[1] << 1) + 3] = -next) << 1] =
    -links[1] - 1;
   this.elements = elements = newElements;
   this.links = links;
  }
  if ((next = -links[emptyLocation << 1]) != emptyLocation)
  {
   links[-(links[(next << 1) + 1] =
    links[(emptyLocation << 1) + 1]) << 1] = -next;
   if (links[1] == emptyLocation - 1)
    links[1] = next - 1;
  }
   else links[1] = elements.length;
  next = links[emptyLocation << 1] = links[prevLoc << 1];
  if ((last = prevLoc) == 0)
   links[(last = next > 0 ? links[(next << 1) + 1] :
    (next = emptyLocation)) << 1] = emptyLocation;
  links[(emptyLocation << 1) + 1] = last;
  links[prevLoc << 1] = links[(next << 1) + 1] = emptyLocation;
  elements[emptyLocation - 1] = value;
  notifyObservers(0, emptyLocation, null);
  return emptyLocation;
 }

/**
 * NOTE: If value is added then it is placed to the end of list. The
 * effectiveness is constant. Observers notification is performed.
 * Must be synchronized outside.
 */
 public Object setAt(int location, Object value)
  throws IllegalArgumentException
 {
  if (location <= 0)
   throw new IllegalArgumentException("location: " +
              Integer.toString(location));
  Object[] elements = this.elements;
  int prevLoc, last;
  int[] links = this.links;
  if ((prevLoc = elements.length) < location)
  {
   int oldCapacity = prevLoc;
   if ((prevLoc += (prevLoc >> 1) + 7) <= location)
    prevLoc = location;
   if (prevLoc >= (-1 >>> 2))
    prevLoc = -1 >>> 1;
   Object[] newElements = new Object[last = prevLoc];
   int[] newLinks = new int[(prevLoc + 1) << 1];
   System.arraycopy(elements, 0, newElements, 0, oldCapacity);
   System.arraycopy(links, 0, newLinks, 0, (oldCapacity + 1) << 1);
   links = newLinks;
   while (--last > oldCapacity)
    links[last << 1] = (links[(last << 1) + 3] = -last) - 1;
   if (links[1] < oldCapacity)
    links[-(links[(oldCapacity << 1) + 3] =
     links[(links[1] << 1) + 3]) << 1] = -oldCapacity - 1;
   links[-(links[(links[1] << 1) + 3] = -prevLoc) << 1] =
    -links[1] - 1;
   this.elements = elements = newElements;
   this.links = links;
  }
  Object oldValue;
  if ((oldValue = elements[location - 1]) != null || value != null)
  {
   if (oldValue == null)
   {
    if ((prevLoc = -links[location << 1]) != location)
    {
     links[-(links[(prevLoc << 1) + 1] =
      links[(location << 1) + 1]) << 1] = -prevLoc;
     if (links[1] == location - 1)
      links[1] = prevLoc - 1;
    }
     else links[1] = elements.length;
    if ((prevLoc = links[0]) > 0)
     last = links[(location << 1) + 1] =
      links[((links[location << 1] = prevLoc) << 1) + 1];
     else links[0] = last = prevLoc = location;
    links[last << 1] = links[(prevLoc << 1) + 1] = location;
   }
   prevLoc = 0;
   if ((elements[location - 1] = value) == null)
   {
    prevLoc = links[(location << 1) + 1];
    links[((links[prevLoc << 1] =
     links[location << 1]) << 1) + 1] = prevLoc;
    if (links[0] == location)
    {
     links[0] = location != prevLoc ? links[location << 1] : 0;
     prevLoc = 0;
    }
    int next;
    if ((next = links[1] + 1) <= elements.length)
    {
     links[location << 1] = -next;
     last = -(links[(location << 1) + 1] = links[(next << 1) + 1]);
    }
     else last = next = location;
    links[last << 1] = links[(next << 1) + 1] = -location;
    links[1] = location - 1;
    location = -location;
   }
   notifyObservers(prevLoc, location, oldValue);
  }
  return oldValue;
 }

/**
 * NOTE: Must be synchronized outside.
 */
 public int childLocation(int parentLocation, boolean forward)
  throws IllegalArgumentException
 {
  int first;
  int[] links = this.links;
  if (parentLocation < 0 || links.length >> 1 <= parentLocation ||
      (first = links[parentLocation << 1]) < 0)
   throw new IllegalArgumentException("location: " +
              Integer.toString(parentLocation));
  if (parentLocation != 0)
   first = 0;
  if (!forward && first > 0)
   first = links[(first << 1) + 1];
  return first;
 }

/**
 * NOTE: Must be synchronized outside.
 */
 public int siblingLocation(int location, boolean forward)
  throws IllegalArgumentException
 {
  int next;
  int[] links = this.links;
  if (location <= 0 || links.length >> 1 <= location ||
      (next = links[(location << 1) + (forward ? 0 : 1)]) <= 0)
   throw new IllegalArgumentException("location: " +
              Integer.toString(location));
  if (forward)
   location = next;
  if (links[0] == location)
   next = 0;
  return next;
 }

/**
 * NOTE: Must be synchronized outside.
 */
 public Object clone()
 {
  ListStorage storage = (ListStorage)super.clone();
  storage.links = (int[])storage.links.clone();
  return storage;
 }

/**
 * NOTE: Shallow check for integrity of this object. Must be
 * synchronized outside. For debug purpose only.
 */
 public void integrityCheck()
 {
  super.integrityCheck();
  Object[] elements = this.elements;
  int[] links;
  if ((links = this.links) == null)
   throw new InternalError("links: null");
  int prev, next, count = elements.length, location, free;
  if (((count + 1) << 1) != links.length || links.length <= 0)
   throw new InternalError("links length: " +
              Integer.toString(links.length) +
              ", elements length: " + Integer.toString(count));
  if ((free = links[0]) != 0)
   if ((next = free) < 0 || elements.length < free ||
       elements[free - 1] == null)
    throw new InternalError("links[0]: " + Integer.toString(free));
    else do
    {
     if ((next = links[(location = next) << 1]) <= 0 ||
         elements.length < next || elements[next - 1] == null)
      throw new InternalError("nextLinks[" +
                 Integer.toString(location) + "]: " +
                 Integer.toString(next));
     if ((prev = links[(next << 1) + 1]) != location)
      throw new InternalError("prevLinks[" +
                 Integer.toString(next) + "]: " +
                 Integer.toString(prev));
     count--;
    } while (next != free);
  if ((free = links[1]) < 0 || elements.length < free ||
      elements.length > free && elements[free] != null ||
      count > 0 && elements.length == free)
   throw new InternalError("emptyLocation: " +
              Integer.toString(free + 1));
  for (location = ++free; count-- > 0; location = next)
   if ((next = -links[location << 1]) != free && count <= 0 ||
       count > 0 && (next == free || next <= 0 ||
       elements.length < next || elements[next - 1] != null))
    throw new InternalError("emptyNextLinks[" +
               Integer.toString(location) + "]: " +
               Integer.toString(next));
    else if ((prev = -links[(next << 1) + 1]) != location)
     throw new InternalError("emptyPrevLinks[" +
                Integer.toString(next) + "]: " +
                Integer.toString(prev));
 }

/**
 * NOTE: Must be synchronized outside.
 */
 private void writeObject(ObjectOutputStream out)
  throws IOException
 {
  out.defaultWriteObject();
  int location;
  int[] links = this.links;
  if ((location = links[0]) > 0)
  {
   int count = 0, capacity, low, high, middle;
   int[] counter = new int[capacity = (links.length >> 1) - 1];
   do
   {
    location--;
    low = 0;
    high = capacity;
    while ((middle = (low + high) >> 1) != location)
     if (location < middle)
      counter[high = middle]++;
      else low = middle + 1;
    counter[location++]++;
    count++;
   } while ((location = links[location << 1]) != links[0]);
   byte bits = 1;
   int prevReduced = 0;
   do
   {
    location--;
    int reduced = 0;
    low = 0;
    high = capacity;
    while ((middle = (low + high) >> 1) != location)
     if (location > middle)
     {
      reduced += counter[low = middle];
      low++;
     }
      else counter[high = middle]--;
    prevReduced += reduced += --counter[location++] - prevReduced;
    if (reduced < 0)
     reduced += count;
    low = 0;
    for (high = --count; high != 0; high >>= 1, low++);
    reduced <<= JavaConsts.INT_SIZE - low;
    for (high = count << (JavaConsts.INT_SIZE - low);
         low-- > 0; reduced <<= 1, high <<= 1)
     if (high < 0)
     {
      byte oldBits = bits;
      bits <<= 1;
      if (reduced >= 0)
       high = -1;
       else bits++;
      if (oldBits < 0)
      {
       out.write(bits);
       bits = 1;
      }
     }
   } while ((location = links[location << 1]) != links[0]);
   if (bits != 1)
   {
    while (bits > 0)
     bits <<= 1;
    out.write(bits << 1);
   }
  }
 }

 private void readObject(ObjectInputStream in)
  throws IOException, ClassNotFoundException
 {
  in.defaultReadObject();
  Object[] elements = this.elements;
  int count = 0, capacity, location, low, high, middle;
  int[] links = new int[(capacity = elements.length) < (-1 >>> 2) ?
   (capacity + 1) << 1 : -1 >>> 1];
  for (location = 0; location < capacity; location++)
   if (elements[location] != null)
   {
    low = 0;
    high = capacity;
    while ((middle = (low + high) >> 1) != location)
     if (location < middle)
      links[((high = middle) << 1) + 1]++;
      else low = middle + 1;
    links[(location << 1) + 1]++;
    count++;
   }
  int bits = 0, prevReduced = location = 0, oldBits;
  while (count > 0)
  {
   low = 0;
   for (high = --count; high != 0; high >>= 1, low++);
   int reduced = 0;
   for (high = count << (JavaConsts.INT_SIZE - low);
        low-- > 0; high <<= 1)
   {
    reduced <<= 1;
    if (high < 0)
    {
     if ((oldBits = (byte)(bits << 1)) == 0 &&
         (bits = in.read()) < 0)
      throw new EOFException();
     if ((byte)bits >= 0)
      high = -1;
      else reduced++;
     bits <<= 1;
     if (oldBits == 0)
      bits++;
    }
   }
   if ((reduced += prevReduced) > count)
    reduced -= count + 1;
   prevReduced = reduced++;
   int value;
   low = 0;
   high = capacity;
   while (low < high)
    if ((value = links[((middle = (low + high) >> 1) << 1) + 1]) <
        reduced)
    {
     reduced -= value;
     low = middle + 1;
    }
     else links[((high = middle) << 1) + 1]--;
   location = links[location << 1] = ++low;
  }
  for (location = links[0];
       (location = links[(count = location) << 1]) > 0;
       links[(location << 1) + 1] = count);
  links[((links[count << 1] = links[0]) << 1) + 1] = count;
  count = capacity + 1;
  location = 0;
  while (location < capacity)
   if (elements[location++] == null)
   {
    if (count <= capacity)
     links[-(links[(location << 1) + 1] = -count) << 1] = -location;
     else links[1] = location - 1;
    count = location;
   }
  if (count <= capacity)
   links[(-(links[count << 1] = -links[1] - 1) << 1) + 1] = -count;
   else links[1] = capacity;
  this.links = links;
 }
}
