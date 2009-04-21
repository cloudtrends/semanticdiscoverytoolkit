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


import org.sd.extract.Extraction;
import org.sd.extract.Interpretation;
import org.sd.fsm.Token;
import org.sd.fsm.impl.GrammarToken;
import org.sd.nlp.DateTimeNumberLexicon;
import org.sd.nlp.Parser;
import org.sd.nlp.StringWrapper;
import org.sd.util.DateUtil;
import org.sd.util.LineBuilder;
import org.sd.util.tree.Tree;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;

/**
 * An interpretation for a date/time entity item.
 * <p>
 * @author Spence Koehler
 */
public class DateTimeInterpretation implements Interpretation {
  
  // disabled using dateFormat because of buggy output.
  private static final boolean USE_DATE_FORMAT = false;

  private GregorianCalendar calendar;

  private Extraction extraction;
  private Parser.Parse parse;
  private Calendar currentCalendar;  // for reference and assumptions on missing info
  private boolean doGuessYear;

  private boolean hasDate;
  private boolean hasTime;
  private Integer weekday;
  private Integer year;
  private boolean guessedYear;
  private int parsedYear;
  private String standardString;
  private boolean useTimeZone;

  private Integer day;
  private Integer month;
  private Integer hour;
  private Integer minute;
  private Integer second;

  private double score;

  private DateFormat dateFormat = DateFormat.getDateInstance();
  private DateFormat timeFormat = DateFormat.getTimeInstance();
  private DateFormat dtFormat = DateFormat.getDateTimeInstance();

  DateTimeInterpretation(Extraction extraction, Parser.Parse parse, Calendar currentCalendar, boolean doGuessYear) {
    this(extraction, parse, currentCalendar, doGuessYear, false);
  }

  DateTimeInterpretation(Extraction extraction, Parser.Parse parse, Calendar currentCalendar, boolean doGuessYear, boolean useTimeZone) {
    this.extraction = extraction;
    this.parse = parse;
    this.currentCalendar = currentCalendar;
    this.doGuessYear = doGuessYear;
    this.guessedYear = false;
    this.standardString = null;

    this.useTimeZone = useTimeZone;

    initializeCalendar();
  }

