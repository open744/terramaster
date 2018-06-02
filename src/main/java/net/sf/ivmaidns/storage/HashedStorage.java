/*
 * @(#) src/net/sf/ivmaidns/storage/HashedStorage.java --
 * Class for storage of hashed elements.
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

import java.io.IOException;
import java.io.ObjectInputStream;

import net.sf.ivmaidns.util.JavaConsts;

/**
 * Class for storage of hashed elements.
 **
 * @version 2.0
 * @author Ivan Maidanski
 */
public class HashedStorage extends ObjectStorage
{

/**
 * The class version unique identifier for serialization
 * interoperability.
 **
 * @since 2.0
 */
 private static final long serialVersionUID = 4893448275881941455L;

/**
 * NOTE: links must be != null, links length == (elements length +
 * 1) * 2 + (2 leftShift ~links[0]), links[0] = seedAndShift,
 * links[1] = emptyLocation() - 1, links[loc * 2] =
 * elements[loc - 1] != null ? elements[loc - 1] hashCode() :
 * -nextEmptyLocation(loc), links[loc * 2 + 1] =
 * elements[loc - 1] != null ? chainNext(loc) :
 * -prevEmptyLocation(loc), links[links length - rootIndex - 1] =
 * chainRoots(rootIndex), where rootIndex = ((links[loc * 2] ^
 * links[0]) * JavaConsts GOLD_MEDIAN) >>> links[0]. All values in
 * each chain are ordered ascending first by their (signed) hash,
 * and then by their locations. seed and order of empty locations
 * are not serialized.
 */
 protected transient int[] links;

/**
 * NOTE: size must be the number of elements in the storage.
 */
 protected transient int size;

