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
 * JUnit Tests for the Word class.
 * <p>
 * @author Spence Koehler
 */
public class TestWord extends TestCase {

  public TestWord(String name) {
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
                 Word.Type._FINAL_.ordinal(), Word.getTypes().size());
  }

  public void testWordTypeMaps() {
    assertTrue(Word.getTypes().contains(Word.Type.NORMAL));
    assertTrue(Word.getTypes().contains(Word.Type.ACRONYM));

    assertEquals(Word.Type.NORMAL, Word.getType(Word.Type.NORMAL.ordinal()));
    assertEquals(Word.Type.ACRONYM, Word.getType(Word.Type.ACRONYM.ordinal()));

    assertEquals(Word.Type.NORMAL, Word.getType(Word.Type.NORMAL.name()));
    assertEquals(Word.Type.ACRONYM, Word.getType(Word.Type.ACRONYM.name()));
  }

  public static Test suite() {
    TestSuite suite = new TestSuite(TestWord.class);
    return suite;
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}
