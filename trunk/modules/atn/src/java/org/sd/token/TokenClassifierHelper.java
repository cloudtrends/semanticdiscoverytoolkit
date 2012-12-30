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
package org.sd.token;


import java.util.Map;
import org.sd.token.Token;
import org.sd.util.range.IntegerRange;
import org.sd.xml.DomElement;
import org.sd.xml.DomNode;
import org.sd.xml.DomUtil;


/**
 * Abstract TokenClassifier with convenience methods for adding features.
 * <p>
 * @author Spence Koehler
 */
public class TokenClassifierHelper {
  
  private Map<String, Normalizer> id2Normalizer;
  public Map<String, Normalizer> getId2Normalizer() {
    return id2Normalizer;
  }

  /**
   * Normalizer to use when classifying token text.
   */
  private Normalizer normalizer;
  public Normalizer getNormalizer() {
    return normalizer;
  }
  public void setNormalizer(Normalizer normalizer) {
    this.normalizer = normalizer;
  }

  /**
   * Maximum number of words to be considered for classification.
   * 
   * If 0, then any number of words are accepted.
   */
  private int maxWordCount;
  public int getMaxWordCount() {
    return maxWordCount;
  }
  public void setMaxWordCount(int maxWordCount) {
    this.maxWordCount = maxWordCount;
  }

  /**
   * Valid range for length of a token to be considered for classification.
   * (Unlimited if null.)
   */
  private IntegerRange validTokenLength;
  public IntegerRange getValidTokenLength() {
    return validTokenLength;
  }
  public void setValidTokenLength(IntegerRange validTokenLength) {
    this.validTokenLength = validTokenLength;
  }
  public boolean lengthIsInRange(Token token) {
    return validTokenLength == null || validTokenLength.includes(token.getLength());
  }

  /**
   * Classifier's name.
   */
  private String name;
  public String getName() {
    return name;
  }
  public void setName(String name) {
    this.name = name;
  }

  /**
   * Classification utility to identify text as digits.
   * <p>
   * If true, the asInt[0] will be the numerical value of the digits.
   */
  public static boolean isDigits(String text, int[] asInt) {
    return isDigits(text, asInt, true);
  }

  public static boolean isDigits(String text, int[] asInt, boolean requireTrueDigit) {
    boolean result = true;

    int value = 0;
    int tens = 1;
    boolean hasTrueDigit = !requireTrueDigit;

    // scan from right to left, building the integer value
    for (int textIndex = text.length() - 1; textIndex >= 0; --textIndex) {
      int c = text.codePointAt(textIndex);
      int digit = 0;

      if (c >= '0' && c <= '9') {
        digit = (int)(c - '0');
        hasTrueDigit = true;
      }
      else {
        // check for OCR-like digits
        final Integer ocrDigit = interpretMistakenDigit(c);
        if (ocrDigit != null) {
          digit = ocrDigit;
        }
        else {
          result = false;
          break;
        }
      }

      if (digit > 0) value += (digit * tens);
      tens *= 10;
    }

    // only accept OCR-like errors if there was a true digit in the mix
    if (!hasTrueDigit) result = false;

    if (result) asInt[0] = value;

    return result;
  }

  /**
   * Classification utility to identify digits in text.
   * <p>
   * If true, the asInt[0] will be the numerical value of the digits.
   */
  public static boolean hasDigits(String text, int[] asInt) {
    return hasDigits(text, asInt, true);
  }

  public static boolean hasDigits(String text, int[] asInt, boolean requireTrueDigit) {
    boolean result = false;

    int value = 0;
    int tens = 1;
    boolean hasTrueDigit = !requireTrueDigit;

    // scan from right to left, building the integer value
    for (int textIndex = text.length() - 1; textIndex >= 0; --textIndex) {
      int c = text.codePointAt(textIndex);
      int digit = 0;

      if (c >= '0' && c <= '9') {
        digit = (int)(c - '0');
        result = hasTrueDigit = true;
      }
      else {
        // check for OCR-like digits
        final Integer ocrDigit = interpretMistakenDigit(c);
        if (ocrDigit != null) {
          digit = ocrDigit;
          result = true;
        }
      }

      if (digit > 0) value += (digit * tens);
      tens *= 10;
    }

    // only accept OCR-like errors if there was a true digit in the mix
    if (!hasTrueDigit) result = false;

    if (result) asInt[0] = value;

    return result;
  }


