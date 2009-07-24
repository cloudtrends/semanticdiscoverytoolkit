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
package org.sd.crawl;


import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Class to combine processor results.
 * <p>
 * @author Spence Koehler
 */
public class ProcessorResultsCombiner {

  private Map<String, List<ProcessorResult>> name2results;

  public ProcessorResultsCombiner() {
    this.name2results = new LinkedHashMap<String, List<ProcessorResult>>();
  }

  public void add(ProcessorResult processorResult) {
    final String name = processorResult.getName();
    List<ProcessorResult> results = name2results.get(name);
    if (results == null) {
      results = new ArrayList<ProcessorResult>();
      name2results.put(name, results);
    }
    results.add(processorResult);
  }

  public List<ProcessorResult> getCombinedResults() {
    final List<ProcessorResult> result = new ArrayList<ProcessorResult>();

    for (Collection<ProcessorResult> processorResults : name2results.values()) {
      result.add(combine(new ArrayList<ProcessorResult>(processorResults)));
    }

    return result;
  }

  // combine using processor results directly
  private final ProcessorResult combine(List<ProcessorResult> processorResults) {
    final ProcessorResult result = processorResults.get(0);

    final int size = processorResults.size();

    if (size > 1) {
      final ProcessorResult[] otherValues = new ProcessorResult[size - 1];
      for (int i = 1; i < processorResults.size(); ++i) {
        otherValues[i - 1] = processorResults.get(i);
      }
      result.combineWithOthers(otherValues);
    }

    return result;
  }

/*
//combine using value string forms
  private final ProcessorResult combine(List<ProcessorResult> processorResults) {
    final ProcessorResult result = processorResults.get(0);

    final int size = processorResults.size();

    if (size > 1) {
      final String[] otherValues = new String[size - 1];
      for (int i = 1; i < processorResults.size(); ++i) {
        otherValues[i - 1] = processorResults.get(i).getValue();
      }
      result.combineWithOthers(otherValues);
//todo: oughtta just combine directly with the objects and not with the string values!
    }

    return result;
  }
*/
}
