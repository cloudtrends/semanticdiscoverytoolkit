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

import java.io.IOException;

/**
 * JUnit Tests for the NounBag class.
 * <p>
 * @author Spence Koehler
 */
public class TestNounBag extends TestCase {

  public TestNounBag(String name) {
    super(name);
  }
  
  public void testBasics() throws IOException {
    final NounBag nounBag = new NounBag();

    final String[] nouns = "now time good men come aid country recesses recess countries counties country county s i it its country's".split("\\s+");

    for (int i = 0; i < nouns.length; ++i) {
      final String word = nouns[i];
      assertTrue(i + ": " + word, nounBag.isNoun(word));
    }

    final String[] nonnouns = "is the for all to of their".split("\\s+");

    for (int i = 0; i < nonnouns.length; ++i) {
      final String word = nonnouns[i];
      assertFalse(i + ": " + word, nounBag.isNoun(word));
    }
  }

  public static Test suite() {
    TestSuite suite = new TestSuite(TestNounBag.class);
    return suite;
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}
