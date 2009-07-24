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


import org.sd.fsm.Rule;

/**
 * A rule consists of a left hand side (lhs) and left hand side (lhs).
 * <p>
 * The lhs defines a token that can be substituted for the rhs tokens.
 * <p>
 * The rhs defines a sequence of tokens; some of which are "special".
 * <p>
 * Special rhs tokens are:<br>
 *    x + == the token, x, can be repeated one or more times to match the rule.<br>
 *    x * == the token, x, can be repeated zero or more times to match the rule,<br>
 *  ( x ) == the token, x, can be optional to match the rule.<br>
 *
 * @author Spence Koehler
 */
public class RuleImpl implements Rule {

  private GrammarToken lhs;
  private GrammarToken[] rhs;

  public RuleImpl(GrammarToken lhs, GrammarToken[] rhs) {
    this.lhs = lhs;
    this.rhs = rhs;
  }
  
  public GrammarToken getLHS() {
    return lhs;
  }

  public int getNumTokens() {
    return rhs.length;
  }

  public GrammarToken getRHS(int position) {
    return position < 0 ? null : position < rhs.length ? rhs[position] : null;
  }

  public String toString() {
    final StringBuilder result = new StringBuilder();

    result.append(lhs).append(" <-");
    for (GrammarToken token : rhs) {
      result.append(' ').append(token);
    }

    return result.toString();
  }

  /**
   * Get the first non-special token position before the given position.
   *
   * @return the previous token position or an invalid position.
   */
  public int getPrevTokenPosition(int position) {
    GrammarToken token = null;
    while ((token == null || token.isSpecial()) && (position > 0) && ((position - 1) < rhs.length)) {
      token = rhs[--position];
    }
    return position;
  }

  /**
   * Get the next non-special token position after the given position.
   *
   * @return the next token position or an invalid position.
   */
  public int getNextTokenPosition(int position) {
    GrammarToken token = null;
    while ((token == null || token.isSpecial()) && ((position + 1) < rhs.length)) {
      token = rhs[++position];
    }
    return position;
  }

  /**
   * Query whether the position is valid for this rule.
   */
  public boolean isValid(int position) {
    return position >= 0 && position < rhs.length;
  }

  /**
   * Query whether the position is terminal for this rule.
   */
  public boolean isTerminal(int position) {
    // is terminal if we're at the end or we can spin to an end past optionals
    if (position == rhs.length) return true;

    GrammarToken thisToken = getRHS(position);
    if (thisToken == GrammarToken.END) return true;

    while (thisToken.isSpecial()) {
      // move forward to an end or non-special
      thisToken = getRHS(++position);
      if (thisToken == null || thisToken == GrammarToken.END) return true;
    }

    // spin beyond optionals
    int nextPos = position;
    while (isOptional(nextPos)) {
      nextPos += 2;
      thisToken = getRHS(nextPos);
      if (thisToken == null || thisToken == GrammarToken.END) return true;
      if (thisToken.isSpecial()) break;
    }

    return false;
  }

  public final boolean isOptional(int position) {
    GrammarToken nextToken = getRHS(position + 1);
    return nextToken == GrammarToken.OPTIONAL || nextToken == GrammarToken.ZERO_OR_MORE;
  }
}
