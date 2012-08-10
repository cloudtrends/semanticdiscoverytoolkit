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
package org.sd.token;


import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * JUnit Tests for the StandardTokenizer class.
 * <p>
 * @author Spence Koehler
 */
public class TestStandardTokenizer extends TestCase {

  public TestStandardTokenizer(String name) {
    super(name);
  }
  

  public void testDefaultTokenizerOptions_Normal() {
    StandardTokenizerOptions defaultOptions = StandardTokenizerFactory.DEFAULT_OPTIONS;

    final TokenizeTest tokenizeTest =
      new TokenizeTest("Default.JJJS.Normal", "John Jacob Jingleheimer Schmidt: His name is my name too!", defaultOptions,
                       new String[] { "John Jacob Jingleheimer Schmidt", "His name is my name too" },
                       new String[][]
                       { new String[] { "John", "John Jacob", "John Jacob Jingleheimer" },
                         new String[] { "His", "His name", "His name is", "His name is my", "His name is my name" } });

    assertTrue(tokenizeTest.runTest());
  }

  public void testDefaultTokenizerOptions_CamelCase1() {
    StandardTokenizerOptions defaultOptions = StandardTokenizerFactory.DEFAULT_OPTIONS;

    final TokenizeTest tokenizeTest =
      new TokenizeTest("Default.JJJS.CamelCase.1", "JohnJacobJingleheimerSchmidt -- His name is my name too!", defaultOptions,
                       new String[] { "JohnJacobJingleheimerSchmidt", "His name is my name too" },
                       new String[][]
                       { new String[] { "John", "JohnJacob", "JohnJacobJingleheimer" },
                         new String[] { "His", "His name", "His name is", "His name is my", "His name is my name" } });

    assertTrue(tokenizeTest.runTest());
  }

  public void testDefaultTokenizerOptions_CamelCase2() {
    StandardTokenizerOptions defaultOptions = StandardTokenizerFactory.DEFAULT_OPTIONS;

    final TokenizeTest tokenizeTest =
      new TokenizeTest("Default.JJJS.CamelCase.2", "JohnJacobJingleheimerSchmidt -- His name is my name, too!", defaultOptions,
                       new String[] { "JohnJacobJingleheimerSchmidt", "His name is my name", "too" },
                       new String[][] { new String[] {"John", "JohnJacob", "JohnJacobJingleheimer" },
                                        new String[] {"His", "His name", "His name is", "His name is my", "His name is my name" },
                                        new String[] {} });

    assertTrue(tokenizeTest.runTest());
  }

  public void test_LS_Strategy_Normal() {
    final StandardTokenizerOptions lsOptions = new StandardTokenizerOptions();
    lsOptions.setRevisionStrategy(TokenRevisionStrategy.LS);

    final TokenizeTest tokenizeTest =
      new TokenizeTest("LS.JJJS.Normal", "John Jacob Jingleheimer Schmidt: His name is my name too!", lsOptions,
                       new String[] { "John Jacob Jingleheimer Schmidt", "His name is my name too" },
                       new String[][]
                       { new String[] { "John Jacob Jingleheimer", "John Jacob", "John" },
                         new String[] { "His name is my name", "His name is my", "His name is", "His name", "His" } });

    assertTrue(tokenizeTest.runTest());
  }

  public void test_LS_Strategy_CamelCase1() {
    final StandardTokenizerOptions lsOptions = new StandardTokenizerOptions();
    lsOptions.setRevisionStrategy(TokenRevisionStrategy.LS);

    final TokenizeTest tokenizeTest =
      new TokenizeTest("LS.JJJS.CamelCase.1", "JohnJacobJingleheimerSchmidt -- His name is my name too!", lsOptions,
                       new String[] { "JohnJacobJingleheimerSchmidt", "His name is my name too" },
                       new String[][]
                       { new String[] { "JohnJacobJingleheimer", "JohnJacob", "John" },
                         new String[] { "His name is my name", "His name is my", "His name is", "His name", "His" } });

    assertTrue(tokenizeTest.runTest());
  }

