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
package org.sd.extract.datetime;


import org.sd.extract.Disambiguator;
import org.sd.extract.Interpretation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Disambiguator for the DateTimeExtractor.
 * <p>
 * @author Spence Koehler
 */
public class DateTimeDisambiguator extends Disambiguator {

  private static final DateTimeDisambiguator INSTANCE = new DateTimeDisambiguator();

  public static final DateTimeDisambiguator getInstance() {
    return INSTANCE;
  }

  private DateTimeDisambiguator() {
    super(new DateTimeInterpreter()); //todo: make interpreter
  }

  /**
   * Disambiguate each extraction's interpretations by considering all.
   *
   * @return the final interpretations, one for each interpretable extraction.
   */
  public List<Interpretation> disambiguate(List<Interpretation[]> allInterpretations) {
    final Map<String, List<Interpretation>> key2ci = new HashMap<String, List<Interpretation>>();
    final Set<String> keys = new HashSet<String>();

    for (Interpretation[] interpretations : allInterpretations) {
      keys.clear();

      // add an interpretation once for each key from a set
      for (Interpretation interpretation : interpretations) {
        final String key = getKey(interpretation);
        if (!keys.contains(key)) {
          keys.add(key);
          
          List<Interpretation> commonInterpretations = key2ci.get(key);
          if (commonInterpretations == null) {
            commonInterpretations = new ArrayList<Interpretation>();
            key2ci.put(key, commonInterpretations);
          }
          commonInterpretations.add(interpretation);
        }
      }
    }

    // Choose the most frequent common interpretations
    List<Interpretation> result = null;
    for (List<Interpretation> ci : key2ci.values()) {
      if (result == null) {
        result = ci;
      }
      else {
        if (ci.size() > result.size()) {
          result = ci;
        }
        else if (ci.size() == result.size() && ci.size() > 0) {
          // when occurrence count is tied, keep the interpretation with the most information
          final int infoCount1 = result.get(0).getStructureKey().split("\\s+").length;
          final int infoCount2 = ci.get(0).getStructureKey().split("\\s+").length;

          if (infoCount2 > infoCount1) {
            result = ci;
          }
        }
      }
    }

    return result;
  }

  private static final String getKey(Interpretation interpretation) {
    final DateTimeInterpretation dateTime = (DateTimeInterpretation)interpretation;
    return dateTime.getParse().getParseKey();
  }
}
