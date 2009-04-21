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
 * JUnit Tests for the MultiDelimSplitFunction class.
 * <p>
 * @author Spence Koehler
 */
public class TestMultiDelimSplitFunction extends TestCase {

  public TestMultiDelimSplitFunction(String name) {
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

  public void testDontSplitOnSingle() {
    final MultiDelimSplitFunction splitter = MultiDelimSplitFunction.getDefaultInstance();
    final SegmentSequence sequence = splitter.split("foo1, bar1, baz1");
    verifyFeatures(sequence, CommonFeatures.DELIM, new Boolean[]{false});
    verifyFeatures(sequence, CommonFeatures.MULTI_DELIM, new Boolean[]{false});
    verifySegments(sequence, new String[]{"foo1, bar1, baz1"});
  }

  public void testSplitOnMultiWithIntercedingWhite() {
    final MultiDelimSplitFunction splitter = MultiDelimSplitFunction.getDefaultInstance();
    final SegmentSequence sequence = splitter.split("foo1, bar1, baz1. - foo2, bar2, baz2");
    verifyFeatures(sequence, CommonFeatures.DELIM, new Boolean[]{false, true, false});
    verifyFeatures(sequence, CommonFeatures.MULTI_DELIM, new Boolean[]{false, true, false});
    verifySegments(sequence, new String[]{"foo1, bar1, baz1", ". - ", "foo2, bar2, baz2"});
  }

  public void testSplitWithTrailingDelims() {
    final MultiDelimSplitFunction splitter = MultiDelimSplitFunction.getDefaultInstance();
    final SegmentSequence sequence = splitter.split("foo1, bar1, baz1. - foo2, bar2, baz2. --");
    verifyFeatures(sequence, CommonFeatures.DELIM, new Boolean[]{false, true, false, true});
    verifyFeatures(sequence, CommonFeatures.MULTI_DELIM, new Boolean[]{false, true, false, true});
    verifySegments(sequence, new String[]{"foo1, bar1, baz1", ". - ", "foo2, bar2, baz2", ". --"});
  }

  public static Test suite() {
    TestSuite suite = new TestSuite(TestMultiDelimSplitFunction.class);
    return suite;
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}
