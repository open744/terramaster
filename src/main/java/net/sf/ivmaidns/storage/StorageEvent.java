/*
 * @(#) src/net/sf/ivmaidns/storage/StorageEvent.java --
 * Class for storage updation events.
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
import java.io.Serializable;

import net.sf.ivmaidns.util.Immutable;
import net.sf.ivmaidns.util.Indexable;
import net.sf.ivmaidns.util.ReallyCloneable;
import net.sf.ivmaidns.util.Verifiable;

/**
 * Class for storage updation events (addition, modification and
 * removal).
 **
 * @version 2.0
 * @author Ivan Maidanski
 */
public final class StorageEvent
 implements Immutable, ReallyCloneable, Serializable, Indexable,
            Verifiable
{

/**
 * The class version unique identifier for serialization
 * interoperability.
 **
 * @since 1.0
 */
 private static final long serialVersionUID = 1079274082506263249L;

/**
 * NOTE: source must be != null.
 */
 protected final Storage source;

 protected final int prevLoc;

 protected final int location;

/**
 * NOTE: oldValue may be == null.
 */
 protected final Object oldValue;

/**
 * NOTE: source must be != null. If location > 0 then storage event
 * is addition or modification (if oldValue != null) at location.
 * Else if -location > 0 and oldValue != null then storage event is
 * removal of oldValue after prevLoc (if 0 >= prevLoc then first
 * child of -prevLoc is removed) at -location.
 */
 public StorageEvent(Storage source, int prevLoc,
         int location, Object oldValue)
  throws NullPointerException
 {
  (this.source = source).equals(source);
  this.prevLoc = prevLoc;
  this.location = location;
  this.oldValue = oldValue;
 }

/**
 * NOTE: Result != null.
 **
 * @since 2.0
 */
 public final Storage source()
 {
  return this.source;
 }

 public final int getPrevLoc()
 {
  return this.prevLoc;
 }

 public final int getLocation()
 {
  return this.location;
 }

 public final Object getOldValue()
 {
  return this.oldValue;
 }

/**
 * NOTE: Result is the number of elements accessible through
 * getAt(int).
 **
 * @since 2.0
 */
 public int length()
 {
  return 4;
 }

/**
 * NOTE: Result is (new Object[] { getSource(),
 * new Integer(getPrevLoc()), new Integer(getLocation()),
 * getOldValue() })[index].
 **
 * @since 2.0
 */
 public Object getAt(int index)
  throws ArrayIndexOutOfBoundsException
 {
  if (((3 - index) | index) >= 0)
   if ((index - 1) >> 1 == 0)
    return new Integer(index == 1 ? this.prevLoc : this.location);
    else if (index == 0)
     return this.source;
     else return this.oldValue;
  throw new ArrayIndexOutOfBoundsException(index);
 }

 public Object clone()
 {
  Object obj;
  try
  {
   if ((obj = super.clone()) instanceof StorageEvent && obj != this)
    return obj;
  }
  catch (CloneNotSupportedException e) {}
  throw new InternalError("CloneNotSupportedException");
 }

/**
 * NOTE: source is not hashed.
 */
 public int hashCode()
 {
  int code = this.prevLoc;
  code = ((code << 5) - code) ^ this.location;
  Object oldValue;
  code = (code << 5) - code;
  if ((oldValue = this.oldValue) != null)
   code ^= oldValue.hashCode();
  return ((code << 5) - code) ^ 3;
 }

 public boolean equals(Object obj)
 {
  boolean isEqual = true;
  if (obj != this)
  {
   StorageEvent event;
   Object oldValue;
   isEqual = false;
   if (obj instanceof StorageEvent &&
       (event = (StorageEvent)obj).location == this.location &&
       event.prevLoc == this.prevLoc &&
       this.source.equals(event.source))
    if ((oldValue = this.oldValue) != null)
     isEqual = oldValue.equals(event.oldValue);
     else if (event.oldValue == null)
      isEqual = true;
  }
  return isEqual;
 }

/**
 * NOTE: source is not represented in the result.
 */
 public String toString()
 {
  int prevLoc = this.prevLoc, location = this.location;
  StringBuffer sBuf = new StringBuffer(58);
  sBuf.append("StorageEvent: ");
  Object oldValue = this.oldValue;
  if (location > 0)
  {
   sBuf.append(oldValue != null ? "modified at " : "added at ").
    append(Integer.toString(location));
   if (oldValue != null)
    sBuf.append(':').append(' ').append(oldValue.toString());
  }
   else if (oldValue != null && -location > 0)
    sBuf.append("removed ").
     append(prevLoc >= 0 ? "after " : "first child of ").
     append(Integer.toString(prevLoc >= 0 ? prevLoc : -prevLoc)).
     append(" at ").append(Integer.toString(-location)).
     append(':').append(' ').append(oldValue.toString());
  return new String(sBuf);
 }

/**
 * NOTE: Check for integrity of this object. source storage is not
 * checked. For debug purpose only.
 **
 * @since 2.0
 */
 public void integrityCheck()
 {
  if (this.source == null)
   throw new InternalError("source: null");
 }

 private void readObject(ObjectInputStream in)
  throws IOException, ClassNotFoundException
 {
  in.defaultReadObject();
  if (this.source == null)
   throw new InvalidObjectException("source: null");
 }
}
