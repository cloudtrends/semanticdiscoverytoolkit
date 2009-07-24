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


import org.sd.fsm.State;
import org.sd.fsm.StateDecoder;
import org.sd.fsm.Token;
import org.sd.util.tree.Tree;

import java.util.List;

/**
 * Decode a state by dumping its tokens into a tree.
 * <p>
 * @author Spence Koehler
 */
public abstract class AbstractStateDecoder implements StateDecoder {
  
  /**
   * Tree builder strategy.
   */
  protected static final StateTreeBuilderStrategy TBS = StateTreeBuilderStrategy.getInstance();

  protected abstract Token getLeaf(Token leafToken);

  public Tree<Token> getTree(State state) {
    return buildTree(StateUtil.getTop(state));
  }

  protected final Tree<Token> buildTree(State top) {
    Tree<Token> result = buildRoot(top);
    if (result != null) {
      final List<State> children = StateUtil.getStateChain(top);
      addChildren(children, result);
    }
    return result;
  }

  protected final Tree<Token> buildRoot(State state) {
    Tree<Token> result = null;
    final RuleImpl rule = (RuleImpl)state.getRule();
    if (rule != null) {
      result = TBS.constructNode(rule.getLHS());
    }
    return result;
  }

  private final void addChildren(List<State> children, Tree<Token> result) {
    for (State child : children) {
      final Tree<Token> node1 = buildNode1(child);
      if (node1 != null) TBS.addChild(result, node1);
      
      final Tree<Token> node2 = buildNode2(child);
      if (node2 != null) TBS.addChild(result, node2);
    }
  }

  private final Tree<Token> buildNode1(State state) {
    Tree<Token> result = null;

    // if token is non-null, then this state was reached by transitioning over
    // its matched grammar token.

    final Token curToken = state.getInputToken();
    if (curToken != null) {
      final Token token = state.getMatchedGrammarToken();
      result = TBS.constructNode(token);

      final Token leaf = getLeaf(curToken);
      if (leaf != null) {
        final Tree<Token> child = TBS.constructNode(leaf);
        TBS.addChild(result, child);
      }
    }

    return result;
  }

  private final Tree<Token> buildNode2(State state) {
    Tree<Token> result = null;

    // if there are children, then this state is the root of a push based on
    // its active grammar token.

    final State lastChild = state.getLastChildState();
    if (lastChild != null) {
      final Token token = state.getMatchedGrammarToken();
      result = TBS.constructNode(token);
      final List<State> children = StateUtil.getStateChain(lastChild);
      addChildren(children, result);
    }

    return result;
  }
}
