/*
    Copyright 2010 Semantic Discovery, Inc. (www.semanticdiscovery.com)

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
package org.sd.atn;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Container for grouping ambiguous parses.
 * <p>
 * Note that ambiguous parses may be ambiguous due to any combination of the
 * following:
 * <ul>
 * <li>Different (start) categories</li>
 * <li>Same (start) categories, but different structure</li>
 * <li>Same category and structure, but different interpretations</li>
 * </ul>
 * 
 * <p>
 * @author Spence Koehler
 */
public class AmbiguousParses {
  
  /**
   * Convenience factory method to create an instance with ambiguities across
   * the given parse results.
   */
  public static AmbiguousParses createInstance(List<AtnParseResult> parseResults, String category) {
    final AmbiguousParses result = new AmbiguousParses();

    // separate parses out by (start) category
    for (AtnParseResult parseResult : parseResults) {
      boolean startNewGroup = true;
      final int numParses = parseResult.getNumParses();
      for (int parseNum = 0; parseNum < numParses; ++parseNum) {
        final AtnParse parse = parseResult.getParse(parseNum);
        if (category.equals(parse.getCategory())) {
          if (parse.getSelected()) {
            result.add(parse, startNewGroup);
            startNewGroup = false;
          }
        }
      }
    }

    return result;
  }

  private boolean hasAmbiguity;
  private Map<Integer, List<AtnParse>> ambiguousParses;
  private int nextId;
  private List<AtnParse> curGroup;

  public AmbiguousParses() {
    this.hasAmbiguity = false;
    this.ambiguousParses = new HashMap<Integer, List<AtnParse>>();
    this.nextId = 0;
    this.curGroup = null;
  }

  public boolean hasAmbiguity() {
    return hasAmbiguity;
  }

  public int numGroups() {
    return nextId;
  }

  public void add(AtnParse atnParse, boolean startNewGroup) {
    if (startNewGroup || curGroup == null || curGroup.size() == 0) {
      curGroup = new ArrayList<AtnParse>();
      ambiguousParses.put(nextId++, curGroup);
    }
    curGroup.add(atnParse);

    if (!this.hasAmbiguity) {
      // if no known ambiguity yet, see if we have it now
      this.hasAmbiguity = (curGroup.size() > 1) || (atnParse.getParseInterpretations().size() > 1);
    }
  }

  public boolean hasAmbiguity(int groupNum) {
    boolean result = false;

    final List<AtnParse> parseGroup = getGroup(groupNum);
    if (parseGroup != null) {
      if (parseGroup.size() > 1) {
        result = true;
      }
      else {
        final AtnParse onlyParse = parseGroup.get(0);
        result = (onlyParse.getParseInterpretations().size() > 1);
      }
    }

    return result;
  }

  public List<AtnParse> getGroup(int groupNum) {
    return ambiguousParses.get(groupNum);
  }

  public Map<Integer, List<AtnParse>> getAmbiguousParses() {
    return ambiguousParses;
  }
}
