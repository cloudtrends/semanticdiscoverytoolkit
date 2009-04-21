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
package org.sd.text.segment;


import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * JUnit Tests for the SingleDelimSplitFunction class.
 * <p>
 * @author Spence Koehler
 */
public class TestSingleDelimSplitFunction extends TestCase {

  public TestSingleDelimSplitFunction(String name) {
    super(name);
  }
  
  private final void verifyFeatures(SegmentSequence sequence, String feature, Boolean[] expected) {
    assertEquals(expected.length, sequence.size());

    for (int i = 0; i < expected.length; ++i) {
      final Segment segment = sequence.getSegment(i);
      final Boolean value = segment.getFeature(feature);

      assertEquals(Integer.toString(i), expected[i], value);
    }
  }

  private final void verifySegments(SegmentSequence sequence, String[] expected) {
    assertEquals(expected.length, sequence.size());

    for (int i = 0; i < expected.length; ++i) {
      final Segment segment = sequence.getSegment(i);
      final String text = segment.getText();

      assertEquals(Integer.toString(i), expected[i], text);
    }
  }

  public void testDontSplit() {
    final SingleDelimSplitFunction splitter = SingleDelimSplitFunction.getDefaultInstance();
    final SegmentSequence sequence = splitter.split("foo");
    verifyFeatures(sequence, CommonFeatures.DELIM, new Boolean[]{false});
    verifyFeatures(sequence, CommonFeatures.MULTI_DELIM, new Boolean[]{null});
    verifySegments(sequence, new String[]{"foo"});
  }

  public void testDoSplit() {
    final SingleDelimSplitFunction splitter = SingleDelimSplitFunction.getDefaultInstance();
    final SegmentSequence sequence = splitter.split("foo, bar, baz");
    verifyFeatures(sequence, CommonFeatures.DELIM, new Boolean[]{false, true, false, true, false});
    verifyFeatures(sequence, CommonFeatures.MULTI_DELIM, new Boolean[]{null, null, null, null, null});
    verifySegments(sequence, new String[]{"foo", ", ", "bar", ", ", "baz"});
  }

  public void testSplitWithTrailingDelims() {
    final SingleDelimSplitFunction splitter = SingleDelimSplitFunction.getDefaultInstance();
    final SegmentSequence sequence = splitter.split("foo, ");
    verifyFeatures(sequence, CommonFeatures.DELIM, new Boolean[]{false, true});
    verifyFeatures(sequence, CommonFeatures.MULTI_DELIM, new Boolean[]{null, null});
    verifySegments(sequence, new String[]{"foo", ", "});
  }

  public static Test suite() {
    TestSuite suite = new TestSuite(TestSingleDelimSplitFunction.class);
    return suite;
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}
