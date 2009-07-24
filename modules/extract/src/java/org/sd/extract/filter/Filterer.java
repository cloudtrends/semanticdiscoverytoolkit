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
package org.sd.extract.filter;


/**
 * Utility class to apply a sequence of filters.
 * <p>
 * @author Spence Koehler
 */
public class Filterer {

  /**
   * Determine whether all filterables should be accepted.
   *
   * @return true unless one filterable is not accepted.
   */
  public static final boolean acceptAll(Filterable[] filterables) {
    boolean result = true;

    for (Filterable filterable : filterables) {
      if (!filterable.accept()) {
        result = false;
        break;
      }
    }

    return result;
  }

  /**
   * Determine whether any filterables should be accepted.
   *
   * @return true if one filterable is accepted.
   */
  public static final boolean acceptOne(Filterable[] filterables) {
    boolean result = false;

    for (Filterable filterable : filterables) {
      if (filterable.accept()) {
        result = true;
        break;
      }
    }

    return result;
  }
}
