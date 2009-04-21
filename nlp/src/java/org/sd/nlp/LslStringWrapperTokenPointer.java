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
 * Extension of StringWrapperTokenPointer that uses a different strategy for
 * revising tokens.
 * <p>
 * This implementation first tries the longest substring, then the shortest,
 * working back up to the longest. (lsl == longest, shortest to longest).
 *
 * @author Spence Koehler
 */
public class LslStringWrapperTokenPointer extends StringWrapperTokenPointer {

  private enum Phase {LONGEST, GROWING};

  private Phase phase;
  private StringWrapper.SubString longest;

  LslStringWrapperTokenPointer(Lexicon lexicon, StringWrapper.SubString subString, int skipUpTo) {
    this(lexicon, subString, Phase.LONGEST, skipUpTo);

    this.longest = subString;
  }

  private LslStringWrapperTokenPointer(Lexicon lexicon, StringWrapper.SubString subString, Phase phase, int skipUpTo) {
    super(lexicon, subString, skipUpTo);
    this.phase = phase;
    this.longest = null;
  }

  protected final TokenPointer buildRevisedTokenPointer(StringWrapper.SubString subString) {
    return new LslStringWrapperTokenPointer(getLexicon(), subString, Phase.GROWING, 0);
  }

  protected final TokenPointer buildNextTokenPointer(StringWrapper.SubString subString, int newSkipLimit) {
    return new LslStringWrapperTokenPointer(getLexicon(), subString, Phase.LONGEST, newSkipLimit);
  }

  /**
   * Get another token pointer based on shortening or lengthening the
   * referenced token's size. This revision strategy must be correlated
   * with the "next" strategy.
   */
  protected final StringWrapper.SubString doRevising(StringWrapper.SubString subString, int maxNumWords) {
    return doRevising(subString, phase, maxNumWords);
  }

  private final StringWrapper.SubString doRevising(StringWrapper.SubString subString, Phase phase, int maxNumWords) {
    StringWrapper.SubString revised = null;
    if (phase == Phase.LONGEST) {
      revised = subString.stringWrapper.getShortestSubString(subString.startPos);
      if (revised != null && revised.equals(subString)) revised = null;
    }
    else {
      // avoid repeating the longest or growing too long
      if (maxNumWords <= 0 || subString.getNumWords() < maxNumWords - 1) {
        revised = subString.getLongerSubString();
      }
    }
    return revised;
  }

  protected final StringWrapper.SubString doNarrowing(StringWrapper.SubString subString, int maxNumWords) {
    StringWrapper.SubString smallest = subString;
    Phase phase = this.phase;
    while (subString != null) {
      final Categories categories = doLookup(subString);
      if (categories != null) break;

      subString = doRevising(subString, phase, maxNumWords);
      if (phase == Phase.LONGEST && subString != null) {
        phase = Phase.GROWING;
        smallest = subString;
      }
    }

    return (subString == null) ? (subString == smallest ? null : smallest) : subString;
  }
}
