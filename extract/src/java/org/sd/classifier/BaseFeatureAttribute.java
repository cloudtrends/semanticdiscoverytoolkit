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
package org.sd.classifier;


import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Abstract base implementation of the FeatureAttribute interface.
 * <p>
 * @author Spence Koehler, Dave Barney
 */
public abstract class BaseFeatureAttribute implements FeatureAttribute {
  
  private static final Pattern BAG_OF_WORDS_PATTERN = Pattern.compile("^_([a-z0-9A-Z]+)_(.*)");

  protected FeatureDictionary featureDictionary;
  protected String name;
  private Double defaultValue;
  private String[] _isBagOfWords;

  protected BaseFeatureAttribute(FeatureDictionary featureDictionary, String name) {
    this.featureDictionary = featureDictionary;
    this.name = name;
    this.defaultValue = null;
    this._isBagOfWords = null;
  }

  /**
   * Get this feature attribute's dictionary.
   */
  public FeatureDictionary getFeatureDictionary() {
    return featureDictionary;
  }

  /**
   * Get this attribute's name.
   */
  public String getName() {
    return name;
  }

  /**
   * Convenience method to determine whether an attribute is a bag
   * of words attribute based on its name pattern.
   *
   * @return the {bagName, bagWord} or null if it isn't a bag of words attribute.
   */
  public String[] getBagOfWords() {
    if (_isBagOfWords == null) {
      final Matcher matcher = BAG_OF_WORDS_PATTERN.matcher(name);
      if (matcher.matches()) {
        _isBagOfWords = new String[]{matcher.group(1), matcher.group(2)};
      }
      else {
        _isBagOfWords = new String[]{};
      }
    }
    return _isBagOfWords.length == 0 ? null : _isBagOfWords;
  }

  /**
   * Convert the value to a double, if valid. Unless dictionary is locked, add
   * the value to this attribute definition.
   *
   * @return the Double if the value is valid; otherwise, null.
   */
  public Double toDouble(double value) {
    return value;
  }

  /**
   * Convert the value to a double, if valid. Unless dictionary is locked, add
   * the value to this attribute definition.
   *
   * @return the Double if the value is valid; otherwise, null.
   */
  public Double toDouble(int value) {
    return (double)value;
  }

  /**
   * Convert the value to a double, if valid. Unless dictionary is locked, add
   * the value to this attribute definition.
   *
   * @return the Double if the value is valid; otherwise, null.
   */
  public Double toDouble(String value) {
    return null;
  }

  /**
   * Test whether this attribute is numeric.
   */
  public boolean isNumeric() {
    return false;
  }

  /**
   * Test whether this attribute is nominal.
   */
  public boolean isNominal() {
    return false;
  }

  /**
   * Safely downcast this feature attribute as numeric if it is numeric.
   */
  public NumericFeatureAttribute asNumeric() {
    return null;
  }

  /**
   * Safely downcast this feature attribute as nominal if it is nominal.
   */
  public NominalFeatureAttribute asNominal() {
    return null;
  }

  /**
   * Get a default value for this attribute if it is an attribute whose value
   * is always present, even when not marked as such. If there is no default
   * value then the "unknown" value (null here or a question mark in an arff)
   * will be emitted.
   * <p>
   * A default value exists for features whose value is always known, but hasn't
   * been explicitly set.
   * <p>
   * For example, bag of words features are either present or not. If not present,
   * they haven't been seen, but it is known that they are not present as opposed
   * to not knowing whether they are present.
   *
   * @return the default value or null if the attribute can be unknown.
   */
  public Double getDefaultValue() {
    return defaultValue;
  }

  /**
   * Set a default value for this attribute.
   * <p>
   * A default value of null means this attribute can be unknown; otherwise,
   * when not seen, its value will be assumed to be the default value.
   *
   * @return the previously set default value.
   */
  public Double setDefaultValue(Double defaultValue) {
    Double result = defaultValue;
    this.defaultValue = defaultValue;
    return result;
  }
}
