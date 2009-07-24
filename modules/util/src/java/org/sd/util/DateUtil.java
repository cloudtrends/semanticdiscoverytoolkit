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

import org.sd.io.FileUtil;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.GregorianCalendar;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class to format dates and times.
 * <p>
 * @author Spence Koehler
 */
public class DateUtil {
  
  public static final Calendar CURRENT_CALENDAR = new GregorianCalendar();
  public static final int CURRENT_YEAR = CURRENT_CALENDAR.get(Calendar.YEAR);

  public static final String[] MONTH_ABBREVS = new String[] {
    "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec",
  };

  public static final String[] NUMBER_SUFFIX = new String[] {
    "th", "st", "nd", "rd", "th", "th", "th", "th", "th", "th",
  };

  /**
   * Get a date in the form MonthDayYear where Mmm is a 3-letter month abbrev,
   * day is of the form Dth and year is of the form YYYY (where D and Y are
   * digits and th is the appropriate number extension for the day.
   *
   * @param day    day of month (1-based)
   * @param month  month (0-based, January=0)
   * @param year   4-digit year
   */
  public static final String formatDate(Integer day, Integer month, Integer year) {
    final StringBuilder result = new StringBuilder();

    if (month != null) {
      result.append(MONTH_ABBREVS[month]);
    }
    if (day != null) {
      result.append(Integer.toString(day)).
        append(getNumberSuffix(day));
    }
    if (year != null) {
      result.append(asDigits(year, 4));
    }

    return result.toString();
  }
  
  /**
   * Formats a time period in legible terms - not a date represented as a long,
   * but a period of time (eg processing time)
   * @param timePeriod time period to format
   */
  public static final String formatTimePeriod(long timePeriod) {
    int hours = (int)timePeriod / 3600000;
    timePeriod = timePeriod % 3600000;
    int minutes = (int)timePeriod / 60000;
    timePeriod = timePeriod % 60000;
    int seconds = (int)timePeriod / 1000;
    timePeriod = timePeriod % 1000;
    
    return (hours > 0 ? hours+"h " : "") + (minutes > 0 ? minutes+"m " : "") + (seconds > 0 ? seconds+"s " : "") + (timePeriod > 0 ? timePeriod+"ms " : "");
  }

  public static final String formatTime(Integer hour, Integer minute, Integer second, Integer ampm, Integer zoneOffset) {
    final StringBuilder result = new StringBuilder();

    int hour24 = (hour == null) ? 0 : hour.intValue();
    if (ampm != null) {
      // am=0, pm=1
      hour24 %= 12;
      if (ampm == 1) hour24 += 12;
    }

    result.append(asDigits(hour24, 2)).append(':').
      append(minute == null ? "00" : asDigits(minute, 2));

    if (second != null) {
      result.append(':').append(asDigits(second, 2));
    }

    if (zoneOffset != null) result.append(' ').append(getTimezoneString(zoneOffset));

    return result.toString();
  }

  /**
   * Build a date string of the form yyyy-mm-dd-hh:mm:ss
   */
  public static final String buildDateString(long millis) {
    final GregorianCalendar gc = new GregorianCalendar();
    gc.setTimeInMillis(millis);

    return buildDateString(gc.get(GregorianCalendar.YEAR),
                           gc.get(GregorianCalendar.MONTH) + 1,
                           gc.get(GregorianCalendar.DAY_OF_MONTH),
                           gc.get(GregorianCalendar.HOUR_OF_DAY),
                           gc.get(GregorianCalendar.MINUTE),
                           gc.get(GregorianCalendar.SECOND));
  }

  /**
   * Build a string of the form yyyy-mm-dd-hh:mm:ss
   *
   * @param year   4-digit year
   * @param month  month (1-based, January=1)
   * @param day    day of month (1-based)
   * @param hour   24-hour clock-based hour (0-23)
   * @param minute minute in hour (0-based)
   * @param second second in minute (0-based)
   */
  public static final String buildDateString(Integer year, Integer month, Integer day, Integer hour, Integer minute, Integer second) {
    final StringBuilder result = new StringBuilder();

    result.append(asDigits(year, 4));
    if (month != null) {
      result.append('-').append(asDigits(month, 2));
      if (day != null) {
        result.append('-').append(asDigits(day, 2));
        if (hour != null) {
          result.append('-').append(asDigits(hour, 2));
          if (minute != null) {
            result.append(':').append(asDigits(minute, 2));
            if (second != null) {
              result.append(':').append(asDigits(second, 2));
            }
          }
        }
      }
    }

    return result.toString();
  }

