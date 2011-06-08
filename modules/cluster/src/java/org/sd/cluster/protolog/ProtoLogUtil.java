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


import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Auxiliary utility methods for working with proto buffers and logs.
 * <p>
 * @author Spence Koehler
 */
public class ProtoLogUtil {

  /**
   * Apply the Message.parseFrom method to the given bytes.
   *
   * @param bytes   The message's bytes.
   * @param parseMethod   The parse method to reconstruct the message.
   *
   * @return the reconstructed message.
   */
  public static final Message parseMessage(byte[] bytes, Method parseMethod) throws InvalidProtocolBufferException {
    Message result = null;

    try {
      result = (Message)parseMethod.invoke(null, bytes);
    }
    catch (IllegalAccessException e) {
      throw new IllegalArgumentException(e);
    }
    catch (InvocationTargetException e) {
      throw new IllegalArgumentException(e);
    }

    return result;
  }

  /**
   * Decode a file's timestamp given a pattern to extract the timestamp
   * from its name (in group 1) and a dateFormat to parse the date.
   *
   * @return the date or null if indecipherable.
   */
  public static final Date decodeFileTimestamp(File file, Pattern filenamePattern, DateFormat dateFormat) {
    Date result = null;

    final String name = file.getName();
    final Matcher m = filenamePattern.matcher(name);
    if (m.matches()) {
      try {
        result = dateFormat.parse(m.group(1));
      }
      catch (ParseException e) {
        //eat exception, returning null
      }
    }

    return result;
  }

  /**
   * Get the parseFrom(byte[]) method from the message class.
   *
   * @param messageClass  The message class to reconstruct.
   *
   * @return the message class's parseFrom(byte[]) method.
   */
  public static final Method getParseMethod(Class<? extends Message> messageClass) {
    Method result = null;

    try {
      result = messageClass.getMethod("parseFrom", byte[].class);
    }
    catch (NoSuchMethodException e) {
      throw new IllegalArgumentException(e);
    }

    return result;
  }

  /**
   * Given a field string of the form "a.b.c..." and getters for the instance
   * such that ((instance.getA()).getB()).getC()... yields a valid result, get
   * that result or return null.
   * <p>
   * Note that this method will not "explode" intermediate results that are
   * collections (or lists) but the final result could be a collection.
   * <p>
   * Also, this represents an inefficient mechanism for accessing information
   * and should only be used when reflection, as opposed to direct access, is
   * necessitated by circumstance.
   *
   * @return the retrieved result or null.
   */
  public static final Object getField(Object instance, String field) {

    if (field == null || "".equals(field)) return null;

    final String[] pieces = field.split("\\.");
    for (String piece : pieces) {
      if (instance != null) {
        instance = getSimpleField(piece, instance);
      }
    }

    return instance;
  }

  /**
   * Given a field string of the form "a.b.c..." and getters for the instance
   * such that ((instance.getA()).getB()).getC()... yields a valid result, get
   * that result as a repeated field or return null.
   * <p>
   * Note that if the field being accessed is not repeated, the single available
   * item will be wrapped within the returned array.
   */
  public static final Object[] getRepeatingField(Object instance, String field) {
    Object[] result = null;

    final Object object = ProtoLogUtil.getField(instance, field);

    if (object != null) {
      if (object instanceof List) {
        final List list = (List)object;
        result = new Object[list.size()];
        int i = 0;
        for (Object o : list) {
          result[i++] = o;
        }
      }
      else {
        result = new Object[]{object};
      }
    }

    return result;
  }

  /**
   * Do the work of getting the simple field's value from the instance.
   */
  private static final Object getSimpleField(String simplefield, Object instance) {
    Object result = null;

    final Method getMethod = getGetMethod(instance.getClass(), simplefield);
    if (getMethod != null) {
      try {
        result = getMethod.invoke(instance);
      }
      catch (IllegalAccessException e) {
        throw new IllegalArgumentException(e);
      }
      catch (InvocationTargetException e) {
        throw new IllegalArgumentException(e);
      }
    }

    return result;
  }

  private static final Method getGetMethod(Class<?> clazz, String field) {
    Method result = null;

    final StringBuilder methodName = new StringBuilder();

    // try to find a "getX" method
    methodName.
      append("get").
      append(Character.toUpperCase(field.charAt(0))).
      append(field.substring(1));

    try {
      result = (Method)clazz.getMethod(methodName.toString());
    }
    catch (NoSuchMethodException e) {
      // leave result as null
    }

    if (result == null) {
      // try to find a "getXList" method
      methodName.append("List");

      try {
        result = (Method)clazz.getMethod(methodName.toString());
      }
      catch (NoSuchMethodException e) {
        // leave result as null
      }
    }

    return result;
  }
}

