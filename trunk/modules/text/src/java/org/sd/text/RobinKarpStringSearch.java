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


import org.sd.io.DataHelper;
import org.sd.nlp.GeneralNormalizedString;
import org.sd.nlp.NormalizedString;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Implementation of the Robin-Karp substring search algorithm.
 * <p>
 * This algorithm is good when multiple substrings are searched for within
 * a string.
 * <p>
 * This implementation uses the NormalizedString abstraction.
 *
 * @author Spence Koehler
 */
public class RobinKarpStringSearch {
  
  private int primeBase;
//  private String[] substringsToFind;

  // auxiliary data
  private Map<Integer, HashData[]> length2hashes;
  private int[] lengths;
  private Map<Integer, Long> n2primeToN;

  private final Object primeLock = new Object();

  /**
   * Default constructor for reconstruction through 'read'.
   */
  public RobinKarpStringSearch() {
  }

  /**
   * Construct using the given primeBase for the rolling hash and
   * the given substrings to find.
   *
   * @param primeBase         A prime base to use for the rolling hash.
   * @param substringsToFind  The (normalized) substrings to search for.
   */
  public RobinKarpStringSearch(int primeBase, String[] substringsToFind) {
    this.primeBase = primeBase;
//    this.substringsToFind = substringsToFind;
    this.n2primeToN = new TreeMap<Integer, Long>();

    if (substringsToFind == null || substringsToFind.length == 0) {
      throw new IllegalArgumentException("can't have null or empty substrings!");
    }

    init(substringsToFind);
  }

  private final void init(String[] substringsToFind) {
    this.length2hashes = new TreeMap<Integer, HashData[]>();

    for (int i = 0; i < substringsToFind.length; ++i) {
      final String substring = substringsToFind[i];
      final int len = substring.length();
      if (len == 0) continue;
      HashData[] hashes = length2hashes.get(len);
      HashData[] newHashes = (hashes == null) ? new HashData[1] : new HashData[hashes.length + 1];
      final int oldlen = newHashes.length - 1;
      for (int j = 0; j < oldlen; ++j) newHashes[j] = hashes[j];
      newHashes[oldlen] = new HashData(substring);
      length2hashes.put(len, newHashes);
    }

    final Set<Integer> lengths = length2hashes.keySet();
    this.lengths = new int[lengths.size()];
    int index = 0;
    for (Integer length : lengths) this.lengths[index++] = length;
  }


  /**
   * Write this message to the dataOutput stream such that this message
   * can be completely reconstructed through this.read(dataInput).
   *
   * @param dataOutput  the data output to write to.
   */
  public void write(DataOutput dataOutput) throws IOException {
    dataOutput.writeInt(primeBase);

    if (length2hashes == null) {
      dataOutput.writeInt(-1);
    }
    else {
      dataOutput.writeInt(length2hashes.size());

      for (Map.Entry<Integer, HashData[]> entry : length2hashes.entrySet()) {
        dataOutput.writeInt(entry.getKey());
        final HashData[] hashDatas = entry.getValue();

        if (hashDatas == null) {
          dataOutput.writeInt(-1);
        }
        else {
          dataOutput.writeInt(hashDatas.length);
        }

        for (HashData hashData : hashDatas) {
          DataHelper.writeString(dataOutput, hashData.substring);
          dataOutput.writeLong(hashData.hash);
        }
      }
    }

    if (lengths == null) {
      dataOutput.writeInt(-1);
    }
    else {
      dataOutput.writeInt(lengths.length);
      for (int length : lengths) {
        dataOutput.writeInt(length);
      }
    }

    if (n2primeToN == null) {
      dataOutput.writeInt(-1);
    }
    else {
      dataOutput.writeInt(n2primeToN.size());
      for (Map.Entry<Integer, Long> entry : n2primeToN.entrySet()) {
        dataOutput.writeInt(entry.getKey());
        dataOutput.writeLong(entry.getValue());
      }
    }
  }

