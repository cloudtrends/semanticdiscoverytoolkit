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


import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * JUnit Tests for the DateTimeNumberLexicon class.
 * <p>
 * @author Spence Koehler
 */
public class TestDateTimeNumberLexicon extends TestCase {

  public TestDateTimeNumberLexicon(String name) {
    super(name);
  }
  
  private final DateTimeNumberLexicon buildLexicon(int minYear) {
    final CategoryFactory categoryFactory = new CategoryFactory();
    categoryFactory.defineCategory("YEAR", false);
    categoryFactory.defineCategory("MONTH", false);
    categoryFactory.defineCategory("DAY", false);
    categoryFactory.defineCategory("HOUR", false);
    categoryFactory.defineCategory("MINUTE", false);
    categoryFactory.defineCategory("SECOND", false);
    categoryFactory.defineCategory("TIME", false);
    return new DateTimeNumberLexicon(categoryFactory, minYear);
  }

  // verify that the tokenIndex-th shortest token from input has the indicated
  // categories after having been run through the lexicon.
  //
  // if tokenIndex is negative, then grow the tokenIndex-th token one longer
  //
  private final void verify(DateTimeNumberLexicon lexicon, String input, int tokenIndex,
                            boolean hasYear, boolean hasMonth, boolean hasDay,
                            boolean hasHour, boolean hasMinute, boolean hasSecond) {
    
    final StringWrapper stringWrapper = new StringWrapper(input, DateTimeBreakStrategy.getInstance());
    StringWrapper.SubString subString = stringWrapper.getShortestSubString(0);
    int num = (tokenIndex < 0) ? -tokenIndex : tokenIndex;
    for (int i = 0; i < num; ++i) subString = subString.getNextShortestSubString();
    if (tokenIndex < 0) subString = subString.getLongerSubString();

    lexicon.lookup(subString);
    final Categories categories = subString.getCategories();

    if (!hasYear && !hasMonth && !hasDay && !hasHour && !hasMinute && !hasSecond) {
      assertNull((categories != null ? categories.toString() : ""), categories);
      return;
    }

    assertNotNull(subString.toString(), categories);

    assertEquals("year", hasYear, categories.hasType(lexicon.getYearCategory()));
    assertEquals("month", hasMonth, categories.hasType(lexicon.getMonthCategory()));
    assertEquals("day", hasDay, categories.hasType(lexicon.getDayCategory()));
    assertEquals("hour", hasHour, categories.hasType(lexicon.getHourCategory()));
    assertEquals("minute", hasMinute, categories.hasType(lexicon.getMinuteCategory()));
    assertEquals("second", hasSecond, categories.hasType(lexicon.getSecondCategory()));
  }

  private final void verifyYear(DateTimeNumberLexicon lexicon, String input, boolean isYear) {
    final StringWrapper stringWrapper = new StringWrapper(input, DateTimeBreakStrategy.getInstance());
    final StringWrapper.SubString subString = stringWrapper.getSubString(0);

    lexicon.lookup(subString);
    final Categories categories = subString.getCategories();

    if (!isYear) {
      assertTrue(categories == null || !categories.hasType(lexicon.getYearCategory()));
    }
    else {
      assertTrue(categories.hasType(lexicon.getYearCategory()));
    }
  }

