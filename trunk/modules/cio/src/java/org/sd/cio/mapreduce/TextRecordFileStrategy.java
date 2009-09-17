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
package org.sd.cio.mapreduce;


import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;

import org.sd.io.FileUtil;
import org.sd.util.KVPair;

/**
 * A record file strategy for text records, one delimited record per line.
 * <p>
 * Note that this generates human readable and easily manipulated textual
 * output at the cost of having to escape the keys.
 *
 * @author Spence Koehler
 */
public abstract class TextRecordFileStrategy<K, V> implements RecordFileStrategy<K, V> {
  
  /**
   * Get an escaped form of the key for safe usage of the delimiter.
   */
  protected abstract String escapeKey(K key);

  /**
   * Unescape and convert the key string back into a key.
   */
  protected abstract K unescapeKey(String keyString);

  /**
   * Convert the value string back into a value instance.
   */
  protected abstract V stringToValue(String valueString);

  /**
   * Convert the value instance to a string.
   */
  protected abstract String valueToString(V value);


  private String delim;
  private BufferedWriter writer;

  protected TextRecordFileStrategy(String delim) {
    this.delim = delim;
    this.writer = null;
  }

  public boolean open(File recordFile) throws IOException {
    return bounce(recordFile);
  }

  public boolean bounce(File recordFile) throws IOException {
    close();
    if (recordFile != null && !recordFile.exists()) {
      final File parentDir = recordFile.getParentFile();
      if (!parentDir.exists()) parentDir.mkdirs();
      this.writer = FileUtil.getWriter(recordFile);
    }
    return this.writer != null;
  }

  public boolean writeRecord(K key, V value) throws IOException {
    if (writer == null) return false;

    final StringBuilder line =
      new StringBuilder().
      append(escapeKey(key)).
      append(delim).
      append(valueToString(value));

    // write the line
    writer.write(line.toString());
    writer.newLine();

    return true;
  }

  /**
   * Decode the parts of a line written by this strategy.
   */
  public KVPair<K, V> decodeLine(String line) {
    KVPair<K, V> result = null;

    final int delimPos = line.indexOf(delim);
    if (delimPos >= 0) {
      final K key = unescapeKey(line.substring(0, delimPos));
      final V value = stringToValue(line.substring(delimPos + delim.length()));
      result = new KVPair<K, V>(key, value);
    }

    return result;
  }

  public void close() throws IOException {
    if (this.writer != null) {
      this.writer.close();
      this.writer = null;
    }
  }
}
