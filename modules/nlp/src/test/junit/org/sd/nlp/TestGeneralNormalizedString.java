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
package org.sd.nlp;


import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.sd.io.FileUtil;
//import org.sd.lucene.IndexingNormalizer;

import java.io.IOException;

/**
 * JUnit Tests for the NormalizedString class.
 * <p>
 * @author Spence Koehler
 */
public class TestGeneralNormalizedString extends TestCase {

  public TestGeneralNormalizedString(String name) {
    super(name);
  }
  
  private final void doTokenizingTest(String input, boolean skipToBreak, String[] expectedOriginal, String[] expectedNormalized) {
    final GeneralNormalizedString nString = (GeneralNormalizedString)GeneralNormalizer.getCaseInsensitiveInstance().normalize(input);
    doTokenizingTest(nString, skipToBreak, expectedOriginal, expectedNormalized);
  }
    
  private final void doTokenizingTest(GeneralNormalizedString nString, boolean skipToBreak, String[] expectedOriginal, String[] expectedNormalized) {
    int index = 0;
    for (GeneralNormalizedString.NormalizedToken token = nString.getToken(0, skipToBreak); token != null; token = token.getNext(skipToBreak)) {
      if (expectedOriginal == null && expectedNormalized == null) {
        System.out.println(index + ": o(" + token.getOriginal() + ") n(" + token.getNormalized() + ")");
      }
      else {
        assertEquals("" + index, expectedOriginal[index], token.getOriginal());
        assertEquals("" + index, expectedNormalized[index], token.getNormalized());
      }
      ++index;
    }
  }

  public void testTokenizing() {
    doTokenizingTest("Feb1st2007", true, new String[]{"Feb", "1", "st", "2007"}, new String[]{"feb", "1", "st", "2007"});
    doTokenizingTest(" Feb 1st 2007", true, new String[]{"Feb", "1", "st", "2007"}, new String[]{"feb", "1", "st", "2007"});
    doTokenizingTest(" Feb 1st 2007", false, new String[]{"Feb", " ", "1", "st", " ", "2007"}, new String[]{"feb", " ", "1", "st", " ", "2007"});
    doTokenizingTest("#1A", true, new String[]{"1", "A"}, new String[]{"1", "a"});
    doTokenizingTest(" -- #1A", true, new String[]{"1", "A"}, new String[]{"1", "a"});
  }

//   public void testAsianTokenizing() {
//     final String as1 = new StringBuilder().appendCodePoint(0x2E80).toString();
//     final String as2 = new StringBuilder().appendCodePoint(0x2E81).toString();
//     final String as3 = new StringBuilder().appendCodePoint(0x2E82).toString();

//     final String input = "foo" + as1 + as2 + as3 + "bar";
//     final NormalizedString nString = IndexingNormalizer.getInstance(IndexingNormalizer.ALL_OPTIONS).normalize(input);

//     doTokenizingTest(nString, true, new String[]{"foo", as1, as2, as3, "bar"}, new String[]{"foo", as1, as2, as3, "bar"});
//   }

  public void testGetPrecedingIndex() {
    final NormalizedString nString = new GeneralNormalizedString("this is a test of get preceding index");
    final int nPos = 32;

    assertEquals(22, nString.getPrecedingIndex(nPos, 1));
    assertEquals(18, nString.getPrecedingIndex(nPos, 2));
    assertEquals(15, nString.getPrecedingIndex(nPos, 3));
    assertEquals(10, nString.getPrecedingIndex(nPos, 4));
    assertEquals(8, nString.getPrecedingIndex(nPos, 5));
    assertEquals(5, nString.getPrecedingIndex(nPos, 6));
    assertEquals(0, nString.getPrecedingIndex(nPos, 7));
    assertEquals(0, nString.getPrecedingIndex(nPos, 8));
  }

  public void testGetOriginal() {
    final NormalizedString nString = GeneralNormalizer.getCaseInsensitiveInstance().normalize("-- gobble-dee-gook --.");
    // normalized is "--gobble-dee-gook--"

    assertEquals("gobble-dee-gook", nString.getOriginal(2, 15));
    assertEquals("gobble-dee-gook --", nString.getOriginal(2, 17));
  }

  public void testSplitCamelCase() {
    final GeneralNormalizedString nString = GeneralNormalizedString.buildLowerCaseInstance("thisIsATest");
    String[] tokens = null;

    tokens = nString.split();
    assertEquals(4, tokens.length);
    assertEquals("this", tokens[0]);         // first token
    assertEquals("thisisatest", tokens[1]);  // alt form
    assertEquals("is", tokens[2]);           // second token
    assertEquals("atest", tokens[3]);        // camel-casing algorithm currently doesn't split these

    nString.setSplitOnCamelCase(false);
    tokens = nString.split();
    assertEquals(1, tokens.length);
    assertEquals("thisisatest", tokens[0]);  // only form
  }

//   public void testArrayOutOfBoundsProblem1() throws IOException {
//     final String input = FileUtil.readAsString(FileUtil.getFile(this.getClass(), "resources/test-normalizedString-1.txt")).trim();
//     final NormalizedString normalized = IndexingNormalizer.getInstance(IndexingNormalizer.DEFAULT_INDEXING_OPTIONS).normalize(input);

// //    normalized.dump();

//     final String[] tokens = normalized.split();

//     assertEquals(26, tokens.length);
//     assertEquals("it", tokens[0]);
//     assertEquals("eastern", tokens[tokens.length - 1]);
//   }

//   public void testArrayOutOfBoundsProblems2() throws IOException {
//     final String input = "URL: http://www.";
//     final NormalizedString normalized = IndexingNormalizer.getInstance(IndexingNormalizer.DEFAULT_INDEXING_OPTIONS).normalize(input);

// //    normalized.dump();

//     final String[] tokens = normalized.split();

//     assertEquals(1, tokens.length);
//     assertEquals("url", tokens[0]);
//   }


  public static Test suite() {
    TestSuite suite = new TestSuite(TestGeneralNormalizedString.class);
    return suite;
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}
