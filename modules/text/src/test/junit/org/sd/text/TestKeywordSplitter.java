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
 * JUnit Tests for the KeywordSplitter class.
 * <p>
 * @author Spence Koehler
 */
public class TestKeywordSplitter extends TestCase {

  public TestKeywordSplitter(String name) {
    super(name);
  }
  
  private final void assertEquals(String[] expected, String[] got) {
    assertEquals(expected.length, got.length);

    for (int i = 0; i < expected.length; ++i) {
      assertEquals("index " + i, expected[i], got[i]);
    }
  }

  private final void doSplitOnLastTest(KeywordSplitter splitter, String[] expected, String string, KeywordSplitter.SplitType type) {
    final KeywordSplitter.Split[] splits = splitter.splitOnLast(string);
    final String[] strings = KeywordSplitter.getStrings(splits, type);
    assertEquals(expected, strings);
  }

  public void testSplitOnLastKeepAll() {
    final KeywordSplitter splitter = new KeywordSplitter(new String[]{"at", "the"}, new String[]{"first", "last", "least"});

    doSplitOnLastTest(splitter, new String[]{"at", "first", "at", "the", "very least", "and", "at", "last", "or", "at", "least last"},
                      "at first, at the very least, and at last, or at least last.", null);

    doSplitOnLastTest(splitter, new String[]{"at", "first", "at", "the", "very least", "and", "at", "last"},
                      "at first at the very least and at last", null);
    
    doSplitOnLastTest(splitter, new String[]{"in first", "and in last", "from least"},
                      "in first and in last from least", null);
    
    doSplitOnLastTest(splitter, new String[]{"in First", "and in Last", "from Least"},
                      "in First and in Last from Least", null);
    
    doSplitOnLastTest(splitter, new String[]{"in cognito"},
                      "in cognito", null);
    
    doSplitOnLastTest(splitter, new String[]{"at a rest home in", "the", "country"},
                      "at a rest home in the country", null);
  }


  public static Test suite() {
    TestSuite suite = new TestSuite(TestKeywordSplitter.class);
    return suite;
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}
