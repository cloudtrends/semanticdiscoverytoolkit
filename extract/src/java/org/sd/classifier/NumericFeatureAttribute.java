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
 * A numeric feature attribute definition class.
 * <p>
 * @author Spence Koehler
 */
public abstract class NumericFeatureAttribute extends BaseFeatureAttribute {

  protected NumericFeatureAttribute(FeatureDictionary featureDictionary, String name) {
    super(featureDictionary, name);
  }

  /**
   * Test whether this attribute is numeric.
   */
  public final boolean isNumeric() {
    return true;
  }

  /**
   * Safely downcast this feature attribute as numeric if it is numeric.
   */
  public final NumericFeatureAttribute asNumeric() {
    return this;
  }

  /**
   * Test whether this numeric attribute is an integer.
   */
  public boolean isInteger() {
    return false;
  }

  /**
   * Test whether this numeric attribute is a real.
   */
  public boolean isReal() {
    return false;
  }

  /**
   * Safely downcast this numeric feature attribute as an integer if it is an integer.
   */
  public IntegerFeatureAttribute asInteger() {
    return null;
  }

  /**
   * Safely downcast this numeric feature attribute as a real if it is a real.
   */
  public RealFeatureAttribute asReal() {
    return null;
  }
}
