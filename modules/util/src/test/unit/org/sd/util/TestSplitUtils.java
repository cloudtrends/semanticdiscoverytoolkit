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
 * JUnit Tests for the SplitUtils class.
 * <p>
 * @author Spence Koehler
 */
public class TestSplitUtils extends TestCase {

  public TestSplitUtils(String name) {
    super(name);
  }
  
  private final void assertEquals(String[] expected, String[] got) {
    assertEquals(expected.length, got.length);

    for (int i = 0; i < expected.length; ++i) {
      assertEquals("index " + i, expected[i], got[i]);
    }
  }

  public void testMultiDelimSplit() {
    assertEquals(new String[]{"foo1, bar1, baz1",},
                 SplitUtils.multiDelimSplit("foo1, bar1, baz1"));

    assertEquals(new String[]{"foo1, bar1, baz1", "foo2, bar2, baz2"},
                 SplitUtils.multiDelimSplit("foo1, bar1, baz1. - foo2, bar2, baz2"));

    assertEquals(new String[]{"foo1, bar1, baz1", "foo2, bar2, baz2"},
                 SplitUtils.multiDelimSplit("foo1, bar1, baz1. - foo2, bar2, baz2. --"));
  }

  public void testSingleDelimSplit() {
    assertEquals(new String[]{"foo"},
                 SplitUtils.singleDelimSplit("foo"));

    assertEquals(new String[]{"foo"},
                 SplitUtils.singleDelimSplit("foo, "));

    assertEquals(new String[]{"foo1", "bar1", "baz1"},
                 SplitUtils.singleDelimSplit("foo1, bar1, baz1"));

  }

  public static Test suite() {
    TestSuite suite = new TestSuite(TestSplitUtils.class);
    return suite;
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}
