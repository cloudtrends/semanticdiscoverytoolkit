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


/**
 * A simple token represented as a string.
 * <p>
 * @author Spence Koehler
 */
public class StringToken extends AbstractToken<String> {

  private String[] keys;

  public StringToken(String token) {
    super(token, null);
    this.keys = new String[]{token.toLowerCase()};
  }

  public boolean matches(AbstractToken other) {
    boolean result = false;
    if (other != null && other instanceof GrammarToken) {
      final GrammarToken grammarToken = (GrammarToken)other;
      final String type = grammarToken.getToken();
      final String token = this.getToken();
      result = token.equals(type);
    }
    return result;
  }

  /**
   * Get this token's common (across all token classes) keys.
   */
  public String[] getCommonKeys() {
    return keys;
  }

  /**
   * Determine whether this token can be "guessed" for undefined terms.
   */
  public boolean isGuessable() {
    return false;
  }
}
