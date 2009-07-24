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
 * Wrapper class for the word type enumeration.
 * <p>
 * @author Spence Koehler
 */
public class Word {
  
  public enum Type {

    NORMAL(2),      // normal
    ACRONYM(2),     // acronym form
    FUNCTIONAL(0),  // conjunctions, prepositions, etc. (contributes no weight)
    NUMERIC(1),     // numeric value (contributes half the weight of a normal word)
    COMPOUND_NO_SPACE(2 /*3?*/),    // compound term concatenated (with space removed)
    COMPOUND_WITH_SPACE(2),  // compound term, space delimited
    SYNONYM(2),     // word synonym

    //
    // NOTE: Always add more types before this. (Used for testing/integrity purposes.)
    //
    //   *** REMEMBER to always add new types to the set below!
    //
    _FINAL_(0);

    private int weight;  // weight this type of word contributes to scoring.

    Type(int weight) {
      this.weight = weight;
    }

    public int getWeight() {
      return weight;
    }
  };

  private static final Set<Type> types = new LinkedHashSet<Type>();
  private static final Map<Integer, Type> ordinal2type = new LinkedHashMap<Integer, Type>();
  private static final Map<String, Type> name2type = new LinkedHashMap<String, Type>();

  static {
    types.add(Type.NORMAL);
    types.add(Type.ACRONYM);
    types.add(Type.FUNCTIONAL);
    types.add(Type.NUMERIC);
    types.add(Type.COMPOUND_NO_SPACE);
    types.add(Type.COMPOUND_WITH_SPACE);
    types.add(Type.SYNONYM);
    
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
