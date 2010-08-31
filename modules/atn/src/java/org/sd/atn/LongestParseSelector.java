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
package org.sd.atn;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.sd.util.tree.Tree;
import org.sd.xml.DomElement;
import org.sd.xml.DomNode;

/**
 * AtnParseSelector that chooses the longest parse.
 * <p>
 * @author Spence Koehler
 */
public class LongestParseSelector implements AtnParseSelector {
  
  private boolean simplest;

  /**
   * Attribute 'simplest' (default 'true') accepts only longest parses with
   * the fewest number of nodes in their parse tree.
   */
  public LongestParseSelector(DomNode domNode, ResourceManager resourceManager) {
    this.simplest = ((DomElement)domNode).getAttributeBoolean("simplest", true);
  }

  public List<AtnParse> selectParses(AtnParseResult parseResult) {
    final List<AtnParse> result = new ArrayList<AtnParse>();

    final Map<Integer, Integer> parseNum2Len = new HashMap<Integer, Integer>();

    String note = "Not longest";

    // find the maxLen of all parses
    int maxLen = 0;
    for (int parseIndex = 0; parseIndex < parseResult.getNumParses(); ++parseIndex) {
      final AtnParse parse = parseResult.getParse(parseIndex);
      if (!parse.getSelected()) continue;

      final int curLen = parse.getEndIndex() - parse.getStartIndex();
      if (curLen > maxLen) maxLen = curLen;

      parseNum2Len.put(parseIndex, curLen);
    }

    // find the min complexity (num parse tree nodes) of all longest parses
    final Map<Integer, Integer> parseNum2NumNodes = simplest ? new HashMap<Integer, Integer>() : null;
    int minNodes = 0;
    if (simplest) {
      for (int parseIndex = 0; parseIndex < parseResult.getNumParses(); ++parseIndex) {
        final int curLen = parseNum2Len.get(parseIndex);

        if (curLen == maxLen) {
          final AtnParse parse = parseResult.getParse(parseIndex);
          final int numNodes = countParseTreeNodes(parse.getParseTree());

          if (minNodes == 0 || numNodes < minNodes) {
            minNodes = numNodes;
          }

          parseNum2NumNodes.put(parseIndex, numNodes);
        }
      }
    }

    // find parses with longest length and (optionally) min complexity
    for (int parseIndex = 0; parseIndex < parseResult.getNumParses(); ++parseIndex) {
      boolean select = false;
      final int curLen = parseNum2Len.get(parseIndex);
      final AtnParse parse = parseResult.getParse(parseIndex);

      if (curLen == maxLen) {
        if (parseNum2NumNodes == null || parseNum2NumNodes.get(parseIndex) == minNodes) {
          select = true;
        }
      }

      if (select && isDuplicate(parse, result)) {
        note = "Is duplicate";
        select = false;
      }

      parse.setSelected(select);
      if (select) result.add(parse);
      else parse.addNote(note);
    }

    return result;
  }

  private final int countParseTreeNodes(Tree<String> parseTree) {
    int result = 0;

    for (Iterator<Tree<String>> iter = parseTree.iterator(Tree.Traversal.DEPTH_FIRST); iter.hasNext(); ) {
      final Tree<String> curNode = iter.next();
      ++result;
    }

    return result;
  }

  private final boolean isDuplicate(AtnParse parse, List<AtnParse> parses) {
    boolean result = false;

    final Tree<String> parseTree = parse.getParseTree();

    for (AtnParse curParse : parses) {
      final Tree<String> curParseTree = curParse.getParseTree();
      if (parseTree.equals(curParseTree)) {
        result = true;
        break;
      }
    }

    return result;
  }
}
