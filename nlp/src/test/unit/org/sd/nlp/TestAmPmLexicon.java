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
package org.sd.nlp;


import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * JUnit Tests for the AmPmLexicon class.
 * <p>
 * @author Spence Koehler
 */
public class TestAmPmLexicon extends TestCase {

  public TestAmPmLexicon(String name) {
    super(name);
  }
  
  private final AmPmLexicon buildLexicon() {
    final CategoryFactory categoryFactory = new CategoryFactory();
    categoryFactory.defineCategory("AMPM", false);
    return new AmPmLexicon(categoryFactory.getCategory("AMPM"));
  }

  private final void verify(AmPmLexicon lexicon, String input, boolean hasAmPm) {
    final StringWrapper stringWrapper = new StringWrapper(input, DateTimeBreakStrategy.getInstance());
    StringWrapper.SubString subString = stringWrapper.getShortestSubString(0);

    lexicon.lookup(subString);

    final Categories categories = subString.getCategories();

    assertNotNull(categories);
    assertEquals(hasAmPm, categories.hasType(lexicon.getCategory()));
  }

  public void testAsianAmPm() {
    final AmPmLexicon lexicon = buildLexicon();
    verify(lexicon, "下午7時45分", true);
  }

  public static Test suite() {
    TestSuite suite = new TestSuite(TestAmPmLexicon.class);
    return suite;
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}
