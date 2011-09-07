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
package org.sd.util;

import java.nio.ByteBuffer;
import java.util.UUID;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * JUnit Tests for the ByteBufferUtil class.
 * <p>
 * @author asanderson
 */
public class TestByteBufferUtil extends TestCase {

  public TestByteBufferUtil(String name) {
    super(name);
  }

  public void testSerialize() 
    throws Exception
  {
    int[] unsignedInputs = new int[] { 0, 345, 1235, 22346, 65535 };
    int[][] truncatedInputs = new int[][] 
      { 
        new int[] { 0, 8, 73, 108, 255 },
        new int[] { 0, 345, 1235, 22346, 65535 },
        new int[] { 0, 2612, 12528, 1298347, 5148931, 12938714, 16777215},
      };
    String[] asciiInputs = new String[] 
      { 
        "",
        "test",
        "This is a test.",
        "This is only a test.",
      };
    UUID[] uuidInputs = new UUID[] 
      { 
        UUID.randomUUID(),
        new UUID(0L, 0L),
        new UUID(Long.MIN_VALUE, Long.MAX_VALUE),
      };

    for(int i = 0; i < unsignedInputs.length; i++)
    {
      ByteBuffer bytes = ByteBuffer.allocate(2);

      ByteBufferUtil.putUnsignedShort(bytes, unsignedInputs[i]);
      int result = ByteBufferUtil.getUnsignedShort(bytes, 0);

      assertEquals(unsignedInputs[i], result);
    }

    for(int i = 0; i < truncatedInputs.length; i++)
    {
      int[] inputs = truncatedInputs[i];

      for(int j = 0; j < inputs.length; j++)
      {
        ByteBuffer bytes = ByteBuffer.allocate(i+1);

        ByteBufferUtil.putTruncatedInt(bytes, inputs[j], i+1);
        int result = ByteBufferUtil.getTruncatedInt(bytes, 0, i+1);
        
        assertEquals(inputs[j], result);
      }
    }

    for(int i = 0; i < asciiInputs.length; i++)
    {
      String asciiString = asciiInputs[i];
      byte[] asciiBytes = asciiString.getBytes("US-ASCII");
      ByteBuffer bytes = ByteBuffer.allocate(asciiBytes.length + 2);

      ByteBufferUtil.putAscii(bytes, asciiInputs[i]);
      String result = ByteBufferUtil.getAscii(bytes, 0);

      assertEquals(asciiInputs[i], result);
    }

    for(int i = 0; i < uuidInputs.length; i++)
    {
      UUID uuid = uuidInputs[i];
      ByteBuffer bytes = ByteBuffer.allocate(16);

      ByteBufferUtil.putUUID(bytes, uuid);
      UUID result = ByteBufferUtil.getUUID(bytes, 0);

      assertEquals(uuid, result);
    }
  }

  public void testSerializeAbsolute() 
    throws Exception
  {
    int[] unsignedInputs = new int[] { 0, 345, 1235, 22346, 65535 };
    int[][] truncatedInputs = new int[][] 
      { 
        new int[] { 0, 8, 73, 108, 255 },
        new int[] { 0, 345, 1235, 22346, 65535 },
        new int[] { 0, 2612, 12528, 1298347, 5148931, 12938714, 16777215},
      };
    String[] asciiInputs = new String[] 
      { 
        "",
        "test",
        "This is a test.",
        "This is only a test.",
      };
    UUID[] uuidInputs = new UUID[] 
      { 
        UUID.randomUUID(),
        new UUID(0L, 0L),
        new UUID(Long.MIN_VALUE, Long.MAX_VALUE),
      };

    for(int i = 0; i < unsignedInputs.length; i++)
    {
      ByteBuffer bytes = ByteBuffer.allocate(2);

      ByteBufferUtil.putUnsignedShort(bytes, 0, unsignedInputs[i]);
      int result = ByteBufferUtil.getUnsignedShort(bytes, 0);

      assertEquals(unsignedInputs[i], result);
    }

    for(int i = 0; i < truncatedInputs.length; i++)
    {
      int[] inputs = truncatedInputs[i];

      for(int j = 0; j < inputs.length; j++)
      {
        ByteBuffer bytes = ByteBuffer.allocate(i+1);

        ByteBufferUtil.putTruncatedInt(bytes, 0, inputs[j], i+1);
        int result = ByteBufferUtil.getTruncatedInt(bytes, 0, i+1);
        
        assertEquals(inputs[j], result);
      }
    }

    for(int i = 0; i < asciiInputs.length; i++)
    {
      String asciiString = asciiInputs[i];
      byte[] asciiBytes = asciiString.getBytes("US-ASCII");
      ByteBuffer bytes = ByteBuffer.allocate(asciiBytes.length + 2);

      ByteBufferUtil.putAscii(bytes, 0, asciiInputs[i]);
      String result = ByteBufferUtil.getAscii(bytes, 0);

      assertEquals(asciiInputs[i], result);
    }

    for(int i = 0; i < uuidInputs.length; i++)
    {
      UUID uuid = uuidInputs[i];
      ByteBuffer bytes = ByteBuffer.allocate(16);

      ByteBufferUtil.putUUID(bytes, 0, uuid);
      UUID result = ByteBufferUtil.getUUID(bytes, 0);

      assertEquals(uuid, result);
    }
  }

  public static Test suite() {
    TestSuite suite = new TestSuite(TestByteBufferUtil.class);
    return suite;
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}
