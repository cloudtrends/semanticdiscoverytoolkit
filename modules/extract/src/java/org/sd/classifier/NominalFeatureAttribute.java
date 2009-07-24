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
 * A nominal feature attribute definition class.
 * <p>
 * @author Spence Koehler, Dave Barney
 */
public class NominalFeatureAttribute extends BaseFeatureAttribute {

  public NominalFeatureAttribute(FeatureDictionary featureDictionary, String name) {
    super(featureDictionary, name);
  }

  public String toString() {
    return "N(" + getName() + ")";
  }

  /**
   * Convert the value to a double, if valid. Unless dictionary is locked, add
   * the value to this attribute definition.
   *
   * @return the Double if the value is valid; otherwise, null.
   */
  public Double toDouble(String value) {
    int index = featureDictionary.getNominalValueIndex(this, value);
    return (index < 0) ? null : (double)index;
  }

  /**
   * Test whether this attribute is nominal.
   */
  public final boolean isNominal() {
    return true;
  }

  /**
   * Safely downcast this feature attribute as nominal if it is nominal.
   */
  public final NominalFeatureAttribute asNominal() {
    return this;
  }
  
  public final String[] getNominalValues() {
    return this.featureDictionary.getNominalFeatureValues(this);
  }
  
  public final boolean containsValue(String value) {
    return this.featureDictionary.containsNominalValue(this, value);
  }
}
