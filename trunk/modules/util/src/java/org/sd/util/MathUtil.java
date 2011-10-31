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
package org.sd.util;


import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility methods for working with counters, etc.
 * <p>
 * @author Spence Koehler
 */
public class MathUtil {

  /** 
   * Compute the Logarithm base 2 of the given value.
   */
  public static final double log2(double d) {
    return Math.log(d) / Math.log(2.0);
  }

  /**
   * Compute the Logarithm base 10 of the given value.
   */
  public static final double log10(double d) {
    return Math.log(d) / Math.log(10.0);
  }
  
  /**
   * Rounds the double to the nearest integer.
   */
  public static int toInt(double d) {
    if (d > 0) d += 0.5;
    else if (d < 0) d -= 0.5;
    
    return (new Double(d)).intValue();
  }
  
  /**
   * Computes the entropy of two numbers.
   * @param pos number of "positive" instances
   * @param neg number of "negative" instances
   * @return the entropy as defined as <code>-P(pos) * log2(P(pos)) - P(neg) * log2(P(neg))</code>
   */
  public static double computeEntropy(int pos, int neg) {
    if (pos == 0 || neg == 0) return 0d;
    
    final double total = (double)pos + neg;
    final double posRatio = pos/total;
    final double negRatio = neg/total;
    final double entropy = -posRatio * log2(posRatio) - negRatio * log2(negRatio);
    
    return entropy;
  }
  
  /**
   * Computes the entropy of <code>N</code> numbers.
   * @param values numbers from which to compute entropy
   * @return the entropy as defined as <code>Sum(-P(values[i])) * log2(P(values[i])))</code>
   */
  public static double computeEntropy(int[] values) {
    double total = 0d;
    for (int i=0; i < values.length; i++) {
      total += values[i];
    }
    
    double entropy = 0d;
    for (int i=0; i < values.length; i++) {
      final double ratio = values[i] / total;
      entropy += ratio == 0 ? 0d : -ratio * log2(ratio);
    }
    
    return entropy;
  }

  /**
   * Normalize (scale) the value from one range to another.
   */
  public static final double normalize(double value, double fromLow, double fromHigh, double toLow, double toHigh) {
    final double pct = (value - fromLow) / (fromHigh - fromLow);
    return toLow + (pct * (toHigh - toLow));
  }

  /**
   * Given a string of the form "a,b,c-d,e,...", parse out the integers
   * represented including those within the (hyphenated) ranges.
   */
  public static final int[] parseIntegers(String integerString) {
    final List<Integer> integers = new ArrayList<Integer>();
    final String[] pieces = integerString.split(",");

    for (String piece : pieces) {
      collectIntegers(piece, integers);
    }

    final int[] result = new int[integers.size()];
    int index = 0;
    for (Integer integer : integers) {
      result[index++] = integer;
    }
    return result;
  }

  /** [-]a - [-]b */
  private static final Pattern RANGE_PATTERN = Pattern.compile("^\\s*(-?\\d+)\\s*-\\s*(-?\\d+)\\s*$");

  private static final void collectIntegers(String piece, List<Integer> collector) {
    final Matcher m = RANGE_PATTERN.matcher(piece);
    if (m.matches()) {
      final String startString = m.group(1);
      final String endString = m.group(2);
      final int startInt = Integer.parseInt(startString);
      final int endInt = Integer.parseInt(endString);

      if (startInt <= endInt) {
        // counting up
        for (int i = startInt; i <= endInt; ++i) {
          collector.add(i);
        }
      }
      else {
        // counting down
        for (int i = startInt; i >= endInt; --i) {
          collector.add(i);
        }
      }
    }
    else {
      final Integer integer = new Integer(piece.trim());
      collector.add(integer);
    }
  }

  /**
   * Get a string for the double rounded off to the given number of decimal
   * places.
   */
  public static final String doubleString(double d, int places) {
    final boolean neg = d < 0.0;
    d = Math.abs(d);

    final long l = Math.round(d * Math.pow(10, places));
    final char[] chars = Long.toString(l).toCharArray();
    final StringBuilder result = new StringBuilder();

    for (int i = 0; i < chars.length - places; ++i) {
      result.append(chars[i]);
    }

    if (chars.length - places <= 0) {
      result.append('0');
    }

    if (places == 0) {
      return result.toString();
    }

    result.append('.');
    for (int i = chars.length - places; i < chars.length; ++i) {
      result.append((i < 0) ? '0' : chars[i]);
    }

    if (neg) result.insert(0, '-');

    return result.toString();
  }

  public enum Justification { RIGHT, LEFT, CENTER };

  public static final String pad(String string, int width, Justification justification) {
    final StringBuilder result = new StringBuilder();
    result.append(string);
    pad(result, width, justification);
    return result.toString();
  }

