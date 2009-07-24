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


/**
 * Defines an attribute by name (schema level).
 * <p>
 * @author Spence Koehler, Dave Barney
 */
public interface FeatureAttribute {

  /**
   * Get this feature attribute's dictionary.
   */
  public FeatureDictionary getFeatureDictionary();

  /**
   * Get this attribute's name.
   */
  public String getName();

  /**
   * Convenience method to determine whether an attribute is a bag
   * of words attribute based on its name pattern.
   *
   * @return the {bagName, bagWord} or null if it isn't a bag of words attribute.
   */
  public String[] getBagOfWords();

  /**
   * Convert the value to a double, if valid. Unless dictionary is locked, add
   * the value to this attribute definition.
   *
   * @return the Double if the value is valid; otherwise, null.
   */
  public Double toDouble(double value);

  /**
   * Convert the value to a double, if valid. Unless dictionary is locked, add
   * the value to this attribute definition.
   *
   * @return the Double if the value is valid; otherwise, null.
   */
  public Double toDouble(int value);

  /**
   * Convert the value to a double, if valid. Unless dictionary is locked, add
   * the value to this attribute definition.
   *
   * @return the Double if the value is valid; otherwise, null.
   */
  public Double toDouble(String value);


  /**
   * Test whether this attribute is numeric.
   */
  public boolean isNumeric();

  /**
   * Test whether this attribute is nominal.
   */
  public boolean isNominal();

  /**
   * Safely downcast this feature attribute as numeric if it is numeric.
   */
  public NumericFeatureAttribute asNumeric();

  /**
   * Safely downcast this feature attribute as nominal if it is nominal.
   */
  public NominalFeatureAttribute asNominal();

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
  public Double getDefaultValue();
}