 public HashedStorage()
 {
  (this.links = new int[4])[0] = JavaConsts.INT_SIZE - 1;
 }

/**
 * NOTE: initialValues must be != null, initialValues[index] may be
 * == null (ignored) for any index. capacity is set to initialValues
 * length, each value from initialValues is added to storage. The
 * effectiveness is nearly linear.
 */
 public HashedStorage(Object[] initialValues)
  throws NullPointerException
 {
  int count = initialValues.length;
  int capacity = count - 1, free = JavaConsts.INT_SIZE;
  do
  {
   free--;
  } while ((capacity >>= 1) > 0);
  int[] links = new int[count <= (-1 >>> 3) + 1 ?
   ((1 << ~free) + count + 1) << 1 : -1 >>> 1];
  links[0] = free;
  if (count > 0)
  {
   Object[] elements = new Object[count];
   randomize(links, count);
   free = (capacity = count) + 1;
   int location = 0, predAddr;
   Object value;
   while (location < capacity)
    if ((value = initialValues[location++]) != null)
    {
     elements[location - 1] = value;
     links[(location << 1) + 1] = links[predAddr =
      addressOf(links[location << 1] = value.hashCode(),
      location, links)];
     links[predAddr] = location;
    }
     else
     {
      if (free <= capacity)
       links[-(links[(location << 1) + 1] = -free) << 1] =
        -location;
       else links[1] = location - 1;
      free = location;
      count--;
     }
   if (free <= capacity)
    links[(-(links[free << 1] = -links[1] - 1) << 1) + 1] = -free;
    else links[1] = capacity;
   this.size = count;
   this.elements = elements;
  }
  this.links = links;
 }

/**
 * NOTE: Result is the count of elements. Result >= 0.
 */
 public final int size()
 {
  return this.size;
 }

/**
 * NOTE: Must be synchronized outside.
 */
 protected void minimizeCapacity()
 {
  int[] links = this.links;
  int capacity, oldCapacity;
  if (links[((capacity = oldCapacity =
      elements.length) << 1) + 1] < 0)
  {
   while (links[(--capacity << 1) + 1] < 0);
   int seed = links[0], count = 2 << ~seed;
   Object[] newElements = new Object[capacity];
   if (capacity > 2)
    while ((count >>= 1) >= capacity)
     seed++;
    else while ((count >>= 1) > 1)
     seed++;
   int[] newLinks = new int[((1 << ~seed) + capacity + 1) << 1];
   if ((count = this.size) >= capacity)
    links[1] = capacity;
    else while (capacity < oldCapacity)
    {
     int next = -links[oldCapacity << 1];
     links[-(links[(next << 1) + 1] =
      links[(oldCapacity << 1) + 1]) << 1] = -next;
     if (--oldCapacity == links[1])
      links[1] = next - 1;
    }
   System.arraycopy(this.elements, 0, newElements, 0, capacity);
   System.arraycopy(links, 1, newLinks, 1, (capacity << 1) + 1);
   if (count > 0 && links[0] == seed)
   {
    count = 2 << ~seed;
    System.arraycopy(links, links.length - count,
     newLinks, newLinks.length - count, count);
    count = 0;
   }
   (links = newLinks)[0] = seed;
   if (count > 0)
   {
    randomize(links, count);
    capacity = 0;
    do
    {
     if (links[(++capacity << 1) + 1] >= 0)
     {
      links[(capacity << 1) + 1] = links[oldCapacity =
       addressOf(links[capacity << 1], capacity, links)];
      links[oldCapacity] = capacity;
      if (--count <= 0)
       break;
     }
    } while (true);
   }
   this.elements = newElements;
   this.links = links;
  }
 }

/**
 * NOTE: The effectiveness is nearly linear. Hash values are not
 * recalculated. Must be synchronized outside.
 */
 public void rehash()
 {
  int[] links;
  int predAddr, location, count;
  if ((count = this.size) > 0)
  {
   links = this.links;
   links[predAddr = links.length - (location = 2 << ~links[0])] = 0;
   links[predAddr + 1] = 0;
   int block = 1;
   while ((block <<= 1) < location)
    System.arraycopy(links, predAddr,
     links, predAddr + block, block);
   randomize(links, count);
   location = 0;
   do
   {
    while (links[(++location << 1) + 1] < 0);
    links[(location << 1) + 1] = links[predAddr =
     addressOf(links[location << 1], location, links)];
    links[predAddr] = location;
   } while (--count > 0);
  }
 }

/**
 * NOTE: Called internally only just before rehashing or if size
 * == 0. NullPointerException and ArrayIndexOutOfBoundsException are
 * thrown only if parameters are bad. Must be synchronized outside.
 */
 protected static final void randomize(int[] links, int count)
  throws NullPointerException, ArrayIndexOutOfBoundsException
 {
  int seed = links[0], value =
   ((seed >>> (JavaConsts.INT_SIZE - 7)) ^ ((seed - count) << 7) ^
   links.length) * JavaConsts.GOLD_MEDIAN;
  seed ^= count * JavaConsts.INT_SIZE;
  if ((count = value & 1 | (((value >>> 1) %
      JavaConsts.INT_SIZE) << 1)) >= JavaConsts.INT_SIZE)
   count -= JavaConsts.INT_SIZE;
  links[0] = seed + value - count;
 }

/**
 * NOTE: location must be > 0. Result > 0. NullPointerException and
 * ArrayIndexOutOfBoundsException are thrown only if parameters are
 * bad. Must be synchronized outside.
 */
 protected static final int addressOf(int hash, int location,
            int[] links)
  throws NullPointerException, ArrayIndexOutOfBoundsException
 {
  int predAddr = links[0], index;
  predAddr = links.length - (((predAddr ^
   hash) * JavaConsts.GOLD_MEDIAN) >>> predAddr) - 1;
  while ((index = links[predAddr]) > 0 && links[index << 1] < hash)
   predAddr = (index << 1) + 1;
  while (index > 0 && index < location && links[index << 1] == hash)
   index = links[predAddr = (index << 1) + 1];
  return predAddr;
 }