  public static final void pad(StringBuilder result, int width, Justification justification) {
    final int len = result.length();
    final int space = width - len;

    if (space <= 0) return;

    switch (justification) {
      case LEFT :
        for (int i = 0; i < space; ++i) result.append(' ');
        break;
      case RIGHT :
        for (int i = 0; i < space; ++i) result.insert(0, ' ');
        break;
      case CENTER :
        final int half = (space >> 1);
        for (int i = 0; i < half; ++i) result.insert(0, ' ');
        final int rem = width - len - half;
        for (int i = 0; i < rem; ++i) result.append(' ');
        break;
    }
  }

  /**
   * Create a right-justified (space padded) string with the given
   * length.
   */
  public static final String integerString(int i, int places) {
    return integerString(i, places, ' ');
  }

  /**
   * Create a right-justified (c-padded) string with the given
   * length.
   */
  public static final String integerString(int i, int places, char c) {
    final StringBuilder result = new StringBuilder();

    result.append(Integer.toString(i));
    while (result.length() < places) result.insert(0, c);

    return result.toString();
  }

  /**
   * Create a right-justified (space padded) string with the given
   * length.
   */
  public static final String longString(long i, int places) {
    return longString(i, 10, places, ' ');
  }

  /**
   * Create a right-justified (0 padded) string with the given
   * length in the given radix.
   */
  public static final String longString(long i, int radix, int places) {
    return longString(i, radix, places, '0');
  }

  /**
   * Create a right-justified (c-padded) string with the given
   * length in the given radix.
   */
  public static final String longString(long i, int radix, int places, char c) {
    final StringBuilder result = new StringBuilder();

    result.append(Long.toString(i, radix));
    while (result.length() < places) result.insert(0, c);

    return result.toString();
  }

  /**
   * Get the number of digits for the num.
   */
  public static final int getNumDigits(int num) {
    int result = (num < 1) ? 1 : 0;

    while (num >= 1) {
      num /= 10;
      ++result;
    }

    return result;
  }

  /**
   * Add commas to a numeric string.
   */
  public static final String addCommas(String doubleString) {
    final StringBuilder result = new StringBuilder();

    int dotPos = doubleString.indexOf('.');
    if (dotPos < 0) {
      dotPos = doubleString.length();
    }
    else {
      result.append(doubleString.substring(dotPos));
    }

    boolean addingChars = true;
    int j = 0;
    for (int i = dotPos - 1; i >= 0; --i) {
      result.insert(0, doubleString.charAt(i));
      j = (j + 1) % 3;
      if ((j == 0) && (i - 1) >= 0) {
        final char c = doubleString.charAt(i - 1);
        if (addingChars && !((c >= '0') && (c <= '9'))) {
          addingChars = false;
        }

        result.insert(0, addingChars ? ',' : ' ');
      }
    }

    return result.toString();
  }

  /**
   * Decompose millis into 0:days, 1:hours, 2:minutes, 3:seconds, 4:millis.
   */
  public static final int[] decomposeMillis(long millis) {
    final int[] result = new int[5];

    result[4] = (int)(millis % 1000);
    long seconds = (millis / 1000);
    if (seconds == 0) return result;

    result[3] = (int)(seconds % 60);
    long minutes = (seconds / 60);
    if (minutes == 0) return result;

    result[2] = (int)(minutes % 60);
    long hours = (minutes / 60);
    if (hours == 0) return result;

    result[1] = (int)(hours % 24);
    long days = (hours / 24);
    if (days == 0) return result;

    result[0] = (int)days;

    return result;
  }

