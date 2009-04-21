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
package org.sd.bdb;


import org.sd.cio.MessageHelper;
import org.sd.io.Publishable;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

/**
 * Utility methods.
 * <p>
 * @author Spence Koehler
 */
public class DbUtil {

  /**
   * Utility to get the string's bytes.
   */
  public static final byte[] getBytes(String string) {
    byte[] result = null;

    if (string != null) {
      try {
        result = string.getBytes("UTF-8");
      }
      catch (UnsupportedEncodingException e) {
        throw new IllegalStateException(e);
      }
    }

    return result;
  }

  /**
   * Utility to rebuild the string from its bytes.
   */
  public static final String getString(byte[] bytes) {
    String result = null;

    if (bytes != null) {
      try {
        result = new String(bytes, "UTF-8");
      }
      catch (UnsupportedEncodingException e) {
        throw new IllegalStateException(e);
      }
    }

    return result;
  }

  /**
   * Utility to get the publishable's bytes.
   */
  public static final byte[] getBytes(Publishable publishable) {

    ByteArrayOutputStream bytesOut = null;
    DataOutputStream dataOut = null;

    try {
      bytesOut = new ByteArrayOutputStream();
      dataOut = new DataOutputStream(bytesOut);
      MessageHelper.writePublishable(dataOut, publishable);
    }
    catch (IOException e) {
      throw new IllegalStateException(e);
    }
    finally {
      try {
        if (dataOut != null) dataOut.close();
        if (bytesOut != null) bytesOut.close();
      }
      catch (IOException e) {
        throw new IllegalStateException(e);
      }
    }

    return bytesOut.toByteArray();
  }

  /**
   * Utility to rebuild the publishable fom its bytes.
   */
  public static final Publishable getPublishable(byte[] bytes) {
    Publishable result = null;

    ByteArrayInputStream bytesIn = null;
    DataInputStream dataIn = null;

    try {
      bytesIn = new ByteArrayInputStream(bytes);
      dataIn = new DataInputStream(bytesIn);
      result = MessageHelper.readPublishable(dataIn);
    }
    catch (IOException e) {
      throw new IllegalStateException(e);
    }
    finally {
      try {
        if (dataIn != null) dataIn.close();
        if (bytesIn != null) bytesIn.close();
      }
      catch (IOException e) {
        throw new IllegalStateException(e);
      }
    }

    return result;
  }
}
