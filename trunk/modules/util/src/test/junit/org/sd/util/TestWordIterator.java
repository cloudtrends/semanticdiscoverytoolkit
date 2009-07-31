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
 * JUnit Tests for the WordIterator class.
 * <p>
 * @author Spence Koehler
 */
public class TestWordIterator extends TestCase {

  public TestWordIterator(String name) {
    super(name);
  }
  
  public void doTest(String input, String[] expected) {
    final WordIterator iter = new WordIterator(input);
    int count = 0;
    while (iter.hasNext()) {
      final String text = iter.next();

      if (count >= expected.length) {
        System.out.println("Extra token! (" + count + "): " + text);
      }
      else {
        assertEquals("(" + count + ")", expected[count], text);
      }
      ++count;
    }

    assertEquals(expected.length, count);
  }


  public void testSimple() {
    doTest("This is a test. This is only a test.",
           new String[] {
             "This", "is", "a", "test",
             "This", "is", "only", "a", "test",
           });
  }

  public void testBadAbbreviation1() {
    // NOTE: This doesn't work quite right. If we find that it is fixed in the
    //       future, we can set this test to rights!
    doTest("Ph.D., #@%! & Dr.",
           new String[] {
             "Ph.D", "Dr",
           });
  }


  public static Test suite() {
    TestSuite suite = new TestSuite(TestWordIterator.class);
    return suite;
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}
