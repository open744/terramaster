/*
 * @(#) src/net/sf/ivmaidns/storage/SortedStorage.java --
 * Class for ascending-sorted storage.
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

import java.io.InvalidObjectException;
import java.io.IOException;
import java.io.ObjectInputStream;

import net.sf.ivmaidns.util.GComparator;

/**
 * Class for ascending-sorted storage.
 **
 * Storage elements are placed during insertAt/setAt/readObject
 * operations into ascending order according to the supplied
 * comparator. If two (compared) elements are equal or uncomparable
 * then they are ordered by their locations. Add, remove and search
 * effectiveness is logarithmic.
 **
 * @version 2.0
 * @author Ivan Maidanski
 */
public class SortedStorage extends ObjectStorage
{

/**
 * The class version unique identifier for serialization
 * interoperability.
 **
 * @since 2.0
 */
 private static final long serialVersionUID = 9156069725514802205L;

/**
 * NOTE: links must be != null, links length == 2 +
 * elements length * 3, links[0] = rootAddress, links[1] =
 * emptyLocation() - 1, links[loc * 2] = elements[loc - 1] != null ?
 * leftAddress(loc) : -nextEmptyLocation(loc), links[loc * 2 + 1] =
 * elements[loc - 1] != null ? rightAddress(loc) :
 * -prevEmptyLocation(loc), links[links length - loc] =
 * upperAddress(loc) (if elements[loc - 1] != null). Equal (or
 * uncomparable) elements are ordered ascending by their locations.
 * Tree node balance is held in the least bit of each
 * leftAddress/rightAddress. The order of empty locations is not
 * serialized.
 */
 protected transient int[] links = new int[2];

/**
 * NOTE: comparator must be != null. This field is serialized and
 * cloned (shallow).
 */
 protected final GComparator comparator;

/**
 * NOTE: Used internally by setAt/locationOf/findLessGreater
 * operations to speed up addressOf function. approxLoc must be
 * either 0 or a non-empty location. This field is not serialized
 * but cloned.
 */
 protected transient int approxLoc;

/**
 * NOTE: The default comparator is used.
 */
 public SortedStorage()
 {
  this.comparator = GComparator.INSTANCE;
 }

/**
 * NOTE: comparator must be != null.
 */
 public SortedStorage(GComparator comparator)
  throws NullPointerException
 {
  comparator.greater(comparator, comparator);
  this.comparator = comparator;
 }

/**
 * NOTE: Result != null.
 */
 public final GComparator comparator()
 {
  return this.comparator;
 }

/**
 * NOTE: Must be synchronized outside.
 */
 protected void minimizeCapacity()
 {
  int[] links = this.links;
  int capacity, oldCapacity;
  if (links[(capacity = oldCapacity = links.length / 3) << 1] < 0)
  {
   while (links[--capacity << 1] < 0);
   Object[] newElements = new Object[capacity];
   int[] newLinks = new int[capacity * 3 + 2];
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
    System.arraycopy(links, links.length - capacity,
     newLinks, newLinks.length - capacity, capacity);
   }
   this.elements = newElements;
   this.links = newLinks;
  }
 }

