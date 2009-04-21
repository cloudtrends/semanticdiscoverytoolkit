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
package org.sd.wn;


/**
 * A pointer filter that only accepts expansions to senses that are consistent
 * with their sources. That is, only expand to the same part of speech and lexName.
 * Extenders can further constrain or specify which types of pointers to expand.
 * <p>
 * @author Spence Koehler
 */
public class ConsistentPointerFilter extends BasePointerFilter {
  
  public ConsistentPointerFilter() {
    super();
  }

  public ConsistentPointerFilter(boolean rejectReverseRelations, Integer maxDepth,
                                 PointerSymbol[] acceptPointers, PointerSymbol[] rejectPointers) {
    super(rejectReverseRelations, maxDepth, acceptPointers, rejectPointers);
  }

  /**
   * Determine whether the part of speech should be accepted for expansion.
   * <p>
   * This will accept a part of speech if it matches that of the source.
   */
  protected boolean acceptPOS(POS partOfSpeech, WordSenseWrapper source) {
    return partOfSpeech == source.getWordSense().getPartOfSpeech();
  }

  /**
   * Determine whether the lex name should be accepted for expansion.
   * <p>
   * Extending classes can override this for desired behavior.
   */
  public boolean acceptLexName(LexName lexName, WordSenseWrapper source) {
    return lexName == source.getWordSense().getFileEntry().getLexName();
  }
}
