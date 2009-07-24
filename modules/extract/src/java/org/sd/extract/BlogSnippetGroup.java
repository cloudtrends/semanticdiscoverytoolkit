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


import org.sd.extract.datetime.DateTimeFlags;
import org.sd.extract.datetime.DateTimeInterpretation;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Container for extraction group(s) for blog snippets.
 * <p>
 * @author Spence Koehler
 */
public class BlogSnippetGroup {

  private List<ExtractionGroup> extractionGroups;
  private Set<String> structureKeys;
  private DateTimeFlags dateTimeFlags;
  private ExtractionGroup _extractionGroup;

  public BlogSnippetGroup(ExtractionGroup extractionGroup) {
    this.extractionGroups = new ArrayList<ExtractionGroup>();
    this.structureKeys = new HashSet<String>(extractionGroup.getStructureKeys());
    this.dateTimeFlags = getGroupFlags(extractionGroup);
    doAddGroup(extractionGroup);
  }

  private final void doAddGroup(ExtractionGroup extractionGroup) {
    extractionGroups.add(extractionGroup);
    _extractionGroup = null;
  }

  private final DateTimeFlags getGroupFlags(ExtractionGroup extractionGroup) {
    final int numStructureKeys = extractionGroup.getStructureKeys().size();
    final DateTimeFlags groupFlags = new DateTimeFlags();
    for (Extraction extraction : extractionGroup.getExtractions()) {
      final DateTimeInterpretation datetime = (DateTimeInterpretation)extraction.getInterpretation();
      groupFlags.or(new DateTimeFlags(datetime));
      if (numStructureKeys == 1) break;  // no need to continue
    }
    return groupFlags;
  }

  public DateTimeFlags getDateTimeFlags() {
    return dateTimeFlags;
  }

  /**
   * Accept the extraction group into this snippet group only if its flags
   * are the same.
   */
  public boolean accept(ExtractionGroup extractionGroup) {
    boolean result = false;

    final DateTimeFlags groupFlags = getGroupFlags(extractionGroup);
    final Set<String> groupKeys = extractionGroup.getStructureKeys();

    if (!areConflicting(dateTimeFlags, groupFlags) && !areConflicting(structureKeys, groupKeys)) {
      doAddGroup(extractionGroup);

      dateTimeFlags.or(groupFlags);
      structureKeys.addAll(groupKeys);

      result = true;
    }

    return result;
  }

  /**
   * Get the combined extraction group.
   */
  public ExtractionGroup getExtractionGroup() {
    if (_extractionGroup == null) {
      if (extractionGroups.size() == 1) {
        _extractionGroup = extractionGroups.get(0);
      }
      else {
        for (ExtractionGroup extractionGroup : extractionGroups) {
          if (_extractionGroup == null) {
            _extractionGroup = new ExtractionGroup(extractionGroup);  // copy
          }
          else {
            for (Extraction extraction : extractionGroup.getExtractions()) {
              _extractionGroup.addExtraction(extraction, true);
            }
          }
        }
      }
    }
    return _extractionGroup;
  }

  /**
   * Determine whether 'day' is set in this group's flags.
   */
  public boolean hasDay() {
    return dateTimeFlags.hasDay();
  }

  /**
   * Count the number of datetime day, month, and not-guessed year flags that
   * are set.
   */
  public int countInfo() {
    int result = 0;

    if (dateTimeFlags.hasDay()) ++result;
    if (dateTimeFlags.hasMonth()) ++result;
    if (dateTimeFlags.hasYear() && !dateTimeFlags.guessedYear()) ++result;

    return result;
  }

  /**
   * Define a conflict as flags2 not being equal to flags1.
   */
  private final boolean areConflicting(DateTimeFlags flags1, DateTimeFlags flags2) {
    return !flags2.equals(flags1);
  }

  /**
   * Define a conflict as keys2 having an element not in keys1.
   */
  private final boolean areConflicting(Set<String> keys1, Set<String> keys2) {
    boolean result = false;

    for (String key2 : keys2) {
      if (!keys1.contains(key2)) {
        result = true;
        break;
      }
    }

    return result;
  }

//   /**
//    * Define a conflict as flags2 having a bit set that is not set in flags1.
//    */
//   private final boolean areConflicting(DateTimeFlags flags1, DateTimeFlags flags2) {
//     final DateTimeFlags copy = new DateTimeFlags(flags1);
//     copy.or(flags2);
//     return !copy.equals(flags1);
//   }
}
