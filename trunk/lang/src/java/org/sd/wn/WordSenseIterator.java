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


import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Iterator;
import java.util.List;

/**
 * Iterator over word senses for input.
 * <p>
 * @author Spence Koehler
 */
public class WordSenseIterator implements Iterator<WordSenseWrapper> {

  private SenseIndex senseIndex;
  private List<WordSense> wordSenses;
  private Iterator<WordSense> iter;

  public WordSenseIterator(File dictDir, String input) throws IOException {
    this.senseIndex = SenseIndex.getInstance(dictDir);
    this.wordSenses = senseIndex.getWordSenses(input);
    this.iter = (wordSenses != null) ? wordSenses.iterator() : null;
  }

  public boolean hasNext() {
    return iter != null && iter.hasNext();
  }

  public WordSenseWrapper next() {
    WordSenseWrapper result = null;

    if (iter != null && iter.hasNext()) {
      result = new WordSenseWrapper(iter.next());
    }

    return result;
  }

  public void remove() {
    //nothing to do.
  }


  public static final void showAll(PrintStream out, File dictDir, String input, PointerFilter pointerFilter, WordSenseOperator operator) throws IOException {
    out.println(input + " -->");

    final WordSenseContainer container = new WordSenseContainer(dictDir, input);
    final List<WordSenseWrapper> senses = container.getWrappedWordSenses();
    for (WordSenseWrapper sense : senses) {
      sense.expand(operator, pointerFilter);
    }
  }

  public static final void showAll(PrintStream out, File dictDir, String[] args, int startIndex, PointerFilter pointerFilter) throws IOException {
    final WordSenseOperator operator = new ShowSenseOperator(out);

    for (int i = startIndex; i < args.length; ++i) {
      final String input = args[i];
      showAll(out, dictDir, input, pointerFilter, operator);
    }
  }

  //java -Xmx640m org.sd.wn.WordSenseIterator /usr/local/share/download/wordnet/WordNet-3.0/dict 0 "butterfly valve"
  public static void main(String[] args) throws IOException {
    //arg0: dictFile
    //arg1: maxDepth
    //args2+: words to lookup

    try {
      final File dictDir = args[0].length() > 1 ? new File(args[0]) : null;
      final Integer maxDepth = new Integer(args[1]);
      final int wordsIndex = 2;

      final ConsistentPointerFilter pointerFilter = new ConsistentPointerFilter();
      pointerFilter.setMaxDepth(maxDepth);

      showAll(System.out, dictDir, args, wordsIndex, pointerFilter);
    }
    finally {
      WordNetUtils.closeAll();
    }
  }
}
