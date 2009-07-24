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
 * JUnit Tests for the Synonym class.
 * <p>
 * @author Spence Koehler
 */
public class TestSynonym extends TestCase {

  public TestSynonym(String name) {
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
                 Synonym.Type._FINAL_.ordinal(), Synonym.getTypes().size());
  }

  public void testSynonymTypeMaps() {
    assertTrue(Synonym.getTypes().contains(Synonym.Type.PRIMARY));
    assertTrue(Synonym.getTypes().contains(Synonym.Type.CONJ));

    assertEquals(Synonym.Type.PRIMARY, Synonym.getType(Synonym.Type.PRIMARY.ordinal()));
    assertEquals(Synonym.Type.CONJ, Synonym.getType(Synonym.Type.CONJ.ordinal()));

    assertEquals(Synonym.Type.PRIMARY, Synonym.getType(Synonym.Type.PRIMARY.name()));
    assertEquals(Synonym.Type.CONJ, Synonym.getType(Synonym.Type.CONJ.name()));
  }

  public static Test suite() {
    TestSuite suite = new TestSuite(TestSynonym.class);
    return suite;
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}
