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


import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.util.Date;
import java.util.GregorianCalendar;

/**
 * JUnit Tests for the DateUtil class.
 * <p>
 * @author Spence Koehler
 */
public class TestDateUtil extends TestCase {

  public TestDateUtil(String name) {
    super(name);
  }
  
  public void testGetTimeInMillis() {
    final GregorianCalendar curDate = new GregorianCalendar();
    final long builtDate =
      DateUtil.getTimeInMillis(curDate.get(GregorianCalendar.YEAR),
                               curDate.get(GregorianCalendar.MONTH) + 1,
                               curDate.get(GregorianCalendar.DAY_OF_MONTH),
                               curDate.get(GregorianCalendar.HOUR_OF_DAY),
                               curDate.get(GregorianCalendar.MINUTE),
                               curDate.get(GregorianCalendar.SECOND));

    final long curTime = curDate.getTimeInMillis() - (curDate.getTimeInMillis() % 1000);  // clip millis
    assertEquals("curDate=" + new Date(curTime).toString() + " builtDate=" + new Date(builtDate).toString(),
                 curTime, builtDate);
  }

  public void testParseDate() {
    long curTime = System.currentTimeMillis();
    final String date = DateUtil.buildDateString(curTime);
    final String logFileName = "log-" + date + "-0.out";
    final long parsedTime = DateUtil.parseDate(logFileName);

    curTime = curTime - (curTime % 1000);  // clip millis

    assertEquals("curTime=" + new Date(curTime).toString() + " parsedTime=" + new Date(parsedTime).toString(),
                 curTime, parsedTime);
  }

  public static Test suite() {
    TestSuite suite = new TestSuite(TestDateUtil.class);
    return suite;
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}
