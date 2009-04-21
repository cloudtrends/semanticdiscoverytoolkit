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
import org.sd.util.tree.SimpleTreeBuilderStrategy;

/**
 * A tree builder strategy for a state tree.
 * <p>
 * @author Spence Koehler
 */
public class StateTreeBuilderStrategy extends SimpleTreeBuilderStrategy<Token> {
  
  public static final StateTreeBuilderStrategy INSTANCE = new StateTreeBuilderStrategy();

  static StateTreeBuilderStrategy getInstance() {
    return INSTANCE;
  }
  
  private StateTreeBuilderStrategy() {
  }

  /**
   * Construct core node data from its string representation.
   * <p>
   * @param coreDataString The string form of the core node data.
   * <p>
   * @return the core node data.
   */
  public Token constructCoreNodeData(String coreDataString) {
    return new GrammarToken(coreDataString);
  }
}
