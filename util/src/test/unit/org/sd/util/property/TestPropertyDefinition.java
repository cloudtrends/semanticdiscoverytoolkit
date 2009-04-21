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
package org.sd.util.property;


import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * JUnit Tests for the PropertyDefinition class.
 * <p>
 * @author Spence Koehler
 */
public class TestPropertyDefinition extends TestCase {

  public TestPropertyDefinition(String name) {
    super(name);
  }
  
  public void testExpectedRepeatBehavior() {
    final PropertyDefinition andPropertyDefinition = AndPropertyDefinition.getInstance();
    final PropertyDefinition orPropertyDefinition = OrPropertyDefinition.getInstance();
    final PropertyDefinition single = new NamedPropertyDefinition("foo");
    final PropertyDefinition oneOrMore = new NamedPropertyDefinition("bar+");
    final PropertyDefinition zeroOrMore = new NamedPropertyDefinition("baz*");
    final PropertyDefinition zeroOrOne = new NamedPropertyDefinition("abc?");

    assertFalse(andPropertyDefinition.repeats());
    assertFalse(orPropertyDefinition.repeats());
    assertFalse(single.repeats());
    assertTrue(oneOrMore.repeats());
    assertTrue(zeroOrMore.repeats());
    assertFalse(zeroOrOne.repeats());

    assertFalse(andPropertyDefinition.isOptional());
    assertFalse(orPropertyDefinition.isOptional());
    assertFalse(single.isOptional());
    assertFalse(oneOrMore.isOptional());
    assertTrue(zeroOrMore.isOptional());
    assertTrue(zeroOrOne.isOptional());

    assertEquals("AND", andPropertyDefinition.getLabel());
    assertEquals("OR", orPropertyDefinition.getLabel());
    assertEquals("foo", single.getLabel());
    assertEquals("bar", oneOrMore.getLabel());
    assertEquals("baz", zeroOrMore.getLabel());
    assertEquals("abc", zeroOrOne.getLabel());

    assertEquals("AND", andPropertyDefinition.toString());
    assertEquals("OR", orPropertyDefinition.toString());
    assertEquals("foo", single.toString());
    assertEquals("bar+", oneOrMore.toString());
    assertEquals("baz*", zeroOrMore.toString());
    assertEquals("abc?", zeroOrOne.toString());
  }

  public static Test suite() {
    TestSuite suite = new TestSuite(TestPropertyDefinition.class);
    return suite;
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}
