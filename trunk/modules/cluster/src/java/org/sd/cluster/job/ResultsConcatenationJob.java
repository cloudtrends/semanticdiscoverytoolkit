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


import org.sd.cio.MessageHelper;
import org.sd.io.FileUtil;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A job that concatenates the final output from other jobs into a single
 * aggregation file.
 * <p>
 * @author Spence Koehler
 */
public class ResultsConcatenationJob extends SteadyStateJob {

  private String finalFilename;
  private transient Object outputFileMutex = new Object();  // for synchronizing writes

  public ResultsConcatenationJob() {
    super();
  }

  public ResultsConcatenationJob(int numThreads, String jobId, String groupName, String dataDirName, String finalFilename) {
    super(numThreads, jobId, groupName, dataDirName, null);

    this.finalFilename = finalFilename;
  }

  public String getDescription() {
    return "ResultsConcatenation steady state job runner (" + finalFilename + ")";
  }

  /**
   * Get the steady state job to which this job's output is to be sent, or null
   * if this is the last stage.
   * <p>
   * NOTE: This method will be called only once during the postHandleHook.
   */
  public SteadyStateJob getNextJob() {
    return null;  // all done.
  }

  /**
   * Run the product snippet extractor over the input. If output already exists
   * corresponding to the input, overwrite it. We currently assume that re-running
   * this job is because of changes that have been made to the processing, so
   * new results will be generated even if the data hasn't been "cleaned" out
   * from a prior run.
   *
   * @return the output job data files for input to the next stage's job (ok if
   *         empty); or null to report failure.
   */
  protected List<JobDataFile> doProcessing(JobDataFile workFile, JobDataDirectory outputDir, AtomicBoolean die) {
    List<JobDataFile> result = new ArrayList<JobDataFile>();

    final String inputFile = workFile.getAbsolutePath();
    final String workName = workFile.getName();   // an output file
    final String outName = finalFilename;         // container for merged results.

    final JobDataFile outFile = outputDir.getJobDataFile(outName);
    final String outFilePath = outFile.getAbsolutePath();

    synchronized (outputFileMutex) {
      try {
        final BufferedWriter writer = FileUtil.getWriter(outFilePath, true);

        final BufferedReader reader = FileUtil.getReader(inputFile);
        String line = null;
        while ((line = reader.readLine()) != null) {
          if (die != null && die.get()) break;
          writer.write(line);
          writer.newLine();
        }
        reader.close();
        writer.close();

        if (die != null && die.get()) return null;

        result.add(outFile);
      }
      catch (IOException e) {
        e.printStackTrace(System.err);
      }
    }

    return result;
  }

  /**
   * Write thie message to the dataOutput stream such that this message
   * can be completely reconstructed through this.read(dataInput).
   *
   * @param dataOutput  the data output to write to.
   */
  public void write(DataOutput dataOutput) throws IOException {
    super.write(dataOutput);

    MessageHelper.writeString(dataOutput, finalFilename);
  }

  /**
   * Read this message's contents from the dataInput stream that was written by
   * this.write(dataOutput).
   * <p>
   * NOTE: this requires all implementing classes to have a default constructor
   *       with no args.
   *
   * @param dataInput  the data output to write to.
   */
  public void read(DataInput dataInput) throws IOException {
    super.read(dataInput);

    this.finalFilename = MessageHelper.readString(dataInput);
  }
}
