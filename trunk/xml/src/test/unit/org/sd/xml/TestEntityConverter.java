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
package org.sd.xml;


import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * JUnit Tests for the EntityConverter class.
 * <p>
 * @author Spence Koehler
 */
public class TestEntityConverter extends TestCase {

  public TestEntityConverter(String name) {
    super(name);
  }
  
  public void testEscape() {
    assertEquals("this &bull; test", EntityConverter.escape("this • test"));
    assertEquals("this &reg; test", EntityConverter.escape("this ® test"));
  }

  public void testUnescape() {
    assertEquals("this is a test", EntityConverter.unescape("this&nbsp;is&nbsp;a&nbsp;test"));
    assertEquals("this • test", EntityConverter.unescape("this  test"));
    assertEquals("this ® test", EntityConverter.unescape("this ® test"));
    assertEquals("this ® test", EntityConverter.unescape("this &reg; test"));
    assertEquals("this ® test", EntityConverter.unescape("this &trade; test"));
    assertEquals("<", EntityConverter.unescape("&lt;"));
    assertEquals(" -> ", EntityConverter.unescape("&nbsp;-&gt;&nbsp;"));
  }

  public void testUnescapeDecimal() {
    assertEquals("A", EntityConverter.unescape("&#65;"));
    assertEquals("B", EntityConverter.unescape("&#66;"));
  }

  public void testUnescapeHexidecimal() {
    assertEquals("A", EntityConverter.unescape("&#x41;"));
    assertEquals("B", EntityConverter.unescape("&#x42;"));
  }

  public void testEscapeNonescapes() {
    assertEquals("a", EntityConverter.escape("a"));
    assertEquals("this", EntityConverter.escape("this"));
    assertEquals("this is a test", EntityConverter.escape("this is a test"));
  }

  public void testUnescapeNonunescapes() {
    assertEquals("a", EntityConverter.unescape("a"));
    assertEquals("this", EntityConverter.unescape("this"));
    assertEquals("this is a test", EntityConverter.unescape("this is a test"));
  }

  public void testEscapeDashes() {
    assertEquals("this&#45;&#45;test", EntityConverter.escape("this--test"));
    assertEquals("this--test", EntityConverter.unescape(EntityConverter.escape("this--test")));
  }

  public static Test suite() {
    TestSuite suite = new TestSuite(TestEntityConverter.class);
    return suite;
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}
