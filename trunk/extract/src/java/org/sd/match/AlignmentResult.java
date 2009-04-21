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
package org.sd.match;


/**
 * Interface for an alignment result.
 * <p>
 * @author Spence Koehler
 */
public interface AlignmentResult extends Comparable<AlignmentResult> {

  /**
   * Disqualify this result by setting the score to 0.
   * <p>
   * This would apply, for example, if the normalized input string is
   * a stopword.
   */
  public void disqualify();

  /**
   * Get a string for just the aligned text.
   */
  public String getAlignedString(boolean normalized);

  public double getScore();
  
//todo: add an accessor to get the extra input terms?

  public String getExplanation();

  // get best-matched form as "F" + form ordinal. (i.e. "F2").
  public String getForm();

}
