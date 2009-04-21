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


import java.util.HashSet;
import java.util.Set;

/**
 * A split function that splits text based on delimiters. This function sets
 * the CommonFeatures.DELIM feature to true on all single-or-more (not including
 * whitespace) delimiter segments and to false on all non-delimiter segments
 * created. It will additionally set the CommonFeatures.MULTI_DELIM feature to
 * true on all delimiter segments with 2 or more non-white delimiters.
 * <p>
 * @author Spence Koehler
 */
public abstract class BaseDelimSplitFunction implements SplitFunction {
  
  
  private int numDelims;
  private Set<Character> charsToExcludeSet;

  /**
   * Construct an instance that will treat any character that is not a letter,
   * digit, or one of the given chars as delimiters for splitting. Whitespace
   * is pulled in with delimiter sets, but is not considered a delimiter for
   * splitting by itself. Also, a dash ('-') will not be defined as a delimiter
   * if it has a letter immediately preceding it.
   *
   * @param charsToExclude  chars that are NOT to be considered delimiters
   *                        above and beyond 
   * @param numDelims the minimum number of non-white delims that must be
   *                  seen before splitting.
   */
  protected BaseDelimSplitFunction(int numDelims, char[] charsToExclude) {
    this.numDelims = numDelims;
    this.charsToExcludeSet = new HashSet<Character>();
    for (char c : charsToExclude) {
      charsToExcludeSet.add(c);
    }
  }

  
  /**
   * Don't split segments that already have a delim or multi-delim feature.
   */
  public boolean shouldSplit(Segment segment) {
    boolean result = false;

    if (segment != null) {
      result = !SegmentUtils.isDelimOrWhitespace(segment);
    }
        
    return result;
  }

  /**
   * Split the text into a segment sequence.
   *
   * @return the segment sequence or null if unable or meaningless to split.
   */
  public SegmentSequence split(String string) {
    final SegmentSequence result = new SegmentSequence();
    final StringBuilder resultBuffer = new StringBuilder();
    final StringBuilder delimBuffer = new StringBuilder();
    final int len = string.length();

    int seenDelims = 0;
    boolean readyToSplit = false;
    boolean sawSpace = false;

    for (int i = 0; i < len; ++i) {
      final char c = string.charAt(i);
      if (Character.isLetterOrDigit(c) || charsToExcludeSet.contains(c) ||  // character for resultBuffer
          (c == '-' && !sawSpace && seenDelims == 0)) {

        if (readyToSplit) {
          addSegment(result, resultBuffer.toString()/*.trim()*/, false);
          resultBuffer.setLength(0);

          addSegment(result, delimBuffer.toString(), true);
        }
        else if (delimBuffer.length() > 0) {
          resultBuffer.append(delimBuffer);  // need to put delims back. it wasn't time to split yet.
        }
        seenDelims = 0;
        sawSpace = false;
        readyToSplit = false;
        delimBuffer.setLength(0);

        // record this char.
        resultBuffer.append(c);
      }
      else if (c == ' ') {  // whitespace can go either way
        if (delimBuffer.length() > 0) {
          delimBuffer.append(c);  // could go either way
        }
        else {
          resultBuffer.append(c);  // belongs in a result string
        }
        sawSpace = true;
      }
      else {  // character for delimBuffer
        ++seenDelims;

        if (seenDelims >= numDelims) {
          readyToSplit = true;
        }

        delimBuffer.append(c);
      }
    }

    if (resultBuffer.length() > 0) {
      addSegment(result, resultBuffer.toString()/*.trim()*/, false);
    }
    if (delimBuffer.length() > 0) {
      addSegment(result, delimBuffer.toString(), true);
    }

    return result;
  }

  protected final void addSegment(SegmentSequence result, String text, boolean isDelim) {
    if (text.length() > 0) {
      final Segment segment = result.add(text);
      segment.setManualFeature(CommonFeatures.DELIM, isDelim);
      if (numDelims > 1) segment.setManualFeature(CommonFeatures.MULTI_DELIM, isDelim);
    }
  }
}
