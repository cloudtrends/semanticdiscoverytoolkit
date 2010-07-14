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
 * JUnit Tests for the StandardNormalizer class.
 * <p>
 * @author Spence Koehler
 */
public class TestStandardNormalizer extends TestCase {

  public TestStandardNormalizer(String name) {
    super(name);
  }
  

  public void testNormalizer_Default() {
    // default normalizer options
    final StandardNormalizerOptions defaultNormalizerOptions = new StandardNormalizerOptions();

    final NormalizeTest normalizeTest =
      new NormalizeTest("Normalize.Default1",
                        "  This tests  nOrMaLiZiNg  non-letters-and-digits for less  than $0.99  w/out a Ph. D..",
                        defaultNormalizerOptions,
                        "this tests normalizing non letters and digits for less than 0 99 w out a ph d");

    normalizeTest.runTest();
  }

  public void testNormalizer_KeepCase1() {
    // keep case normalizer options
    final StandardNormalizerOptions keepCaseNormalizerOptions = new StandardNormalizerOptions();
    keepCaseNormalizerOptions.setCommonCase(false);

    final NormalizeTest normalizeTest =
      new NormalizeTest("Normalize.KeepCase1",
                        "  This tests  nOrMaLiZiNg  non-letters-and-digits for less  than $0.99  w/out a Ph. D..",
                        keepCaseNormalizerOptions,
                        "This tests nOrMaLiZiNg non letters and digits for less than 0 99 w out a Ph D");

    normalizeTest.runTest();
  }

  public void testNormalizer_KeepCaseAndSymbols1() {
    // keep case and symbols normalizer options
    final StandardNormalizerOptions keepCaseAndSymbolsNormalizerOptions = new StandardNormalizerOptions();
    keepCaseAndSymbolsNormalizerOptions.setCommonCase(false);
    keepCaseAndSymbolsNormalizerOptions.setReplaceSymbolsWithWhite(false);

    final NormalizeTest normalizeTest =
      new NormalizeTest("Normalize.KeepCaseAndSymbols1",
                        "  This tests  nOrMaLiZiNg  non-letters-and-digits for less  than $0.99  w/out a Ph. D..",
                        keepCaseAndSymbolsNormalizerOptions,
                        "This tests nOrMaLiZiNg non-letters-and-digits for less than $0.99 w/out a Ph. D..");

    normalizeTest.runTest();
  }


  private class NormalizeTest {

    private String name;
    private String inputText;
    private StandardNormalizerOptions options;
    private String expectedText;

    public NormalizeTest(String name, String inputText, StandardNormalizerOptions options, String expectedText) {
      this.name = name;
      this.inputText = inputText;
      this.options = options;
      this.expectedText = expectedText;
    }

    public void runTest() {
      final StandardNormalizer normalizer = new StandardNormalizer(options);
      final String normalized = normalizer.normalize(inputText);

      assertEquals(name + ": Normalization failure.",
                   expectedText, normalized);
    }
  }


  public static Test suite() {
    TestSuite suite = new TestSuite(TestStandardNormalizer.class);
    return suite;
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}
