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
package org.sd.fsm.impl;


import org.sd.fsm.Token;

/**
 * A simple token represented as a string.
 * <p>
 * @author Spence Koehler
 */
public abstract class AbstractToken<T> implements Token {

  protected static enum SpecialToken {
    ZOM(1, "*"),  // zero or more
    OOM(2, "+"),  // one or more
    OPT(3, "?"),  // optional
    END(4, ".");  // plausible end

    int id;
    String string;

    SpecialToken(int id, String string) {
      this.id = id;
      this.string = string;
    }

    public String toString() {
      return string;
    }
  };

  private T token;
  private SpecialToken specialToken;

  protected AbstractToken(T token, SpecialToken specialToken) {
    this.token = token;
    this.specialToken = specialToken;
  }

  /**
   * Get this token's wrapped token object.
   */
  public T getToken() {
    return token;
  }

  /**
   * Query whether this token matches another.
   */
  public abstract boolean matches(AbstractToken other);

  /**
   * Determine whether this token can be "guessed" for undefined terms.
   */
  public abstract boolean isGuessable();

  /**
   * Get this token's common (across all token classes) key(s).
   */
  public abstract String[] getCommonKeys();

  public final boolean equals(Object other) {
    boolean result = (this == other);
    if (!result && other instanceof AbstractToken) {
      if (!isSpecial()) {
        result = token.equals(((AbstractToken)other).token);
      }
      // else this is special and should have been ==
    }
    return result;
  }

  public final int hashCode() {
    return (isSpecial()) ? specialToken.id : 17 + token.hashCode();
  }

  public final String toString() {
    return (isSpecial()) ? specialToken.toString() : token.toString();
  }

  public final boolean isSpecial() {
    return specialToken != null;
  }
}
