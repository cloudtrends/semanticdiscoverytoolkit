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

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * Implementation of a trie for string matching.
 * <p>
 * @author Spence Koehler
 */
public interface Trie {

  /**
   * Add the given string to this trie.
   *
   * @return true if the string was added; false if it is already in this trie.
   */
  public boolean add(String string);

  /**
   * Search for the given complete string in this trie.
   *
   * @return true if the string is a complete entry in this trie; otherwise, false;
   */
  public boolean contains(String string);

  /**
   * Search for the given string as a prefix to a word in this trie.
   *
   * @return true if the prefix is in this trie; otherwise, false.
   */
  public boolean containsPrefix(String string);

  /**
   * Get the max depth, which is the length of the longest word in this trie.
   */
  public int getMaxDepth();

  /**
   * Get the number of words contained in this trie.
   */
  public int getNumWords();

  /**
   * Get the total number of characters that have been encoded in this trie.
   * <p>
   * This gives an introspective count of the total information contained in
   * the trie to track compression metrics.
   */
  public long getNumEncodedChars();

  /**
   * Serialize this trie.
   */
  public void dump(DataOutput dataOut) throws IOException;

  /**
   * Deserialize the data into this trie, replacing any existing trie data with
   * the contents.
   */
  public void read(DataInput dataIn) throws IOException;

}
