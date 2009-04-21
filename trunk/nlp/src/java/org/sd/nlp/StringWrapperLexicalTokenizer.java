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


import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Tokenizes words from a sentence that have definitions according to a lexicon.
 * <p>
 * If no definitions are found, the "smallest" word is returned undefined.
 * <p>
 * This implementation is based on using util.StringWrapper for tokenization.
 *
 * @author Spence Koehler
 */
public class StringWrapperLexicalTokenizer implements LexicalTokenizer {

  private StringWrapper stringWrapper;
  private TokenPointerFactory tokenPointerFactory;
  private int skipUpTo;

  private final Object buildFirstEntryMutex = new Object();
  private LexicalEntry _firstEntry;
  private boolean builtFirstEntry = false;

  public StringWrapperLexicalTokenizer(String string, TokenPointerFactory tokenPointerFactory, int skipUpTo) {
    this(new StringWrapper(string), tokenPointerFactory, skipUpTo);
  }

  public StringWrapperLexicalTokenizer(StringWrapper stringWrapper, TokenPointerFactory tokenPointerFactory, int skipUpTo) {
    this.stringWrapper = stringWrapper;
    this.tokenPointerFactory = tokenPointerFactory;
    this.skipUpTo = skipUpTo;

    this._firstEntry = null;
    this.builtFirstEntry = false;
  }

  public String getInputString() {
    return stringWrapper.string;
  }

  public LexicalEntry getFirstEntry() {
    synchronized (buildFirstEntryMutex) {
      if (!builtFirstEntry) {
        builtFirstEntry = true;
        final TokenPointer pointer = tokenPointerFactory.getFirstPointer(stringWrapper, skipUpTo);
        _firstEntry = (pointer != null) ? new LexicalEntry(pointer) : null;
      }
    }
    return _firstEntry;
  }
}
