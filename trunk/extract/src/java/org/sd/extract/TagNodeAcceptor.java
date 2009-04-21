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
package org.sd.extract;


import org.sd.util.tree.Tree;
import org.sd.xml.XmlLite;

/**
 * Interface for accepting a node with an extracted tag.
 * <p>
 * @author Spence Koehler
 */
public interface TagNodeAcceptor {

  /**
   * Given that the node has a tag whose name has been identified and extracted,
   * apply any further criterea to decide whether to accept the node.
   * <p>
   * This interface is used by the TagExtractor class, which will ensure
   * that nodes passed to this interface are nodes that already have a tag
   * name identified for extraction. It ensures this either by finding the
   * tags on the tag stack and building a sparse xml tree from the tag stack
   * and xml node, or by finding nodes with the tag name within the existing
   * xml tree found from a docText.
   */
  public boolean acceptTagNode(Tree<XmlLite.Data> tagNode);

  
//todo: return a confidence value instead of (or in addition to) a boolean.
}
