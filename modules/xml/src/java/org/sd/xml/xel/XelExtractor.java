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
import java.util.Map;
import org.sd.util.tree.Tree;
import org.sd.xml.XmlLite;

/**
 * Xml Extraction Language (xel) extractor interface.
 * <p>
 * @author Spence Koehler
 */
public interface XelExtractor {
  
  /**
   * Get this extractor's root node.
   */
  public Tree<XmlLite.Data> getRootNode();

  /**
   * Perform extraction according to the given key.
   *
   * @param key  A key to identify the extraction.
   *
   * @return an extraction instance or null.
   */
  public XelExtraction extract(String key);

  /**
   * Set exclusions according to the given key.
   */
  public boolean setExcludes(String excludeKey, boolean append);

  /**
   * Get the excluded nodes.
   */
  public List<Tree<XmlLite.Data>> getExcludes();

  /**
   * Test whether a node is excluded.
   *
   * @param node  The node to check.
   * @param inherit  True if descendents of excluded nodes should inherit
   *                 exclusion status.
   */
  public boolean isExcluded(Tree<XmlLite.Data> node, boolean inherit);

  /**
   * Get text resulting from applying the given keys, associating results
   * with the same labels. For example, for each label=foo, xpath=bar, the
   * result should hold key=foo, value=extract(bar).getText().
   */
  public Map<String, List<String>> getText(Map<String, String> label2key);
}
