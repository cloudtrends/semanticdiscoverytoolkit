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


import org.sd.util.range.IntegerRange;
import org.sd.util.range.NumericRange;
import org.sd.util.range.RealRange;

import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

/**
 * Class to manage constraints for a feature attribute.
 * <p>
 * @author Spence Koehler
 */
public class AttributeConstraints {

  private String attributeName;          // name of FeatureAttribute
  private AttributeType attributeType;   // type of FeatureAttribute
  private Set<String> extractionTypes;   // extraction types that map to this attribute
  private Constraint constraint;         // the constraint to apply, built from 'values'
  private boolean isOpen;                // flag that's true when values can grow

  /**
   * Construct a new instance from the String.
   * <p>
   * The string must be of the form:
   * <p>
   * attributeName|attributeType|isOpenFlag|constraints
   * <p>
   * Where <ul>
   * <li>attributeName is the name of the feature attribute being constrained</li>
   * <li>attributeType is the attributeType: {NOMINAL, INTEGER, REAL}</li>
   * <li>isOpenFlag is "true" if values should be added as encountered or "false" if all values are defined by the constraints.</li>
   * <li>constraints is of the form: a comma-delimitted list of strings for a nominal or a range string (as documented in AbstractNumericRange.parseValue) for numeric (integer or real).</li>
   * </ul>
   * Note that if isOpenFlag and constraints are missing, isOpenFlag defaults to true.
   */
  public AttributeConstraints(String string) {
    final String[] pieces = string.split("\\s*\\|\\s*");
    
    final String attributeName = pieces[0];
    AttributeType attributeType = pieces.length > 1 ? Enum.valueOf(org.sd.classifier.AttributeType.class, pieces[1].toUpperCase()) : AttributeType.NOMINAL;
    boolean isOpen = pieces.length > 2 ? "true".equals(pieces[2].toLowerCase()) : true;
    String values = pieces.length > 3 ? pieces[3] : attributeType == AttributeType.NOMINAL ? attributeName : null;

    init(attributeName, attributeType, values, isOpen);
  }

  /**
   * Construct a new open instance without any extraction types or values.
   */
  public AttributeConstraints(String attributeName, AttributeType attributeType) {
    this(attributeName, attributeType, null, true);
  }

  /**
   * Construct a new instance with the given info.
   * <p>
   * Note that the behavior of an instance is controlled by the attribute type. This
   * includes the interpretation of elements in values. When nominal, each value is
   * a string. When integer or real, each value is a string of the form, for example:
   * <p>
   * "(a-b],c^d,[e-f)"
   * <p>
   * Where a, b, c, d, e, and f are integers or reals (depending on attributeType) and
   * represent the range of values from a (exclusive) to b (inclusive), c plus or minus d
   * (exclusive), and the values from e (inclusive) to f (exclusive).
   *
   * @param attributeName    The name of the feature attribute defined by this instance.
   * @param attributeType    The type of the feature attribute defined by this instance.
   * @param values           The values or ranges that the attribute can have in the form of a
   *                         comma-delimitted string of nominals or ranges.
   * @param isOpen           If true, new values can be added; otherwise, no new values can be added.
   */
  public AttributeConstraints(String attributeName, AttributeType attributeType, String values, boolean isOpen) {
    init(attributeName, attributeType, values, isOpen);
  }

  private final void init(String attributeName, AttributeType attributeType, String values, boolean isOpen) {
    this.attributeName = attributeName;
    this.attributeType = attributeType;
    this.extractionTypes = new HashSet<String>();
    this.constraint = buildConstraint(values);
    this.isOpen = isOpen;
  }
  
  /**
   * Provide public access to nominal values for writing to file.
   * 
   * @return a {@link Set} of {@link String}'s of the nominal values if this contraint is a nominal value, otherwise null.
   */
  public Set<String> getNominalValues() {
    Set<String> result = null;

    final NominalConstraint nominalConstraint = constraint.asNominalConstraint();
    if (nominalConstraint != null) {
      result = nominalConstraint.getValues();
    }

    return result;
  }

  /**
   * Add another type of extraction that applies to this instance.
   */
  public void addExtractionType(String extractionType) {
    this.extractionTypes.add(extractionType);
  }

  /**
   * Get the attribute name.
   */
  public String getAttributeName() {
    return attributeName;
  }

  /**
   * Get the attribute type.
   */
  public AttributeType getAttributeType() {
    return attributeType;
  }

  /**
   * Test whether this instance is open.
   */
  public boolean isOpen() {
    return isOpen;
  }

  /**
   * Set the value on the feature vector for this instance's attribute if valid.
   *
   * @return true if the value was set on the featureVector; otherwise, false.
   */
  public boolean setIfValid(String value, FeatureDictionary featureDictionary, FeatureVector featureVector) {
    return featureVector.setValue(getIfValid(value, featureDictionary), value);
  }

