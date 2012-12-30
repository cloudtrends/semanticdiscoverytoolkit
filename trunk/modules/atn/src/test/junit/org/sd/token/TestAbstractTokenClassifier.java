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
 * JUnit Tests for the AbstractTokenClassifier class.
 * <p>
 * @author Spence Koehler
 */
public class TestAbstractTokenClassifier extends TestCase {

  public TestAbstractTokenClassifier(String name) {
    super(name);
  }
  

  public void testIsDigits() {
    verifyDigits("76", 76);
    verifyDigits("0076", 76);
    verifyDigits("OO76", 76);
    verifyDigits("1B76", 1876);
    verifyDigits("lB76", 1876);
    verifyDigits("76lB", 7618);
    verifyDigits("0", 0);
    verifyDigits("0000", 0);
    verifyDigits("O", null);
    verifyDigits("LOB", null);
    verifyDigits("10B", 108);
    verifyDigits("1001", 1001);
  }

  private final void verifyDigits(String input, Integer expected) {
    final int[] gotValue = new int[]{0};
    final boolean parsedDigits = TokenClassifierHelper.isDigits(input, gotValue);

    if (expected == null) {
      assertFalse("Unexpectedly parsed digits from '" + input + "'", parsedDigits);
    }
    else {
      assertTrue("Couldn't parse ditigs from '" + input + "'", parsedDigits);
      assertEquals((int)expected, gotValue[0]);
    }
  }


  public static Test suite() {
    TestSuite suite = new TestSuite(TestAbstractTokenClassifier.class);
    return suite;
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}
