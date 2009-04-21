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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Choose records with the most diverse field patterns. ...actually, just sort them from most to least diverse ...
 * <p>
 * @author Spence Koehler
 */
public class HeterogeneousRecordChooser extends FilterStrategy {
  
  private Record.View view;

  public HeterogeneousRecordChooser(DivideStrategy divideStrategy) {
    this(divideStrategy, new Record.View(false, Record.DEFAULT_HTML_VIEW_STRINGS));
  }

  public HeterogeneousRecordChooser(DivideStrategy divideStrategy, Record.View view) {
    super(divideStrategy);
    this.view = view;
  }

  /**
   * Filter the list of records.
   */
  protected List<Record> filter(List<Record> records) {
    final Map<AttributeBag, List<Record>> ab2recs = new HashMap<AttributeBag, List<Record>>();
    for (Record record : records) {
      final AttributeBag ab = new AttributeBag(view, record);
      List<Record> abrecords = ab2recs.get(ab);
      if (abrecords == null) {
        abrecords = new ArrayList<Record>();
        ab2recs.put(ab, abrecords);
      }
      abrecords.add(record);
    }
    final List<AttributeBag> bags = new ArrayList<AttributeBag>(ab2recs.keySet());
    Collections.sort(bags);

    final List<Record> result = new ArrayList<Record>();

    // keep all records, just sort them into priority order
    for (AttributeBag bag : bags) {
      final List<Record> abRecords = ab2recs.get(bag);
      result.addAll(abRecords);
    }

//note: this heuristic works okay-ish, but not generally
//     // keep all records "near" the maximum bag size (not less than 3/4ths)
//     final int maxSize = bags.get(0).size();
//     int minSize = (maxSize > 4) ?
//       maxSize - (maxSize >> 2) :  // 3/4ths of maxSize
//       (maxSize >> 1);             // half of maxSize

//     if (minSize == maxSize) minSize = maxSize - 1;

//     for (AttributeBag bag : bags) {
//       if (bag.size() < minSize) break;

//       final List<Record> abRecords = ab2recs.get(bag);
//       result.addAll(abRecords);
//     }

    return result;
  }


//
//todo: use this idea for "grouping/harmonizing" records based on an example  ...now taken care of in Record.View's groupId
//
//   private List<Record> getSiblingRecords(Record record) {
//     final List<Record> result = new ArrayList<Record>();

//     final Tree<XmlLite.Data> recordTree = record.asTree();
//     final String key = view.buildKey(recordTree);
//     final List<Tree<XmlLite.Data>> siblings = recordTree.getGlobalSiblings();
//     for (Tree<XmlLite.Data> sibling : siblings) {
//       final String sibKey = view.buildKey(sibling);
//       if (key.equals(sibKey)) {
//         final Record sibRecord = Record.getRecord(sibling);
//         if (sibRecord != null) result.add(sibRecord);
//       }
//     }

//     return result;
//   }

  private static final class AttributeBag implements Comparable<AttributeBag> {
    // key off the path from the tree's root to the record's root
    // compute the number of paths from record's root to leaves (exclusive of text)

    private Record.View view;
    private String key;              // path from tree's root to record's root
    private int numPaths;
    private Set<String> attributes;  // attributes from record to leaves.

    AttributeBag(Record.View view, Record record) {
      this.view = view;
      this.key = view.buildKey(record.asTree());
      this.numPaths = record.maxPathIndex() - record.minPathIndex();
      this.attributes = new HashSet<String>();
      addAll(record);
    }

    public final void addAll(Record record) {
      final List<Record.Field> fields = record.getFields(view);
      for (Record.Field field : fields) {
        attributes.add(field.attribute);
      }
    }

    public int diversity() {
      return attributes.size();  // measure of diversity
    }

    public int size() {
      return numPaths;
    }

    public int compareTo(AttributeBag other) {
      return other.diversity() - this.diversity();
    }

    public boolean equals(Object o) {
      boolean result = (this == o);

      if (!result && o instanceof AttributeBag) {
        final AttributeBag other = (AttributeBag)o;
        result = key.equals(other.key);
      }

      return result;
    }

    public int hashCode() {
      return key.hashCode();
    }
  }


  public static final void main(String[] args) throws IOException {
    //arg0: path to html file

    final Record topRecord = Record.buildHtmlRecord(new File(args[0]));

    if (false) {
      System.out.println(topRecord);
    }

    if (true) {
      final DivideStrategy divideStrategy = new HeterogeneousRecordChooser(new DoubleBranchStrategy());
      final List<Record> records = divideStrategy.divide(topRecord);
      if (records == null) {
        System.out.println("NO RECORDS!");
      }
      else {
        System.out.println(records.size() + " RECORDS:");
        int i = 0;
        for (Record record : records) {
          System.out.println("\n" + i + ": " + record);
          ++i;
        }
      }
    }
  }
}
