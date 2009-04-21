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
package org.sd.xml;


import org.sd.io.FileUtil;
import org.sd.util.MainDirectoryWrapper;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A utility to run over tidied xhtml files, extracting patterns from the
 * text portions.
 * <p>
 * @author Spence Koehler
 */
public class XhtmlGrep extends MainDirectoryWrapper {
  
  private String outputFile;
  private Pattern[] patterns;
  private BufferedWriter outputFileWriter;

  /**
   * Traverse (.gz) files under the input path searching for patterns, writing
   * (appending) output to outputFile (or stdout if null).
   */
  public XhtmlGrep(String inputPath, String outputFile, Pattern[] patterns) {
    super("XhtmlGrep", inputPath, null, ".gz", 0);
    this.outputFile = outputFile;
    this.patterns = patterns;
    this.outputFileWriter = null;
  }

  /**
   * Fix the proposed output path if needed.
   * <p>
   * For example, if the operation is converting html files to xml, then
   * an implementation would proposedOutputPath.replace(".html", ".xml").
   *
   * @param proposedOutputPath  Same name as the input path but under the
   *                            output path directory.
   */
  protected String fixOutputPath(String proposedOutputPath) {
    return null;
  }

  protected boolean preRunHook() {
    if (outputFile != null) {
      try {
        outputFileWriter = FileUtil.getWriter(outputFile, true);
      }
      catch (IOException e) {
        throw new IllegalStateException("IOException opening '" + outputFile + "'!", e);
      }
    }
    return true;
  }

  protected boolean postRunHook() {
    if (outputFileWriter != null) {
      try {
        outputFileWriter.close();
        outputFileWriter = null;
      }
      catch (IOException e) {
        // ignore
      }
    }
    return true;
  }

  private final void dumpLine(String inputFilePath, int lineNum, String line) throws IOException {
    if (outputFileWriter != null) {
      outputFileWriter.write(inputFilePath);
      outputFileWriter.write(":");
      outputFileWriter.write(Integer.toString(lineNum));
      outputFileWriter.write(":");
      outputFileWriter.write(line);
      outputFileWriter.newLine();
    }
    else {
      System.out.println(inputFilePath + ":" + lineNum + ":" + line);
    }
  }

  protected boolean lineMatches(String line) {
    if (patterns != null) {
      for (Pattern pattern : patterns) {
        final Matcher m = pattern.matcher(line);
        if (m.matches()) return true;
      }
    }
    return false;
  }

  /**
   * Operate on the given file path.
   *
   * @param inputFilePath   The path to the input file to operate over.
   * @param outputFilePath  The output path corresponding to the input file.
   *
   * @return true to keep operating; false to stop.
   */
  protected boolean operate(String inputFilePath, String outputFilePath, AtomicBoolean die) throws IOException {
    final BufferedReader reader = FileUtil.getReader(inputFilePath);

    String line;
    int lineNum = 0;
    while ((line = reader.readLine()) != null) {
      if (die != null && die.get()) return false;
      if (line.length() > 0 && line.charAt(0) != '<') {
        // check for pattern(s), spit out line if matches.
        if (lineMatches(line)) dumpLine(inputFilePath, lineNum, line);
      }
      ++lineNum;
    }

    return true;
  }

  // arg1: inputDir (i.e. /usr/local/share/data/blog/cache-400)
  // arg2: pattern
  // arg3: [outputFile]
  public static void main(String[] args) throws IOException {
    String inputPath = null;
    String outputPath = null;
    String patternString = null;

    if (args.length >= 2) {
      inputPath = args[0];
      patternString = args[1];

      if (args.length == 3) {
        outputPath = args[2];
      }
    }

    if (inputPath != null && patternString != null) {
      final Pattern pattern = Pattern.compile(patternString);

      final XhtmlGrep xhtmlGrep = new XhtmlGrep(inputPath, outputPath, new Pattern[]{pattern});
      xhtmlGrep.run(System.err, null);
    }
  }
}
