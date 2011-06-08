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


import com.google.protobuf.Message;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Utility class to iterate through messages within multiple logs that are
 * within a time range.
 * <p>
 * @author Spence Koehler
 */
public class MultiLogIterator implements Iterator<Message> {

  private File logDir;
  private Class<? extends Message> messageClass;
  private ProtoLogStreamer streamer;
  private int maxMessageBytes;
  private Pattern filenamePattern;
  private DateFormat dateFormat;
  private Long minTime;
  private Long maxTime;

  private List<File> _logFiles;
  private Iterator<File> _logFileIterator;
  private File _logFile;
  private LogIterator _logIterator;
  private Message _nextMessage;

  /**
   * Construct an unbounded collector over the logs.
   *
   * @param logDir  The log directory (or file) from which to collect.
   * @param messageClass  The class of the messages to retrieve.
   * @param streamer  The ProtoLogStreamer to use to deserialize messages.
   * @param maxMessageBytes  The maximum number of message bytes used while serializing.
   */
  public MultiLogIterator(File logDir, Class<? extends Message> messageClass, ProtoLogStreamer streamer, int maxMessageBytes) {
    this(logDir, messageClass, streamer, maxMessageBytes, null, null, null, null);
  }

  /**
   * Construct a collector over the logs within the time range.
   *
   * @param logDir  The log directory (or file) from which to collect.
   * @param messageClass  The class of the messages to retrieve.
   * @param streamer  The ProtoLogStreamer to use to deserialize messages.
   * @param maxMessageBytes  The maximum number of message bytes used while serializing.
   * @param filenamePattern   A pattern over a filename to extract the timestamp (in group 1).
   * @param dateFormat  The DateFormat instance to parse the date.
   * @param minTime   The minimum time (inclusive) for messages to collect (unbounded if null).
   * @param maxTime   The maximum time (inclusive) for messages to collect (unbounded if null).
   */
  public MultiLogIterator(File logDir, Class<? extends Message> messageClass,
                          ProtoLogStreamer streamer, int maxMessageBytes,
                          Pattern filenamePattern, DateFormat dateFormat,
                          Long minTime, Long maxTime) {
    this.logDir = logDir;
    this.messageClass = messageClass;
    this.streamer = streamer;
    this.maxMessageBytes = maxMessageBytes;
    this.filenamePattern = filenamePattern;
    this.dateFormat = dateFormat;
    this.minTime = minTime;
    this.maxTime = maxTime;

    this._logFiles = null;
    this._logFileIterator = null;
    this._logFile = null;
    this._logIterator = null;
    this._nextMessage = null;
  }

  /**
   * Retrieve the root log directory.
   */
  public File getLogDir() {
    return logDir;
  }

  /**
   * Retrieve the message class.
   */
  public Class<? extends Message> getMessageClass() {
    return messageClass;
  }

  /**
   * Get the current log file being iterated over.
   *
   * @return the current log file, or null if not started.
   */
  public File getLogFile() {
    return _logFile;
  }

  /**
   * Determine whether there is another message to retrieve.
   *
   * @return true if there is another message; otherwise, false.
   */
  public boolean hasNext() {
    return (getNextMessage() != null);
  }

  /**
   * Get the next message from the stream.
   *
   * @return the next message or null if the end has been reached.
   */
  public Message next() {
    Message result = getNextMessage();
    this._nextMessage = readNextMessage();
    return result;
  }

  /**
   * Throws an UnsupportedOperationException.
   */
  public void remove() {
    throw new UnsupportedOperationException("Not supported.");
  }

  /**
   * Get this instance's DateFormat.
   */
  public DateFormat getDateFormat() {
    return dateFormat;
  }

  /**
   * Close resources associated with this instance (terminates iteration).
   */
  public void close() {
    if (_logIterator != null) {
      try {
        _logIterator.close();
      }
      catch (IOException eat) {}
      _logIterator = null;
    }
  }

  /**
   * Overridable method for selecting a file to process.
   */
  protected boolean selectFile(File file) {
    return true;
  }

  /**
   * Lazily get the nextMessage.
   */
  private final Message getNextMessage() {
    if (_nextMessage == null && _logFiles == null) {
      initialize();
      _nextMessage = readNextMessage();
    }
    return _nextMessage;
  }

