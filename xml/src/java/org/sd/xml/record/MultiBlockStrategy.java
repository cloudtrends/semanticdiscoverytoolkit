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
import org.sd.util.StatsAccumulator;
import org.sd.util.tree.Tree;
import org.sd.xml.XmlLite;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * A record divide strategy for dividing a record where multiple blocks of
 * consecutive fields exist and each block is a record.
 * <p>
 * @author Spence Koehler
 */
public class MultiBlockStrategy implements DivideStrategy {
  
  private Record.View view;

  /**
   * Construct an instance using the default html view.
   */
  public MultiBlockStrategy() {
    this(new Record.View(false, Record.DEFAULT_HTML_VIEW_STRINGS));
  }

  /**
   * Construct an instance using the given view.
   */
  public MultiBlockStrategy(Record.View view) {
    this.view = view;
  }

  /**
   * Divide the given record into sub-records.
   * 
   * @return the sub-records or null if the record cannot be divided by
   *         this strategy.
   */
  public List<Record> divide(Record record) {
    List<Record> result = null;

    //
    // algorithm:
    //  - find repeating text with repeating paths using the view.
    //  - group the most (equally) frequent paths into records.
    //  - distribute same paths across records.
    //

    final PathHistogram pathHistogram = new PathHistogram(record);
    final PathHistogram.FieldGroupCollector fieldGroupCollector = new PathHistogram.FieldGroupCollector();

    for (Iterator<PathHistogram.PathGroup> pgIter = pathHistogram.iterator(); pgIter.hasNext(); ) {
      final PathHistogram.PathGroup pathGroup = pgIter.next();

      if (!accept(pathGroup)) continue;

      final boolean complete = fieldGroupCollector.addAll(pathGroup);
      if (accept(fieldGroupCollector)) {
        result = fieldGroupCollector.getRecords(false/*includeIncomplete*/);

        if (accept(result)) break;
      }
      else {
        // zero out for another try
        fieldGroupCollector.clear();
      }
    }

    return result;
  }

  protected boolean accept(PathHistogram.PathGroup pathGroup) {
    return true;  //pathGroup.freq == 10;
  }

  protected boolean accept(PathHistogram.FieldGroupCollector fieldGroupCollector) {
    boolean result = false;

    // accept if there are more than 2 records and none of them are null.

    final List<Record> records = fieldGroupCollector.getRecords(false/*includeIncomplete*/);
    if (records.size() > 2) {
      result = true;
      for (Record record : records) {
        if (record == null) {
          result = false;
          break;
        }
      }
    }

    return result;
  }

  protected boolean accept(List<Record> records) {

    if (true) return true;

    final StatsAccumulator stats = new StatsAccumulator();

    // average size of records is > 4 paths
    for (Record record : records) {
      if (record != null) {
        stats.add(record.maxPathIndex() - record.minPathIndex());
      }
    }

    System.out.println(stats);

    return stats.getMean() > 0.0;
  }


// java -Xmx640m org.sd.xml.record.DumpUtil divideStrategy=org.sd.xml.record.MultiBlockStrategy suppressTopLevel=false suppressDivisions=false /home/sbk/doc/personal/www/index.html

}
