/*
 * @(#) src/net/sf/ivmaidns/storage/IntStorage.java --
 * Abstract class for storage of integers.
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

/**
 * Abstract class for storage of integers (optimized).
 **
 * @version 2.0
 * @author Ivan Maidanski
 **
 * @since 2.0
 */
public abstract class IntStorage extends Storage
{

/**
 * NOTE: links must be != null (length > 1), links[1] =
 * emptyLocation() - 1.
 */
 protected transient int[] links;

 public IntStorage(int initialLength)
 {
  if (initialLength <= 1)
   initialLength = -1 >>> 1;
  this.links = new int[initialLength];
 }

 public final int emptyLocation()
 {
  return this.links[1] + 1;
 }

/**
 * NOTE: value must be of Integer and insertion after prevLoc must
 * be possible in this storage (according to the semantics),
 * otherwise ArrayStoreException is thrown. Enough capacity ensured
 * before any changes. Result is the location of the inserted value.
 * Observers notification is performed. Must be synchronized
 * outside.
 */
 public final int insertAt(int prevLoc, int emptyLocation,
         Object value)
  throws IllegalArgumentException, ArrayStoreException
 {
  if (value instanceof Integer)
   return insertIntAt(prevLoc, emptyLocation,
    ((Integer)value).intValue());
  if (prevLoc != 0 && !isValidAt(prevLoc > 0 ? prevLoc : -prevLoc))
   throw new IllegalArgumentException("prevLoc: " +
              Integer.toString(prevLoc));
  if (emptyLocation < 0 || isValidAt(emptyLocation))
   throw new IllegalArgumentException("emptyLocation: " +
              Integer.toString(emptyLocation));
  throw new ArrayStoreException("prevLoc: " +
             Integer.toString(prevLoc) + ", emptyLocation: " +
             Integer.toString(emptyLocation) + ", value: " +
             (value != null ? value.toString() : "null"));
 }

/**
 * NOTE: If prevLoc != 0 and (prevLoc > 0 ? prevLoc : -prevLoc) is
 * an "empty" (including negative) location then
 * IllegalArgumentException is thrown. If 0 > emptyLocation or
 * emptyLocation is not an "empty" location then
 * IllegalArgumentException is thrown. emptyLocation is the location
 * of the inserted value unless emptyLocation == 0 (means any empty
 * location). If insertion is impossible then ArrayStoreException is
 * thrown. Enough capacity is ensured before any changes. New value
 * is inserted as a first child node of -prevLoc (if 0 >= prevLoc)
 * or as a next sibling node of prevLoc (if prevLoc > 0). Result is
 * the location of the inserted value. Alters storage state. The
 * effectiveness is constant (typically). Observers notification is
 * performed. Must be synchronized outside.
 */
 public abstract int insertIntAt(int prevLoc, int emptyLocation,
         int value)
  throws IllegalArgumentException, ArrayStoreException;

/**
 * NOTE: If 0 >= location then IllegalArgumentException is thrown.
 * value must be either null (always accepted) or of Integer
 * (otherwise ArrayStoreException is thrown), result (oldValue) is
 * either null or of Integer. If operation is impossible (according
 * to the storage semantics) then ArrayStoreException is thrown.
 * Enough capacity ensured before any changes. Alters storage state.
 * Observers notification is performed. Must be synchronized
 * outside.
 */
 public final Object setAt(int location, Object value)
  throws IllegalArgumentException, ArrayStoreException
 {
  int intOldValue = getIntAt(location);
  Object oldValue = null;
  if (intOldValue != 0 || isValidAt(location))
   oldValue = new Integer(intOldValue);
  if (value == null)
  {
   removeIntAt(location);
   return oldValue;
  }
  if (value instanceof Integer)
  {
   setIntAt(location, ((Integer)value).intValue());
   return oldValue;
  }
  if (location <= 0)
   throw new IllegalArgumentException("location: " +
              Integer.toString(location));
  throw new ArrayStoreException("location: " +
             Integer.toString(location) + ", oldValue: " +
             (oldValue != null ? oldValue.toString() :
             "null") + ", value: " + value.toString());
 }

/**
 * NOTE: If 0 >= location then IllegalArgumentException is thrown.
 * If operation is impossible (according to the storage semantics)
 * then ArrayStoreException is thrown. Enough capacity ensured
 * before any changes. Alters storage state. Result is the previous
 * value at location (or 0 if location was "empty"). The
 * effectiveness is constant (typically) or logarithmic. Observers
 * notification is performed. Must be synchronized outside.
 */
 public abstract int setIntAt(int location, int value)
  throws IllegalArgumentException, ArrayStoreException;

/**
 * NOTE: If 0 >= location then IllegalArgumentException is thrown.
 * If location is "empty" then nothing is performed and 0 is
 * returned. Alters storage state. Result is the removed value at
 * location. Observers notification is performed. Must be
 * synchronized outside.
 */
 public abstract int removeIntAt(int location)
  throws IllegalArgumentException;

/**
 * NOTE: Result != null if and only if location is not "empty". If
 * location is not "empty" then it is "valid". Storage state is not
 * altered. Requires no synchronization.
 */
 public final Object getAt(int location)
 {
  Object value = null;
  int intValue;
  if ((intValue = getIntAt(location)) != 0 || isValidAt(location))
   value = new Integer(intValue);
  return value;
 }

/**
 * NOTE: If location is "empty" then 0 is returned. Storage state is
 * not altered. The effectiveness is constant. Requires no
 * synchronization.
 */
 public abstract int getIntAt(int location);

/**
 * NOTE: If value is not instanceof Integer then 0 is returned. Must
 * be synchronized outside.
 */
 public final int locationOf(Object value, int prevLocation,
         boolean forward)
  throws IllegalArgumentException
 {
  if (value instanceof Integer)
   prevLocation = locationOfInt(((Integer)value).intValue(),
    prevLocation, forward);
   else if (prevLocation != 0)
   {
    parentLocation(prevLocation);
    prevLocation = 0;
   }
  return prevLocation;
 }

/**
 * NOTE: If prevLocation != 0 and prevLocation is an "empty"
 * location then IllegalArgumentException is thrown. Search is
 * started after/before prevLocation. Result is either 0 (no more
 * values) or a (not "empty") location of the first found element,
 * which is equal to value. If (prevLocation == 0 or prevLocation is
 * "valid") and result != 0 then result is a "valid" location.
 * Storage state is not altered. The effectiveness is linear,
 * logarithmic (typically) or constant. Must be synchronized
 * outside.
 */
 public abstract int locationOfInt(int value, int prevLocation,
         boolean forward)
  throws IllegalArgumentException;

/**
 * NOTE: If value is not instanceof Integer then 0 is returned. Must
 * be synchronized outside.
 */
 public final int findLessGreater(Object value, boolean greater,
         int prevLocation, boolean forward)
  throws IllegalArgumentException
 {
  if (value instanceof Integer)
   prevLocation = findLessGreaterInt(((Integer)value).intValue(),
    greater, prevLocation, forward);
   else if (prevLocation != 0)
   {
    parentLocation(prevLocation);
    prevLocation = 0;
   }
  return prevLocation;
 }

/**
 * NOTE: If prevLocation != 0 and prevLocation is an "empty"
 * location then IllegalArgumentException is thrown. Else search is
 * started after/before prevLocation. Result is either 0 (no more
 * values) or a (not "empty") location of the first/next found
 * element, which is (greater ? 'greater' : 'less') than value.
 * Storage state is not altered. The effectiveness is linear,
 * logarithmic (typically) or constant. Must be synchronized
 * outside.
 */
 public abstract int findLessGreaterInt(int value, boolean greater,
         int prevLocation, boolean forward)
  throws IllegalArgumentException;

/**
 * NOTE: Must be synchronized outside.
 */
 public Object clone()
 {
  IntStorage storage = (IntStorage)super.clone();
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
  if (this.links == null)
   throw new InternalError("links: null");
 }
}
