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
 * JUnit Tests for the Form class.
 * <p>
 * @author Spence Koehler
 */
public class TestForm extends TestCase {

  public TestForm(String name) {
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
                 Form.Type._FINAL_.ordinal(), Form.getTypes().size());
  }

  public void testFormTypeMaps() {
    assertTrue(Form.getTypes().contains(Form.Type.FULL_UK_DESC));
    assertTrue(Form.getTypes().contains(Form.Type.FULL_US_DESC));

    assertEquals(Form.Type.FULL_UK_DESC, Form.getType(Form.Type.FULL_UK_DESC.ordinal()));
    assertEquals(Form.Type.FULL_US_DESC, Form.getType(Form.Type.FULL_US_DESC.ordinal()));

    assertEquals(Form.Type.FULL_UK_DESC, Form.getType(Form.Type.FULL_UK_DESC.name()));
    assertEquals(Form.Type.FULL_US_DESC, Form.getType(Form.Type.FULL_US_DESC.name()));
  }

  public static Test suite() {
    TestSuite suite = new TestSuite(TestForm.class);
    return suite;
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}
