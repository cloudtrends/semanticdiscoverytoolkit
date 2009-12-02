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


import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * JUnit Tests for the BufferedStringReader class.
 * <p>
 * @author Spence Koehler
 */
public class TestBufferedStringReader extends TestCase {

  public TestBufferedStringReader(String name) {
    super(name);
  }
  

  private final String buildLargeString(int size, int lowChar, int highChar) {
    final StringBuilder stringBuilder = new StringBuilder();

    final int nchars = highChar - lowChar + 1;
    for (int i = 0; i < size; ++i) {
      stringBuilder.append((char)(lowChar + (i % nchars)));
    }

    return stringBuilder.toString();
  }

  public void testReadFully() throws IOException {
    // initialize a string reader using chars from ascii 33(!) to 126(~)
    final String string = buildLargeString((int)(BufferedStringReader.IO_BUFFER_SIZE * 2.5), '!', '~');
    final StringReader reader = new StringReader(string);

    // initialize a buffered string reader
    final BufferedStringReader bsr = new BufferedStringReader();
    assertEquals(0, bsr.getCharOffset());

    // read fully
    final String got = bsr.readFully(reader);

    // check
    assertEquals(string, got);
    assertEquals(string.length(), bsr.getCharOffset());

    // verify finished
    assertNull(bsr.readFully(reader));
    assertNull(bsr.readUntil(reader, 'a'));
    assertNull(bsr.readWhile(reader, 'a'));
  }

  public void testReadUntilWhileAndFully() throws IOException {
    // initialize a string reader
    final String largeString = buildLargeString((int)(BufferedStringReader.IO_BUFFER_SIZE * 2.5), 'a', 'z');
    final String[] strings = new String[] {
      largeString,
      "   ",
      "foobarbaz",
      "       ",
      largeString,
    };
    
    final StringBuilder concat = new StringBuilder();
    for (String string : strings) concat.append(string);
    final StringReader reader = new StringReader(concat.toString());

    // initialize a buffered string reader
    final BufferedStringReader bsr = new BufferedStringReader();
    assertEquals(0, bsr.getCharOffset());
    int len = 0;

    // read until ' '
    final String gotUntil1 = bsr.readUntil(reader, ' ');
    len += gotUntil1.length();
    assertEquals(strings[0], gotUntil1);
    assertEquals(len, bsr.getCharOffset());

    // read while ' '
    final String gotWhile1 = bsr.readWhile(reader, ' ');
    len += gotWhile1.length();
    assertEquals(strings[1], gotWhile1);
    assertEquals(len, bsr.getCharOffset());

    // read until ' '
    final String gotUntil2 = bsr.readUntil(reader, ' ');
    len += gotUntil2.length();
    assertEquals(strings[2], gotUntil2);
    assertEquals(len, bsr.getCharOffset());

    // read while ' '
    final String gotWhile2 = bsr.readWhile(reader, ' ');
    len += gotWhile2.length();
    assertEquals(strings[3], gotWhile2);
    assertEquals(len, bsr.getCharOffset());

    // read fully (remainder)
    final String remainder = bsr.readFully(reader);
    len += remainder.length();
    assertEquals(strings[4], remainder);
    assertEquals(len, bsr.getCharOffset());
  }

  public void testReadWhileAcrossBufferBoundary() throws IOException {
    // initialize a string reader
    final String largeString = buildLargeString((int)(BufferedStringReader.IO_BUFFER_SIZE - 2), 'a', 'z');
    final String[] strings = new String[] {
      largeString,
      "     ",
      "foobarbaz",
    };
    
    final StringBuilder concat = new StringBuilder();
    for (String string : strings) concat.append(string);
    final StringReader reader = new StringReader(concat.toString());

    // initialize a buffered string reader
    final BufferedStringReader bsr = new BufferedStringReader();
    assertEquals(0, bsr.getCharOffset());
    int len = 0;

    // read until ' '
    final String gotUntil1 = bsr.readUntil(reader, ' ');
    len += gotUntil1.length();
    assertEquals(strings[0], gotUntil1);
    assertEquals(len, bsr.getCharOffset());

    // read while ' '
    final String gotWhile1 = bsr.readWhile(reader, ' ');
    len += gotWhile1.length();
    assertEquals(strings[1], gotWhile1);
    assertEquals(len, bsr.getCharOffset());

    // read fully (remainder)
    final String remainder = bsr.readFully(reader);
    len += remainder.length();
    assertEquals(strings[2], remainder);
    assertEquals(len, bsr.getCharOffset());
  }


  public static Test suite() {
    TestSuite suite = new TestSuite(TestBufferedStringReader.class);
    return suite;
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}
