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
package org.sd.cluster.config;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.sd.cluster.io.Message;
import org.sd.cluster.io.Messenger;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * JUnit Tests for the StringResponse class.
 * <p>
 * @author Spence Koehler
 */
public class TestStringResponse extends TestCase {

  public TestStringResponse(String name) {
    super(name);
  }
  
  private final void verify(String expected) throws IOException {
    final Message message = new StringResponse(null, expected);

    final ByteArrayOutputStream bytesOut = new ByteArrayOutputStream();
    final DataOutputStream dataOut = new DataOutputStream(bytesOut);

    Messenger.sendMessage(message, dataOut);
    dataOut.close();

    final byte[] bytes = bytesOut.toByteArray();
    final ByteArrayInputStream bytesIn = new ByteArrayInputStream(bytes);
    final DataInputStream dataIn = new DataInputStream(bytesIn);

    final StringResponse got = (StringResponse)Messenger.receiveMessage(dataIn);
    assertEquals(expected, got.getValue());
  }

  public void testValues() throws IOException {
    verify("testing 1, 2, 3...");
    verify("");
  }

  public static Test suite() {
    TestSuite suite = new TestSuite(TestStringResponse.class);
    return suite;
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}
