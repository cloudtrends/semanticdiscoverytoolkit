/*
    Copyright 2011 Semantic Discovery, Inc. (www.semanticdiscovery.com)

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
package org.sd.xml;


import java.io.File;
import java.io.IOException;
import java.util.List;
import org.sd.util.Histogram;
import org.sd.util.HistogramDistribution;
import org.sd.util.MathUtil;
import org.sd.util.SampleCollector;
import org.sd.util.range.IntegerRange;
import org.w3c.dom.NodeList;

/**
 * A histogram with the frequent tail keys collapsed by their count.
 * <p>
 * This data structure is useful to represent histograms that grow very
 * large due to relatively unique (bin) keys (long tails).
 * <p>
 * The top-frequency bins are kept as in a normal histogram, but the
 * low-frequency bins are collapsed by count with sample bin keys kept
 * instead of storing the entire set of bin keys for the histogram.
 * <p>
 * This can contain (or copy) a full histogram without collapsing it
 * if the base histogram is considered small, which is indicated by
 * supplying a minSwitchRank that is greater than or equal to the
 * number of ranks in the histogram (e.g. Integer.MAX_VALUE).
 *
 * @author Spence Koehler
 */
public class CollapsedHistogram <T> extends Histogram<CollapsedKeyContainer<T>> {
  
  public static final String DEFAULT_COLLAPSED_XML_ROOT_TAG = "chisto";

  /**
   * Construct with a fully populated histogram using automatic rank switching
   * and collecting a single sample for collapsed buckets.
   */
  public static final <T> CollapsedHistogram<T> makeInstance(Histogram<T> original) {
    return makeInstance(original, 0, 1);
  }

  /**
   * Construct with a fully populated histogram and the number of key samples
   * to collect for collapsed keys.
   *
   * @param original  The original histogram to load.
   * @param minSwitchRank  The minimum rank at which to switch to collapsed buckets.
   *                       A value of 0 will ensure automatic switching at the appropriate
   *                       rank while a value of original.getNumRanks() will ensure no
   *                       collapsing.
   * @param maxSamples  The maximum number of sample keys to collect for collapsed buckets.
   */
  public static final <T> CollapsedHistogram<T> makeInstance(Histogram<T> original, int minSwitchRank, int maxSamples) {
    final HistogramDistribution distribution = original.getDistribution();
    final int switchRank = Math.max(minSwitchRank, distribution.getSwitchRank());
    final CollapsedHistogram<T> result = new CollapsedHistogram<T>(switchRank, maxSamples);

    int curRank = 0;
    for (Histogram<T>.Frequency<T> freq : original.getFrequencies()) {
      result.add(curRank++, freq.getElement(), freq.getFrequency());
    }

    return result;
  }

  /**
   * Construct with a persisted XmlHistogram using automatic rank switching
   * and collecting a single sample for collapsed buckets.
   */
  public static final CollapsedHistogram<String> makeInstance(File xmlHistogramFile) throws IOException {
    return makeInstance(xmlHistogramFile, 0, 1);
  }

  /**
   * Construct with a persisted XmlHistogram and the number of key samples
   * to collect for collapsed keys.
   *
   * @param xmlHistogramFile  The xml histogram to load.
   * @param minSwitchRank  The minimum rank at which to switch to collapsed buckets.
   *                       A value of 0 will ensure automatic switching at the appropriate
   *                       rank while a value of original.getNumRanks() will ensure no
   *                       collapsing.
   * @param maxSamples  The maximum number of sample keys to collect for collapsed buckets.
   */
  public static final CollapsedHistogram<String> makeInstance(File xmlHistogramFile, int minSwitchRank, int maxSamples) throws IOException {
    final HistogramDistribution distribution = XmlHistogram.loadDistribution(xmlHistogramFile);
    final int switchRank = Math.max(minSwitchRank, distribution.getSwitchRank());
    final CollapsedHistogram<String> result = new CollapsedHistogram<String>(switchRank, maxSamples);

    //
    // To guard against overwhelming memory with very large histograms,
    // this method incrementally considers each key without ever loading
    // everything into memory.
    //

    //
    // Because the file may not be in rank order, two passes are necessary:
    // (1) compute distribution histogram
    // (2) load keys and counts
    //

    XmlHistogramIterator iter = null;
    try {
      for (iter = new XmlHistogramIterator(xmlHistogramFile); iter.hasNext(); ) {
        final XmlKeyContainer xmlKeyContainer = iter.next();
        final int curCount = xmlKeyContainer.getCount();
        final IntegerRange rankRange = distribution.getOriginalRank(curCount);
        final int curRank = rankRange.getLow();

        result.add(curRank, xmlKeyContainer.getKey(), curCount);
      }
    }
    finally {
      if (iter != null) iter.close();
    }

    return result;
  }

