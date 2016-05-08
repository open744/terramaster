/*
 * @(#) src/net/sf/ivmaidns/storage/TreeStorage.java --
 * Class for tree storage.
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
 * Class for tree storage.
 **
 * @version 2.0
 * @author Ivan Maidanski
 */
public class TreeStorage extends ObjectStorage
{

/**
 * The class version unique identifier for serialization
 * interoperability.
 **
 * @since 2.0
 */
 private static final long serialVersionUID = 538074621019444581L;

/**
 * NOTE: links must be != null, links length == 2 +
 * elements length * 4, links[0] = childLocation(0, true),
 * links[1] = emptyLocation() - 1, links[loc * 4] =
 * elements[loc - 1] != null ? childLocation(loc, true) :
 * -nextEmptyLocation(loc), links[loc * 4 + 1] =
 * elements[loc - 1] != null ? parentLocation(loc) :
 * -prevEmptyLocation(loc), links[loc * 4 - 2] = loc !=
 * childLocation(parentLocation(loc), false) ?
 * siblingLocation(loc, true) :
 * childLocation(parentLocation(loc), true) (if elements[loc - 1] !=
 * null), links[loc * 4 - 1] = loc !=
 * childLocation(parentLocation(loc), true) ?
 * siblingLocation(loc, false) :
 * childLocation(parentLocation(loc), false) (if
 * elements[loc - 1] != null). Empty locations order is not
 * serialized.
 */
 protected transient int[] links = new int[2];