  public static final DateFormat dateFormat = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);

  /**
   * Get a short representation of the date represented by the given millis.
   */
  public static final String dateString(long millis) {
    return dateString(millis, dateFormat);
  }

  /**
   * Get a representation of the date represented by the given millis.
   */
  public static final String dateString(long millis, DateFormat dateFormat) {
    return dateFormat.format(new Date(millis));
  }

  private static final String[] UNITS = new String[]{"d", "h", "min", "sec", "ms"};
  private static final int DAY = 0;
  private static final int HOUR = 1;
  private static final int MIN = 2;
  private static final int SEC = 3;
  private static final int MILLIS = 4;

  /**
   * Get a string for the millis with appropriate units.
   * <p>
   * If breakApart is true, the decomposed time pieces will be shown separately.
   */
  public static final String timeString(long millis, boolean breakApart) {
    final StringBuilder result = new StringBuilder();

    final int[] decomp = decomposeMillis(millis);
    int firstDecompIndex = 0;
    for (int i = 0; i < decomp.length; ++i) {
      if (decomp[i] > 0 || result.length() > 0) {

        if (result.length() == 0) firstDecompIndex = i;

        // for h, min, sec, add a leading 0 if needed
        if (i > 0 && i < 4 && decomp[i] < 10) result.append('0');

        // add the value
        result.append(decomp[i]);

        // add the units or punctuation
        if (breakApart) {
          result.append(UNITS[i]);
          if ((i + 1) < decomp.length) result.append(' ');
        }
        else {  //!breakApart
          if ((i + 2) < decomp.length) result.append(':');
          else if ((i + 1) < decomp.length) result.append('.');
        }
      }
    }

    // ok, go ahead and add the units after all
    if (!breakApart) result.append(' ').append(UNITS[firstDecompIndex]);

    return result.toString();
  }

  /**
   * Get a string for the rate with appropriate units.
   *
   * @param rate  The rate in units per millis.
   * @param places  The number of places to show with the rate.
   */
  public static final String rateString(double rate, int places) {
    final StringBuilder result = new StringBuilder();

    double orig = rate;

    int unit = MILLIS;
    if (rate > 0) {
      if (rate < 1) {
        rate *= 1000;
        unit = SEC;

        if (rate < 1) {
          rate *= 60;
          unit = MIN;

          if (rate < 1) {
            rate *= 60;
            unit = HOUR;

            if (rate < 1) {
              rate *= 24;
              unit = DAY;
            }
          }
        }
      }
    }

    result.append(doubleString(rate, places)).append('/').append(UNITS[unit]);

    return result.toString();
  }

  /**
   * Auxiliary for parseDate.
   */
  private static final Map<String, Integer> MONTH_MAP = new HashMap<String, Integer>();
  static {
    MONTH_MAP.put("Jan", Calendar.JANUARY);
    MONTH_MAP.put("Feb", Calendar.FEBRUARY);
    MONTH_MAP.put("Mar", Calendar.MARCH);
    MONTH_MAP.put("Apr", Calendar.APRIL);
    MONTH_MAP.put("May", Calendar.MAY);
    MONTH_MAP.put("Jun", Calendar.JUNE);
    MONTH_MAP.put("Jul", Calendar.JULY);
    MONTH_MAP.put("Aug", Calendar.AUGUST);
    MONTH_MAP.put("Sep", Calendar.SEPTEMBER);
    MONTH_MAP.put("Oct", Calendar.OCTOBER);
    MONTH_MAP.put("Nov", Calendar.NOVEMBER);
    MONTH_MAP.put("Dec", Calendar.DECEMBER);
  }

  /**
   * Parse a string from java.util.Date's toString method into to a
   * GregorianCalendar object.
   */
  public static final GregorianCalendar parseGregorianCalendar(String dateString) {
    final String[] pieces = dateString.trim().split("[ :]");

    if (pieces.length != 8) return null;

    final int month = MONTH_MAP.get(pieces[1]);      // Jan, Feb, Mar, Apr, May, Jun, Jul, Aug, Sep, Oct, Nov, Dec
    final int day = Integer.parseInt(pieces[2]);     // 01-31
    final int hour = Integer.parseInt(pieces[3]);    // 00-23
    final int minute = Integer.parseInt(pieces[4]);  // 00-59
    final int second = Integer.parseInt(pieces[5]);  // 00-61
    final int year = Integer.parseInt(pieces[7]);

    return new GregorianCalendar(year, month, day, hour, minute, second);
  }

  /**
   * Parse a string from java.util.Date's toString method back to a
   * Date object.
   */
  public static final Date parseDate(String dateString) {
    final GregorianCalendar gc = parseGregorianCalendar(dateString);
    return gc == null ? null : gc.getTime();
  }

  private static final Pattern BYTE_PATTERN = Pattern.compile("^.*?(\\d+)([BKMGT]).*$");
  private static final Map<String, Long> BYTE_CONVS = new HashMap<String, Long>();
  static {
    long conv = 1L;

    BYTE_CONVS.put("B", conv);
    conv *= 1024;
    BYTE_CONVS.put("K", conv);
    conv *= 1024;
    BYTE_CONVS.put("M", conv);
    conv *= 1024;
    BYTE_CONVS.put("G", conv);
    conv *= 1024;
    BYTE_CONVS.put("T", conv);
  }

  /**
   * Given a string of the form xY, where x is a number and Y is
   * <ul>
   * <li><empty> -- if x is in bytes.</li>
   * <li>B -- if x is in bytes.</li>
   * <li>K -- if x is in kilobytes.</li>
   * <li>M -- if x is in megabytes.</li>
   * <li>G -- if x is in gigabytes.</li>
   * <li>T -- if x is in terabytes.</li>
   * </ul>
   * Return the number of bytes represented.
   *
   * @return the number of bytes parsed from the string or -1 if malformed.
   */
  public static final long parseBytes(String kbString) {
    long result = -1L;

    String numberString = null;
    String unitsString = null;

    final Matcher matcher = BYTE_PATTERN.matcher(kbString.toUpperCase());
    if (matcher.matches()) {
      numberString = matcher.group(1);
      unitsString = matcher.group(2);
    }
    else {
      numberString = kbString;
      unitsString = "B";
    }

    Long conv = BYTE_CONVS.get(unitsString);
    if (conv != null) {
      try {
        result = Long.parseLong(numberString);
        result *= conv;
      }
      catch (Exception e) {
        // illegal format or null pointer; either way -- return -1.
      }
    }
    // else, illegal unit specification -- return -1.

    return result;
  }
}
