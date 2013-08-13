/*
    Copyright 2011 Semantic Discovery, Inc. (www.semanticdiscovery.com)

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
package org.sd.atn;


import java.util.Map;
import org.sd.atn.ResourceManager;
import org.sd.token.Normalizer;
import org.sd.token.Token;
import org.sd.util.range.IntegerRange;
import org.sd.util.Usage;
import org.sd.xml.DomElement;

/**
 * Classifier for recognizing digits (for improved efficiency over regexes).
 * <p>
 * Attributes:
 * <ul>
 * <li>feature -- (optional, default='value') specifies name of feature holding
 *                 recognized value.</li>
 * <li>range -- (optional, default unbounded) specifies the range of acceptable
 *               values</li>
 * <li>acceptUnknowns -- (optional, default=false) specifies whether to
 *                       accept '?' as an unknown digit</li>
 * <li>requireTrueDigit -- (optional, default=true) specifies whether to accept
 *                         letters that look like digits without finding any
 *                         true digits</li>
 * <li>minLength -- (optional, default=0 [unbounded]) specifies the minimum
 *                  acceptable input text length (e.g. at least 2 digits).
 * <li>ignoreLetters -- (optional, default=false) specifies whether to ignore
 *                      letters and accept any digits found.
 * </ul>
 *
 * @author Spence Koehler
 */
@Usage(notes =
       "org.sd.atn.RoteListClassifier for recognizing digits\n" +
       "(for improved efficiency over regexes).\n" +
       "\n" +
       "Attributes:\n" +
       "\n" +
       "  feature -- (optional, default='value') specifies name of feature holding\n" +
       "             recognized value.\n" +
       "  range -- (optional, default unbounded) specifies the range of acceptable\n" +
       "           values\n" +
       "  acceptUnknowns -- (optional, default=false) specifies whether to\n" +
       "                    accept '?' as an unknown digit\n" +
       "  requireTrueDigit -- (optional, default=true) specifies whether to accept\n" +
       "                      letters that look like digits without finding any\n" +
       "                      true digits\n" +
       "  minLength -- (optional, default=0 [unbounded]) specifies the minimum\n" +
       "               acceptable input text length (e.g. at least 2 digits).\n" +
       "  ignoreLetters -- (optional, default=false) specifies whether to ignore\n" +
       "                   letters and accept any digits found."
  )
public class DigitsClassifier extends RoteListClassifier {

  private String featureName;
  private IntegerRange range;
  private boolean acceptUnknowns;
  private boolean requireTrueDigit;
  private int minLength;
  private boolean ignoreLetters;

  public DigitsClassifier(DomElement classifierIdElement, ResourceManager resourceManager, Map<String, Normalizer> id2Normalizer) {
    super(classifierIdElement, resourceManager, id2Normalizer);

    // ignore any maxWordCount specified by the element and set to 1
    getTokenClassifierHelper().setMaxWordCount(1);

    this.featureName = classifierIdElement.getAttributeValue("feature", "value");

    this.range = null;
    final String rangeString = classifierIdElement.getAttributeValue("range", null);
    if (rangeString != null) {
      this.range = new IntegerRange(rangeString);
    }

    this.acceptUnknowns = classifierIdElement.getAttributeBoolean("acceptUnknowns", false);
    this.requireTrueDigit = classifierIdElement.getAttributeBoolean("requireTrueDigit", true);

    this.minLength = classifierIdElement.getAttributeInt("minLength", 0);

    this.ignoreLetters = classifierIdElement.getAttributeBoolean("ignoreLetters", false);
  }
  
  public boolean doClassify(Token token, AtnState atnState) {
    boolean result = false;

    final String text = token.getText();

    if (!doClassifyStopword(token, atnState)) {
      result = doClassifyTerm(token, atnState);

      if (!result) {
        if (text.length() >= minLength) {
          result = verify(text);

          if (!result && acceptUnknowns) {
            final String unknownEnhancedText = enhanceUnknowns(text);
            if (unknownEnhancedText != null) {
              result = verify(unknownEnhancedText);
            }
          }
        }
      }
    }

    if (result && featureName != null && !"".equals(featureName)) {
      if (!token.hasFeatures() || !token.getFeatures().hasFeatureType(featureName)) {
        token.setFeature(featureName, text, this);
      }
    }

    return result;
  }

  private final boolean verify(String text) {
    boolean result = false;

    final int[] intValue = new int[]{0};

    if (text != null && !"".equals(text) && verifyDigits(text, intValue)) {
      if (range == null || range.includes(intValue[0])) {
        result = true;
      }
    }

    return result;
  }

  private final boolean verifyDigits(String text, int[] intValue) {
    boolean result = false;

    if (!ignoreLetters) {
      result = getTokenClassifierHelper().isDigits(text, intValue, requireTrueDigit);
    }
    else {
      result = getTokenClassifierHelper().hasDigits(text, intValue, requireTrueDigit);
    }

    return result;
  }

  private final String enhanceUnknowns(String text) {
    final StringBuilder result = new StringBuilder();

    boolean foundUnknown = false;
    final int len = text.length();

    for (int i = 0; i < len; ++i) {
      char c = text.charAt(i);
      if (c == '?') {
        foundUnknown = true;
        c = '1';
      }
      result.append(c);
    }

    return foundUnknown ? result.toString() : null;
  }
}
