/*
    Copyright 2010 Semantic Discovery, Inc. (www.semanticdiscovery.com)

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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.sd.token.CategorizedToken;
import org.sd.util.tree.Tree;
import org.sd.xml.DomNode;
import org.sd.xml.XmlLite;

/**
 * An AtnParseInterpreter that uses the parse itself as the interpretation.
 * <p>
 * @author Spence Koehler
 */
public class IdentityParseInterpreter implements AtnParseInterpreter {
  
  private boolean compress;

  public IdentityParseInterpreter(DomNode domNode, ResourceManager resourceManager) {
    this.compress = domNode.getAttributeBoolean("compress", false);
  }

  public IdentityParseInterpreter(boolean compress) {
    this.compress = compress;
  }

  /**
   * Get classifications offered by this interpreter.
   * 
   * Note that classifications are applied to parse-based tokens. Access
   * to the potential classifications is intended to help with monitoring,
   * introspection, and other high-level tools for building grammars and
   * parsers.
   */
  public String[] getClassifications() {
    return new String[]{"identity"};
  }

  /**
   * Get the interpretations for the parse or null.
   */
  public List<ParseInterpretation> getInterpretations(AtnParse parse) {
    final List<ParseInterpretation> result = new ArrayList<ParseInterpretation>();
    final Tree<XmlLite.Data> interpTree = asInterpTree(parse.getParseTree(), null);
    result.add(new ParseInterpretation(interpTree));
    return result;
  }

  private final Tree<XmlLite.Data> asInterpTree(Tree<String> parseTree, Tree<XmlLite.Data> parent) {
    Set<Tree<String>> compressNodes = null;

    if (compress) {
      compressNodes = getCompressNodes(parseTree);
    }

    final Tree<XmlLite.Data> result = convertTree(parseTree, parent, compressNodes);
    return result;
  }

  private final Tree<XmlLite.Data> convertTree(Tree<String> parseTree, Tree<XmlLite.Data> parent, Set<Tree<String>> compressNodes) {

    boolean isTag = parseTree.hasChildren();
    Tree<XmlLite.Data> curInterpNode = null;

    if (isTag) {
      curInterpNode = XmlLite.createTagNode(parseTree.getData());

      if (compressNodes != null && compressNodes.contains(parseTree)) {
        if (parent != null) {
          parent.addChild(curInterpNode);
        }
        parent = curInterpNode;
        isTag = false;
      }
    }

    if (!isTag) {
      final CategorizedToken cToken = ParseInterpretationUtil.getCategorizedToken(parseTree);

      String text = null;
      if (cToken != null) {
        text = cToken.token.getText();
      }
      else {
        text = parseTree.getData();
      }

      curInterpNode = XmlLite.createTextNode(text);
    }

    // construct tree
    if (parent != null) {
      parent.addChild(curInterpNode);
    }

    // recurse
    if (isTag) {
      for (Tree<String> childTree : parseTree.getChildren()) {
        convertTree(childTree, curInterpNode, compressNodes);
      }
    }

    return curInterpNode;
  }

  private final Set<Tree<String>> getCompressNodes(Tree<String> parseTree) {
    final Set<Tree<String>> result = new HashSet<Tree<String>>();
    final Set<Tree<String>> gpNodes = new HashSet<Tree<String>>();

    // collect nodes 2 above the leaves
    final List<Tree<String>> leaves = parseTree.gatherLeaves();
    for (Tree<String> leaf : leaves) {
      final Tree<String> parent = leaf.getParent();
      if (parent != null) {
        final Tree<String> gparent = parent.getParent();
        if (gparent != null) {
          gpNodes.add(gparent);
        }
      }
    }

    // ascend nodes
    for (Tree<String> gpNode : gpNodes) {
      if (gpNode.equidepth()) {
        result.add(gpNode.ascend());
      }
    }

    return result;
  }
}
