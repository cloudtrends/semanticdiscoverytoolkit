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


import org.sd.fsm.Grammar;
import org.sd.fsm.impl.GrammarImpl;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Utility to load a grammar.
 * <p>
 * @author Spence Koehler
 */
public class GrammarFactory {
  
  public static final Grammar loadGrammar(CategoryFactory categoryFactory, InputStream grammarInputStream) throws IOException {
    final Grammar result = GrammarImpl.loadGrammar(grammarInputStream, ExtendedGrammarTokenFactory.getInstance(categoryFactory), ParseStateDecoder.getInstance());
    grammarInputStream.close();
    return result;
  }

  public static final InputStream getGrammarInputStream(String[] ruleStrings) {
    final StringBuilder builder = new StringBuilder();
    for (String ruleString : ruleStrings) builder.append(ruleString).append('\n');
    return new ByteArrayInputStream(builder.toString().getBytes());
  }

  public static final Grammar loadGrammar(CategoryFactory categoryFactory, String[] ruleStrings) throws IOException {
    return loadGrammar(categoryFactory, getGrammarInputStream(ruleStrings));
  }
}
