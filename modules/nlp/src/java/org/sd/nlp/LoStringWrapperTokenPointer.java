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
 * Extension of StringWrapperTokenPointer thta uses a different strategy fo
 * revising tokens.
 * <p>
 * This implementation only tries the longest tokens.
 *
 * @author Spence Koehler
 */
public class LoStringWrapperTokenPointer extends StringWrapperTokenPointer {
  
  LoStringWrapperTokenPointer(Lexicon lexicon, StringWrapper.SubString subString, int skipUpTo) {
    super(lexicon, subString, skipUpTo);
  }

  protected final TokenPointer buildRevisedTokenPointer(StringWrapper.SubString subString) {
    return null;
  }

  /**
   * Get another token pointer based on shortening or lengthening the
   * referenced token's size. This revision strategy must be correlated
   * with the "next" strategy.
   */
  protected final StringWrapper.SubString doRevising(StringWrapper.SubString subString) {
    return null;
  }

  protected StringWrapper.SubString doGetNext(StringWrapper.SubString subString) {
    return subString.getNextLongestSubString(getSkipUpTo());
  }

  protected final TokenPointer buildNextTokenPointer(StringWrapper.SubString subString, int newSkipLimit) {
    return new LoStringWrapperTokenPointer(getLexicon(), subString, newSkipLimit);
  }
}
