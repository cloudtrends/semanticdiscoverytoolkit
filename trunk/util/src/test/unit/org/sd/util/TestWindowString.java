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
 * JUnit Tests for the WindowString class.
 * <p>
 * @author Spence Koehler
 */
public class TestWindowString extends TestCase {

  public TestWindowString(String name) {
    super(name);
  }
  

  public void testBoundary1() {
    final WindowString windowString = new WindowString(7);

    assertTrue(windowString.append("01234"));
    assertEquals("01234", windowString.toString());
    assertFalse(windowString.rolled());
    assertFalse(windowString.truncated());
    
    assertTrue(windowString.append(""));
    assertEquals("01234", windowString.toString());
    assertFalse(windowString.rolled());
    assertFalse(windowString.truncated());


    assertTrue(windowString.append("56"));
    assertEquals("0123456", windowString.toString());
    assertFalse(windowString.rolled());
    assertFalse(windowString.truncated());

    assertTrue(windowString.append(""));
    assertEquals("0123456", windowString.toString());
    assertFalse(windowString.rolled());
    assertFalse(windowString.truncated());


    assertTrue(windowString.append("7890", 3));
    assertEquals("4567890", windowString.toString());
    assertTrue(windowString.rolled());
    assertFalse(windowString.truncated());

    assertTrue(windowString.append(""));
    assertEquals("4567890", windowString.toString());
    assertTrue(windowString.rolled());
    assertFalse(windowString.truncated());


    assertFalse(windowString.append("ABC"));
    assertEquals("7890ABC", windowString.toString());
    assertTrue(windowString.rolled());
    assertFalse(windowString.truncated());

    assertFalse(windowString.append(""));
    assertEquals("7890ABC", windowString.toString());
    assertTrue(windowString.rolled());
    assertFalse(windowString.truncated());


    assertFalse(windowString.append("DEF"));
    assertEquals("7890ABC", windowString.toString());
    assertTrue(windowString.rolled());
    assertTrue(windowString.truncated());

    assertFalse(windowString.append(""));
    assertEquals("7890ABC", windowString.toString());
    assertTrue(windowString.rolled());
    assertTrue(windowString.truncated());
  }

  public void testBoundary2() {
    final WindowString windowString = new WindowString(7);

    assertTrue(windowString.append("01234"));
    assertEquals("01234", windowString.toString());
    assertFalse(windowString.rolled());
    assertFalse(windowString.truncated());

    assertTrue(windowString.append(""));
    assertEquals("01234", windowString.toString());
    assertFalse(windowString.rolled());
    assertFalse(windowString.truncated());


    assertFalse(windowString.append("567890ABC", 5));
    assertEquals("7890ABC", windowString.toString());
    assertTrue(windowString.rolled());
    assertFalse(windowString.truncated());

    assertFalse(windowString.append(""));
    assertEquals("7890ABC", windowString.toString());
    assertTrue(windowString.rolled());
    assertFalse(windowString.truncated());
  }

  public void testBoundary3() {
    final WindowString windowString = new WindowString(7);

    assertTrue(windowString.append("01234"));
    assertEquals("01234", windowString.toString());
    assertFalse(windowString.rolled());
    assertFalse(windowString.truncated());

    assertTrue(windowString.append(""));
    assertEquals("01234", windowString.toString());
    assertFalse(windowString.rolled());
    assertFalse(windowString.truncated());


    assertFalse(windowString.append("567890ABCDEF", 5));
    assertEquals("7890ABC", windowString.toString());
    assertTrue(windowString.rolled());
    assertTrue(windowString.truncated());

    assertFalse(windowString.append(""));
    assertEquals("7890ABC", windowString.toString());
    assertTrue(windowString.rolled());
    assertTrue(windowString.truncated());
  }

  public void testBoundary4() {
    final WindowString windowString = new WindowString(7);

    assertTrue(windowString.append("01234", 3));
    assertEquals("01234", windowString.toString());
    assertFalse(windowString.rolled());
    assertFalse(windowString.truncated());

    assertTrue(windowString.append(""));
    assertEquals("01234", windowString.toString());
    assertFalse(windowString.rolled());
    assertFalse(windowString.truncated());


    assertFalse(windowString.append("567890ABCDEF"));
    assertEquals("0123456", windowString.toString());
    assertFalse(windowString.rolled());
    assertTrue(windowString.truncated());

    assertFalse(windowString.append(""));
    assertEquals("0123456", windowString.toString());
    assertFalse(windowString.rolled());
    assertTrue(windowString.truncated());
  }

  public void testBoundary5() {
    final WindowString windowString = new WindowString(7);

    assertTrue(windowString.append("01234", 4));
    assertEquals("01234", windowString.toString());
    assertFalse(windowString.rolled());
    assertFalse(windowString.truncated());

    assertTrue(windowString.append(""));
    assertEquals("01234", windowString.toString());
    assertFalse(windowString.rolled());
    assertFalse(windowString.truncated());


    assertFalse(windowString.append("567890ABCDEF"));
    assertEquals("1234567", windowString.toString());
    assertTrue(windowString.rolled());
    assertTrue(windowString.truncated());

    assertFalse(windowString.append(""));
    assertEquals("1234567", windowString.toString());
    assertTrue(windowString.rolled());
    assertTrue(windowString.truncated());
  }

  public void testBoundary6() {
    final WindowString windowString = new WindowString(7);

    assertTrue(windowString.append("01234", 1));
    assertEquals("01234", windowString.toString());
    assertFalse(windowString.rolled());
    assertFalse(windowString.truncated());

    assertTrue(windowString.append(""));
    assertEquals("01234", windowString.toString());
    assertFalse(windowString.rolled());
    assertFalse(windowString.truncated());


    assertFalse(windowString.append("567890ABCDEF"));
    assertEquals("0123456", windowString.toString());
    assertFalse(windowString.rolled());
    assertTrue(windowString.truncated());

    assertFalse(windowString.append(""));
    assertEquals("0123456", windowString.toString());
    assertFalse(windowString.rolled());
    assertTrue(windowString.truncated());
  }

  public void testRolling1() {
    final WindowString windowString = new WindowString(7);

    assertTrue(windowString.append("01234"));
    assertEquals("01234", windowString.toString());
    assertFalse(windowString.rolled());
    assertFalse(windowString.truncated());

    assertTrue(windowString.append(""));
    assertEquals("01234", windowString.toString());
    assertFalse(windowString.rolled());
    assertFalse(windowString.truncated());


    assertTrue(windowString.append("56789"));
    assertEquals("3456789", windowString.toString());
    assertTrue(windowString.rolled());
    assertFalse(windowString.truncated());

    assertTrue(windowString.append(""));
    assertEquals("3456789", windowString.toString());
    assertTrue(windowString.rolled());
    assertFalse(windowString.truncated());


    assertTrue(windowString.append("0ABCDEFGHIJKLMNOP"));
    assertEquals("JKLMNOP", windowString.toString());
    assertTrue(windowString.rolled());
    assertFalse(windowString.truncated());

    assertTrue(windowString.append(""));
    assertEquals("JKLMNOP", windowString.toString());
    assertTrue(windowString.rolled());
    assertFalse(windowString.truncated());
  }


  public static Test suite() {
    TestSuite suite = new TestSuite(TestWindowString.class);
    return suite;
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}
