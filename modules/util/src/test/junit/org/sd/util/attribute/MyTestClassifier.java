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
public class MyTestClassifier extends BaseAttributeClassifier<MyTestEnum> {
  
  private static final String[] MAKES = new String[] {
  };

  private static final String[] MODELS = new String[] {
    "type",
  };

  private static final String[] STYLES = new String[] {
    "type",
  };

  private static final String[] YEARS = new String[] {
  };



  private Set<String> makes;
  private Set<String> models;
  private Set<String> styles;
  private Set<String> years;

  public MyTestClassifier() {
    super(1000);

    this.makes = load(MAKES);
    this.models = load(MODELS);
    this.styles = load(STYLES);
    this.years = load(YEARS);
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

  protected MyTestEnum getValueOf(String upperCaseType) {
    return MyTestEnum.valueOf(upperCaseType.toUpperCase());
  }

  protected Attribute<MyTestEnum> classifyOtherAttribute(String type) {
    Attribute<MyTestEnum> result = null;

    type = type.toLowerCase();

    if (makes.contains(type)) result = addAtt(MyTestEnum.MAKE, result);
    if (models.contains(type)) result = addAtt(MyTestEnum.MODEL, result);
    if (styles.contains(type)) result = addAtt(MyTestEnum.STYLE, result);
    if (years.contains(type)) result = addAtt(MyTestEnum.YEAR, result);

    return result;
  }
}
