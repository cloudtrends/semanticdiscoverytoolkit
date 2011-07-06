/*
    Copyright 2011 Semantic Discovery, Inc. (www.semanticdiscovery.com)

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
package org.sd.util;


import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Console utility to convert textual dates to universal time and back.
 * <p>
 * @author Spence Koehler
 */
public class TimeConverterUtil {

  public static final Pattern LONG_PATTERN = Pattern.compile("^\\d+$");


  /**
   * Determine whether the string could contain a universal time in millis.
   */
  public static final boolean isMillis(String string) {
    final Matcher longMatcher = LONG_PATTERN.matcher(string);
    return longMatcher.matches();
  }

  /**
   * Get a date object for the given time in millis.
   */
  public static final Date millis2date(String millis) {
    return new Date(Long.parseLong(millis));
  }

  /**
   * Get a date object for the given time in millis.
   */
  public static final Date millis2date(long millis) {
    return new Date(millis);
  }

  /**
   * Convert a date string (from date.toString()) to a date instance.
   * <p>
   * Date strings are of the form:
   * <ul><li>DOW Mon dd hh:mm:ss TMZ yyyy</li></ul>
   * Where
   * <ul>
   * <li>"DOW" is the week day (text, ignored)</li>
   * <li>"Mon" is the month (3 letter text)</li>
   * <li>"dd" is the day (digits)</li>
   * <li>"hh" is the hour (digits)</li>
   * <li>"mm" is the minutes (digits)</li>
   * <li>"ss" is the seconds (digits)</li>
   * <li>"TMZ" is the timezone (3 letter text, ignored -- current timezone is used)</li>
   * <li>"yyyy" is the year (4 digits)</li>
   * </ul>
   * For example: "Tue Jun 28 11:53:04 MDT 2011"
   */
  public static final Date dateString2date(String dateString) {
    return MathUtil.parseDate(dateString);
  }

  /**
   * Convert a date string (from date.toString()) to a date instance.
   * <p>
   * Date strings are of the form:
   * <ul><li>DOW Mon dd hh:mm:ss TMZ yyyy</li></ul>
   * Where
   * <ul>
   * <li>"DOW" is the week day (text, ignored)</li>
   * <li>"Mon" is the month (3 letter text)</li>
   * <li>"dd" is the day (digits)</li>
   * <li>"hh" is the hour (digits)</li>
   * <li>"mm" is the minutes (digits)</li>
   * <li>"ss" is the seconds (digits)</li>
   * <li>"TMZ" is the timezone (3 letter text, ignored -- current timezone is used)</li>
   * <li>"yyyy" is the year (4 digits)</li>
   * </ul>
   * For example: "Tue Jun 28 11:53:04 MDT 2011"
   *
   * @return the universal time for the date or -1 if unable to parse.
   */
  public static final long dateString2millis(String dateString) {
    final Date date = MathUtil.parseDate(dateString);
    return date == null ? -1L : date.getTime();
  }


  public static void main(String[] args) {
    for (String arg : args) {
      if (isMillis(arg)) {
        // convert universal time to date string
        System.out.println(arg + "\t" + millis2date(arg));
      }
      else {
        // convert date string to universal time
        System.out.println(arg + "\t" + dateString2date(arg).getTime());
      }
    }
  }
}
