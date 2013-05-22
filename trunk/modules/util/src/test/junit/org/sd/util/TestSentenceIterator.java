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


import java.util.Locale;

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
  

  public void doTest(String input, String[] expected, int[][] expectedIndexes) {
    final SentenceIterator iter = new SentenceIterator(input);
    doTest(iter, expected, expectedIndexes);
  }

  public void doTest(SentenceIterator iter, String[] expected, int[][] expectedIndexes) {
    int count = 0;
    while (iter.hasNext()) {
      final String text = iter.next();
      if (expected == null) {
        System.out.println(count + "(" + iter.getStartIndex() + "," + iter.getEndIndex() + "): " + text);
      }
      else {
        assertEquals("(" + count + ")", expected[count], text);
        assertEquals(expectedIndexes[count][0], iter.getStartIndex());
        assertEquals(expectedIndexes[count][1], iter.getEndIndex());
      }
      ++count;
    }

    if (expected != null) {
      assertEquals(expected.length, count);
    }
  }

  public void testSimple() {
    doTest("This is a test. This is only a test.",
           new String[] {
             "This is a test.",
             "This is only a test.",
           },
           new int[][] {
             {0, 16},
             {16, 36},
           });
  }

  public void testBadAbbreviation1() {
    final String input = "Try parsing beyond tokens like Ph.D. and Dr. Smith, if you please.";

    // NOTE: This doesn't work quite right. If we find that it is fixed in the
    //       future, we can set this test to rights!
    doTest(input,
           new String[] {
             "Try parsing beyond tokens like Ph.D. and Dr.",
             "Smith, if you please.",
           },
           new int[][] {
             {0, 45},
             {45, 66},
           });

    // Here we've fixed it with the optional 'detectAbbrev' flag.
    final SentenceIterator iter = new SentenceIterator(input, true);
    doTest(iter,
           new String[] {
             "Try parsing beyond tokens like Ph.D. and Dr. Smith, if you please.",
           },
           new int[][] {
             {0, 66},
           });
  }

  public void testBeginningAbbreviation() {
    final String input = "Dr. Smith was well known in the medical industry.";

    doTest(input,
           new String[] {
	           "Dr.",
	           "Smith was well known in the medical industry.",
           },
           new int[][] {
             {0, 4},
             {4, 49},
           });


    // Here we've fixed it with the optional 'detectAbbrev' flag.
    final SentenceIterator iter = new SentenceIterator(input, true);
    doTest(iter,
           new String[] {
	           "Dr. Smith was well known in the medical industry.",
           },
           new int[][] {
             {0, 49},
           });
  }

  public void testComplex1() {
    doTest("\"What is Machine Translation? Machine translation (MT) is the application of computers to the task of translating texts from one natural language to another. One of the very earliest pursuits in computer science, MT has proved to be an elusive goal, but today a number of systems are available which produce output which, if not perfect, is of sufficient quality to be useful in a number of specific domains.\" A definition from the European Association for Machine Translation (EAMT), \"an organization that serves the growing community of people interested in MT and translation tools, including users, developers, and researchers of this increasingly viable technology.\"",
           new String[] {
             "\"What is Machine Translation?",
             "Machine translation (MT) is the application of computers to the task of translating texts from one natural language to another.",
             "One of the very earliest pursuits in computer science, MT has proved to be an elusive goal, but today a number of systems are available which produce output which, if not perfect, is of sufficient quality to be useful in a number of specific domains.\"",
             "A definition from the European Association for Machine Translation (EAMT), \"an organization that serves the growing community of people interested in MT and translation tools, including users, developers, and researchers of this increasingly viable technology.\"",
           },
           new int[][] {
             {0, 30},
             {30, 158},
             {158, 410},
             {410, 671},
           });
  }

  public void testComplex2() {
    doTest("&quot;What is Machine Translation? Machine translation (MT) is the application of computers to the task of translating texts from one natural language to another. One of the very earliest pursuits in computer science, MT has proved to be an elusive goal, but today a number of systems are available which produce output which, if not perfect, is of sufficient quality to be useful in a number of specific domains.&quot; A definition from the European Association for Machine Translation (EAMT), &quot;an organization that serves the growing community of people interested in MT and translation tools, including users, developers, and researchers of this increasingly viable technology.&quot;",
           new String[] {
             "&quot;What is Machine Translation?",
             "Machine translation (MT) is the application of computers to the task of translating texts from one natural language to another.",
             "One of the very earliest pursuits in computer science, MT has proved to be an elusive goal, but today a number of systems are available which produce output which, if not perfect, is of sufficient quality to be useful in a number of specific domains.",
             "&quot; A definition from the European Association for Machine Translation (EAMT), &quot;an organization that serves the growing community of people interested in MT and translation tools, including users, developers, and researchers of this increasingly viable technology.",
             "&quot;",
           },
           new int[][] {
             {0, 35},
             {35, 163},
             {163, 413},
             {413, 685},
             {685, 691},
           });
  }

  /** Test behavior with empty input. */
  public void testEmptyInput() {
    doTest("", new String[0], null);
  }

  public void testEnglishChineseMix() {
    doTest(new SentenceIterator(
             "NAND flash 不是有寫入次數的限制嗎？ 這對 ioDrive™ 的使用壽命會有什麼影響？",
             Locale.CHINESE),
           new String[] {
             "NAND flash 不是有寫入次數的限制嗎？",
             "這對 ioDrive™ 的使用壽命會有什麼影響？"
           },
           new int[][] {
             {0, 24},
             {24, 48},
           });
  }

  public void testNoBreaksAtAll() {
    doTest(new SentenceIterator("Schleehauf George W (Kathleen H)", true),
           new String[] {
             "Schleehauf George W (Kathleen H)",
           },
           new int[][] {
             {0, 32},
           });
  }

  public void testNoBreaksAtEnd() {
    doTest(new SentenceIterator("This is the first sentence. Schleehauf George W (Kathleen H)", true),
           new String[] {
             "This is the first sentence.",
             "Schleehauf George W (Kathleen H)",
           },
           new int[][] {
             {0, 28},
             {28, 60},
           });
  }

  public void testLeadingEllipsis() {
    doTest(new SentenceIterator(". . . LLOYD E. CRANDALL, Manager", true),
           new String[] {
             ". . .",
             "LLOYD E. CRANDALL, Manager",
           },
           new int[][] {
             {0, 6},
             {6, 32},
           });
  }

  public void testGreedyInclusion() {
    doTest(new SentenceIterator("Cowdrey Mary E. Mrs. h. 18 Talbot ave. N. B.", true, true),
           new String[] {
             "Cowdrey Mary E. Mrs. h. 18 Talbot ave. N. B.",
           },
           new int[][] {
             {0, 44},
           });

    doTest(new SentenceIterator("Cowdrey Mary E. Mrs. h. 18 Talbot ave. N. B.", true, false),
           new String[] {
             "Cowdrey Mary E. Mrs. h. 18 Talbot ave.",
             "N. B.",
           },
           new int[][] {
             {0, 39},
             {39, 44},
           });
  }

  public void testNonGreedy1() {
    doTest(new SentenceIterator("Michal Hixon. Standing:", true, true),
           new String[] {
             "Michal Hixon. Standing:"
           },
           new int[][] {
             {0, 23},
           });

    doTest(new SentenceIterator("Michal Hixon. Standing:", true, false),
           new String[] {
             "Michal Hixon.",
             "Standing:",
           },
           new int[][] {
             {0, 14},
             {14, 23},
           });
  }

  public void testEmbeddedDot() {
    doTest(new SentenceIterator("2:17 p.m. Tuesday (Dec. 27, 2005)", true, true),
           new String[] {
             "2:17 p.m. Tuesday (Dec. 27, 2005)"
           },
           new int[][] {
             {0, 33},
           });
    doTest(new SentenceIterator("2:17 p.m. Tuesday (Dec. 27, 2005)", true, false),
           new String[] {
             "2:17 p.m. Tuesday (Dec. 27, 2005)"
           },
           new int[][] {
             {0, 33},
           });
    doTest(new SentenceIterator("2:17 p.m. Tuesday (Dec. 27, 2005)", false, true),
           new String[] {
             "2:17 p.m.",
             "Tuesday (Dec. 27, 2005)",
           },
           new int[][] {
             {0, 10},
             {10, 33},
           });
    doTest(new SentenceIterator("2:17 p.m. Tuesday (Dec. 27, 2005)", false, false),
           new String[] {
             "2:17 p.m.",
             "Tuesday (Dec. 27, 2005)",
           },
           new int[][] {
             {0, 10},
             {10, 33},
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
