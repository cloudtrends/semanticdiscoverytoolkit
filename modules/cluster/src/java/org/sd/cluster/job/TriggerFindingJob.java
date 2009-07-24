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


import org.sd.cluster.config.Config;
import org.sd.io.FileUtil;

/**
 * Job output processor for finding trigger domains.
 * <p>
 * @author Spence Koehler
 */
public class TriggerFindingJob extends JobOutputProcessor {

  private transient String inputFile;
  private transient String outputFile;
  private transient String triggerDomain;

  /**
   * Hook called when we start handling the job.
   * <p>
   * A common operation is to initialize the totalOps.
   *
   * @return true to start handling right away; false to delay handling
   *         until manually started.
   */
  protected boolean startHandlingHook() {
    final Config config = getConfig();

    this.inputFile = config.getJobDataPath(getOriginalJobId(), getDataDirName());
    this.outputFile = config.getOutputDataPath(getOriginalJobId(), getDataDirName());
    final String lastOutputLine = FileUtil.getLastLine(outputFile);

    if (lastOutputLine != null) {
      final String[] pieces = lastOutputLine.split("\\s*\\|\\s*");
      this.triggerDomain = pieces[0];
    }
    else {
      this.triggerDomain = null;
    }

    return true;
  }

  public void start() {
    System.out.println(inputFile + "|" + outputFile);
  }
}
