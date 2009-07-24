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
 * Wrapper class for the synonym type enumeration.
 * <p>
 * @author Spence Koehler
 */
public class Synonym {
  
  public enum Type {

    PRIMARY(1),  // form of the term text itself (without acronyms, with conjunctions)
    TABLE(1),    // looked up from a synonym table
    ACRONYM(1),  // term text using acronym(s)
    CONJ(1),     // form of part of the term text between conjunctions (and, with, ...)
    DISJ(0),     // form of part of the term text between disjunctions (or)

    //
    // NOTE: Always add more types before this. (Used for testing/integrity purposes.)
    //
    //   *** REMEMBER to always add new types to the set below!
    //
    _FINAL_(0);

    private final int weightMultiplier;

    Type(int weightMultiplier) {
      this.weightMultiplier = weightMultiplier;
    }

    public int getWeightMultiplier() {
      return weightMultiplier;
    }
  };

  private static final Set<Type> types = new LinkedHashSet<Type>();
  private static final Map<Integer, Type> ordinal2type = new LinkedHashMap<Integer, Type>();
  private static final Map<String, Type> name2type = new LinkedHashMap<String, Type>();

  static {
    types.add(Type.PRIMARY);
    types.add(Type.TABLE);
    types.add(Type.ACRONYM);
    types.add(Type.CONJ);
    types.add(Type.DISJ);
    
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
