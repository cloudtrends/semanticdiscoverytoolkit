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
package org.sd.cluster.util;

import org.sd.cio.MessageHelper;
import org.sd.io.FileUtil;
import org.sd.io.Publishable;
import org.sd.util.DateUtil;
import org.sd.util.ExecUtil;

import java.io.BufferedReader;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;

/**
 * Utility to grep info out of logs identified by date.
 * <p>
 * @author Spence Koehler
 */
public class LogGrepper {
  final private static Pattern EXCEPTION_PATTERN = Pattern.compile("^.*([a-z0-9]\\.)+[A-Za-z0-9]*Exception.*$", 0);
  
  private Pattern pattern;
  private Integer[] ymdhms;
  private long referenceDate;
  private long[] timeRange;
  private boolean lines;
  private boolean filenames;
  private boolean after;
  private boolean before;
  private boolean on;
  private boolean mostRecent;
  private boolean error;
  private boolean normal;
  private Integer[] ymdhmsEnd;
  private long[] timeRangeEnd;

  public LogGrepper(String patternString, Integer[] ymdhms, boolean insensitive, boolean after, boolean before, boolean on, 
                    boolean error, boolean normal, boolean mostRecent, Integer[] ymdhmsEnd) {
    final int flags = insensitive ? Pattern.CASE_INSENSITIVE : 0;

    this.pattern = patternString == null ? null : Pattern.compile("^.*" + patternString + ".*$", flags);
    this.ymdhms = ymdhms;
    this.referenceDate = DateUtil.getTimeInMillis(ymdhms);
    this.timeRange = DateUtil.getRange(ymdhms);
    this.lines = lines;
    this.filenames = filenames;
    this.after = after;
    this.before = before;
    this.on = on;
    this.mostRecent = mostRecent;
    this.error = error;
    this.normal = normal;
    this.ymdhmsEnd = ymdhmsEnd;
    this.timeRangeEnd = DateUtil.getRange(ymdhmsEnd);
  }

  public List<GrepResult> doGrep(File dir) throws IOException {
    final File[] files = dir.listFiles();
    if(files.length == 0) return null;

    List<GrepResult> results = new ArrayList<GrepResult>();

    long mostRecentTime = -1L;
    if(mostRecent){
      for(File file : files){
        final long timestamp = DateUtil.parseDate(file.getName());
        if(timestamp > mostRecentTime) mostRecentTime = timestamp;
      }
    }

    for (File file : files) {
      final String name = file.getName();
      boolean isMostRecent = (mostRecent ? ((mostRecentTime - DateUtil.parseDate(name)) < 1000) : false);
      if (shouldGrep(name) || isMostRecent) {
        List<GrepResult> resultsList = grep(file);
        if(resultsList != null && resultsList.size() != 0){
          results.addAll(resultsList);
        }
      }
    }

    return results;
  }

  public List<GrepResult> doGrepStackTraces(File dir) throws IOException {
    final File[] files = dir.listFiles();
    if(files.length == 0) return null;

    List<GrepResult> results = new ArrayList<GrepResult>();

    long mostRecentTime = -1L;
    if(mostRecent){
      for(File file : files){
        final long timestamp = DateUtil.parseDate(file.getName());
        if(timestamp > mostRecentTime) mostRecentTime = timestamp;
      }
    }

    for (File file : files) {
      final String name = file.getName();
      boolean isMostRecent = (mostRecent ? ((mostRecentTime - DateUtil.parseDate(name)) < 1000) : false);
      if (shouldGrep(name) || isMostRecent) {
        List<GrepResult> stackTraceResultsList = grepStackTraces(file);
        if(stackTraceResultsList != null && stackTraceResultsList.size() != 0){
          results.addAll(stackTraceResultsList);
        }
      }
    }

    return results;
  }

  private List<GrepResult> grep(File file) throws IOException {
    List<GrepResult> results = new ArrayList<GrepResult>();
    final BufferedReader reader = FileUtil.getReader(file);

    int lineNum = 0;
    String line = null;
    while ((line = reader.readLine()) != null) {
      lineNum++;

      line = line.trim();
      if(pattern == null){
        results.add(new GrepResult("", file.getName(), line, lineNum));
      }
      else {
        final Matcher m = pattern.matcher(line);
        if(m.matches()){
          GrepResult match = new GrepResult(pattern.toString(), file.getName(), line, lineNum);
          results.add(match);
        }
      }
    }
    reader.close();

    return results;
  }