/**
 * NOTE: Find place for the child link to the value (value may be
 * == null, location may be of any int value, abs(approxLoc) must be
 * either 0 or a non-empty location, comparator must be != null).
 * Result >= 0. If 0 >= location or location > elements length then
 * links[result] == 0. NullPointerException and
 * ArrayIndexOutOfBoundsException are thrown only if parameters are
 * bad. Must be synchronized outside.
 */
 protected static final int addressOf(Object value, int location,
            Object[] elements, int[] links, int approxLoc,
            GComparator comparator)
  throws NullPointerException, ArrayIndexOutOfBoundsException
 {
  int address = 0;
  Object elementValue;
  if (approxLoc != 0)
  {
   if (comparator.greater(value, elementValue =
       elements[(approxLoc > 0 ? approxLoc : -approxLoc) - 1]))
    address = 1;
    else if (!comparator.greater(elementValue, value))
     address = -1;
   if (((elements.length + 1 - location) | location) < 0)
    if (approxLoc > 0)
    {
     if (address < 0 || ((address - 1) ^ location) >= 0)
      return (approxLoc << 1) + (location < 0 ? 1 : 0);
    }
     else if (address >= 0 && ((address - 1) ^ location) < 0)
      return (-approxLoc << 1) + (location < 0 ? 0 : 1);
   if (approxLoc < 0)
    approxLoc = -approxLoc;
   int linksLength = links.length;
   if (address < 0)
    if (location > approxLoc)
     address = 1;
     else if (location == approxLoc)
      return links[linksLength - location];
      else address = 0;
   if (address != 0)
   {
    address = approxLoc << 1;
    while ((address = links[linksLength - (address >> 1)]) > 0)
     if ((address & 1) == 0)
     {
      if (!comparator.greater(value, elementValue =
          elements[(address >>= 1) - 1]))
       if (location < address ||
           comparator.greater(elementValue, value))
        break;
        else if (location == address)
         return links[linksLength - location];
      approxLoc = address;
      address <<= 1;
     }
    address = 1;
   }
    else
    {
     address = approxLoc << 1;
     while ((address = links[linksLength - (address >> 1)]) > 0)
      if ((address & 1) != 0)
      {
       if (!comparator.greater(elementValue =
           elements[(address >>= 1) - 1], value))
        if (location > address ||
            comparator.greater(value, elementValue))
         break;
         else if (location == address)
          return links[linksLength - location];
       approxLoc = address;
       address <<= 1;
      }
     address = 0;
    }
  }
  while ((approxLoc = links[address =
         (approxLoc << 1) + (address & 1)] >> 1) > 0)
  {
   elementValue = elements[approxLoc - 1];
   if ((address & 1) != 0)
   {
    if (!comparator.greater(value, elementValue))
     if (location < approxLoc ||
         comparator.greater(elementValue, value))
      address = 0;
      else if (location == approxLoc)
       break;
   }
    else if (!comparator.greater(elementValue, value))
     if (location > approxLoc ||
         comparator.greater(value, elementValue))
      address = 1;
      else if (location == approxLoc)
       break;
  }
  return address;
 }

/**
 * NOTE: (address / 2) must be either 0 or a non-empty location. The
 * follow direction is specified in the least bit of address (this
 * direction bit is not preserved in the resulting address). If
 * resulting address is 0 then tree end is reached.
 * NullPointerException and ArrayIndexOutOfBoundsException are
 * thrown only if parameters are bad. Must be synchronized outside.
 */
 protected static final int followLinks(int[] links, int address)
  throws NullPointerException, ArrayIndexOutOfBoundsException
 {
  int next;
  if ((next = links[address > 1 ? address : 0]) > 0)
   if ((address & 1) != 0)
    while ((address = links[next & ~1]) > 0)
     next = address;
    else while ((address = links[next | 1]) > 0)
     next = address;
   else if (address > 1)
   {
    int linksLength = links.length;
    if (((next = address) & 1) == 0)
     while (((next = links[linksLength - (next >> 1)]) & 1) == 0 &&
            next > 0);
     else while (((next =
                 links[linksLength - (next >> 1)]) & 1) != 0);
   }
  return next;
 }

