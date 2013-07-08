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

import org.sd.xml.EntityConverter;

/**
 * JUnit Tests for the TokenPointerFactory classes as identified by TokenizationStrategy.
 * <p>
 * @author Spence Koehler
 */
public class TestTokenizationStrategy extends TestCase {

  public TestTokenizationStrategy(String name) {
    super(name);
  }
  
  // always define a term so that we spin through all tokens.
  private final class AlwaysDefineLexicon extends AbstractLexicon {
    private Category category;
    AlwaysDefineLexicon() {
      super(new GeneralNormalizer(false));
      this.category = new CategoryFactory().defineCategory("ALWAYS", false);
    }
    protected boolean alreadyHasTypes(Categories categories) {
      return categories.hasType(category);
    }
    protected void define(StringWrapper.SubString subString, AbstractNormalizer normalizer) {
      subString.addCategory(category);
    }
  }

  private final void doTokenizationTest(TokenizationStrategy.Type type, Lexicon lexicon, String line, String[] expected) {
    final TokenPointerFactory tpf = TokenizationStrategy.getStrategy(type, lexicon);
    TokenPointer tokenPointer = tpf.getFirstPointer(new StringWrapper(EntityConverter.unescape(line)), 0);
    int index = 0;

    if (expected == null) {
      System.out.println("\ntokenization(" + type + ", \"" + line + "\") = new String[]{");
      System.out.print("\t\"" + tokenPointer.getString() + "\",");
    }
    else {
      assertEquals(expected[index++], tokenPointer.getString());
    }

    while (tokenPointer != null) {
      int minSize = tokenPointer.getString().length();
      TokenPointer lastTokenPointer = tokenPointer;
      while ((tokenPointer = tokenPointer.revise()) != null) {
        if (expected == null) {
          System.out.print(" \"" + tokenPointer.getString() + "\",");
        }
        else {
          assertEquals(expected[index++], tokenPointer.getString());
        }
        final int curSize = tokenPointer.getString().length();
        if (curSize < minSize) {
          minSize = curSize;
          lastTokenPointer = tokenPointer;
        }
      }
      tokenPointer = lastTokenPointer.next(false);
      if (tokenPointer != null) {
        if (expected == null) {
          System.out.println();
          System.out.print("\t\"" + tokenPointer.getString() + "\",");
        }
        else {
          assertEquals(expected[index++], tokenPointer.getString());
        }
      }
    }

    if (expected == null) {
      System.out.println();
      System.out.println("}");
    }
  }

  public void testLines() {
    final Lexicon lexicon = new AlwaysDefineLexicon();
    doTokenizationTest(TokenizationStrategy.Type.LONGEST_TO_SHORTEST_TO_LONGEST, lexicon, "SatDec31st &middot; 4:00AM",
                       new String[]{
                         "SatDec31st", "Sat", "SatDec", "SatDec31st", "SatDec31st · 4", "SatDec31st · 4:00AM",
                         "Dec31st", "Dec", "Dec31st", "Dec31st · 4", "Dec31st · 4:00AM",
                         "31st",
                         "4:00AM", "4", "4:00AM",
                         "00AM",
                       });
    doTokenizationTest(TokenizationStrategy.Type.LONGEST_TO_SHORTEST_TO_LONGEST, lexicon, "SatDec31st &middot; 4:00A.M.",
                       new String[]{
                         "SatDec31st", "Sat", "SatDec", "SatDec31st", "SatDec31st · 4", "SatDec31st · 4:00A", "SatDec31st · 4:00A.M",
                         "Dec31st", "Dec", "Dec31st", "Dec31st · 4", "Dec31st · 4:00A", "Dec31st · 4:00A.M",
                         "31st",
                         "4:00A.M.", "4", "4:00A", "4:00A.M",
                         "00A.M.", "00A", "00A.M",
                         "M.", "M",
                       });
    doTokenizationTest(TokenizationStrategy.Type.SHORTEST_ONLY, lexicon, "SatDec31st &middot; 4:00A.M.",
                       new String[]{
                         "Sat",
                         "Dec",
                         "31st",
                         "4",
                         "00A",
                         "M",
                       });
    doTokenizationTest(TokenizationStrategy.Type.LONGEST_TO_SHORTEST, lexicon, "SatDec31st &middot; 4:00A.M.",
                       new String[]{
                         "SatDec31st", "SatDec", "Sat",
                         "Dec31st", "Dec",
                         "31st",
                         "4:00A.M.", "4:00A.M", "4:00A", "4",
                         "00A.M.", "00A.M", "00A",
                         "M.", "M",
                       });
    doTokenizationTest(TokenizationStrategy.Type.LONGEST_TO_SHORTEST_TO_LONGEST, lexicon, "-6:30 a.m-",
                       new String[]{
                         "6:30 a.m-", "6", "6:30", "6:30 a", "6:30 a.m",
                         "30 a.m-", "30", "30 a", "30 a.m",
                         "a.m-", "a", "a.m",
                         "m-", "m",
                       });
    doTokenizationTest(TokenizationStrategy.Type.LONGEST_TO_SHORTEST, lexicon, "Höfener Str. 1&3",
                       new String[]{
                         "Höfener Str", "Höfener",
                         "Str",
                         "1&3", "1",
                         "3",
                       });
  }

  public static Test suite() {
    TestSuite suite = new TestSuite(TestTokenizationStrategy.class);
    return suite;
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}
