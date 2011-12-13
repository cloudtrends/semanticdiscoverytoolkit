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


import org.sd.util.Histogram;
import org.sd.util.tree.Tree;
import org.sd.xml.XmlLite;

import java.util.ArrayList;
import java.util.List;

/**
 * Divide strategy that identifies records where children of branching children
 * branch.
 * <p>
 * NOTE: This strategy does not preserve all text.
 *
 * @author Spence Koehler
 */
public class DoubleBranchStrategy implements DivideStrategy {

  private Record.View view;

  public DoubleBranchStrategy() {
    this(new Record.View(false, Record.DEFAULT_HTML_VIEW_STRINGS));
  }

  public DoubleBranchStrategy(Record.View view) {
    this.view = view;
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

    final List<Record> result = new ArrayList<Record>();
    doDivide1(xmlTree, result);

    return result.size() == 0 ? null : result;
  }

  protected void separateBranchingChildren(Tree<XmlLite.Data> recordTree, List<Tree<XmlLite.Data>> branchingChildren, List<Tree<XmlLite.Data>> nonBranchingChildren) {
    final int numChildren = recordTree.numChildren();

    if (numChildren == 1) {
      nonBranchingChildren.add(recordTree.getChildren().get(0));
    }
    else if (numChildren > 1) {
      final Histogram<String> h = new Histogram<String>();

      // branching children are those with the view names that occur more than once
      for (Tree<XmlLite.Data> child : recordTree.getChildren()) {
        if (Record.isNonRecord(child)) continue;

        final String name = view.getViewName(child);
        h.add(name);
      }

      // those with frequency counts > 1 are branching; others are non-branching
      for (Tree<XmlLite.Data> child : recordTree.getChildren()) {
        if (Record.isNonRecord(child)) continue;

        final String name = view.getViewName(child);
        final Histogram.Frequency freq = h.getElementFrequency(name);
        if (freq.getFrequency() > 1) {
          branchingChildren.add(child);
        }
        else {
          nonBranchingChildren.add(child);
        }
      }
    }
  }

  private final boolean doDivide1(Tree<XmlLite.Data> recordTree, List<Record> result) {
    boolean retval = false;

    final List<Tree<XmlLite.Data>> branchingChildren = new ArrayList<Tree<XmlLite.Data>>();
    final List<Tree<XmlLite.Data>> nonBranchingChildren = new ArrayList<Tree<XmlLite.Data>>();

    separateBranchingChildren(recordTree, branchingChildren, nonBranchingChildren);

    for (Tree<XmlLite.Data> branchingChild : branchingChildren) {
      retval |= doDivide2(branchingChild, result);
    }

    for (Tree<XmlLite.Data> nonBranchingChild : nonBranchingChildren) {
      retval |= doDivide1(nonBranchingChild, result);
    }

    return retval;
  }

  private final boolean doDivide2(Tree<XmlLite.Data> recordTree, List<Record> result) {
    boolean retval = false;

    if (recordTree.numChildren() > 1) {
      // found branching under branching
      final Record record = Record.getRecord(recordTree);
      if (record != null) {
        if (!doDivide1(recordTree, result)) {
          result.add(record);
          retval = true;
        }
      }
    }
    else if (recordTree.numChildren() == 1) {
      for (Tree<XmlLite.Data> child : recordTree.getChildren()) {
        if (false /*consecutiveOnly*/) {
          // start over looking for consecutive branching
          retval |= doDivide1(child, result);
        }
        else {  // non-consecutive ok
          // if not necessarily consecutive nodes, then go down until we find the next branching
          retval |= doDivide2(child, result);
        }
      }
    }

    return retval;
  }


// java -Xmx640m org.sd.xml.record.DumpUtil divideStrategy=org.sd.xml.record.DoubleBranchStrategy suppressTopLevel=false suppressDivisions=false /home/sbk/doc/personal/www/index.html

}
