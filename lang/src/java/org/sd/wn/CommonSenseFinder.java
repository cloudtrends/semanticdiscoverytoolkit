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
package org.sd.wn;

import java.io.IOException;

/**
 * Utility class for finding common senses.
 * <p>
 * @author Spence Koehler
 */
public class CommonSenseFinder {

  public static final WordSenseWrapper findCommonSense(WordSenseWrapper sense1, WordSenseWrapper sense2, PointerSymbol relation) {
    final CommonSenseOperator operator = new CommonSenseOperator(sense1, relation);
    sense2.expand(operator, SingleRelationFilter.getInstance(relation));
    return operator.getCommonSense();
  }

//todo: walk through/compare all senses? avoid O(n-squared) complexity or worse? use WordSenseContainer.
//
// do they have a common word sense?
// expand one along the relation
// do they have a common word sense?
// expand the other along the relation
// repeat

  public static void main(String[] args) throws IOException {
    //arg0: dictDir
    //arg1: inputWord1
    //arg2: inputWord2
  }
}
