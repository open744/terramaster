/*
 * @(#) src/net/sf/ivmaidns/storage/Storage.java --
 * Root abstract class for storage of objects.
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

import net.sf.ivmaidns.util.GComparator;
import net.sf.ivmaidns.util.MultiObservable;
import net.sf.ivmaidns.util.ObservedCore;
import net.sf.ivmaidns.util.ReallyCloneable;
import net.sf.ivmaidns.util.Sortable;
import net.sf.ivmaidns.util.ToString;

/**
 * Root abstract class for storage of objects.
 **
 * @version 2.0
 * @author Ivan Maidanski
 */
public abstract class Storage extends ObservedCore
 implements ReallyCloneable, Sortable
{

 public Storage() {}

/**
 * NOTE: Shallow "trim-to-size" is performed. Must be synchronized
 * outside.
 */
 public void trimToSize()
 {
  super.trimToSize();
  int location;
  try
  {
   minimizeCapacity();
  }
  catch (OutOfMemoryError e) {}
  location = 0;
 }

/**
 * NOTE: The effectiveness is linear. Locations are not re-ordered.
 * If OutOfMemoryError is thrown then storage remains unchanged.
 * Must be synchronized outside.
 */
 protected abstract void minimizeCapacity();

/**
 * NOTE: Must be synchronized outside.
 */
 public final void clear()
 {
  int location = nextLocation(0, false), prevLoc;
  while (location > 0)
  {
   try
   {
    prevLoc = nextLocation(location, false);
   }
   catch (IllegalArgumentException e)
   {
    if ((location = nextLocation(0, false)) <= 0)
     break;
    prevLoc = nextLocation(location, false);
   }
   try
   {
    setAt(location, null);
   }
   catch (ArrayStoreException e)
   {
    break;
   }
   location = prevLoc;
  }
  location = 0;
 }

/**
 * NOTE: Result is an empty location (result > 0). Storage state is
 * not altered. The effectiveness is constant (typically). The order
 * of empty locations is undefined. Requires no synchronization.
 */
 public abstract int emptyLocation();

/**
 * NOTE: This method must be called after any atomary changes
 * committed (inside insertAt/setAt operations mainly). If location
 * > 0 then storage event is addition or modification (if oldValue
 * != null) at location. Else if -location > 0 and oldValue != null
 * then storage event is removal of oldValue after prevLoc (if 0 >=
 * prevLoc then first child of -prevLoc is removed else next sibling
 * of prevLoc is removed) at -location.
 */
 protected final void notifyObservers(int prevLoc, int location,
            Object oldValue)
 {
  if (hasObservers())
   super.notifyObservers(this, new StorageEvent(this,
    prevLoc, location, oldValue));
 }

/**
 * NOTE: It is a dummy method which hides the same method in the
 * superclass.
 */
 public final void notifyObservers(MultiObservable observed,
         Object argument) {}

/**
 * NOTE: If prevLoc != 0 and (prevLoc > 0 ? prevLoc : -prevLoc) is
 * an "empty" (including negative) location then
 * IllegalArgumentException is thrown. If 0 > emptyLocation or
 * emptyLocation is not an "empty" location then
 * IllegalArgumentException is thrown. emptyLocation is the location
 * of the inserted value unless emptyLocation == 0 (means any empty
 * location). If value == null or insertion is impossible then
 * ArrayStoreException is thrown. Enough capacity is ensured before
 * any changes. New value is inserted as a first child node of
 * -prevLoc (if 0 >= prevLoc) or as a next sibling node of prevLoc
 * (if prevLoc > 0). Result is the location of the inserted value
 * (result is a "valid" location if prevLoc == 0 or (prevLoc > 0 ?
 * prevLoc : -prevLoc) is a "valid" location). Alters storage state.
 * value itself is not modified. The effectiveness is constant
 * (typically). Observers notification is performed. Must be
 * synchronized outside. If exception is thrown then storage remains
 * unchanged.
 */
 public abstract int insertAt(int prevLoc, int emptyLocation,
         Object value)
  throws IllegalArgumentException, ArrayStoreException;

/**
 * NOTE: If 0 >= location then IllegalArgumentException is thrown.
 * value may be == null (if accepted semantically), result
 * (oldValue) may be == null. If operation is impossible (according
 * to the storage semantics) then ArrayStoreException is thrown.
 * Enough capacity is ensured before any changes. Alters storage
 * state. value itself is not modified. The effectiveness is
 * constant (typically) or logarithmic (or linear when deleting an
 * inner tree node). Observers notification is performed. Must be
 * synchronized outside. If exception is thrown then storage remains
 * unchanged.
 */
 public abstract Object setAt(int location, Object value)
  throws IllegalArgumentException, ArrayStoreException;

/**
 * NOTE: Result != null if and only if location is not "empty" (only
 * positive locations may be not "empty"). Storage state is not
 * altered. The effectiveness is constant. Requires no
 * synchronization.
 */
 public abstract Object getAt(int location);

/**
 * NOTE: Result is true if and only if location is "valid" (only not
 * "empty" locations may be "valid"). Storage state is not altered.
 * The effectiveness is constant. Must be synchronized outside.
 */
 public abstract boolean isValidAt(int location);

/**
 * NOTE: If value == null then ArrayStoreException is thrown. If
 * unique is false or this storage has not contained the specified
 * value yet then value is added to the storage. Anyway, its
 * location is returned. Must be synchronized outside.
 */
 public final int add(Object value, boolean unique)
  throws ArrayStoreException
 {
  if (value == null)
   throw new ArrayStoreException("value: null");
  int location;
  if (!unique || (location = locationOf(value, 0, true)) <= 0)
   setAt(location = emptyLocation(), value);
  return location;
 }

/**
 * NOTE: value may be == null. Only first found value occurance is
 * removed. If removal is denied then ArrayStoreException is thrown.
 * Result is either 0 (not found) or an "empty" location of removed
 * value. Must be synchronized outside.
 */
 public final int remove(Object value)
  throws ArrayStoreException
 {
  int location;
  if ((location = locationOf(value, 0, true)) > 0)
   setAt(location, null);
  return location;
 }

/**
 * NOTE: If 0 >= location then IllegalArgumentException is thrown.
 * If removal is denied then ArrayStoreException is thrown. Must be
 * synchronized outside.
 */
 public final Object removeAt(int location)
  throws IllegalArgumentException, ArrayStoreException
 {
  return setAt(location, null);
 }

/**
 * NOTE: If exception is thrown then nothing is changed. Result is
 * remainder of len (that is, result is the count of not processed
 * values due to ArrayStoreException). array and its elements are
 * not modified. Must be synchronized outside.
 */
 public final int insertAtAll(int prevLoc, Object[] array,
         int offset, int len)
  throws NullPointerException, ArrayIndexOutOfBoundsException,
         IllegalArgumentException
 {
  if (len <= 0)
  {
   len = array.length;
   return 0;
  }
  Object value = array[offset];
  value = array[offset + len - 1];
  try
  {
   do
   {
    prevLoc = insertAt(prevLoc, 0, array[offset++]);
   } while (--len > 0);
  }
  catch (ArrayStoreException e) {}
  return len;
 }

/**
 * NOTE: If exception is thrown then nothing is changed. If unique
 * is true and this storage has already contained value then it is
 * skipped. Result is remainder of len (that is, result is the count
 * of not processed values due to null or ArrayStoreException).
 * array and its elements are not modified. Must be synchronized
 * outside.
 */
 public final int addAll(Object[] array, int offset, int len,
         boolean unique)
  throws NullPointerException, ArrayIndexOutOfBoundsException
 {
  if (len <= 0)
  {
   len = array.length;
   return 0;
  }
  Object value = array[offset];
  value = array[offset + len - 1];
  do
  {
   if ((value = array[offset++]) == null)
    break;
   if (!unique || locationOf(value, 0, true) <= 0)
   {
    try
    {
     setAt(emptyLocation(), value);
    }
    catch (ArrayStoreException e)
    {
     break;
    }
   }
  } while (--len > 0);
  return len;
 }

/**
 * NOTE: If exception is thrown then nothing is changed. Result is
 * remainder of len (that is, result is the count of not processed
 * values due to ArrayStoreException). array and its elements are
 * not modified. Must be synchronized outside.
 */
 public final int removeAll(Object[] array, int offset, int len)
  throws NullPointerException, ArrayIndexOutOfBoundsException
 {
  if (len <= 0)
  {
   len = array.length;
   return 0;
  }
  int location;
  Object value = array[offset];
  value = array[offset + len - 1];
  do
  {
   if ((location = locationOf(array[offset++], 0, true)) > 0)
   {
    try
    {
     setAt(location, null);
    }
    catch (ArrayStoreException e)
    {
     break;
    }
   }
  } while (--len > 0);
  return len;
 }

/**
 * NOTE: If exception is thrown then nothing is changed. Result is
 * remainder of len (that is, result is the count of not processed
 * values due to null or not found). array and its elements are not
 * modified. Must be synchronized outside.
 */
 public final int containsAll(Object[] array, int offset, int len)
  throws NullPointerException, ArrayIndexOutOfBoundsException
 {
  if (len <= 0)
  {
   len = array.length;
   return 0;
  }
  Object value = array[offset];
  value = array[offset + len - 1];
  while (locationOf(array[offset++], 0, true) > 0 && --len > 0);
  return len;
 }

/**
 * NOTE: Result is either 0 or location of the first element in
 * storage (after storagePrevLocation) at which value insertion has
 * failed here. storage and its elements are not modified. Must be
 * synchronized outside, storage must be synchronized too.
 */
 public final int insertAtAll(int prevLoc, Storage storage,
         int storagePrevLocation)
  throws NullPointerException, IllegalArgumentException
 {
  do
  {
   int storageNext;
   if ((storageNext =
       storage.childLocation(storagePrevLocation, true)) > 0)
    prevLoc = -prevLoc;
    else while (prevLoc > 0 && storagePrevLocation > 0 &&
                (storageNext =
                storage.siblingLocation(storagePrevLocation,
                true)) <= 0)
    {
     storagePrevLocation =
      storage.parentLocation(storagePrevLocation);
     prevLoc = parentLocation(prevLoc);
    }
   if (storageNext <= 0)
    break;
   Object value = storage.getAt(storagePrevLocation = storageNext);
   try
   {
    prevLoc = insertAt(prevLoc, 0, value);
   }
   catch (ArrayStoreException e)
   {
    break;
   }
  } while (true);
  return storagePrevLocation;
 }

/**
 * NOTE: If unique is true and this storage has already contained
 * value then it is skipped. Result is either 0 or location of the
 * first element in storage (after storagePrevLocation) at which
 * value addition has failed here. storage and its elements are not
 * modified. Must be synchronized outside, storage must be
 * synchronized too.
 */
 public final int addAll(Storage storage, int storagePrevLocation,
         boolean unique)
  throws NullPointerException, IllegalArgumentException
 {
  while ((storagePrevLocation =
         storage.nextLocation(storagePrevLocation, true)) > 0)
  {
   Object value = storage.getAt(storagePrevLocation);
   if (!unique || locationOf(value, 0, true) <= 0)
   {
    try
    {
     setAt(emptyLocation(), value);
    }
    catch (ArrayStoreException e)
    {
     break;
    }
   }
  }
  return storagePrevLocation;
 }

/**
 * NOTE: Result is either 0 (normally) or location of the first
 * element in storage (after storagePrevLocation) at which value
 * removal has failed here. storage and its elements are not
 * modified. Must be synchronized outside, storage must be
 * synchronized too.
 */
 public final int removeAll(Storage storage,
         int storagePrevLocation)
  throws NullPointerException, IllegalArgumentException
 {
  int location;
  while ((storagePrevLocation =
         storage.nextLocation(storagePrevLocation, true)) > 0)
   if ((location =
       locationOf(storage.getAt(storagePrevLocation), 0, true)) > 0)
   {
    try
    {
     setAt(location, null);
    }
    catch (ArrayStoreException e)
    {
     break;
    }
   }
  return storagePrevLocation;
 }

/**
 * NOTE: Result is either 0 or location of the first element in
 * storage (after storagePrevLocation) at which value is not
 * contained here. Must be synchronized outside, storage must be
 * synchronized too.
 */
 public final int containsAll(Storage storage,
         int storagePrevLocation)
  throws NullPointerException, IllegalArgumentException
 {
  while ((storagePrevLocation =
         storage.nextLocation(storagePrevLocation, true)) > 0 &&
         locationOf(storage.getAt(storagePrevLocation),
         0, true) > 0);
  return storagePrevLocation;
 }

/**
 * NOTE: If parentLocation != 0 and parentLocation is "empty" then
 * IllegalArgumentException is thrown. Result is either 0 (not
 * found) or a found (not "empty") location. If (parentLocation == 0
 * or is "valid") and result != 0 then result is a "valid" location.
 * Storage state is not altered. The effectiveness is constant. Must
 * be synchronized outside.
 */
 public abstract int childLocation(int parentLocation,
         boolean forward)
  throws IllegalArgumentException;

/**
 * NOTE: If location is "empty" then IllegalArgumentException is
 * thrown. Result is either 0 (not found) or a found (not "empty")
 * location. If location is "valid" and result != 0 then result is
 * a "valid" location. Storage state is not altered. The
 * effectiveness is constant (typically). Must be synchronized
 * outside.
 */
 public abstract int siblingLocation(int location, boolean forward)
  throws IllegalArgumentException;

/**
 * NOTE: If location is "empty" then IllegalArgumentException is
 * thrown. Result is either 0 (not found) or a found (not "empty")
 * location. If location is "valid" and result != 0 then result is a
 * "valid" location. Storage state is not altered. The effectiveness
 * is constant. Must be synchronized outside.
 */
 public abstract int parentLocation(int location)
  throws IllegalArgumentException;

/**
 * NOTE: parentLocation may be == 0. Must be synchronized outside.
 */
 public final boolean hasChildren(int parentLocation)
  throws IllegalArgumentException
 {
  return childLocation(parentLocation, true) > 0;
 }

/**
 * NOTE: prevLocation may be == 0. If result == 0 then no next
 * location. This method is used for iteration on storage elements.
 * Must be synchronized outside.
 */
 public final int nextLocation(int prevLocation, boolean forward)
  throws IllegalArgumentException
 {
  int next;
  if (!forward)
   if ((next = prevLocation) <= 0 ||
       (next = siblingLocation(prevLocation, false)) > 0)
    while ((prevLocation = childLocation(next, false)) > 0)
     next = prevLocation;
    else next = parentLocation(prevLocation);
   else if ((next = childLocation(prevLocation, true)) <= 0)
    while (prevLocation > 0 &&
           (next = siblingLocation(prevLocation, true)) <= 0)
     prevLocation = parentLocation(prevLocation);
  return next;
 }

/**
 * NOTE: If prevLocation != 0 and prevLocation is an "empty"
 * location then IllegalArgumentException is thrown. If value
 * == null then 0 is returned. Else search is started after/before
 * prevLocation. Result is either 0 (no more values) or a (not
 * "empty") location of the first found element, which is equal to
 * value. If (prevLocation == 0 or prevLocation is "valid") and
 * result != 0 then result is a "valid" location. Storage state is
 * not altered. The effectiveness is linear (typically), logarithmic
 * or constant. Must be synchronized outside.
 */
 public abstract int locationOf(Object value, int prevLocation,
         boolean forward)
  throws IllegalArgumentException;

/**
 * NOTE: If prevLocation != 0 and prevLocation is an "empty"
 * location then IllegalArgumentException is thrown. If value
 * == null then 0 is returned. Search is started after/before
 * prevLocation. Result is either 0 (no more values) or a (not
 * "empty") location of the first found element, which is (greater
 * ? 'greater' : 'less') than value. If (prevLocation == 0 or
 * prevLocation is "valid") and result != 0 then result is a "valid"
 * location. Storage state is not altered. The effectiveness is
 * linear (typically), logarithmic or constant. Must be synchronized
 * outside.
 */
 public abstract int findLessGreater(Object value, boolean greater,
         int prevLocation, boolean forward)
  throws IllegalArgumentException;

/**
 * NOTE: value may be == null. Must be synchronized outside.
 */
 public final boolean contains(Object value)
 {
  return locationOf(value, 0, true) > 0;
 }

/**
 * NOTE: value may be == null. Result >= 0. Must be synchronized
 * outside.
 */
 public final int containsCount(Object value)
 {
  int count = 0, location = 0;
  while ((location = locationOf(value, location, true)) > 0)
   count++;
  return count;
 }

/**
 * NOTE: Result is exact instanceof Object[] with non-null elements.
 * Must be synchronized outside.
 */
 public Object[] toArray()
 {
  int location, capacity = 0;
  if ((location = childLocation(0, true)) > 0)
   capacity = 15;
  Object[] array = new Object[capacity], newArray;
  if (location > 0)
  {
   int index = 0;
   do
   {
    if (index >= capacity)
    {
     if ((capacity += capacity >> 1) <= 0)
      capacity = -1 >>> 1;
     System.arraycopy(array, 0,
      newArray = new Object[capacity], 0, index);
     array = newArray;
    }
    array[index++] = getAt(location);
   } while ((location = nextLocation(location, true)) > 0);
   if (index < capacity)
   {
    System.arraycopy(array, 0,
     newArray = new Object[index], 0, index);
    array = newArray;
   }
  }
  return array;
 }

/**
 * NOTE: Must be synchronized outside.
 */
 public Object clone()
 {
  Object obj;
  try
  {
   if ((obj = super.clone()) instanceof Storage && obj != this)
    return obj;
  }
  catch (CloneNotSupportedException e) {}
  throw new InternalError("CloneNotSupportedException");
 }

/**
 * NOTE: Storage state is not altered. Must be synchronized outside.
 */
 public int hashCode()
 {
  int location;
  if ((location = childLocation(0, true)) > 0)
  {
   int code = 0, next, count = 0;
   do
   {
    code ^= getAt(location).hashCode();
    count++;
    if ((next = childLocation(location, true)) > 0)
     code = (code << 7) - code;
     else if ((next = siblingLocation(location, true)) > 0)
      code = (code << 5) - code;
      else do
      {
       if ((location = parentLocation(location)) > 0)
        code = (code << 4) + code;
        else return code ^ count;
      } while ((next = siblingLocation(location, true)) <= 0);
    location = next;
   } while (true);
  }
  return 0;
 }

/**
 * NOTE: Storage state is not altered. Must be synchronized outside,
 * obj must be synchronized too if not null.
 */
 public boolean equals(Object obj)
 {
  if (obj == this)
   return true;
  if (obj instanceof Storage)
  {
   int location = 0, next, storageLocation = 0, storageNext;
   Storage storage = (Storage)obj;
   do
   {
    storageNext = storage.childLocation(storageLocation, true);
    if ((next = childLocation(location, true)) <= 0)
     if (storageNext > 0)
      break;
      else do
      {
       if (location <= 0 || storageLocation <= 0)
        return true;
       storageNext = storage.siblingLocation(storageLocation, true);
       if ((next = siblingLocation(location, true)) > 0)
        break;
       if (storageNext > 0)
        return false;
       location = parentLocation(location);
       storageLocation = storage.parentLocation(storageLocation);
      } while (true);
   } while ((storageLocation = storageNext) > 0 && getAt(location =
            next).equals(storage.getAt(storageLocation)));
  }
  return false;
 }

/**
 * NOTE: The first found non-equal elements pair is compared through
 * GComparator INSTANCE. Storage state is not altered. Must be
 * synchronized outside, obj must be synchronized too if not null.
 */
 public boolean greaterThan(Object obj)
 {
  if (obj == this || !(obj instanceof Storage))
   return false;
  Storage storage = (Storage)obj;
  int location = 0, next, storageLocation = 0;
  Object value, storageValue;
  do
  {
   int storageNext = storage.childLocation(storageLocation, true);
   if ((next = childLocation(location, true)) <= 0)
    if (storageNext > 0)
     return false;
     else do
     {
      if (location <= 0 || storageLocation <= 0)
       return false;
      storageNext = storage.siblingLocation(storageLocation, true);
      if ((next = siblingLocation(location, true)) > 0)
       break;
      if (storageNext > 0)
       return false;
      location = parentLocation(location);
      storageLocation = storage.parentLocation(storageLocation);
     } while (true);
   if (storageNext <= 0)
    return true;
   storageValue = storage.getAt(storageLocation = storageNext);
  } while ((value = getAt(location = next)).equals(storageValue));
  return GComparator.INSTANCE.greater(value, storageValue);
 }

/**
 * NOTE: To separate storage values ", " is used, to represent tree
 * layout/hierarchy " {" and " }" are used. converter must be
 * != null. Result != null, result is 'in-line' (of course, if
 * values toString() are 'in-line' too). Storage state is not
 * altered. Must be synchronized outside.
 */
 public final String toInlineString(ToString converter)
  throws NullPointerException
 {
  int location, next;
  converter.equals(converter);
  StringBuffer sBuf = new StringBuffer(58);
  sBuf.append('{');
  if ((location = childLocation(0, true)) > 0)
   do
   {
    sBuf.append(' ').append(converter.toString(getAt(location)));
    if ((next = childLocation(location, true)) <= 0)
    {
     while ((next = siblingLocation(location, true)) <= 0)
     {
      sBuf.append(' ').append('}');
      if ((location = parentLocation(location)) <= 0)
       return new String(sBuf);
     }
     sBuf.append(',');
    }
     else sBuf.append(' ').append('{');
    location = next;
   } while (true);
  return new String(sBuf.append(' ').append('}'));
 }

/**
 * NOTE: To separate storage values "\n" is used, to represent tree
 * layout/hierarchy "`", "|" and "-- " are used. converter must be
 * != null. Result != null. Storage state is not altered. Must be
 * synchronized outside.
 */
 public final String toOutlineString(ToString converter)
  throws NullPointerException
 {
  int location, next = 0, tab, sibling;
  converter.equals(converter);
  if ((location = childLocation(0, true)) > 0)
   next = 58;
  StringBuffer sBuf = new StringBuffer(next);
  if (location > 0)
  {
   char[] layout = new char[0];
   tab = sibling = 0;
   do
   {
    sBuf.append(converter.toString(getAt(location))).append('\n');
    if ((next = childLocation(location, true)) > 0)
    {
     if (tab > 0)
     {
      layout[tab - 3] = layout[tab - 2] = ' ';
      if (layout[tab - 4] != '|')
       layout[tab - 4] = ' ';
     }
     if ((tab += 4) > layout.length)
     {
      if ((location = ((tab >> 3) << 2) + tab) <= 0)
       location = -1 >>> 1;
      char[] newLayout;
      System.arraycopy(layout, 0,
       newLayout = new char[location], 0, tab - 4);
      (layout = newLayout)[tab - 1] = ' ';
     }
     layout[tab - 3] = layout[tab - 2] = '-';
    }
     else if ((next = tab > 0 ? sibling :
              siblingLocation(location, true)) <= 0)
     {
      do
      {
       if ((tab -= 4) < 0 ||
           (location = parentLocation(location)) <= 0)
        return new String(sBuf);
      } while (tab > 0 && layout[tab - 4] != '|' ||
               (next = siblingLocation(location, true)) <= 0);
      if (tab > 0)
       layout[tab - 3] = layout[tab - 2] = '-';
     }
    location = next;
    if (tab > 0)
    {
     char ch = '`';
     if ((sibling = siblingLocation(location, true)) > 0)
      ch = '|';
     layout[tab - 4] = ch;
     sBuf.append(layout, 0, tab);
    }
   } while (true);
  }
  return new String(sBuf);
 }

/**
 * NOTE: Result != null, result is 'out-line' here. This method may
 * be overridden in subclasses if needed. Must be synchronized
 * outside.
 */
 public String toString()
 {
  return toOutlineString(ToString.INSTANCE);
 }

/**
 * NOTE: Shallow check for integrity of this object. The
 * effectiveness is linear. Should be overridden in subclasses. Must
 * be synchronized outside. For debug purpose only.
 */
 public void integrityCheck()
 {
  super.integrityCheck();
 }
}
