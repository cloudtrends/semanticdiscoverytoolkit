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
import org.sd.xml.DomElement;
import org.sd.xml.DomNode;
import org.sd.xml.DomUtil;


/**
 * Abstract TokenClassifier with convenience methods for adding features.
 * <p>
 * @author Spence Koehler
 */
public abstract class AbstractTokenClassifier implements TokenClassifier {
  
  /**
   * Normalizer to use when classifying token text.
   */
  private Normalizer normalizer;
  public Normalizer getNormalizer() {
    return normalizer;
  }
  protected void setNormalizer(Normalizer normalizer) {
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
  protected void setMaxWordCount(int maxWordCount) {
    this.maxWordCount = maxWordCount;
  }


  /**
   * Classification utility to identify text as digits.
   * <p>
   * If true, the asInt[0] will be the numerical value of the digits.
   */
  public static boolean isDigits(String text, int[] asInt) {
    boolean result = true;

    for (int textIndex = 0; textIndex < text.length(); ++textIndex) {
      if (!Character.isDigit(text.codePointAt(textIndex))) {
        result = false;
        break;
      }
    }

    if (result) asInt[0] = Integer.parseInt(text);

    return result;
  }


  /**
   * Classify the given token, adding features as appropriate.
   */
  public abstract boolean doClassify(Token token);


  public boolean classify(Token token) {
    boolean result = false;

    if (token.getWordCount() <= maxWordCount || maxWordCount == 0) {
      result = doClassify(token);
    }

    return result;
  }

  protected AbstractTokenClassifier() {
    init(null, 0);
  }

  protected AbstractTokenClassifier(Normalizer normalizer, int maxWordCount) {
    init(normalizer, maxWordCount);
  }

  protected AbstractTokenClassifier(DomElement classifierIdElement, Map<String, Normalizer> id2Normalizer) {
    final Normalizer normalizer = loadNormalizer((DomNode)classifierIdElement.selectSingleNode("normalizer"), id2Normalizer);
    final int maxWordCount = DomUtil.getSelectedNodeInt(classifierIdElement, "maxWordCount", 0);

    init(normalizer, maxWordCount);
  }

  private void init(Normalizer normalizer, int maxWordCount) {
    this.normalizer = normalizer;
    this.maxWordCount = maxWordCount;
  }

  /**
   * Add a feature with the given type and value and P=1.0 to the token.
   */
  protected void addFeature(Token token, String type, String value) {
    addFeature(token, type, value, 1.0);
  }

  /**
   * Add a feature with the given type, value, and probability to the token.
   */
  protected void addFeature(Token token, String type, String value, double p) {
    token.getFeatures().add(new Feature(type, value, p, this));
  }

  /**
   * Get the token's normalized text according to this Classifier's
   * normalizer, returning null if the MaxWordCount constraint is not met.
   */
  protected String getNormalizedText(Token token) {
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