  /**
   * Create a string of the form yyyy-mm-dd from the time indicated by the millis.
   */
  public static final String buildYMDString(long millis) {
    final GregorianCalendar gc = new GregorianCalendar();
    gc.setTimeInMillis(millis);

    return buildDateString(gc.get(GregorianCalendar.YEAR),
                           gc.get(GregorianCalendar.MONTH) + 1,
                           gc.get(GregorianCalendar.DAY_OF_MONTH),
                           null, null, null);
  }

  public static final String asDigits(int number, int minNumDigits) {
    final StringBuilder result = new StringBuilder();

    result.append(Integer.toString(number));
    final int len = result.length();
    for (int i = minNumDigits - len; i > 0; --i) result.insert(0, '0');

    return result.toString();
  }

  public static final String getNumberSuffix(int number) {
    final int mod100 = number % 100;
    return (mod100 >= 10 && mod100 <= 20) ? "th" : NUMBER_SUFFIX[(number % 10)];
  }

  public static final String getTimezoneString(int zoneOffset) {
    final StringBuilder result = new StringBuilder();
    result.append("GMT");
    if (zoneOffset != 0) {
      final String offsetString = Integer.toString(zoneOffset);
      if (zoneOffset > 0) result.append('+');
      result.append(offsetString);
    }
    return result.toString();
  }

  /**
   * Given integers for year, month, day, hours, minutes, seconds; get the
   * indicated time in millis.
   */
  public static final long getTimeInMillis(Integer[] ymdhms) {
    return ymdhms == null ? 0L : getTimeInMillis(ymdhms[0], ymdhms[1], ymdhms[2], ymdhms[3], ymdhms[4], ymdhms[5]);
  }

  /**
   * Translate the given date into milliseconds.
   *
   * @param year   4-digit year
   * @param month  month (1-based, January=1)
   * @param day    day of month (1-based)
   * @param hour   24-hour clock-based hour (0-23)
   * @param minute minute in hour (0-based)
   * @param second second in minute (0-based)
   */
  public static final long getTimeInMillis(Integer year, Integer month, Integer day,
                                           Integer hour, Integer minute, Integer second) {
    if (month == null) month = 1;
    if (day == null) day = 0;
    if (hour == null) hour = 0;
    if (minute == null) minute = 0;
    if (second == null) second = 0;

    final GregorianCalendar gc = new GregorianCalendar(year, month - 1, day, hour, minute, second);
    return gc.getTimeInMillis();
  }


  private static final String YEAR_PLUS_PATTERN_STRING = "^.*(\\d{4}.*)$";
  private static final Pattern YEAR_PLUS_PATTERN = Pattern.compile(YEAR_PLUS_PATTERN_STRING);

  /**
   * Get the number of milliseconds represented by the date string of the form:
   * ".*yyyy-mm-dd-hh:mm:ss.*".
   */
  public static final long parseDate(String dateString) {
    long result = 0L;

    final Integer[] pieces = parseDateComponents(dateString);

    if (pieces != null) {
      result = getTimeInMillis(pieces);
    }

    return result;
  }

  /**
   * Get the year, month, day, hour, minute, second data contained in the
   * date string. If components are not specified in the string, then their
   * values will be null.
   *
   * @return null if no date can be parsed or an Integer[6] array where
   *         some elements in the array may be null.
   */
  public static final Integer[] parseDateComponents(String dateString) {
    Integer[] result = null;

    Integer year = null;
    Integer month = null;
    Integer day = null;
    Integer hour = null;
    Integer minute = null;
    Integer second = null;

    final Matcher m = YEAR_PLUS_PATTERN.matcher(dateString);
    if (m.matches()) {
      final String yearPlusString = m.group(1);
      final String[] pieces = yearPlusString.split("[-:]");

      year = parseNumber(pieces, 0);
      month = parseNumber(pieces, 1);
      day = parseNumber(pieces, 2);
      hour = parseNumber(pieces, 3);
      minute = parseNumber(pieces, 4);
      second = parseNumber(pieces, 5);

      result = new Integer[]{year, month, day, hour, minute, second};
    }

    return result;
  }

  private static final Integer parseNumber(String[] pieces, int pieceNum) {
    Integer result = null;

    if (pieces.length > pieceNum) {
      result = Integer.parseInt(pieces[pieceNum]);
    }

    return result;
  }

