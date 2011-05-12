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
package org.sd.cluster.io;


import junit.framework.Test;
import junit.framework.TestSuite;
import org.sd.testtools.BaseTestCase;
import org.sd.cio.MessageHelper;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * JUnit Tests for the Messenger class.
 * <p>
 * @author Spence Koehler
 */
public class TestMessenger extends BaseTestCase {

  public TestMessenger(String name) {
    super(name);
  }
  
  /**
   * Tests basic roundtripping through the static sendMessage and receiveMessage
   * methods.
   */
  public void testBasicRoundTrip() throws IOException {
    final Message[] messages = new Message[] {
      new SimpleMessage((byte)255, true, 255, "simple1", 255.0, "foo"),
      new SimpleMessage((byte)127, false, 128, "simple2", 129.5, "bar"),
      new SimpleMessage((byte)0, true, 65535, "simple3", 3.14159, "baz"),
    };

    final ByteArrayOutputStream bytesOut = new ByteArrayOutputStream();
    final DataOutputStream dataOut = new DataOutputStream(bytesOut);

    // send the messages
    for (Message message : messages) {
      Messenger.sendMessage(message, dataOut);
    }
    dataOut.close();

    final byte[] bytes = bytesOut.toByteArray();
    final ByteArrayInputStream bytesIn = new ByteArrayInputStream(bytes);
    final DataInputStream dataIn = new DataInputStream(bytesIn);

    // receive the messages
    for (Message expected : messages) {
      final SimpleMessage got = (SimpleMessage)Messenger.receiveMessage(dataIn);
      assertEquals(expected, got);
    }

    dataIn.close();
  }

  public static final class SimpleMessage implements Message {
    byte a;
    boolean b;
    int i;
    String s;
    double d;
    String serializable;

    boolean responded = false;
    boolean handled = false;
    boolean handledResponse = false;

    public SimpleMessage() {
      a = (byte)0;
      b = false;
      i = 0;
      s = null;
      d = 0.0;
      serializable = null;
    }

    public SimpleMessage(byte a, boolean b, int i, String s, double d, String serializable) {
      this.a = a;
      this.b = b;
      this.i = i;
      this.s = s;
      this.d = d;
      this.serializable = serializable;
    }

    public void write(DataOutput dataOutput) throws IOException {
      dataOutput.writeByte(a);
      dataOutput.writeBoolean(b);
      dataOutput.writeInt(i);
      dataOutput.writeInt(s.length());
      dataOutput.write(s.getBytes());
      dataOutput.writeDouble(d);
      MessageHelper.writeSerializable(dataOutput, serializable);
      dataOutput.writeBoolean(responded);
      dataOutput.writeBoolean(handled);
      dataOutput.writeBoolean(handledResponse);
    }

    public void read(DataInput dataInput) throws IOException {
      a = dataInput.readByte();
      b = dataInput.readBoolean();
      i = dataInput.readInt();
      final int len = dataInput.readInt();
      final byte[] bytes = new byte[len];
      dataInput.readFully(bytes);
      s = new String(bytes);
      d = dataInput.readDouble();
      serializable = (String)MessageHelper.readSerializable(dataInput);
      responded = dataInput.readBoolean();
      handled = dataInput.readBoolean();
      handledResponse = dataInput.readBoolean();
    }

    public int numBytes() {
      return 1 + 1 + 4 + 8 + s.getBytes().length + serializable.getBytes().length;
    }

    public Message getResponse(Context context, ConnectionContext connectionContext) {
      responded = true;
      return this;
    }

    public void handle(Context context, ConnectionContext connectionContext) {
      handled = true;
    }

    public boolean equals(Object other) {
      boolean result = (this == other);
      if (!result && other instanceof SimpleMessage) {
        final SimpleMessage o = (SimpleMessage)other;
        result = (a == o.a && b == o.b && i == o.i &&
                  (s == o.s || (s != null && s.equals(o.s))) &&
                  (serializable == o.serializable || (serializable != null && serializable.equals(o.serializable))) &&
                  d == o.d);
      }
      return result;
    }

    public String toString() {
      final StringBuilder result = new StringBuilder();

      result.append("a=").append(a).
        append(",b=").append(b).
        append(",i=").append(i).
        append(",s=").append(s).
        append(",d=").append(d).
        append(",serializable=").append(serializable);

      return result.toString();
    }
  }

  public static Test suite() {
    TestSuite suite = new TestSuite(TestMessenger.class);
    return suite;
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}
