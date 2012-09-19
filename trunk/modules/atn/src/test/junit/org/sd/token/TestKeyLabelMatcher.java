/*
    Copyright 2011 Semantic Discovery, Inc. (www.semanticdiscovery.com)

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
package org.sd.token;


import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * JUnit Tests for the KeyLabelMatcher class.
 * <p>
 * @author Spence Koehler
 */
public class TestKeyLabelMatcher extends TestCase {

  public TestKeyLabelMatcher(String name) {
    super(name);
  }
  

  public void testUnpack() {
    final KeyLabelMatcher matcher = new KeyLabelMatcher("lcC[cCl][cC][lc][cC]nns");

    final String[] expected = new String[] {
      "l",
      "c",
      "C",
      "cCl",
      "cC",
      "lc",
      "cC",
      "n",
      "n",
      "s",
    };

    for (int i = 0; i < expected.length; ++i) {
      final KeyLabel keyLabel = matcher.KEY_LABELS[i];
      assertEquals(expected[i], matcher.getPattern(keyLabel));
    }
  }

  public void testCollapse() {
    final KeyLabelMatcher matcher = new KeyLabelMatcher("lcC[cCl][cC][lc][cC]nns");

    KeyLabel[] wordLabels = null;
    KeyLabel[] collapsed = null;

    wordLabels = new KeyLabel[]{KeyLabel.AllLower, KeyLabel.SingleLower, KeyLabel.LowerMixed};
    collapsed = matcher.collapse(wordLabels);
    assertEquals(1, collapsed.length);
    assertEquals(KeyLabel.AllLower, collapsed[0]);


    wordLabels = new KeyLabel[]{KeyLabel.AllLower, KeyLabel.SingleLower, KeyLabel.UpperMixed, KeyLabel.LowerMixed};
    collapsed = matcher.collapse(wordLabels);
    assertEquals(2, collapsed.length);
    assertEquals(KeyLabel.AllLower, collapsed[0]);
    assertEquals(KeyLabel.UpperMixed, collapsed[1]);
  }

/*
  public void testMatches() {
  }
*/

  public static Test suite() {
    TestSuite suite = new TestSuite(TestKeyLabelMatcher.class);
    return suite;
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}