  public void test_LS_Strategy_CamelCase2() {
    final StandardTokenizerOptions lsOptions = new StandardTokenizerOptions();
    lsOptions.setRevisionStrategy(TokenRevisionStrategy.LS);

    final TokenizeTest tokenizeTest =
      new TokenizeTest("LS.JJJS.CamelCase.2", "JohnJacobJingleheimerSchmidt -- His name is my name, too!", lsOptions,
                       new String[] { "JohnJacobJingleheimerSchmidt", "His name is my name", "too" },
                       new String[][] { new String[] { "JohnJacobJingleheimer", "JohnJacob", "John" },
                                        new String[] { "His name is my", "His name is", "His name", "His" },
                                        new String[] { } });

    assertTrue(tokenizeTest.runTest());
  }

  public void test_LS_Strategy_CamelCase3() {
    final StandardTokenizerOptions lsOptions = new StandardTokenizerOptions();
    lsOptions.setRevisionStrategy(TokenRevisionStrategy.LS);

    final TokenizeTest tokenizeTest =
      new TokenizeTest("LS.JJJS.CamelCase.3", "John Jacob Jingleheimer Schmidt, born 10Oct1857, died 28Feb1899", lsOptions,
                       new String[] { "John Jacob Jingleheimer Schmidt", "born 10Oct1857", "died 28Feb1899" },
                       new String[][] { new String[] { "John Jacob Jingleheimer", "John Jacob", "John" },
                                        new String[] { "born 10Oct", "born" },
                                        new String[] { "died 28Feb", "died" } });

    assertTrue(tokenizeTest.runTest());
  }

  public void test_SL_Strategy_Normal() {
    final StandardTokenizerOptions slOptions = new StandardTokenizerOptions();
    slOptions.setRevisionStrategy(TokenRevisionStrategy.SL);

    final TokenizeTest tokenizeTest =
				new TokenizeTest("SL.JJJS.Normal", "John Jacob Jingleheimer Schmidt: His name is my name too!", slOptions,
												 new String[] { "John", "Jacob", "Jingleheimer", "Schmidt", "His", "name", "is", "my", "name", "too" },
												 new String[][]
												 { new String[] { "John Jacob", "John Jacob Jingleheimer", "John Jacob Jingleheimer Schmidt" },
													 new String[] { "Jacob Jingleheimer", "Jacob Jingleheimer Schmidt" },
													 new String[] { "Jingleheimer Schmidt" },
													 new String[] {},
													 new String[] { "His name", "His name is", "His name is my", "His name is my name", "His name is my name too" },
													 new String[] { "name is", "name is my", "name is my name", "name is my name too" },
													 new String[] { "is my", "is my name", "is my name too" },
													 new String[] { "my name", "my name too" },
													 new String[] { "name too" },
													 new String[] {} } );

    assertTrue(tokenizeTest.runTest());
  }

  public void test_SL_Strategy_CamelCase1() {
    final StandardTokenizerOptions slOptions = new StandardTokenizerOptions();
    slOptions.setRevisionStrategy(TokenRevisionStrategy.SL);

    final TokenizeTest tokenizeTest =
				new TokenizeTest("SL.JJJS.CamelCase.1", "JohnJacobJingleheimerSchmidt -- His name is my name too!", slOptions,
												 new String[] { "John", "Jacob", "Jingleheimer", "Schmidt", "His", "name", "is", "my", "name", "too" },
												 new String[][]
												 { new String[] { "JohnJacob", "JohnJacobJingleheimer", "JohnJacobJingleheimerSchmidt" },
													 new String[] { "JacobJingleheimer", "JacobJingleheimerSchmidt" },
													 new String[] { "JingleheimerSchmidt" },
													 new String[] {},
													 new String[] { "His name", "His name is", "His name is my", "His name is my name", "His name is my name too" },
													 new String[] { "name is", "name is my", "name is my name", "name is my name too" },
													 new String[] { "is my", "is my name", "is my name too" },
													 new String[] { "my name", "my name too" },
													 new String[] { "name too" },
													 new String[] {} } );

    assertTrue(tokenizeTest.runTest());
  }