/**
 * NOTE: If emptyLocation > 0 then another leaf is added to the tree
 * else one leaf (which is refered by non-zero address) is removed.
 * The tree remains balanced. NullPointerException and
 * ArrayIndexOutOfBoundsException are thrown only if parameters are
 * bad. Must be synchronized outside.
 */
 protected static final void changeLinks(int[] links, int address,
            int emptyLocation)
  throws NullPointerException, ArrayIndexOutOfBoundsException
 {
  int child, parent, leaf, linksLength = links.length;
  if (emptyLocation > 0)
  {
   links[(emptyLocation << 1) + 1] = links[emptyLocation << 1] = 0;
   links[links[linksLength - emptyLocation] = address] =
    emptyLocation << 1;
  }
   else
   {
    parent = address =
     links[linksLength - ((emptyLocation = address | 1) >> 1)];
    if ((child = links[emptyLocation] & ~1) <= 0)
     child = links[emptyLocation - 1] & ~1;
     else if (links[emptyLocation - 1] > 0)
     {
      while ((address = links[child]) > 0)
       child = address & ~1;
      links[address = links[linksLength - (child >> 1)]] &= 1;
      if ((leaf = links[child + 1] & ~1) > 0)
       links[linksLength - ((links[address] |= leaf) >> 1)] =
        address;
      links[linksLength - ((links[child] =
       links[emptyLocation - 1]) >> 1)] = child;
      if ((leaf = (links[child + 1] =
          links[emptyLocation]) >> 1) > 0)
       links[linksLength - leaf] = child + 1;
      if (address == emptyLocation)
       address = child + 1;
     }
    links[parent] &= 1;
    if (child > 0)
     links[links[linksLength - (child >> 1)] = parent] |= child;
    emptyLocation = 0;
   }
  while (address > 0)
  {
   if (emptyLocation <= 0)
    address ^= 1;
   parent = links[linksLength - (address >> 1)];
   if (((child = links[address]) & 1) != 0)
   {
    if ((links[child ^= address & 1] & 1) != 0)
    {
     int middle = child;
     if ((leaf = (links[middle] =
         links[child = links[child] ^ (child & 1)]) >> 1) > 0)
      links[linksLength - leaf] = middle;
     links[linksLength - ((links[child] =
      middle & ~1) >> 1)] = child;
     if ((links[child ^= 1] & 1) != 0)
     {
      links[middle ^ 1]++;
      links[child]--;
     }
      else if ((links[middle] & 1) != 0)
      {
       links[address ^ 1]++;
       links[middle]--;
      }
    }
     else if ((links[child ^ 1] & 1) != 0)
      links[child ^ 1]--;
      else links[child]++;
    if ((leaf = (links[address] = links[child]) >> 1) > 0)
     links[linksLength - leaf] = address;
    links[linksLength - ((links[child] =
     address & ~1) >> 1)] = child;
    links[links[linksLength - (child >> 1)] = parent] &= 1;
    links[parent] |= child & ~1;
    if ((links[address] & 1) != 0)
    {
     links[child]++;
     break;
    }
    if (emptyLocation > 0)
     break;
   }
    else if ((links[address ^ 1] & 1) != 0)
    {
     links[address ^ 1]--;
     if (emptyLocation > 0)
      break;
    }
     else if (child > 0)
     {
      links[address]++;
      if (emptyLocation <= 0)
       break;
     }
   address = parent;
  }
  address = 0;
 }

 public final int emptyLocation()
 {
  return this.links[1] + 1;
 }

