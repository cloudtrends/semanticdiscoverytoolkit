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


import java.io.IOException;
import java.io.Reader;
import org.apache.lucene.analysis.Token;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.TermAttribute;
import org.sd.io.BufferedStringReader;
import org.sd.nlp.NormalizedString;
import org.sd.nlp.Normalizer;


/**
 * A token stream using a normalizer to tokenize.
 * <p>
 * @author Spence Koehler
 */
public class SdTokenStream extends Tokenizer {

  private static final int IO_BUFFER_SIZE = 1024;

  private BufferedStringReader reader = new BufferedStringReader(IO_BUFFER_SIZE);
  private Normalizer normalizer;
  private int baseOffset;
  private NormalizedString.Token nextToken;
  private TermAttribute termAtt;
  private OffsetAttribute offsetAtt;


  public SdTokenStream(Reader in, Normalizer normalizer) {
    super(in);
    init(normalizer);
  }

  private final void init(Normalizer normalizer) {
    this.normalizer = normalizer;
    this.baseOffset = 0;
    this.nextToken = null;
    this.termAtt = (TermAttribute)addAttribute(TermAttribute.class);
    this.offsetAtt = (OffsetAttribute)addAttribute(OffsetAttribute.class);
  }

  public boolean incrementToken() throws IOException {
    clearAttributes();

    boolean result = false;

    if (nextToken != null) {
      nextToken = nextToken.getNext(true);
    }

    if (nextToken == null) {  // time to read (more)

      // skip past any new lines
      if (reader.readWhile(input, '\n') != null) {

        // read up to the next new line
        baseOffset = reader.getCharOffset();
        final String string = reader.readUntil(input, '\n');

        if (string != null && !"".equals(string)) {
          final NormalizedString nstring = normalizer.normalize(string);
          nextToken = nstring.getToken(0, true);
        }
      }
    }

    // fill in attribute info
    if (nextToken != null) {
      final char[] tokenChars = nextToken.getNormalizedString().getNormalizedChars();
      final int startPos = nextToken.getStartPos();
      final int endPos = nextToken.getEndPos();
      termAtt.setTermBuffer(tokenChars, startPos, endPos - startPos);

      offsetAtt.setOffset(baseOffset + startPos, baseOffset + endPos);
      result = true;
    }

    return result;
  }


  public final void end() {
    // set final offset
    final int finalOffset = baseOffset + reader.getCharOffset();
    this.offsetAtt.setOffset(finalOffset, finalOffset);
  }
    
  public void reset() throws IOException {
    super.reset();
    input.reset();
    reader.reset();
    this.baseOffset = 0;
    this.nextToken = null;
  }
    
  public void reset(Reader reader) throws IOException {
    super.reset(reader);
    reset();
  }
}
