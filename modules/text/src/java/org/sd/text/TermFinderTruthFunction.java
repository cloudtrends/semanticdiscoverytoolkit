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
package org.sd.text;


import org.sd.nlp.NormalizedString;
import org.sd.util.logic.LogicalResult;
import org.sd.util.logic.TruthFunction;

/**
 * A TruthFunction (used with the logic package's LogicalExpression class)
 * for input strings containing terms according to a term finder.
 * <p>
 * @author Spence Koehler
 */
public class TermFinderTruthFunction extends TruthFunction<String> {

  private TermFinder termFinder;
  private int acceptPartial;

  public TermFinderTruthFunction(boolean caseSensitive, String[] terms, int acceptPartial) {
    this(new TermFinder("", caseSensitive, terms), acceptPartial);
  }

  public TermFinderTruthFunction(TermFinder termFinder, int acceptPartial) {
    this.termFinder = termFinder;
    this.acceptPartial = acceptPartial;
  }

  public TermFinder getTermFinder() {
    return termFinder;
  }

  public int getAcceptPartial() {
    return acceptPartial;
  }

  public LogicalResult<String> evaluateInput(String input) {
    final NormalizedString nstring = termFinder.normalize(input);
    final int[] nppos = termFinder.findPatternPos(nstring, acceptPartial);  // normalized position

    // translate normalized positions back to original positions
    if (nppos != null) {
      final int oStart = nstring.getOriginalIndex(nppos[0]);
      final int oEnd = nstring.getOriginalIndex(nppos[0] + nppos[1] - 1);
      nppos[0] = oStart;
      nppos[1] = oEnd - oStart + 1;
    }

    return new TermFinderLogicalResult(input, nppos, this);
  }
}
