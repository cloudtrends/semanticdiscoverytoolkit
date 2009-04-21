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
package org.sd.text.align;


import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.sd.nlp.NormalizedString;
import org.sd.util.LineBuilder;

/**
 * JUnit Tests for the AlignmentNormalizer class.
 * <p>
 * @author Spence Koehler
 */
public class TestAlignmentNormalizer extends TestCase {

  public TestAlignmentNormalizer(String name) {
    super(name);
  }
  
  // expectations[0] = string to normalize
  // expectations[1] = normalized string
  // expectations[2+] = tokens from splitting normalized string
  private final void verify(String[] expectations) {
    final NormalizedString nstring = AlignmentNormalizer.getInstance().normalize(expectations[0]);
    final String[] tokens = nstring.split();

    if (expectations.length == 1) {
      final LineBuilder builder = new LineBuilder();
      builder.append(expectations[0]).append(nstring.toString());
      for (String token : tokens) builder.append(token);
      System.out.println(builder.toString());
    }
    else {
      assertEquals("bad norm for '" + expectations[0] + "'!", expectations[1], nstring.toString());
      assertEquals("bad num tokens for '" + expectations[0] + "'", expectations.length - 2, tokens.length);
      for (int i = 0; i < tokens.length; ++i) {
        assertEquals("bad token #" + i + " for '" + expectations[0] + "'", expectations[i + 2], tokens[i]);
      }
    }
  }

  public void test1() {
    verify(new String[]{"'don't' a&b Ph.D. c/o either/or high/low i/o jack input/output jacks",
                        "'dont' a&b ph.d. c/o either/or high/low i/o jack input/output jacks",
                        "dont", "a", "b", "ph", "d", "c", "o", "either", "or", "high", "low", "i", "o", "jack", "input", "output", "jacks"});
  }

  public static Test suite() {
    TestSuite suite = new TestSuite(TestAlignmentNormalizer.class);
    return suite;
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}
