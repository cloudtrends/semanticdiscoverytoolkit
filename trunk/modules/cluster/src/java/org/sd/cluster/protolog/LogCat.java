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
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Pattern;
import org.sd.util.PropertiesParser;

/**
 * Utility to 'cat' lumberjack log files.
 * <p>
 * @author Spence Koehler
 */
public class LogCat {

  private MultiLogIterator messageIterator;
  private PrintStream out;

  public LogCat(MultiLogIterator messageIterator, PrintStream out) {
    this.messageIterator = messageIterator;
    this.out = out;
  }

  public final void doCat() {
    // can only do this once per iterator
    if (messageIterator != null) {
      int messageNum = 0;
      while (messageIterator.hasNext()) {
        final Message message = messageIterator.next();
        catMessage(out, message, messageNum++, messageIterator.getLogFile());
      }
      messageIterator.close();
      messageIterator = null;
    }
  }

  /**
   * Reset this instance's message iterator.
   */
  public void setMessageIterator(MultiLogIterator messageIterator) {
    this.messageIterator = messageIterator;
  }

  /**
   * Overridable method for formatting the contents of a message.
   */
  protected void catMessage(PrintStream out, Message message, int messageNum, File logfile) {
    out.println(message.toString());
  }

  /**
   * Utility method for potential use from within a catMessage implementation
   * that grabs the contents of (non-repeating) log message fields as identified
   * by dot-delimited strings.
   * <p>
   * For fields that don't generate a result, the values in the array will be
   * null.
   */
  protected final String[] getFields(Message message, String[] fields) {
    final String[] result = new String[fields.length];

    int i = 0;
    for (String field : fields) {
      final Object value = ProtoLogUtil.getField(message, field);
      result[i++] = value == null ? null : value.toString();
    }

    return result;
  }

  /**
   * Utility method for potential use from within a catMessage implementation
   * that grabs the contents of a repeating log message field as identified
   * by the dot-delimited string.
   */
  protected final String[] getRepeatingField(Message message, String field) {
    String[] result = null;

    final Object[] objects = ProtoLogUtil.getRepeatingField(message, field);

    if (objects != null) {
      result = new String[objects.length];

      for (int i = 0; i < objects.length; ++i) {
        result[i] = objects[i] == null ? null : objects[i].toString();
      }
    }

    return result;
  }

  /**
   * Utility method for potential use from within a catMessage implementation
   * that grabs the contents of a repeating log message field (attrField) that
   * yields another Message class containing a keyField and a valueField to
   * extract into a map.
   */
  protected final Map<String, String> getAttributes(Message message, String attrField, String keyField, String valueField) {
    final Map<String, String> result = new HashMap<String, String>();

    final Object[] objects = ProtoLogUtil.getRepeatingField(message, attrField);
    for (Object object : objects) {
      final Object keyObject = ProtoLogUtil.getField(object, keyField);
      if (keyObject != null) {
        final Object valueObject = ProtoLogUtil.getField(object, valueField);
        if (valueObject != null) {
          result.put(keyObject.toString(), valueObject.toString());
        }
      }
    }

    return result;
  }

  /**
   * Decode a file's timestamp given a pattern to extract the timestamp
   * from its name (in group 1) and a dateFormat to parse the date.
   */
  protected final long decodeFileTimestamp(File file, Pattern filenamePattern, DateFormat dateFormat) {
    long result = -1L;

    final Date date = ProtoLogUtil.decodeFileTimestamp(file, filenamePattern, dateFormat);
    if (date != null) {
      result = date.getTime();
    }

    return result;
  }


//java -Xmx640m -cp `cpgen /home/sbk/co/lingosuite/trunk/modules/logplugins` lingotek.lumberjack.util.LogCat /home/sbk/co/lingosuite/trunk/modules/logplugins/src/conf/translator-action-log.document-event.properties /usr/local/share/data/lingotek/lingosuite/logs/translator/doc/docevent.2009-10-09.15:20:36.342.log