  private final void initializeCalendar() {
    Integer year = null;
    Integer month = null;
    Integer day = null;
    Integer weekday = null;

    Integer hour = null;
    Integer minute = null;
    Integer second = null;
    String secondString = null;

    int ampm = 0;
    Integer tz = null;

    this.score = 0.0;

    for (Tree<Token> leaf : parse.getLeaves()) {
      boolean isDate = false;

      // unravel leaf (input) token
      final StringWrapper.SubString subString = Parser.getSubString(leaf);
      final String inputString = subString.originalSubString;

      // look for NUMBER and AMPM attributes
      final String numberString = subString.getAttribute(DateTimeNumberLexicon.NUMBER_ATTRIBUTE);
      final int number = (numberString == null) ? -1 : Integer.parseInt(numberString);
      final String ampmString = subString.getAttribute(DateTimeNumberLexicon.AMPM_ATTRIBUTE);
      if (ampmString != null) {ampm = getAmPm(ampmString); isDate = false;}
//      final String normalized = subString.getAttribute(GenericLexicon.NORMALIZED_ATTRIBUTE);

      // unravel grammar (category) token
      final GrammarToken categoryToken = (GrammarToken)leaf.getParent().getData();
      final String category = categoryToken.getToken();

      if (category.equals("YEAR")) {
        final String plausibleYearString = subString.getAttribute(DateTimeNumberLexicon.PLAUSIBLE_YEAR_ATTRIBUTE);
        year = (plausibleYearString != null) ? Integer.parseInt(plausibleYearString) : number;
        parsedYear = number;
        score += 25.0;
        isDate = true;
      }
      else if (category.equals("MONTH")) {month = number - 1; score += 20.0; isDate = true;}
      else if (category.equals("DAY")) {day = number; score += 15.0; isDate = true;}
      else if (category.equals("WEEKDAY")) {weekday = number; isDate = true;}
      else if (category.equals("HOUR") || category.equals("TIME")) {hour = number; score += 20.0; isDate = false;}
      else if (category.equals("MINUTE")) {minute = number; score += 5.0; isDate = false;}
      else if (category.equals("SECOND")) {
        if (number >= 0) {
          second = number;
          score += 5.0;
        }
        else {
          final String floatNumberString = subString.getAttribute(DateTimeNumberLexicon.FLOAT_NUMBER_ATTRIBUTE);
          if (floatNumberString != null) {
            secondString = floatNumberString;
          }
        }
        isDate = false;
      }
      else if (category.equals("AMPM")) {ampm = getAmPm(inputString); isDate = false;}
      else if (category.equals("TZ") && numberString != null) {tz = number; isDate = false;}

      if (isDate) this.hasDate = true;
      if (!isDate) this.hasTime = true;
      this.weekday = weekday;
    }

    this.calendar = new GregorianCalendar();
    this.calendar.clear();  // start out empty instead of with current date/time
    this.calendar.setTimeZone(TimeZone.getTimeZone("GMT"));

    year = fixYear(year, month, day);

    // todo: this can probably be done more properly by extracting the TimeZone object from a standardly formatted string
    //       and then setting the calendar's time zone
    if (useTimeZone && tz != null) calendar.set(Calendar.ZONE_OFFSET, tz * 3600000);
    if (year != null) {this.year = year; calendar.set(Calendar.YEAR, year);}
    if (month != null) {this.month = month; calendar.set(Calendar.MONTH, month);}
    if (day != null) {this.day = day; calendar.set(Calendar.DAY_OF_MONTH, day);}
    if (hour != null) {
      this.hour = hour;
      if (ampm == 0) {
        calendar.set(Calendar.HOUR_OF_DAY, hour);
      }
      else {
        calendar.set(Calendar.HOUR, (hour == 12) ? 0 : hour);
        calendar.set(Calendar.AM_PM, (ampm == 1) ? Calendar.AM : Calendar.PM);
      }
    }
    if (minute != null) {this.minute = minute; calendar.set(Calendar.MINUTE, minute);}
    if (second != null) {
      if (second != null) {
        this.second = second;
        calendar.set(Calendar.SECOND, second);
      }
      else if (secondString != null) {
        final String[] pieces = secondString.split("\\.");
        final int sval = Integer.parseInt(pieces[0]);
        int mval = Integer.parseInt(pieces[1]);
        for (int i = pieces[1].length(); i < 3; ++i) mval *= 10;

        this.second = sval;
        calendar.set(Calendar.SECOND, sval);
        calendar.set(Calendar.MILLISECOND, mval);
      }
    }
  }

  private final Integer fixYear(Integer year, Integer month, Integer day) {
    // if no explicit year and we're allowed to guess, base the year on the
    // current calendar.
    if (year == null && doGuessYear && month != null) {  
      final int currentYear = currentCalendar.get(Calendar.YEAR);
      final int currentMonth = currentCalendar.get(Calendar.MONTH);

      if (month == currentMonth) {
        if (day != null) {
          final int currentDay = currentCalendar.get(Calendar.DAY_OF_MONTH);
          if (day <= currentDay) {
            year = currentYear;
          }
          else {
            year = currentYear - 1;
          }
        }
        else {
          year = currentYear;
        }        
      }
      else if (month < currentMonth) {
        year = currentYear;
      }
      else {
        year = currentYear - 1;
      }
      guessedYear = true;
    }

    return year;
  }

  /**
   * Get the extraction that contains this interpretation.
   */
  public Extraction getExtraction() {
    return extraction;
  }

  /**
   * Set or update the extraction backpointer.
   */
  public void setExtraction(Extraction extraction) {
    // NOTE: assuming we don't need to update the parse. an extraction may have multiple parses, so we don't know which to update!
    this.extraction = extraction;
  }

