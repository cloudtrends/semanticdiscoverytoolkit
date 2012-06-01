/*
    Copyright 2011 Semantic Discovery, Inc. (www.semanticdiscovery.com)

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
package org.sd.atn.interp;


import java.util.List;
import org.sd.atn.Parse;
import org.sd.util.tree.NodePath;
import org.sd.util.tree.Tree;
import org.sd.xml.DomElement;

/**
 * NodeSelector for NodePath's.
 * <p>
 * @author Spence Koehler
 */
public class NodePathSelector implements NodeSelector {
  
  private NodePath<String> nodePath;
  private InnerResources resources;

  NodePathSelector(DomElement selectElement, InnerResources resources ) {
    //todo: decode attributes, none for now

    this.resources = resources;
    this.nodePath = new NodePath<String>(selectElement.getTextContent());
  }

  public List<Tree<String>> select(Parse parse, Tree<String> parseTreeNode) {
    return nodePath.apply(parseTreeNode);
  }
}
