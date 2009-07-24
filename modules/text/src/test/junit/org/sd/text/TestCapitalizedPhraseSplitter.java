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


import java.util.List;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * JUnit Tests for the CapitalizedPhraseSplitter class.
 * <p>
 * @author Spence Koehler
 */
public class TestCapitalizedPhraseSplitter extends TestCase {

  public TestCapitalizedPhraseSplitter(String name) {
    super(name);
  }
  

  private final void doGetWordGramsTest(WordGramSplitter splitter, String input, String[] expectedWordGrams) {
    final List<String> wordGrams = splitter.getWordGrams(input);

    assertEquals(expectedWordGrams.length, wordGrams.size());
    int index = 0;
    for (String wordGram : wordGrams) {
      assertEquals(expectedWordGrams[index++], wordGram);
    }
  }

  public void testGetWordGramsSkip2IncludeAllCaps() {
    final CapitalizedPhraseSplitter splitter = new CapitalizedPhraseSplitter(2, true);
    doGetWordGramsTest(splitter,
                       "The National Association of Weirdos held their quasi-annual Weirdo Convention",
                       new String[] {
                         "The National Association of Weirdos",
                         "Weirdo Convention"
                       });
  }


  public static Test suite() {
    TestSuite suite = new TestSuite(TestCapitalizedPhraseSplitter.class);
    return suite;
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}
