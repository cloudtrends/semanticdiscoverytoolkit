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
package org.sd.match;


import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * JUnit Tests for the MatchUtil class.
 * <p>
 * @author Spence Koehler
 */
public class TestMatchUtil extends TestCase {

  public TestMatchUtil(String name) {
    super(name);
  }
  
  public void testMergeConceptModels() {
    final ConceptModel c1 = new ConceptModel("(C2621 (F7 (T0 (S0 (V0 W0|architraves)))))");
    final ConceptModel c2 = new ConceptModel("(C307613 (F2 (T1 (S0 (V0 W0|architraves)))))");

    final String expected = "(C2621 (F7 (T0 (S0 (V0 W0|architraves)))) (F2 (T1 (S0 (V0 W0|architraves)))))";
    final ConceptModel merged = MatchUtil.mergeConceptModels(new ConceptModel[]{c1, c2});

    assertEquals(expected, merged.getTreeString());
  }

  public static Test suite() {
    TestSuite suite = new TestSuite(TestMatchUtil.class);
    return suite;
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}
