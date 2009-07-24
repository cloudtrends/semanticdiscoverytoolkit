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
package org.sd.text.datrie;


import org.sd.util.MathUtil;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

/**
 * Utility to do timing tests on the trie.
 * <p>
 * @author Spence Koehler
 */
public class TrieTimer {
  
  private static void timeTrie(DoubleArrayTrie trie, String[] words, long limit) {
    final long startTime = System.currentTimeMillis();
    final Random random = new Random(startTime);
    long count = 0L;

    while (true) {
      final int index = random.nextInt(words.length);
      trie.contains(words[index]);
      ++count;

      if ((count % 10000000) == 0) {
        final long curTime = System.currentTimeMillis();
        final long elapsed = curTime - startTime;
        final double rate = ((double)count / (double)elapsed);
        System.out.println("looked up " + count + " words in " + MathUtil.timeString(elapsed, false) + " rate=" + MathUtil.doubleString(rate, 3) + " words/ms");

        if (elapsed > limit) break;
      }
    }
  }

  private static void timeHash(HashSet<String> set, String[] words, long limit) {
    final long startTime = System.currentTimeMillis();
    final Random random = new Random(startTime);
    long count = 0L;

    while (true) {
      final int index = random.nextInt(words.length);
      set.contains(words[index]);
      ++count;

      if ((count % 10000000) == 0) {
        final long curTime = System.currentTimeMillis();
        final long elapsed = curTime - startTime;
        final double rate = ((double)count / (double)elapsed);
        System.out.println("looked up " + count + " words in " + MathUtil.timeString(elapsed, false) + " rate=" + MathUtil.doubleString(rate, 3) + " words/ms");

        if (elapsed > limit) break;
      }
    }
  }

  public static void main(String[] args) throws IOException {
    //arg0: double array trie data file.
    //arg1: "trie" or "set"

    final String trieFilename = args[0];
    final boolean timeTrie = "trie".equals(args[1]);

    DoubleArrayTrie trie = DoubleArrayTrie.loadTrie(args[0]);

    final DoubleArrayDecoder decoder = new DoubleArrayDecoder(trie);
    final List<String> wordsList = decoder.decode();
    final String[] words = wordsList.toArray(new String[wordsList.size()]);
    wordsList.clear();

    final long limit = 5 * 60 * 1000;  // 5 min.

    if (timeTrie) {
      timeTrie(trie, words, limit);
    }
    else {
      trie = null;
      final HashSet<String> set = new HashSet<String>();
      for (String word : words) set.add(word);
      timeHash(set, words, limit);
    }
  }
}
