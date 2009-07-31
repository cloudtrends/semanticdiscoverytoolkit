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


import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * JUnit Tests for the MappedString class.
 * <p>
 * @author Spence Koehler
 */
public class TestMappedString extends TestCase {

  public TestMappedString(String name) {
    super(name);
  }
  

  public void testIdentity() {
    final MappedString mappedString = new MappedString();

    final String string = "gobbledygook";
    mappedString.append(string);  // identity mapping

    assertEquals(string, mappedString.getOriginalString());
    assertEquals(string, mappedString.getMappedString());

    for (int i = 0; i < string.length(); ++i) {
      final int[] origIndex = mappedString.getOriginalIndex(i);
      assertEquals(i, origIndex[0]);  // same start pos
      assertEquals(1, origIndex[1]);  // length 1
    }
  }

  public void testTransformed() {
    final MappedString mappedString = new MappedString();

    // testing &quot;escaped entity&quot; conversion
    // testing ".....escaped entity"..... conversion
    mappedString.
      append("testing ").
      append("\"".codePointAt(0), "&quot;").
      append("escaped entity").
      append("\"".codePointAt(0), "&quot;").
      append(" conversion");

    assertEquals("testing &quot;escaped entity&quot; conversion",
                 mappedString.getOriginalString());
    final String mstring = mappedString.getMappedString();
    assertEquals("testing \"escaped entity\" conversion", mstring);

    int j = 0;  // original pos
    for (int i = 0; i < mstring.length(); ++i) {
      final int[] origIndex = mappedString.getOriginalIndex(i);

      if (i == 8 || i == 23) {
        // &quot; mapping
        assertEquals(j, origIndex[0]);
        assertEquals(6, origIndex[1]);
      }
      else {
        assertEquals(j, origIndex[0]);
        assertEquals(1, origIndex[1]);  // length 1
      }

      j += origIndex[1]; // increment by original length
    }
  }

  public static Test suite() {
    TestSuite suite = new TestSuite(TestMappedString.class);
    return suite;
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}
