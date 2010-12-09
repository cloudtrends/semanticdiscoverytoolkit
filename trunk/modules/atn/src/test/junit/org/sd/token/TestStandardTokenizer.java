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

    tokenizeTest.runTest();
  }

  public void testDefaultTokenizerOptions_CamelCase1() {
    StandardTokenizerOptions defaultOptions = StandardTokenizerFactory.DEFAULT_OPTIONS;

    final TokenizeTest tokenizeTest =
      new TokenizeTest("Default.JJJS.CamelCase.1", "JohnJacobJingleheimerSchmidt -- His name is my name too!", defaultOptions,
                       new String[] { "JohnJacobJingleheimerSchmidt", "His name is my name too" },
                       new String[][]
                       { new String[] { "John", "JohnJacob", "JohnJacobJingleheimer" },
                         new String[] { "His", "His name", "His name is", "His name is my", "His name is my name" } });

    tokenizeTest.runTest();
  }

  public void testDefaultTokenizerOptions_CamelCase2() {
    StandardTokenizerOptions defaultOptions = StandardTokenizerFactory.DEFAULT_OPTIONS;

    final TokenizeTest tokenizeTest =
      new TokenizeTest("Default.JJJS.CamelCase.2", "JohnJacobJingleheimerSchmidt -- His name is my name, too!", defaultOptions,
                       new String[] { "JohnJacobJingleheimerSchmidt", "His name is my name", "too" },
                       new String[][] { new String[] {"John", "JohnJacob", "JohnJacobJingleheimer" },
                                        new String[] {"His", "His name", "His name is", "His name is my", "His name is my name" },
                                        new String[] {} });

    tokenizeTest.runTest();
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

    tokenizeTest.runTest();
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

    tokenizeTest.runTest();
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

    tokenizeTest.runTest();
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

    tokenizeTest.runTest();
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

    tokenizeTest.runTest();
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

    tokenizeTest.runTest();
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

    tokenizeTest.runTest();
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

    tokenizeTest.runTest();
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

    tokenizeTest.runTest();
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

    tokenizeTest.runTest();
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

    tokenizeTest.runTest();
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


  private final class TokenizeTest {

    private String name;
    private String text;
    private StandardTokenizerOptions options;
    private String[] expectedPrimaryTokens;
    private String[][] expectedSecondaryTokens;

    public TokenizeTest(String name, String text, StandardTokenizerOptions options, String[] expectedPrimaryTokens, String[][] expectedSecondaryTokens) {
      this.name = name;
      this.text = text;
      this.options = options;
      this.expectedPrimaryTokens = expectedPrimaryTokens;
      this.expectedSecondaryTokens = expectedSecondaryTokens;
    }

    public void runTest() {
      final StandardTokenizer tokenizer = StandardTokenizerFactory.getTokenizer(text, options);

      int numPrimaryTokens = 0;
      for (Token primaryToken = tokenizer.getToken(0); primaryToken != null; primaryToken = tokenizer.getNextToken(primaryToken)) {
        assertEquals(name + ": Primary token #" + numPrimaryTokens + " mismatch.",
                     expectedPrimaryTokens[numPrimaryTokens], primaryToken.getText());

        int numRevisedTokens = 0;
        for (Token revisedToken = tokenizer.revise(primaryToken); revisedToken != null; revisedToken = tokenizer.revise(revisedToken)) {
          assertEquals(name + ": Secondary token #" + numPrimaryTokens + "/" + numRevisedTokens + " mismatch.",
                       expectedSecondaryTokens[numPrimaryTokens][numRevisedTokens], revisedToken.getText());
          ++numRevisedTokens;
        }

        ++numPrimaryTokens;
      }
      
      assertEquals(name + ": Primary token count mismatch.",
                   expectedPrimaryTokens.length, numPrimaryTokens);
    }
  }


  public static Test suite() {
    TestSuite suite = new TestSuite(TestStandardTokenizer.class);
    return suite;
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}
