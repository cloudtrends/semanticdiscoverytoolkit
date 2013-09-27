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
import java.util.Comparator;
import java.util.List;
import org.sd.util.Histogram;
import org.sd.util.HistogramDistribution;
import org.sd.util.MathUtil;
import org.sd.util.SampleCollector;
import org.sd.util.range.LongRange;
import org.w3c.dom.NodeList;

/**
 * A histogram with the frequent (usually tail) keys collapsed by their count.
 * <p>
 * This data structure is useful to represent histograms that grow very
 * large due to relatively unique (bin) keys (long tails).
 * <p>
 * The top-frequency bins are kept as in a normal histogram, but the
 * low-frequency bins are collapsed by count with sample bin keys kept
 * instead of storing the entire set of bin keys for the histogram.
 *
 * @author Spence Koehler
 */
public class CollapsedHistogram <T> extends Histogram<CollapsedKeyContainer<T>> {
  
  public static final String DEFAULT_COLLAPSED_XML_ROOT_TAG = "chisto";

  public static final KeyComparator<String> SORTED_KEY_COMPARATOR = new KeyComparator<String>();
  private static final class KeyComparator <T> implements Comparator<CollapsedKeyContainer<T>> {
    public int compare(CollapsedKeyContainer<T> o1, CollapsedKeyContainer<T> o2) {
      int result = 0;

      final T o1Key = o1.getAnyKey();
      final T o2Key = o2.getAnyKey();

      if (o1Key != null && o2Key != null) {
        result = o1Key.toString().compareTo(o2Key.toString());
      }
      else if (o2Key == null) {
        // prefer o1
        result = -1;
      }
      else {  // o1Key == null
        // prefer o2
        result = 1;
      }

      return result;
    }
    public boolean equals(Object obj) { return (obj instanceof KeyComparator); }
  }


  /**
   * Construct with a fully populated histogram using automatic rank switching
   * and collecting a single sample for collapsed buckets.
   */
  public static final <T> CollapsedHistogram<T> makeInstance(Histogram<T> original) {
    return makeInstance(original, 1);
  }

  /**
   * Construct with a fully populated histogram and the number of key samples
   * to collect for collapsed keys.
   *
   * @param original  The original histogram to load.
   * @param maxSamples  The maximum number of sample keys to collect for collapsed buckets.
   */
  public static final <T> CollapsedHistogram<T> makeInstance(Histogram<T> original, int maxSamples) {
    final HistogramDistribution distribution = original.getDistribution();
    final CollapsedHistogram<T> result = new CollapsedHistogram<T>(maxSamples);

    for (Histogram<T>.Frequency<T> freq : original.getFrequencies()) {
      final long curCount = freq.getFrequency();
      final long numBuckets = distribution.getNumBins(curCount);
      result.add(numBuckets, freq.getElement(), curCount);
    }

    return result;
  }

  /**
   * Construct with a persisted XmlHistogram using automatic rank switching
   * and collecting a single sample for collapsed buckets.
   */
  public static final CollapsedHistogram<String> makeInstance(File xmlHistogramFile) throws IOException {
    return makeInstance(xmlHistogramFile, 1);
  }

  /**
   * Construct with a persisted XmlHistogram and the number of key samples
   * to collect for collapsed keys.
   *
   * @param xmlHistogramFile  The xml histogram to load.
   * @param maxSamples  The maximum number of sample keys to collect for collapsed buckets.
   */
  public static final CollapsedHistogram<String> makeInstance(File xmlHistogramFile, int maxSamples) throws IOException {
    final HistogramDistribution distribution = XmlHistogram.loadDistribution(xmlHistogramFile);
    final CollapsedHistogram<String> result = new CollapsedHistogram<String>(maxSamples);

    //
    // To guard against overwhelming memory with very large histograms,
    // this method incrementally considers each key without ever loading
    // everything (except the count -> numBucketsWithCount distribution)
    // into memory.
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
        final long curCount = xmlKeyContainer.getCount();
        final long numBuckets = distribution.getNumBins(curCount);
        result.add(numBuckets, xmlKeyContainer.getKey(), curCount);
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
    final int maxSamples = collapsedXml.getAttributeInt("maxSamples", 1);

    final CollapsedHistogram<String> result = new CollapsedHistogram<String>(maxSamples);

    final NodeList childNodes = collapsedXml.getChildNodes();
    final int numNodes = childNodes.getLength();
    for (int childIdx = 0; childIdx < numNodes; ++childIdx) {
      final DomNode childNode = (DomNode)childNodes.item(childIdx);
      if (childNode.getNodeType() == DomNode.ELEMENT_NODE && "cc".equals(childNode.getLocalName())) {
        final CollapsedKeyContainer<String> ckc = CollapsedKeyContainer.loadFromXml((DomElement)childNode, maxSamples);
        final Histogram<CollapsedKeyContainer<String>>.Frequency<CollapsedKeyContainer<String>> freq = result.set(ckc, ckc.getOrigCount());
        freq.setAttributes(ckc.getAttributes());
      }
    }

    return result;
  }



  private int maxSamples;

  private Long _originalTotalCount;
  private Long _originalNumRanks;
  private Long _originalMaxFrequencyCount;

