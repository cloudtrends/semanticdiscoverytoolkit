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
package org.sd.util.tree;


import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.util.Collection;

/**
 * JUnit Tests for the Hierarchy class.
 * <p>
 * @author Spence Koehler
 */
public class TestHierarchy extends TestCase {

  public TestHierarchy(String name) {
    super(name);
  }
  
  private static TreeBuilder<String> treeBuilder = TreeBuilderFactory.getStringTreeBuilder();

  private void verify(String treeString, int numIndentSpaces, String[] expectedHierarchyStrings) {
    final Tree<String> tree = treeBuilder.buildTree(treeString);
    final Hierarchy<String> hierarchy = new Hierarchy<String>(tree);
    final Collection<String> gotHierarchyStrings = hierarchy.getHierarchyStrings(numIndentSpaces);

    assertEquals(expectedHierarchyStrings.length, gotHierarchyStrings.size());
    int index = 0;
    for (String got : gotHierarchyStrings) {
      final String expected = expectedHierarchyStrings[index++];
      assertEquals("index=" + index + " allGot=" + gotHierarchyStrings + " expected=" + expected + " got=" + got, expected, got);
    }
  }

  public void testGetHierarchyStrings() {
    verify("(a b c d)", 2, new String[]{"a b c d"});
    verify("(a (b c) d)", 0, new String[]{"a b d", "b c"});
    verify("(a (b c) d)", 2, new String[]{"a b d", "  b c"});

    verify("(a (b (c d e) f (g h)) (i (j k l) m))", 0,
           new String[] {
             "a b i",
             "b c f g",
             "c d e",
             "g h",
             "i j m",
             "j k l",
           });

    verify("(a (b (c d e) f (g h)) (i (j k l) m))", 2,
           new String[] {
             "a b i",
             "  b c f g",
             "    c d e",
             "    g h",
             "  i j m",
             "    j k l",
           });
  }

  public static Test suite() {
    TestSuite suite = new TestSuite(TestHierarchy.class);
    return suite;
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}
