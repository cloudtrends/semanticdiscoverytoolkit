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
 * JUnit Tests for the SentenceSplitter class.
 * <p>
 * @author Spence Koehler
 */
public class TestSentenceSplitter extends TestCase {

  public TestSentenceSplitter(String name) {
    super(name);
  }
  

  private final void doSplitTest(String text, String[] expected) {
    final SentenceSplitter splitter = new SentenceSplitter();
    final String[] sentences = splitter.split(text);

    if (expected == null) {
      System.out.println(text + " --(" + sentences.length + ")-->");
      int index = 0;
      for (String sentence : sentences) {
        System.out.println("\t" + index + ": " + sentence);
        ++index;
      }
    }
    else {
      for (int i = 0; i < sentences.length; ++i) {
        assertEquals("i=" + i, expected[i], sentences[i]);
      }
      assertEquals(expected.length, sentences.length);
    }
  }

  public void testExclamationInName() {
    doSplitTest("Names like Yahoo! shouldn't split! But other sentences should.",
                new String[] {
                  "Names like Yahoo! shouldn't split!",
                  "But other sentences should.",
                });
  }

  public void testAcronyms() {
    doSplitTest("Shouldn't split on M.D. or Ph.D. or Mr. or Mrs. but can split otherwise. Mostly.",
                new String[] {
                  "Shouldn't split on M.D. or Ph.D. or Mr. or Mrs. but can split otherwise.",
                  "Mostly.",
                });
  }

  public void testQuoted1() {
    doSplitTest("\"Split appropriately with quotes.\" (At least, \"try.\") Hopefully.",
                new String[] {
                  "\"Split appropriately with quotes.\"",
                  "(At least, \"try.\")",
                  "Hopefully.",
                });

  }

  public void testProperNameEndsSentence() {
    doSplitTest("If the Wii Remote has a Wii Remote Jacket on it, remove it before using the remote with the Wii  Wheel. Replace the jacket on the remote when you remove it from the Wii Wheel.",
                new String[] {
                  "If the Wii Remote has a Wii Remote Jacket on it, remove it before using the remote with the Wii  Wheel.",
                  "Replace the jacket on the remote when you remove it from the Wii Wheel.",
                });
  }

  public void testVariousAbbreviations() {

    //note: there is still a spot in this test where the breaking isn't right, but we're living with it for now!

    doSplitTest("We shouldn't split i.e. after a latin abbreviation. Neither should we after an abbreviation for Dr. Foo. Nor for Ms. Foo, D.D.S. While we should split later. Sometimes. We could also have Foo Bar, Jr., or some such, and Bar Baz, Jr. Possibly. And what about the Hon. Jdg. Judy? Can we handle her properly?",
                new String[] {
                  "We shouldn't split i.e. after a latin abbreviation.",
                  "Neither should we after an abbreviation for Dr. Foo. Nor for Ms. Foo, D.D.S.",
                  "While we should split later.",
                  "Sometimes.",
                  "We could also have Foo Bar, Jr., or some such, and Bar Baz, Jr. Possibly.",
                  "And what about the Hon. Jdg. Judy?",
                  "Can we handle her properly?",
                });
  }

  public void testBeginningAbbreviations() {
    doSplitTest("Dr. James Johnson, a M.D. from St. Judy's Hospital, passed away Aug. 17th, 2010.  Dr. Johnson was well-known in the medical profession as a pioneer in E.M.G. research.  A.M.A. directors have lobbied to award him a Nobel Prize in the field of medicine posthumously.  Dr. Johnson is survived by his wife and two children.",
                new String[] {
	                "Dr. James Johnson, a M.D. from St. Judy's Hospital, passed away Aug. 17th, 2010.",
	                "Dr. Johnson was well-known in the medical profession as a pioneer in E.M.G. research.",
	                "A.M.A. directors have lobbied to award him a Nobel Prize in the field of medicine posthumously.",
	                "Dr. Johnson is survived by his wife and two children.",
                });
  }

  public void testNonBreakingSpace() {
    doSplitTest("This is a test with a sentence ending in a non-breaking space.\u00A0It should break this into separate sentences.\u00A0 Even cases where a breaking space follows a non-breaking space.",
                new String[] {
                  "This is a test with a sentence ending in a non-breaking space.",
                  "It should break this into separate sentences.",
                  "Even cases where a breaking space follows a non-breaking space.",
                });
  }


  public static Test suite() {
    TestSuite suite = new TestSuite(TestSentenceSplitter.class);
    return suite;
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}
