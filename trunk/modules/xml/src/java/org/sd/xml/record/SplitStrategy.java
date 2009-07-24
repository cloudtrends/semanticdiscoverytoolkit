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
package org.sd.xml.record;


import org.sd.util.tree.Tree;
import org.sd.xml.XmlLite;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A divide strategy to split a record when an attribute repeats.
 * <p>
 * @author Spence Koehler
 */
public class SplitStrategy implements DivideStrategy {
  
  private String name;

  /**
   * Construct with the name to give new (divided) records.
   */
  public SplitStrategy(String name) {
    this.name = name;
  }

  /**
   * Divide the given record into sub-records.
   * 
   * @return the sub-records or null if the record cannot be divided by
   *         this strategy.
   */
  public List<Record> divide(Record record) {
    if (!record.hasTree()) return null;

    final Tree<XmlLite.Data> xmlTree = record.asTree();
    if (!xmlTree.hasChildren()) return null;

    final List<Tree<XmlLite.Data>> children = xmlTree.getChildren();
    List<Record> result = null;
    
    final Set<String> seenAttributes = new HashSet<String>();
    final List<Tree<XmlLite.Data>> recordChildren = new ArrayList<Tree<XmlLite.Data>>();

    for (Tree<XmlLite.Data> child : children) {
      final XmlLite.Tag tag = child.getData().asTag();
      boolean startNextRecord = false;
      if (tag != null) {
        final String attribute = tag.name;
        if (seenAttributes.contains(attribute)) {
          startNextRecord = true;
          seenAttributes.clear();
        }
        seenAttributes.add(attribute);
      }
      if (startNextRecord) {
        if (result == null) result = new ArrayList<Record>();

        // put the recordChildren into their own record and add to the result
        result.add(Record.buildRecord(name, recordChildren));

        // clear out the recordChildren for the next record
        recordChildren.clear();
      }

      recordChildren.add(child);
    }

    // add remaining recordChildren as another record
    if (recordChildren.size() > 0) {
      if (result == null) result = new ArrayList<Record>();
      result.add(Record.buildRecord(name, recordChildren));
    }

    return result;
  }
}
