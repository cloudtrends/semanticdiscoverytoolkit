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


import org.sd.text.segment.BooleanFeatureComputation;
import org.sd.text.segment.SegmentSequence;
import org.sd.util.StringUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * A boolean feature computation to determine whether text has a digit.
 * <p>
 * @author Spence Koehler
 */
public class HasDigitFeature implements BooleanFeatureComputation {

  public static final String FEATURE_NAME = "hasDigit";

  private static final HasDigitFeature INSTANCE = new HasDigitFeature();

  public static final HasDigitFeature getInstance() {
    return INSTANCE;
  }

  private HasDigitFeature() {
  }

  /**
   * Get the feature type (or key).
   */
  public String getFeatureType() {
    return FEATURE_NAME;
  }

  /**
   * Compute whether the text contains at least one digit.
   *
   * @return true if a digit is found; otherwise, false.
   */
  public Boolean computeFeature(String text) {
    return StringUtil.hasDigit(text);
  }
  
  /**
   * Splits the string to be alternating sequences of digits and letters.
   * <p>
   * The pivot index is the first index where the text has a digit.
   * <p>
   * If a word of text contains digits and letters, the full word delimited by
   * spaces is kept together. Multiple words of digits and/or letters are also
   * kept together along with their delimiting spaces.
   *
   * @return the split text sequence or null if there is no digit in the text.
   */
  public SegmentSequence split(String text) {
    final SegmentSequence result = new SegmentSequence();

    final String[] pieces = text.split("\\s");
    final StringBuilder curString = new StringBuilder();
    boolean hasDigits = false;
    boolean didFirst = false;

    for (String piece : pieces) {
      boolean curHasDigit = StringUtil.hasDigit(piece);
      boolean startNewString = false;

      if (curHasDigit && curString.length() > 0 && !hasDigits) {
        // start a new string
        startNewString = true;

        hasDigits = true;  // toggle
      }
      else if (!curHasDigit && curString.length() > 0 && hasDigits) {
        // start a new string
        startNewString = true;

        hasDigits = false;
      }
      // else just add to current string

      // start a new string
      if (startNewString) {
        if (didFirst) {
          result.add(" ").setManualFeature(CommonFeatures.WHITESPACE, true);  // add whitespace back in
        }
        else {
          didFirst = true;
        }

        result.add(curString.toString()).setManualFeature(FEATURE_NAME, !hasDigits);  // note: have toggled hasDigits to mean opposite!
        
        curString.setLength(0);
      }

      // add piece to current string.
      if (curString.length() > 0) curString.append(' ');
      curString.append(piece);
    }

    if (curString.length() > 0) {
      if (didFirst) {
        result.add(" ").setManualFeature(CommonFeatures.WHITESPACE, true);  // add whitespace back in
      }
      result.add(curString.toString()).setManualFeature(FEATURE_NAME, hasDigits);  // note: not toggled!
    }

    return result.size() == 0 ? null : result;
  }
}
