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
import org.sd.util.tree.Tree;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Helper class to clean up html/xml.
 * <p>
 * @author Spence Koehler
 */
public class Tidy extends MainDirectoryWrapper {

  public Tidy(String inputPath, String outputPath, long timeLimit) {
    super("Tidy", inputPath, outputPath, ".gz", timeLimit);
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
//    return proposedOutputPath.replace(".html", ".xhtml");
    // don't change html to xhtml so that lynx can recognize/open the file.
    return proposedOutputPath;
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
    final Tree<XmlLite.Data> xmlTree = XmlFactory.readXmlTree(FileUtil.getFile(inputFilePath), false, true, die, true);

    if (xmlTree != null) {
      final BufferedWriter writer = FileUtil.getWriter(outputFilePath);
      XmlLite.writeXml(xmlTree, writer);
      writer.close();
    }

    return true;
  }

  // arg1: inputDir (i.e. /usr/local/share/data/blog/cache-400)
  // arg2: outputDir (i.e. /usr/local/share/data/block/cache-400-doc)
  // arg3: timelimit (optional -- in seconds)
  public static void main(String[] args) throws IOException {
    String inputPath = null;
    String outputPath = null;
    long timelimit = 0;

    if (args.length >= 2) {
      inputPath = args[0];
      outputPath = args[1];
      if (args.length == 3) {
        timelimit = Long.parseLong(args[2]) * 1000;  // convert seconds to millis
      }
    }

    if (inputPath != null && outputPath != null) {
      final Tidy tidy = new Tidy(inputPath, outputPath, timelimit);
      tidy.run(System.err, null);
    }
  }
}
