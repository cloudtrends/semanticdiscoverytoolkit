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
package org.sd.text.sentiment;


import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * JUnit Tests for the SentimentType class.
 * <p>
 * @author Spence Koehler
 */
public class TestSentimentType extends TestCase {

  public TestSentimentType(String name) {
    super(name);
  }
  

  public void testFlipSentiment() {
    assertEquals(SentimentType.NEGATIVE, SentimentType.flipSentiment(SentimentType.POSITIVE));
    assertEquals(SentimentType.POSITIVE, SentimentType.flipSentiment(SentimentType.NEGATIVE));
    assertEquals(SentimentType.NEGATIVE, SentimentType.flipSentiment(SentimentType.NEUTRAL));
    assertEquals(SentimentType.UNKNOWN, SentimentType.flipSentiment(SentimentType.UNKNOWN));
    assertEquals(SentimentType.MIXED, SentimentType.flipSentiment(SentimentType.MIXED));
    assertEquals(SentimentType.UNKNOWN, SentimentType.flipSentiment(null));
  }

  public void testFlip() {
    assertEquals(SentimentType.NEGATIVE, SentimentType.POSITIVE.flip());
    assertEquals(SentimentType.POSITIVE, SentimentType.NEGATIVE.flip());
    assertEquals(SentimentType.NEGATIVE, SentimentType.NEUTRAL.flip());
    assertEquals(SentimentType.MIXED, SentimentType.MIXED.flip());
  }


  public static Test suite() {
    TestSuite suite = new TestSuite(TestSentimentType.class);
    return suite;
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}
