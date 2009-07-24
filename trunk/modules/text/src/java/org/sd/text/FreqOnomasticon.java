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


import org.sd.bdb.BaseFileLoader;
import org.sd.bdb.DbMap;
import org.sd.bdb.DbValue;
import org.sd.io.DataHelper;
import org.sd.io.Publishable;
import org.sd.io.FileUtil;
import org.sd.util.LineBuilder;
import org.sd.util.StringSplitter;
import org.sd.util.StringUtil;

import java.io.BufferedReader;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.File;
import java.io.IOException;

/**
 * An onomasticon (names database) with frequencies attached to the names.
 * <p>
 * This implementation will store the names in a backing berkeley db database
 * through a DbMap.
 *
 * @author Spence Koehler
 */
public class FreqOnomasticon {

  private static final String TOTAL_COUNT_KEY = "TC";
  private static final String NUM_NAMES_KEY = "NN";
  private static final String MAX_FREQ_KEY = "MF";


  private File namesTextFile;
  private DbMap _onomasticon;
  private Integer _totalCount;
  private Integer _numNames;
  private Integer _maxFrequency;

  /**
   * Construct with the location of the names text file.
   * <p>
   * The file is assumed to have lines of the form: "name|freq" and the order
   * of the lines in the file are assumed to correspond to 'rank', starting
   * from 1. Empty lines and those beginning with '#' are ignored.
   */
  public FreqOnomasticon(File namesTextFile) {
    this.namesTextFile = namesTextFile;
    this._onomasticon = null; // lazily load.
  }

  /**
   * Lookup the given (unnormalized) name.
   *
   * @return the FreqName instance for the name or null if the name is not
   *         in this onomasticon.
   */
  public FreqName lookup(String name) {
    FreqName result = null;

    if (name != null && !"".equals(name)) {
      final String nname = normalize(name);
      if (nname != null && !"".equals(nname)) {
        result = doLookup(nname);

        if (result != null) {
          result.setCounts(getTotalCount(), getNumNames(), getMaxFrequency());
        }
      }
    }

    return result;
  }

  private final FreqName doLookup(String nname) {
    FreqName result = null;

    final DbMap onomasticon = getOnomasticon();
    final DbValue dbValue = onomasticon.get(nname);
    if (dbValue != null) {
      result = (FreqName)dbValue.getPublishable();
    }

    return result;
  }

  private static final int retrieveFrequency(DbMap onomasticon, String nname) {
    int result = -1;

    final DbValue dbValue = onomasticon.get(nname);
    if (dbValue != null) {
      final FreqName freqName = (FreqName)dbValue.getPublishable();
      result = freqName.getFrequency();
    }

    return result;
  }

  /**
   * Normalization entails lowercasing and replacing diacritics.
   */
  private static final String normalize(String name) {
    return StringSplitter.replaceDiacritics(name.toLowerCase());
  }

  public final DbMap getOnomasticon() {
    if (_onomasticon == null) {
      final NamesLoader namesLoader = new NamesLoader(namesTextFile);
      _onomasticon = DbMap.createInstance(namesTextFile, namesLoader);
      if (namesLoader.didLoad()) {
        // if we actually did the loading (rather than are falling back
        // onto a pre-loaded db), then pull out the counts and pack them
        // into the map under the special keys.
        _onomasticon.put(TOTAL_COUNT_KEY, new DbValue(new FreqName(TOTAL_COUNT_KEY, namesLoader.getTotalCount())));
        _onomasticon.put(NUM_NAMES_KEY, new DbValue(new FreqName(NUM_NAMES_KEY, namesLoader.getNumNames())));
        _onomasticon.put(MAX_FREQ_KEY, new DbValue(new FreqName(MAX_FREQ_KEY, namesLoader.getMaxFrequency())));
      }
    }
    return _onomasticon;
  }

  /**
   * Get the total count of names in this onomasticon.
   */
  public int getTotalCount() {
    if (_totalCount == null) {
      final DbMap onomasticon = getOnomasticon();
      final FreqName freqName = doLookup(TOTAL_COUNT_KEY);
      if (freqName != null) {
        _totalCount = freqName.getFrequency();
      }
      else _totalCount = 0;
    }
    return _totalCount;
  }

  /**
   * Get the total number of names in this onomasticon.
   */
  public int getNumNames() {
    if (_numNames == null) {
      final DbMap onomasticon = getOnomasticon();
      final FreqName freqName = doLookup(NUM_NAMES_KEY);
      if (freqName != null) {
        _numNames = freqName.getFrequency();
      }
      else _numNames = 0;
    }
    return _numNames;
  }

  /**
   * Get the maximum frequency of any name in this onomasticon.
   */
  public int getMaxFrequency() {
    if (_maxFrequency == null) {
      final DbMap onomasticon = getOnomasticon();
      final FreqName freqName = doLookup(MAX_FREQ_KEY);
      if (freqName != null) {
        _maxFrequency = freqName.getFrequency();
      }
      else _maxFrequency = 0;
    }
    return _maxFrequency;
  }


  private static final class NamesLoader extends BaseFileLoader {
    private int totalCount;
    private int numNames;
    private int maxFrequency;
    private boolean loaded;

    NamesLoader(File namesTextFile) {
      super(100000, namesTextFile);

      this.totalCount = 0;
      this.numNames = 0;
      this.maxFrequency = 0;
    }

    protected boolean lineHasBeenLoaded(DbMap dbMap, String line, int lineNum) {
      this.totalCount = retrieveFrequency(dbMap, TOTAL_COUNT_KEY);
      this.numNames = retrieveFrequency(dbMap, NUM_NAMES_KEY);
      this.maxFrequency = retrieveFrequency(dbMap, MAX_FREQ_KEY);

      final FreqName freqName = new FreqName(-1, line);
      return dbMap.get(freqName.getNormalizedName()) != null;
    }

