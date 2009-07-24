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


import java.util.ArrayList;
import java.util.List;

/**
 * A disambiguator that always chooses the first interpretation.
 * <p>
 * @author Spence Koehler
 */
public class FirstInterpretationDisambiguator extends Disambiguator {
  
  public FirstInterpretationDisambiguator(Interpreter interpreter) {
    super(interpreter);
  }

  /**
   * Disambiguate each extraction's interpretations by considering all.
   *
   * @return the final interpretations, one for each interpretable extraction.
   */
  public List<Interpretation> disambiguate(List<Interpretation[]> allInterpretations) {
    List<Interpretation> result = null;

    for (Interpretation[] interpretations : allInterpretations) {
      if (interpretations != null && interpretations.length > 0) {
        if (result == null) result = new ArrayList<Interpretation>();
        result.add(interpretations[0]);
      }
    }

    return result;
  }
}
