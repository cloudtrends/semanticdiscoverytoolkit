/*
    Copyright 2011 Semantic Discovery, Inc. (www.semanticdiscovery.com)

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


import org.sd.util.range.IntegerRange;

/**
 * Container for rule step 'require' or 'unless' data.
 * <p>
 * @author Spence Koehler
 */
public class StepRequirement {

  /**
   * Build StepRequirement instances from the vertical-bar-delimited value.
   */
  public static final StepRequirement[] buildInstances(String value) {
    if (value == null) return null;

    final String[] pieces = value.split("\\s*\\|\\s*");
    final StepRequirement[] result = new StepRequirement[pieces.length];

    for (int i = 0; i < pieces.length; ++i) {
      result[i] = new StepRequirement(pieces[i]);
    }

    return result;
  }


  private IntegerRange levelRange;
  private String category;

  /**
   * Instantiate by parsing value of form C[:R], where C is a category
   * label and the optional R is an integer range expression, defaulting
   * to "-0", which means to apply the category requirement from -infinity
   * to 0, which means to apply the category requirement to the current rule's
   * level (0) and those deeper constituents (-N) that have been "pushed" from
   * the current rule.
   */
  public StepRequirement(String value) {
    this(value, "-0");
  }

  public StepRequirement(String value, String defaultLevelRange) {
    final String[] pieces = value.split("\\s*:\\s*");
    this.category = pieces[0];
    this.levelRange = null;

    if (pieces.length == 1) {
      this.levelRange = defaultLevelRange == null ? null : new IntegerRange(defaultLevelRange);
    }
    else {
      this.levelRange = new IntegerRange(pieces[1]);
    }
  }
  
  public IntegerRange getLevelRange() {
    return levelRange;
  }

  public void setLevelRange(IntegerRange levelRange) {
    this.levelRange = levelRange;
  }

  public String getCategory() {
    return category;
  }

  public void setCategory(String category) {
    this.category = category;
  }

  /**
   * Check the given category and level for match against this requirement.
   * <p>
   * If level (or levelRange) is null, then ignore any level restrictions;
   * otherwise enforce level against the levelRange.
   */
  public boolean matches(String category, Integer level) {
    return matches(category, null, level);
  }

  /**
   * Check the given category and level for match against this requirement.
   * <p>
   * If level (or levelRange) is null, then ignore any level restrictions;
   * otherwise enforce level against the levelRange.
   */
  public boolean matches(String category, String altLabel, Integer level) {
    boolean result = category.equals(this.category);

    if (!result && altLabel != null && altLabel != category) {
      result = altLabel.equals(this.category);
    }

    if (result && level != null && levelRange != null) {
      result = levelRange.includes(level);
    }

    return result;
  }
}
