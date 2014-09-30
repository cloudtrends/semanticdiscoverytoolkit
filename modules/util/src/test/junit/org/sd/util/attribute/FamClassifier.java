/*
    Copyright 2013 Semantic Discovery, Inc. (www.semanticdiscovery.com)

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
package org.sd.util.attribute;


import java.util.HashSet;
import java.util.Set;

/**
 * Simple classifier for package testing.
 * <p>
 * @author Spence Koehler
 */
public class FamClassifier extends BaseAttributeClassifier<FamEnum> {
  
  private static final String[] CHILDREN = new String[] {
    "child", "children",
  };


  private Set<String> children;

  public FamClassifier() {
    super(100);

    this.children = load(CHILDREN);
  }

  private final Set<String> load(String[] terms) {
    final Set<String> result = new HashSet<String>();

    if (terms != null) {
      for (String term : terms) {
        if (term != null && !"".equals(term)) {
          result.add(term.toLowerCase());
        }
      }
    }

    return result;
  }

  protected FamEnum getValueOf(String upperCaseType) {
    return FamEnum.valueOf(upperCaseType.toUpperCase());
  }

  protected Attribute<FamEnum> classifyOtherAttribute(String type) {
    Attribute<FamEnum> result = null;

    type = type.toLowerCase();

    if (children.contains(type)) result = addAtt(FamEnum.CHILD, result);

    return result;
  }
}
