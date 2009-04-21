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

import java.util.ArrayList;
import java.util.List;

/**
 * JUnit Tests for the TreeAnalyzer class.
 * <p>
 * @author Spence Koehler
 */
public class TestTreeAnalyzer extends TestCase {

  public TestTreeAnalyzer(String name) {
    super(name);
  }
  
  private final List<Tree<String>> makeNodes(String[] strings) {
    final List<Tree<String>> result = new ArrayList<Tree<String>>();

    for (String string : strings) {
      result.add(new Tree<String>(string));
    }

    return result;
  }

  public void testFoo() {
  }
/*
  public void testCalcRepeatIndex_NonConsec() {
    final NodeDataMatchFunction<String> matchFunction = new NodeDataMatchFunction<String>() {
      public boolean matches(String data1, String data2) {
        return ((data1 == null && data1 == data2) || data1.equals(data2));
      }
    };
    final List<Tree<String>> nodes = makeNodes(new String[]{"a", "b", "b", "c", "b", "b", "b", "d"});

    assertEquals(0, TreeAnalyzer.calcRepeatIndex("a", nodes, 0, false, matchFunction));
    assertEquals(0, TreeAnalyzer.calcRepeatIndex("b", nodes, 1, false, matchFunction));
    assertEquals(1, TreeAnalyzer.calcRepeatIndex("b", nodes, 2, false, matchFunction));
    assertEquals(0, TreeAnalyzer.calcRepeatIndex("c", nodes, 3, false, matchFunction));
    assertEquals(2, TreeAnalyzer.calcRepeatIndex("b", nodes, 4, false, matchFunction));
    assertEquals(3, TreeAnalyzer.calcRepeatIndex("b", nodes, 5, false, matchFunction));
    assertEquals(4, TreeAnalyzer.calcRepeatIndex("b", nodes, 6, false, matchFunction));
    assertEquals(0, TreeAnalyzer.calcRepeatIndex("d", nodes, 7, false, matchFunction));
  }

  public void testCalcRepeatIndex_Consec() {
    final NodeDataMatchFunction<String> matchFunction = new NodeDataMatchFunction<String>() {
      public boolean matches(String data1, String data2) {
        return ((data1 == null && data1 == data2) || data1.equals(data2));
      }
    };
    final List<Tree<String>> nodes = makeNodes(new String[]{"a", "b", "b", "c", "b", "b", "b", "d"});

    assertEquals(0, TreeAnalyzer.calcRepeatIndex("a", nodes, 0, true, matchFunction));
    assertEquals(0, TreeAnalyzer.calcRepeatIndex("b", nodes, 1, true, matchFunction));
    assertEquals(1, TreeAnalyzer.calcRepeatIndex("b", nodes, 2, true, matchFunction));
    assertEquals(0, TreeAnalyzer.calcRepeatIndex("c", nodes, 3, true, matchFunction));
    assertEquals(0, TreeAnalyzer.calcRepeatIndex("b", nodes, 4, true, matchFunction));
    assertEquals(1, TreeAnalyzer.calcRepeatIndex("b", nodes, 5, true, matchFunction));
    assertEquals(2, TreeAnalyzer.calcRepeatIndex("b", nodes, 6, true, matchFunction));
    assertEquals(0, TreeAnalyzer.calcRepeatIndex("d", nodes, 7, true, matchFunction));
  }

  public void testCountLaterRepeatIndex_NonConsec() {
    final NodeDataMatchFunction<String> matchFunction = new NodeDataMatchFunction<String>() {
      public boolean matches(String data1, String data2) {
        return ((data1 == null && data1 == data2) || data1.equals(data2));
      }
    };
    final List<Tree<String>> nodes = makeNodes(new String[]{"a", "b", "b", "c", "b", "b", "b", "d"});

    assertEquals(0, TreeAnalyzer.countLaterRepeatIndex("a", nodes, 0, false, matchFunction));
    assertEquals(4, TreeAnalyzer.countLaterRepeatIndex("b", nodes, 1, false, matchFunction));
    assertEquals(3, TreeAnalyzer.countLaterRepeatIndex("b", nodes, 2, false, matchFunction));
    assertEquals(0, TreeAnalyzer.countLaterRepeatIndex("c", nodes, 3, false, matchFunction));
    assertEquals(2, TreeAnalyzer.countLaterRepeatIndex("b", nodes, 4, false, matchFunction));
    assertEquals(1, TreeAnalyzer.countLaterRepeatIndex("b", nodes, 5, false, matchFunction));
    assertEquals(0, TreeAnalyzer.countLaterRepeatIndex("b", nodes, 6, false, matchFunction));
    assertEquals(0, TreeAnalyzer.countLaterRepeatIndex("d", nodes, 7, false, matchFunction));
  }

  public void testCountLaterRepeatIndex_Consec() {
    final NodeDataMatchFunction<String> matchFunction = new NodeDataMatchFunction<String>() {
      public boolean matches(String data1, String data2) {
        return ((data1 == null && data1 == data2) || data1.equals(data2));
      }
    };
    final List<Tree<String>> nodes = makeNodes(new String[]{"a", "b", "b", "c", "b", "b", "b", "d"});

    assertEquals(0, TreeAnalyzer.countLaterRepeatIndex("a", nodes, 0, true, matchFunction));
    assertEquals(1, TreeAnalyzer.countLaterRepeatIndex("b", nodes, 1, true, matchFunction));
    assertEquals(0, TreeAnalyzer.countLaterRepeatIndex("b", nodes, 2, true, matchFunction));
    assertEquals(0, TreeAnalyzer.countLaterRepeatIndex("c", nodes, 3, true, matchFunction));
    assertEquals(2, TreeAnalyzer.countLaterRepeatIndex("b", nodes, 4, true, matchFunction));
    assertEquals(1, TreeAnalyzer.countLaterRepeatIndex("b", nodes, 5, true, matchFunction));
    assertEquals(0, TreeAnalyzer.countLaterRepeatIndex("b", nodes, 6, true, matchFunction));
    assertEquals(0, TreeAnalyzer.countLaterRepeatIndex("d", nodes, 7, true, matchFunction));
  }

  public void testCountUniquesTo() {
    final NodeDataMatchFunction<String> matchFunction = new NodeDataMatchFunction<String>() {
      public boolean matches(String data1, String data2) {
        return ((data1 == null && data1 == data2) || data1.equals(data2));
      }
    };
    final List<Tree<String>> nodes = makeNodes(new String[]{"a", "b", "b", "c", "b", "b", "b", "d"});

    assertEquals(0, TreeAnalyzer.countUniquesTo("a", nodes, 0, matchFunction));
    assertEquals(1, TreeAnalyzer.countUniquesTo("b", nodes, 1, matchFunction));
    assertEquals(1, TreeAnalyzer.countUniquesTo("b", nodes, 2, matchFunction));
    assertEquals(2, TreeAnalyzer.countUniquesTo("c", nodes, 3, matchFunction));
    assertEquals(1, TreeAnalyzer.countUniquesTo("b", nodes, 4, matchFunction));
    assertEquals(1, TreeAnalyzer.countUniquesTo("b", nodes, 5, matchFunction));
    assertEquals(1, TreeAnalyzer.countUniquesTo("b", nodes, 6, matchFunction));
    assertEquals(3, TreeAnalyzer.countUniquesTo("d", nodes, 7, matchFunction));
  }

  public void testCountConsecutiveUniquesTo() {
    final NodeDataMatchFunction<String> matchFunction = new NodeDataMatchFunction<String>() {
      public boolean matches(String data1, String data2) {
        return ((data1 == null && data1 == data2) || data1.equals(data2));
      }
    };
    final List<Tree<String>> nodes = makeNodes(new String[]{"a", "b", "b", "c", "b", "b", "b", "d"});

    assertEquals(0, TreeAnalyzer.countConsecutiveUniquesTo("a", nodes, 0, matchFunction));
    assertEquals(1, TreeAnalyzer.countConsecutiveUniquesTo("b", nodes, 1, matchFunction));
    assertEquals(1, TreeAnalyzer.countConsecutiveUniquesTo("b", nodes, 2, matchFunction));
    assertEquals(2, TreeAnalyzer.countConsecutiveUniquesTo("c", nodes, 3, matchFunction));
    assertEquals(3, TreeAnalyzer.countConsecutiveUniquesTo("b", nodes, 4, matchFunction));
    assertEquals(3, TreeAnalyzer.countConsecutiveUniquesTo("b", nodes, 5, matchFunction));
    assertEquals(3, TreeAnalyzer.countConsecutiveUniquesTo("b", nodes, 6, matchFunction));
    assertEquals(4, TreeAnalyzer.countConsecutiveUniquesTo("d", nodes, 7, matchFunction));
  }
*/

  public static Test suite() {
    TestSuite suite = new TestSuite(TestTreeAnalyzer.class);
    return suite;
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}
