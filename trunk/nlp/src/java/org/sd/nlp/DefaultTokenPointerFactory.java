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
 * Default token pointer factory creates "longest" tokens that "shrink" when
 * revised.
 * <p>
 * @author Spence Koehler
 */
public class DefaultTokenPointerFactory implements TokenPointerFactory {
  
  protected final Lexicon lexicon;
  private final boolean dontShrinkDefined;

  /**
   * Package protected to enforce creating through TokenizationStrategy.
   */
  DefaultTokenPointerFactory(Lexicon lexicon) {
    this(lexicon, false);
  }

  /**
   * Package protected to enforce creating through TokenizationStrategy.
   */
  DefaultTokenPointerFactory(Lexicon lexicon, boolean dontShrinkDefined) {
    this.lexicon = lexicon;
    this.dontShrinkDefined = dontShrinkDefined;
  }

  protected TokenPointer makeTokenPointer(StringWrapper.SubString subString, int skipUpTo) {
    return new StringWrapperTokenPointer(lexicon, subString, skipUpTo, dontShrinkDefined);
  }

  public TokenPointer getFirstPointer(StringWrapper stringWrapper, int skipUpTo) {
    TokenPointer result = null;
    final StringWrapper.SubString firstSubString = stringWrapper.getLongestSubString(0, lexicon.maxNumWords());
    if (firstSubString != null) {
      result = makeTokenPointer(firstSubString, skipUpTo);
    }
    return result;
  }

}
