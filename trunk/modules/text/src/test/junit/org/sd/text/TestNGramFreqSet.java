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
 * JUnit Tests for the NGramFreqSet class.
 * <p>
 * @author Spence Koehler
 */
public class TestNGramFreqSet extends TestCase {

  public TestNGramFreqSet(String name) {
    super(name);
  }
  

  public void testBuild() {
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

    final NGramFreqSet ngrams = new NGramFreqSet(true);
    int index = 0;
    for (NGramFreq ngramFreq : ngramFreqs) {
      if (index < 5) {
        assertTrue(ngrams.add(ngramFreq));
      }
      else {
        assertFalse(ngrams.add(ngramFreq));
      }
      ++index;
    }

    assertEquals(5, ngrams.size());
  }

  public void testCollapse1() {
    final NGramFreq[] ngramFreqs = new NGramFreq[] {
      new NGramFreq("sun microsystems inc all rights reserved use is subject to", 9045, 10),
      new NGramFreq("copyright 2008 sun microsystems inc all rights reserved use is", 9045, 10),
      new NGramFreq("2008 sun microsystems inc all rights reserved use is subject", 9045, 10),
      new NGramFreq("inc all rights reserved use is subject to license terms", 9045, 10),
      new NGramFreq("microsystems inc all rights reserved use is subject to license", 9045, 10),
    };

    final NGramFreqSet ngrams = new NGramFreqSet(true);
    for (NGramFreq ngramFreq : ngramFreqs) {
      assertTrue(ngrams.add(ngramFreq));
    }

    assertTrue(ngrams.isCollapsible());
    assertFalse(ngrams.isCollapsed());
    assertEquals(5, ngrams.size());

    ngrams.collapse();

    assertTrue(ngrams.isCollapsible());
    assertTrue(ngrams.isCollapsed());

    final List<NGramFreq> collapsed = ngrams.getNGrams();

    assertEquals(1, ngrams.size());
    assertEquals(1, collapsed.size());

    final NGramFreq collapsedNGram = collapsed.get(0);
    assertEquals("copyright 2008 sun microsystems inc all rights reserved use is subject to license terms",
                 collapsedNGram.getNGram());
    assertEquals(9045, collapsedNGram.getFreq());
  }

  public void testCollapse2() {
    final NGramFreq[] ngramFreqs = new NGramFreq[] {
      new NGramFreq("workarounds and working code examples copyright 2008 sun microsystems inc", 8778, 10),
      new NGramFreq("documentation that documentation contains more detailed developer targeted descriptions with", 8778, 10),
      new NGramFreq("conceptual overviews definitions of terms workarounds and working code examples", 8778, 10),
      new NGramFreq("reference and developer documentation see java se developer documentation that", 8778, 10),
      new NGramFreq("use is subject to license terms also see the documentation", 8778, 10),
      new NGramFreq("rights reserved use is subject to license terms also see", 8778, 10),
      new NGramFreq("detailed developer targeted descriptions with conceptual overviews definitions of terms", 8778, 10),
      new NGramFreq("examples copyright 2008 sun microsystems inc all rights reserved use", 8778, 10),
      new NGramFreq("code examples copyright 2008 sun microsystems inc all rights reserved", 8778, 10),
      new NGramFreq("working code examples copyright 2008 sun microsystems inc all rights", 8778, 10),
      new NGramFreq("documentation contains more detailed developer targeted descriptions with conceptual overviews", 8778, 10),
      new NGramFreq("reserved use is subject to license terms also see the", 8778, 10),
      new NGramFreq("api reference and developer documentation see java se developer documentation", 8778, 10),
      new NGramFreq("and developer documentation see java se developer documentation that documentation", 8778, 10),
      new NGramFreq("see java se developer documentation that documentation contains more detailed", 8778, 10),
      new NGramFreq("of terms workarounds and working code examples copyright 2008 sun", 8778, 10),
      new NGramFreq("that documentation contains more detailed developer targeted descriptions with conceptual", 8778, 10),
      new NGramFreq("and working code examples copyright 2008 sun microsystems inc all", 8778, 10),
      new NGramFreq("se developer documentation that documentation contains more detailed developer targeted", 8778, 10),
      new NGramFreq("all rights reserved use is subject to license terms also", 8778, 10),
      new NGramFreq("targeted descriptions with conceptual overviews definitions of terms workarounds and", 8778, 10),
      new NGramFreq("is subject to license terms also see the documentation redistribution", 8778, 10),
      new NGramFreq("further api reference and developer documentation see java se developer", 8778, 10),
      new NGramFreq("java se developer documentation that documentation contains more detailed developer", 8778, 10),
      new NGramFreq("developer targeted descriptions with conceptual overviews definitions of terms workarounds", 8778, 10),
      new NGramFreq("documentation see java se developer documentation that documentation contains more", 8778, 10),
      new NGramFreq("subject to license terms also see the documentation redistribution policy", 8778, 10),
      new NGramFreq("developer documentation see java se developer documentation that documentation contains", 8778, 10),
      new NGramFreq("descriptions with conceptual overviews definitions of terms workarounds and working", 8778, 10),
      new NGramFreq("with conceptual overviews definitions of terms workarounds and working code", 8778, 10),
      new NGramFreq("contains more detailed developer targeted descriptions with conceptual overviews definitions", 8778, 10),
      new NGramFreq("definitions of terms workarounds and working code examples copyright 2008", 8778, 10),
      new NGramFreq("developer documentation that documentation contains more detailed developer targeted descriptions", 8778, 10),
      new NGramFreq("overviews definitions of terms workarounds and working code examples copyright", 8778, 10),
      new NGramFreq("for further api reference and developer documentation see java se", 8778, 10),
      new NGramFreq("more detailed developer targeted descriptions with conceptual overviews definitions of", 8778, 10),
      new NGramFreq("terms workarounds and working code examples copyright 2008 sun microsystems", 8778, 10),
    };

    final NGramFreqSet ngrams = new NGramFreqSet(true);
    for (NGramFreq ngramFreq : ngramFreqs) {
      assertTrue(ngrams.add(ngramFreq));
    }

    final List<NGramFreq> collapsed = ngrams.getCollapsedNGrams();

    assertEquals(1, collapsed.size());

    final NGramFreq collapsedNGram = collapsed.get(0);
    assertEquals("for further api reference and developer documentation see java se developer documentation that documentation contains more detailed developer targeted descriptions with conceptual overviews definitions of terms workarounds and working code examples copyright 2008 sun microsystems inc all rights reserved use is subject to license terms also see the documentation redistribution policy",
                 collapsedNGram.getNGram());
    assertEquals(8778, collapsedNGram.getFreq());
  }


  public static Test suite() {
    TestSuite suite = new TestSuite(TestNGramFreqSet.class);
    return suite;
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}
