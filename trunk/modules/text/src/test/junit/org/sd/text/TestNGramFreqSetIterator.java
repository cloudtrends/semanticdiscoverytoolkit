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


import java.util.Iterator;
import java.util.ArrayList;
import java.util.List;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * JUnit Tests for the NGramFreqSetIterator class.
 * <p>
 * @author Spence Koehler
 */
public class TestNGramFreqSetIterator extends TestCase {

  public TestNGramFreqSetIterator(String name) {
    super(name);
  }
  

  private final List<NGramFreq> getNGrams() {
    final NGramFreq[] ngramFreqs = new NGramFreq[] {
      new NGramFreq("sun microsystems inc all rights reserved use is subject to", 9045, 10),
      new NGramFreq("copyright 2008 sun microsystems inc all rights reserved use is", 9045, 10),
      new NGramFreq("2008 sun microsystems inc all rights reserved use is subject", 9045, 10),
      new NGramFreq("inc all rights reserved use is subject to license terms", 9045, 10),
      new NGramFreq("microsystems inc all rights reserved use is subject to license", 9045, 10),
      new NGramFreq("workarounds and working code examples copyright 2008 sun microsystems inc", 8778, 10),
      new NGramFreq("documentation that documentation contains more detailed developer targeted descriptions with", 8778, 10),
      new NGramFreq("conceptual overviews definitions of terms workarounds and working code examples", 8778, 10),
    };

    final List<NGramFreq> ngrams = new ArrayList<NGramFreq>();
    for (NGramFreq ngram : ngramFreqs) {
      ngrams.add(ngram);
    }

    return ngrams;
  }

  public void testIterationNoSortNoCollapse() {
    final List<NGramFreq> ngrams = getNGrams();

    final int[] expectedNumNgrams = new int[]{5, 3};
    int count = 0;
    for (Iterator<NGramFreqSet> iter = new NGramFreqSetIterator(ngrams, true, false); iter.hasNext(); ) {
      final NGramFreqSet ngramSet = iter.next();
      assertEquals(expectedNumNgrams[count], ngramSet.size());
      ngramSet.collapse();
      assertEquals(expectedNumNgrams[count], ngramSet.size());
      ++count;
    }

    assertEquals(expectedNumNgrams.length, count);
  }

  public void testIterationSortNoCollapse() {
    final List<NGramFreq> ngrams = getNGrams();

    final int[] expectedNumNgrams = new int[]{5, 3};
    int count = 0;
    for (Iterator<NGramFreqSet> iter = new NGramFreqSetIterator(ngrams, false, false); iter.hasNext(); ) {
      final NGramFreqSet ngramSet = iter.next();
      assertEquals(expectedNumNgrams[count], ngramSet.size());
      ngramSet.collapse();
      assertEquals(expectedNumNgrams[count], ngramSet.size());
      ++count;
    }

    assertEquals(expectedNumNgrams.length, count);
  }

  public void testIterationSortCollapse() {
    final List<NGramFreq> ngrams = getNGrams();

    final int[] expectedNumNgrams = new int[]{5, 3};
    final int[] expectedNumCollapsedNgrams = new int[]{1, 2};
    int count = 0;
    for (Iterator<NGramFreqSet> iter = new NGramFreqSetIterator(ngrams, false, true); iter.hasNext(); ) {
      final NGramFreqSet ngramSet = iter.next();
      assertEquals(expectedNumNgrams[count], ngramSet.size());
      ngramSet.collapse();
      assertEquals(expectedNumCollapsedNgrams[count], ngramSet.size());
      ++count;
    }

    assertEquals(expectedNumNgrams.length, count);
  }


  public static Test suite() {
    TestSuite suite = new TestSuite(TestNGramFreqSetIterator.class);
    return suite;
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}
