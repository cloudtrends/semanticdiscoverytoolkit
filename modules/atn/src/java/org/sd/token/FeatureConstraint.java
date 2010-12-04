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


/**
 * Container for specication of a feature constraint.
 * <p>
 * @author Spence Koehler
 */
public class FeatureConstraint {
  
  public static FeatureConstraint getInstance(String type, Object source, Class featureValueType) {
    final FeatureConstraint constraint = new FeatureConstraint();
    constraint.setType(type);
    constraint.setClassType(source != null ? source.getClass() : null);
    constraint.setFeatureValueType(featureValueType != null ? featureValueType : null);
    return constraint;
  }


  private String type;
  public String getType() {
    return type;
  }
  public void setType(String type) {
    this.type = type;
  }

  private Object value;
  public Object getValue() {
    return value;
  }
  public void setValue(Object value) {
    this.value = value;
  }

  private double minP;
  public double getMinP() {
    return minP;
  }
  public void setMinP(double MinP) {
    this.minP = minP;
  }

  private double maxP;
  public double getMaxP() {
    return maxP;
  }
  public void setMaxP(double MaxP) {
    this.maxP = maxP;
  }

  private Class classType;
  public Class getClassType() {
    return classType;
  }
  public void setClassType(Class classType) {
    this.classType = classType;
  }

  private Class featureValueType;
  public Class getFeatureValueType() {
    return featureValueType;
  }
  public void setFeatureValueType(Class featureValueType) {
    this.featureValueType = featureValueType;
  }

  public FeatureConstraint() {
    this.type = null;
    this.value = null;
    this.minP = 0.0;
    this.maxP = 1.0;
    this.classType = null;
    this.featureValueType = null;
  }

  /**
   * Determine whether this constraint includes the given feature.
   */
  public boolean includes(Feature feature) {
    boolean result = true;

    if (result && type != null) {
      result = type.equals(feature.getType());
    }

    if (result && value != null) {
      result = value.equals(feature.getValue());
    }

    if (result && minP <= feature.getP() && maxP >= feature.getP()) {
      result = true;
    }

    if (result && classType != null) {
      //result = classType.isInstance(feature.getSource());
      final Class<?> sourceType = feature.getSourceType();
      if (sourceType == null) {
        result = false;
      }
      else {
        result = sourceType.isAssignableFrom(classType);
      }
    }

    if (result && featureValueType != null) {
      result = featureValueType.isInstance(feature.getValue());
    }

    return result;
  }
}
