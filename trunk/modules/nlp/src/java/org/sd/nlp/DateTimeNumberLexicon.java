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


import org.sd.util.DateUtil;
import org.sd.util.tree.Tree;
import org.sd.xml.XmlLite;

/**
 * A lexicon to classify numbers as date or time categories.
 * <p>
 * @author Spence Koehler
 */
public class DateTimeNumberLexicon extends AbstractLexicon {

  public static final String AMPM_ATTRIBUTE = "AMPM";
  public static final String AMPM_AM = "AM";
  public static final String AMPM_PM = "PM";

  public static final String NUMBER_ATTRIBUTE = "NUMBER";
  public static final String FLOAT_NUMBER_ATTRIBUTE = "FLOAT_NUMBER";
  public static final String PLAUSIBLE_YEAR_ATTRIBUTE = "PLAUSIBLE_YEAR";

  private int currentYear;
  private int minYear;
  private int currentCentury;

  private Category hour;
  private Category minute;
  private Category second;
  private Category day;
  private Category month;
  private Category year;
  private Category time;

  /**
   * Construct with the given parameters.
   *
   * @param categoryFactory  the category factory which needs HOUR, MINUTE,
   *                         SECOND, DAY, MONTH, YEAR categories defined.
   * @param minYear          the minimum year to accept as valid (gregorian AD).
   *                         if 0 or less, defaults to current year - 1.
   */
  public DateTimeNumberLexicon(CategoryFactory categoryFactory, int minYear) {
    super(null);

    this.hour = categoryFactory.getCategory("HOUR");
    this.minute = categoryFactory.getCategory("MINUTE");
    this.second = categoryFactory.getCategory("SECOND");
    this.day = categoryFactory.getCategory("DAY");
    this.month = categoryFactory.getCategory("MONTH");
    this.year = categoryFactory.getCategory("YEAR");
    this.time = categoryFactory.getCategory("TIME");

    this.currentYear = DateUtil.CURRENT_YEAR + 1;  // expect jvm to be restarted within 1 year
    if (minYear > 0) {
      this.minYear = minYear;
    }
    else {
      this.minYear = DateUtil.CURRENT_YEAR - 1;
    }
    this.currentCentury = (currentYear / 100) * 100;  // i.e. for 2000

    setMaxNumWords(2);  // "00A.M." -> "00A"  "M"
  }

  public DateTimeNumberLexicon(Tree<XmlLite.Data> lexiconNode, CategoryFactory categoryFactory, AbstractNormalizer normalizer) {
    this(categoryFactory, 0);

    final String myString = getAttribute("minYear");
    if (myString != null && myString.length() > 0) {
      this.minYear = Integer.parseInt(myString);
    }

    setLexiconNode(lexiconNode);
  }

  public Category getYearCategory() {
    return year;
  }

  public Category getMonthCategory() {
    return month;
  }

  public Category getDayCategory() {
    return day;
  }

  public Category getHourCategory() {
    return hour;
  }

  public Category getMinuteCategory() {
    return minute;
  }

  public Category getSecondCategory() {
    return second;
  }

  public Category getTimeCategory() {
    return time;
  }

  private final boolean isGoodYear(int value) {
    return value >= minYear && value <= currentYear;
  }

  protected final boolean isPlausibleYear(int value) {
    return getPlausibleYear(value) != null;
  }

  protected final Integer getPlausibleYear(int value) {
    Integer plausibleYear = null;

    if (isGoodYear(value)) {
      plausibleYear = value;
    }
    else if (value < 100) {

      // try i.e. as 19xx or 20xx. Check if either are valid.
      int prevCenturyYear = currentCentury - 100 + value;
      int thisCenturyYear = currentCentury + value;

      if (isGoodYear(prevCenturyYear)) {
        plausibleYear = prevCenturyYear;
      }
      else if (isGoodYear(thisCenturyYear)) {
        plausibleYear = thisCenturyYear;
      }
    }

    return plausibleYear;
  }

  /**
   * Determine whether the categories container already has category type(s)
   * that this lexicon would add.
   * <p>
   * NOTE: this is used to avoid calling "define" when categories already exist
   *       for the substring.
   *
   * @return true if this lexicon's category type(s) are already present.
   */
  protected boolean alreadyHasTypes(Categories categories) {
    return categories.hasType(year) || categories.hasType(month) || categories.hasType(day) ||
      categories.hasType(hour) || categories.hasType(minute) || categories.hasType(second);
  }

