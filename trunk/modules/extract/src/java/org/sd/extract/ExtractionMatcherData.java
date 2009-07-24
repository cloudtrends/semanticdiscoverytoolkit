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


import java.util.regex.Matcher;

/**
 * Extraction data for holding a matcher from a regex pattern.
 * <p>
 * @author Spence Koehler
 */
public final class ExtractionMatcherData extends AbstractExtractionData {
  
  private Matcher matcher;

  public ExtractionMatcherData(Matcher matcher) {
    this.matcher = matcher;
  }

  public ExtractionMatcherData asMatcherData() {
    return this;
  }

  public Matcher getMatcher() {
    return matcher;
  }

  public String toString() {
    final StringBuilder result = new StringBuilder();

    if (matcher.groupCount() > 1) {
      for (int i = 1; i <= matcher.groupCount(); ++i) {
        if (result.length() > 0) result.append(',');
        result.append(i).append(':').append(matcher.group(i));
      }
    }
    else {
      result.append(matcher.group());
    }

    return result.toString();
  }

  /**
   * The extracted group is the first matched group if there is only one matched group
   * or the full input subsequence matched.
   */
  public String getExtractedString() {
    return matcher.groupCount() > 1 ? matcher.group() : matcher.group(1);
  }
}
