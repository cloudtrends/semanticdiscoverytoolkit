/*
    Copyright 2013 Semantic Discovery, Inc. (www.semanticdiscovery.com)

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
 * Container for all parse results for a generic parser as accessed through
 * a generic parse cache.
 * <p>
 * @author Spence Koehler
 */
public class GenericParseResults {
  
  private ParseOutputCollector parseOutput;
  private GenericParseHelper genericParseHelper;
  private Map<String, List<GenericParse>> parses;  // by ruleID

  GenericParseResults(ParseOutputCollector parseOutput, GenericParseHelper genericParseHelper) {
    this.parseOutput = parseOutput;
    this.genericParseHelper = genericParseHelper;
    this.parses = null;
    initParses();
  }

  private final void initParses() {
    if (parseOutput != null) {
      final List<AtnParseResult> parseResults = parseOutput.getParseResults();
      if (parseResults != null) {
        for (AtnParseResult parseResult : parseResults) {
          final int numParses = parseResult.getNumParses();
          for (int parseNum = 0; parseNum < numParses; ++parseNum) {
            final AtnParse atnParse = parseResult.getParse(parseNum);
            if (atnParse.getSelected()) {
              final List<ParseInterpretation> curInterps = atnParse.getParseInterpretations();
              if (curInterps != null) {
                for (ParseInterpretation interp : curInterps) {
                  final GenericParse parse = genericParseHelper.buildGenericParse(interp);
                  if (parse != null) {
                    if (parses == null) {
                      this.parses = new HashMap<String, List<GenericParse>>();
                    }
                    final String ruleId = parse.getRuleId();
                    List<GenericParse> parseList = parses.get(ruleId);
                    if (parseList == null) {
                      parseList = new ArrayList<GenericParse>();
                      parses.put(ruleId, parseList);
                    }
                    parseList.add(parse);
                  }
                }
              }
            }
          }
        }
      }
    }
  }

  public boolean hasParses() {
    return parses != null;
  }

  public boolean hasSingleParse() {
    boolean result = (parses != null && parses.size() == 1);

    if (result) {
      final List<GenericParse> parseList = parses.values().iterator().next();
      if (parseList == null || parseList.size() != 1) {
        result = false;
      }
    }

    return result;
  }

  public boolean hasParse(String ruleId) {
    return parses != null && parses.containsKey(ruleId);
  }

  public List<GenericParse> getParses(String ruleId) {
    return parses != null ? parses.get(ruleId) : null;
  }

  public Map<String, List<GenericParse>> getParses() {
    return parses;
  }

  /**
   * Get the single parse iff there is a single parse.
   */
  public GenericParse getSingleParse() {  // by ruleID
    GenericParse result = null;

    if (hasSingleParse()) {
      result = parses.values().iterator().next().get(0);
    }

    return result;
  }

  /**
   * Get the first parse if there are parses.
   */
  public GenericParse getFirstParse() {  // by ruleID
    GenericParse result = null;

    if (parses != null && parses.size() > 0) {
      final List<GenericParse> parseList = parses.values().iterator().next();
      if (parseList != null && parseList.size() > 0) {
        result = parseList.get(0);
      }
    }

    return result;
  }

  public ParseOutputCollector getParseOutput() {
    return parseOutput;
  }
}