  public boolean meetsConstraints(Token token) {
    return
      ((maxWordCount == 0 || token.getWordCount() <= maxWordCount) &&
       lengthIsInRange(token));
  }

  public TokenClassifierHelper() {
    init(null, 0);
  }

  public TokenClassifierHelper(Normalizer normalizer, int maxWordCount) {
    init(normalizer, maxWordCount);
  }

  //
  // <classifierId validTokenLength='rangeExpr'>
  //   <maxWordCount>N</maxWordCount>
  //   <normalizer>...</normalizer>
  //   <jclass>...classifier class...</jclass>
  //   ...config elts for classifier class...
  // </classifierId>
  //

  public TokenClassifierHelper(DomElement classifierIdElement, Map<String, Normalizer> id2Normalizer) {
    this.id2Normalizer = id2Normalizer;
    final Normalizer normalizer = loadNormalizer((DomNode)classifierIdElement.selectSingleNode("normalizer"), id2Normalizer);
    final int maxWordCount = DomUtil.getSelectedNodeInt(classifierIdElement, "maxWordCount", 0);
    this.name = classifierIdElement.getAttributeValue("name", null);

    init(normalizer, maxWordCount);

    final String vtlString = classifierIdElement.getAttributeValue("validTokenLength", null);
    if (vtlString != null && !"".equals(vtlString)) {
      this.validTokenLength = new IntegerRange(vtlString);
    }
  }

  private void init(Normalizer normalizer, int maxWordCount) {
    this.normalizer = normalizer;
    this.maxWordCount = maxWordCount;
    this.validTokenLength = null;
  }

  /**
   * Add a feature with the given type and value and P=1.0 to the token.
   */
  public void addFeature(Token token, String type, String value) {
    addFeature(token, type, value, 1.0);
  }

  /**
   * Add a feature with the given type, value, and probability to the token.
   */
  public void addFeature(Token token, String type, String value, double p) {
    token.getFeatures().add(new Feature(type, value, p, this));
  }

  public static Integer interpretMistakenDigit(int c) {
    Integer digit = null;

    // check for OCR-like digits
    final int lc = Character.toLowerCase(c);
    if (lc == 'o') {
      digit = 0;
    }
    else if (lc == 'l' || lc == 'i') {
      digit = 1;
    }
    else if (c == 'B') {
      digit = 8;
    }
    else if (lc == 'b') {
      digit = 6;
    }
    else if (lc == 's') {
      digit = 5;
    }

    return digit;
  }

  /**
   * Get the token's normalized text according to this Classifier's
   * normalizer, returning null if the MaxWordCount constraint is not met.
   */
  public String getNormalizedText(Token token) {
    if (maxWordCount > 0 && token.getWordCount() > maxWordCount) return null;

    String result = token.getText();

    if (normalizer != null) {
      boolean alreadyNormalized = false;

      final String tokenFeature = normalizer.getTokenFeature();
      if (tokenFeature != null) {
        Object normalized = token.getFeatureValue(tokenFeature, normalizer);
        if (normalized != null) {
          result = normalized.toString();
          alreadyNormalized = true;
        }
      }

      if (!alreadyNormalized) {
        result = normalizer.normalize(token.getText());

        if (tokenFeature != null) {
          token.setFeature(tokenFeature, result, normalizer);
        }
      }
    }

    return result;
  }

  public String normalize(String text) {
    String result = text;

    if (normalizer != null) {
      result = normalizer.normalize(text);
    }

    return result;
  }

  private Normalizer loadNormalizer(DomNode domNode, Map<String, Normalizer> id2Normalizer) {
    if (domNode == null) return null;

    Normalizer result = null;
    final String id = domNode.getAttributeValue("id");

    result = id2Normalizer.get(id);

    if (result == null)
    {
      // load from 'options'
      final DomElement optionsElement = (DomElement)domNode.selectSingleNode("options");
      if (optionsElement != null) {
        final StandardNormalizerOptions options = new StandardNormalizerOptions(optionsElement);
        result = new StandardNormalizer(options);
      }
    }

    return result;
  }
}
