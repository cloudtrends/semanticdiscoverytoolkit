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


import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang.StringEscapeUtils;
import org.sd.io.FileUtil;
import org.sd.util.Histogram;
import org.sd.util.HistogramDistribution;
import org.w3c.dom.NodeList;

/**
 * Utility for representing a histogram in XML.
 * <p>
 * @author Spence Koehler
 */
public class XmlHistogram extends Histogram<String> {
  
  public static final String DEFAULT_ROOT_TAG = "histogram";
  public static final KeyComparator s_keyComparator = new KeyComparator();
  private static final class KeyComparator 
    implements Comparator<String>
  {
    public int compare(String o1, String o2) { return o1.compareTo(o2); }
    public boolean equals(Object obj) { return (obj instanceof KeyComparator); }
  }

  /**
   * Convert an arbitrary histogram to an XmlHistogram by converting each
   * key to a string using the object's toString method.
   */
  public static <T> XmlHistogram asXmlHistogram(Histogram<T> histogram) {
    final XmlHistogram result = new XmlHistogram();

    for (Histogram<T>.Frequency<T> freq : histogram.getFrequencies()) {
      result.add(freq.getElement().toString(), freq.getFrequency());
    }

    return result;
  }

  /**
   * Load the distribution histogram for the xml histogram contained in the
   * given file.
   * <p>
   * NOTE: This method incrementally considers each key without ever loading
   * everything into memory to guard against overwhelming memory with very
   * large histograms.
   */
  public static HistogramDistribution loadDistribution(File xmlHistogram) throws IOException {
    final HistogramDistribution result = new HistogramDistribution();

    XmlHistogramIterator iter = null;
    try {
      for (iter = new XmlHistogramIterator(xmlHistogram); iter.hasNext(); ) {
        final XmlKeyContainer keyContainer = iter.next();
        result.add(keyContainer.getCount());
      }
    }
    finally {
      if (iter != null) iter.close();
    }

    return result;
  }


  /**
   * Construct an empty instance.
   */
  public XmlHistogram() {
    super();
  }

  //
  // <histogram total='' bins=''>
  //   <key count=''>_string_</key>
  //   ...
  // </histogram>

  /**
   * Construct with histogram text of the form:
   * <p>
   * &lt;_rootTag_&gt;&lt;key count='...'&gt;...element text...&lt;/key&gt;...&lt;/_rootTag_&gt;
   */
  public XmlHistogram(String histogramXml) {
    this(new XmlStringBuilder().setXmlString(histogramXml));
  }

  public XmlHistogram(File xmlHistogram) throws IOException {
    XmlHistogramIterator iter = null;
    try {
      for (iter = new XmlHistogramIterator(xmlHistogram); iter.hasNext(); ) {
        final XmlKeyContainer keyContainer = iter.next();
        add(keyContainer);
      }
    }
    finally {
      if (iter != null) iter.close();
    }
  }

  /**
   * Construct with histogram xml of the form:
   * <p>
   * &lt;_rootTag_&gt;&lt;key count='...'&gt;...element text...&lt;/key&gt;...&lt;/_rootTag_&gt;
   */
  public XmlHistogram(DomElement histogram) {
    final NodeList childNodes = histogram.getChildNodes();
    final int numNodes = childNodes.getLength();
    for (int childIdx = 0; childIdx < numNodes; ++childIdx) {
      final DomNode childNode = (DomNode)childNodes.item(childIdx);
      if (childNode.getNodeType() == DomNode.ELEMENT_NODE && "key".equals(childNode.getLocalName())) {
        // create entry with count
        final long count = childNode.getAttributeLong("count", 0L);
        if (count > 0) {
          final String key = childNode.getTextContent();
          if (key != null) {
            final Map<String, String> atts = ((DomElement)childNode).getDomAttributes().getAttributes();
            final XmlKeyContainer keyContainer = new XmlKeyContainer(key, atts);
            add(keyContainer);
          }
        }
      }
    }
  }

  /**
   * Construct with histogram xml of the form:
   * <p>
   * &lt;_rootTag_&gt;&lt;key count='...'&gt;...element text...&lt;/key&gt;...&lt;/_rootTag_&gt;
   */
  public XmlHistogram(XmlStringBuilder histogramXml) {
    this(histogramXml.getXmlElement());
  }

