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


import org.sd.util.LRU;

/**
 * A factory for creating and caching (StringWrapper-based) lexical tokenizers.
 * <p>
 * @author Spence Koehler
 */
public class LexicalTokenizerFactory {

  private TokenPointerFactory tokenPointerFactory;
  private int skipUpTo;

  private LRU<String, StringWrapperLexicalTokenizer> cache;

  public LexicalTokenizerFactory(TokenPointerFactory tokenPointerFactory, int skipUpTo) {
    this.tokenPointerFactory = tokenPointerFactory;
    this.skipUpTo = skipUpTo;
    this.cache = new LRU<String, StringWrapperLexicalTokenizer>(100);
  }

  public StringWrapperLexicalTokenizer getLexicalTokenizer(StringWrapper stringWrapper) {
    final String string = stringWrapper.string;
    StringWrapperLexicalTokenizer result = cache.get(string);
    if (result == null) {
      result = new StringWrapperLexicalTokenizer(stringWrapper, tokenPointerFactory, skipUpTo);
      cache.put(string, result);
    }
    return result;
  }
}