  public List<GrepResult> grepStackTraces(File file) throws IOException {
    List<GrepResult> results = new ArrayList<GrepResult>();
    final BufferedReader reader = FileUtil.getReader(file);

    int lineNum = 0;
    String line = reader.readLine();
    lineNum++;
    List<String> stackTrace = new ArrayList<String>();
    while (line != null) {
      final Matcher m = EXCEPTION_PATTERN.matcher(line);
      if(m.matches()){
        stackTrace.add(line);

        int traceLineNum = 0;
        String traceLine = reader.readLine();
        traceLineNum++;

        while(traceLine != null && traceLine.startsWith("\tat ")){
          stackTrace.add(traceLine);

          traceLine = reader.readLine();
          traceLineNum++;
        }
        
        if(stackTrace.size() > 1){
          String stackTraceString = "";
          for(String stackTraceLine : stackTrace){
            stackTraceString += stackTraceLine + '\n';
          }

          stackTraceString = stackTraceString.trim();
          GrepResult stackTraceResult = new GrepResult(EXCEPTION_PATTERN.toString(), file.getName(), stackTraceString, lineNum);
          results.add(stackTraceResult);
        }
        
        line = traceLine;
        lineNum += traceLineNum;
        stackTrace = new ArrayList<String>();
      }
      else{
        line = reader.readLine();
        lineNum++;
      }
    }

    return results;
  }

  protected boolean shouldGrep(String filename) {
    boolean result = false;

    if (filename.startsWith("log-") && ((error && filename.endsWith(".err")) || (normal && filename.endsWith(".out")))) {
      final long timestamp = DateUtil.parseDate(filename);

      // determine whether we meet timestamp constraints
      if (ymdhmsEnd == null) {
        result =
          (after && (timestamp > timeRange[1])) ||
          (before && (timestamp < timeRange[0])) ||
          (on && (timestamp >= timeRange[0] && timestamp <= timeRange[1]));
      }
      else {
        result = timestamp > timeRange[0] && timestamp < timeRangeEnd[1];
      }
    }

    return result;
  }

  public class GrepResult implements Publishable{
    private String matchPattern;
    private String filename;
    private String matchLine;
    private int lineNum;

    public GrepResult(){
      this.matchPattern = "";
      this.filename = "";
      this.matchLine = "";
      this.lineNum = -1;
    }

    public GrepResult(String matchPattern, String filename, String matchLine, int lineNum){
      this.matchPattern = matchPattern;
      this.filename = filename;
      this.matchLine = matchLine;
      this.lineNum = lineNum;
    }

    public String getMatch(){
      return matchLine;
    }

    public String getFilename(){
      return filename;
    }

    public int getLineNum(){
      return lineNum;
    }

    public String toString(){
      return filename + "\t" + lineNum + ":\t" + matchLine;
    }

    public void write(DataOutput dataOutput) throws IOException {
      MessageHelper.writeString(dataOutput, matchPattern);
      MessageHelper.writeString(dataOutput, filename);
      MessageHelper.writeString(dataOutput, matchLine);
      dataOutput.writeInt(lineNum);
    }

    public void read(DataInput dataInput) throws IOException {
      this.matchPattern = MessageHelper.readString(dataInput);
      this.filename = MessageHelper.readString(dataInput);
      this.matchLine = MessageHelper.readString(dataInput);
      this.lineNum = dataInput.readInt();
    }
  }

  private static final void usage() {
    System.err.println();
    System.err.println("LogGrepper - utility to grep cluster logs for patterns.");
    System.err.println();
    System.err.println("USAGE:");
    System.err.println("\tjava " + LogGrepper.class.getName() + " [OPTION]...\n");
    System.err.println();
    System.err.println("DESCRIPTION:");
    System.err.println();
    System.err.println("\t-d, --date=DATE (required)");
    System.err.println("\t\tthe date as a point of reference for selecting logs to grep.");
    System.err.println("\t\tDates are of the form: yyyy-mm-dd-hh:mm:ss where a date can");
    System.err.println("\t\tspecified by as few continuous left-side constituents as");
    System.err.println("\t\tdesired.");
    System.err.println();
    System.err.println("\t-p, --pattern=PATTERN (optional)");
    System.err.println("\t\tthe pattern to grep for. If absent the selected log file(s)");
    System.err.println("\t\tbe 'cat'ed.");
    System.err.println();
    System.err.println("\t-i, --insensitive");
    System.err.println("\t\tif option is present, use case-insensitive pattern matching.");
    System.err.println();
    System.err.println("\t-a, --after");
    System.err.println("\t\tif option is present, logs after date will be included. logs");
    System.err.println("\t\ton the date will not be included unless -o flag is set when");
    System.err.println("\t\tthis flag is set.");
    System.err.println("\t-b, --before");
    System.err.println("\t\tif option is present, logs before date will be included. logs");
    System.err.println("\t\ton the date will not be included unless -o flag is set when");
    System.err.println("\t\tthis flag is set.");
    System.err.println();
    System.err.println("\t-o, --on (default)");
    System.err.println("\t\tThis is the default if none of -a, -b, or -o are set. Indicates");
    System.err.println("\t\tto include files on the date specified. Note that the date can");
    System.err.println("\t\tbe fairly loose to encompass multiple logs that are 'on' the date.");
    System.err.println();
    System.err.println("\t-e, --error");
    System.err.println("\t\tLook in error logs. If neither -e nor -n are set, then both are");
    System.err.println("\t\tsearched by default; otherwise, only those set are searched.");
    System.err.println();
    System.err.println("\t-n, --normal");
    System.err.println("\t\tLook in normal logs. If neither -e nor -n are set, then both are");
    System.err.println("\t\tsearched by default; otherwise, only those set are searched.");
    System.err.println();
    System.err.println("\t-r, --recent");
    System.err.println("\t\tLook in the most recent log(s), even if they are outside of the date range.");
    System.err.println();
    System.err.println("\t-b, --between=DATE");
    System.err.println("\t\tAccept files between the 'date' and this date, ignoring the other");
    System.err.println("\t\trange flags of -a, -b, and -o");
    System.err.println();
    System.err.println("\t-l, --lines");
    System.err.println("\t\tDisplay line numbers with matches.");
    System.err.println();
    System.err.println("\t-h, --filenames");
    System.err.println("\t\tDisplay filenames of matches.");
    System.err.println();
  }

