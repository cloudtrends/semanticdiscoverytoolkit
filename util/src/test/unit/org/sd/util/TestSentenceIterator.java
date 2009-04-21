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
 * JUnit Tests for the SentenceIterator class.
 * <p>
 * @author Spence Koehler
 */
public class TestSentenceIterator extends TestCase {

  public TestSentenceIterator(String name) {
    super(name);
  }
  

	public void doTest(String input, String[] expected) {
		final SentenceIterator iter = new SentenceIterator(input);
		int count = 0;
		while (iter.hasNext()) {
			final String text = iter.next();
			assertEquals("(" + count + ")", expected[count], text);
			++count;
		}

		assertEquals(expected.length, count);
	}

	public void testSimple() {
		doTest("This is a test. This is only a test.",
					 new String[] {
						 "This is a test.",
						 "This is only a test.",
					 });
	}

	public void testBadAbbreviation1() {
		// NOTE: This doesn't work quite right. If we find that it is fixed in the
		//       future, we can set this test to rights!
		doTest("Try parsing beyond tokens like Ph.D. and Dr. Smith, if you please.",
					 new String[] {
						 "Try parsing beyond tokens like Ph.D. and Dr.",
						 "Smith, if you please.",
					 });
	}


  public static Test suite() {
    TestSuite suite = new TestSuite(TestSentenceIterator.class);
    return suite;
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}
