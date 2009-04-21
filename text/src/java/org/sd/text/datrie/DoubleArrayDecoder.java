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


import org.sd.util.tree.Tree;

import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Decode a double-array trie.
 * <p>
 * @author Spence Koehler
 */
public class DoubleArrayDecoder {
  
  private DoubleArrayTrie trie;
  private Tree<Data> root;

  public DoubleArrayDecoder(DoubleArrayTrie trie) {
    this.trie = trie;
    init();
  }

  private final void init() {
    this.root = new Tree<Data>(new Root(1));

    LinkedList<Tree<Data>> openList = new LinkedList<Tree<Data>>();
    openList.add(root);

    while (!openList.isEmpty()) {
      final Tree<Data> curNode = openList.removeFirst();
      final Data data = curNode.getData();
      final int s = data.getIndex();
      final int[] chars = trie.getChars(s);

      if (chars.length == 0) {
        final String tail = trie.getTail(s);
        final Leaf leaf = new Leaf(s, tail);
        final Tree<Data> child = new Tree<Data>(leaf);
        curNode.addChild(child);
      }
      else {
        for (int c : chars) {
          final int t = trie.getNextState(s, c);
          final Letter letter = new Letter(t, c);
          final Tree<Data> child = new Tree<Data>(letter);
          openList.add(child);
          curNode.addChild(child);
        }
      }
    }
  }

  public List<String> decode() {
    List<String> result = new ArrayList<String>();

    List<Tree<Data>> leaves = root.gatherLeaves();
    for (Tree<Data> leafNode : leaves) {
      final StringBuilder builder = new StringBuilder();

      Tree<Data> node = leafNode;
      
      final Leaf leafData = leafNode.getData().asLeaf();
      if (leafData != null) {
        final String tail = leafData.getTail();
        if (tail != null) builder.append(tail);
        node = node.getParent();
      }

      while (node != null) {
        final Letter letter = node.getData().asLetter();
        if (letter == null) break;
        builder.insert(0, letter.getC());
        node = node.getParent();
      }

      if (builder.length() > 0) {
        final String s = builder.substring(0, builder.length() - 1);
        result.add(s);
      }
    }

    return result;
  }

  private static interface Data {
    public Root asRoot();
    public Letter asLetter();
    public Leaf asLeaf();

    public int getIndex();
  }

  public static abstract class AbstractData implements Data {
    private int index;

    protected AbstractData(int index) {
      this.index = index;
    }

    public Root asRoot() { return null; }
    public Letter asLetter() { return null; }
    public Leaf asLeaf() { return null; }

    public int getIndex() { return index; }
  }

  public static final class Root extends AbstractData {
    Root(int index) { super(index); }
    public final Root asRoot() { return this; }
  }

  public static final class Letter extends AbstractData {
    private char c;

    Letter(int index, int c) {
      super(index);
      this.c = (char)c;
    }

    public final char getC() {
      return c;
    }

    public final Letter asLetter() { return this; }
  }

  public static final class Leaf extends AbstractData {
    private String tail;

    Leaf(int index, String tail) {
      super(index); 
      this.tail = tail;
    }

    public final String getTail() {
      return tail;
    }

    public final Leaf asLeaf() { return this; }
  }

  public static void decode(DoubleArrayTrie trie, PrintStream out) throws IOException {
    final DoubleArrayDecoder decoder = new DoubleArrayDecoder(trie);
    final List<String> terms = decoder.decode();
    for (String term : terms) {
      out.println(term);
    }
  }

  public static void main(String[] args) throws IOException {
    //arg0: double array trie data file. (input)
    //stdout output

    final DoubleArrayTrie trie = DoubleArrayTrie.loadTrie(args[0]);
    decode(trie, System.out);
  }
}
