/*
 * @(#) src/net/sf/ivmaidns/storage/ObjectStorage.java --
 * Class for simple array-like storage.
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
import java.io.ObjectOutputStream;
import java.io.Serializable;

import net.sf.ivmaidns.util.GComparator;

/**
 * Class for simple array-like storage.
 **
 * @version 2.0
 * @author Ivan Maidanski
 */
public class ObjectStorage extends Storage
 implements Serializable
{

/**
 * The class version unique identifier for serialization
 * interoperability.
 **
 * @since 2.0
 */
 private static final long serialVersionUID = 7652582440997838363L;

/**
 * NOTE: elements must be != null. This field is cloned (shallow)
 * and manually serialized (by elements). elements length is not
 * serialized. Initially, elements length == 0.
 */
 protected transient Object[] elements = new Object[0];

 public ObjectStorage() {}

/**
 * NOTE: The effectiveness is linear. Locations are not re-ordered.
 * If OutOfMemoryError is thrown then storage remains unchanged.
 * Must be synchronized outside. Should be overridden in subclasses.
 */
 protected void minimizeCapacity()
 {
  Object[] elements = this.elements;
  int capacity;
  if ((capacity = elements.length) > 0 &&
      elements[--capacity] == null)
  {
   while (capacity-- > 0 && elements[capacity] == null);
   capacity++;
   Object[] newElements;
   System.arraycopy(elements, 0,
    newElements = new Object[capacity], 0, capacity);
   this.elements = newElements;
  }
 }

 public int emptyLocation()
 {
  Object[] elements = this.elements;
  int location = -1, capacity = elements.length;
  while (++location < capacity && elements[location] != null);
  return location + 1;
 }

/**
 * NOTE: prevLoc must be >= 0 and insertion after prevLoc must be
 * possible in this storage (according to the semantics of array),
 * otherwise ArrayStoreException is thrown. Insertion (storage state
 * altering) here is entirely performed through setAt operation. The
 * effectiveness is nearly constant. Observers notification is
 * performed. Must be synchronized outside.
 */
 public int insertAt(int prevLoc, int emptyLocation, Object value)
  throws IllegalArgumentException, ArrayStoreException
 {
  Object[] elements = this.elements;
  int capacity = elements.length, location;
  if ((location = prevLoc) != 0)
  {
   if (prevLoc < 0)
    location = -prevLoc;
   if (location - 1 >= capacity || elements[location - 1] == null)
    throw new IllegalArgumentException("prevLoc: " +
               Integer.toString(prevLoc));
  }
  if (emptyLocation != 0 && (emptyLocation < 0 ||
      emptyLocation <= capacity &&
      elements[emptyLocation - 1] != null))
   throw new IllegalArgumentException("emptyLocation: " +
              Integer.toString(emptyLocation));
  if (value != null && prevLoc >= 0)
  {
   if (prevLoc < emptyLocation)
   {
    if (capacity >= emptyLocation)
     capacity = emptyLocation - 1;
    while (location < capacity)
     if (elements[location++] != null)
      capacity = -1;
   }
    else if (emptyLocation > 0 || prevLoc < capacity &&
             elements[prevLoc] != null)
     capacity = -1;
   if (capacity >= 0)
   {
    if (emptyLocation == 0)
     emptyLocation = prevLoc + 1;
    setAt(emptyLocation, value);
    return emptyLocation;
   }
  }
  throw new ArrayStoreException("prevLoc: " +
             Integer.toString(prevLoc) + ", emptyLocation: " +
             Integer.toString(emptyLocation) + ", value: " +
             (value != null ? value.toString() : "null"));
 }

/**
 * NOTE: The effectiveness is constant. Observers notification is
 * performed. Must be synchronized outside.
 */
 public Object setAt(int location, Object value)
  throws IllegalArgumentException
 {
  if (location <= 0)
   throw new IllegalArgumentException("location: " +
              Integer.toString(location));
  int prevLoc;
  Object[] elements = this.elements;
  if ((prevLoc = elements.length) < location)
  {
   if ((prevLoc += (prevLoc >> 1) + 7) <= location)
    prevLoc = location;
   Object[] newElements;
   System.arraycopy(elements, 0,
    newElements = new Object[prevLoc], 0, elements.length);
   this.elements = elements = newElements;
  }
  Object oldValue;
  if ((oldValue = elements[location - 1]) != null || value != null)
  {
   prevLoc = 0;
   if ((elements[location - 1] = value) == null)
   {
    if (!hasObservers())
     return oldValue;
    prevLoc = location - 1;
    while (prevLoc-- > 0 && elements[prevLoc] == null);
    prevLoc++;
    location = -location;
   }
   notifyObservers(prevLoc, location, oldValue);
  }
  return oldValue;
 }

/**
 * NOTE: All non-empty locations are valid.
 */
 public final Object getAt(int location)
 {
  Object value = null;
  Object[] elements;
  if (location > 0 && (elements = this.elements).length >= location)
   value = elements[location - 1];
  return value;
 }

 public final boolean isValidAt(int location)
 {
  Object[] elements;
  return location > 0 && (elements = this.elements).length >=
   location && elements[location - 1] != null;
 }

 public int childLocation(int parentLocation, boolean forward)
  throws IllegalArgumentException
 {
  Object[] elements = this.elements;
  int capacity = elements.length;
  if (parentLocation == 0)
   if (forward)
   {
    while (parentLocation < capacity &&
           elements[parentLocation] == null)
     parentLocation++;
    if (++parentLocation > capacity)
     parentLocation = 0;
   }
    else
    {
     parentLocation = capacity;
     while (parentLocation-- > 0 &&
            elements[parentLocation] == null);
     parentLocation++;
    }
   else if (parentLocation < 0 || parentLocation > capacity ||
            elements[parentLocation - 1] == null)
    throw new IllegalArgumentException("location: " +
               Integer.toString(parentLocation));
    else parentLocation = 0;
  return parentLocation;
 }

 public int siblingLocation(int location, boolean forward)
  throws IllegalArgumentException
 {
  Object[] elements = this.elements;
  int capacity;
  if ((capacity = elements.length) < location ||
      location <= 0 || elements[location - 1] == null)
   throw new IllegalArgumentException("location: " +
              Integer.toString(location));
  if (forward)
  {
   while (location < capacity && elements[location] == null)
    location++;
   if (++location > capacity)
    location = 0;
  }
   else
   {
    location--;
    while (location-- > 0 && elements[location] == null);
    location++;
   }
  return location;
 }

 public int parentLocation(int location)
  throws IllegalArgumentException
 {
  Object[] elements;
  if (location <= 0 || (elements = this.elements).length <
      location || elements[location - 1] == null)
   throw new IllegalArgumentException("location: " +
              Integer.toString(location));
  return 0;
 }

/**
 * NOTE: If value == null then 0 is returned. value is tested for
 * equality against elements. The effectiveness is linear. Must be
 * synchronized outside.
 */
 public int locationOf(Object value, int prevLocation,
         boolean forward)
  throws IllegalArgumentException
 {
  if (value != null)
   while ((prevLocation = nextLocation(prevLocation,
          forward)) > 0 && !value.equals(getAt(prevLocation)));
   else if (prevLocation != 0)
   {
    parentLocation(prevLocation);
    prevLocation = 0;
   }
  return prevLocation;
 }

/**
 * NOTE: If value == null then 0 is returned. If greater then
 * elements are compared against value else value is compared
 * against elements. Here, the comparison is performed through
 * GComparator INSTANCE. The effectiveness is linear. Must be
 * synchronized outside.
 */
 public int findLessGreater(Object value, boolean greater,
         int prevLocation, boolean forward)
  throws IllegalArgumentException
 {
  if (value != null)
  {
   GComparator comparator = GComparator.INSTANCE;
   if (greater)
    while ((prevLocation =
           nextLocation(prevLocation, forward)) > 0 &&
           !comparator.greater(getAt(prevLocation), value));
    else while ((prevLocation =
                nextLocation(prevLocation, forward)) > 0 &&
                !comparator.greater(value, getAt(prevLocation)));
  }
   else if (prevLocation != 0)
   {
    parentLocation(prevLocation);
    prevLocation = 0;
   }
  return prevLocation;
 }

/**
 * NOTE: Must be synchronized outside.
 */
 public Object clone()
 {
  ObjectStorage storage = (ObjectStorage)super.clone();
  storage.elements = (Object[])storage.elements.clone();
  return storage;
 }

/**
 * NOTE: Shallow check for integrity of this object. Must be
 * synchronized outside. For debug purpose only.
 */
 public void integrityCheck()
 {
  super.integrityCheck();
  if (this.elements == null)
   throw new InternalError("elements: null");
 }

/**
 * NOTE: Must be synchronized outside.
 */
 private void writeObject(ObjectOutputStream out)
  throws IOException
 {
  out.defaultWriteObject();
  Object[] elements = this.elements;
  int location = 0, capacity = elements.length;
  while (capacity-- > 0 && elements[capacity] == null);
  out.writeInt(++capacity);
  while (location < capacity)
   out.writeObject(elements[location++]);
 }

 private void readObject(ObjectInputStream in)
  throws IOException, ClassNotFoundException
 {
  in.defaultReadObject();
  int location = 0, capacity;
  if ((capacity = in.readInt()) < 0)
   capacity = -1 >>> 1;
  Object[] elements = new Object[capacity];
  while (location < capacity)
   elements[location++] = in.readObject();
  this.elements = elements;
 }
}
