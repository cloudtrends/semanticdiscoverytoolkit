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


import java.util.Arrays;
import java.util.Set;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * JUnit Tests for the OrderedMergeSet class.
 * <p>
 * @author Spence Koehler
 */
public class TestOrderedMergeSet extends TestCase {

  public TestOrderedMergeSet(String name) {
    super(name);
  }
  

  public void test1() {
    // a  b  c  d  e  f  g  h  i  j  k  l  m       (underlying distribution)
    //
    //    b  c     e           i     k             (initial pieces)
    //       c  d  e  f           j  k  l          (fill in some holes, leaving others open)
    // a                                   m       (add totally disconnected)
    //                   g                         (add totally disconnected)
    // a              f  g                 m       (tie in some disconnected)
    // a  b              g  h  i  j     l  m       (tie everything together)

    // sequential input/merged pairs
    final String[][][] lists = new String[][][] {
      new String[][] { new String[] { "b", "c", "e", "i", "k" }, new String[] { "b", "c", "e", "i", "k" }, },
      { new String[] { "c", "d", "e", "f", "j", "k", "l" }, new String[] { "b", "c", "d", "e", "i", "f", "j", "k", "l" }, },
      { new String[] { "a", "m" }, new String[] { "b", "c", "d", "e", "i", "f", "j", "k", "l", "a", "m" }, },
      { new String[] { "g" }, new String[] { "b", "c", "d", "e", "i", "f", "j", "k", "l", "a", "m", "g" }, },
      { new String[] { "a", "f", "g", "m" }, new String[] { "b", "c", "d", "e", "i", "a", "f", "j", "k", "l", "g", "m" }, },
      { new String[] { "a", "b", "g", "h", "i", "j", "l", "m" }, new String[] { "a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m" }, },
    };

    final OrderedMergeSet<String> mergeList = new OrderedMergeSet<String>();
    int step = 0;
    for (String[][] pair : lists) {
      mergeList.merge(Arrays.asList(pair[0]));

      final Set<String> elements = mergeList.getElements();
      assertEquals("step=" + step + ", elements=" + elements,
                   pair[1].length, elements.size());
      int index = 0;
      for (String element : elements) {
        assertEquals("step=" + step + ", index=" + index + ", elements=" + elements,
                     pair[1][index++], element);
      }
      ++step;
    }
  }


  public static Test suite() {
    TestSuite suite = new TestSuite(TestOrderedMergeSet.class);
    return suite;
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}
