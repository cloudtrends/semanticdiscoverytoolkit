/*
    Copyright 2009 Semantic Discovery, Inc. (www.semanticdiscovery.com)

    This file is part of the Semantic Discovery Toolkit.

    The Semantic Discovery Toolkit is free software: you can redistribute it and/or modify
    it under the terms of the GNU Lesser General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    The Semantic Discovery Toolkit is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Lesser General Public License for more details.

    You should have received a copy of the GNU Lesser General Public License
    along with The Semantic Discovery Toolkit.  If not, see <http://www.gnu.org/licenses/>.
*/
package org.sd.cluster.job;


import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;

/**
 * A buffer around a work factory for looking for specific work.
 * <p>
 * @author Spence Koehler
 */
public class WorkBuffer {
  
  private WorkFactory workFactory;
  private int bufferSize;

  private LinkedList<UnitOfWork> buffer;
  private Set<String> keys;

  private final Object bufferMutex = new Object();
  private final Object keysMutex = new Object();

  public WorkBuffer(WorkFactory workFactory, int bufferSize) throws IOException {
    this.workFactory = workFactory;
    this.bufferSize = bufferSize < 1 ? 1 : bufferSize;

    this.buffer = new LinkedList<UnitOfWork>();
    this.keys = new HashSet<String>();

    loadBuffer();
  }

  private final void loadBuffer() throws IOException {
    while (buffer.size() < bufferSize) {
      if (!loadNext()) break;
    }
  }

  private final boolean loadNext() throws IOException {
    boolean result = false;

    if (!workFactory.isComplete()) {
      final UnitOfWork workUnit = workFactory.getNext();
      if (workUnit != null) {

        result = true;

        synchronized (bufferMutex) {
          buffer.addLast(workUnit);
        }
        
        final String key = getKey(workUnit);
        if (key != null) {
          synchronized (keysMutex) {
            keys.add(key);
          }
        }
      }
    }

    return result;
  }

  public UnitOfWork getNext(String keyHint) throws IOException {
    UnitOfWork result = null;

    if (keyHint != null && keys.contains(keyHint)) {
      synchronized (bufferMutex) {
        for (Iterator<UnitOfWork> iter = buffer.iterator(); iter.hasNext(); ) {
          final UnitOfWork workUnit = iter.next();
          final String key = getKey(workUnit);
          if (key != null && keyHint.equals(key)) {
            iter.remove();
            result = workUnit;
          }
        }
      }

      if (result == null) {
        synchronized (keysMutex) {
          keys.remove(keyHint);
        }
      }
    }

    if (result == null) {
      if (buffer.size() > 0) {
        synchronized (bufferMutex) {
          result = buffer.removeFirst();
        }
      }
    }

    if (result != null) {
      while (buffer.size() < bufferSize) {
        if (!loadNext()) break;
      }
    }

    return result;
  }

  public void addToFront(UnitOfWork workUnit) {
    workFactory.addToFront(workUnit);
  }

  public void addToBack(UnitOfWork workUnit) {
    workFactory.addToBack(workUnit);
  }

  public boolean isComplete() {
    return workFactory.isComplete() && buffer.size() == 0;
  }

  public long getRemainingEstimate() {
    return workFactory.getRemainingEstimate() + buffer.size();
  }

  public void close() throws IOException {
    workFactory.close();
  }

  //assuming work units are of form "/mnt/<nodename>/..."  ...generalize when needed.
  protected String getKey(UnitOfWork workUnit) {
    String result = null;

    final String value = ((StringUnitOfWork)workUnit).getString();
    final int slashPos = value.indexOf('/', 5);
    if (slashPos > 5) {
      result = value.substring(5, slashPos).toLowerCase();
    }

    return result;
  }
}