  public void test_SL_Strategy_CamelCase2() {
    final StandardTokenizerOptions slOptions = new StandardTokenizerOptions();
    slOptions.setRevisionStrategy(TokenRevisionStrategy.SL);

    final TokenizeTest tokenizeTest =
			new TokenizeTest("SL.JJJS.CamelCase.2", "JohnJacobJingleheimerSchmidt -- His name is my name, too!", slOptions,
												 new String[] { "John", "Jacob", "Jingleheimer", "Schmidt", "His", "name", "is", "my", "name", "too" },
												 new String[][]
												 { new String[] { "JohnJacob", "JohnJacobJingleheimer", "JohnJacobJingleheimerSchmidt" },
													 new String[] { "JacobJingleheimer", "JacobJingleheimerSchmidt" },
													 new String[] { "JingleheimerSchmidt" },
													 new String[] {},
													 new String[] { "His name", "His name is", "His name is my", "His name is my name", "His name is my name too" },
													 new String[] { "name is", "name is my", "name is my name", "name is my name too" },
													 new String[] { "is my", "is my name", "is my name too" },
													 new String[] { "my name", "my name too" },
													 new String[] { "name too" },
													 new String[] {},
													 new String[] {} } );

    assertTrue(tokenizeTest.runTest());
  }

  public void test_SO_Strategy_Normal() {
    final StandardTokenizerOptions soOptions = new StandardTokenizerOptions();
    soOptions.setRevisionStrategy(TokenRevisionStrategy.SO);

    final TokenizeTest tokenizeTest =
				new TokenizeTest("SL.JJJS.Normal", "John Jacob Jingleheimer Schmidt: His name is my name too!", soOptions,
												 new String[] { "John", "Jacob", "Jingleheimer", "Schmidt", "His", "name", "is", "my", "name", "too" },
												 new String[][]
												 { new String[] {},
													 new String[] {},
													 new String[] {},
													 new String[] {},
													 new String[] {},
													 new String[] {},
													 new String[] {},
													 new String[] {},
													 new String[] {},
													 new String[] {} });

    assertTrue(tokenizeTest.runTest());
  }

  public void test_SO_Strategy_CamelCase1() {
    final StandardTokenizerOptions soOptions = new StandardTokenizerOptions();
    soOptions.setRevisionStrategy(TokenRevisionStrategy.SO);

    final TokenizeTest tokenizeTest =
				new TokenizeTest("SL.JJJS.CamelCase.1", "JohnJacobJingleheimerSchmidt -- His name is my name too!", soOptions,
												 new String[] { "John", "Jacob", "Jingleheimer", "Schmidt", "His", "name", "is", "my", "name", "too" },
												 new String[][]
												 { new String[] {},
													 new String[] {},
													 new String[] {},
													 new String[] {},
													 new String[] {},
													 new String[] {},
													 new String[] {},
													 new String[] {},
													 new String[] {},
													 new String[] {} });

    assertTrue(tokenizeTest.runTest());
  }

  public void test_LO_Strategy_Normal() {
    final StandardTokenizerOptions loOptions = new StandardTokenizerOptions();
    loOptions.setRevisionStrategy(TokenRevisionStrategy.LO);

    final TokenizeTest tokenizeTest =
				new TokenizeTest("SL.JJJS.Normal", "John Jacob Jingleheimer Schmidt: His name is my name too!", loOptions,
												 new String[] { "John Jacob Jingleheimer Schmidt", "His name is my name too" },
												 new String[][]
												 { new String[] {},
													 new String[] {} });

    assertTrue(tokenizeTest.runTest());
  }

  public void test_LO_Strategy_CamelCase1() {
    final StandardTokenizerOptions loOptions = new StandardTokenizerOptions();
    loOptions.setRevisionStrategy(TokenRevisionStrategy.LO);

    final TokenizeTest tokenizeTest =
				new TokenizeTest("SL.JJJS.CamelCase.1", "JohnJacobJingleheimerSchmidt -- His name is my name too!", loOptions,
												 new String[] { "JohnJacobJingleheimerSchmidt", "His name is my name too" },
												 new String[][]
												 { new String[] {},
													 new String[] {} });

    assertTrue(tokenizeTest.runTest());
  }

