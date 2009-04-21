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
import java.util.List;


/**
 * Base class for top-down divide strategies where all children
 * of a node are considered to be records according to some rule.
 * <p>
 * @author Spence Koehler
 */
public abstract class TopDownDivideStrategy implements DivideStrategy {
  
  /**
   * Given a "record" xmlNode, determine whether its a final (divided) record.
   *
   * @return true the node is considered as a record; otherwise, false.
   */
  protected abstract boolean nodeIsRecord(Tree<XmlLite.Data> xmlNode);


  private Record.View view;

  protected TopDownDivideStrategy() {
    this(new Record.View(false, Record.DEFAULT_HTML_VIEW_STRINGS));
  }

  protected TopDownDivideStrategy(Record.View view) {
    this.view = view;
  }

  /**
   * Divide the given record into sub-records.
   * 
   * @return the sub-records or null if the record cannot be divided by
   *         this strategy.
   */
  public final List<Record> divide(Record record) {
    if (!record.hasTree()) return null;

    final Tree<XmlLite.Data> xmlTree = record.asTree();
    if (!xmlTree.hasChildren()) return null;

    final List<Record> result = new ArrayList<Record>();
    doDivide(xmlTree, result);

    return result.size() == 0 ? null : result;
  }

  /**
   * Overridable utility to get the (tag or view) name for the given node.
   */
  protected String getName(Tree<XmlLite.Data> xmlNode) {
    //return xmlNode.getData().asTag().name;
    return view.getViewName(xmlNode);
  }

  private final boolean doDivide(Tree<XmlLite.Data> recordTree, List<Record> result) {
    boolean retval = false;
    
    if (Record.isNonRecord(recordTree)) return false;

    if (nodeIsRecord(recordTree)) {
      result.add(Record.getRecord(recordTree));
      retval = true;
    }
    else {
      int numRecordChildren = 0;

      // step down and test each "record" child.
      if (recordTree.hasChildren()) {
        for (Tree<XmlLite.Data> childNode : recordTree.getChildren()) {
          if (Record.isNonRecord(childNode)) continue;

          retval |= doDivide(childNode, result);
          ++numRecordChildren;
        }
      }

      if (!retval && numRecordChildren == 1) {
        // add this indivisible node as a record.
        result.add(Record.getRecord(recordTree));
        retval = true;
      }
    }

    return retval;
  }
}
