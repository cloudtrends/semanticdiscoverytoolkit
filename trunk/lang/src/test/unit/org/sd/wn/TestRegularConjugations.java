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
package org.sd.wn;


import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.util.List;

/**
 * JUnit Tests for the RegularConjugations class.
 * <p>
 * @author Spence Koehler
 */
public class TestRegularConjugations extends TestCase {

  public TestRegularConjugations(String name) {
    super(name);
  }
  

  public void doWordTest(String input, String[] expectedBases, POS[] expectedWordPoss, POS[] expectedBasePoss) {
    final List<RegularConjugations.Word> words = RegularConjugations.getInstance().getPotentialWords(input);

    if (expectedBases == null) {
      assertNull(words);
    }
    else {
      assertEquals(expectedBases.length, words.size());

      int index = 0;
      for (RegularConjugations.Word word : words) {
        assertEquals("index=" + index, expectedBases[index], word.base);
        assertEquals("index=" + index, expectedWordPoss[index], word.wordPos);
        assertEquals("index=" + index, expectedBasePoss[index], word.basePos);

        ++index;
      }
    }
  }

  public void test1() {
    doWordTest("baddies", new String[]{"baddy", "baddie"}, new POS[]{POS.NOUN, POS.NOUN}, new POS[]{POS.NOUN, POS.NOUN});
    doWordTest("bies", new String[]{"by", "bie"}, new POS[]{POS.NOUN, POS.NOUN}, new POS[]{POS.NOUN, POS.NOUN});
    doWordTest("ies", new String[]{"i", "ie"}, new POS[]{POS.NOUN, POS.NOUN}, new POS[]{POS.NOUN, POS.NOUN});
    doWordTest("mom's", new String[]{"mom"}, new POS[]{POS.NOUN}, new POS[]{POS.NOUN});
    doWordTest("birches", new String[]{"birch", "birche"}, new POS[]{POS.NOUN, POS.NOUN}, new POS[]{POS.NOUN, POS.NOUN});
    doWordTest("rolls", new String[]{"roll", "rol", "roll", "rol"},
               new POS[]{POS.NOUN, POS.NOUN, POS.VERB, POS.VERB},
               new POS[]{POS.NOUN, POS.NOUN, POS.NOUN, POS.NOUN});
    doWordTest("rolled", new String[]{"roll", "rol"}, new POS[]{POS.VERB, POS.VERB}, new POS[]{POS.VERB, POS.VERB});
    doWordTest("rolling", new String[]{"roll", "rol"}, new POS[]{POS.VERB, POS.VERB}, new POS[]{POS.VERB, POS.VERB});
    doWordTest("roller", new String[]{"roll", "rol", "roll", "rol"},
               new POS[]{POS.NOUN, POS.NOUN, POS.ADJ, POS.ADJ},
               new POS[]{POS.VERB, POS.VERB, POS.VERB, POS.VERB});
    doWordTest("bagged", new String[]{"bagg", "bag"}, new POS[]{POS.VERB, POS.VERB}, new POS[]{POS.VERB, POS.VERB});
    doWordTest("running", new String[]{"runn", "run"}, new POS[]{POS.VERB, POS.VERB}, new POS[]{POS.VERB, POS.VERB});
    doWordTest("runner", new String[]{"runn", "run", "runn", "run"},
               new POS[]{POS.NOUN, POS.NOUN, POS.ADJ, POS.ADJ},
               new POS[]{POS.VERB, POS.VERB, POS.VERB, POS.VERB});
    doWordTest("greater", new String[]{"great", "great"}, new POS[]{POS.NOUN, POS.ADJ}, new POS[]{POS.VERB, POS.VERB});
    doWordTest("greatest", new String[]{"great"}, new POS[]{POS.ADJ}, new POS[]{POS.ADJ});
  }

  public void test2() {
    doWordTest("greatest", new String[]{"great"}, new POS[]{POS.ADJ}, new POS[]{POS.ADJ});
    doWordTest("funniest", new String[]{"funny"}, new POS[]{POS.ADJ}, new POS[]{POS.ADJ});
    doWordTest("funnier", new String[]{"funny", "funny"}, new POS[]{POS.NOUN, POS.ADJ}, new POS[]{POS.VERB, POS.VERB});
    doWordTest("funnies", new String[]{"funny", "funnie"}, new POS[]{POS.NOUN, POS.NOUN}, new POS[]{POS.NOUN, POS.NOUN});
    doWordTest("funned", new String[]{"funn", "fun"}, new POS[]{POS.VERB, POS.VERB}, new POS[]{POS.VERB, POS.VERB});
    doWordTest("carried", new String[]{"carry"}, new POS[]{POS.VERB}, new POS[]{POS.VERB});
    doWordTest("quickly", new String[]{"quick"}, new POS[]{POS.ADV}, new POS[]{POS.ADJ});
    doWordTest("lummox", null, null, null);
  }


  public static Test suite() {
    TestSuite suite = new TestSuite(TestRegularConjugations.class);
    return suite;
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}
