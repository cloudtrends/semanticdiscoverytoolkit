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
package org.sd.nlp;


/**
 * Convenience class for identifying tokenization strategies by enumerated
 * strategy type.
 * <p>
 * @author Spence Koehler
 */
public class TokenizationStrategy {
  
//todo: add more types as more strategies become necessary.
// to create a new strategy, create both a TokenPointerFactory and a
//   TokenPointer implementation (referenced from the factory) that defines
//   the strategy.

  public enum Type {LONGEST_TO_SHORTEST, LONGEST_TO_SHORTEST_TO_LONGEST, SHORTEST_ONLY, LONGEST_ONLY, LONGEST_DEFINED};

  public static TokenPointerFactory getStrategy(Type type, Lexicon lexicon) {
    TokenPointerFactory result = null;

    switch (type) {
      case LONGEST_TO_SHORTEST_TO_LONGEST :
        result = new LslTokenPointerFactory(lexicon);
        break;

      case SHORTEST_ONLY :
        result =  new SoTokenPointerFactory(lexicon);
        break;

      case LONGEST_ONLY :
        result =  new LoTokenPointerFactory(lexicon);
        break;

      case LONGEST_DEFINED :
        result = new DefaultTokenPointerFactory(lexicon, true);
        break;

      case LONGEST_TO_SHORTEST :
      default :
        result = new DefaultTokenPointerFactory(lexicon);
    }

    return result;
  }

  public static final Type getType(String name) {
    return Enum.valueOf(TokenizationStrategy.Type.class, name);
  }
}
