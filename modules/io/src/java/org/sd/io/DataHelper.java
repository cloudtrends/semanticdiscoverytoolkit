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
package org.sd.io;


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;

/**
 * General utilities for helping with messages.
 * <p>
 * @author Spence Koehler
 */
public class DataHelper {
  
  public static final Charset UTF_8 = Charset.forName("UTF-8");

  public static void writeString(DataOutput dataOutput, String string) throws IOException {
    if (string == null) {
      writeBytes(dataOutput, null);
    }
    else {
      final byte[] bytes = string.getBytes(UTF_8);
      writeBytes(dataOutput, bytes);
    }
  }

  /**
   * Get a string's publishable bytes.
   */
  public static final byte[] getStringBytes(String string) throws IOException {
    final ByteArrayOutputStream bytesOut = new ByteArrayOutputStream();
    final DataOutputStream dataOut = new DataOutputStream(bytesOut);

    // serialize
    writeString(dataOut, string);

    dataOut.close();
    final byte[] result = bytesOut.toByteArray();
    bytesOut.close();

    return result;
  }

  /**
   * Get the number of bytes needed to serialize this string.
   * <p>
   * The amount of overhead is the size of an int plus the number of bytes
   * to store the string.
   */
  public static final int numOverheadBytes(String string) {
    int result = 4;  // size of int

    if (string != null) {
      result += string.getBytes(UTF_8).length;
    }

    return result;
  }

  public static String readString(DataInput dataInput) throws IOException {
    String result = null;

    final byte[] bytes = readBytes(dataInput);
    if (bytes != null) {
      result = new String(bytes, UTF_8);
    }
    return result;
  }

  public static String readString(DataInput dataInput, int maxLen) throws IOException {
    String result = null;

    final byte[] bytes = readBytes(dataInput, maxLen);
    if (bytes != null) {
      result = new String(bytes, UTF_8);
    }
    return result;
  }

  public static void writeStrings(DataOutput dataOutput, String[] strings) throws IOException {
    if (strings == null) {
      dataOutput.writeInt(-1);
    }
    else {
      dataOutput.writeInt(strings.length);
      for (String string : strings) {
        writeString(dataOutput, string);
      }
    }
  }

  public static String[] readStrings(DataInput dataInput) throws IOException {
    String[] result = null;
    final int len = dataInput.readInt();
    if (len >= 0) {
      result = new String[len];
      for (int i = 0; i < len; ++i) {
        result[i] = readString(dataInput);
      }
    }
    return result;
  }

  public static void writeProperties(DataOutput dataOutput, Properties properties) throws IOException {
    if (properties == null) {
      dataOutput.writeInt(-1);
    }
    else {
      final Set<String> propertyNames = properties.stringPropertyNames();
      dataOutput.writeInt(propertyNames.size());
      for (String propertyName : propertyNames) {
        writeString(dataOutput, propertyName);
        writeString(dataOutput, properties.getProperty(propertyName));
      }
    }
  }

  public static Properties readProperties(DataInput dataInput) throws IOException {
    Properties result = null;
    final int num = dataInput.readInt();
    if (num >= 0) {
      result = new Properties();
      for (int i = 0; i < num; ++i) {
        final String propertyName = readString(dataInput);
        final String propertyValue = readString(dataInput);
        result.setProperty(propertyName, propertyValue);
      }
    }
    return result;
  }

  /**
   * Write the byte array to the data output to be read in by readBytes.
   */
  public static final void writeBytes(DataOutput dataOutput, byte[] bytes) throws IOException {
    if (bytes == null) {
      dataOutput.writeInt(-1);
    }
    else {
      dataOutput.writeInt(bytes.length);
      dataOutput.write(bytes);
    }
  }

  /**
   * Read the byte array from data input as written by writeBytes.
   */
  public static final byte[] readBytes(DataInput dataInput) throws IOException {
    byte[] result = null;

    final int len = dataInput.readInt();
    if (len >= 0) {
      result = new byte[len];
      dataInput.readFully(result);
    }

    return result;
  }

  /**
   * Read the byte array from data input as written by writeBytes.
   * <p>
   * If the bytes to read is greater than maxLen, then return null.
   */
  public static final byte[] readBytes(DataInput dataInput, int maxLen) throws IOException {
    byte[] result = null;

    final int len = dataInput.readInt();
    if (len >= 0 && len <= maxLen) {
      result = new byte[len];
      dataInput.readFully(result);
    }

    return result;
  }

  public static void writeSerializable(DataOutput dataOutput, Serializable serializable) throws IOException {
    if (serializable == null) {
      dataOutput.writeBoolean(false);
    }
    else {
      dataOutput.writeBoolean(true);
      final byte[] bytes = asBytes(serializable);
      writeBytes(dataOutput, bytes);
    }
  }

  public static Serializable readSerializable(DataInput dataInput) throws IOException {
    Serializable result = null;
    final boolean hasSerializable = dataInput.readBoolean();
    if (hasSerializable) {
      final byte[] bytes = readBytes(dataInput);
      result = fromBytes(bytes);
    }
    return result;
  }

  /**
   * Get the serializable object as bytes.
   */
  public static final byte[] asBytes(Serializable serializable) throws IOException {
    final ByteArrayOutputStream bytesOut = new ByteArrayOutputStream();
    final ObjectOutputStream objectOut = new ObjectOutputStream(bytesOut);

    // serialize
    objectOut.writeObject(serializable);

    objectOut.close();
    final byte[] result = bytesOut.toByteArray();
    bytesOut.close();

    return result;
  }

  /**
   * Turn serialized bytes back into an object.
   */
  public static final Serializable fromBytes(byte[] bytes) throws IOException {
    Serializable result = null;

    final ByteArrayInputStream bytesIn = new ByteArrayInputStream(bytes);
    final ObjectInputStream objectIn = new ObjectInputStream(bytesIn);

    // deserialize
    try {
      result = (Serializable)objectIn.readObject();
    }
    catch (ClassNotFoundException e) {
      // leave result as null.
    }

    objectIn.close();
    return result;
  }
}
