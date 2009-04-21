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
 * JUnit Tests for the NumberFinder class.
 * <p>
 * @author Spence Koehler
 */
public class TestNumberFinder extends TestCase {

  public TestNumberFinder(String name) {
    super(name);
  }
  
  private final void doNumberTest(String expectedNumber, String input) {
    assertEquals(expectedNumber, new NumberFinder("test1").findPattern(input, PatternFinder.FULL_WORD));
  }

  public void test1() {
    doNumberTest("two thousand seven", "the year is two thousand seven!");
    doNumberTest("2nd, 2007", "it is March 2nd, 2007");
    doNumberTest("W129N10825", "W129N10825 Washington Drive");
    doNumberTest("W130 N10751", "W130 N10751 Washington Drive");
    doNumberTest("W 140 N 9572", "W 140 N 9572 Fountain Boulevard");
    doNumberTest(null, " -- nothing here! --");
    doNumberTest("123", "here is a number -- 123 --- and symbols");
    doNumberTest("VIII", "Henry VIII");
  }

  public static Test suite() {
    TestSuite suite = new TestSuite(TestNumberFinder.class);
    return suite;
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}