  /**
   * Read this message's contents from the dataInput stream that was written by
   * this.write(dataOutput).
   * <p>
   * NOTE: this requires all implementing classes to have a default constructor
   *       with no args.
   *
   * @param dataInput  the data output to write to.
   */
  public void read(DataInput dataInput) throws IOException {
    this.primeBase = dataInput.readInt();

    final int numl2h = dataInput.readInt();
    if (numl2h >= 0) {
      this.length2hashes = new TreeMap<Integer, HashData[]>();
      for (int i = 0; i < numl2h; ++i) {
        HashData[] hashDatas = null;
        final int length = dataInput.readInt();
        final int numhs = dataInput.readInt();
        if (numhs >= 0) {
          hashDatas = new HashData[numhs];
          for (int j = 0; j < numhs; ++j) {
            hashDatas[j] = new HashData(DataHelper.readString(dataInput), dataInput.readLong());
          }
        }
        length2hashes.put(length, hashDatas);
      }
    }

    final int numLengths = dataInput.readInt();
    if (numLengths >= 0) {
      this.lengths = new int[numLengths];
      for (int i = 0; i < numLengths; ++i) {
        this.lengths[i] = dataInput.readInt();
      }
    }

    final int numn2p = dataInput.readInt();
    if (numn2p >= 0) {
      this.n2primeToN = new TreeMap<Integer, Long>();
      for (int i = 0; i < numn2p; ++i) {
        n2primeToN.put(dataInput.readInt(), dataInput.readLong());
      }
    }
  }


  /**
   * Compute a hash value for the given string. Note that the algorithm
   * works best when this can be a rolling hash.
   */
  public final long hash(String string) {
    return hash(string.toCharArray(), 0, string.length());
  }

  private final long hash(char[] chars, int startIndex, int endIndex) {
    long result = chars[startIndex];
    for (int i = startIndex + 1; i < endIndex; ++i) {
      result = (primeBase * result) + chars[i];
    }
    return result;
  }

  private final long hash(int c, long prevHash) {
    return primeBase * prevHash + c;
  }

  private final long hash(int oldFirstChar, int newLastChar, long prevHash, int len) {
    prevHash -= oldFirstChar * primeToN(len - 1);  // take off the first
    return hash(newLastChar, prevHash);  // shift and add on the last
  }

  private final long primeToN(int n) {
    long result = 0;
    synchronized (primeLock) {
      Long got = n2primeToN.get(n);
      if (got == null) {
        result = computePower(n);
        n2primeToN.put(n, result);
      }
      else {
        result = got;
      }
    }
    return result;
  }

  private final long computePower(int n) {
    long result = 1;
    for (int i = 0; i < n; ++i) {
      result *= primeBase;
    }
    return result;
  }

  /**
   * Search the string for any of the substrings.
   *
   * @param string         The normalized string to search.
   * @param acceptPartial  PatternFinder.ACCEPT_PARTIAL if a partial word match is ok;
   *                       PatternFinder.FULL_WORD if an entire word (delineated by breaks on either side) must match;
   *                       PatternFinder.BEGIN_WORD if a match must be found after a beginning break;
   *                       PatternFinder.END_WORD if a match must be found ending with an end break.
   *
   * @return an array with the index of the first substring to match (at index 0),
   *         and its length (at index 1) or null.
   */
  public int[] search(String string, int acceptPartial) {
    return search(new GeneralNormalizedString(string), acceptPartial);
  }

  /**
   * Search the string for any of the substrings.
   *
   * @param string         The normalized string to search.
   * @param acceptPartial  PatternFinder.ACCEPT_PARTIAL if a partial word match is ok;
   *                       PatternFinder.FULL_WORD if an entire word (delineated by breaks on either side) must match;
   *                       PatternFinder.BEGIN_WORD if a match must be found after a beginning break;
   *                       PatternFinder.END_WORD if a match must be found ending with an end break.
   *
   * @return an array with the index of the first substring to match (at index 0),
   *         and its length (at index 1) or null.
   */
  public int[] search(NormalizedString string, int acceptPartial) {
    return search(string, 0, string.getNormalizedLength(), acceptPartial);
  }

  /**
   * Search the string for any of the substrings.
   *
   * @param string         The normalized string to search.
   * @param acceptPartial  PatternFinder.ACCEPT_PARTIAL if a partial word match is ok;
   *                       PatternFinder.FULL_WORD if an entire word (delineated by breaks on either side) must match;
   *                       PatternFinder.BEGIN_WORD if a match must be found after a beginning break;
   *                       PatternFinder.END_WORD if a match must be found ending with an end break.
   * @param fromPos        The first index to start searching from in the string.
   * @param toPos          The index after the last index to search. Note that this
   *                       position will NOT be forced to be considered as a break.
   *
   * @return an array with the index of the first substring to match (at index 0),
   *         and its length (at index 1) or null.
   */
  public int[] search(String string, int fromPos, int toPos, int acceptPartial) {
    return search(new GeneralNormalizedString(string), fromPos, toPos, acceptPartial);
  }