  public void testTextWithDelims1() {
    final StandardTokenizerOptions options = new StandardTokenizerOptions();
    options.setRevisionStrategy(TokenRevisionStrategy.SO);

    final StandardTokenizer tokenizer = StandardTokenizerFactory.getTokenizer("Mr. John /Smith/,");
    Token token = tokenizer.getToken(0);
    token = tokenizer.getNextToken(token);
    token = tokenizer.getNextToken(token);

    assertEquals("/Smith/,", token.getTextWithDelims());
  }

  public void testSymbolBreak1() {
    final StandardTokenizerOptions options = new StandardTokenizerOptions();

    options.setRevisionStrategy(TokenRevisionStrategy.SO);
    options.setLowerUpperBreak(Break.NO_BREAK);
    options.setUpperLowerBreak(Break.NO_BREAK);
    options.setUpperDigitBreak(Break.NO_BREAK);
    options.setLowerDigitBreak(Break.NO_BREAK);
    options.setDigitUpperBreak(Break.NO_BREAK);
    options.setDigitLowerBreak(Break.NO_BREAK);
    options.setNonEmbeddedDoubleDashBreak(Break.SINGLE_WIDTH_HARD_BREAK);
    options.setEmbeddedDoubleDashBreak(Break.SINGLE_WIDTH_HARD_BREAK);
    options.setEmbeddedDashBreak(Break.NO_BREAK);
    options.setLeftBorderedDashBreak(Break.SINGLE_WIDTH_SOFT_BREAK);
    options.setRightBorderedDashBreak(Break.SINGLE_WIDTH_SOFT_BREAK);
    options.setFreeStandingDashBreak(Break.SINGLE_WIDTH_HARD_BREAK);
    options.setWhitespaceBreak(Break.SINGLE_WIDTH_SOFT_BREAK);
    options.setQuoteAndParenBreak(Break.NO_BREAK);
    options.setSymbolBreak(Break.SINGLE_WIDTH_HARD_BREAK);
    options.setSlashBreak(Break.SINGLE_WIDTH_HARD_BREAK);

    final StandardTokenizer tokenizer = new StandardTokenizer("/Smith/", options);
    final Token token = tokenizer.getToken(0);
    assertEquals("Smith", token.getText());
    assertEquals("/Smith/", token.getTextWithDelims());
  }

  public void testTokenBreakLimit1() {
    final StandardTokenizerOptions options = new StandardTokenizerOptions();
    options.setTokenBreakLimit(1);

    final TokenizeTest tokenizeTest =
      new TokenizeTest("TokenBreakLimit(1)", "John Jacob Jingleheimer Schmidt: His name is my name too!", options,
                       new String[] { "John", "Jacob", "Jingleheimer", "Schmidt", "His", "name", "is", "my", "name", "too" },
                       new String[][]
                       { new String[] {},
                         new String[] {} });

    assertTrue(tokenizeTest.runTest());
  }

  public void testTokenBreakLimit3() {
    final StandardTokenizerOptions options = new StandardTokenizerOptions();
    options.setTokenBreakLimit(3);

    final TokenizeTest tokenizeTest =
      new TokenizeTest("TokenBreakLimit(3)", "John Jacob Jingleheimer Schmidt: His name is my name too!", options,
                       new String[] { "John Jacob Jingleheimer", "Schmidt", "His name is", "my name too" },
                       new String[][] {
                         new String[] { "John", "John Jacob" },
                         new String[] {},
                         new String[] { "His", "His name" },
                         new String[] { "my", "my name" } });

    assertTrue(tokenizeTest.runTest());
  }


  public static Test suite() {
    TestSuite suite = new TestSuite(TestStandardTokenizer.class);
    return suite;
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}
