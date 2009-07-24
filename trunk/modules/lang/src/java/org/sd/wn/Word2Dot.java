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

import org.sd.util.DotMaker;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Iterator;
import java.util.List;

/**
 * Utility class for creating a dot-formatted graph from a word and its senses.
 * <p>
 * @author Spence Koehler
 */
public class Word2Dot extends DotMaker {
  
  private File dictDir;
  private String word;
  private PointerFilter pointerFilter;

  protected Word2Dot() {
    super();
  }

  public Word2Dot(File dictDir, String word, PointerFilter pointerFilter) throws IOException {
    super();

    this.dictDir = dictDir;
    this.word = word;
    this.pointerFilter = pointerFilter;
  }

  protected void populateEdges() {
    final String root = "input: " + word;
    final int nodeId = addId2Label(root);
    try {
      populateEdges(word, nodeId);
    }
    catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  private final void populateEdges(String word, int nodeId) throws IOException {
    for (Iterator<WordSenseWrapper> iter = new WordSenseIterator(dictDir, word); iter.hasNext(); ) {
      final WordSenseWrapper wrapper = iter.next();

      final Integer childId = addEdge(nodeId, wrapper);
      populateEdges(wrapper, childId);
    }
  }

  private final void populateEdges(WordSenseWrapper wrapper, int wrapperId) {

    final List<WordSenseWrapper> next = wrapper.getNext(pointerFilter);

    if (next != null) {
      for (WordSenseWrapper w : next) {
        final Integer childId = addEdge(wrapperId, w);
        populateEdges(w, childId);
      }
    }
  }

  protected final Integer addEdge(int parentId, WordSenseWrapper child) {
    final String childLabel = getLabel(child);
    Integer childId = findLabelId(childLabel);
    if (childId == null) {
      childId = addId2Label(childLabel);
    }

    String edgeLabel = null;

    final PointerSymbol ps = child.getRelationFromSource();
    if (ps != null) {
      edgeLabel = ps.name().toLowerCase();
    }

    addEdge(parentId, childId, edgeLabel);

    return childId;
  }

  protected final String getLabel(WordSenseWrapper wrapper) {
    final StringBuilder result = new StringBuilder();

    final int wordNum = wrapper.getWordId().wordNum;
    if (wordNum == 0) {
      result.append(wrapper.getWordSense().getFileEntry());
    }
    else {
      result.
        append('(').append(wrapper.getLexName()).append(')').
        append(wrapper.getWord());
    }

    return result.toString();
  }

  //java -Xmx640m org.sd.wn.Word2Dot /usr/local/share/download/wordnet/WordNet-3.0/dict "pump"
  //java -Xmx640m org.sd.wn.Word2Dot /usr/local/share/download/wordnet/WordNet-3.0/dict "butterfly valve" 3 > temp.dot
  //dot -Tpng temp.dot -o temp.png
  public static void main(String[] args) throws IOException {
    //arg0: dictDir
    //arg1: word
    //arg2: depth

    if (args.length != 3) {
      System.out.println("Usage: Word2Dot wordNetDictDir inputWord depth");
    }
    else {
      try {
        final File dictDir = args[0].length() > 1 ? new File(args[0]) : null;
        final String input = args[1];
        final Integer depth = new Integer(args[2]);

        final ConsistentPointerFilter pointerFilter = new ConsistentPointerFilter();
        pointerFilter.setMaxDepth(depth);

        final Word2Dot word2dot = new Word2Dot(dictDir, input, pointerFilter);
        final BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(System.out));
        word2dot.writeDot(writer);
        writer.close();
      }
      finally {
        WordNetUtils.closeAll();
      }
    }
  }
}
