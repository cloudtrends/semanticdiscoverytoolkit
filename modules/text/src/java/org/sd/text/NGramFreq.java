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
import org.sd.io.Publishable;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Container for an NGram and associated information.
 * <p>
 * @author Spence Koehler
 */
public class NGramFreq implements Publishable, Comparable<NGramFreq> {
  

  private static final Pattern NGRAMFREQ_PATTERN = Pattern.compile("^(.*) \\((.*)\\)$");


  private String ngram;
  private Set<String> sources;
  private long freq;
  private Map<String, NGramFreq> container;  // back-reference for pruning
  private int n;

  /**
   * Default constructor for publishable reconstruction only.
   */
  public NGramFreq() {
  }

  public NGramFreq(String ngram, long freq, int n) {
    this.ngram = ngram;
    this.sources = null;
    this.freq = freq;
    this.container = null;
    this.n = n;
  }

  public NGramFreq(String ngram, String source, Map<String, NGramFreq> container, int n) {
    this.ngram = ngram;
    this.sources = null;
    addSource(source);
    this.freq = 1L;
    this.container = container;
    this.n = n;
  }

  public NGramFreq(String toStringForm) {
    final Matcher m = NGRAMFREQ_PATTERN.matcher(toStringForm);
    if (m.matches()) {
      this.ngram = m.group(1);
      this.freq = Long.parseLong(m.group(2));
    }
  }

  public final void addSource(String source) {
    if (source != null) {
      if (sources == null) sources = new HashSet<String>();
      sources.add(source);
    }
  }

  public final void addAllSources(NGramFreq other) {
    if (other.sources != null) {
      for (String source : other.sources) {
        addSource(source);
      }
    }
  }

  public String getNGram() {
    return ngram;
  }

  public long getFreq() {
    return freq;
  }

  public void inc(String source) {
    ++freq;
    addSource(source);
  }

  public void inc(long freq, Set<String> sources) {
    this.freq += freq;
    if (sources != null) {
      if (this.sources == null) this.sources = new HashSet<String>();
      this.sources.addAll(sources);
    }
  }

  /**
   * Get sources for this NGram (if available).
   */
  public Set<String> getSources() {
    return sources;
  }

  /**
   * Remove this instance from its container.
   */
  public void removeFromContainer() {
    if (container != null) {
      container.remove(ngram);
    }
  }

  /**
   * Get the value of N for this N-Gram.
   */
  public int getN() {
    return n;
  }

  /**
   * Sort from higher to lower frequency.
   */
  public int compareTo(NGramFreq other) {
    return this.freq > other.freq ? -1 : this.freq == other.freq ? 0 : 1;
  }

  public String toString() {
    final StringBuilder result = new StringBuilder();
    result.append(ngram).append(" (").append(freq).append(')');
    return result.toString();
  }

  /**
   * Write this message to the dataOutput stream such that this message
   * can be completely reconstructed through this.read(dataInput).
   *
   * @param dataOutput  the data output to write to.
   */
  public void write(DataOutput dataOutput) throws IOException {
    DataHelper.writeString(dataOutput, ngram);

    if (sources == null) {
      dataOutput.writeInt(-1);
    }
    else {
      dataOutput.writeInt(sources.size());
      for (String source : sources) {
        DataHelper.writeString(dataOutput, source);
      }
    }

    dataOutput.writeLong(freq);
    dataOutput.writeInt(n);
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
    this.ngram = DataHelper.readString(dataInput);

    final int numSources = dataInput.readInt();
    if (numSources >= 0) {
      this.sources = new HashSet<String>();
      for (int i = 0; i < numSources; ++i) {
        this.sources.add(DataHelper.readString(dataInput));
      }
    }

    this.freq = dataInput.readLong();
    this.n = dataInput.readInt();
  }
}
