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


import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.sd.io.FileUtil;
import org.sd.nlp.Parser;
import org.sd.nlp.StringWrapper;
import org.sd.util.DateUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * JUnit Tests for the DateTimeExtractor class.
 * <p>
 * @author Spence Koehler
 */
public class TestDateTimeExtractor extends TestCase {

  public TestDateTimeExtractor(String name) {
    super(name);
  }
  
  private final void verify(DateTimeExtractor extractor, String sentence, String[] expectedParses) throws IOException {
    final List<Parser.Parse> parses = extractor.parse(sentence);
    if (expectedParses == null) {
      assertNull("expected null, but got: " + parses, parses);
    }
    else {
      assertNotNull("couldn't parse: '" + sentence + "'", parses);
      assertEquals("got parses: " + parses, expectedParses.length, parses.size());

      int index = 0;
      for (Parser.Parse parse : parses) {
        assertEquals("got=" + parse.toString() + ", expected=" + expectedParses[index], expectedParses[index], parse.toString());
        ++index;
      }
    }
  }

  public void testSimple() throws IOException {
    final DateTimeExtractor extractor = new DateTimeExtractor(2003);

    verify(extractor, "06/30/05", new String[]{"(DATE (MONTH '06') (DAY '30') (YEAR '05'))"});
    verify(extractor, "at 02:39 PM |", new String[]{"(TIME (PREP 'at') (TIME (HOUR '02') (MINUTE '39') (AMPM 'PM')))"});
    verify(extractor, "2005.12.31 A.M. 12:29", new String[]{"(DATE_TIME (DATE (YEAR '2005') (MONTH '12') (DAY '31')) (TIME (AMPM 'A.M.') (HOUR '12') (MINUTE '29')))"});
    verify(extractor, "-6:30 a.m-", new String[]{"(TIME (HOUR '6') (MINUTE '30') (AMPM 'a.m-'))"});
    verify(extractor, "WedApr6th &middot; 2:06PM", new String[]{"(DATE_TIME (DATE (WEEKDAY 'Wed') (MONTH 'Apr') (DAY '6th')) (TIME (HOUR '2') (MINUTE '06PM')))"});
    verify(extractor, "12:08am 28/12/2006", new String[]{"(DATE_TIME (TIME (HOUR '12') (MINUTE '08am')) (DATE (DAY '28') (MONTH '12') (YEAR '2006')))"});
    verify(extractor, "AM 12:08", new String[]{"(TIME (AMPM 'AM') (HOUR '12') (MINUTE '08'))"});
  }

  public void testSkipOne() throws IOException {
    final DateTimeExtractor extractor = new DateTimeExtractor(2003, false, 1);

    verify(extractor, "Gobbledygook: 12 Dec 2005", new String[]{"(DATE (DAY '12') (MONTH 'Dec') (YEAR '2005'))"});
  }

  public void testSkip5AndIgnoreExtra() throws IOException {
    final DateTimeExtractor extractor = new DateTimeExtractor(2003, true, 5);

    verify(extractor, "Gobbledygook: 12 Dec 2005", new String[]{"(DATE (YEAR '12') (MONTH 'Dec'))", "(DATE (DAY '12') (MONTH 'Dec') (YEAR '2005'))"});
    verify(extractor, "September 2005(11)", new String[]{"(DATE (MONTH 'September') (YEAR '2005'))"});
    verify(extractor, "MyFinances.co.uk, UK - Jun 15, 2005", new String[]{"(DATE (MONTH 'Jun') (DAY '15') (YEAR '2005'))"});
    verify(extractor, "Published: December 24, 2005", new String[]{"(DATE (MONTH 'December') (DAY '24') (YEAR '2005'))"});
    verify(extractor, "Posted: Tue Dec 27, 2005 3:35 pm", new String[]{"(DATE_TIME (DATE (WEEKDAY 'Tue') (MONTH 'Dec') (DAY '27') (YEAR '2005')) (TIME (HOUR '3') (MINUTE '35') (AMPM 'pm')))"});
    verify(extractor, "since January 21, 2004", new String[]{"(DATE (MONTH 'January') (DAY '21') (YEAR '2004'))"});
    verify(extractor, "December 2005: Visiting the States", new String[]{"(DATE (MONTH 'December') (YEAR '2005'))"});
    verify(extractor, "posted 12/31/05", new String[]{"(DATE (MONTH '12') (DAY '31') (YEAR '05'))"});
    verify(extractor, "27th December, 2005. 8:50 pm.", new String[]{"(DATE_TIME (DATE (DAY '27th') (MONTH 'December') (YEAR '2005.')) (TIME (HOUR '8') (MINUTE '50') (AMPM 'pm.')))"});
    verify(extractor, ", by try70243 (Sep 6)", new String[]{"(DATE (MONTH 'Sep') (DAY '6'))"});
    verify(extractor, "___23 March 2004", new String[]{"(DATE (DAY '23') (MONTH 'March') (YEAR '2004'))"});
    verify(extractor, "posted at 2005/12/29 02:33 | 'Ü'è'è'ñ |", new String[]{"(DATE_TIME (DATE (PREP 'at') (DATE (YEAR '2005') (MONTH '12') (DAY '29'))) (TIME (HOUR '02') (MINUTE '33')))"});
    verify(extractor, "2005. december 31. 23:49., szerz", new String[]{"(DATE_TIME (DATE (YEAR '2005.') (MONTH 'december') (DAY '31.')) (TIME (HOUR '23') (MINUTE '49.')))"});
    verify(extractor, "5pm on Tuesday 29th November", new String[]{"(DATE_TIME (TIME '5pm') (DATE (PREP 'on') (DATE (WEEKDAY 'Tuesday') (DAY '29th') (MONTH 'November'))))"});

    //todo: fix date parser to get this date right?
    verify(extractor, ", by uosae11 (Dec 15)",
           DateUtil.CURRENT_YEAR >= 2010 ?
           new String[]{"(DATE (YEAR '11') (MONTH 'Dec') (DAY '15'))"} :
           new String[]{"(DATE (MONTH 'Dec') (DAY '15'))"});

    verify(extractor, "Sun Herald - Dec 06 10:06 PM", new String[]{"(DATE_TIME (DATE (MONTH 'Dec') (YEAR '06')) (TIME (HOUR '10') (MINUTE '06') (AMPM 'PM')))",
                                                                   "(DATE_TIME (DATE (MONTH 'Dec') (DAY '06')) (TIME (HOUR '10') (MINUTE '06') (AMPM 'PM')))"});
    verify(extractor, "10:12pm 10/10/2006", new String[]{"(DATE_TIME (TIME (HOUR '10') (MINUTE '12pm')) (DATE (YEAR '10') (MONTH '10')))",
                                                         "(DATE_TIME (TIME (HOUR '10') (MINUTE '12pm')) (DATE (MONTH '10') (YEAR '10')))",
                                                         "(DATE_TIME (TIME (HOUR '10') (MINUTE '12pm')) (DATE (MONTH '10') (DAY '10') (YEAR '2006')))",
                                                         "(DATE_TIME (TIME (HOUR '10') (MINUTE '12pm')) (DATE (DAY '10') (MONTH '10') (YEAR '2006')))"});

//todo: fix grammar to support these...
//
//    verify(extractor, "27/12/05:22:18:38", new String[]{"(DATE_TIME (DATE (DAY '27') (MONTH '12') (YEAR '05')) (TIME (HOUR '22') (MINUTE '18') (SECOND '38')))"});
//        -- ambiguity for classifying ":22:" as an hour (in addition to minute/second) seems too great because every minute and second will also look like an hour
//
  }

