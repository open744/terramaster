/*
 * @(#) src/net/sf/ivmaidns/storage/FilterStorage.java --
 * Abstract class for filter storage.
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

import net.sf.ivmaidns.util.MultiObservable;
import net.sf.ivmaidns.util.Notifiable;

/**
 * Abstract class for filter storage (virtual storage).
 **
 * @version 2.0
 * @author Ivan Maidanski
 **
 * @since 1.2
 */
public abstract class FilterStorage extends Storage
 implements Serializable
{

/**
 * The class version unique identifier for serialization
 * interoperability.
 **
 * @since 1.2
 */
 private static final long serialVersionUID = 6628976016725372247L;

/**
 * NOTE: Filter storage must be synchronized with source if
 * source != null.
 */
 protected Storage source;

/**
 * NOTE: agent must be != null if and only if source != null. agent
 * should not be accessible outside.
 */
 private transient Notifiable agent;

 public FilterStorage() {}

/**
 * NOTE: source may be == null. If source == this then source is set
 * to null. If source == oldSource then nothing more is performed.
 * Proper observers notifications are performed. Must be
 * synchronized outside with oldSource (if not null) and with source
 * (if not null). When this storage is not needed any more,
 * setSource(null) should be called to stop observing of source
 * (if not null).
 */
 public void setSource(Storage source)
 {
  if (source == this)
   source = null;
  Storage oldSource = this.source;
  if (oldSource != source)
  {
   int location, prevLoc;
   if (oldSource != null)
   {
    location = 0;
    if (hasObservers())
     do
     {
      try
      {
       if ((location = nextLocation(location, false)) <= 0)
        break;
       if ((prevLoc = siblingLocation(location, false)) <= 0)
        prevLoc = -parentLocation(location);
      }
      catch (IllegalArgumentException e)
      {
       break;
      }
      notifyObservers(prevLoc, -location, getAt(location));
     } while (true);
    oldSource.removeObserver(this.agent);
   }
   if ((this.source = source) != null)
   {
    if (oldSource == null)
     this.agent = new FilterStorageAgent(this);
    source.addObserver(this.agent);
    location = 0;
    if (hasObservers())
     do
     {
      try
      {
       if ((location = nextLocation(location, true)) <= 0)
        return;
      }
      catch (IllegalArgumentException e)
      {
       break;
      }
      notifyObservers(0, location, null);
     } while (true);
   }
    else this.agent = null;
  }
  oldSource = null;
 }

/**
 * NOTE: Called internally only from source observer agent
 * (source != null). Observers notification is performed (if
 * needed). Must be synchronized outside with source. Should be
 * protected.
 */
 protected void update(int prevLoc, int location, Object oldValue)
 {
  notifyObservers(prevLoc, location, oldValue);
 }

 protected void minimizeCapacity() {}

 public final int emptyLocation()
 {
  int emptyLoc = 1;
  Storage source;
  if ((source = this.source) != null)
   emptyLoc = source.emptyLocation();
  return emptyLoc;
 }

/**
 * NOTE: Observers notification is performed. Must be synchronized
 * outside with source if not null.
 */
 public int insertAt(int prevLoc, int emptyLocation, Object value)
  throws IllegalArgumentException, ArrayStoreException
 {
  Storage source;
  if ((source = this.source) != null)
   return source.insertAt(prevLoc, emptyLocation, value);
  if (prevLoc != 0)
   throw new IllegalArgumentException("prevLoc: " +
              Integer.toString(prevLoc));
  if (emptyLocation < 0)
   throw new IllegalArgumentException("emptyLocation: " +
              Integer.toString(emptyLocation));
  throw new ArrayStoreException("prevLoc: 0, emptyLocation: " +
             Integer.toString(emptyLocation) + ", value: " +
             (value != null ? value.toString() : "null"));
 }

/**
 * NOTE: Observers notification is performed. Must be synchronized
 * outside with source if not null.
 */
 public Object setAt(int location, Object value)
  throws IllegalArgumentException, ArrayStoreException
 {
  Storage source;
  if ((source = this.source) != null)
   return source.setAt(location, value);
  if (location <= 0)
   throw new IllegalArgumentException("location: " +
              Integer.toString(location));
  if (value == null)
   return null;
  throw new ArrayStoreException("location: " +
             Integer.toString(location) +
             ", oldValue: null, value: " + value.toString());
 }

/**
 * NOTE: Not all non-empty locations may be valid.
 */
 public final Object getAt(int location)
 {
  Object value = null;
  Storage source;
  if ((source = this.source) != null)
   value = source.getAt(location);
  return value;
 }

/**
 * NOTE: Must be synchronized outside with source if not null.
 */
 public boolean isValidAt(int location)
 {
  Storage source;
  return (source = this.source) != null &&
   source.isValidAt(location);
 }

/**
 * NOTE: Must be synchronized outside with source if not null.
 */
 public int childLocation(int parentLocation, boolean forward)
  throws IllegalArgumentException
 {
  Storage source;
  if ((source = this.source) != null)
   parentLocation = source.childLocation(parentLocation, forward);
   else if (parentLocation != 0)
    throw new IllegalArgumentException("location: " +
               Integer.toString(parentLocation));
  return parentLocation;
 }

/**
 * NOTE: Must be synchronized outside with source if not null.
 */
 public int siblingLocation(int location, boolean forward)
  throws IllegalArgumentException
 {
  Storage source;
  if ((source = this.source) != null)
   return source.siblingLocation(location, forward);
  throw new IllegalArgumentException("location: " +
             Integer.toString(location));
 }

/**
 * NOTE: Must be synchronized outside with source if not null.
 */
 public int parentLocation(int location)
  throws IllegalArgumentException
 {
  Storage source;
  if ((source = this.source) != null)
   return source.parentLocation(location);
  throw new IllegalArgumentException("location: " +
             Integer.toString(location));
 }

/**
 * NOTE: Must be synchronized outside with source if not null.
 */
 public int locationOf(Object value, int prevLocation,
         boolean forward)
  throws IllegalArgumentException
 {
  Storage source;
  if ((source = this.source) != null)
   prevLocation = source.locationOf(value, prevLocation, forward);
   else if (prevLocation != 0)
    throw new IllegalArgumentException("location: " +
               Integer.toString(prevLocation));
  return prevLocation;
 }

/**
 * NOTE: Must be synchronized outside with source if not null.
 */
 public int findLessGreater(Object value, boolean greater,
         int prevLocation, boolean forward)
  throws IllegalArgumentException
 {
  Storage source;
  if ((source = this.source) != null)
   prevLocation = source.findLessGreater(value,
    greater, prevLocation, forward);
   else if (prevLocation != 0)
    throw new IllegalArgumentException("location: " +
               Integer.toString(prevLocation));
  return prevLocation;
 }

/**
 * NOTE: Must be synchronized outside with source if not null.
 */
 public Object clone()
 {
  FilterStorage filter = (FilterStorage)super.clone();
  Storage source = filter.source;
  filter.source = null;
  filter.setSource(source);
  return filter;
 }

/**
 * NOTE: Shallow check for integrity of this object. source storage
 * is not checked. Must be synchronized outside with source if not
 * null. For debug purpose only.
 */
 public void integrityCheck()
 {
  super.integrityCheck();
  Storage source;
  if ((source = this.source) == this)
   throw new InternalError("source: this");
  Notifiable agent = this.agent;
  if ((source != null) != (agent != null))
   throw new InternalError("source/agent");
  if (agent != null)
   agent.integrityCheck();
 }

 private void readObject(ObjectInputStream in)
  throws IOException, ClassNotFoundException
 {
  in.defaultReadObject();
  Storage source;
  if ((source = this.source) == this)
   throw new InvalidObjectException("source: this");
  this.source = null;
  setSource(source);
 }
}

/**
 * NOTE: This helper class is used only inside FilterStorage.
 */
final class FilterStorageAgent
 implements Notifiable
{

/**
 * NOTE: filter must be != null.
 */
 private final FilterStorage filter;

 protected FilterStorageAgent(FilterStorage filter)
  throws NullPointerException
 {
  (this.filter = filter).equals(filter);
 }

 public void update(MultiObservable observed, Object argument)
 {
  StorageEvent event;
  FilterStorage filter;
  if (argument instanceof StorageEvent &&
      (filter = this.filter).source == observed &&
      (event = (StorageEvent)argument).source() == observed)
   filter.update(event.getPrevLoc(), event.getLocation(),
    event.getOldValue());
 }

/**
 * NOTE: Check for integrity of this object. filter storage is not
 * checked. For debug purpose only.
 */
 public void integrityCheck()
 {
  if (this.filter == null)
   throw new InternalError("filter: null");
 }
}
