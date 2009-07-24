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


import java.util.HashMap;
import java.util.Map;

/**
 * A token for use in grammar rules.
 * <p>
 * @author Spence Koehler
 */
public class GrammarToken extends AbstractToken<String> {
  
  static final Map<String, GrammarToken> string2special = new HashMap<String, GrammarToken>();

  public static final GrammarToken ZERO_OR_MORE = new GrammarToken(SpecialToken.ZOM);
  public static final GrammarToken ONE_OR_MORE = new GrammarToken(SpecialToken.OOM);
  public static final GrammarToken OPTIONAL = new GrammarToken(SpecialToken.OPT);
  public static final GrammarToken END = new GrammarToken(SpecialToken.END);

  private String[] keys;

  private GrammarToken(SpecialToken type) {
    super(null, type);
    string2special.put(type.string, this);
  }

  /**
   * Package protected for factory creation through DefaultGrammarTokenFactory.
   */
  protected GrammarToken(String token) {
    super(token, null);
    this.keys = new String[]{token.toLowerCase()};
  }

  public boolean matches(AbstractToken other) {
    boolean result = false;
    if (other != null) {
      // let other types of tokens decide how they match the grammar.
      result = other.matches(this);
    }
    return result;
  }

  /**
   * Get this token's common (across all token classes) key.
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