 public TreeStorage() {}

/**
 * NOTE: Must be synchronized outside.
 */
 protected void minimizeCapacity()
 {
  int[] links = this.links;
  int capacity, oldCapacity;
  if (links[(capacity = oldCapacity = links.length >> 2) << 2] < 0)
  {
   while (links[--capacity << 2] < 0);
   Object[] newElements = new Object[capacity];
   int[] newLinks = new int[(capacity << 2) + 2];
   if (capacity > 0)
   {
    do
    {
     int next = -links[oldCapacity << 2];
     links[-(links[(next << 2) + 1] =
      links[(oldCapacity << 2) + 1]) << 2] = -next;
     if (--oldCapacity == links[1])
      links[1] = next - 1;
    } while (oldCapacity > capacity);
    System.arraycopy(this.elements, 0, newElements, 0, capacity);
    System.arraycopy(links, 0, newLinks, 0, (capacity << 2) + 2);
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
 * NOTE: ArrayStoreException is thrown only if value == null.
 * The effectiveness is constant. Observers notification is
 * performed. Must be synchronized outside.
 */
 public int insertAt(int prevLoc, int emptyLocation, Object value)
  throws IllegalArgumentException, ArrayStoreException
 {
  int[] links = this.links;
  int pred, next = links.length >> 2;
  if ((pred = prevLoc) != 0)
  {
   if (prevLoc < 0)
    pred = -prevLoc;
   if (pred - 1 >= next || links[pred << 2] < 0)
    throw new IllegalArgumentException("prevLoc: " +
               Integer.toString(prevLoc));
  }
  if (emptyLocation != 0 && (emptyLocation < 0 ||
      emptyLocation <= next && links[emptyLocation << 2] >= 0))
   throw new IllegalArgumentException("emptyLocation: " +
              Integer.toString(emptyLocation));
  if (value == null)
   throw new ArrayStoreException("prevLoc: " +
              Integer.toString(prevLoc) + ", emptyLocation: " +
              Integer.toString(emptyLocation) + ", value: null");
  if (emptyLocation == 0)
   emptyLocation = (pred < next && links[(pred + 1) << 2] < 0 ?
    pred : links[1]) + 1;
  Object[] elements = this.elements;
  if (next < emptyLocation)
  {
   int oldCapacity = next, last;
   if ((next += (next >> 1) + 7) <= emptyLocation)
    next = emptyLocation;
   if (next >= (-1 >>> 3))
    next = -1 >>> 1;
   Object[] newElements = new Object[last = next];
   int[] newLinks = new int[(next << 2) + 2];
   System.arraycopy(elements, 0, newElements, 0, oldCapacity);
   System.arraycopy(links, 0, newLinks, 0, (oldCapacity << 2) + 2);
   links = newLinks;
   while (--last > oldCapacity)
    links[last << 2] = (links[(last << 2) + 5] = -last) - 1;
   if (links[1] < oldCapacity)
    links[-(links[(oldCapacity << 2) + 5] =
     links[(links[1] << 2) + 5]) << 2] = -oldCapacity - 1;
   links[-(links[(links[1] << 2) + 5] = -next) << 2] =
    -links[1] - 1;
   this.elements = elements = newElements;
   this.links = links;
  }
  if ((next = -links[emptyLocation << 2]) != emptyLocation)
  {
   links[-(links[(next << 2) + 1] =
    links[(emptyLocation << 2) + 1]) << 2] = -next;
   if (links[1] == emptyLocation - 1)
    links[1] = next - 1;
  }
   else links[1] = elements.length;
  links[emptyLocation << 2] = 0;
  links[(emptyLocation << 2) + 1] =
   prevLoc > 0 ? links[(prevLoc << 2) + 1] : pred;
  if (prevLoc > 0)
   next = links[(prevLoc << 2) - 2];
   else if ((next = links[pred << 2]) > 0)
    pred = links[(next << 2) - 1];
    else next = pred = emptyLocation;
  links[(emptyLocation << 2) - 2] = next;
  links[(emptyLocation << 2) - 1] = pred;
  links[(pred << 2) - 2] = links[(next << 2) - 1] = emptyLocation;
  if (prevLoc <= 0)
   links[-prevLoc << 2] = emptyLocation;
  elements[emptyLocation - 1] = value;
  notifyObservers(0, emptyLocation, null);
  return emptyLocation;
 }

/**
 * NOTE: If value is added then it is placed to the end of tree (at
 * the first level). The effectiveness is constant (linear when
 * deleting an inner tree node). Observers notification is
 * performed. Must be synchronized outside.
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
   if (prevLoc >= (-1 >>> 3))
    prevLoc = -1 >>> 1;
   Object[] newElements = new Object[last = prevLoc];
   int[] newLinks = new int[(prevLoc << 2) + 2];
   System.arraycopy(elements, 0, newElements, 0, oldCapacity);
   System.arraycopy(links, 0, newLinks, 0, (oldCapacity << 2) + 2);
   links = newLinks;
   while (--last > oldCapacity)
    links[last << 2] = (links[(last << 2) + 5] = -last) - 1;
   if (links[1] < oldCapacity)
    links[-(links[(oldCapacity << 2) + 5] =
     links[(links[1] << 2) + 5]) << 2] = -oldCapacity - 1;
   links[-(links[(links[1] << 2) + 5] = -prevLoc) << 2] =
    -links[1] - 1;
   this.elements = elements = newElements;
   this.links = links;
  }
  Object oldValue = elements[location - 1];
  if (value != null)
  {
   if (oldValue == null)
   {
    if ((prevLoc = -links[location << 2]) != location)
    {
     links[-(links[(prevLoc << 2) + 1] =
      links[(location << 2) + 1]) << 2] = -prevLoc;
     if (links[1] == location - 1)
      links[1] = prevLoc - 1;
    }
     else links[1] = elements.length;
    links[location << 2] = links[(location << 2) + 1] = 0;
    if ((prevLoc = links[0]) > 0)
     last = links[(location << 2) - 1] =
      links[((links[(location << 2) - 2] = prevLoc) << 2) - 1];
     else links[0] = last = prevLoc = location;
    links[(last << 2) - 2] = links[(prevLoc << 2) - 1] = location;
   }
   elements[location - 1] = value;
   notifyObservers(0, location, oldValue);
  }
   else if (oldValue != null)
    do
    {
     int removed = location;
     while ((prevLoc = links[removed << 2]) > 0)
      removed = links[(prevLoc << 2) - 1];
     int next = links[(removed << 2) - 2];
     prevLoc = links[(removed << 2) - 1];
     links[((links[(prevLoc << 2) - 2] = next) << 2) - 1] = prevLoc;
     if (links[(last = links[(removed << 2) + 1]) << 2] == removed)
     {
      links[last << 2] = removed != next ? next : 0;
      prevLoc = -last;
     }
     value = elements[removed - 1];
     elements[removed - 1] = null;
     if ((next = links[1] + 1) <= elements.length)
     {
      links[removed << 2] = -next;
      last = -(links[(removed << 2) + 1] = links[(next << 2) + 1]);
     }
      else last = next = removed;
     links[last << 2] = links[(next << 2) + 1] = -removed;
     links[1] = removed - 1;
     notifyObservers(prevLoc, -removed, value);
    } while ((elements = this.elements).length >= location &&
             (links = this.links)[location << 2] >= 0);
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
  if (parentLocation < 0 || (links.length >> 2) < parentLocation ||
      (first = links[parentLocation << 2]) < 0)
   throw new IllegalArgumentException("location: " +
              Integer.toString(parentLocation));
  if (!forward && first > 0)
   first = links[(first << 2) - 1];
  return first;
 }

/**
 * NOTE: Must be synchronized outside.
 */
 public int siblingLocation(int location, boolean forward)
  throws IllegalArgumentException
 {
  int next, parent;
  int[] links = this.links;
  if (location <= 0 || (links.length >> 2) < location ||
      (parent = links[(next = location << 2) + 1]) < 0)
   throw new IllegalArgumentException("location: " +
              Integer.toString(location));
  if (forward)
   next--;
  next = links[next - 1];
  if (forward)
   location = next;
  if (links[parent << 2] == location)
   next = 0;
  return next;
 }

 public int parentLocation(int location)
  throws IllegalArgumentException
 {
  int parent;
  int[] links = this.links;
  if (location <= 0 || (links.length >> 2) < location ||
      (parent = links[(location << 2) + 1]) < 0)
   throw new IllegalArgumentException("location: " +
              Integer.toString(location));
  return parent;
 }

/**
 * NOTE: Must be synchronized outside.
 */
 public Object clone()
 {
  TreeStorage storage = (TreeStorage)super.clone();
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
  int prev, next, count = elements.length;
  int location = 0, parent, free;
  if (links.length >> 2 != count || (links.length & 3) != 2)
   throw new InternalError("links length: " +
              Integer.toString(links.length) +
              ", elements length: " + Integer.toString(count));
  do
  {
   if ((free = next = links[location << 2]) == 0)
    while (location > 0 && (next = links[(location << 2) - 2]) ==
           links[(parent = links[(location << 2) + 1]) << 2])
    {
     if ((prev = links[(next << 2) - 1]) != location)
      throw new InternalError("lastSiblingLinks[" +
                 Integer.toString(next) + "]: " +
                 Integer.toString(prev));
     location = parent;
     next = 0;
    }
   if ((location | next) == 0)
    break;
   if (count <= 0 || next <= 0 || elements.length < next ||
       elements[next - 1] == null)
    throw new InternalError((free > 0 ? "firstChildLinks[" :
               "nextSiblingLinks[") + Integer.toString(location) +
               "]: " + Integer.toString(next));
   if ((parent = links[(next << 2) + 1]) !=
       (free > 0 ? location : links[(location << 2) + 1]))
    throw new InternalError("parentLinks[" +
               Integer.toString(next) + "]: " +
               Integer.toString(parent));
   if (free <= 0 && (prev = links[(next << 2) - 1]) != location)
    throw new InternalError("prevSiblingLinks[" +
               Integer.toString(next) + "]: " +
               Integer.toString(prev));
   location = next;
   count--;
  } while (true);
  if ((free = links[1]) < 0 || elements.length < free ||
      elements.length > free && elements[free] != null ||
      count > 0 && elements.length == free)
   throw new InternalError("emptyLocation: " +
              Integer.toString(free + 1));
  for (location = ++free; count-- > 0; location = next)
   if ((next = -links[location << 2]) != free && count <= 0 ||
       count > 0 && (next == free || next <= 0 ||
       elements.length < next || elements[next - 1] != null))
    throw new InternalError("emptyNextLinks[" +
               Integer.toString(location) + "]: " +
               Integer.toString(next));
    else if ((prev = -links[(next << 2) + 1]) != location)
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
   int count = 0, capacity, low, high, middle, parent, prev;
   int[] counter = new int[capacity = links.length >> 2];
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
    if ((location = links[(prev = location) << 2]) <= 0)
     while ((location = links[(prev << 2) - 2]) ==
            links[(parent = links[(prev << 2) + 1]) << 2] &&
            (location = prev = parent) > 0);
    count++;
   } while (location > 0);
   int total = count;
   byte bits = 1, oldBits;
   int prevReduced = 0;
   location = links[0];
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
      oldBits = bits;
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
    if ((location = links[(reduced = location) << 2]) <= 0)
     while ((location = links[(reduced << 2) - 2]) ==
            links[(low = links[(reduced << 2) + 1]) << 2] &&
            (location = reduced = low) > 0);
   } while (location > 0);
   if (bits != 1)
   {
    while (bits > 0)
     bits <<= 1;
    out.write(bits << 1);
    bits = 1;
   }
   location = links[0];
   while (--total > 0)
   {
    if ((location = links[(parent = prev = location) << 2]) <= 0)
     do
     {
      oldBits = bits;
      bits <<= 1;
      if (oldBits < 0)
      {
       out.write(bits);
       bits = 1;
      }
     } while ((location = links[(prev << 2) - 2]) == links[(parent =
              links[(prev << 2) + 1]) << 2] && (prev = parent) > 0);
    if (parent > 0)
    {
     oldBits = bits;
     bits <<= 1;
     bits++;
     if (oldBits < 0)
     {
      out.write(bits);
      bits = 1;
     }
    }
   }
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
  int[] links = new int[(capacity = elements.length) < (-1 >>> 3) ?
   (capacity << 2) + 2 : -1 >>> 1];
  for (location = 0; location < capacity; location++)
   if (elements[location] != null)
   {
    low = 0;
    high = capacity;
    while ((middle = (low + high) >> 1) != location)
     if (location < middle)
      links[((high = middle) << 2) + 1]++;
      else low = middle + 1;
    links[(location << 2) + 1]++;
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
    if ((value = links[((middle = (low + high) >> 1) << 2) + 1]) <
        reduced)
    {
     reduced -= value;
     low = middle + 1;
    }
     else links[((high = middle) << 2) + 1]--;
   location = links[location << 2] = ++low;
  }
  middle = location = bits = 0;
  while ((high = links[location << 2]) > 0)
  {
   oldBits = 1;
   if (middle > 0 && (oldBits = (byte)(bits << 1)) == 0 &&
       (bits = in.read()) < 0)
    throw new EOFException();
   if (middle <= 0 || (byte)bits < 0)
   {
    links[location << 2] = 0;
    if ((high = links[(links[((location = high) << 2) + 1] =
        middle) << 2]) > 0)
     low = links[(location << 2) - 1] =
      links[((links[(location << 2) - 2] = high) << 2) - 1];
     else links[middle << 2] = low = high = location;
    if (middle > 0)
     bits <<= 1;
    middle = links[(low << 2) - 2] =
     links[(high << 2) - 1] = location;
   }
    else
    {
     middle = links[(middle << 2) + 1];
     bits <<= 1;
    }
   if (oldBits == 0)
    bits++;
  }
  count = capacity + 1;
  location = 0;
  while (location < capacity)
   if (elements[location++] == null)
   {
    if (count <= capacity)
     links[-(links[(location << 2) + 1] = -count) << 2] = -location;
     else links[1] = location - 1;
    count = location;
   }
  if (count <= capacity)
   links[(-(links[count << 2] = -links[1] - 1) << 2) + 1] = -count;
   else links[1] = capacity;
  this.links = links;
 }
}
