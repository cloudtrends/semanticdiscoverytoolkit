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
package org.sd.testtools;


import junit.framework.TestCase;

import org.sd.cio.MessageHelper;
import org.sd.io.Publishable;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * JUnit Tests for the BaseTestCase class.
 * <p>
 * @author Spence Koehler
 */
public abstract class BaseTestCase extends TestCase {

  public BaseTestCase(String name) {
    super(name);
  }
  
  public static Publishable roundTrip(Publishable publishable) throws IOException {
    final ByteArrayOutputStream bytesOut = new ByteArrayOutputStream();
    final DataOutputStream dataOut = new DataOutputStream(bytesOut);

    MessageHelper.writePublishable(dataOut, publishable);
    dataOut.close();

    final byte[] bytes = bytesOut.toByteArray();
    final ByteArrayInputStream bytesIn = new ByteArrayInputStream(bytes);
    final DataInputStream dataIn = new DataInputStream(bytesIn);
    
    final Publishable result = MessageHelper.readPublishable(dataIn);
    dataIn.close();

    return result;
  }

  /**
   * Get a port to use for testing.
   * <p>
   * This ensures that multithreaded tests don't conflict on ports.
   */
//   public static int getNextTestPort() {
//     return PortServer.getInstance().getNextTestPort();
//   }

  /**
   * Assert that the bytes got are as expected.
   */
  protected final void assertEquals(byte[] expected, byte[] got) {
    if (expected == null) {
      assertNull(got);
    }
    else {
      assertEquals("length mismatch", expected.length, got.length);

      for (int i = 0; i < expected.length; ++i) {
        assertEquals("mismatch at index " + i, expected[i], got[i]);
      }
    }
  }

}
