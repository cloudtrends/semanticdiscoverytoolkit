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
 * JUnit Tests for the NumberedNameGenerator class.
 * <p>
 * @author Spence Koehler
 */
public class TestNumberedNameGenerator extends TestCase {

  public TestNumberedNameGenerator(String name) {
    super(name);
  }
  

  public void testAddPeriodToPrefix() {
    NumberedNameGenerator nngen = null;

    nngen = new NumberedNameGenerator("foo", "bar");
    assertEquals("foo.", nngen.getPrefix());

    nngen = new NumberedNameGenerator("foo1", "bar");
    assertEquals("foo1.", nngen.getPrefix());
  }

  public void testDontAddPeriodToPrefix() {
    NumberedNameGenerator nngen = null;

    nngen = new NumberedNameGenerator("foo-", "bar");
    assertEquals("foo-", nngen.getPrefix());

    nngen = new NumberedNameGenerator("foo1$", "bar");
    assertEquals("foo1$", nngen.getPrefix());

    nngen = new NumberedNameGenerator("foo.", "bar");
    assertEquals("foo.", nngen.getPrefix());
  }

  public void testEmptyPrefix() {
    NumberedNameGenerator nngen = null;

    nngen = new NumberedNameGenerator("", "bar");
    assertEquals("", nngen.getPrefix());

    nngen = new NumberedNameGenerator(null, "bar", 3);
    assertEquals("", nngen.getPrefix());

    assertTrue(nngen.isValidName("000.bar"));
    assertTrue(nngen.isValidName("010.bar"));
    assertTrue(nngen.isValidName("100.bar"));
  }

  public void testAddPeriodToPostfix() {
    NumberedNameGenerator nngen = null;

    nngen = new NumberedNameGenerator("foo", "bar");
    assertEquals(".bar", nngen.getPostfix());

    nngen = new NumberedNameGenerator("foo", "1bar");
    assertEquals(".1bar", nngen.getPostfix());
  }

  public void testDontAddPeriodToPostfix() {
    NumberedNameGenerator nngen = null;

    nngen = new NumberedNameGenerator("foo", "-bar");
    assertEquals("-bar", nngen.getPostfix());

    nngen = new NumberedNameGenerator("foo", "$1bar");
    assertEquals("$1bar", nngen.getPostfix());

    nngen = new NumberedNameGenerator("foo", ".bar");
    assertEquals(".bar", nngen.getPostfix());
  }

  public void testEmptyPostfix() {
    NumberedNameGenerator nngen = null;

    nngen = new NumberedNameGenerator("foo", "");
    assertEquals("", nngen.getPostfix());

    nngen = new NumberedNameGenerator("foo", null, 3);
    assertEquals("", nngen.getPostfix());

    assertTrue(nngen.isValidName("foo.000"));
    assertTrue(nngen.isValidName("foo.010"));
    assertTrue(nngen.isValidName("foo.100"));
  }

  public void testEmptyPrefixAndEmptyPostfix() {
    NumberedNameGenerator nngen = null;

    nngen = new NumberedNameGenerator("", "", 3);
    assertEquals("", nngen.getPrefix());
    assertEquals("", nngen.getPostfix());

    assertTrue(nngen.isValidName("000"));
    assertTrue(nngen.isValidName("010"));
    assertTrue(nngen.isValidName("100"));
  }

  public void testRecognizeZeroAsName() {
    NumberedNameGenerator nngen = null;

    nngen = new NumberedNameGenerator("foo", "bar", 1);
    assertTrue(nngen.isValidName("foo.0.bar"));

    nngen = new NumberedNameGenerator("foo", "bar", 2);
    assertTrue(nngen.isValidName("foo.00.bar"));

    nngen = new NumberedNameGenerator("foo", "bar", 3);
    assertTrue(nngen.isValidName("foo.000.bar"));

    nngen = new NumberedNameGenerator(null, null, 3);
    assertTrue(nngen.isValidName("000"));
    assertFalse(nngen.isValidName("00"));
    assertFalse(nngen.isValidName("0"));
    assertFalse(nngen.isValidName("0000"));
  }

  public void testEqualsNDigits() {
    NumberedNameGenerator nngen = null;

    for (int numDigits = 1; numDigits <= 2; ++numDigits) {
      nngen = new NumberedNameGenerator("foo", "bar", numDigits);

      String name = null;
      for (int i = 0; i < 20; ++i) {
        name = nngen.getNextName(name);
        assertEquals("foo." + MathUtil.integerString(i, numDigits, '0') + ".bar", name);
      }
    }
  }

  public void testLessThanNDigits() {
    NumberedNameGenerator nngen = null;

    nngen = new NumberedNameGenerator("foo", "bar", 1);
    assertTrue(nngen.isValidName("foo.0.bar"));
    assertFalse(nngen.isValidName("foo..bar"));

    nngen = new NumberedNameGenerator("foo", "bar", 3);
    assertTrue(nngen.isValidName("foo.000.bar"));
    assertFalse(nngen.isValidName("foo.00.bar"));
    assertFalse(nngen.isValidName("foo.0.bar"));
    assertFalse(nngen.isValidName("foo..bar"));
  }

  public void testGreaterThanNDigits() {
    NumberedNameGenerator nngen = null;

    nngen = new NumberedNameGenerator("foo", "bar", 3);

    assertTrue(nngen.isValidName("foo.000.bar"));
    assertTrue(nngen.isValidName("foo.010.bar"));
    assertTrue(nngen.isValidName("foo.100.bar"));

    assertFalse(nngen.isValidName("foo.0000.bar"));  // invalid because too long and padded
    assertFalse(nngen.isValidName("foo.01011.bar")); // invalid because too long and padded
    assertTrue(nngen.isValidName("foo.1000.bar"));   // valid because padded even though too long
  }

  public void testValidNeedsCorrectPrefixPostfix() {
    NumberedNameGenerator nngen = null;

    nngen = new NumberedNameGenerator("foo", "bar", 3);

    assertTrue(nngen.isValidName("foo.000.bar"));
    assertTrue(nngen.isValidName("foo.010.bar"));
    assertTrue(nngen.isValidName("foo.100.bar"));

    assertFalse(nngen.isValidName("bar.000.bar"));
    assertFalse(nngen.isValidName("bar.010.bar"));
    assertFalse(nngen.isValidName("bar.100.bar"));

    assertFalse(nngen.isValidName("bar.000.foo"));
    assertFalse(nngen.isValidName("bar.010.foo"));
    assertFalse(nngen.isValidName("bar.100.foo"));
  }


  public static Test suite() {
    TestSuite suite = new TestSuite(TestNumberedNameGenerator.class);
    return suite;
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}