  public void testAmbiguousParses() throws IOException {
    final DateTimeExtractor extractor = new DateTimeExtractor(2003);

    verify(extractor, "01/10/2005",
           new String[]{"(DATE (MONTH '01') (DAY '10') (YEAR '2005'))",
                        "(DATE (DAY '01') (MONTH '10') (YEAR '2005'))"});
  }

  public void testOtherFormats() throws IOException {
    final DateTimeExtractor extractor = new DateTimeExtractor(2000);

    verify(extractor, "2007-06-28", new String[]{"(DATE (YEAR '2007') (MONTH '06') (DAY '28'))"});
    verify(extractor, "Thu Jun 28 13:27:44 MDT 2007",
           new String[] {
             "(DATE_TIME (DATE (WEEKDAY 'Thu') (MONTH 'Jun') (DAY '28')) (TIME (HOUR '13') (MINUTE '27') (SECOND '44') (TZ 'MDT')) (YEAR '2007'))"
           });
    verify(extractor, "Thu Jun 28 13:46:16 MDT 2007",
           new String[] {
             "(DATE_TIME (DATE (WEEKDAY 'Thu') (MONTH 'Jun') (DAY '28')) (TIME (HOUR '13') (MINUTE '46') (SECOND '16') (TZ 'MDT')) (YEAR '2007'))",
           });
  }

  public void testKanjiMarkerRecognition() throws IOException {
    final DateTimeExtractor extractor = new DateTimeExtractor(2000);

    final File kanjiFile = FileUtil.getFile(this.getClass(), "resources/kanji-dates.txt");
    final BufferedReader reader = FileUtil.getReader(kanjiFile, "EUC-JP");
    String line = null;

    while ((line = reader.readLine()) != null) {
      final String[] pieces = line.split("\\s*\\|\\s*");
      final String kanjiInput = pieces[0];
      final String[] expectedParses = pieces[1].split("\\s*,\\s*");

//      System.out.println(line);
      verify(extractor, kanjiInput, expectedParses);
    }

    reader.close();
  }

  public void testAsianDates() throws IOException {
    final DateTimeExtractor extractor = new DateTimeExtractor(2000);

    verify(extractor, "  2006年3月23日      ", new String[]{"(DATE (YEAR '2006') (MONTH '3') (DAY '23'))"});
  }

  public void testMoreAmbiguity() throws IOException {
    DateTimeExtractor extractor = null;

    extractor = new DateTimeExtractor(2000);
    verify(extractor, "02 December 2007 @ 11:05 pm", new String[]{"(DATE_TIME (DATE (DAY '02') (MONTH 'December') (YEAR '2007')) (TIME (HOUR '11') (MINUTE '05') (AMPM 'pm')))"});

    extractor = new DateTimeExtractor(2003);
    verify(extractor, "02 December 2007 @ 11:05 pm", new String[]{"(DATE_TIME (DATE (DAY '02') (MONTH 'December') (YEAR '2007')) (TIME (HOUR '11') (MINUTE '05') (AMPM 'pm')))"});
  }

  public static Test suite() {
    TestSuite suite = new TestSuite(TestDateTimeExtractor.class);
    return suite;
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}