  public static final long[] getRange(Integer[] ymdhms) {
    if (ymdhms == null) return null;

    int range = 0;
    if (ymdhms[5] == null) {  // second
      range = 60000;  // 60 seconds

      if (ymdhms[4] == null) {  // minute
        range *= 60;  // 60 minutes

        if (ymdhms[3] == null) {  // hour
          range *= 24;  // 24 hours

          if (ymdhms[2] == null) {  // day
            range *= 30;  // 30 days

            if (ymdhms[1] == null) {  // month
              range *= 12;  // 12 months
            }
          }
        }
      }
    }

    return new long[] {getTimeInMillis(ymdhms), getTimeInMillis(ymdhms) + range};
  }

  public static DateMarker getDateMarker(File baseDir, String markerId, int bufferSize){
    try {
      return new DateMarker(baseDir, markerId, bufferSize);
    }
    catch (IOException ioe){
      System.err.println("Unable to obtain date marker!: " + ioe.getMessage());
      return null;
    }
  }

  public static class DateMarker{
    private static final String markerSuffix = "marker";
    private static final DateFormat format = new SimpleDateFormat("yyyy-MM-dd-HH:mm:ss");

    private int count;
    private int bufferSize;
    private Date lastDate;

    private final String markerId;
    private File markerFile;
    private BufferedWriter markerBuffer;

    public DateMarker(File baseDir, String markerId, int bufferSize) throws IOException{
      this.markerId = markerId;
      this.markerFile = new File(baseDir, markerId + "." + markerSuffix);

      this.bufferSize = (bufferSize < 1 ? 1: bufferSize);
      this.lastDate = null;
      init();
    }

    public void init() throws IOException{
      System.out.println("Initializing marker file '" + markerFile.getAbsolutePath() + "' with markerId=" + markerId + ",bufferSize=" + bufferSize);
      if(markerFile.exists()){
        ArrayList<String> lines = FileUtil.readLines(markerFile.getAbsolutePath());
        count = lines.size();
        if(lines.size() != 0){
          try {
            lastDate = this.format.parse(lines.get(lines.size() - 1));
          }
          catch (ParseException pe){
            System.err.println("Parse error while reading last date value: " + pe.getMessage());
            lastDate = null;
          }
        }

        System.out.println("Marker file found from prior iteration: " + count + "/" + bufferSize + " lines, lastDate=" + lastDate);
        markerBuffer = FileUtil.getWriter(markerFile, true/*append*/);
      } 
      else {
        count = 0;
        markerBuffer = FileUtil.getWriter(markerFile, true/*append*/);
      }
    }

    public Date getLastDate(){
      return lastDate;
    }

    public boolean writeNext(Date date) throws IOException{
      synchronized (markerFile) {
        if(count >= bufferSize){
          roll();
        }

        markerBuffer.write(this.format.format(date) + "\n");
        markerBuffer.flush();
        count++;
      }

      return true;
    }

    private void roll() throws IOException{
      // copy old file to backup
      markerBuffer.flush();
      boolean success = FileUtil.copyFile(markerFile, new File(markerFile.getAbsolutePath() + ".bup"));
      if(!success){
        throw new IOException("Unable to create backup file due to low disk space!");
      }

      // clean up old file
      try{
        markerBuffer.close();
      }
      finally {
        markerFile.delete();
      }

      // start new file
      count = 0;
      markerBuffer = FileUtil.getWriter(markerFile, true/*append*/);
    }

    public void close(){
      try{
        markerBuffer.close();
      }
      catch(IOException ioe){
        System.err.println("Unable to close BufferedWriter for marker file!: " + ioe.getMessage());
      }
      finally {
        markerBuffer = null;
      }
    }
  }

  public static final void main(String[] args) {
    // for each arg,
    //   if numeric convert the arg into a date string
    //   if string, parse and print the 'long' value form. (todo: implement this)

    final Date curDate = new Date();
    System.out.println("Current time: " + curDate.getTime() + " (" + curDate + ")");

    final SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, MMM dd yyyy hh:mm a");
//    dateFormat.setTimeZone(java.util.TimeZone.getTimeZone("GMT"));

    for (String arg : args) {
      final Date date = new Date(Long.parseLong(arg));
      System.out.println(arg + " --> " + date + " (" + dateFormat.format(date) + ")");
    }
  }
}
