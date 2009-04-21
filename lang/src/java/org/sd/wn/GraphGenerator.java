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
package org.sd.wn;


import org.sd.io.FileUtil;
import org.sd.util.ExecUtil;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Utility class for generating word graphs.
 * <p>
 * @author Spence Koehler
 */
public class GraphGenerator {

  public static final PointerFilter DEFAULT_POINTER_FILTER = new ConsistentPointerFilter();

  public static final void generateGraph(WordSenseWrapper sense, File outputDir) throws IOException {
    generateGraph(sense, DEFAULT_POINTER_FILTER, outputDir);
  }

  public static final void generateGraph(WordSenseWrapper sense, PointerFilter pointerFilter, File outputDir) throws IOException {

    if (!outputDir.exists()) outputDir.mkdirs();

    // word.pos.lexName.senseNum
    final String senseNum = sense.getProperty(WordSenseContainer.SENSE_NUM_PROPERTY);
    final String fileName = sense.getWord() + "." + sense.getLexName() + "." + (senseNum == null ? "0" : senseNum);
    final String dotName = fileName + ".dot";
    final String pngName = fileName + ".png";
    final File dotFile = new File(outputDir, dotName);
    final File pngFile = new File(outputDir, pngName);

    if (!dotFile.exists()) {
      final Word2DotOperator operator = new Word2DotOperator();
      sense.expand(operator, pointerFilter);

      final BufferedWriter writer = FileUtil.getWriter(dotFile);
      operator.writeDot(writer);
      writer.close();
    }

    if (!pngFile.exists()) {
      final String dotCommand = "dot -Tpng " + dotFile.getAbsolutePath() + " -o " + pngFile.getAbsolutePath();
      ExecUtil.executeProcess(dotCommand);
    }
  }

  public static final void generateGraphs(String inputWord, File dictDir, File outputDir) throws IOException {
    generateGraphs(inputWord, DEFAULT_POINTER_FILTER, dictDir, outputDir);
  }

  public static final void generateGraphs(String inputWord, PointerFilter pointerFilter, File dictDir, File outputDir) throws IOException {
    final WordSenseContainer container = new WordSenseContainer(dictDir, inputWord);

    final List<WordSenseWrapper> senses = container.getWrappedWordSenses();
    for (WordSenseWrapper sense : senses) {
      generateGraph(sense, outputDir);
    }
  }


  // java -Xmx640m org.sd.wn.GraphGenerator /usr/local/share/download/wordnet/WordNet-3.0/dict "pump" ~/tmp/graphs/pump/
  public static final void main(String[] args) throws IOException {
    //arg0: dictDir
    //arg1: inputWord
    //arg2: outputDir

    try {
      final File dictDir = args[0].length() > 1 ? new File(args[0]) : null;
      final String inputWord = args[1];
      final File outputDir = new File(args[2]);

      generateGraphs(inputWord, dictDir, outputDir);
    }
    finally {
      WordNetUtils.closeAll();
    }
  }
}
