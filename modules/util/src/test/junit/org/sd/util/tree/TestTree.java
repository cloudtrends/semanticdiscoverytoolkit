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

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.Iterator;

/**
 * JUnit Tests for the Tree class.
 * <p>
 * @author Spence Koehler
 */
public class TestTree extends TestCase {

  public TestTree(String name) {
    super(name);
  }
  
  public void testGetPaths1() {
    Tree<String> tree = new Tree<String>("a");
    List<List<String>> paths = tree.getPaths();
    assertEquals(1, paths.size());
    List<String> path = paths.get(0);
    assertEquals(1, paths.size());
    String elt = path.get(0);
    assertEquals("a", elt);
  }

  public void testGetPaths2() {
    // tree:
    // 1 -- 2 -- 4
    //  \-- 3 -- 5
    //       \-- 6
    // yields:
    // 1 2 4, 1 3 5, 1 3 6

    Map<Integer, Tree<Integer>> trees = new HashMap<Integer, Tree<Integer>>();

    for (int i = 0; i < 6; ++i) {
      trees.put(i, new Tree<Integer>(i + 1));
    }

    int[][] mappings = new int[][] {
      {1, 2}, {1, 3}, {2, 4}, {3, 5}, {3, 6},
    };

    int[][] expected = {
      {1, 2, 4},
      {1, 3, 5},
      {1, 3, 6},
    };

    for (int i = 0; i < mappings.length; ++i) {
      trees.get(mappings[i][0] - 1).addChild(trees.get(mappings[i][1] - 1));
    }

    List<List<Integer>> paths = trees.get(0).getPaths();

    assertEquals(3, paths.size());  // have three paths

    int index0 = 0;
    for (Iterator<List<Integer>> pathIt = paths.iterator(); pathIt.hasNext(); ++index0) {
      List<Integer> curInts = pathIt.next();

      int index1 = 0;
      for (Iterator<Integer> intIt = curInts.iterator(); intIt.hasNext(); ++index1) {
        Integer curInt = intIt.next();

        assertEquals(expected[index0][index1], curInt.intValue());
      }
    }
  }

  public static Test suite() {
    TestSuite suite = new TestSuite(TestTree.class);
    return suite;
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}
