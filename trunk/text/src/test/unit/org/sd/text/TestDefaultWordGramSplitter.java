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

import java.util.List;

import org.sd.nlp.GeneralNormalizer;

/**
 * JUnit Tests for the WordGramSplitter class.
 * <p>
 * @author Spence Koehler
 */
public class TestDefaultWordGramSplitter extends TestCase {

  public TestDefaultWordGramSplitter(String name) {
    super(name);
  }
  
  public static Test suite() {
    TestSuite suite = new TestSuite(TestDefaultWordGramSplitter.class);
    return suite;
  }


  private final void doTest(WordGramSplitter splitter, String input, String[] expectedWordGrams) {
    final List<String> wordGrams = splitter.getWordGrams(input);

    assertEquals(expectedWordGrams.length, wordGrams.size());
    for (int i = 0; i < expectedWordGrams.length; ++i) {
      assertEquals(expectedWordGrams[i], wordGrams.get(i));
    }
  }

  public void testBasic2Gram() {
    final WordGramSplitter splitter = new DefaultWordGramSplitter(2, GeneralNormalizer.getCaseInsensitiveInstance(), null);
    doTest(splitter, "The quick brown fox jumped over the lazy dog.", new String[] {
        "the quick", "quick brown", "brown fox", "fox jumped", "jumped over", "over the", "the lazy", "lazy dog",
      });
  }

  public void testBasic3Gram() {
    final WordGramSplitter splitter = new DefaultWordGramSplitter(3, GeneralNormalizer.getCaseInsensitiveInstance(), null);
    doTest(splitter, "The quick brown fox jumped over the lazy dog.", new String[] {
        "the quick brown", "quick brown fox", "brown fox jumped", "fox jumped over", "jumped over the", "over the lazy", "the lazy dog",
      });
  }


  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}