  /**
   * Construct an instance from collapsed xml format.
   */
  public static CollapsedHistogram<String> loadFromXml(String xml) {
    return loadFromXml(new XmlStringBuilder().setXmlString(xml).getXmlElement());
  }

  /**
   * Construct an instance from collapsed xml format.
   */
  public static CollapsedHistogram<String> loadFromXml(File xmlFile) throws IOException {
    final DomElement xml = XmlFactory.loadDocument(xmlFile, false).getDocumentDomElement();
    return loadFromXml(xml);
  }

  /**
   * Load from CollapsedHistogram xml.
   */
  public static final CollapsedHistogram<String> loadFromXml(DomElement collapsedXml) {
    final int switchRank = collapsedXml.getAttributeInt("switchRank", 0);
    final int maxSamples = collapsedXml.getAttributeInt("maxSamples", 0);

    final CollapsedHistogram<String> result = new CollapsedHistogram<String>(switchRank, maxSamples);

    final NodeList childNodes = collapsedXml.getChildNodes();
    final int numNodes = childNodes.getLength();
    for (int childIdx = 0; childIdx < numNodes; ++childIdx) {
      final DomNode childNode = (DomNode)childNodes.item(childIdx);
      if (childNode.getNodeType() == DomNode.ELEMENT_NODE && "cc".equals(childNode.getLocalName())) {
        final CollapsedKeyContainer<String> ckc = CollapsedKeyContainer.loadFromXml((DomElement)childNode, maxSamples);
        result.set(ckc, ckc.getOrigCount());
      }
    }

    return result;
  }



  private int maxSamples;
  private int switchRank;

  private Integer _originalTotalCount;
  private Integer _originalNumRanks;
  private Integer _originalMaxFrequencyCount;

  /**
   * Construct an empty instance with no switching, no samples.
   */
  public CollapsedHistogram() {
    this(Integer.MAX_VALUE, 0);
  }

  /**
   * Construct with the given parameters.
   */
  public CollapsedHistogram(int switchRank, int maxSamples) {
    super();

    this.maxSamples = maxSamples;
    this.switchRank = switchRank;

    this._originalTotalCount = null;
    this._originalNumRanks = null;
    this._originalMaxFrequencyCount = null;
  }

  public int getMaxSamples() {
    return maxSamples;
  }

  public void setMaxSamples(int maxSamples) {
    this.maxSamples = maxSamples;
  }

  public int getSwitchRank() {
    return switchRank;
  }

  public void setSwitchRank(int switchRank) {
    this.switchRank = switchRank;
  }

  /**
   * Add an element.
   */
  public Frequency<CollapsedKeyContainer<T>> add(CollapsedKeyContainer<T> element) {
    return add(element, 1);
  }

  public Frequency<CollapsedKeyContainer<T>> add(CollapsedKeyContainer<T> element, int freqCount) {
    this._originalTotalCount = null;
    this._originalNumRanks = null;
    this._originalMaxFrequencyCount = null;
    return super.add(element, freqCount);
  }

  public void add(Histogram<CollapsedKeyContainer<T>> other) {
    this._originalTotalCount = null;
    this._originalNumRanks = null;
    this._originalMaxFrequencyCount = null;
    super.add(other);
  }

  /**
   * Get the total count for the original histogram.
   */
  public int getOriginalTotalCount() {
    if (_originalTotalCount == null) {
      _originalTotalCount = computeOriginalTotalCount();
    }
    return _originalTotalCount;
  }

  private final int computeOriginalTotalCount() {
    int result = 0;

    for (Frequency<CollapsedKeyContainer<T>> freq : getFrequencies()) {
      final CollapsedKeyContainer<T> keyContainer = freq.getElement();
      result += keyContainer.getTotalCount();
    }

    return result;
  }

  /**
   * Get the number of ranks in the original histogram.
   */
  public int getOriginalNumRanks() {
    if (_originalNumRanks == null) {
      _originalNumRanks = computeOriginalNumRanks();
    }
    return _originalNumRanks;
  }

  private final int computeOriginalNumRanks() {
    int result = 0;

    for (Frequency<CollapsedKeyContainer<T>> freq : getFrequencies()) {
      final CollapsedKeyContainer<T> keyContainer = freq.getElement();
      result += keyContainer.getBinCount();
    }

    return result;
  }

