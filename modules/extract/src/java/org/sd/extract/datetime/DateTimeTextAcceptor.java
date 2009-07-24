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
package org.sd.extract.datetime;


import org.sd.extract.DocText;
import org.sd.extract.TextAcceptor;
import org.sd.text.TermFinder;
import org.sd.util.StringUtil;

/**
 * TextAcceptor for the DateTimeExtractor.
 * <p>
 * @author Spence Koehler
 */
public class DateTimeTextAcceptor implements TextAcceptor {
  
  private static final DateTimeTextAcceptor INSTANCE = new DateTimeTextAcceptor();

  public static final DateTimeTextAcceptor getInstance() {
    return INSTANCE;
  }

  private TermFinder dateTermFinder;

  private DateTimeTextAcceptor() {
    this.dateTermFinder =
      new TermFinder("DateTerm", false,  // case-insensitive
                     new String[] {
                       "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec",
                       "Mon", "Tues", "Wed", "Thurs", "Fri", "Sat", "Sun",
                     });
  }

  /**
   * Determine whether the text is acceptable for further processing.
   *
   * @param docText  The candidate doc text to process.
   *
   * @return true if the text is acceptable for further processing; otherwise, false.
   */
  public boolean accept(DocText docText) {
    final String string = docText.getString();

    int numDigits = 0;
    int numSpaces = 0;
    for (StringUtil.StringContext iter = new StringUtil.StringContext(2, string); iter.hasNext(); ) {
      final int codePoint = iter.next();
      if (codePoint == ' ') {
        ++numSpaces;
        if (numSpaces > 5) return false;  // too many words
      }
      else if (isDigit(codePoint)) {
        final int prevc = iter.getPrevCodePoint(1);
        if (numDigits > 0 &&
            (prevc == '/' || prevc == ':' || prevc == '.' || prevc == '-' ||
             prevc == 24180 || prevc == 26376 || prevc == 26085 ||   // asian year, month, day
             prevc == 26178 || prevc == 20998)) {                    // asian hour, minute
          final int pprevc = iter.getPrevCodePoint(2);
          if (isDigit(pprevc)) return true;  // found digit:digit or digit/digit or digit.digit or digit-digit
        }
        ++numDigits;
      }
    }

    if (numDigits < 2) return false;  // not enough digits

    // if we get this far, we must find a weekday or month to accept.
    return dateTermFinder.hasPattern(string, TermFinder.ACCEPT_PARTIAL);
  }

  // NOTE: only interpreting roman numbers for now.
  private final boolean isDigit(int codePoint) {
    return (codePoint <= '9' && codePoint >= '0');
  }
}
