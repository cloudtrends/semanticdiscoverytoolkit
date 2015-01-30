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
package org.sd.token;


import org.sd.util.InputContext;

/**
 * Interface for tokenizing text.
 * <p>
 * @author Spence Koehler
 */
public interface Tokenizer {
  
  /**
   * Get the token that starts at the given position. To get the first
   * token, use GetToken(0).
   *
   * @returns The token at the positioni or null.
   */
  public Token getToken(int startPosition);

  /**
   * Get the smallest token after the given token.
   */
  public Token getNextSmallestToken(Token token);

  /**
   * Get the smallest token that starts at the given position.
   */
  public Token getSmallestToken(int startPosition);

  /**
   * Get the delimiter text immediately following the token.
   *
   * @returns A non-null but possibly empty string.
   */
  public String getPostDelim(Token token);

  /**
   * Get the delimiter text preceding the given token.
   *
   * @returns A non-null but possibly empty string.
   */
  public String getPreDelim(Token token);

  /**
   * Determine whether the token follows a hard break.
   * <p>
   * A token follows a hard break if there is a hard break among the token's
   * preDelim characters. Therefore, the first token of a string is *not*
   * considered to follow a hard break.
   */
  public boolean followsHardBreak(Token token);

  /**
   * Revise the token if possible.
   *
   * @returns A revised token or null.
   */
  public Token revise(Token token);

  /**
   * Broaden the token's start position to an already established token with
   * the same end but an earlier start, if possible.
   */
  public Token broadenStart(Token token);

  /**
   * Get the next token after the given token if possible.
   *
   * @returns The next token or null.
   */
  public Token getNextToken(Token token);

  /**
   * Get the token preceding the given token if possible.
   *
   * @returns The prior token or null.
   */
  public Token getPriorToken(Token token);

  /**
   * Get the full text being tokenized.
   *
   * @returns The full text, possibly empty but not null.
   */
  public String getText();

  /**
   * Get the number of 'words' being tokenized by this instance.
   */
  public int getWordCount();

  /**
   * Count the number of words encompassed from the start of the startToken
   * to the end of the endToken.
   */
  public int computeWordCount(Token startToken, Token endToken);

  /**
   * Get the full text after the given token if possible.
   *
   * @returns The full following text, possibly empty but not null.
   */
  public String getNextText(Token token);

  /**
   * Get the full text preceding the token.
   *
   * @returns The full prior text, possibly empty but not null.
   */
  public String getPriorText(Token token);

  /**
   * Get the input context associated with this tokenizer's input or null.
   */
  public InputContext getInputContext();

  /**
   * Build a token for the identified substring from startPosition (inclusive)
   * to endPosition (exclusive). Intended for expert use only.
   * <p>
   * NOTE: This is an atypical way to create a token as it bypasses the normal
   *       sequencing but is provided for those rare cases where a specific
   *       portion of the text is required as a token. The built token's sequence
   *       number will be -1.
   * <p>
   * @return the token or null if the positions are out of range.
   */
  public Token buildToken(int startPosition, int endPosition);

  /**
   * Split the text from start to end position into words based on breaks.
   */
  public String[] getWords(int startPosition, int endPosition);

  /**
   * Determine whether this instance is still initializing.
   */
  public boolean initializing();
}
