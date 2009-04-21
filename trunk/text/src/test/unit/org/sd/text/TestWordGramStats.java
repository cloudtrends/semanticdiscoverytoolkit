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

import java.util.Map;

/**
 * JUnit Tests for the WordGramStats class.
 * <p>
 * @author Spence Koehler
 */
public class TestWordGramStats extends TestCase {

  public TestWordGramStats(String name) {
    super(name);
  }
  

  public void testPruneOverlap() {
    final WordGramStats wordGramStats = new WordGramStats(1, 3, null, null);

    final String[] strings = new String[] {
      "high stakes poker",
      "poker",
      "high stakes gambling",
      "high stakes games",
      "high stakes casino",
      "gambling",
    };

    for (String string : strings) {
      wordGramStats.add(string);
      wordGramStats.flush(null);
    }

    wordGramStats.pruneOverlap();

    final String[][] expectedStrings = new String[][] {
      {"poker", "gambling",},  // 1-word strings
      {"high stakes",},        // 2-word strings
      {"high stakes games", "high stakes poker", "high stakes casino", "high stakes gambling",},  // 3-word strings
    };

    for (int i = 0; i < expectedStrings.length; ++i) {
      final String[] expected = expectedStrings[i];
      final Map<String, NGramFreq> ngram2freq = wordGramStats.getNgram2Freq(i + 1);

      for (String string : expected) {
        final NGramFreq ngram = ngram2freq.get(string);
        assertNotNull("No ngram for '" + string + "' where expected!", ngram);
        assertEquals(string, ngram.getNGram());
      }

      assertEquals(expected.length, ngram2freq.size());
    }
  }


  public static Test suite() {
    TestSuite suite = new TestSuite(TestWordGramStats.class);
    return suite;
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}