  /**
   * Auxiliary to iterate over the messages in log files, printing them to
   * stdout.
   * <p>
   * Properties:
   * <ul>
   * <li>messageClass -- (required) the classpath for the message instances in the log
   *     (i.e. 'mypackage.MyProtoWrapper$MyMessageType')</li>
   * <li>dateFormat -- (optional) the simple date format string used for the log files
   *     (i.e. 'yyyy-MM-dd.kk:mm:ss.SSS')</li>
   * <li>filenamePattern -- (optional) pattern for extracting the date from a log filename
   *     (i.e. ".*?\\.(.*)\\.log")</li>
   * <li>fields -- (optional if have afields) semicolon-delimited fields to extract from messages</li>
   * <li>afields -- (optional if have fields) semicolon-delimited attribute fields to extract from messages</li>
   * <li>kfield -- (optional, default='key') name of key field for attributes</li>
   * <li>vfield -- (optional, default='value') name of value field for attributes</li>
   * <li>minTime -- (optional) minimum time in millis of messages to consider, default is unbounded</li>
   * <li>maxTime -- (optional) maximum time in millis of messages to consider, default is unbounded</li>
   * </ul>
   * Args:
   * <ul>
   * <li>args hold the paths to log files and/or diretories to dump.</li>
   * </ul>
   */
  @SuppressWarnings("unchecked")
  public static final void main(String[] args) throws Exception {
    final PropertiesParser pp = new PropertiesParser(args);
    final Properties properties = pp.getProperties();
    args = pp.getArgs();

    // extract properties
    final String messageClassName = properties.getProperty("messageClass");
    final String dateFormatString = properties.getProperty("dateFormat", "yyyy-MM-dd.kk:mm:ss.SSS");
    final String filenamePatternString = properties.getProperty("filenamePattern", ".*?\\.(.*)\\.log");
    final String fieldsString = properties.getProperty("fields");
    final String afieldsString = properties.getProperty("afields");
    final String kfield = properties.getProperty("kfield", "key");
    final String vfield = properties.getProperty("vfield", "value");
    final String minTimeString = properties.getProperty("minTime");
    final String maxTimeString = properties.getProperty("maxTime");
    final boolean echoProperties = "true".equalsIgnoreCase(properties.getProperty("echoProperties", "false"));

    // validate properties
    boolean valid = true;
    if (messageClassName == null) {
      System.err.println("Must specify 'messageClass'");
      valid = false;
    }
    if (fieldsString == null && afieldsString == null) {
      System.err.println("Must specify 'fields' or 'afields'");
      valid = false;
    }
    if (!valid) System.exit(1);

    // echo properties
    if (echoProperties) {
      System.err.println("messageClass=" + messageClassName);
      System.err.println("dateFormat=" + dateFormatString);
      System.err.println("filenamePattern=" + filenamePatternString);
      System.err.println("fields=" + fieldsString);
      System.err.println("afields=" + afieldsString);
      System.err.println("kfield=" + kfield);
      System.err.println("vfield=" + vfield);
      System.err.println("minTime=" + minTimeString);
      System.err.println("maxTime=" + maxTimeString);
    }

    // transform property values
    final Class<Message> messageClass = (Class<Message>)Class.forName(messageClassName);
    final SimpleDateFormat dateFormat = new SimpleDateFormat(dateFormatString);
    final Pattern filenamePattern = Pattern.compile(filenamePatternString);
    final String[] fields = fieldsString.split(";");
    final String[] afields = afieldsString.split(";");
    final Long minTime = minTimeString == null || "".equals(minTimeString) ? null : Long.parseLong(minTimeString);
    final Long maxTime = maxTimeString == null || "".equals(maxTimeString) ? null : Long.parseLong(maxTimeString);


    // args0+: proto log files
    for (int i = 0; i < args.length; ++i) {
      final File argFile = new File(args[i]);
      final int argFileLen = argFile.getAbsolutePath().length();
      final MultiLogIterator messageIterator =
        new MultiLogIterator(argFile, messageClass, ProtoLogStreamer.DEFAULT_INSTANCE, ProtoLogStreamer.MAX_MESSAGE_BYTES, filenamePattern, dateFormat, minTime, maxTime);
      final LogCat logCat = new LogCat(messageIterator, System.out) {
          protected void catMessage(PrintStream out, Message message, int messageNum, File logFile) {
            final StringBuilder result = new StringBuilder();

            result.
              append(logFile.getAbsolutePath().substring(argFileLen)).  // col0: add filename sans path to root
              append('\t').
              append(messageNum);                                        // col1: message number within its file

            final String[] fieldValues = getFields(message, fields);
            for (String fieldValue : fieldValues) {
              result.
                append('\t').
                append(fieldValue == null ? "" : fieldValue);           // col2+: fields in order
            }

            for (String afield : afields) {
              final Map<String, String> attrs = getAttributes(message, afield, kfield, vfield);
              for (Map.Entry<String, String> entry : attrs.entrySet()) {
                result.
                  append('\t').
                  append(entry.getKey()).
                  append('=').
                  append(entry.getValue());                             // colN+: attributes
              }
            }

            out.println(result.toString());
          }
        };

      logCat.doCat();
    }
  }
}