/**
 * NOTE: prevLoc must be >= 0, value must be comparable and
 * insertion of this value must not destroy the order of elements in
 * this storage (according to the semantics of sorted collection),
 * otherwise ArrayStoreException is thrown. The effectiveness is
 * constant. Observers notification is performed. Must be
 * synchronized outside.
 */
 public int insertAt(int prevLoc, int emptyLocation, Object value)
  throws IllegalArgumentException, ArrayStoreException
 {
  Object[] elements = this.elements;
  int[] links = this.links;
  int minLoc = 0, maxLoc, nextLoc, firstLoc;
  int capacity = elements.length;
  if (prevLoc != 0)
  {
   if ((nextLoc = prevLoc) < 0)
    nextLoc = -prevLoc;
   if (nextLoc - 1 >= capacity || links[nextLoc << 1] < 0)
    throw new IllegalArgumentException("prevLoc: " +
               Integer.toString(prevLoc));
  }
  if (emptyLocation != 0 && (emptyLocation < 0 ||
      emptyLocation <= capacity && links[emptyLocation << 1] >= 0))
   throw new IllegalArgumentException("emptyLocation: " +
              Integer.toString(emptyLocation));
  if (value != null && prevLoc >= 0)
  {
   GComparator comparator = this.comparator;
   Object elementValue;
   if (prevLoc > 0 && !comparator.greater(value,
       elementValue = elements[prevLoc - 1]))
    minLoc = comparator.greater(elementValue, value) ?
     -1 >>> 1 : prevLoc;
   if ((maxLoc = emptyLocation > 0 ?
       emptyLocation : -1 >>> 1) > minLoc)
   {
    if ((nextLoc = followLinks(links,
        (prevLoc << 1) + 1) >> 1) > 0 &&
        !comparator.greater(elementValue =
        elements[nextLoc - 1], value))
     maxLoc = emptyLocation >= nextLoc ||
      comparator.greater(value, elementValue) ? 0 : nextLoc - 1;
    if (minLoc < maxLoc)
    {
     if (emptyLocation == 0 &&
         (firstLoc = emptyLocation = links[1] + 1) <= capacity)
      if (minLoc >= capacity)
       emptyLocation = minLoc + 1;
       else if (prevLoc < capacity && links[(prevLoc + 1) << 1] < 0)
        emptyLocation = prevLoc + 1;
        else while (emptyLocation <= minLoc ||
                    emptyLocation > maxLoc)
         if ((emptyLocation = -links[emptyLocation << 1]) ==
             firstLoc)
         {
          emptyLocation = capacity + 1;
          break;
         }
     if (emptyLocation <= maxLoc)
     {
      if (capacity < emptyLocation)
      {
       int oldCapacity = capacity;
       if ((capacity += (capacity >> 1) + 7) <= emptyLocation)
        capacity = emptyLocation;
       if (capacity >= (-1 >>> 1) / 3)
        capacity = -1 >>> 1;
       Object[] newElements = new Object[maxLoc = capacity];
       int[] newLinks = new int[capacity * 3 + 2];
       System.arraycopy(elements, 0, newElements, 0, oldCapacity);
       System.arraycopy(links, 0,
        newLinks, 0, (oldCapacity + 1) << 1);
       System.arraycopy(links, links.length - oldCapacity,
        newLinks, newLinks.length - oldCapacity, oldCapacity);
       links = newLinks;
       while (--maxLoc > oldCapacity)
        links[maxLoc << 1] =
         (links[(maxLoc << 1) + 3] = -maxLoc) - 1;
       if (links[1] < oldCapacity)
        links[-(links[(oldCapacity << 1) + 3] =
         links[(links[1] << 1) + 3]) << 1] = -oldCapacity - 1;
       links[-(links[(links[1] << 1) + 3] = -capacity) << 1] =
        -links[1] - 1;
       this.elements = elements = newElements;
       this.links = links;
      }
      if ((maxLoc = -links[emptyLocation << 1]) != emptyLocation)
      {
       links[-(links[(maxLoc << 1) + 1] =
        links[(emptyLocation << 1) + 1]) << 1] = -maxLoc;
       if (links[1] == emptyLocation - 1)
        links[1] = maxLoc - 1;
      }
       else links[1] = capacity;
      elements[emptyLocation - 1] = value;
      if (links[maxLoc = (prevLoc << 1) + 1] != 0)
       maxLoc = nextLoc << 1;
      changeLinks(links, maxLoc, emptyLocation);
      notifyObservers(0, approxLoc = emptyLocation, null);
      return emptyLocation;
     }
    }
   }
  }
  throw new ArrayStoreException("prevLoc: " +
             Integer.toString(prevLoc) + ", emptyLocation: " +
             Integer.toString(emptyLocation) + ", value: " +
             (value != null ? value.toString() : "null"));
 }

/**
 * NOTE: This is a special operation for manual setting of approxLoc
 * (approxLoc is also set internally by insertAt, setAt, locationOf
 * and findLessGreater operations). approxLoc is entirely used
 * internally to speed up setAt, locationOf and findLessGreater
 * operations. approxLoc must be either 0 or a non-empty location,
 * otherwise IllegalArgumentException is thrown. Must be
 * synchronized outside.
 */
 public final void setApproxLoc(int approxLoc)
  throws IllegalArgumentException
 {
  int[] links = this.links;
  if (approxLoc != 0 && (((approxLoc << 1) | approxLoc) < 0 ||
      links.length - approxLoc <= approxLoc << 1 ||
      links[approxLoc << 1] < 0))
   throw new IllegalArgumentException("approxLoc: " +
              Integer.toString(approxLoc));
  this.approxLoc = approxLoc;
 }

