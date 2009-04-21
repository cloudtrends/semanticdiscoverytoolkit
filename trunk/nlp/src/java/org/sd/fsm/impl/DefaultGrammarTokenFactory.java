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
 * Default GrammarTokenFactory for creating GrammarToken instances.
 * <p>
 * @author Spence Koehler
 */
public class DefaultGrammarTokenFactory implements GrammarTokenFactory {
  
  private static final DefaultGrammarTokenFactory INSTANCE = new DefaultGrammarTokenFactory();

  public static DefaultGrammarTokenFactory getInstance() {
    return INSTANCE;
  }

  /**
   * Private constructor to enforce singleton pattern.
   */
  private DefaultGrammarTokenFactory() {
  }

  public GrammarToken getSpecialGrammarToken(String string) {
    return GrammarToken.string2special.get(string);
  }

  public GrammarToken getGrammarToken(String string) {
    GrammarToken result = GrammarToken.string2special.get(string);
    if (result == null) {
      result = new GrammarToken(string);
    }
    return result;
  }
}
