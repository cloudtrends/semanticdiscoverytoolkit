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
package org.sd.text.lucene;


import org.sd.nlp.NormalizedString;
import org.sd.nlp.Normalizer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.lucene.analysis.Token;
import org.apache.lucene.analysis.TokenStream;

/**
 * A token stream using a normalizer to tokenize.
 * <p>
 * @author Spence Koehler
 */
public class SdTokenStream extends TokenStream {

  private BufferedReader reader;
  private Normalizer normalizer;

  private NormalizedString nString;
  private NormalizedString.Token nextToken;

  private List<NormalizedString.Token> tokens;         // for reset
  private Iterator<NormalizedString.Token> tokenIter;  // for reset

  private boolean didFirst;

  public SdTokenStream(Reader reader, Normalizer normalizer) {
    this.reader = new BufferedReader(reader);
    this.normalizer = normalizer;
    this.nString = null;
    this.tokens = new ArrayList<NormalizedString.Token>();
    this.tokenIter = null;

    init();
  }

  public SdTokenStream(NormalizedString nString) {
    this.reader = null;
    this.normalizer = null;
    this.nString = nString;
    this.tokens = new ArrayList<NormalizedString.Token>();
    this.tokenIter = null;

    init();
  }
  
  /**
   * Set the reader (to re-use this instance.)
   */
  public void setReader(Reader reader) {
    if (reader != null) {
      try {
        reader.close();
      }
      catch (IOException e) {
        throw new IllegalStateException(e);
      }
    }

    this.reader = new BufferedReader(reader);
    this.nString = null;
    this.tokens.clear();
    this.tokenIter = null;

    init();
  }

  private final void init() {
    try {
      loadNextToken();
    }
    catch (IOException e) {
      throw new IllegalStateException(e);
    }
    didFirst = false;
  }

  private final void loadNextToken() throws IOException {

    if (nextToken != null) {
      nextToken = nextToken.getNext(true);
    }

    if (nextToken == null) {
      if (nString == null && reader != null) {
        String line = reader.readLine();
        for (; line != null; line = reader.readLine()) {
          nString = normalizer.normalize(line);
          this.nextToken = nString.getToken(0, true);
          if (nextToken != null) {
            nString = null;
            break;
          }
        }

        if (line == null) {
          close();
          reader = null;
        }
      }
      else if (nString != null) {
        this.nextToken = nString.getToken(0, true);
        nString = null;
      }
    }

    if (nextToken != null) tokens.add(nextToken);
  }

  public Token next(Token result) throws IOException {

    didFirst = true;

    if (tokenIter != null) {
      nextToken = tokenIter.hasNext() ? tokenIter.next() : null;
    }

    if (nextToken == null) return null;

    if (result == null) {
      result = new Token();
    }
    else {
      result.clear();
    }

    // set text (in the term buffer)
    final String text = nextToken.getNormalized();
    final char[] chars = text.toCharArray();

    char[] termBuffer = result.resizeTermBuffer(chars.length);

    for (int i = 0; i < chars.length; ++i) {
      termBuffer[i] = chars[i];
    }

    result.setTermLength(chars.length);

    // set other info
    result.setStartOffset(nextToken.getStartPos());
    result.setEndOffset(nextToken.getEndPos());
    //result.setPositionIncrement(int);
    //result.setType(String);

    if (tokenIter == null) loadNextToken();

    return result;
  }

  public void close() throws IOException {
    if (reader != null) {
      reader.close();
    }
  }

  public final void reset() throws IOException {
    if (didFirst) {
      this.tokenIter = tokens.iterator();
    }
  }
}