  /**
   * Construct an empty instance with no switching, no samples.
   */
  public CollapsedHistogram() {
    this(1);
  }

  /**
   * Construct with the given parameters.
   */
  public CollapsedHistogram(int maxSamples) {
    super();

    this.maxSamples = maxSamples;

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

  /**
   * Add an element.
   */
  public Frequency<CollapsedKeyContainer<T>> add(CollapsedKeyContainer<T> element) {
    return add(element, 1);
  }

  public Frequency<CollapsedKeyContainer<T>> add(CollapsedKeyContainer<T> element, long freqCount) {
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
  public long getOriginalTotalCount() {
    if (_originalTotalCount == null) {
      _originalTotalCount = computeOriginalTotalCount();
    }
    return _originalTotalCount;
  }

  private final long computeOriginalTotalCount() {
    long result = 0;

    for (Frequency<CollapsedKeyContainer<T>> freq : getFrequencies()) {
      final CollapsedKeyContainer<T> keyContainer = freq.getElement();
      result += keyContainer.getTotalCount();
    }

    return result;
  }

  /**
   * Get the number of ranks in the original histogram.
   */
  public long getOriginalNumRanks() {
    if (_originalNumRanks == null) {
      _originalNumRanks = computeOriginalNumRanks();
    }
    return _originalNumRanks;
  }

  private final long computeOriginalNumRanks() {
    long result = 0;

    for (Frequency<CollapsedKeyContainer<T>> freq : getFrequencies()) {
      final CollapsedKeyContainer<T> keyContainer = freq.getElement();
      result += keyContainer.getBinCount();
    }

    return result;
  }

  public long getOriginalMaxFrequencyCount() {
    if (_originalMaxFrequencyCount == null) {
      _originalMaxFrequencyCount = getOriginalFrequencyCount(0);
    }
    return _originalMaxFrequencyCount;
  }

  public long getOriginalFrequencyCount(long collapsedRank) {
    long result = 0;

    final Frequency<CollapsedKeyContainer<T>> freq = getRankFrequency(collapsedRank);
    if (freq != null) {
      final CollapsedKeyContainer<T> keyContainer = freq.getElement();
      result = keyContainer.getTotalCount();
    }

    return result;
  }

  public String toString() {
    final StringBuilder result = new StringBuilder();

    final long totalCount = getOriginalTotalCount();
    long cumulativeCount = 0;

    final long numRanks = getOriginalNumRanks();
    final int maxRankDigits = (int)Math.round(MathUtil.log10(numRanks) + 0.5);

    final long maxFreq = getOriginalMaxFrequencyCount();
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

    for (long i = 0; i < getNumRanks(); ++i) {

      final Frequency<CollapsedKeyContainer<T>> freq = getRankFrequency(i);
      final CollapsedKeyContainer<T> keyContainer = freq.getElement();
      final long curRankFrequency = keyContainer.getTotalCount();
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


      if (keyContainer.hasSamples() && keyContainer.getSampleCollector().getNumSamples() > 1) {
        result.
          append(' ').
          append(keyContainer.getSamples().toString());
      }
    }

    return result.toString();
  }

  /**
   * Add an appropriate element to this instance with the given params.
   */
  public Frequency<CollapsedKeyContainer<T>> add(long numBuckets, T bucketKey, long curCount) {
    Frequency<CollapsedKeyContainer<T>> result = null;

    this._originalTotalCount = null;
    this._originalNumRanks = null;
    this._originalMaxFrequencyCount = null;

    if (numBuckets <= curCount) {
      // keep the original key
      final CollapsedKeyContainer<T> keyContainer = new CollapsedKeyContainer<T>(bucketKey, curCount);
      result = super.add(keyContainer, curCount);
    }
    else {
      // store (or update) a countKey
      final CollapsedKeyContainer<T> keyContainer = new CollapsedKeyContainer<T>(null, curCount, numBuckets, maxSamples);
      result = getElementFrequency(keyContainer);
      if (result == null) {
        result = super.add(keyContainer, curCount);
      }
      if (maxSamples > 0) {
        result.getElement().considerSample(bucketKey);
      }
    }

    return result;
  }

  public XmlStringBuilder asXml() {
    return asXml(DEFAULT_COLLAPSED_XML_ROOT_TAG);
  }

  public XmlStringBuilder asXml(String rootTag) {
    final StringBuilder tag = new StringBuilder();
    tag.
      append(rootTag).
      append(" maxSamples='").append(maxSamples).
      append("' total='").append(getTotalCount()).
      append("' bins='").append(getNumRanks()).
      append("' origTotal='").append(getOriginalTotalCount()).
      append("' origRanks='").append(getOriginalNumRanks()).
      append("'");

    final XmlStringBuilder result = new XmlStringBuilder(tag.toString());

    for (Frequency<CollapsedKeyContainer<T>> freq : getFrequencies()) {
      final CollapsedKeyContainer<T> ckc = freq.getElement();
      ckc.setAttributes(freq.getAttributes());
      ckc.asXml(result);
    }

    return result;
  }


  public static void main(String[] args) throws IOException {
    // arg0: file w/xml histogram
    final File xmlFile = new File(args[0]);
    final CollapsedHistogram h = CollapsedHistogram.makeInstance(xmlFile, 5);

    System.out.println(h.toString());
  }
}
