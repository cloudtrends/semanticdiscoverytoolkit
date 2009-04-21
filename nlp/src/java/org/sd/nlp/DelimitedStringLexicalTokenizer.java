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


import org.sd.util.DelimitedString;

/**
 * Tokenizes words from a sentence that have definitions according to a lexicon.
 * <p>
 * If no definitions are found, the "smallest" word is returned undefined.
 * <p>
 * This implementation is based on using util.DelimitedString for tokenization.
 *
 * @author Spence Koehler
 */
public class DelimitedStringLexicalTokenizer implements LexicalTokenizer {

  private String sentence;
  private DelimitedString dString;
  private Lexicon lexicon;
  private int skipUpTo;

  public DelimitedStringLexicalTokenizer(String sentence, Lexicon lexicon, int skipUpTo) {
    this.sentence = sentence;
    this.dString = new DelimitedString(sentence, null);
    this.lexicon = lexicon;
    this.skipUpTo = skipUpTo;
  }
  
  public String getInputString() {
    return sentence;
  }

  private TokenPointer getFirstPointer() {
    TokenPointer result = null;
    if (dString.numStrings() > 0) {
      final DelimitedString.ContinuousSegment firstSegment = dString.getSegment(0);
      if (firstSegment != null) result = new DelimitedStringTokenPointer(firstSegment, lexicon, skipUpTo);
    }
    return result;
  }

  public LexicalEntry getFirstEntry() {
    final TokenPointer pointer = getFirstPointer();
    return (pointer != null) ? new LexicalEntry(pointer) : null;
  }
}
