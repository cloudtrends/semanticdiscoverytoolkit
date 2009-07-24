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
package org.sd.cluster.job.work;


import org.sd.cluster.config.ClusterContext;
import org.sd.io.Publishable;
import org.sd.io.FileUtil;
import org.sd.util.MathUtil;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Date;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A worker for testing that will drain a server's work to a flat file.
 * <p>
 * @author Spence Koehler
 */
public class DrainageWorker implements Worker {
  
  private String drainName;
  private boolean noOutput;
  private BufferedWriter bufferedWriter;
  private AtomicInteger workCount = new AtomicInteger(0);
  private long starttime;

  /**
   * Properties: "drainName"
   * Properties: "noOutput"
   */
  public DrainageWorker(Properties properties) {
    this.drainName = properties.getProperty("drainName", "drainage.txt");
    this.noOutput = "true".equals(properties.getProperty("noOutput", "false"));
  }

  /**
   * Initialize this worker.
   */
  public boolean initialize(ClusterContext clusterContext, String jobIdString, String dataDirName) {
    final String drainPath = clusterContext.getConfig().getOutputDataPath(dataDirName + "/", jobIdString + "." + drainName);
    try {
      this.bufferedWriter = FileUtil.getWriter(drainPath);
    }
    catch (IOException e) {
      throw new IllegalStateException(e);
    }
    this.starttime = System.currentTimeMillis();

    return true;
  }

  /**
   * Perform work for a job on the workUnit.
   * <p>
   * If destination is non-null and this worker generates more work, then add
   * the generated work to the destination queue.
   *
   * @return true if work was successfully completed; otherwise, false.
   */
  public boolean performWork(KeyedWork keyedWork, AtomicBoolean die, AtomicBoolean pause, QueueDesignator queueDesignator, WorkQueue destination) {
    if(!noOutput){
      final Publishable workUnit = keyedWork.getWork();
      if (workUnit != null) {
        synchronized (bufferedWriter) {
          try {
            bufferedWriter.write(workUnit.toString());
            bufferedWriter.newLine();
            bufferedWriter.flush();
          }
          catch (IOException e) {
            System.err.println(new Date() + ": couldn't write work='" + workUnit + "'!");
            e.printStackTrace(System.err);
          }
        }
      }
    }
    workCount.incrementAndGet();
    return true;
  }

  /**
   * Flush this worker.
   */
  public boolean flush(Publishable payload) {
    return true;  // nothing to do... is there?
  }

  /**
   * Close this worker.
   */
  public void close() {
    if (bufferedWriter != null) {
      try {
        bufferedWriter.close();
      }
      catch (IOException e) {
        e.printStackTrace(System.err);
      }
    }
  }

  public String toString() {
    final StringBuilder result = new StringBuilder();

    final long curtime = System.currentTimeMillis();
    final int units = workCount.get();
    final double rate = ((double)units / ((double)(curtime - starttime) / 1000.0));  // units/sec

    // <count> units in <time> at <rate> units/sec
    result.
      append(units).
      append(" units in ").
      append(MathUtil.timeString(curtime - starttime, false)).
      append(" at ").
      append(MathUtil.doubleString(rate, 3)).
      append(" units/sec");

    return result.toString();
  }


  /**
   * Get a status string or null.
   */
  public String getStatusString() {
    return toString();
  }
}
