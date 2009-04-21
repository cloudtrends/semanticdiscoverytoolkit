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
package org.sd.cluster.job;


import org.sd.io.FileUtil;
import org.sd.util.MathUtil;
import org.sd.util.StatsAccumulator;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Utility to analyze a job log.
 * <p>
 * @author Spence Koehler
 */
public class JobLogAnalyzer {

  public static void locateLongRunningSites(String logfile, double numStdDevs) throws IOException {
    final StatsAccumulator stats = new StatsAccumulator();
    final Map<String, Long> domain2time = loadDomainTimes(logfile, stats);

    final long threshold = (long)(stats.getMean() + stats.getStandardDeviation() * numStdDevs);

    System.err.println("   logFile=" + logfile);
    System.err.println("  numSites=" + stats.getN());
    System.err.println("  meanTime=" + MathUtil.timeString((long)(stats.getMean() + 0.5), false));
    System.err.println("    stdDev=" + MathUtil.timeString((long)(stats.getStandardDeviation() + 0.5), false));
    System.err.println(" threshold=" + MathUtil.timeString((long)(threshold + 0.5), false));

    // report domains that took longer than average to process.
    for (Map.Entry<String, Long> entry : domain2time.entrySet()) {
      final String domain = entry.getKey();
      final long time = entry.getValue();

      if (time > threshold) {
        System.out.println(domain + " : " + MathUtil.timeString(time, false));
      }
    }
  }

  private static Map<String, Long> loadDomainTimes(String logfile, StatsAccumulator stats) throws IOException {
    final Map<String, Long> domain2time = new LinkedHashMap<String, Long>();

    final BufferedReader reader = FileUtil.getReader(logfile);

    String line = null;
    Date lastDate = null;
    String lastDomain = null;

    while ((line = reader.readLine()) != null) {
      final int pos = line.indexOf(" -- Submitting ");
      if (pos >= 0) {
        final String timeString = line.substring(0, pos);
        final Date curDate = MathUtil.parseDate(timeString);

        if (lastDate != null) {
          final long diff = curDate.getTime() - lastDate.getTime();
          domain2time.put(lastDomain, diff);
          stats.add(diff);
        }

        lastDomain = extractDomain(line);
        lastDate = curDate;
      }
    }

    reader.close();

    return domain2time;
  }

  private static String extractDomain(String line) {
    final String[] pieces = line.split("'");
    return pieces[1];
/*
    final String[] pieces = line.split("/");
    final String domainString = pieces[pieces.length - 1];
    final int pos = domainString.indexOf("'");
    return domainString.substring(0, pos);
*/
  }

  public static void main(String[] args) throws IOException {
    //arg0: numStdDevs (double)
    //args1+: logfiles
    final double numStdDevs = Double.parseDouble(args[0]);

    for (int i = 1; i < args.length; ++i) {
      final String arg = args[i];
      locateLongRunningSites(arg, numStdDevs);
    }
  }
}
