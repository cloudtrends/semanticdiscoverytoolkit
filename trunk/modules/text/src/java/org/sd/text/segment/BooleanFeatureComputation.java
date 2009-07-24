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
package org.sd.text.segment;


/**
 * Interface for computing a boolean feature for text.
 * <p>
 * @author Spence Koehler
 */
public interface BooleanFeatureComputation {

  /**
   * Get the feature type (or key).
   */
  public String getFeatureType();

  /**
   * Compute the boolean feature value for the text.
   *
   * @return true, false, or null if this computation does not apply to the text.
   */
  public Boolean computeFeature(String text);

  /**
   * Function to split the text in a way meaningul to an instance's
   * computation.
   * <p>
   * For example, term finders might split the text into the data before
   * a key term, the key term, and the data after the key term.
   *
   * @return the segment sequence of the split text or null if unable or
   *         meaningless to split.
   */
  public SegmentSequence split(String text);
}
