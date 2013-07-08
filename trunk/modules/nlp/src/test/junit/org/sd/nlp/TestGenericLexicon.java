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

import java.io.IOException;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.sd.util.DelimitedString;

/**
 * JUnit Tests for the GenericLexicon class.
 * <p>
 * 
 * @author Dave Barney, Spence Koehler
 */
public class TestGenericLexicon extends TestCase {

  public TestGenericLexicon(String name) {
    super(name);
  }

  private void verify(Lexicon lexicon, String term, boolean expectDefined, Category category) {
    final StringWrapper.SubString subString = new StringWrapper(term).getSubString(0);
    lexicon.lookup(subString);
    final Categories def = subString.getCategories();

    if (expectDefined) {
      assertTrue(def.hasType(category));
    } else {
      assertTrue(def == null || !def.hasType(category));
    }
  }

  public void testTerms() throws IOException {
    final CategoryFactory cfactory = new CategoryFactory();
    cfactory.defineCategory("LOC_PREP", false);
    cfactory.defineCategory("CITY", false);
    cfactory.defineCategory("PREP", false);
    cfactory.defineCategory("ADV", false);
    cfactory.defineCategory("ADJ", true);
    cfactory.defineCategory("ADDRESS", false);

    //Test case-insensitive
    final AbstractNormalizer normalizer = GeneralNormalizer.getCaseSensitiveInstance();
//     final Normalizer normalizer = new Normalizer() {
//         public final NormalizedString normalize(StringWrapper.SubString subString) {
//           return normalize(subString.originalSubString);
//         }
//         public final NormalizedString normalize(String inputString) {
//           return new DelimitedString(inputString, null).getCleanInput(false);
//         }
//       };
    final Lexicon lexicon = new GenericLexicon(this.getClass().getResourceAsStream("resources/simple_loc_prep.txt"),
                                               normalizer, cfactory.getCategory("LOC_PREP"), false, false, false, null);

    verify(lexicon, "bei", true, cfactory.getCategory("LOC_PREP"));
    verify(lexicon, "am", true, cfactory.getCategory("LOC_PREP"));
    verify(lexicon, "kreis", true, cfactory.getCategory("LOC_PREP"));
    verify(lexicon, "b", true, cfactory.getCategory("LOC_PREP"));
    verify(lexicon, "a", true, cfactory.getCategory("LOC_PREP"));

    verify(lexicon, "Bei", true, cfactory.getCategory("LOC_PREP"));
    verify(lexicon, "Am", true, cfactory.getCategory("LOC_PREP"));
    verify(lexicon, "Kreis", true, cfactory.getCategory("LOC_PREP"));
    verify(lexicon, "B", true, cfactory.getCategory("LOC_PREP"));
    verify(lexicon, "A", true, cfactory.getCategory("LOC_PREP"));
    
    verify(lexicon, "B.", true, cfactory.getCategory("LOC_PREP"));
    verify(lexicon, "A.", true, cfactory.getCategory("LOC_PREP"));
    
    verify(lexicon, "foobar", false, cfactory.getCategory("LOC_PREP"));
    verify(lexicon, "c", false, cfactory.getCategory("LOC_PREP"));
    verify(lexicon, "c.", false, cfactory.getCategory("LOC_PREP"));
    verify(lexicon, "D", false, cfactory.getCategory("LOC_PREP"));
    verify(lexicon, "D.", false, cfactory.getCategory("LOC_PREP"));

    
    //Test category checking
    verify(lexicon, "bei", false, cfactory.getCategory("CITY"));
    verify(lexicon, "am", false, cfactory.getCategory("PREP"));
    verify(lexicon, "kreis", false, cfactory.getCategory("ADV"));
    verify(lexicon, "b", false, cfactory.getCategory("ADJ"));
    verify(lexicon, "a", false, cfactory.getCategory("ADDRESS"));
    verify(lexicon, "b.", false, cfactory.getCategory("ADJ"));
    verify(lexicon, "a.", false, cfactory.getCategory("ADDRESS"));

    
    //Test case-insensitive
    final Lexicon lexicon2 = new GenericLexicon(this.getClass().getResourceAsStream("resources/simple_loc_prep.txt"),
                                                normalizer, cfactory.getCategory("LOC_PREP"), true, false, false, null);

    verify(lexicon2, "bei", true, cfactory.getCategory("LOC_PREP"));
    verify(lexicon2, "am", true, cfactory.getCategory("LOC_PREP"));
    verify(lexicon2, "kreis", true, cfactory.getCategory("LOC_PREP"));
    verify(lexicon2, "B", true, cfactory.getCategory("LOC_PREP"));
    verify(lexicon2, "A", true, cfactory.getCategory("LOC_PREP"));
    verify(lexicon2, "B.", true, cfactory.getCategory("LOC_PREP"));
    verify(lexicon2, "A.", true, cfactory.getCategory("LOC_PREP"));
    
    verify(lexicon2, "b", false, cfactory.getCategory("LOC_PREP"));
    verify(lexicon2, "a", false, cfactory.getCategory("LOC_PREP"));
    verify(lexicon2, "b.", false, cfactory.getCategory("LOC_PREP"));
    verify(lexicon2, "a.", false, cfactory.getCategory("LOC_PREP"));
    verify(lexicon2, "Bei", false, cfactory.getCategory("LOC_PREP"));
    verify(lexicon2, "Am", false, cfactory.getCategory("LOC_PREP"));
    verify(lexicon2, "Kreis", false, cfactory.getCategory("LOC_PREP"));
  }

  public static Test suite() {
    TestSuite suite = new TestSuite(TestGenericLexicon.class);
    return suite;
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}
