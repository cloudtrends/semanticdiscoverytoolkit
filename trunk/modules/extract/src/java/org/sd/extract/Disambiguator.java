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
 * Utility to resolve ambiguities within extraction groups.
 * <p>
 * @author Spence Koehler
 */
public abstract class Disambiguator {
  
  private Interpreter interpreter;

  public Disambiguator(Interpreter interpreter) {
    this.interpreter = interpreter;
  }

  /**
   * Disambiguate the extractions in the given group.
   */
  public void disambiguate(ExtractionGroup group) {
    List<Interpretation[]> allInterpretations = null;

    // interpret all extractions.
    for (Extraction extraction : group.getExtractions()) {
      final Interpretation[] interpretations = interpreter.interpret(extraction);
      if (interpretations != null) {
        if (allInterpretations == null) allInterpretations = new ArrayList<Interpretation[]>();
        allInterpretations.add(interpretations);
      }
    }

    if (allInterpretations != null) {
      // disambiguate each extraction's interpretations by considering all.
      final List<Interpretation> disambiguatedInterpretations = disambiguate(allInterpretations);

      if (disambiguatedInterpretations != null) {
        // set each disambiguated interpretation on its extraction instance.
        for (Interpretation interpretation : disambiguatedInterpretations) {
          interpretation.getExtraction().setInterpretation(interpretation);
        }
      }
    }
  }


  /**
   * Disambiguate each extraction's interpretations by considering all.
   *
   * @return the final interpretations, one for each interpretable extraction.
   */
  public abstract List<Interpretation> disambiguate(List<Interpretation[]> allInterpretations);
}
