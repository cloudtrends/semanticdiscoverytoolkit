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
 * JUnit Tests for the ExtendedGrammarToken class.
 * <p>
 * @author Spence Koehler
 */
public class TestExtendedGrammarToken extends TestCase {

  private CategoryFactory cfactory;

  public TestExtendedGrammarToken(String name) {
    super(name);

    this.cfactory = new CategoryFactory();
    cfactory.defineCategory("NOUN", true);
  }
  
  private void checkExtendedToken(String expectedString, Category expectedCategory,
                                  boolean expectedNegated, boolean expectedPeek,
                                  boolean expectedLiteral, boolean expectGuessable,
                                  ExtendedGrammarToken token) {
    assertEquals(expectedString, token.getToken());
    assertEquals(expectedCategory, token.getCategory());
    assertEquals(expectedNegated, token.isNegated());
    assertEquals(expectedPeek, token.isPeek());
    assertEquals(expectedLiteral, token.isLiteral());
    assertEquals(expectGuessable || (expectedCategory != null && expectedCategory.canGuess()), token.isGuessable());
  }

  public void testNoExtension() {
    checkExtendedToken("noun", cfactory.getCategory("NOUN"), false, false, false, false, new ExtendedGrammarToken(cfactory, "noun"));
    checkExtendedToken("Noun", cfactory.getCategory("NOUN"), false, false, false, false, new ExtendedGrammarToken(cfactory, "Noun"));
    checkExtendedToken("NOUN", cfactory.getCategory("NOUN"), false, false, false, false, new ExtendedGrammarToken(cfactory, "NOUN"));

    try {
      final ExtendedGrammarToken token = new ExtendedGrammarToken(cfactory, "non-existant category");
      fail("can't create a token unless it is a valid category or is 'literal'");
    }
    catch (IllegalArgumentException e) {
      // success
    }
  }

  public void testNegated() {
    checkExtendedToken("noun", cfactory.getCategory("NOUN"), true, false, false, false, new ExtendedGrammarToken(cfactory, "!noun"));
    checkExtendedToken("Noun", cfactory.getCategory("NOUN"), true, false, false, false, new ExtendedGrammarToken(cfactory, "!Noun"));
    checkExtendedToken("NOUN", cfactory.getCategory("NOUN"), true, false, false, false, new ExtendedGrammarToken(cfactory, "!NOUN"));
  }

  public void testPeek() {
    checkExtendedToken("noun", cfactory.getCategory("NOUN"), false, true, false, false, new ExtendedGrammarToken(cfactory, "&noun"));
    checkExtendedToken("Noun", cfactory.getCategory("NOUN"), false, true, false, false, new ExtendedGrammarToken(cfactory, "&Noun"));
    checkExtendedToken("NOUN", cfactory.getCategory("NOUN"), false, true, false, false, new ExtendedGrammarToken(cfactory, "&NOUN"));
  }

  public void testLiteral() {
    checkExtendedToken("noun", null, false, false, true, false, new ExtendedGrammarToken(cfactory, "_noun"));
    checkExtendedToken("Noun", null, false, false, true, false, new ExtendedGrammarToken(cfactory, "_Noun"));
    checkExtendedToken("NOUN", null, false, false, true, false, new ExtendedGrammarToken(cfactory, "_NOUN"));

    checkExtendedToken("anything", null, false, false, true, false, new ExtendedGrammarToken(cfactory, "_anything"));
    checkExtendedToken("Anything", null, false, false, true, false, new ExtendedGrammarToken(cfactory, "_Anything"));
    checkExtendedToken("ANYTHING", null, false, false, true, false, new ExtendedGrammarToken(cfactory, "_ANYTHING"));
  }

  public void testGuessable() {
    checkExtendedToken("noun", cfactory.getCategory("NOUN"), false, false, false, true, new ExtendedGrammarToken(cfactory, "?noun"));
    checkExtendedToken("Noun", cfactory.getCategory("NOUN"), false, false, false, true, new ExtendedGrammarToken(cfactory, "?Noun"));
    checkExtendedToken("NOUN", cfactory.getCategory("NOUN"), false, false, false, true, new ExtendedGrammarToken(cfactory, "?NOUN"));
  }

