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
package org.sd.atn;


import java.util.HashMap;
import java.util.Map;

/**
 * Container for an interpretation of a parse.
 * <p>
 * @author Spence Koehler
 */
public class ParseInterpretation {
  
  private String classification;
  /**
   * The classification of this interpretation for future parsing.
   */
  public String getClassification() {
    return classification;
  }
  public void setClassification(String classification) {
    this.classification = classification;
  }

  private Object interpretation;
  /**
   * An object representing the detailed interpretation when non-null.
   */
  public Object getInterpretation() {
    return interpretation;
  }
  public void setInterpretation(Object interpretation) {
    this.interpretation = interpretation;
  }

  private double confidence;
  /**
   * A confidence associated with this interpretation, defaults to 1.0 if not
   * explicitly set.
   */
  public double getConfidence() {
    return confidence;
  }
  public void setConfidence(double confidence) {
    this.confidence = confidence;
  }

  private String toStringOverride;
  /**
   * A string to override this instance's ToString when non-null.
   */
  public String getToStringOverride() {
    return toStringOverride;
  }
  public void setToStringOverride(String toStringOverride) {
    this.toStringOverride = toStringOverride;
  }

  private Map<String, Object> category2Value;
  /**
   * Mappings from categories to their interpreted values for components
   * of the interpretation.
   */
  public Map<String, Object> getCategory2Value() {
    return category2Value;
  }

  public ParseInterpretation()
  {
    init(null);
  }

  public ParseInterpretation(String classification)
  {
    init(classification);
  }

  private final void init(String classification)
  {
    this.classification = classification;
    this.interpretation = null;
    this.confidence = 1.0;
    this.toStringOverride = null;
    this.category2Value = null;
  }

  /**
   * Add a mapping to this instance.
   */
  public void add(String category, Object value)
  {
    if (category2Value == null) category2Value = new HashMap<String, Object>();
    category2Value.put(category, value);
  }

  /**
   * Get the category mapping or null.
   */
  public Object get(String category)
  {
    Object result = null;

    if (category2Value != null) {
      result = category2Value.get(category);
    }

    return result;
  }

  public String toString()
  {
    String result = toStringOverride;

    if (result == null)
    {
      if (interpretation != null)
      {
        result = interpretation.toString();
      }
      else
      {
        result = super.toString();
      }
    }

    return result;
  }
}
