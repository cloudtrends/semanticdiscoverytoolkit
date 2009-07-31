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

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * JUnit Tests for the LookAheadIterator class.
 * <p>
 * @author Spence Koehler
 */
public class TestLookAheadIterator extends TestCase {

  public TestLookAheadIterator(String name) {
    super(name);
  }
  
  public void testBasicIteration() {
    final List<String> list = Arrays.asList(new String[] {"a", "b", "c", "d", "e", "f", "g", "h", "i"});
    final LookAheadIterator iter = new LookAheadIterator<String>(list.iterator());

    final Iterator<String> expected = list.iterator();

    while (iter.hasNext()) {
      assertTrue(expected.hasNext());

      final String expectedNext = expected.next();
      assertEquals(expectedNext, iter.next());
      assertEquals(expectedNext, iter.lookAhead(0));

      assertEquals(null, iter.lookAhead(-1));
    }

    assertFalse(expected.hasNext());
  }

  public void testLookAheadIterationFromFarToNear() {
    final String[] strings = new String[] {"a", "b", "c", "d", "e", "f", "g", "h", "i"};
    final List<String> list = Arrays.asList(strings);
    final LookAheadIterator<String> iter = new LookAheadIterator<String>(list.iterator());

    final Iterator<String> expected = list.iterator();

    int index = 0;
    while (iter.hasNext()) {
      assertTrue(expected.hasNext());
      final String expectedNext = expected.next();
      final String next = iter.next();

      assertEquals(expectedNext, next);
      assertEquals(expectedNext, iter.lookAhead(0));

      assertEquals(null, iter.lookAhead(-1));

      for (int numAhead = strings.length; numAhead >= 0; --numAhead) {
        final String expectedAhead = (index + numAhead) >= strings.length ? null : strings[index + numAhead];
        assertEquals("index=" + index + " numAhead=" + numAhead, expectedAhead, iter.lookAhead(numAhead));
      }

      ++index;
    }

    assertFalse(expected.hasNext());
  }

  public void testLookAheadIterationFromNearToFar() {
    final String[] strings = new String[] {"a", "b", "c", "d", "e", "f", "g", "h", "i"};
    final List<String> list = Arrays.asList(strings);
    final LookAheadIterator<String> iter = new LookAheadIterator<String>(list.iterator());

    final Iterator<String> expected = list.iterator();

    int index = 0;
    while (iter.hasNext()) {
      assertTrue(expected.hasNext());
      final String expectedNext = expected.next();
      final String next = iter.next();

      assertEquals(expectedNext, next);
      assertEquals(expectedNext, iter.lookAhead(0));

      assertEquals(null, iter.lookAhead(-1));

      for (int numAhead = 0; numAhead < strings.length; ++numAhead) {
        final String expectedAhead = (index + numAhead) >= strings.length ? null : strings[index + numAhead];
        assertEquals("index=" + index + " numAhead=" + numAhead, expectedAhead, iter.lookAhead(numAhead));
      }

      ++index;
    }

    assertFalse(expected.hasNext());
  }

  public void testLookBothWaysIteration() {
    final String[] strings = new String[] {"a", "b", "c", "d", "e", "f", "g", "h", "i"};
    final List<String> list = Arrays.asList(strings);
    final LookAheadIterator<String> iter = new LookAheadIterator<String>(list.iterator(), true);

    final Iterator<String> expected = list.iterator();

    int index = 0;
    while (iter.hasNext()) {
      assertTrue(expected.hasNext());
      final String expectedNext = expected.next();
      final String next = iter.next();

      assertEquals(expectedNext, next);
      assertEquals(expectedNext, iter.lookAhead(0));

      for (int numAhead = -strings.length; numAhead < strings.length; ++numAhead) {
        final String expectedAhead = (index + numAhead) >= strings.length ? null : (index + numAhead) < 0 ? null : strings[index + numAhead];
        assertEquals("index=" + index + " numAhead=" + numAhead, expectedAhead, iter.lookAhead(numAhead));
      }

      ++index;
    }

    assertFalse(expected.hasNext());
  }

  public static Test suite() {
    TestSuite suite = new TestSuite(TestLookAheadIterator.class);
    return suite;
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}
