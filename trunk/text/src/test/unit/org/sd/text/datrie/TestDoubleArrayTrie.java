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


import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * JUnit Tests for the DoubleArrayTrie class.
 * <p>
 * @author Spence Koehler
 */
public class TestDoubleArrayTrie extends TestCase {

  public TestDoubleArrayTrie(String name) {
    super(name);
  }
  
  private static final void addWord(DoubleArrayTrie trie, String word, boolean verbose) {
    if (verbose) {
      System.out.println("adding '" + word + "'...");
    }
    trie.add(word);
  }

  private static final void checkWord(DoubleArrayTrie trie, String word, boolean expected, boolean verbose) {
    final boolean isWord = trie.contains(word);
    if (verbose) {
      System.out.println(word + " -> " + isWord);
    }
    assertEquals("expected '" + expected + "' but got '" + isWord + "' for word=" + word,
                 expected, isWord);
  }

  public void testBasics() {
    final DoubleArrayTrie trie = new DoubleArrayTrie();

    final String[] words = new String[] {
      "pool", "prize", "preview", "prepare", "produce", "progress",
      "predicament", "proprietary", "punk",
    };

    // make sure words aren't there yet.
    for (String word : words) {
      checkWord(trie, word, false, false);
    }

    // add all words.
    for (String word : words) {
      addWord(trie, word, false);
    }

    // make sure words are there now.
    for (String word : words) {
      checkWord(trie, word, true, false);
    }

    // make sure a partial word isn't there.
    checkWord(trie, "prev", false, false);

    // add the partial word
    addWord(trie, "prev", false);

    // make sure all old words are still there.
    for (String word : words) {
      checkWord(trie, word, true, false);
    }

    // check the partial word is there now.
    checkWord(trie, "prev", true, false);

    // make sure longer words don't match erroneously.
    checkWord(trie, "punky", false, false);
  }

  private final void checkBase(int[] base) {
    // make sure free bases point backward and wrap around.
    final int len = base.length;

    int b = -base[0];
    int expected = len - 1;
    int count = 0;

    while (b != 0) {
      assertEquals(expected, b);

      expected--;
      b = -base[b];
      ++count;
    }

    assertEquals(len - 1, count);

    assertEquals(-(len - 1), base[0]);
  }

  private final void checkCheck(int[] check) {
    // make sure free checks point forward and wrap around.
    final int len = check.length;

    int c = -check[0];
    int expected = 1;
    int count = 0;

    while (c != 0) {
      assertEquals(expected, c);

      expected++;
      c = -check[c];
      ++count;
    }

    assertEquals(len - 1, count);

    assertEquals(-1, check[0]);
    assertEquals(0, check[len - 1]);
    assertEquals(-(len - 1), check[len - 2]);
  }

  public void testGrow() {
    final DoubleArrayTrie trie = new DoubleArrayTrie();

    checkBase(trie.getBase());
    checkCheck(trie.getCheck());

    final int preBaseSize = trie.getBase().length;
    final int preCheckSize = trie.getCheck().length;
    assertEquals(preBaseSize, preCheckSize);

    trie.grow();

    final int postBaseSize = trie.getBase().length;
    final int postCheckSize = trie.getCheck().length;
    assertEquals(postBaseSize, postCheckSize);

    assertTrue(postBaseSize > preBaseSize);
    assertTrue(postCheckSize > preCheckSize);

    checkBase(trie.getBase());
    checkCheck(trie.getCheck());
  }

  public void testShrink() {
    final DoubleArrayTrie trie = new DoubleArrayTrie();

    final String[] words = new String[] {
      "pool", "prize", "preview", "prepare", "produce", "progress",
    };

    for (String word : words) {
      addWord(trie, word, false);
    }

    final int preBaseSize = trie.getBase().length;
    final int preCheckSize = trie.getCheck().length;
    assertEquals(preBaseSize, preCheckSize);

    trie.shrink();

    final int postBaseSize = trie.getBase().length;
    final int postCheckSize = trie.getCheck().length;
    assertEquals(postBaseSize, postCheckSize);

    assertTrue(postBaseSize < preBaseSize);
    assertTrue(postCheckSize < preCheckSize);

    // check that wrap-arounds are correct...
    final int[] base = trie.getBase();
    final int[] check = trie.getCheck();
    assertEquals(-(base.length - 1), base[0]);
    assertEquals(0, check[check.length - 1]);

    for (String word : words) {
      checkWord(trie, word, true, false);
    }
    checkWord(trie, "prev", false, false);

    final String[] moreWords = new String[]{"predicament", "proprietary", "punk", "zoolander", "zztop"};
    final int preGrowSize = trie.getBase().length;
    for (String word : moreWords) {
      addWord(trie, word, false);
    }
    final int postGrowSize = trie.getBase().length;
    assertTrue(postGrowSize > preGrowSize);

    for (String word : words) {
      checkWord(trie, word, true, false);
    }
    for (String word : moreWords) {
      checkWord(trie, word, true, false);
    }
    checkWord(trie, "prev", false, false);
  }

  public void testDumpAndRead() throws IOException {
    final DoubleArrayTrie trie = new DoubleArrayTrie();

    final String[] words = new String[] {
      "pool", "prize", "preview", "prepare", "produce", "progress",
      "predicament", "proprietary", "punk",
    };

    // add all words.
    for (String word : words) {
      addWord(trie, word, false);
    }
    
    // make sure words are there now.
    for (String word : words) {
      checkWord(trie, word, true, false);
    }

    // dump trie data
    final ByteArrayOutputStream bytesOut = new ByteArrayOutputStream();
    final DataOutputStream dataOut = new DataOutputStream(bytesOut);
    trie.dump(dataOut);
    dataOut.close();
    bytesOut.close();

    // load dumped data
    final byte[] bytes = bytesOut.toByteArray();
//System.out.println("nbytes=" + bytes.length);
    final ByteArrayInputStream bytesIn = new ByteArrayInputStream(bytes);
    final DataInputStream dataIn = new DataInputStream(bytesIn);
    final DoubleArrayTrie trie2 = DoubleArrayTrie.loadTrie(dataIn);
    dataIn.close();

    // make sure words are there.
    for (String word : words) {
      checkWord(trie2, word, true, false);
    }
  }

  public void testAddAndReAdd() {
    final DoubleArrayTrie trie = new DoubleArrayTrie();

    final String[] words = new String[] {
      "pool", "prize", "preview", "prepare", "produce", "progress",
      "predicament", "proprietary", "punk",
    };

    // add all words.
    for (String word : words) {
      addWord(trie, word, false);
    }
    
    // make sure words are there now.
    for (String word : words) {
      checkWord(trie, word, true, false);
    }

    // verify count.
    assertEquals(words.length, trie.getNumWords());

    // add all words.
    for (String word : words) {
      addWord(trie, word, false);
    }
    
    // make sure words are there now.
    for (String word : words) {
      checkWord(trie, word, true, false);
    }

    // verify count.
    assertEquals(words.length, trie.getNumWords());
  }

  public static Test suite() {
    TestSuite suite = new TestSuite(TestDoubleArrayTrie.class);
    return suite;
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}