  /**
   * Get this interpretation's data as a pipe-delimited string of fields
   * specific to the type and implementation.
   * <p>
   * This implementation gives integer values for: year|month|day|hour|minute|second|ampm|timezone
   * <ul>
   * <li>"-1" identifies "empty" values.</li>
   * <li>year is the year (i.e. 2007)</li>
   * <li>month ranges from 0=January to 11=December</li>
   * <li>day ranges from 1 to N</li>
   * <li>hour ranges from 0 to 11</li>
   * <li>ampm: 1=AM; 2=PM</li>
   * <li>timezone is the millis offset from GMT (i.e. 28800000 for MDT)</li>
   * </ul>
   */
  public String getFieldsString() {
    final LineBuilder result = new LineBuilder();

    result.append(hasYear() ? calendar.get(Calendar.YEAR) : -1);
    result.append(hasMonth() ? calendar.get(Calendar.MONTH) : -1);
    result.append(hasDay() ? calendar.get(Calendar.DAY_OF_MONTH) : -1);

    result.append(hasHour() ? calendar.get(Calendar.HOUR) : -1);
    result.append(hasMinute() ? calendar.get(Calendar.MINUTE) : -1);
    result.append(hasSecond() ? calendar.get(Calendar.SECOND) : -1);

    result.append(hasAmPm() ? calendar.get(Calendar.AM_PM) : -1);
    result.append(hasTimeZone() ? calendar.get(Calendar.ZONE_OFFSET) : -1);

    return result.toString();
  }

  /**
   * Get a key identifying the structure (not content) of this interpretation.
   */
  public String getStructureKey() {
    String result = null;

    if (parse != null) {
      result = parse.getParseKey();
    }

    return result;
  }

  public boolean isValid() {
    boolean result = hasDate || hasTime;

    if (result) {
      result = hasValidParse();
    }

    return result;
  }

  /**
   * Determine whether this interpretation's parse is valid.
   * <p>
   * Currently, this checks for the condition of parsing only two from month,
   * day, year when there appears to be three constituents.
   */
  public boolean hasValidParse() {
    boolean result = true;

    // check for condition where a 2 digit year was interpreted
    // as a current year, but the unparsed input shows evidence
    // that there was more date-like data to parse.
    if (hasYear() && (parsedYear != year)) {
      final String unparsedInput = parse.getUnparsedInput(null);
      if (unparsedInput != null && unparsedInput.length() > 1) {
        final char c = unparsedInput.charAt(0);
        if (c == '/' || c == '-' || c == '.') {
          result = false;
        }
      }
    }

    return result;
  }

  /**
   * This returns a weighted score for the completeness of the interpretation.
   * Each date and time part carries the following weights:
   *     year 25
   *    month 20
   *      day 15
   *     hour 20
   *   minute 10
   *   second  5
   *   millis  5
   */
  public double getScore() {
    return score;
  }

  public Parser.Parse getParse() {
    return parse;
  }

  /**
   * Get the input that lead to this interpretation.
   * <p>
   * Note that this is the substring of input from the full input
   * line.
   */
  public StringWrapper.SubString getInput() {
    return parse.getParsedInput();
  }

  /**
   * Get an unambiguous string representing this interpretation's content.
   */
  public String asString() {
    return getStandardString();
  }

  /**
   * Get a standardized form for the content of an entity item.
   */
  public String getStandardString() {
    if (standardString == null) {
      if (USE_DATE_FORMAT) {
        final DateFormat df = (hasDate && hasTime) ? dtFormat : (hasDate ? dateFormat : timeFormat);
        standardString = df.format(calendar.getTime());
      }
      else {
        final StringBuilder result = new StringBuilder();

        if (hasDate) {
          result.append(
            DateUtil.formatDate(
              hasDay() ? new Integer(calendar.get(Calendar.DAY_OF_MONTH)) : null,
              hasMonth() ? new Integer(calendar.get(Calendar.MONTH)) : null,
              hasYear() ? new Integer(calendar.get(Calendar.YEAR)) : null));
        }
        if (hasTime) {
          if (hasDate) result.append(' ');
          result.append(
            DateUtil.formatTime(
              hasHour() ? new Integer(calendar.get(Calendar.HOUR)) : null,
              hasMinute() ? new Integer(calendar.get(Calendar.MINUTE)) : null,
              hasSecond() ? new Integer(calendar.get(Calendar.SECOND)) : null,
              hasAmPm() ? new Integer(calendar.get(Calendar.AM_PM)) : null,
              hasTimeZone() ? new Integer(calendar.get(Calendar.ZONE_OFFSET) / 3600000) : null));
        }

        standardString = result.toString();
      }
    }

    return standardString;
  }

  /**
   * Determine whether this interpretation collides with another.
   * <p>
   * If two interpretations do not collide then they can simultaneously
   * co-exist and cooperatively define an entity. For example a date
   * and a time would not collide but a date and a date would.
   */
  public boolean collidesWith(Interpretation other) {
    if (!(other instanceof DateTimeInterpretation)) return false;
    final DateTimeInterpretation otherDateTimeInterp = (DateTimeInterpretation)other;
    return (hasDate && otherDateTimeInterp.hasDate) || (hasTime && otherDateTimeInterp.hasTime);
  }

