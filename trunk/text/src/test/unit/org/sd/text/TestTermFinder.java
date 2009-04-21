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

import org.sd.nlp.NormalizedString;
import org.sd.nlp.GeneralNormalizer;

/**
 * JUnit Tests for the TermFinder class.
 * <p>
 * @author Spence Koehler
 */
public class TestTermFinder extends TestCase {

  public TestTermFinder(String name) {
    super(name);
  }
  
  public void testFindAtEndOfString() {
    final TermFinder termFinder = new TermFinder("test", false);  // case-insensitive
    termFinder.loadTerms(new String[] {"St", "Nt", "Rd", "Pk", "Cir"});
    assertTrue(termFinder.hasPattern("30-32 Wycliffe Rd", PatternFinder.FULL_WORD));
    assertTrue(termFinder.hasPattern("Rd", PatternFinder.FULL_WORD));

    assertEquals("Rd", termFinder.findPattern("30-32 Wycliffe Rd", PatternFinder.FULL_WORD));
    assertEquals("Rd", termFinder.findPattern("Rd", PatternFinder.FULL_WORD));
  }

  public void testFindInString() {
    final TermFinder termFinder = new TermFinder("test", true);  // case-sensitive so camel casing will apply
    termFinder.loadTerms(new String[] {"St", "Nt", "Rd", "Pk", "Cir"});
    assertTrue(termFinder.hasPattern("OldStNick", PatternFinder.FULL_WORD));
    assertEquals("St", termFinder.findPattern("OldStNick", PatternFinder.FULL_WORD));
  }

  public void testFindInNormalizedString() {
    final TermFinder termFinder = new TermFinder("test", false);  // case-insensitive, but camel casing should now apply
    termFinder.loadTerms(new String[] {"St", "Nt", "Rd", "Pk", "Cir"});
    assertTrue(termFinder.hasPattern("OldStNick", PatternFinder.FULL_WORD));
    assertEquals("St", termFinder.findPattern("OldStNick", PatternFinder.FULL_WORD));
  }

  public void testFindFullWordAtBeginningOfString() {
    final TermFinder termFinder = new TermFinder("test", false);  // case-insensitive
    termFinder.loadTerms(new String[] {"box"});
    assertTrue(termFinder.hasPattern("BOX 158", PatternFinder.FULL_WORD));
  }

  private final void doSplitTest(TermFinder termFinder, String string, String[] expected) {
    final NormalizedString[] pieces = termFinder.split(string);

    if (expected == null) {
      assertNull(pieces);
    }
    else {
      assertEquals(expected.length, pieces.length);

      int index = 0;
      for (NormalizedString piece : pieces) {
        final String original = piece.getOriginal();
        assertEquals(expected[index], original);
        ++index;
      }
    }
  }

  public void testSplit() {
    final TermFinder termFinder = new TermFinder("test-split", true, new String[] {
      "foo", "bar", "baz",
    });
    
    doSplitTest(termFinder, "AfooBbarC", new String[]{"A", "B", "C"});
    doSplitTest(termFinder, "AfooBbarCbaz", new String[]{"A", "B", "C"});
    doSplitTest(termFinder, "bazAfooBbarC", new String[]{"A", "B", "C"});
    doSplitTest(termFinder, "bazAfooBbarCbaz", new String[]{"A", "B", "C"});
    doSplitTest(termFinder, "fooA", new String[]{"A"});
    doSplitTest(termFinder, "Abar", new String[]{"A"});
    doSplitTest(termFinder, "A", new String[]{"A"});
    doSplitTest(termFinder, "", null);
    doSplitTest(termFinder, "fooAbar", new String[]{"A"});
  }

  public void testSplitKeepMatch() {
    final TermFinder termFinder = new TermFinder("test-split-keep-match", true, new String[] {
      "on",
    });
    
    final NormalizedString nString = GeneralNormalizer.getCaseSensitiveInstance().normalize("Air Con Plant");

    NormalizedString[] splits = termFinder.split(nString, TermFinder.FULL_WORD, true);
    assertEquals(1, splits.length);
    assertEquals("Air Con Plant", splits[0].getNormalized());

    splits = termFinder.split(nString, TermFinder.END_WORD, true);
    assertEquals(3, splits.length);
    assertEquals("Air C", splits[0].getNormalized());
    assertEquals("on", splits[1].getNormalized());
    assertEquals("Plant", splits[2].getNormalized());
  }

  public static Test suite() {
    TestSuite suite = new TestSuite(TestTermFinder.class);
    return suite;
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}