  /**
   * Convert this histogram to xml with the given rootTag.
   * <p>
   * Generated xml is of the form:
   * <p>
   * &lt;histogram&gt;&lt;key count='...'&gt;...element text...&lt;/key&gt;...&lt;/histogram&gt;
   */
  public XmlStringBuilder asXml() {
    return asXml(DEFAULT_ROOT_TAG, false);
  }
  public XmlStringBuilder asXml(String rootTag) {
    return asXml(rootTag, false);
  }

  /**
   * Convert this histogram to xml with the given rootTag.
   * <p>
   * Generated xml is of the form:
   * <p>
   * &lt;_rootTag_&gt;&lt;key count='...'&gt;...element text...&lt;/key&gt;...&lt;/_rootTag_&gt;
   */
  public XmlStringBuilder asXml(String rootTag, boolean sortByKey) {
    final StringBuilder tag = new StringBuilder();
    buildRootTag(tag, rootTag);

    final XmlStringBuilder result = new XmlStringBuilder(tag.toString());
    tag.setLength(0);

    final List<Frequency<String>> freqs = 
      (sortByKey ? getFrequencies(s_keyComparator) : getFrequencies());
    for (Frequency<String> freq : freqs) {
      buildFreqTag(tag, freq);
      result.addTagAndText(tag.toString(), freq.element, true);
      tag.setLength(0);
    }

    return result;
  }

  /**
   * Dump this histogram directly to the given output file.
   * <p>
   * NOTE: In the case of very large histograms, this avoids the memory cost
   *       of holding the full histogram and its xml version in memory at
   *       the same time.
   */
  public void dumpXml(File xmlOutputFile) throws IOException {
    dumpXml(DEFAULT_ROOT_TAG, xmlOutputFile);
  }

  public void dumpXml(String rootTag, File xmlOutputFile) throws IOException {
    BufferedWriter writer = null;
    try {
      final StringBuilder tag = new StringBuilder();

      writer = FileUtil.getWriter(xmlOutputFile);
      writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");

      tag.append('<');
      buildRootTag(tag, rootTag);
      tag.append(">\n");
      writer.write(tag.toString());

      for (Frequency<String> freq : getFrequencies()) {
        tag.setLength(0);
        tag.append("  <");
        buildFreqTag(tag, freq);
        tag.append(">").append(freq.element).append("</key>\n");
        writer.write(tag.toString());
      }

      tag.setLength(0);
      tag.append("</").append(rootTag).append(">\n");
      writer.write(tag.toString());
    }
    finally {
      if (writer != null) writer.close();
    }
  }

  private final void buildRootTag(StringBuilder tag, String rootTag) {
    tag.
      append(rootTag).append(" total='").append(getTotalCount()).
      append("' bins='").append(getNumRanks()).append("'");
  }

  private final void buildFreqTag(StringBuilder tag, Frequency<String> freq) {
    tag.append("key count='").append(freq.getFrequency()).append("'");
    if (freq.hasAttributes()) {
      for (Map.Entry<String, String> attrEntry : freq.getAttributes().entrySet()) {
        final String attribute = attrEntry.getKey();
        final String val = attrEntry.getValue();
        tag.
          append(' ').
          append(attribute).
          append("='").
          append(StringEscapeUtils.escapeXml(val)).
          append("'");
      }
    }
  }

  private final void add(XmlKeyContainer keyContainer) {
    add(keyContainer.getKey(), keyContainer.getCount()).setAttributes(keyContainer.getAttributes());
  }


  public static void main(String[] args) throws IOException {
    // arg0: file w/xml histogram
    // arg1: (optional) limit on # of entries to show
    final File xmlFile = new File(args[0]);
    //final DomElement histoXml = XmlFactory.loadDocument(xmlFile, false).getDocumentDomElement();
    //final XmlHistogram xmlHisto = new XmlHistogram(histoXml);
    final XmlHistogram xmlHisto = new XmlHistogram(xmlFile);

    if (args.length > 1) {
      final int limit = Integer.parseInt(args[1]);
      System.out.println(xmlHisto.toString(limit));

      System.out.println("\nDistribution " + xmlHisto.getDistribution().toString(0));
    }
    else {
      System.out.println(xmlHisto.toString());
    }
  }
}
