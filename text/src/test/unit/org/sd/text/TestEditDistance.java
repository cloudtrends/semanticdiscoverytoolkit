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
package org.sd.text;


import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * JUnit Tests for the EditDistance class.
 * <p>
 * @author Spence Koehler
 */
public class TestEditDistance extends TestCase {

  public TestEditDistance(String name) {
    super(name);
  }
  
  public void testLev1a() {
    assertEquals(0, EditDistance.lev("same", "same"));           // same
    assertEquals(2, EditDistance.lev("this", "that"));           // i -> s, s -> t
    assertEquals(2, EditDistance.lev("this", "tihs"));           // swap h, i
    assertEquals(1, EditDistance.lev("something", "somthing"));  // delete e
    assertEquals(1, EditDistance.lev("john", "johnn"));          // add n
    assertEquals(9, EditDistance.lev("abcdefg", "123456789"));   // all differ
    assertEquals(9, EditDistance.lev("123456789", "abcdefg"));   // all differ
    assertEquals(1, EditDistance.lev("johnn", "john"));          // add n
    assertEquals(1, EditDistance.lev("somthing", "something"));  // delete e
    assertEquals(2, EditDistance.lev("tihs", "this"));           // swap h, i
    assertEquals(2, EditDistance.lev("that", "this"));           // i -> s, s -> t
  }

  public void testLev1b() {
//    System.out.println("john a smith  john smith " + EditDistance.lev("john a smith".split(" "), "john smith".split(" ")));
//    System.out.println("jj smith  john j smith " + EditDistance.lev("jj smith".split(" "), "john j smith".split(" ")));
  }

  public void testAbbrev() {
    assertEquals(0, EditDistance.levAbbrev("E", "Edward"));
    assertEquals(0, EditDistance.levAbbrev("E.", "Edward"));
    assertEquals(0, EditDistance.levAbbrev("Eng", "England"));
    assertEquals(0, EditDistance.levAbbrev("Eng.", "England"));

    assertEquals(-1, EditDistance.levAbbrev("England", "Eng"));

    assertEquals(1, EditDistance.levAbbrev("Enag", "England"));
    assertEquals(1, EditDistance.levAbbrev("Enag.", "England"));

    assertEquals(0, EditDistance.levAbbrev("Slt Lk", "Salt Lake"));
    assertEquals(0, EditDistance.levAbbrev("Salt Lk", "Salt Lake"));
    assertEquals(-1, EditDistance.levAbbrev("Abc", "Bbc"));

    assertEquals(0, EditDistance.levAbbrev("slake", "salt lake"));
    assertEquals(0, EditDistance.levAbbrev("s.lake", "salt lake"));
    assertEquals(0, EditDistance.levAbbrev("s.lake", "salt lake city"));

    assertEquals(-1, EditDistance.levAbbrev("slat lake city", "salt lake city"));

    int got = EditDistance.levAbbrev("somewhere", "salt lake");
    assertEquals(-1, got);
  }

  public static Test suite() {
    TestSuite suite = new TestSuite(TestEditDistance.class);
    return suite;
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}
