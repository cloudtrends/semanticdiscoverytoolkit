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
 * A break strategy for dates and times.
 * <p>
 * This augments the general break strategy such that forward slashes and periods are
 * always soft breaks.
 *
 * @author Spence Koehler
 */
public class DateTimeBreakStrategy extends GeneralBreakStrategy {
  
  private static final DateTimeBreakStrategy INSTANCE = new DateTimeBreakStrategy();

  public static final DateTimeBreakStrategy getInstance() {
    return INSTANCE;
  }

  private DateTimeBreakStrategy() {
  }

  /**
   * Augment the general strategy by marking forward slashes and periods
   * are always soft breaking.
   *
   * @return null if the next index will be index + 1; otherwise, return nextIndex - 1.
   */
  protected Integer setBreak(int index, int[] codePoints, Break[] result) {
    Integer retVal = null;
    final int codePoint = codePoints[index];

    if (codePoint == '/' || codePoint == '.') {
      result[index] = Break.SOFT_FULL;
    }
    else if (codePoint == 24180 ||   // asian year marker
             codePoint == 26376 ||   // asian month marker
             codePoint == 26085 ||   // asian day marker
//             codePoint == 21320      // asian am/pm marker
             codePoint == 26178 ||   // asian hour marker
             codePoint == 20998      // asian minute marker
      ) {
      result[index] = Break.HARD;
    }
    else if (codePoint == 21320 && index + 1 < result.length) {   // asian am/pm marker
      result[index] = Break.NONE;
      result[index + 1] = Break.SOFT_SPLIT;
      retVal = index + 1;
    }
    else {
      retVal = super.setBreak(index, codePoints, result);
    }
    return retVal;
  }

  public boolean equals(Object o) {
    boolean result = (this == o);

    if (!result && (o != null) && (o instanceof DateTimeBreakStrategy)) {
      result = true;
    }

    return result;
  }

  public int hashCode() {
    return 39;
  }
}