  public long getTimeInMillis() {
    return calendar.getTimeInMillis();
  }

  public boolean hasWeekday() {
    return weekday != null;
  }

  public boolean hasDate() {
    return hasDate;
  }

  public boolean hasTime() {
    return hasTime;
  }

  /**
   * True if this has year, month, and day.
   */
  public boolean hasCompleteDate() {
    return hasDate && calendar.isSet(Calendar.YEAR) && calendar.isSet(Calendar.MONTH) && calendar.isSet(Calendar.DAY_OF_MONTH);
  }

  public boolean hasDay() {
    return this.day != null;  //calendar.isSet(Calendar.DAY_OF_MONTH);
  }

  public Integer getDay() {
    return this.day;  //calendar.isSet(Calendar.DAY_OF_MONTH) ? new Integer(calendar.get(Calendar.DAY_OF_MONTH)) : null;
  }

  public boolean hasMonth() {
    return this.month != null;  //calendar.isSet(Calendar.MONTH);
  }

  public Integer getMonth() {
    return this.month;  //calendar.isSet(Calendar.MONTH) ? new Integer(calendar.get(Calendar.MONTH)) : null;
  }

  public boolean hasYear() {
    return year != null;  // calendar.isSet(Calendar.YEAR) returns true and 1970 when we don't set it.
  }

  public boolean guessedYear() {
    return guessedYear;
  }

  public Integer getYear() {
    return year;
  }

  /**
   * True if this has hours, minutes, and seconds.
   */
  public boolean hasCompleteTime() {
    return hasDate && calendar.isSet(Calendar.HOUR) && calendar.isSet(Calendar.MINUTE) && calendar.isSet(Calendar.SECOND);
  }

  public boolean hasHour() {
    return this.hour != null;  //calendar.isSet(Calendar.HOUR);
  }

  public Integer getHour() {
    return this.hour;  //calendar.isSet(Calendar.HOUR) ? new Integer(calendar.get(Calendar.HOUR)) : null;
  }

  public boolean hasMinute() {
    return this.minute != null;  //calendar.isSet(Calendar.MINUTE);
  }

  public Integer getMinute() {
    return this.minute;  //calendar.isSet(Calendar.MINUTE) ? new Integer(calendar.get(Calendar.MINUTE)) : null;
  }

  public boolean hasSecond() {
    return this.second != null;  //calendar.isSet(Calendar.SECOND);
  }

  public Integer getSecond() {
    return this.second;  //calendar.isSet(Calendar.SECOND) ? new Integer(calendar.get(Calendar.SECOND)) : null;
  }

  public boolean hasAmPm() {
    return calendar.isSet(Calendar.AM_PM);
  }

  public Integer getAmPm() {
    return calendar.get(Calendar.AM_PM);
  }

  public boolean hasTimeZone() {
    return calendar.isSet(Calendar.ZONE_OFFSET);
  }

  public Integer getTimeZone() {
    return calendar.get(Calendar.ZONE_OFFSET);
  }

  public Calendar getCalendar() {
    return calendar;
  }

  public Date getDate() {
    return calendar.getTime();
  }

  public boolean equals(Object o) {
    boolean result = (this == o);

    if (!result && o instanceof DateTimeInterpretation) {
      final DateTimeInterpretation other = (DateTimeInterpretation)o;
      return calendar.equals(other.calendar);
    }

    return result;
  }

  public int hashCode() {
    return calendar.hashCode();
  }

  private final int getAmPm(String ampmString) {
    // look for 'A', 'P', 'a', or 'p' and set AmPm value accordingly.
    int value = 0;

    if (ampmString.length() > 0) {
      final int cp = ampmString.codePointAt(0);
      if (cp == 19978) {   // asian AM
        value = 1;
      }
      else if (cp == 19979) {  // asian PM
        value = 2;
      }
      else {
        ampmString = ampmString.toUpperCase();
        if (ampmString.indexOf('A') >= 0) {
          value = 1;
        }
        else if (ampmString.indexOf('P') >= 0) {
          value = 2;
        }
      }
    }

    return value;
  }
}
