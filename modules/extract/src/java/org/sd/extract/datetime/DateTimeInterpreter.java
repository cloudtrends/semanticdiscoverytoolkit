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
import org.sd.extract.Interpreter;
import org.sd.nlp.Parser;
import org.sd.util.DateUtil;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;

/**
 * Interpreter to generate DateTimeInterpretations.
 * <p>
 * @author Spence Koehler
 */
public class DateTimeInterpreter implements Interpreter {
  
  private static Calendar DEFAULT_CURRENT_CALENDAR = DateUtil.CURRENT_CALENDAR;
  public static Calendar getDefaultCalendar() {
    return DEFAULT_CURRENT_CALENDAR;
  }
  public static final Calendar setDefaultCalendar(Calendar calendar) {
    Calendar result = DEFAULT_CURRENT_CALENDAR;
    DEFAULT_CURRENT_CALENDAR = calendar;
    return result;
  }


  private Calendar currentCalendar;
  private boolean doGuessYear;

  public DateTimeInterpreter() {
    this(DEFAULT_CURRENT_CALENDAR, true);
  }

  /**
   * Construct with the date/time that is to be assumed current with respect
   * to the data being interpreted (i.e. a crawl date).
   */
  public DateTimeInterpreter(Calendar currentCalendar, boolean doGuessYear) {
    this.currentCalendar = currentCalendar;
    this.doGuessYear = doGuessYear;
  }

  public Interpretation[] interpret(Extraction extraction) {
    Interpretation[] result = null;
    List<Interpretation> interpretations = null;

    final List<Parser.Parse> parses = extraction.getData().getParses();

    if (parses != null) {
      interpretations = new ArrayList<Interpretation>();
      for (Iterator<Parser.Parse> iter = parses.iterator(); iter.hasNext(); ) {
        final Parser.Parse parse  = iter.next();
        final DateTimeInterpretation interp = new DateTimeInterpretation(extraction, parse, currentCalendar, doGuessYear);
        if (interp.isValid()) {
          interpretations.add(interp);
        }
        else {
          iter.remove();
        }
      }
      result = interpretations.toArray(new Interpretation[interpretations.size()]);
    }

    return result;
  }
}