  public void testAllExtensionsAtOnce() {
    checkExtendedToken("noun", null, true, true, true, true, new ExtendedGrammarToken(cfactory, "!&_?noun"));
    checkExtendedToken("noun", null, true, true, true, true, new ExtendedGrammarToken(cfactory, "!_&?noun"));
    checkExtendedToken("noun", null, true, true, true, true, new ExtendedGrammarToken(cfactory, "&!_?noun"));
    checkExtendedToken("noun", null, true, true, true, true, new ExtendedGrammarToken(cfactory, "&_!?noun"));
    checkExtendedToken("noun", null, true, true, true, true, new ExtendedGrammarToken(cfactory, "_!&?noun"));
    checkExtendedToken("noun", null, true, true, true, true, new ExtendedGrammarToken(cfactory, "_&!?noun"));

    checkExtendedToken("test", null, true, true, true, true, new ExtendedGrammarToken(cfactory, "!&_?test"));
    checkExtendedToken("test", null, true, true, true, true, new ExtendedGrammarToken(cfactory, "!_&?test"));
    checkExtendedToken("test", null, true, true, true, true, new ExtendedGrammarToken(cfactory, "&!_?test"));
    checkExtendedToken("test", null, true, true, true, true, new ExtendedGrammarToken(cfactory, "&_!?test"));
    checkExtendedToken("test", null, true, true, true, true, new ExtendedGrammarToken(cfactory, "_!&?test"));
    checkExtendedToken("test", null, true, true, true, true, new ExtendedGrammarToken(cfactory, "_&!?test"));

    checkExtendedToken("noun", null, true, true, true, true, new ExtendedGrammarToken(cfactory, "!&?_noun"));
    checkExtendedToken("noun", null, true, true, true, true, new ExtendedGrammarToken(cfactory, "!_?&noun"));
    checkExtendedToken("noun", null, true, true, true, true, new ExtendedGrammarToken(cfactory, "&!?_noun"));
    checkExtendedToken("noun", null, true, true, true, true, new ExtendedGrammarToken(cfactory, "&_?!noun"));
    checkExtendedToken("noun", null, true, true, true, true, new ExtendedGrammarToken(cfactory, "_!?&noun"));
    checkExtendedToken("noun", null, true, true, true, true, new ExtendedGrammarToken(cfactory, "_&?!noun"));

    checkExtendedToken("test", null, true, true, true, true, new ExtendedGrammarToken(cfactory, "!&?_test"));
    checkExtendedToken("test", null, true, true, true, true, new ExtendedGrammarToken(cfactory, "!_?&test"));
    checkExtendedToken("test", null, true, true, true, true, new ExtendedGrammarToken(cfactory, "&!?_test"));
    checkExtendedToken("test", null, true, true, true, true, new ExtendedGrammarToken(cfactory, "&_?!test"));
    checkExtendedToken("test", null, true, true, true, true, new ExtendedGrammarToken(cfactory, "_!?&test"));
    checkExtendedToken("test", null, true, true, true, true, new ExtendedGrammarToken(cfactory, "_&?!test"));

    checkExtendedToken("noun", null, true, true, true, true, new ExtendedGrammarToken(cfactory, "!?&_noun"));
    checkExtendedToken("noun", null, true, true, true, true, new ExtendedGrammarToken(cfactory, "!?_&noun"));
    checkExtendedToken("noun", null, true, true, true, true, new ExtendedGrammarToken(cfactory, "&?!_noun"));
    checkExtendedToken("noun", null, true, true, true, true, new ExtendedGrammarToken(cfactory, "&?_!noun"));
    checkExtendedToken("noun", null, true, true, true, true, new ExtendedGrammarToken(cfactory, "_?!&noun"));
    checkExtendedToken("noun", null, true, true, true, true, new ExtendedGrammarToken(cfactory, "_?&!noun"));

    checkExtendedToken("test", null, true, true, true, true, new ExtendedGrammarToken(cfactory, "!?&_test"));
    checkExtendedToken("test", null, true, true, true, true, new ExtendedGrammarToken(cfactory, "!?_&test"));
    checkExtendedToken("test", null, true, true, true, true, new ExtendedGrammarToken(cfactory, "&?!_test"));
    checkExtendedToken("test", null, true, true, true, true, new ExtendedGrammarToken(cfactory, "&?_!test"));
    checkExtendedToken("test", null, true, true, true, true, new ExtendedGrammarToken(cfactory, "_?!&test"));
    checkExtendedToken("test", null, true, true, true, true, new ExtendedGrammarToken(cfactory, "_?&!test"));

    checkExtendedToken("noun", null, true, true, true, true, new ExtendedGrammarToken(cfactory, "?!&_noun"));
    checkExtendedToken("noun", null, true, true, true, true, new ExtendedGrammarToken(cfactory, "?!_&noun"));
    checkExtendedToken("noun", null, true, true, true, true, new ExtendedGrammarToken(cfactory, "?&!_noun"));
    checkExtendedToken("noun", null, true, true, true, true, new ExtendedGrammarToken(cfactory, "?&_!noun"));
    checkExtendedToken("noun", null, true, true, true, true, new ExtendedGrammarToken(cfactory, "?_!&noun"));
    checkExtendedToken("noun", null, true, true, true, true, new ExtendedGrammarToken(cfactory, "?_&!noun"));

    checkExtendedToken("test", null, true, true, true, true, new ExtendedGrammarToken(cfactory, "?!&_test"));
    checkExtendedToken("test", null, true, true, true, true, new ExtendedGrammarToken(cfactory, "?!_&test"));
    checkExtendedToken("test", null, true, true, true, true, new ExtendedGrammarToken(cfactory, "?&!_test"));
    checkExtendedToken("test", null, true, true, true, true, new ExtendedGrammarToken(cfactory, "?&_!test"));
    checkExtendedToken("test", null, true, true, true, true, new ExtendedGrammarToken(cfactory, "?_!&test"));
    checkExtendedToken("test", null, true, true, true, true, new ExtendedGrammarToken(cfactory, "?_&!test"));
  }

  public static Test suite() {
    TestSuite suite = new TestSuite(TestExtendedGrammarToken.class);
    return suite;
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}
