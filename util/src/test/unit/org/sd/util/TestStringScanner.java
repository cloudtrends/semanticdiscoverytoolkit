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
package org.sd.util;


import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * JUnit Tests for the StringScanner class.
 * <p>
 * @author Spence Koehler
 */
public class TestStringScanner extends TestCase {

  public TestStringScanner(String name) {
    super(name);
  }
  
  private void validateNextValidValues(StringScanner.CharFunction charFunction, String string, int startIndex, int maxClosedParens, int numValuesToCapture, String expectedCaptured) {
    final StringScanner stringScanner = new StringScanner(charFunction, string, startIndex, maxClosedParens);
    final int[] values = stringScanner.nextValidValues(numValuesToCapture, true);
    final String captured = stringScanner.getCapturedString(true);

    assertEquals(expectedCaptured, captured);
  }

  public void testNextValidValues() {
    final StringScanner.CharFunction charFunction = new StringScanner.NumberCharFunction();

    validateNextValidValues(charFunction, "123456", 0, 0, 6, "123456");
    validateNextValidValues(charFunction, "123 456", 0, 0, 6, "123 456");
    validateNextValidValues(charFunction, "O1 23 45", 0, 0, 6, "O1 23 45");
    validateNextValidValues(charFunction, "O1-23-45", 0, 0, 6, "O1-23-45");

    validateNextValidValues(charFunction, "1234567", 0, 0, 6, "123456");

    validateNextValidValues(charFunction, "-12345", 0, 0, 5, "-12345");  // because '-' is a known char
    validateNextValidValues(charFunction, "+12345", 0, 0, 5, "");        // because '+' isn't a 'known' char
  }

  public static Test suite() {
    TestSuite suite = new TestSuite(TestStringScanner.class);
    return suite;
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}
