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

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A main wrapper that operates on files while recursing through directories.
 * <p>
 * @author Spence Koehler
 */
public abstract class MainDirectoryWrapper extends MainWrapper {
  
  private String description;
  private String inputPath;
  private String outputPath;
  private String fileSuffixFilter;

  private int inputPathLen;

  /**
   * Operate on the given file path.
   *
   * @param inputFilePath   The path to the input file to operate over.
   * @param outputFilePath  The output path corresponding to the input file.
   * @param die             Boolean for impl to monitor for kill processing signal.
   *
   * @return true to keep operating; false to stop.
   */
  protected abstract boolean operate(String inputFilePath, String outputFilePath, AtomicBoolean die) throws IOException;

  /**
   * Fix the proposed output path if needed.
   * <p>
   * For example, if the operation is converting html files to xml, then
   * an implementation would proposedOutputPath.replace(".html", ".xml").
   * <p>
   * If the operation does not create an output file for each input file,
   * then this method should return null to avoid creating output dirs.
   *
   * @param proposedOutputPath  Same name as the input path but under the
   *                            output path directory.
   *
   * @return the fixed output path or null.
   */
  protected abstract String fixOutputPath(String proposedOutputPath);

  /**
   * Construct an instance.
   *
   * @param description       A description of this instance.
   * @param inputPath         The path to the directory (or file) to begin operating over.
   * @param outputPath        The path to the directory to write results to. (possibly null)
   * @param fileSuffixFilter  If non-null, only files ending with this filter will be operated on.
   * @param timeLimit         A limit on the length of time to run in seconds. 0 for unlimited.
   */
  protected MainDirectoryWrapper(String description, String inputPath, String outputPath, String fileSuffixFilter, long timeLimit) {
    super(timeLimit);

    this.description = description;
    this.inputPath = new File(inputPath).getAbsolutePath();
    this.outputPath = outputPath != null ? new File(outputPath).getAbsolutePath() : this.inputPath;
    this.fileSuffixFilter = fileSuffixFilter;

    this.inputPathLen = inputPath.length();
  }
  
  protected String getDescription() {
    return description + " w/input=" + inputPath + " output=" + outputPath + "...";
  }

  protected boolean doRun(PrintStream verbose, AtomicInteger numOperations, long startTime, AtomicBoolean die) throws IOException {
    return run(verbose, new File(inputPath), numOperations, startTime, die);
  }

  private final boolean run(PrintStream verbose, File inputFile, AtomicInteger numOperations, long startTime, AtomicBoolean die) throws IOException {
    boolean result = true;

    final String filePath = inputFile.getAbsolutePath();
    if (inputFile.isDirectory()) {
      if (filePath.charAt(filePath.length() - 1) == '.') return true;
      final File[] files = inputFile.listFiles();
      for (File file : files) {
        if (!run(verbose, file, numOperations, startTime, die)) return false;
      }
    }
    else {
      if (fileSuffixFilter == null || filePath.endsWith(fileSuffixFilter)) {
        final String curOutputPath = buildOutputPath(filePath);

        // mkdirs if needed
        if (curOutputPath != null) {
          final File outputDir = new File(FileUtil.getBasePath(curOutputPath));
          if (!outputDir.exists()) outputDir.mkdirs();
        }

        // check whether we've already done this one
        if (curOutputPath != null && new File(curOutputPath).exists()) {
          // already did this one. skip it and reset the startTime.
          startTime = System.currentTimeMillis();
        }
        else {
          try {
            result = operate(filePath, curOutputPath, die);
          }
          catch (Throwable t) {
            final PrintStream out = (verbose == null) ? System.err : verbose;
            out.println("!ERROR!: inputPath=" + filePath + " outputPath=" + outputPath);
            t.printStackTrace(out);
            result = false;
          }

          // increment stats
          if (result) {
            result = increment(verbose, numOperations, startTime);
          }
        }
      }
    }

    return result;
  }

  private final String buildOutputPath(String filePath) {
    final String mirrorPath = outputPath + filePath.substring(inputPathLen);
    return fixOutputPath(mirrorPath);
  }
}