/**
 * NOTE: The effectiveness is logarithmic (may be nearly constant).
 * Observers notification is performed. Must be synchronized
 * outside.
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
   if (prevLoc >= (-1 >>> 1) / 3)
    prevLoc = -1 >>> 1;
   Object[] newElements = new Object[last = prevLoc];
   int[] newLinks = new int[prevLoc * 3 + 2];
   System.arraycopy(elements, 0, newElements, 0, oldCapacity);
   System.arraycopy(links, 0, newLinks, 0, (oldCapacity + 1) << 1);
   System.arraycopy(links, links.length - oldCapacity,
    newLinks, newLinks.length - oldCapacity, oldCapacity);
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
   prevLoc = value == null ?
    followLinks(links, location << 1) >> 1 : addressOf(value,
    location, elements, links, oldValue != null ?
    location : approxLoc, this.comparator);
   if (oldValue == null)
    if ((last = -links[location << 1]) != location)
    {
     links[-(links[(last << 1) + 1] =
      links[(location << 1) + 1]) << 1] = -last;
     if (links[1] == location - 1)
      links[1] = last - 1;
    }
     else links[1] = elements.length;
    else if (value == null ||
             prevLoc >> 1 != location && links[prevLoc] == 0)
     changeLinks(links, location << 1, 0);
     else prevLoc = -1;
   if ((elements[location - 1] = value) == null)
   {
    int pred;
    if ((last = links[1] + 1) <= elements.length)
    {
     links[location << 1] = -last;
     pred = -(links[(location << 1) + 1] = links[(last << 1) + 1]);
    }
     else pred = last = location;
    links[pred << 1] = links[(last << 1) + 1] = -location;
    links[1] = location - 1;
    approxLoc = prevLoc;
    location = -location;
   }
    else
    {
     if (prevLoc >= 0)
     {
      if ((last = links[prevLoc]) > 0)
       if ((prevLoc & 1) != 0)
        while ((last = links[prevLoc = last & ~1]) > 0);
        else while ((last = links[prevLoc = last | 1]) > 0);
      changeLinks(links, prevLoc, location);
     }
     approxLoc = location;
     prevLoc = 0;
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
  int[] links = this.links;
  if (parentLocation == 0)
   parentLocation = followLinks(links, forward ? 1 : 0) >> 1;
   else if (((parentLocation << 1) | parentLocation) < 0 ||
            links.length - parentLocation <= parentLocation << 1 ||
            links[parentLocation << 1] < 0)
    throw new IllegalArgumentException("location: " +
               Integer.toString(parentLocation));
    else parentLocation = 0;
  return parentLocation;
 }

/**
 * NOTE: Must be synchronized outside.
 */
 public int siblingLocation(int location, boolean forward)
  throws IllegalArgumentException
 {
  int[] links = this.links;
  if (((location << 1) | location) <= 0 || links.length -
      location <= location << 1 || links[location << 1] < 0)
   throw new IllegalArgumentException("location: " +
              Integer.toString(location));
  location <<= 1;
  if (forward)
   location++;
  return followLinks(links, location) >> 1;
 }

/**
 * NOTE: Search is performed using the supplied comparator. The
 * effectiveness is logarithmic (may be nearly constant). Must be
 * synchronized outside.
 */
 public int locationOf(Object value, int prevLocation,
         boolean forward)
  throws IllegalArgumentException
 {
  Object[] elements = this.elements;
  int[] links = this.links;
  if (prevLocation != 0 && (prevLocation < 0 || prevLocation >
      elements.length || links[prevLocation << 1] < 0))
   throw new IllegalArgumentException("location: " +
              Integer.toString(prevLocation));
  if (value != null)
  {
   prevLocation <<= 1;
   if (forward)
    prevLocation++;
   if (prevLocation > 1 && elements[(prevLocation >> 1) - 1] ==
       value || ((prevLocation = addressOf(value, forward ?
       -prevLocation : elements.length + prevLocation + 1,
       elements, links, prevLocation > 0 ? prevLocation >> 1 :
       approxLoc, this.comparator)) & 1) == (forward ? 1 : 0))
    prevLocation = followLinks(links, prevLocation);
   if (prevLocation > 0 &&
       value.equals(elements[(approxLoc = prevLocation >>= 1) - 1]))
    return prevLocation;
  }
  return 0;
 }

