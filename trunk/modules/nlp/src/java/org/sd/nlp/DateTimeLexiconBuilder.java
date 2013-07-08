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


import java.io.IOException;

/**
 * Utility class to build a lexicon for date/time extraction.
 * <p>
 * @author Spence Koehler
 */
public class DateTimeLexiconBuilder {

  public static Lexicon buildLexicon(CategoryFactory categoryFactory, AbstractNormalizer normalizer, int minYear) throws IOException {

    final Category ampm = categoryFactory.getCategory("AMPM");
    final Category month = categoryFactory.getCategory("MONTH");
    final Category weekday = categoryFactory.getCategory("WEEKDAY");
    final Category timezone = categoryFactory.getCategory("TZ");
    final Category prep = categoryFactory.getCategory("PREP");

    final Lexicon[] lexicons = new Lexicon[] {
      new AmPmLexicon(ampm),                                   // define am/pm
      new DateTimeNumberLexicon(categoryFactory, minYear),     // define date/time number categories
      loadMonthLexicon(normalizer, month),                     // month names & abbrev.'s
      GenericLexicon.loadGenericLexicon(                       // weekday names & abbrev.'s
        DateTimeLexiconBuilder.class, "resources/weekdays.txt", normalizer,
        weekday, true, true, true, null),
      GenericLexicon.loadGenericLexicon(                       // timezone names & abbrev.'s
        DateTimeLexiconBuilder.class, "resources/timezones.txt", normalizer,
        timezone, true, true, true, null),
      new GenericLexicon(                                      // date/time preposition(s) (on, at ...)
        new String[]{"at", "on"}, null, prep, false, true, false, null),
//...
// todo: text days ("Independence Day", "Christmas", "Thanksgiving", "Easter", ...)
    };

    return new LexiconPipeline(lexicons);
  }

  private static final GenericLexicon loadMonthLexicon(AbstractNormalizer normalizer, Category monthCategory) throws IOException {
    final GenericLexicon result = GenericLexicon.loadGenericLexicon(        // full month names are not case sensitive
      DateTimeLexiconBuilder.class, "resources/months.txt", normalizer, monthCategory,
      false, true, true, null);
    result.addTerms(DateTimeLexiconBuilder.class, "resources/month-abbrevs.txt", true);  // abbreviations are case sensitive
    return result;
  }
}
