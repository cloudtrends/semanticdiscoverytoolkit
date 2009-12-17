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
package org.sd.xml.xel;


import java.util.List;
import org.sd.util.tree.Tree;
import org.sd.xml.XmlLite;

/**
 * An extraction instance, which can also serve as an extractor.
 * <p>
 * @author Spence Koehler
 */
public interface XelExtraction extends XelExtractor {
  
  /**
   * Get the key that identifies this extraction.
   */
  public String getKey();

  /**
   * Get this instance's extracted nodes (ignoring local exclusions).
   */
  public List<Tree<XmlLite.Data>> getNodes();

  /**
   * Get the attribute from the (non-excluded) extracted nodes.
   */
  public List<String> getAttribute(String attribute);

  /**
   * Get the text from the (non-excluded) extracted nodes.
   */
  public List<String> getText();

  /**
   * Get the extractor that produced this extraction.
   */
  public XelExtractor getExtractor();

  /**
   * Re-cast this extraction's nodes as XelExtractors with each node
   * as the root node.
   */
  public List<XelExtractor> asExtractors();
}