  public int getOriginalMaxFrequencyCount() {
    if (_originalMaxFrequencyCount == null) {
      _originalMaxFrequencyCount = getOriginalFrequencyCount(0);
    }
    return _originalMaxFrequencyCount;
  }

  public int getOriginalFrequencyCount(int collapsedRank) {
    int result = 0;

    final Frequency<CollapsedKeyContainer<T>> freq = getRankFrequency(collapsedRank);
    if (freq != null) {
      final CollapsedKeyContainer<T> keyContainer = freq.getElement();
      result = keyContainer.getTotalCount();
    }

    return result;
  }

  public String toString() {
    final StringBuilder result = new StringBuilder();

    final int totalCount = getOriginalTotalCount();
    int cumulativeCount = 0;

    final int numRanks = getOriginalNumRanks();
    final int maxRankDigits = (int)Math.round(MathUtil.log10(numRanks) + 0.5);

    final int maxFreq = getOriginalMaxFrequencyCount();
    final int maxFreqDigits = (int)Math.round(MathUtil.log10(maxFreq) + 0.5);

    // rank  freq  cumulativePct  pct  label
    // %<maxRankDigits>d  %<maxFreqDigits>d  %6.2f  %6.2f  %40s
    final StringBuilder formatString = new StringBuilder();
    formatString.
      append("%").
      append(Math.max(1, maxRankDigits)).
      append("d  %").
      append(Math.max(1, maxFreqDigits)).
      append("d  %6.2f  %6.2f  %-40s");

    result.
      append("h(").
      append(totalCount).append('/').append(numRanks).
      append(':').
      append(getTotalCount()).append('/').append(getNumRanks()).
      append(")");

    for (int i = 0; i < getNumRanks(); ++i) {

      final Frequency<CollapsedKeyContainer<T>> freq = getRankFrequency(i);
      final CollapsedKeyContainer<T> keyContainer = freq.getElement();
      final int curRankFrequency = keyContainer.getTotalCount();
      final String keyString = keyContainer.buildKeyString();
      cumulativeCount += curRankFrequency;
      final double cumPct = 100.0 * ((double)cumulativeCount / (double)totalCount);
      final double pct = 100.0 * ((double)curRankFrequency / (double)totalCount);
      result.
        append("\n  ").
        append(String.format(formatString.toString(),
                             i,                    // rank
                             curRankFrequency,     // freq
                             cumPct,               // cumPct
                             pct,                  // pct
                             keyString));
    }

    return result.toString();
  }

  /**
   * Add an appropriate element to this instance with the given params.
   */
  public void add(int curRank, T bucketKey, int curCount) {
    if (curRank < switchRank) {
      // keep the original key
      final CollapsedKeyContainer<T> keyContainer = new CollapsedKeyContainer<T>(bucketKey, curCount, 0);
      super.add(keyContainer, curCount);
    }
    else {
      // store (or update) a countKey
      final CollapsedKeyContainer<T> keyContainer = new CollapsedKeyContainer<T>(null, curCount, maxSamples);
      final Frequency<CollapsedKeyContainer<T>> curfreq = getElementFrequency(keyContainer);
      if (curfreq != null) {
        curfreq.getElement().incBinCount();
      }
      else {
        super.add(keyContainer, curCount);
      }
      keyContainer.considerSample(bucketKey);
    }
  }

  public XmlStringBuilder asXml() {
    return asXml(DEFAULT_COLLAPSED_XML_ROOT_TAG);
  }

  public XmlStringBuilder asXml(String rootTag) {
    final StringBuilder tag = new StringBuilder();
    tag.
      append(rootTag).
      append(" switchRank='").append(switchRank).
      append("' maxSamples='").append(maxSamples).
      append("' total='").append(getTotalCount()).
      append("' bins='").append(getNumRanks()).
      append("' origTotal='").append(getOriginalTotalCount()).
      append("' origRanks='").append(getOriginalNumRanks());

    final XmlStringBuilder result = new XmlStringBuilder(tag.toString());

    for (Frequency<CollapsedKeyContainer<T>> freq : getFrequencies()) {
      freq.getElement().asXml(result);
    }

    return result;
  }


  public static void main(String[] args) throws IOException {
    // arg0: file w/xml histogram
    final File xmlFile = new File(args[0]);
    final CollapsedHistogram h = CollapsedHistogram.makeInstance(xmlFile, 0, 1);

    System.out.println(h.toString());
  }
}