  /**
   * Search the string for any of the substrings.
   *
   * @param string         The normalized string to search.
   * @param acceptPartial  PatternFinder.ACCEPT_PARTIAL if a partial word match is ok;
   *                       PatternFinder.FULL_WORD if an entire word (delineated by breaks on either side) must match;
   *                       PatternFinder.BEGIN_WORD if a match must be found after a beginning break;
   *                       PatternFinder.END_WORD if a match must be found ending with an end break.
   * @param fromPos        The first index to start searching from in the string.
   * @param toPos          The index after the last index to search. Note that this
   *                       position will NOT be forced to be considered as a break.
   *
   * @return an array with the index of the first substring to match (at index 0),
   *         and its length (at index 1) or null.
   */
  public int[] search(NormalizedString string, int fromPos, int toPos, int acceptPartial) {

    long[][] hashes = new long[2][lengths.length];  // just allocate these once.

    int curHash = 0;

    for (int i = fromPos; i <= toPos - lengths[0]; ++i) {
      final int len = updateHashes(string, i, hashes, curHash, acceptPartial, fromPos, toPos);
      if (len > 0) return new int[]{i, len};
      curHash = (curHash + 1) % 2;
    }

    return null;
  }

  private final int updateHashes(NormalizedString string, int index, long[][] hashes, int curHash, int acceptPartial,
                                 int fromPos, int toPos) {
    final long[] result = hashes[curHash];
    final long[] prevHashes = hashes[(curHash + 1) % 2];
    final char[] chars = string.getNormalizedChars();

    if (index == fromPos) {
      for (int i = lengths.length - 1; i >= 0; --i) {
        final int len = lengths[i];
        final int endIndex = index + len;
        if (endIndex > toPos) continue;
        final HashData[] hashesAtLength = length2hashes.get(len);
        result[i] = hash(chars, index, endIndex);

        if (matches(result[i], hashesAtLength, string, index, endIndex, acceptPartial)) {
          return len;  // FOUND!
        }
      }
    }
    else {
      for (int i = lengths.length - 1; i >= 0; --i) {
        final int len = lengths[i];
        final int endIndex = index + len;
        if (endIndex > toPos) continue;
        final HashData[] hashesAtLength = length2hashes.get(len);
        final char oldC = chars[index - 1];
        final char newC = chars[endIndex - 1];
        result[i] = hash(oldC, newC, prevHashes[i], len);

        if (matches(result[i], hashesAtLength, string, index, endIndex, acceptPartial)) {
          return len;  // FOUND!
        }
      }
    }

    return -1;
  }

  private final boolean matches(long hash, HashData[] hashes, NormalizedString string, int startIndex, int endIndex, int acceptPartial) {
    boolean result = false;

    final char[] chars = string.getNormalizedChars();
    for (HashData hashData : hashes) {
      if (hashData.matches(hash, chars, startIndex, endIndex)) {
        if (((acceptPartial & PatternFinder.BEGIN_WORD) == 0 || string.isStartBreak(startIndex)) &&
            ((acceptPartial & PatternFinder.END_WORD) == 0 || string.isEndBreak(endIndex))) {
          result = true;
          break;
        }
      }
    }

    return result;
  }

  private final class HashData {
    String substring;
    long hash;

    private char[] chars;

    public HashData(String substring) {
      this.substring = substring;
      this.hash = hash(substring);
      this.chars = null;
    }

    HashData(String substring, long hash) {
      this.substring = substring;
      this.hash = hash;
      this.chars = null;
    }

    public boolean matches(long value, char[] stringChars, int startIndex, int endIndex) {
      if (hash != value) return false;

      if (chars == null) chars = substring.toCharArray();

      int index = 0;
      for (int i = startIndex; i < endIndex; ++i) {
        if (chars[index++] != stringChars[i]) return false;
      }

      return true;
    }
  }
}