  /**
   * Get the feature attribute if the value meets the constraints for this
   * instance and the dictionary supplies it.
   */
  public FeatureAttribute getIfValid(String value, FeatureDictionary featureDictionary) {
    FeatureAttribute result = null;

    switch (attributeType) {
      case NOMINAL :
        result = getNominalIfValid(value, featureDictionary);
        break;
      case INTEGER :
        result = getIntegerIfValid(value, featureDictionary);
        break;
      case REAL :
        result = getRealIfValid(value, featureDictionary);
        break;
    }

    return result;
  }

  private final Constraint buildConstraint(String values) {
    Constraint result = null;

    if (values == null) return result;  // no constraints.

    switch (attributeType) {
      case NOMINAL :
        result = new NominalConstraint(values);
        break;
      case INTEGER :
        result = new IntegerConstraint(values);
        break;
      case REAL :
        result = new RealConstraint(values);
        break;
    }

    return result;
  }

  /**
   * Check the value against this instance's constraints, then retrieve
   * from the dictionary.
   */
  private final NominalFeatureAttribute getNominalIfValid(String value, FeatureDictionary featureDictionary) {
    NominalFeatureAttribute result = null;

    // no need to consult constraints if feature dictionary is locked because it'll 'constrain' the value.
    if (featureDictionary.isLocked() || passesConstraints(value)) {
      result = featureDictionary.getNominalFeatureAttribute(attributeName, value);
    }

    return result;
  }

  private final IntegerFeatureAttribute getIntegerIfValid(String value, FeatureDictionary featureDictionary) {
    IntegerFeatureAttribute result = null;

    // do need to consult constraints because feature dictionary doesn't know about ranges.
    if (passesConstraints(value)) {
      result = featureDictionary.getIntegerFeatureAttribute(attributeName);
    }

    return result;
  }

  private final RealFeatureAttribute getRealIfValid(String value, FeatureDictionary featureDictionary) {
    RealFeatureAttribute result = null;

    // do need to consult constraints because feature dictionary doesn't know about ranges.
    if (passesConstraints(value)) {
      result = featureDictionary.getRealFeatureAttribute(attributeName);
    }

    return result;
  }

  // package protected access for JUnit testing.
  final boolean passesConstraints(String value) {
    boolean result = true;

    if (constraint != null) {
      result = constraint.isValid(value, isOpen);
    }

    return result;
  }



  private interface Constraint {
    public boolean isValid(String string, boolean isOpen);

    public NominalConstraint asNominalConstraint();
    public NumericConstraint asNumericConstraint();
    public IntegerConstraint asIntegerConstraint();
    public RealConstraint asRealConstraint();
  }

  private abstract class BaseConstraint implements Constraint {
    public NominalConstraint asNominalConstraint() {
      return null;
    }
    public NumericConstraint asNumericConstraint() {
      return null;
    }
    public IntegerConstraint asIntegerConstraint() {
      return null;
    }
    public RealConstraint asRealConstraint() {
      return null;
    }
  }

  private class NominalConstraint extends BaseConstraint {
    private Set<String> values;

    public NominalConstraint(String values) {
      this.values = new TreeSet<String>();
      final String[] strings = values.split("\\s*,\\s*");
      for (String string : strings) this.values.add(string);
    }

    public boolean isValid(String string, boolean isOpen) {
      boolean result = false;

      if (isOpen) {
        values.add(string);
        result = true;
      }
      else {
        result = values.contains(string);
      }

      return result;
    }
    
    public final NominalConstraint asNominalConstraint() {
      return this;
    }

    public Set<String> getValues() {
      return values;
    }
  }

  private abstract class NumericConstraint extends BaseConstraint {
    private NumericRange range;

    protected NumericConstraint(NumericRange range) {
      this.range = range;
    }

    public boolean isValid(String string, boolean isOpen) {
      boolean result = false;

      if (isOpen) {
        range.include(string);
        result = true;
      }
      else {
        result = range.includes(string);
      }

      return result;
    }

    public final NumericConstraint asNumericConstraint() {
      return this;
    }
  }

  private final class IntegerConstraint extends NumericConstraint {
    public IntegerConstraint(String values) {
      super(new IntegerRange(values.split("\\s*,\\s*")));
    }
    public final IntegerConstraint asIntegerConstraint() {
      return this;
    }
  }

  private final class RealConstraint extends NumericConstraint {
    public RealConstraint(String values) {
      super(new RealRange(values.split("\\s*,\\s*")));
    }
    public final RealConstraint asRealConstraint() {
      return this;
    }
  }
}
