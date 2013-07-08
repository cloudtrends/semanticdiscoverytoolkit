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
 * JUnit Tests for the NormalizingTokenizer class.
 * <p>
 * @author Spence Koehler
 */
public class TestNormalizingTokenizer extends TestCase {

  public TestNormalizingTokenizer(String name) {
    super(name);
  }
  
  private void verify(AbstractNormalizer normalizer, BreakStrategy breakStrategy, String inputString,
                      String[] expectedOriginals, String[] expectedNormalized) {
    final NormalizingTokenizer tokenizer = new NormalizingTokenizer(normalizer, breakStrategy, inputString);

    final int num = tokenizer.getNumTokens();

    if (expectedOriginals.length == 0 && expectedNormalized.length == 0) {
      System.out.print("\n" + inputString + "\n\toriginal: ");
      for (int i = 0; i < num; ++i) {
        System.out.print(tokenizer.getOriginalToken(i) + " ");
      }
      System.out.print("\n\t  normal: ");
      for (int i = 0; i < num; ++i) {
        System.out.print(tokenizer.getNormalizedToken(i) + " ");
      }
    }
    else {
      assertEquals(expectedOriginals.length, num);

      for (int i = 0; i < num; ++i) {
        final String originalToken = tokenizer.getOriginalToken(i);
        final String normalizedToken = tokenizer.getNormalizedToken(i);

        assertEquals(expectedOriginals[i], originalToken);
        assertEquals(expectedNormalized[i], normalizedToken);
      }
    }
  }

  public void testBehavior() {
    final AbstractNormalizer normalizer = new GeneralNormalizer(true);
    final BreakStrategy breakStrategy = GeneralBreakStrategy.getInstance();

    verify(normalizer, breakStrategy, "Very simple test.",
           new String[]{"Very", "simple", "test"},
           new String[]{"very", "simple", "test"});

    verify(normalizer, breakStrategy, "Testing123",
           new String[]{"Testing", "123"},
           new String[]{"testing", "123"});

    verify(normalizer, breakStrategy, "CamelCaseTest",
           new String[]{"Camel", "Case", "Test"},
           new String[]{"camel", "case", "test"});

// 4s-(-)-2,4-(4-Tert-Butyl-4,5-Dihydro-Oxazol-2-Yl)Propan-2-Ol. 98%
//   original: 4s 2 4 4 Tert Butyl 4 5 Dihydro Oxazol 2 Yl Propan 2 Ol 98 
//     normal: 4s 2 4 4 tert butyl 4 5 dihydro oxazol 2 yl propan 2 ol 98 

    // 4s (-) 2,4 4 Tert Butyl 4,5 Dihydro Oxazol 2 Yl Propan 2 01 98%
    verify(normalizer, breakStrategy, "4s-(-)-2,4-(4-Tert-Butyl-4,5-Dihydro-Oxazol-2-Yl)Propan-2-Ol. 98%",
           new String[]{},
           new String[]{});
  }

  public static Test suite() {
    TestSuite suite = new TestSuite(TestNormalizingTokenizer.class);
    return suite;
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}