  // java -Xmx640m org.sd.cluster.util.LogGrepper -d 2007-02-23-21:14 -o -a -e -p "No sitemap found"
  public static final void main(String[] args) throws IOException {

    final Options options = new Options();

    // define options
    options.addOption(OptionBuilder.withArgName("date").withLongOpt("timestampDate").hasArg().isRequired(true).create('d'));
    options.addOption(OptionBuilder.withArgName("pattern").withLongOpt("grepPattern").hasArg().isRequired(false).create('p'));
    options.addOption(OptionBuilder.withArgName("insensitive").withLongOpt("caseInsensitive").isRequired(false).create('i'));
    options.addOption(OptionBuilder.withArgName("after").withLongOpt("afterDate").isRequired(false).create('a'));
    options.addOption(OptionBuilder.withArgName("before").withLongOpt("beforeDate").isRequired(false).create('b'));
    options.addOption(OptionBuilder.withArgName("on").withLongOpt("onDate").isRequired(false).create('o'));
    options.addOption(OptionBuilder.withArgName("error").withLongOpt("errorOnly").isRequired(false).create('e'));
    options.addOption(OptionBuilder.withArgName("normal").withLongOpt("normalOutputOnly").isRequired(false).create('n'));
    options.addOption(OptionBuilder.withArgName("recent").withLongOpt("mostRecent").isRequired(false).create('r'));
    options.addOption(OptionBuilder.withArgName("between").withLongOpt("betweenDates").hasArg().isRequired(false).create('w'));
    options.addOption(OptionBuilder.withArgName("lines").withLongOpt("showLineNumbers").isRequired(false).create('l'));
    options.addOption(OptionBuilder.withArgName("filename").withLongOpt("showFileName").isRequired(false).create('h'));

    // initialize parameters
    final CommandLineParser parser = new PosixParser();

    try {
      final CommandLine commandLine = parser.parse(options, args);

      // get args
      final String dateString = commandLine.getOptionValue('d');
      final String patternString = commandLine.getOptionValue('p');
      final boolean insensitive = commandLine.hasOption('i');
      final boolean after = commandLine.hasOption('a');
      final boolean before = commandLine.hasOption('b');
      boolean on = commandLine.hasOption('o');
      boolean error = commandLine.hasOption('e');
      boolean normal = commandLine.hasOption('n');
      boolean mostRecent = commandLine.hasOption('r');
      boolean lines = commandLine.hasOption('l');
      boolean filenames = commandLine.hasOption('h');
      final String betweenDateString = commandLine.getOptionValue('w');

      if (!before && !after && !on && betweenDateString == null) on = true;
      if (!error && !normal) {
        error = true;
        normal = true;
      }

      final Integer[] ymdhms = DateUtil.parseDateComponents(dateString);
      final Integer[] ymdhmsEnd = betweenDateString != null ? DateUtil.parseDateComponents(betweenDateString) : null;
      
      final LogGrepper logGrepper = new LogGrepper(patternString, ymdhms, insensitive, after, before, on, error, normal, mostRecent, ymdhmsEnd);
      final String logDirName = ExecUtil.getUserHome() + "/cluster/log/";  //todo: parameterize this?

      List<GrepResult> grepResults = logGrepper.doGrep(new File(logDirName));
      for(GrepResult result : grepResults){
        System.out.println((filenames ? result.getFilename() + ":\t" : "") + (lines ? result.getLineNum() + ":\t" : "") + result.getMatch());
      }
    }
    catch (ParseException e) {
      System.err.println("Command line error: " + e.getLocalizedMessage());
      e.printStackTrace(System.err);
      usage();
    }
  }
}
