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
package org.sd.match;


import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * JUnit Tests for the Decomp class.
 * <p>
 * @author Spence Koehler
 */
public class TestDecomp extends TestCase {

  public TestDecomp(String name) {
    super(name);
  }
  
  /**
   * Do an integrity check on the types to make sure all have been added to the set.
   */
  public void testTypeIntegrity() {
    //
    // if/when this fails, fix the code; Don't modify this test!
    //
    assertEquals("FailedIntegrityCheck! all defined types haven't been added to types set!",
                 Decomp.Type._FINAL_.ordinal(), Decomp.getTypes().size());
  }

  public void testDecompTypeMaps() {
    assertTrue(Decomp.getTypes().contains(Decomp.Type.BASE));
    assertTrue(Decomp.getTypes().contains(Decomp.Type.GROUP_NAME));

    assertEquals(Decomp.Type.BASE, Decomp.getType(Decomp.Type.BASE.ordinal()));
    assertEquals(Decomp.Type.GROUP_NAME, Decomp.getType(Decomp.Type.GROUP_NAME.ordinal()));

    assertEquals(Decomp.Type.BASE, Decomp.getType(Decomp.Type.BASE.name()));
    assertEquals(Decomp.Type.GROUP_NAME, Decomp.getType(Decomp.Type.GROUP_NAME.name()));

    assertEquals(Decomp.Type.BASE, Decomp.getTypeByFieldName(Decomp.Type.BASE.getFieldName()));
    assertEquals(Decomp.Type.GROUP_NAME, Decomp.getTypeByFieldName(Decomp.Type.GROUP_NAME.getFieldName()));

    assertEquals(Decomp.Type.BASE, Decomp.getTypeByText(Decomp.Type.BASE.getText()));
    assertEquals(Decomp.Type.GROUP_NAME, Decomp.getTypeByText(Decomp.Type.GROUP_NAME.getText()));
  }

  public static Test suite() {
    TestSuite suite = new TestSuite(TestDecomp.class);
    return suite;
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}
