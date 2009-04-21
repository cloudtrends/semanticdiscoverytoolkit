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
 * JUnit Tests for the CombinationLineBuilder class.
 * <p>
 * @author Spence Koehler
 */
public class TestCombinationLineBuilder extends TestCase {

  public TestCombinationLineBuilder(String name) {
    super(name);
  }
  
  public void testSimple() {
    final CombinationLineBuilder builder = new CombinationLineBuilder();
    builder.addField(1);
    builder.addField(new String[] {"a", "b"});
    builder.addField(3.14159, 3);
    builder.addField(new String[] {"c", "d"});

    final String[] lines = builder.getStrings();

    final String[] expected = new String[] {
      "1|a|3.142|c",
      "1|a|3.142|d",
      "1|b|3.142|c",
      "1|b|3.142|d",      
    };

    assertEquals(expected.length, lines.length);

    for (int i = 0; i < expected.length; ++i) {
      assertEquals(i + ": " + expected[i] + " -vs- " + lines[i], expected[i], lines[i]);
    }
  }

  public void testNulls() {
    final CombinationLineBuilder builder = new CombinationLineBuilder();
    builder.addField(1);
    builder.addField((String[])null);
    builder.addField(3.14159, 3);
    builder.addField(new String[] {"c", "d"});

    final String[] lines = builder.getStrings();

    final String[] expected = new String[] {
      "1||3.142|c",
      "1||3.142|d",
    };

    assertEquals(expected.length, lines.length);

    for (int i = 0; i < expected.length; ++i) {
      assertEquals(i + ": " + expected[i] + " -vs- " + lines[i], expected[i], lines[i]);
    }
  }

  public static Test suite() {
    TestSuite suite = new TestSuite(TestCombinationLineBuilder.class);
    return suite;
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}
