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
package org.sd.match;


import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Wrapper class for the form type enumeration.
 * <p>
 * @author Spence Koehler
 */
public class Form {

  /**
   * An enumeration of form types, one of which will apply for each
   * concept term.
   */
  public enum Type {

    FULL_UK_DESC(1),
    FULL_US_DESC(1),
    ONTOLOGY_DECOMP(1),
    ALT_SEARCH_FORM(1),
    FULL_DUTCH_DESC(1),
    FULL_GERMAN_DESC(1),
    TAXONOMY_FORM(1),
    FOLKSONOMY_FORM(1),
    UK_DISPLAY(1),
    US_DISPLAY(1),

    //
    // NOTE: Always add more types before this. (Used for testing/integrity purposes.)
    //
    //   *** REMEMBER to always add new types to the set below!
    //
    _FINAL_(0);

    private int weightMultiplier;
    
    Type(int weightMultiplier) {
      this.weightMultiplier = weightMultiplier;
    }

    public int getWeightMultiplier() {
      return weightMultiplier;
    }

    public String getFormString() {
      return "F" + Integer.toString(ordinal());
    }
  };

  private static final Set<Type> types = new LinkedHashSet<Type>();
  private static final Map<Integer, Type> ordinal2type = new LinkedHashMap<Integer, Type>();
  private static final Map<String, Type> name2type = new LinkedHashMap<String, Type>();

  static {

    types.add(Type.FULL_UK_DESC);
    types.add(Type.FULL_US_DESC);
    types.add(Type.ONTOLOGY_DECOMP);
    types.add(Type.ALT_SEARCH_FORM);
    types.add(Type.FULL_DUTCH_DESC);
    types.add(Type.FULL_GERMAN_DESC);
    types.add(Type.TAXONOMY_FORM);
    types.add(Type.FOLKSONOMY_FORM);
    types.add(Type.UK_DISPLAY);
    types.add(Type.US_DISPLAY);

    for (Type type : types) {
      ordinal2type.put(type.ordinal(), type);
      name2type.put(type.name(), type);
    }
  }

  public static final Set<Type> getTypes() {
    return types;
  }

  public static final Type getType(int ordinal) {
    return ordinal2type.get(ordinal);
  }

  public static final Type getType(String name) {
    return name2type.get(name);
  }
}
