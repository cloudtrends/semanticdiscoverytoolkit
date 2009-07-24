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


import org.sd.nlp.NormalizedString;
import org.sd.text.PatternFinder;
import org.sd.util.StringUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * A pattern finder split function that extracts the first found
 * pattern from the text.
 * <p>
 * @author Spence Koehler
 */
public class PatternExtractorSplitFunction extends PatternFinderSplitFunction {
  
  public PatternExtractorSplitFunction(PatternFinder patternFinder) {
    super(patternFinder);
  }

  /**
   * Split the text on found patterns and consecutive text around found patterns.
   * <p>
   * Multiple consecutive patterns will be split into multiple consecutive
   * segments. The segment's manual feature will be set with the key of the
   * pattern finder's type set to true or false depending on whether it was a pattern.
   *
   * @return the split text or null if the text doesn't have a recognized pattern.
   */
  public SegmentSequence split(PatternFinder patternFinder, String text) {
    SegmentSequence result = null;

    final NormalizedString nText = patternFinder.normalize(text);
    final int[] patternPos = patternFinder.findPatternPos(nText, PatternFinder.FULL_WORD);

    if (patternPos != null) {
      result = doSplit(patternFinder, nText, patternPos);
    }

    return result;
  }

  private final void addSegment(SegmentSequence result, PatternFinder patternFinder, NormalizedString text, int startPos, int endPos, boolean isPattern) {
    if (endPos > startPos) {
      final int patternLength = endPos - startPos;
      final String patternText = text.getOriginal(startPos, patternLength);

      int curStartPos = 0;
      int curEndPos = patternText.length();
      while (curStartPos < curEndPos && patternText.charAt(curStartPos) == ' ') ++curStartPos;

      if (curStartPos > 0) {  // found white at beginning
        result.add(" ").setManualFeature(CommonFeatures.WHITESPACE, true);
      }

      --curEndPos;
      while (curEndPos >= curStartPos && patternText.charAt(curEndPos) == ' ') --curEndPos;

      if (curStartPos <= curEndPos) {
        result.add(patternText.substring(curStartPos, curEndPos + 1)).setManualFeature(patternFinder.getType(), isPattern);

        if (curEndPos < patternText.length() - 1) {  // found white at end
          result.add(" ").setManualFeature(CommonFeatures.WHITESPACE, true);
        }
      }
    }
  }

  private final SegmentSequence doSplit(PatternFinder patternFinder, NormalizedString nText, int[] startNormPos) {
    final SegmentSequence result = new SegmentSequence();

    final int nLen = nText.getNormalizedLength();
    int priorEndPos = 0;

    while (startNormPos != null) {
      if (priorEndPos < nLen && startNormPos[0] - priorEndPos > 0) {
        addSegment(result, patternFinder, nText, priorEndPos, startNormPos[0], false);
      }
      priorEndPos = startNormPos[0] + startNormPos[1];
      addSegment(result, patternFinder, nText, startNormPos[0], priorEndPos, true);
      startNormPos = patternFinder.findNextPatternPos(nText, startNormPos, nLen, PatternFinder.FULL_WORD);
    }        

    // add non-pattern segment to end of string
    if (priorEndPos < nLen) {
      addSegment(result, patternFinder, nText, priorEndPos, nLen, false);
    }

    return result;
  }
}
