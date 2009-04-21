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
package org.sd.text.fixedtrie;


import org.sd.io.FileUtil;

import java.io.BufferedReader;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;


/**
 * Trie for storage and retrieval of strings of hex digits.
 * <p>
 * @author Spence Koehler
 */
public class HexTrie {

  private SparseFixedTrie trie;

  public HexTrie() {
  }

  public HexTrie(int numHexDigits, int initialSize) {
    this.trie = new SparseFixedTrie(numHexDigits, 16, initialSize);
  }

  public SparseFixedTrie getTrie() {
    return trie;
  }

  public void add(String hexDigitsString) {
    final int[] values = asValues(hexDigitsString);
    trie.add(values);
  }

  public boolean contains(String hexDigitsString) {
    final int[] values = asValues(hexDigitsString);
    return trie.contains(values);
  }

  private final int[] asValues(String hexDigitsString) {
    final int strlen = hexDigitsString.length();
    final int numValues = Math.min(strlen, trie.getDepth());
    final int[] values = new int[numValues];

    for (int i = 0; i < numValues; ++i) {
      final char c = hexDigitsString.charAt(i);
      values[i] = hexValue(c);
    }

    return values;
  }

  private final int hexValue(char c) {
    int result = 0;

    if (c <= '9') {
      result = c - 48;  // '0'
    }
    else if (c <= 'E') {
      result = c - 55;  // 'A' - 10;
    }
    else {
      result = c - 87;  // 'a' - 10
    }

    return result;
  }

  /**
   * Write this instance to the dataOutput stream such that it
   * can be completely reconstructed through this.read(dataInput).
   *
   * @param dataOutput  the data output to write to.
   */
  public void write(DataOutput dataOutput) throws IOException {
    trie.write(dataOutput);
  }

  /**
   * Read this instance's contents from the dataInput stream that was written by
   * this.write(dataOutput).
   * <p>
   * NOTE: this requires all implementing classes to have a default constructor
   *       with no args.
   *
   * @param dataInput  the data output to write to.
   */
  public void read(DataInput dataInput) throws IOException {
    this.trie = new SparseFixedTrie();
    trie.read(dataInput);
  }

  public static final HexTrie load(File file, int numBytes) throws IOException {
    HexTrie result = null;

    final File parentFile = file.getParentFile();
    final File trieFile = new File(parentFile, file.getName() + ".hxt");  // hex trie

    if (trieFile.exists()) {
      // load serialized hex trie
      result = new HexTrie();
      final InputStream inputStream = FileUtil.getInputStream(trieFile);
      final DataInputStream dataInput = new DataInputStream(inputStream);
      result.read(dataInput);
      dataInput.close();
      inputStream.close();
    }
    else {
      // load from file, then serialize.

      System.out.println(new Date() + ": loading '" + file + "'... (initialSize=" + numBytes + " bytes)");

      result = new HexTrie(32, numBytes);

      int numLines = 0;
      BufferedReader reader = FileUtil.getReader(file);
      String line = null;
      while ((line = reader.readLine()) != null) {
        final String hexString = line.split("\\s*\\|\\s*")[1];
        if (hexString.length() != 32) continue;

        result.add(hexString);
        ++numLines;

        if ((numLines % 1000) == 0) {
          System.out.println(new Date() + ": processed " + numLines + " size=" + result.getTrie().getSize());
        }
      }
      reader.close();
      System.out.println(new Date() + ": loaded " + numLines + ".  ...Serializing");

      // serialize
      final OutputStream outputStream = new FileOutputStream(trieFile);
      final DataOutputStream dataOutput = new DataOutputStream(outputStream);
      result.write(dataOutput);
      dataOutput.close();
      outputStream.close();
    }

    return result;
  }

  public static final void main(String[] args) throws IOException {
    //arg0: file to load w/ "x|32-hex|y" formatted lines
    //arg1: (optional) initial num bytes i.e. 1073741824

    // load trie
    final File file = new File(args[0]);
    final int numBytes = (args.length > 1) ? Integer.parseInt(args[1]) : 0;

    final HexTrie hexTrie = load(file, numBytes);


    if (args.length > 2) {
      for (int i = 2; i < args.length; ++i) {
        final String arg = args[i];
        System.out.println(arg + "|" + hexTrie.contains(arg));
      }
    }
  }
}
