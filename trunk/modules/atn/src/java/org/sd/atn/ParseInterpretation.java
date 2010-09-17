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


import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import org.sd.util.tree.Tree;
import org.sd.xml.XmlLite;

/**
 * Container for an interpretation of a parse.
 * <p>
 * @author Spence Koehler
 */
public class ParseInterpretation implements Serializable {
  
  private String classification;
  private Object interpretation;
  private double confidence;
  private String toStringOverride;
  private Map<String, Object> category2Value;

  //todo: phase out above fields add confidence as attr on tree nodes; add xml accessors
  private Tree<XmlLite.Data> interpTree;

  private transient AtnParse sourceParse;

  public ParseInterpretation() {
    init(null);
  }

  public ParseInterpretation(String classification) {
    init(classification);
  }

  public ParseInterpretation(Tree<XmlLite.Data> interpTree) {
    this.interpTree = interpTree;

    init(interpTree.getData().asTag().name);
  }

  private final void init(String classification) {
    if (this.interpTree == null) this.interpTree = new Tree<XmlLite.Data>(new XmlLite.Tag(classification, false));

    this.classification = classification;
    this.interpretation = null;
    this.confidence = 1.0;
    this.toStringOverride = null;
    this.category2Value = null;
  }

  /**
   * Set the source parse for this interpretation.
   */
  public void setSourceParse(AtnParse sourceParse) {
    this.sourceParse = sourceParse;
  }

  /**
   * Get the source parse for this interpretation, if available.
   */
  public AtnParse getSourceParse() {
    return sourceParse;
  }

  public Tree<XmlLite.Data> getInterpTree() {
    return this.interpTree;
  }

  /**
   * The classification of this interpretation for future parsing.
   */
  public String getClassification() {
    return classification;
  }

  /**
   * The classification of this interpretation for future parsing.
   */
  public void setClassification(String classification) {
    this.classification = classification;
  }

  /**
   * An object representing the detailed interpretation when non-null.
   * <p>
   * NOTE: If this interpretation is to be persisted, the Object must be serializable.
   */
  public Object getInterpretation() {
    return interpretation == null ? (interpTree == null ? classification : interpTree) : interpretation;
  }

  /**
   * An object representing the detailed interpretation when non-null.
   * <p>
   * NOTE: If this interpretation is to be persisted, the Object must be serializable.
   */
  public void setInterpretation(Object interpretation) {
    this.interpretation = interpretation;
  }

  /**
   * A confidence associated with this interpretation, defaults to 1.0 if not
   * explicitly set.
   */
  public double getConfidence() {
    return confidence;
  }

  /**
   * A confidence associated with this interpretation, defaults to 1.0 if not
   * explicitly set.
   */
  public void setConfidence(double confidence) {
    this.confidence = confidence;
  }

  /**
   * A string to override this instance's ToString when non-null.
   */
  public String getToStringOverride() {
    return toStringOverride;
  }

  /**
   * A string to override this instance's ToString when non-null.
   */
  public void setToStringOverride(String toStringOverride) {
    this.toStringOverride = toStringOverride;
  }

  /**
   * Mappings from categories to their interpreted values for components
   * of the interpretation.
   */
  public Map<String, Object> getCategory2Value() {
    return category2Value;
  }

  /**
   * Get the category mapping or null.
   */
  public Object get(String category) {
    Object result = null;

    if (category2Value != null) {
      result = category2Value.get(category);
    }

    return result;
  }

  /**
   * Add a mapping to this instance.
   * <p>
   * NOTE: If this interpretation is to be persisted, the Object must be serializable.
   */
  public void add(String category, Object value) {
    if (category2Value == null) category2Value = new HashMap<String, Object>();
    category2Value.put(category, value);
  }


  public String toString() {
    String result = toStringOverride;

    if (result == null) {
      if (interpretation != null) {
        result = interpretation.toString();
      }
      else {
        result = super.toString();
      }
    }

    return result;
  }

  public boolean equals(Object o) {
    boolean result = this == o;

    if (!result && o instanceof ParseInterpretation) {
      final ParseInterpretation other = (ParseInterpretation)o;

      result = this.classification.equals(other.getClassification()) &&
        this.interpretation.equals(other.getInterpretation());
    }

    return result;
  }

  public int hashCode() {
    int result = 13;

    result = 17 * result + this.classification.hashCode();
    if (this.interpretation != null) {
      result = 17 * result + this.interpretation.hashCode();
    }

    return result;
  }
}