  /**
   * Do the work of reading the next message.
   *
   * @return the next message or null.
   */
  private final Message readNextMessage() {
    Message result = null;

    while (_logIterator != null) {

      boolean keepGoing = true;

      // get the next (in range) message from the current _logIterator
      while (_logIterator.hasNext()) {
        result = _logIterator.next();

        final int rangeFlag = isInRange(result);

        if (rangeFlag == 0) {
          break;  // we've found our next message!
        }
        else if (rangeFlag < 0) {   // we haven't incremented up to into the correct range yet
          result = null;  // loop until we're in range
        }
        else { // we're beyond the valid range
          result = null;
          keepGoing = false;
          break;
        }
      }

      if (result != null) {
        break;  // found the next message!
      }
      else if (!keepGoing) {
        // we're beyond the valid range. time to quit iterating altogether.
        _logIterator = null;
        break;
      }

      // still haven't found the next message
      _logIterator = getNextLogIterator();  // inc and loop
    }

    return result;
  }

  /**
   * Initialize all lazily loaded data.
   */
  private final void initialize() {
    this._logFiles = new ArrayList<File>();

    // load _logFiles with only appropriate (in range) files
    final File[] logfiles = logDir.isDirectory() ?
      logDir.listFiles(new FileFilter() {
          public boolean accept(File pathname) {
            return pathname.isDirectory() || selectFile(pathname);
          }
        }) : new File[]{logDir};

    for (int i = 0; i < logfiles.length; ++i) {
      if (couldHaveRange(logfiles, i)) {
        this._logFiles.add(logfiles[i]);
      }
    }

    // set up _logFileIterator over _logFiles
    this._logFileIterator = this._logFiles.iterator();

    // get first _logIterator
    this._logIterator = getNextLogIterator();
  }

  /**
   * Determine whether the logfile at the given index could have messages
   * in the current time range.
   * <p>
   * Protected access for junit testing.
   */
  protected final boolean couldHaveRange(File[] logfiles, int index) {
    boolean result = true;  // in range unless proven to be out of range.

    if (filenamePattern == null || dateFormat == null) return result;

    final File logfile = logfiles[index];
    final Date date = ProtoLogUtil.decodeFileTimestamp(logfile, filenamePattern, dateFormat);

    if (date != null) {
      final long time = date.getTime();
      final int rangeFlag = isInRange(time);
      if (rangeFlag > 0) {  // beyond the range
        result = false;
      }
      else if (rangeFlag < 0) {   // before the range
        // could not have entries within the range if the next log file is
        // also before or starts at the lower bound of the range
        if (index + 1 < logfiles.length) {
          final File nextlogfile = logfiles[index + 1];
          final Date nextdate = ProtoLogUtil.decodeFileTimestamp(nextlogfile, filenamePattern, dateFormat);
          if (nextdate != null) {
            final long nextTime = nextdate.getTime();
            final int nextRangeFlag = isInRange(nextTime);
            if (nextRangeFlag < 0) {
              result = false;
            }
            else if (nextRangeFlag == 0) {
              // only proves logfile isn't in range if the timestamp is AT the minTime
              if (minTime != null && minTime.equals(nextTime)) {
                result = false;
              }
            }
          }
        }
      }
      //else is in range
    }

    return result;
  }

  /**
   * Determine whether the given timestamp is in range.
   * <p>
   * Protected access for junit testing.
   */
  protected final int isInRange(long timestamp) {
    int result = 0;   // in range unless proven to be out of range.

    if (minTime != null && timestamp < minTime) {   // below the minimum time
      result = -1;
    }
    else { // above the minimum time
      if (maxTime != null && timestamp > maxTime) {   // above the maximum time
        result = 1;
      }
      // else, below the maximum time == in range.
    }

    return result;
  }

  /**
   * Determine whether the message's timestamp is in range.
   *
   * @return -1 if before the range; 0 if in range; 1 if after the range.
   */
  private final int isInRange(Message message) {
    int result = 0;   // in range unless proven otherwise

    final Object object = ProtoLogUtil.getField(message, "timestamp");
    if (object != null && object instanceof Long) {
      result = isInRange((Long)object);
    }

    return result;
  }

  /**
   * Get the next log iterator based on _logFileIterator's next file.
   *
   * @return the next log iterator or null.
   */
  private final LogIterator getNextLogIterator() {
    LogIterator result = null;

    try {
      if (_logIterator != null) {
        _logIterator.close();
      }

      if (_logFileIterator.hasNext()) {
        this._logFile = _logFileIterator.next();
        final FileInputStream fis = new FileInputStream(_logFile);
        result = new LogIterator(new DataInputStream(fis), messageClass, streamer, maxMessageBytes);
      }
    }
    catch (IOException e) {
      throw new IllegalStateException(e);
    }

    return result;
  }
}

