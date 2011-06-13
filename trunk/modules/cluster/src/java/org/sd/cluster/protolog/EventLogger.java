/*
    Copyright 2011 Semantic Discovery, Inc. (www.semanticdiscovery.com)

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
package org.sd.cluster.protolog;


import org.sd.cluster.protolog.codegen.ProtoLogProtos.*;

/**
 * Utility to log start, succeed, error, kill, and timeout events as
 * org.sd.cluster.protolog.codegen.ProtoLogProtos.EventEntry messages.
 * <p>
 * @author Spence Koehler
 */
public abstract class EventLogger {

  protected EventLogger() {
  }

  /**
   * Create an EventEntry.Builder with the current timestamp and the given
   * eventID info and type on which to optionally set a message and/or
   * attributes.
   */
  public static final EventEntry.Builder createEventEntry(long eventId, String who, String what, EventEntry.EventType eventType) {
    return createEventEntry(eventId, System.currentTimeMillis(), who, what, eventType);
  }

  /**
   * Create an EventEntry.Builder with the given timestamp and the given
   * eventID info and type on which to optionally set a message and/or
   * attributes.
   */
  public static final EventEntry.Builder createEventEntry(long eventId, long timestamp, String who, String what, EventEntry.EventType eventType) {
    final EventEntry.Builder result = EventEntry.newBuilder();

    // Set the timestamp
    result.setTimestamp(timestamp);

    // Set the eventID
    final EventId.Builder eventIdBuilder = EventId.newBuilder();
    eventIdBuilder.setId(eventId);
    eventIdBuilder.setWho(who);
    eventIdBuilder.setWhat(what);
    result.setId(eventIdBuilder);

    // Set the eventType
    result.setType(eventType);

    return result;
  }
  
  /**
   * Get the event entry's bytes for logging, performing any actions indicated
   * within the implementation.
   */
  public abstract byte[] logEventEntry(EventEntry.Builder eventEntry);

  /**
   * Close this event logger.
   */
  public abstract void close();
}