  public void testLookup() {
    final DateTimeNumberLexicon lexicon = buildLexicon(1990);

    verify(lexicon, "2106", 0, false, false, false, false, false, false);

    String string = "Dec10th | 4:03PM";
    verify(lexicon, string, 0, false, false, false, false, false, false); //  "Dec" = nothing
    verify(lexicon, string, 1, false, false, true, false, false, false);  // "10th" = day
    verify(lexicon, string, 2, false, false, false, true, false, false);  //    "4" = hour
    verify(lexicon, string, 3, false, false, false, false, true, true);   // "03PM" = minute or second

    string = "Jan3rd | 8:09A.M.";
    verify(lexicon, string,  0, false, false, false, false, false, false); //    "Jan" = nothing
    verify(lexicon, string,  1, false, false, true, false, false, false);  //    "3rd" = day
    verify(lexicon, string,  2, false, false, false, true, false, false);  //      "8" = hour
    verify(lexicon, string,  3, false, false, false, false, false, false); //    "09A" = nothing
    verify(lexicon, string, -3, false, false, false, false, true, true);   // "09A.M." = minute or second

    string = "December 2005(7)";
    verify(lexicon, string, 0, false, false, false, false, false, false);  // "December" = nothing
    verify(lexicon, string, 1, true, false, false, false, false, false);   //     "2005" = year
    verify(lexicon, string, 2, false, true, true, false, false, false);    // "7" = not hour, minute, or second

    string = "07/04/2005 12:00p.m.";
    verify(lexicon, string,  0, lexicon.isPlausibleYear(7), true, true, false, false, false);  // "07"
    verify(lexicon, string,  1, lexicon.isPlausibleYear(4), true, true, false, false, false);  // "04"
    verify(lexicon, string,  2, true, false, false, false, false, false);                      // "2005"
    verify(lexicon, string,  3, false, false, false, true, false, false);                      // "12"
    verify(lexicon, string,  4, false, false, false, false, false, false);                     // "00p"
    verify(lexicon, string, -4, false, false, false, false, true, true);                       // "00p.m"

    string = "07.04.2005";
    verify(lexicon, string, 0, lexicon.isPlausibleYear(7), true, true, false, false, false);   // "07"
    verify(lexicon, string, 1, lexicon.isPlausibleYear(4), true, true, false, false, false);   // "04"
    verify(lexicon, string, 2, true, false, false, false, false, false);                       // "2005"

    string = "07.04.05";
    verify(lexicon, string, 0, lexicon.isPlausibleYear(7), true, true, false, false, false);   // "07"
    verify(lexicon, string, 1, lexicon.isPlausibleYear(4), true, true, false, false, false);   // "04"
    verify(lexicon, string, 2, lexicon.isPlausibleYear(5), true, true, false, false, false);   // "05"

    string = "2005-07-04";
    verify(lexicon, string, 0, true, false, false, false, false, false);                       // "2005"
    verify(lexicon, string, 1, lexicon.isPlausibleYear(7), true, true, false, false, false);   // "07"
    verify(lexicon, string, 2, lexicon.isPlausibleYear(4), true, true, false, false, false);   // "04"

    string = "-6:30a.m-";
    verify(lexicon, string, -1, false, false, false, false, true, true);  // "30a.m-"
  }

  public void testMinYear() {
    DateTimeNumberLexicon lexicon = buildLexicon(1);
    verifyYear(lexicon, "345", true);
    verifyYear(lexicon, "1985", true);
    verifyYear(lexicon, "85", true);
    verifyYear(lexicon, "1995", true);
    verifyYear(lexicon, "95", true);
    verifyYear(lexicon, "2002", true);
    verifyYear(lexicon, "02", true);
    verifyYear(lexicon, "1902", true);
    verifyYear(lexicon, "2006", true);
    verifyYear(lexicon, "06", true);
    verifyYear(lexicon, "2017", false);
    verifyYear(lexicon, "17", true);

    lexicon = buildLexicon(1990);
    verifyYear(lexicon, "345", false);
    verifyYear(lexicon, "1985", false);
    verifyYear(lexicon, "85", false);
    verifyYear(lexicon, "1995", true);
    verifyYear(lexicon, "95", true);
    verifyYear(lexicon, "2002", true);
    verifyYear(lexicon, "02", true);
    verifyYear(lexicon, "1902", false);
    verifyYear(lexicon, "2006", true);
    verifyYear(lexicon, "06", true);
    verifyYear(lexicon, "2017", false);
    verifyYear(lexicon, "17", false);

    lexicon = buildLexicon(2003);
    verifyYear(lexicon, "345", false);
    verifyYear(lexicon, "1985", false);
    verifyYear(lexicon, "85", false);
    verifyYear(lexicon, "1995", false);
    verifyYear(lexicon, "95", false);
    verifyYear(lexicon, "2002", false);
    verifyYear(lexicon, "02", false);
    verifyYear(lexicon, "1902", false);
    verifyYear(lexicon, "2006", true);
    verifyYear(lexicon, "06", true);
    verifyYear(lexicon, "2017", false);
    verifyYear(lexicon, "17", false);
  }

  public static Test suite() {
    TestSuite suite = new TestSuite(TestDateTimeNumberLexicon.class);
    return suite;
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}
