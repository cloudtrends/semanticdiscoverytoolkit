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


import org.sd.fsm.impl.GrammarToken;

/**
 * A grammar token with extended functionality.
 * <p>
 * Extensions include the following:<br>
 *   <ul><li>negation (represented with '!' prefix)</li>
 *       <li>peek (represented with '&' prefix)</li>
 *       <li>literal (represented with '_')</li></ul>
 * <p>
 * A negated grammar token represents a term in the machine that "matches" when
 * the input token does not match.
 * <p>
 * A peek grammar token represents a term in the machine that is tested against
 * an input token, but leaves the token to be consumed by the next machine state
 * if the match succeeds.
 * <p>
 * NOTE: multiple extensions may be present for a single extended grammar token.
 *
 * @author Spence Koehler
 */
public class ExtendedGrammarToken extends GrammarToken {

  private static final char[] extensions = new char[]{'!', '&', '_', '?'};

  /**
   * Skip past extensions (if any) and return the raw token string.
   */
  static final String extractTokenString(String string) {
    // strip prefix(es)
    final int len = string.length();
    int pos = 0;
    while (pos < len) {
      final char curChar = string.charAt(pos);
      boolean isExtension = false;
      for (char extension : extensions) {
        if (extension == curChar) {
          isExtension = true;
          ++pos;
          break;
        }
      }
      if (!isExtension) break;
    }

    return (pos > 0) ? string.substring(pos) : string;
  }

  private boolean negated;
  private boolean peek;
  private boolean literal;
  private boolean guessable;
  private Category category;

  /**
   * Package protected to enforce factory creation through ExtendedGrammarTokenFactory.
   */
  ExtendedGrammarToken(CategoryFactory categoryFactory, String string) {
    super(extractTokenString(string));

    this.negated = false;
    this.peek = false;
    this.literal = false;
    this.guessable = false;
    this.category = null;

    setExtensions(categoryFactory, string);
  }

  /**
   * Set extension flags present in the string.
   */
  private final void setExtensions(CategoryFactory categoryFactory, String string) {
    final int len = string.length();
    boolean isCategory = true;
    int pos = 0;
    while (pos < len) {
      final char curChar = string.charAt(pos);
      boolean done = false;
      switch (curChar) {
        case '!' : this.negated = true; break;
        case '&' : this.peek = true; break;
        case '_' : this.literal = true; isCategory = false; break;
        case '?' : this.guessable = true; break;
        default : done = true;
      }
      if (done) break;
      ++pos;
    }
    if (isCategory) {
      final String name = string.substring(pos).toUpperCase();
      try {
        this.category = categoryFactory.getCategory(name);
      }
      catch (IllegalArgumentException e) {
        throw new IllegalArgumentException("can't find Category for '" + name + "' string=" + string + " pos=" + pos, e);
      }
    }
  }

  /**
   * Get this extended grammar token's category.
   *
   * @return the category, or null if it is literal.
   */
  public Category getCategory() {
    return category;
  }

  /**
   * Query whether this extended grammar token is negated.
   */
  public boolean isNegated() {
    return negated;
  }

  /**
   * Query whether this extended grammar token is peek.
   */
  public boolean isPeek() {
    return peek;
  }

  /**
   * Query whether this extended grammar token is literal.
   */
  public boolean isLiteral() {
    return literal;
  }

  /**
   * Determine whether this token can be "guessed" for undefined terms.
   */
  public boolean isGuessable() {
    return guessable || (category != null && category.canGuess());
  }
}
