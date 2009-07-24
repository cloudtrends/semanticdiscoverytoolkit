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
 * JUnit Tests for the PropertySchema class.
 * <p>
 * @author Spence Koehler
 */
public class TestPropertySchema extends TestCase {

  public TestPropertySchema(String name) {
    super(name);
  }
  
  public void testAddGoodDefinitions() {
    final PropertySchema schema = new PropertySchema();
    try {
      schema.addDefinition("foo");
      schema.addDefinition("(bar+)");
      schema.addDefinition("(baz abc def)");
      schema.addDefinition("(geh (OR (AND ijk+ lmn) opq))");
    }
    catch (Exception e) {
      fail("Shouldn't have thrown exception! " + e);
    }

    assertNotNull(schema.getPropertyDefinitionTree("foo"));
    assertNotNull(schema.getPropertyDefinitionTree("bar"));
    assertNotNull(schema.getPropertyDefinitionTree("baz"));
    assertNotNull(schema.getPropertyDefinitionTree("geh"));

    assertNull(schema.getPropertyDefinitionTree("abc"));
    assertNull(schema.getPropertyDefinitionTree("def"));
    assertNull(schema.getPropertyDefinitionTree("ijk"));
    assertNull(schema.getPropertyDefinitionTree("lmn"));
    assertNull(schema.getPropertyDefinitionTree("opq"));
  }

  public void testAddAndDefinition() {
    boolean caughtException = false;

    final PropertySchema schema = new PropertySchema();
    try {
      schema.addDefinition("and");
    }
    catch (IllegalArgumentException e) {
      caughtException = true;
    }

    assertTrue("Should have caught an exception!", caughtException);
  }

  public void testAddOrDefinition() {
    boolean caughtException = false;

    final PropertySchema schema = new PropertySchema();
    try {
      schema.addDefinition("or");
    }
    catch (IllegalArgumentException e) {
      caughtException = true;
    }

    assertTrue("Should have caught an exception!", caughtException);
  }

  public void testAddDuplicateDefinition() {
    boolean caughtException = false;

    final PropertySchema schema = new PropertySchema();
    try {
      schema.addDefinition("foo");
      schema.addDefinition("(foo)");
    }
    catch (IllegalStateException e) {
      caughtException = true;
    }

    assertTrue("Should have caught an exception!", caughtException);
    assertNotNull(schema.getPropertyDefinitionTree("foo"));
  }

  public static Test suite() {
    TestSuite suite = new TestSuite(TestPropertySchema.class);
    return suite;
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}