 public final int emptyLocation()
 {
  return this.links[1] + 1;
 }

/**
 * NOTE: No equals checking is performed here. The effectiveness is
 * nearly constant (may be linear in the worst case). Observers
 * notification is performed. Must be synchronized outside.
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
   int oldCapacity = prevLoc, count;
   if ((prevLoc += (prevLoc >> 1) + 7) <= location)
    prevLoc = location;
   if (prevLoc >= (-1 >>> 3))
    prevLoc = -1 >>> 1;
   Object[] newElements = new Object[prevLoc];
   last = links[0];
   if ((count = 2 << ~last) < prevLoc)
    do
    {
     last--;
    } while ((count <<= 1) < prevLoc);
   int[] newLinks = new int[((1 << ~last) + prevLoc + 1) << 1];
   System.arraycopy(elements, 0, newElements, 0, oldCapacity);
   System.arraycopy(links, 1, newLinks, 1, (oldCapacity << 1) + 1);
   if ((count = this.size) > 0 && links[0] == last)
   {
    count = 2 << ~last;
    System.arraycopy(links, links.length - count,
     newLinks, newLinks.length - count, count);
    count = 0;
   }
   (links = newLinks)[0] = last;
   last = prevLoc;
   while (--last > oldCapacity)
    links[last << 1] = (links[(last << 1) + 3] = -last) - 1;
   if (links[1] < oldCapacity)
    links[-(links[(oldCapacity << 1) + 3] =
     links[(links[1] << 1) + 3]) << 1] = -oldCapacity - 1;
   links[-(links[(links[1] << 1) + 3] = -prevLoc) << 1] =
    -links[1] - 1;
   if (count > 0)
   {
    randomize(links, count);
    last = 0;
    do
    {
     if (links[(++last << 1) + 1] >= 0)
     {
      links[(last << 1) + 1] = links[oldCapacity =
       addressOf(links[last << 1], last, links)];
      links[oldCapacity] = last;
      if (--count <= 0)
       break;
     }
    } while (true);
   }
   this.elements = elements = newElements;
   this.links = links;
  }
  Object oldValue;
  if ((oldValue = elements[location - 1]) != null || value != null)
  {
   int hash = 0, predAddr;
   prevLoc = location;
   if (value != null)
   {
    if (this.size <= 0)
     randomize(links, location);
    prevLoc = addressOf(hash = value.hashCode(), location, links);
   }
   if (oldValue == null)
   {
    if ((last = -links[location << 1]) != location)
    {
     links[-(links[(last << 1) + 1] =
      links[(location << 1) + 1]) << 1] = -last;
     if (links[1] == location - 1)
      links[1] = last - 1;
    }
     else links[1] = elements.length;
    this.size++;
   }
    else if (prevLoc >> 1 != location)
    {
     predAddr = links[0];
     predAddr = links.length - (((predAddr ^ links[location << 1]) *
      JavaConsts.GOLD_MEDIAN) >>> predAddr) - 1;
     while ((last = links[predAddr]) != location)
      predAddr = (last << 1) + 1;
     links[predAddr] = links[(location << 1) + 1];
    }
   links[location << 1] = hash;
   if ((elements[location - 1] = value) == null)
   {
    this.size--;
    if ((last = links[1] + 1) <= elements.length)
    {
     links[location << 1] = -last;
     predAddr =
      -(links[(location << 1) + 1] = links[(last << 1) + 1]);
    }
     else predAddr = last = location;
    links[predAddr << 1] = links[(last << 1) + 1] = -location;
    links[1] = location - 1;
    if (!hasObservers())
     return oldValue;
    while (links[(--prevLoc << 1) + 1] < 0);
    location = -location;
   }
    else
    {
     if (prevLoc >> 1 != location)
     {
      links[(location << 1) + 1] = links[prevLoc];
      links[prevLoc] = location;
     }
     prevLoc = 0;
    }
   notifyObservers(prevLoc, location, oldValue);
  }
  return oldValue;
 }

/**
 * NOTE: value is compared against elements. The effectiveness is
 * nearly constant (may be linear in the worst case). Must be
 * synchronized outside.
 */
 public int locationOf(Object value, int prevLocation,
         boolean forward)
  throws IllegalArgumentException
 {
  Object[] elements = this.elements;
  int[] links = this.links;
  if (prevLocation != 0 && (prevLocation < 0 ||
      prevLocation > elements.length ||
      links[(prevLocation << 1) + 1] < 0))
   throw new IllegalArgumentException("location: " +
              Integer.toString(prevLocation));
  int foundLoc = 0;
  if (value != null &&
      (forward ? elements.length : 1) != prevLocation)
  {
   int hash = value.hashCode(), predAddr = links[0];
   predAddr = links.length - (((predAddr ^
    hash) * JavaConsts.GOLD_MEDIAN) >>> predAddr) - 1;
   while ((predAddr = links[predAddr]) > 0 &&
          links[predAddr << 1] < hash)
    predAddr = (predAddr << 1) + 1;
   if (forward)
   {
    while (predAddr <= prevLocation && predAddr > 0 &&
           links[predAddr << 1] == hash)
     predAddr = links[(predAddr << 1) + 1];
    while (predAddr > 0 && links[predAddr << 1] == hash)
     if (value.equals(elements[predAddr - 1]))
     {
      foundLoc = predAddr;
      break;
     }
      else predAddr = links[(predAddr << 1) + 1];
   }
    else
    {
     if (prevLocation <= 0)
      prevLocation = elements.length + 1;
     while (predAddr > 0 && links[predAddr << 1] == hash &&
            predAddr < prevLocation)
     {
      if (value.equals(elements[predAddr - 1]))
       foundLoc = predAddr;
      predAddr = links[(predAddr << 1) + 1];
     }
    }
  }
  return foundLoc;
 }

/**
 * NOTE: Must be synchronized outside.
 */
 public Object clone()
 {
  HashedStorage storage = (HashedStorage)super.clone();
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
  int linksLength;
  if ((links = this.links) == null)
   throw new InternalError("links: null");
  if ((linksLength = links.length) <= 0)
   throw new InternalError("links length: 0");
  int prev, next, count = elements.length;
  int location, free = links[0], hash;
  if ((prev = free & 1 | (((free >>> 1) %
      JavaConsts.INT_SIZE) << 1)) >= JavaConsts.INT_SIZE)
   prev -= JavaConsts.INT_SIZE;
  if (prev < 3 || (location = 2 << ~free) +
      ((count + 1) << 1) != linksLength)
   throw new InternalError("links length: " +
              Integer.toString(linksLength) +
              ", elements length: " + Integer.toString(count) +
              ", seed: 0x" + Integer.toHexString(free));
  while ((prev = 0) < location)
   for (next = links[linksLength - (location--)]; next != 0;
        next = links[((prev = next) << 1) + 1], count--)
    if (count <= 0 || next < 0 || next > elements.length ||
        elements[next - 1] == null)
     throw new InternalError((prev <= 0 ? "rootLinks[" +
                Integer.toString(location) : "nextLinks[" +
                Integer.toString(prev)) + "]: " +
                Integer.toString(next));
     else if ((((hash = links[next << 1]) ^
              free) * JavaConsts.GOLD_MEDIAN) >>> free != location)
      throw new InternalError("Root mismatch for: " +
                 Integer.toString(next));
      else if (prev != 0 && links[prev << 1] >= hash &&
               (prev > next || links[prev << 1] > hash))
       throw new InternalError("Bad chain order at: " +
                  Integer.toString(next));
  if ((free = this.size) != elements.length - count)
   throw new InternalError("size: " + Integer.toString(free));
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

 private void readObject(ObjectInputStream in)
  throws IOException, ClassNotFoundException
 {
  in.defaultReadObject();
  Object[] elements = this.elements;
  int count = elements.length, capacity = count - 1;
  int free = JavaConsts.INT_SIZE;
  do
  {
   free--;
  } while ((capacity >>= 1) > 0);
  int[] links = new int[count <= (-1 >>> 3) + 1 ?
   ((1 << ~free) + count + 1) << 1 : -1 >>> 1];
  links[0] = free;
  if (count > 0)
  {
   randomize(links, count);
   free = (capacity = count) + 1;
   int location = 0, predAddr;
   Object value;
   while (location < capacity)
    if ((value = elements[location++]) != null)
    {
     links[(location << 1) + 1] = links[predAddr =
      addressOf(links[location << 1] = value.hashCode(),
      location, links)];
     links[predAddr] = location;
    }
     else
     {
      if (free <= capacity)
       links[-(links[(location << 1) + 1] = -free) << 1] =
        -location;
       else links[1] = location - 1;
      free = location;
      count--;
     }
   if (free <= capacity)
    links[(-(links[free << 1] = -links[1] - 1) << 1) + 1] = -free;
    else links[1] = capacity;
   this.size = count;
  }
  this.links = links;
 }
}
