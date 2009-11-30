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
package org.sd.xml;


import java.util.ArrayList;
import java.util.List;
import org.sd.util.tree.NodePath;


/**
 * A NodePath.PatternSplitter implementation for use with the XmlDataMatcher.
 * <p>
 * This implementation is necessary for use with the XmlDataMatcher in XPaths
 * because of the regex pattern matching that is allowed for attributes and
 * text that render naive splitting on periods alone faulty.
 * <p>
 * Still, attribute name or value patterns that have ',' or '=' characters will
 * lead to problems.
 *
 * @author Spence Koehler
 */
public class XmlPatternSplitter implements NodePath.PatternSplitter {
  
  /**
   * Split out the path constituents of the form recognized by XmlDataMatcher,
   * taking into account delimiters that may exist within the regex patterns.
   */
  public String[] split(String patternString) {
    final List<String> result = new ArrayList<String>();

    int startPos = 0;
    int periodPos = nextPeriodPos(patternString, startPos);
    while (periodPos >= 0) {
      final String patternConstituent = patternString.substring(startPos, periodPos);
      result.add(patternConstituent);

      startPos = periodPos + 1;
      periodPos = nextPeriodPos(patternString, startPos);
    }
    result.add(patternString.substring(startPos));

    return result.toArray(new String[result.size()]);
  }

  private final int nextPeriodPos(String patternString, int startPos) {
    int result = patternString.indexOf('.', startPos);

    while (result >= 0 && isInPattern(patternString, startPos, result)) {
      result = patternString.indexOf('.', result + 1);
    }

    return result;
  }

  /**
   * Determine whether the position is within a regex pattern.
   */
  private final boolean isInPattern(String patternString, int startPos, int position) {
    boolean result = false;

    // return true if find '{' at or after startPos and before position,
    // with '~' after '{' and before position, and with '}' after position
    final int lcbpos = patternString.lastIndexOf('{', position - 1);
    if (lcbpos >= startPos) {
      final int tpos = patternString.lastIndexOf('~', position - 1);
      if (tpos > lcbpos) {
        final int rcbpos = patternString.indexOf('}', position + 1);

        if (rcbpos > 0) {  // there is a right curly brace after position
          final int lcbpos2 = patternString.indexOf('{', lcbpos + 1);
          // and it isn't a match for a later left curly brace
          if (lcbpos2 < 0 || lcbpos2 > rcbpos) {
            // so position is within curly braces and after a pattern marker
            result = true;
          }
        }
      }
    }

    // or return true if find "/~" at or after startPos and before position
    if (!result) {
      final int spos = patternString.lastIndexOf("/~", position - 1);
      if (spos >= startPos) {
        result = true;
      }
    }

    return result;
  }
}
