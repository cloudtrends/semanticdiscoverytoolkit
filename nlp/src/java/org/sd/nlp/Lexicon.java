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
package org.sd.nlp;


/**
 * Interface for getting the categories for a String.
 * <p>
 * @author Spence Koehler
 */
public interface Lexicon {

  /**
   * Look up the categories for a subString, adding definition that apply.
   *
   * @param subString  The subString to lookup categories for.
   */
  public void lookup(StringWrapper.SubString subString);
  
  /**
   * Get the normalizer that applies to this lexicon.
   *
   * @return this lexicon's normalizer. null when no normalization occurs.
   */
  public Normalizer getNormalizer();

  /**
   * Get the maximum number of words in terms defined by this lexicon.
   *
   * @return the maximum number of words or 0 for unlimited.
   */
  public int maxNumWords();

}
