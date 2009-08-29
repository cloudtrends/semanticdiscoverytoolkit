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
package org.sd.cio.mapreduce;


import org.sd.util.NameGenerator;
import org.sd.util.NumberedNameGenerator;

/**
 * Utility methods for building resources.
 * <p>
 * @author Spence Koehler
 */
public class ResourceHelper {

  /**
   * Build a numbered name generator.
   */
  public static final NameGenerator buildNameGenerator(String basePrefix, String basePostfix, int numDigits, Integer chainNumber) {

    final NumberedNameGenerator result = new NumberedNameGenerator(basePrefix, basePostfix, numDigits);

    // add chainNumber onto the output files
    if (chainNumber != null) {
      result.setPrefix(result.getPrefix() + chainNumber + "-");
    }

    return result;
  }
}