  /**
   * Define applicable categories in the subString.
   *
   * @param subString   The substring to define.
   * @param normalizer  The normalizer to use.
   */
  protected void define(StringWrapper.SubString subString, AbstractNormalizer normalizer) {
    final StringWrapper sw = subString.stringWrapper;

    // must start with a digit to apply.
    final int firstcp = sw.getCodePoint(subString.startPos);
    if (firstcp > '9' || firstcp < '0') return;

    // if 2 words, only a.m. or p.m. apply; otherwise, we must have just 1 word.
    final int numWords = subString.getNumWords();
    if (numWords > 2) return;

    boolean okay = (numWords == 1);
    int endPos = subString.endPos - 1;
    final int startPos = subString.startPos;
    boolean timeOnly = false;
    boolean dateOnly = false;
    String ampm = null;

    // check for a.m. or p.m.
    {
      int newEndPos = endPos;
      int lastcp = newEndPos >= 0 ? sw.getCodePoint(newEndPos--) : 0;
      if (newEndPos >= 0 && lastcp == '.') lastcp = sw.getCodePoint(newEndPos--);
      if (lastcp == 'm' || lastcp == 'M') {
        // must have a break, or [aA], or [pP] and then a break or digit preceding.
        int penucp = newEndPos >= 0 ? sw.getCodePoint(newEndPos--) : 0;
        if (newEndPos >= 0 && penucp == '.') penucp = sw.getCodePoint(newEndPos--);
        if (penucp == 'a' || penucp == 'A' || penucp == 'p' || penucp == 'P') {
          while (newEndPos > 0 && sw.getBreak(newEndPos - 1).skip()) --newEndPos;
          int precp = newEndPos >= 0 ? sw.getCodePoint(newEndPos) : 0;
          if (precp <= '9' && precp >= '0') {
            okay = true;      // now, it's okay if we have 2 words.
            timeOnly = true;  // we're looking at a number that is part of a time, not a date.
            endPos = newEndPos;
            ampm = (penucp == 'a' || penucp == 'A') ? AMPM_AM : AMPM_PM;
          }
        }
      }
    }

    if (!okay) return;

    ++endPos;  // leave endPos at end + 1

    // now we can try to interpret the number as an hour, minute, second, day, month, year
    boolean dayOnly = false;
    if (!timeOnly) {
      // find ordinal qualifiers "-st", "-nd", "-rd", "-th". these only come with dates
      int lastcp = endPos > 0 ? sw.getCodePoint(endPos - 1) : 0;
      if (endPos - 1 > 0) {
        int penucp = sw.getCodePoint(endPos - 2);
        if ((lastcp == 't' && penucp == 's') ||                     // -st
            (lastcp == 'd' && (penucp == 'n' || penucp == 'r')) ||  // -nd, -rd
            (lastcp == 'h' && penucp == 't')) {                     // -th
          dateOnly = true;
          dayOnly = true;
          endPos -= 2;
        }
      }
    }

    // make sure we backup over any breaks on the end
    while (endPos >= 0 && sw.getBreak(endPos - 1).skip()) --endPos;

    // now we can extract the number between startPos and endPos
    final StringBuilder digits = new StringBuilder();

    // should be all digits unless we're looking at seconds with a '.' of the form dd.d+
    int periodpos = -1;
    for (int index = startPos; index < endPos; ++index) {
      final int curcp = sw.getCodePoint(index);
      if (curcp > '9' || curcp < '0') {
        // nothing other than a period is valid; and that only for times
        if (curcp != '.' || dateOnly) return;

        // it is a period. need to have two digits before and 1+ digits after.
        if (index != 2 || index == endPos - 1) return;

        periodpos = index;
      }
      else {
        digits.appendCodePoint(curcp);
      }
    }

    // now we're just dealing with digits
    int number = 0;
    final String numberString = digits.toString();

    // we found dd.d+, classify as seconds, but only if the subString follows a colon or asian 'minute' marker.
    if (periodpos >= 0) {
      if (startPos > 0 && (sw.getCodePoint(startPos - 1) == ':' || sw.getCodePoint(startPos - 1) == 20998)) {
        subString.addCategory(second);
        subString.setDefinitive(true);
        subString.setAttribute(FLOAT_NUMBER_ATTRIBUTE, numberString);
        if (ampm != null) subString.setAttribute(AMPM_ATTRIBUTE, ampm);
        return;
      }
    }

    try {
      number = Integer.parseInt(numberString);
    }
    catch (NumberFormatException e) {
      // couldn't parse, (too big, etc.) so isn't a number we're trying to classify.
      return;
    }

    if (dayOnly) {
      subString.addCategory(day);
      subString.setDefinitive(true);
      subString.setAttribute(NUMBER_ATTRIBUTE, numberString);
      if (ampm != null) subString.setAttribute(AMPM_ATTRIBUTE, ampm);
      return;
    }

    int beforecp = (startPos > 0) ? sw.getCodePoint(startPos - 1) : 0;
    int aftercp = (subString.endPos < sw.length()) ? sw.getCodePoint(endPos) : 0;
    final boolean aacpb = (subString.endPos + 1 < sw.length()) ? sw.getBreak(endPos + 1).skip() : true;  // after aftercp break

    if (beforecp == 26178 || beforecp == 20998) beforecp = ':';  // change asian hour/minute markers to ':'
    if (aftercp == 26178 || aftercp == 20998) aftercp = ':';  // change asian hour/minute markers to ':'

    // preceded or followed by a colon ==> time only (actually, a date can be followed by a colon and a break)
    if (!dateOnly && (beforecp == ':' || (aftercp == ':' && !aacpb && beforecp != '/'))) {
      timeOnly = true;
    }

    // preceded or followed by a . / - , ==> date only
    if (!timeOnly &&
        (beforecp == '/' || beforecp == '-' || beforecp == '.' ||
         aftercp == '/' || aftercp == '-' || aftercp == '.')) {
      dateOnly = true;
    }

    boolean yearRange = false;
    boolean monthRange = false;
    boolean dayRange = false;
    boolean hourRange = false;
    boolean minuteRange = false;
    boolean secondRange = false;
    boolean fullTime = false;

    // let's classify the number
    // months: [1-12], days: [1-31], years: [2 digits or 4 digits up to the current year + 1]
    // hours: 1-12 or 00-23; minutes: [0]0-59; seconds [0]0-59
    final int numDigits = endPos - startPos;
    if (numDigits == 4) {
      // only a year can be 4 digits and it needs to be a valid year.
      if (timeOnly || !isGoodYear(number)) return;  // only a year can be 4 digits.
      yearRange = true;
      // note that we may still have a (2 digit) year.
    }
    else if (number == 0) {
      if (dateOnly) return;  // only times could be "0"
      if (numDigits == 2) hourRange = beforecp != ':' && aftercp == ':';  // military time
      minuteRange = beforecp == ':';
      secondRange = beforecp == ':';
    }
    else {
      final boolean oneTo12 = (number <= 12);  // months or hours
      final boolean oneTo31 = (number <= 31);  // days
      final boolean oneTo23 = (number <= 23);  // hours
      final boolean oneTo59 = (number <= 59);  // minutes or seconds

      if (!timeOnly || dateOnly) {
        final Integer plausibleYear = getPlausibleYear(number);
        if (plausibleYear != null && numDigits >= 2) {
          yearRange = true;
          if (plausibleYear != number) {
            subString.setAttribute(PLAUSIBLE_YEAR_ATTRIBUTE, plausibleYear.toString());
          }
        }
        monthRange = oneTo12;
        dayRange = oneTo31;
      }

      if (timeOnly || !dateOnly) {
        //
        // for hourRange, classify something like 5pm as a full time instead of:
        //
        // hourRange = (oneTo12 || (oneTo23 && numDigits == 2)) && beforecp != ':' &&
        //   (aftercp == ':' || aftercp == 'a' || aftercp == 'p' || aftercp == 'A' || aftercp == 'P');
        //
        if ((oneTo12 || (oneTo23 && numDigits == 2)) && beforecp != ':') {
          if (aftercp == 'a' || aftercp == 'p' || aftercp == 'A' || aftercp == 'P') {
            // we have something like 5pm -- classify it as a full TIME
            fullTime = true;
          }
          else if (aftercp == ':') {
            hourRange = true;
          }
        }

        minuteRange = (numDigits == 2) && oneTo59 && beforecp == ':';
        secondRange = (numDigits == 2) && oneTo59 && beforecp == ':';
      }
    }

    if (yearRange) subString.addCategory(year);
    if (monthRange) subString.addCategory(month);
    if (dayRange) subString.addCategory(day);
    if (hourRange) subString.addCategory(hour);
    if (minuteRange) subString.addCategory(minute);
    if (secondRange) subString.addCategory(second);
    if (fullTime) subString.addCategory(time);

    if (subString.getCategories() != null) {
      subString.setDefinitive(true);
    }

    subString.setAttribute(NUMBER_ATTRIBUTE, numberString);
    if (ampm != null) subString.setAttribute(AMPM_ATTRIBUTE, ampm);
  }
}
