/*
 * @(#) src/net/sf/ivmaidns/storage/StorageEnumerator.java --
 * Storage elements enumerator.
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

import java.util.Enumeration;
import java.util.NoSuchElementException;

import net.sf.ivmaidns.util.MultiObservable;
import net.sf.ivmaidns.util.Notifiable;
import net.sf.ivmaidns.util.ObservedCore;
import net.sf.ivmaidns.util.ReallyCloneable;

/**
 * Storage elements enumerator (observable location wrapper).
 **
 * @version 2.0
 * @author Ivan Maidanski
 */
public class StorageEnumerator extends ObservedCore
 implements ReallyCloneable, Serializable, Enumeration
{

/**
 * The class version unique identifier for serialization
 * interoperability.
 **
 * @since 2.0
 */
 private static final long serialVersionUID = 6807429201174280727L;

/**
 * NOTE: source must be != null. enumerator must be synchronized
 * with source.
 */
 protected final Storage source;

/**
 * NOTE: If location != 0 then location must be valid. If value at
 * this location is removed in source then location is automatically
 * set to the next location in source. If this location is changed
 * then the observers are notified with the argument of its previous
 * value wrapped by Integer.
 */
 protected int location;

/**
 * NOTE: agent must be != null. agent must observe source if and
 * only if location != 0. agent should not be accessible outside.
 */
 private transient Notifiable agent;

/**
 * NOTE: source must be != null. location is initialized with source
 * childLocation(0, true). Must be synchronized outside with source.
 * When this enumerator is not needed any more, setLocation(0)
 * should be called to stop observing of source.
 */
 public StorageEnumerator(Storage source)
  throws NullPointerException
 {
  this.location = (this.source = source).childLocation(0, true);
  this.agent = new StorageEnumeratorAgent(this);
  if (this.location > 0)
   source.addObserver(this.agent);
 }

/**
 * NOTE: Result != null.
 */
 public final Storage source()
 {
  return this.source;
 }

/**
 * NOTE: This method must be called internally after any change of
 * location. oldLocation must be >= 0.
 */
 protected final void notifyObservers(int oldLocation)
 {
  if (hasObservers())
   super.notifyObservers(this, new Integer(oldLocation));
 }

/**
 * NOTE: It is a dummy method which hides the same method in the
 * superclass.
 */
 public final void notifyObservers(MultiObservable observed,
         Object argument) {}

/**
 * NOTE: Called internally only from source observer agent
 * (source != null). If value at enumerator location is removed in
 * source then this location is set to the next location in source
 * (and the observers are notified with the argument of wrapped old
 * location). Must be synchronized outside with source.
 */
 protected void update(int prevLoc, int location)
 {
  if (location < 0 && this.location == -location)
  {
   if (prevLoc <= 0)
    prevLoc = -prevLoc;
   Storage source = this.source;
   try
   {
    prevLoc = source.nextLocation(prevLoc, true);
   }
   catch (IllegalArgumentException e)
   {
    prevLoc = 0;
   }
   if ((this.location = prevLoc) <= 0)
    source.removeObserver(this.agent);
   notifyObservers(-location);
  }
 }

 public final boolean hasMoreElements()
 {
  return this.location > 0;
 }

/**
 * NOTE: If no more values (elements) then NoSuchElementException is
 * thrown. Else the result (not null) is the current value and
 * location is set to next (the observers are notified with the
 * argument of wrapped old location). Must be synchronized outside
 * with source.
 */
 public Object nextElement()
  throws NoSuchElementException
 {
  int location;
  if ((location = this.location) <= 0)
   throw new NoSuchElementException();
  Storage source = this.source;
  Object value = source.getAt(location);
  if ((this.location = source.nextLocation(location, true)) <= 0)
   source.removeObserver(this.agent);
  notifyObservers(location);
  return value;
 }

/**
 * NOTE: If location != 0 and location is not valid then
 * IllegalArgumentException is thrown. Else location is changed (and
 * the observers are notified with the argument of wrapped old
 * location). Must be synchronized outside with source.
 */
 public void setLocation(int location)
  throws IllegalArgumentException
 {
  int oldLocation;
  if ((oldLocation = this.location) != location)
  {
   Storage source = this.source;
   if (location != 0 && !source.isValidAt(location))
    throw new IllegalArgumentException("location: " +
               Integer.toString(location));
   if (location <= 0)
    source.removeObserver(this.agent);
    else if (oldLocation <= 0)
     source.addObserver(this.agent);
   this.location = location;
   notifyObservers(oldLocation);
  }
 }

/**
 * NOTE: Result is the current location (valid or 0).
 */
 public final int location()
 {
  return this.location;
 }

/**
 * NOTE: Result is value at the current location. Result != null if
 * and only if location != 0.
 */
 public final Object get()
 {
  return this.source.getAt(this.location);
 }

/**
 * NOTE: If insertion is impossible here then storage remains
 * unchanged and ArrayStoreException is thrown. enumerator itself is
 * not modified. Alters storage state. Must be synchronized outside
 * with source.
 */
 public void insertAtPrevious(boolean isChild, Object value)
  throws ArrayStoreException
 {
  Storage source = this.source;
  int prevLoc = source.nextLocation(this.location, false);
  if (isChild)
   prevLoc = -prevLoc;
  source.insertAt(prevLoc, 0, value);
 }

/**
 * NOTE: value may be == null. Result is old value (not null). If
 * operation is impossible here then storage remains unchanged and
 * ArrayStoreException is thrown. enumerator itself is not modified.
 * Alters storage state. Must be synchronized outside with source.
 */
 public Object setAtPrevious(Object value)
  throws ArrayStoreException
 {
  Storage source = this.source;
  int prevLoc;
  if ((prevLoc = source.nextLocation(this.location, false)) <= 0)
   throw new ArrayStoreException(
              "location: 0, oldValue: null, value: " +
              (value != null ? value.toString() : "null"));
  return source.setAt(prevLoc, value);
 }

/**
 * NOTE: Must be synchronized outside with source.
 */
 public Object clone()
 {
  Object obj = null;
  try
  {
   obj = super.clone();
  }
  catch (CloneNotSupportedException e) {}
  if (obj == this || !(obj instanceof StorageEnumerator))
   throw new InternalError("CloneNotSupportedException");
  StorageEnumerator enumerator = (StorageEnumerator)obj;
  enumerator.agent = new StorageEnumeratorAgent(enumerator);
  if (enumerator.location > 0)
   enumerator.source.addObserver(enumerator.agent);
  return enumerator;
 }

/**
 * NOTE: source is not hashed.
 */
 public int hashCode()
 {
  return this.location;
 }

/**
 * NOTE: Must be synchronized outside with source.
 */
 public boolean equals(Object obj)
 {
  if (obj == this)
   return true;
  StorageEnumerator enumerator;
  if (!(obj instanceof StorageEnumerator) || (enumerator =
      (StorageEnumerator)obj).location != this.location)
   return false;
  return this.source.equals(enumerator.source);
 }

/**
 * NOTE: source is not represented in the result.
 */
 public String toString()
 {
  return Integer.toString(this.location);
 }

/**
 * NOTE: Check for integrity of this object. source storage is not
 * checked. Must be synchronized outside with source. For debug
 * purpose only.
 */
 public void integrityCheck()
 {
  super.integrityCheck();
  Storage source;
  if ((source = this.source) == null)
   throw new InternalError("source: null");
  int location;
  Notifiable agent;
  if ((agent = this.agent) == null)
   throw new InternalError("agent: null");
  if ((location = this.location) != 0 &&
      !source.isValidAt(location))
   throw new InternalError("location: " +
              Integer.toString(location));
  agent.integrityCheck();
 }

 private void readObject(ObjectInputStream in)
  throws IOException, ClassNotFoundException
 {
  in.defaultReadObject();
  Storage source;
  if ((source = this.source) == null)
   throw new InvalidObjectException("source: null");
  int location;
  if ((location = this.location) != 0 &&
      !source.isValidAt(location))
   throw new InvalidObjectException("location: " +
              Integer.toString(location));
  this.agent = new StorageEnumeratorAgent(this);
  if (location > 0)
   source.addObserver(this.agent);
 }
}

/**
 * NOTE: This helper class is used only inside StorageEnumerator.
 */
final class StorageEnumeratorAgent
 implements Notifiable
{

/**
 * NOTE: enumerator must be != null.
 */
 private final StorageEnumerator enumerator;

 protected StorageEnumeratorAgent(StorageEnumerator enumerator)
  throws NullPointerException
 {
  (this.enumerator = enumerator).equals(enumerator);
 }

 public void update(MultiObservable observed, Object argument)
 {
  StorageEvent event;
  StorageEnumerator enumerator;
  if (argument instanceof StorageEvent &&
      (enumerator = this.enumerator).source == observed &&
      (event = (StorageEvent)argument).source() == observed)
   enumerator.update(event.getPrevLoc(), event.getLocation());
 }

/**
 * NOTE: Check for integrity of this object. enumerator itself is
 * not checked. For debug purpose only.
 */
 public void integrityCheck()
 {
  if (this.enumerator == null)
   throw new InternalError("enumerator: null");
 }
}