/**
 * NOTE: Search is performed using the supplied comparator. The
 * effectiveness is logarithmic (may be nearly constant). Must be
 * synchronized outside.
 */
 public int findLessGreater(Object value, boolean greater,
         int prevLocation, boolean forward)
  throws IllegalArgumentException
 {
  Object[] elements = this.elements;
  int[] links = this.links;
  if (prevLocation != 0 && (prevLocation < 0 || prevLocation >
      elements.length || links[prevLocation << 1] < 0))
   throw new IllegalArgumentException("location: " +
              Integer.toString(prevLocation));
  if (value != null)
  {
   prevLocation <<= 1;
   if (forward)
    prevLocation++;
   GComparator comparator = this.comparator;
   if (greater != forward || ((prevLocation = addressOf(value,
       greater ? elements.length + prevLocation + 1 : -prevLocation,
       elements, links, prevLocation > 0 ? -(prevLocation >> 1) :
       approxLoc, comparator)) & 1) == (greater ? 1 : 0))
    prevLocation = followLinks(links, prevLocation);
   if ((prevLocation >>= 1) > 0 && (greater == forward || (greater ?
       comparator.greater(elements[prevLocation - 1], value) :
       comparator.greater(value, elements[prevLocation - 1]))))
    return approxLoc = prevLocation;
  }
  return 0;
 }

/**
 * NOTE: Must be synchronized outside.
 */
 public Object clone()
 {
  SortedStorage storage = (SortedStorage)super.clone();
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
  if (this.comparator == null)
   throw new InternalError("comparator: null");
  int linksLength, prev, next, count = elements.length;
  if ((linksLength = links.length) / 3 != count ||
      linksLength % 3 != 2)
   throw new InternalError("links length: " +
              Integer.toString(linksLength) +
              ", elements length: " + Integer.toString(count));
  if ((next = approxLoc) < 0 || next > count ||
      links[next << 1] < 0)
   throw new InternalError("approxLoc: " + Integer.toString(next));
  int location = 0, free = -1;
  do
  {
   if ((next = links[location]) != 0)
   {
    if (next <= 1 || (next >> 1) > elements.length ||
        elements[(next >> 1) - 1] == null)
     throw new InternalError("links[" + Integer.toString(location) +
                "]: " + Integer.toString(next));
    if ((prev = links[linksLength - (next >> 1)]) != location)
     throw new InternalError("links[length - " +
                Integer.toString(next >> 1) + "]: " +
                Integer.toString(prev));
    if (free >= 0)
    {
     if ((links[location ^ 1] & 1) != 0)
      if ((links[location] & 1) != 0)
       throw new InternalError("Bad balance code at: " +
                  Integer.toString(location >> 1));
       else free--;
     if (--free < 0)
      throw new InternalError("Balance mismatch at: " +
                 Integer.toString(next >> 1));
    }
    location = next & ~1;
    count--;
   }
    else
    {
     if (free >= 0)
     {
      if (location > 0 && (links[location ^ 1] & 1) != 0)
       free--;
      if (free != 0)
       throw new InternalError("Invalid tree balance at: " +
                  Integer.toString(location >> 1));
     }
     free = 0;
     do
     {
      if ((links[location ^ 1] & 1) != 0)
       free++;
      if ((location & 1) == 0)
       break;
      free++;
      location = links[linksLength - (location >> 1)];
     } while (true);
     if (location > 0)
      location++;
    }
  } while (location > 0);
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
  int location = 0, capacity;
  GComparator comparator;
  if ((comparator = this.comparator) == null)
   throw new InvalidObjectException("comparator: null");
  Object[] elements = this.elements;
  int[] links = new int[(capacity = elements.length) <
   (-1 >>> 1) / 3 ? capacity * 3 + 2 : -1 >>> 1];
  int free = capacity + 1, approxLoc = 0;
  Object value;
  while (location < capacity)
   if ((value = elements[location++]) != null)
   {
    changeLinks(links, addressOf(value, location, elements, links,
     approxLoc, comparator), location);
    approxLoc = location;
   }
    else
    {
     if (free <= capacity)
      links[-(links[(location << 1) + 1] = -free) << 1] = -location;
      else links[1] = location - 1;
     free = location;
    }
  if (free <= capacity)
   links[(-(links[free << 1] = -links[1] - 1) << 1) + 1] = -free;
   else links[1] = capacity;
  this.links = links;
 }
}
