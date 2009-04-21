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
package org.sd.classifier;


import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * JUnit Tests for the AttributeConstraints class.
 * <p>
 * @author Spence Koehler
 */
public class TestAttributeConstraints extends TestCase {

  public TestAttributeConstraints(String name) {
    super(name);
  }
  
  public void testClosedNominalConstraints() {
    final AttributeConstraints attributeConstraints = new AttributeConstraints("class|NOMINAL|false|B2B,B2C,BOTH,OTHER");

    assertEquals("class", attributeConstraints.getAttributeName());
    assertEquals(AttributeType.NOMINAL, attributeConstraints.getAttributeType());
    assertFalse(attributeConstraints.isOpen());
    assertTrue(attributeConstraints.passesConstraints("B2B"));
    assertTrue(attributeConstraints.passesConstraints("B2C"));
    assertTrue(attributeConstraints.passesConstraints("BOTH"));
    assertTrue(attributeConstraints.passesConstraints("OTHER"));
    assertFalse(attributeConstraints.passesConstraints("b2b"));
  }

  public void testNominalWithHyphen() {
    final AttributeConstraints attributeConstraints = new AttributeConstraints("class|NOMINAL|false|BUSINESS,NON-BUSINESS");

    assertEquals("class", attributeConstraints.getAttributeName());
    assertEquals(AttributeType.NOMINAL, attributeConstraints.getAttributeType());
    assertFalse(attributeConstraints.isOpen());
    assertTrue(attributeConstraints.passesConstraints("BUSINESS"));
    assertTrue(attributeConstraints.passesConstraints("NON-BUSINESS"));
    assertFalse(attributeConstraints.passesConstraints("BOTH"));
    assertFalse(attributeConstraints.passesConstraints("OTHER"));
    assertFalse(attributeConstraints.passesConstraints("b2b"));
  }

  public void testIntegerConstraints() {
    final AttributeConstraints attributeConstraints = new AttributeConstraints("foo|integer|false|0-");
    

    assertEquals("foo", attributeConstraints.getAttributeName());
    assertEquals(AttributeType.INTEGER, attributeConstraints.getAttributeType());
    assertFalse(attributeConstraints.isOpen());
    assertTrue(attributeConstraints.passesConstraints("0"));
    assertTrue(attributeConstraints.passesConstraints("1"));
    assertTrue(attributeConstraints.passesConstraints("10"));
    assertTrue(attributeConstraints.passesConstraints("100"));
    assertFalse(attributeConstraints.passesConstraints("-1"));
    assertFalse(attributeConstraints.passesConstraints("foo"));
  }

  public static Test suite() {
    TestSuite suite = new TestSuite(TestAttributeConstraints.class);
    return suite;
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}