    protected boolean doLoadLine(DbMap dbMap, String line, int lineNum) {
      FreqName freqName = new FreqName(numNames + 1, line);

      if (freqName.isValid()) {
        int curFrequency = freqName.getFrequency();
        totalCount += curFrequency;

        // check for collision due to normalization
        final DbValue curEntry = dbMap.get(freqName.getNormalizedName());
        if (curEntry != null) {
          // collides! combine entries, keeping prior rank
          final FreqName priorFreqName = (FreqName)curEntry.getPublishable();
          priorFreqName.incFrequency(freqName.getFrequency());
          curFrequency += priorFreqName.getFrequency();
          freqName = priorFreqName;
        }
        else {
          // no collision -- we have another name
          ++numNames;
        }

        if (curFrequency > maxFrequency) maxFrequency = curFrequency;
        dbMap.put(freqName.getNormalizedName(), new DbValue(freqName));
      }

      return true;
    }

    public int getTotalCount() {
      return totalCount;
    }

    public int getNumNames() {
      return numNames;
    }

    public int getMaxFrequency() {
      return maxFrequency;
    }

    public boolean didLoad() {
      return getNumLoadedLines() > 0;
    }
  }

  /**
   * Data structure to hold a name with its frequency info.
   */
  public static final class FreqName implements Publishable {
    private String name;  // normalized name
    private int freq;     // frequency count
    private int rank;     // non-empty, non-comment line number

    private transient int totalCount;
    private transient int numNames;
    private transient int maxFrequency;

    /**
     * Default constructor for publishable reconstruction.
     */
    public FreqName() {
    }

    /**
     * Construct from a line formatted as "name|freq".
     */
    public FreqName(int lineNum, String line) {
      this.rank = lineNum;

      final String[] pieces = line.split("\\s*\\|\\s*");
      this.name = normalize(pieces[0]);
      if (pieces.length > 1) {
        freq = Integer.parseInt(pieces[1]);
      }
      // else, default unspecified frequency is 0.
    }

    /**
     * Construct with the given values.
     */
    FreqName(String name, int freq) {
      this.name = name;
      this.freq = freq;
      this.rank = 0;
    }

    /**
     * Determine whether the name is valid.
     * <p>
     * Currently, the following names are considered to be invalid:
     * <ul>
     * <li>Names of length 1</li>
     * <li>Names with digits</li>
     * <li>Names with the same letter repeated 3 times consecutively</li>
     * </ul>
     */
    public boolean isValid() {

      if (name.length() < 2) return false;

      if (StringUtil.hasDigit(name)) return false;

      if (StringUtil.hasConsecutiveRepeats(name, 3)) return false;

      return true;
    }

    /**
     * Get the normalized name.
     */
    public String getNormalizedName() {
      return name;
    }

    /**
     * Get the name's frequency count.
     * <p>
     * NOTE: Zero is returned for names for which a frequency is not specified.
     */
    public int getFrequency() {
      return freq;
    }

    /**
     * Get the rank (1-based line number).
     */
    public int getRank() {
      return rank;
    }

    /**
     * Increment the frequency by the additional amount.
     */
    void incFrequency(int additionalFreq) {
      freq += additionalFreq;
    }

    /**
     * Set the transient totalCount and numNames values.
     */
    void setCounts(int totalCount, int numNames, int maxFrequency) {
      this.totalCount = totalCount;
      this.numNames = numNames;
      this.maxFrequency = maxFrequency;
    }

    /**
     * Get the totalCount from this entry's onomasticon.
     */
    public int getTotalCount() {
      return totalCount;
    }

    /**
     * Get the number of names in this entry's onomasticon.
     */
    public int getNumNames() {
      return numNames;
    }

    /**
     * Get the maximum frequency in this entry's onomasticon.
     */
    public int getMaxFrequency() {
      return maxFrequency;
    }

    /**
     * Get a string representation of this instance.
     */
    public String toString() {
      final LineBuilder result = new LineBuilder();

      result.
        append(name).
        append("freq=" + freq + "/" + maxFrequency + "/" + totalCount).
        append("rank=" + rank + "/" + numNames);

      return result.toString();
    }

    /**
     * Write thie message to the dataOutput stream such that this message
     * can be completely reconstructed through this.read(dataInput).
     *
     * @param dataOutput  the data output to write to.
     */
    public void write(DataOutput dataOutput) throws IOException {
      DataHelper.writeString(dataOutput, name);
      dataOutput.writeInt(freq);
      dataOutput.writeInt(rank);
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
      this.name = DataHelper.readString(dataInput);
      this.freq = dataInput.readInt();
      this.rank = dataInput.readInt();
    }
  }


  /**
   * Load the freq onomasticon backed by the file (args[0]) and lookup the words
   * (args[1+]).
   */
  public static final void main(String[] args) {
    //java -Xmx640m org.sd.nlp.FreqOnomasticon /usr/local/share/data/sd/resources/nouns/onomasticon/names.freq.s.txt.gz bob joe rumplestiltskin mary kay
    //  took ~30 minutes to load.

    final FreqOnomasticon onomasticon = new FreqOnomasticon(new File(args[0]));

    for (int i = 1; i < args.length; ++i) {
      final FreqName freqName = onomasticon.lookup(args[i]);

      String outline = null;
      if (freqName == null) {
        outline = args[i];
      }
      else {
        outline = args[i] + "|" + freqName.toString();
      }

      System.out.println(outline);
    }
  }
}
