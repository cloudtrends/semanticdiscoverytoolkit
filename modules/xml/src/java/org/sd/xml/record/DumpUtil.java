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


import org.sd.util.PropertiesParser;
import org.sd.util.ReflectUtil;
import org.sd.util.fsm.FsmBuilder;
import org.sd.util.fsm.FsmBuilder2;
import org.sd.util.fsm.FsmMatcher;
import org.sd.util.fsm.FsmSequence;
import org.sd.util.fsm.FsmState;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Utility to dump records.
 * <p>
 * @author Spence Koehler
 */
public class DumpUtil {
  
  private static final void showRecord(Record record, int recNum) {
    System.out.println("\nRecord #" + recNum + " (" +
                       record.minPathIndex() + "-" + record.maxPathIndex() +
                       ") groupId=" + record.getGroupId() + " key=" + record.getKey() +
                       ": " + record.getViewName());

    final List<Record.Field> fields = record.getViewFields();
    if (fields != null) {
      for (Record.Field field : fields) {
        System.out.println("\t" + field.attribute + ": " + field.value);
      }
    }
    else {
      if (record.asValue() != null) {
        System.out.println("\t<text>: " + record.asValue().getData());
      }
      else {
        System.out.println("\t<EMPTY>");
      }
    }
  }

  private static final void showRecords(List<Record> records) {
    int recNum = 0;
    for (Record record : records) {
      showRecord(record, recNum++);
    }
  }

  private static final List<List<Record>> buildRecordGroups(List<Record> records, List<FsmSequence<Integer>> seqs) {
    final List<List<Record>> recordGroups = new ArrayList<List<Record>>();

    // build fsm sequences
    final FsmBuilder<Integer> fsmBuilder = new FsmBuilder2<Integer>("~");
    for (Record record : records) {
      fsmBuilder.add(record.getGroupId());
    }

    // match against fsm sequences
    final FsmMatcher<Integer> fsmMatcher = fsmBuilder.getMatcher(FsmMatcher.Mode.LONGEST);
    final List<Record> curRecords = new ArrayList<Record>();
    for (Record record : records) {
      final FsmState<Integer> fsmState = fsmMatcher.add(record.getGroupId());
      curRecords.add(record);
      if (fsmState == null || fsmState.isAtAnEnd()) {
        recordGroups.add(new ArrayList<Record>(curRecords));
        seqs.add(fsmState == null ? null : fsmState.getCompletedSequence());
        curRecords.clear();
      }
    }

    return recordGroups;
  }

  private static final void showFsmRecords(List<Record> records) {
    final List<FsmSequence<Integer>> seqs = new ArrayList<FsmSequence<Integer>>();
    final List<List<Record>> recordGroups = buildRecordGroups(records, seqs);

    System.out.println("\n\nFound " + recordGroups.size() + " record groups:");
    int groupNum = 0;
    for (List<Record> recordGroup : recordGroups) {
      final FsmSequence<Integer> seq = seqs.get(groupNum);
      System.out.println("\n\t Record group#" + groupNum + " has " + recordGroup.size() + " records:");
      if (seq == null) {
        System.out.println("\t\t(fsmSeq=<NULL>)");
      }
      else {
        System.out.println("\t\t(fsmSeq=" + seq.getKey() + " w/minRepeat=" + seq.getMinRepeat() + ", maxRepeat=" + seq.getMaxRepeat() + ", totRepeat=" + seq.getTotalRepeat() + ")");
      }

      int recNum = 0;
      for (Record record : recordGroup) {
        showRecord(record, recNum++);
      }
      ++groupNum;
    }
  }

  // java -Xmx640m org.sd.xml.record.DumpUtil divideStrategy=org.sd.xml.record.RepeatingPatternDivideStrategy /home/sbk/tmp/Slashdot.2009-01-09.html

  public static final void main(String[] args) throws IOException {
    //
    // Properties:
    //
    //  divideStrategy -- (optional) divide strategy to use
    //  suppressTopLevel -- (optional, default=false) suppress showing each file as a single record
    //  suppressDivisions -- (optional, default=false) suppress showing raw divisions
    //  suppressFsmDivisions -- (optional, default=false) suppress showing fsm divisions
    //
    //  args -- paths to files to load/dump
    //
    final PropertiesParser pp = new PropertiesParser(args);
    final Properties properties = pp.getProperties();
    args = pp.getArgs();

    DivideStrategy divideStrategy = null;
    final boolean suppressTopLevel = "true".equals(properties.getProperty("suppressTopLevel", "false"));
    final boolean suppressDivisions = "true".equals(properties.getProperty("suppressDivisions", "false"));
    final boolean suppressFsmDivisions = "true".equals(properties.getProperty("suppressFsmDivisions", "false"));

    final String divideStrategyString = properties.getProperty("divideStrategy");
    if (divideStrategyString != null && !"".equals(divideStrategyString)) {
      divideStrategy = (DivideStrategy)ReflectUtil.buildInstance(divideStrategyString, properties);
    }


    for (String filePath : args) {
      final File file = new File(filePath);
      final Record topRecord = Record.buildHtmlRecord(file);
      final List<Record> records = divideStrategy != null ? divideStrategy.divide(topRecord) : null;
      final String countString = (records == null) ? "No" : Integer.toString(records.size());

      System.out.println("\n\n" + countString + " " + divideStrategyString +
                         " divisions of '" + filePath + "':");


      if (records != null) {
        if (!suppressDivisions) {
          showRecords(records);
        }

        if (!suppressFsmDivisions) {
          showFsmRecords(records);
        }
      }

      if (!suppressTopLevel) {
        System.out.println("\n\nTop level record '" + filePath + "':");
        System.out.println(topRecord);
      }
    }
  }
}
