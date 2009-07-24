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
package org.sd.text;


import org.sd.io.FileUtil;
import org.sd.util.StringUtil;

import java.io.BufferedReader;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Implementation of a trie for string matching.
 * <p>
 * @author Spence Koehler
 */
public class BruteForceTrie implements Trie {

  private Node root;
  private int maxDepth;
  private long numEncodedChars;

  private transient int numComplete = 0;

  /**
   * Load a trie that has been dumped to a file.
   */
  public static final BruteForceTrie loadTrie(String filename) throws IOException {
    final BruteForceTrie result = new BruteForceTrie();
    DataInputStream dataIn = null;
    try {
      System.out.println("loading trie from '" + new java.io.File(filename).getAbsolutePath() + "'...");
      dataIn = new DataInputStream(new FileInputStream(filename));
      result.read(dataIn);
    }
    finally {
      if (dataIn != null) dataIn.close();
    }
    return result;
  }

  public BruteForceTrie() {
    this.root = new Node(0);
    this.maxDepth = 0;
    this.numEncodedChars = 0L;
  }

  public BruteForceTrie(String[] initialStrings) {
    this();
    for (String string : initialStrings) {
      add(string);
    }
  }

  public final int getMaxDepth() {
    return maxDepth;
  }

  public final int getNumWords() {
    return numComplete;
  }

  public final long getNumEncodedChars() {
    return numEncodedChars;
  }

  public final boolean add(String string) {
    if (contains(string)) return false;

    Node curNode = root;
    for (StringUtil.StringIterator iter = new StringUtil.StringIterator(string); iter.hasNext(); ) {
      final StringUtil.StringPointer pointer = iter.next();
      curNode = curNode.make(pointer.codePoint);
    }
    curNode.setComplete(true);


    final int len = string.length();
    if (len > maxDepth) maxDepth = len;
    numEncodedChars += len;

    return true;
  }

  /**
   * Search for the given complete string in this trie.
   * 
   * @return true if the string is a complete entry in this trie; otherwise, false.
   */
  public boolean contains(String string) {
    Node curNode = root;
    for (StringUtil.StringIterator iter = new StringUtil.StringIterator(string); iter.hasNext(); ) {
      final StringUtil.StringPointer pointer = iter.next();
      curNode = curNode.get(pointer.codePoint);
      if (curNode == null) return false;
    }
    return curNode.isComplete();
  }

  /**
   * Search for the code points from startPos (inclusive) to endPos (exclusive)
   * for a complete string in this trie.
   * 
   * @return true if the code points are a complete entry in this trie; otherwise, false.
   */
  public boolean search(int[] codePoints, int startPos, int endPos) {
    Node curNode = root;
    for (int i = startPos; i < endPos; ++i) {
      curNode = curNode.get(codePoints[i]);
      if (curNode == null) return false;
    }
    return curNode.isComplete();
  }

  /**
   * Determine whether the given string is at least a partial match to strings
   * in this trie.
   *
   * @return true if the string matches an entry in this trie (possibly incompletely); otherwise, false.
   */
  public boolean containsPrefix(String string) {
    Node curNode = root;
    for (StringUtil.StringIterator iter = new StringUtil.StringIterator(string); iter.hasNext(); ) {
      final StringUtil.StringPointer pointer = iter.next();
      curNode = curNode.get(pointer.codePoint);
      if (curNode == null) return false;
    }
    return true;
  }

  /**
   * Determine whether the code points from startPos (inclusive) to endPos (exclusive)
   * are at least a partial match to strings in this trie.
   *
   * @return true if the code points are a partial entry in this trie; otherwise, false.
   */
  public boolean match(int[] codePoints, int startPos, int endPos) {
    Node curNode = root;
    for (int i = startPos; i < endPos; ++i) {
      curNode = curNode.get(codePoints[i]);
      if (curNode == null) return false;
    }
    return true;
  }

  public void dump(DataOutput dataOut) throws IOException {
    dataOut.writeInt(maxDepth);
    dataOut.writeLong(numEncodedChars);
    root.dump(dataOut);
  }

  public void read(DataInput dataIn) throws IOException {
    this.maxDepth = dataIn.readInt();
    this.numEncodedChars = dataIn.readLong();
    root.read(dataIn);
  }

  public String toString() {
    final StringBuilder result = new StringBuilder();

    result.
      append("trie[numWords=").append(numComplete).
      append(",maxDepth=").append(maxDepth).
      append(",numChars=").append(numEncodedChars).
      append(']');

    return result.toString();
  }

  private final class Node {
    private Map<Integer, Node> cp2child;  // codePoint -> childNode
    private int codePoint;
    private boolean complete;

    Node() {
    }

    Node(int codePoint) {
      this.codePoint = codePoint;
      this.complete = false;
      this.cp2child = null;
    }

    public Node make(int codePoint) {
      if (cp2child == null) cp2child = new HashMap<Integer, Node>();
      Node result = cp2child.get(codePoint);
      if (result == null) {
        result = new Node(codePoint);
        cp2child.put(codePoint, result);
      }
      return result;
    }

    public Node get(int codePoint) {
      return (cp2child == null) ? null : cp2child.get(codePoint);
    }

    public void setComplete(boolean complete) {
      if (complete && !this.complete) ++numComplete;
      this.complete = complete;
    }

    public boolean isComplete() {
      return complete;
    }

    public void dump(final DataOutput out) throws IOException {
      out.writeInt(codePoint);
      out.writeBoolean(complete);

      if (cp2child == null) {
        out.writeInt(0);
      }
      else {
        out.writeInt(cp2child.size());
        for (Map.Entry<Integer, Node> entry : cp2child.entrySet()) {
          out.writeInt(entry.getKey());
          entry.getValue().dump(out);
        }
      }
    }

    public void read(DataInput in) throws IOException {
      this.codePoint = in.readInt();
      this.complete = in.readBoolean();
      if (complete) ++numComplete;

      final int numChildren = in.readInt();
      if (numChildren > 0) {
        this.cp2child = new HashMap<Integer, Node>();
        for (int i = 0; i < numChildren; ++i) {
          final int key = in.readInt();
          final Node value = new Node();
          value.read(in);
          cp2child.put(key, value);
        }
      }
    }
  }

  public static void main(String[] args) throws IOException {
    //arg0: output trie dat file.
    //args1+: files with input words (1 per line)

    final String outputDatFile = args[0];
    final BruteForceTrie trie = new BruteForceTrie();

    for (int i = 1; i < args.length; ++i) {
      final String inputWordsFile = args[i];
      System.out.println("loading '" + inputWordsFile + "'...");

      final BufferedReader reader = FileUtil.getReader(inputWordsFile);
      String line = null;
      while ((line = reader.readLine()) != null) {
        trie.add(line.trim());
      }
      reader.close();

      System.out.println("\t" + trie);
    }

    System.out.println("Writing trie to '" + outputDatFile + "'");
    final DataOutputStream dataOut = new DataOutputStream(new FileOutputStream(outputDatFile));
    trie.dump(dataOut);
    dataOut.close();

    System.out.println("\t" + trie);
  }
}
