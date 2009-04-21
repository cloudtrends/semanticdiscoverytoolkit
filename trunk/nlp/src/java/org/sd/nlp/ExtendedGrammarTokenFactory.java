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


import org.sd.fsm.impl.DefaultGrammarTokenFactory;
import org.sd.fsm.impl.GrammarToken;
import org.sd.fsm.impl.GrammarTokenFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * GrammarTokenFactory for creating ExtendedGrammarToken instances.
 *
 * @author Spence Koehler
 */
public class ExtendedGrammarTokenFactory implements GrammarTokenFactory {
  
  private static final Map<CategoryFactory, ExtendedGrammarTokenFactory> INSTANCES = new HashMap<CategoryFactory, ExtendedGrammarTokenFactory>();
  private static final DefaultGrammarTokenFactory DGTF = DefaultGrammarTokenFactory.getInstance();

  public static ExtendedGrammarTokenFactory getInstance(CategoryFactory categoryFactory) {
    ExtendedGrammarTokenFactory result = INSTANCES.get(categoryFactory);
    if (result == null) {
      result = new ExtendedGrammarTokenFactory(categoryFactory);
      INSTANCES.put(categoryFactory, result);
    }
    return result;
  }

  private CategoryFactory categoryFactory;

  /**
   * Private constructor to enforce singleton pattern.
   */
  private ExtendedGrammarTokenFactory(CategoryFactory categoryFactory) {
    this.categoryFactory = categoryFactory;
  }

  /**
   * Create a grammar token from the given string.
   */
  public GrammarToken getGrammarToken(String string) {
    GrammarToken result = DGTF.getSpecialGrammarToken(string);
    if (result == null) {
      result = new ExtendedGrammarToken(categoryFactory, string);
    }
    return result;
  }
}
