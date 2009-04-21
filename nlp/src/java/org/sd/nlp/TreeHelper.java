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


import org.sd.fsm.Token;
import org.sd.util.tree.Tree;

import java.util.Iterator;
import java.util.List;

/**
 * Utilities for working with trees.
 * <p>
 * @author Spence Koehler
 */
public class TreeHelper {
  
  public static final String getString(Tree<Token> parseTree) {
    final List<Tree<Token>> leaves = parseTree.gatherLeaves();

    final StringBuilder result = new StringBuilder();

    for (Iterator<Tree<Token>> it = leaves.iterator(); it.hasNext(); ) {
      final Tree<Token> leaf = it.next();
      final LexicalToken lexicalToken = (LexicalToken)(leaf.getData());
      final LexicalEntry lexicalEntry = lexicalToken.getLexicalEntry();

      result.append(lexicalEntry.getString());
      if (it.hasNext()) {
        result.append(' ');
      }
    }

    return result.toString();
  }
}
