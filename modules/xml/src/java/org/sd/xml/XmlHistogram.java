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


import java.util.List;
import org.sd.util.Histogram;
import org.w3c.dom.NodeList;

/**
 * Utility for representing a histogram in XML.
 * <p>
 * @author Spence Koehler
 */
public class XmlHistogram extends Histogram<String> {
  
  public static final String DEFAULT_ROOT_TAG = "histogram";


  /**
   * Construct an empty instance.
   */
  public XmlHistogram() {
    super();
  }

  //
  // <histogram>
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
        final int count = childNode.getAttributeInt("count", 0);
        if (count > 0) {
          final String key = childNode.getTextContent();
          if (key != null) {
            add(key, count);
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
    return asXml(DEFAULT_ROOT_TAG);
  }

  /**
   * Convert this histogram to xml with the given rootTag.
   * <p>
   * Generated xml is of the form:
   * <p>
   * &lt;_rootTag_&gt;&lt;key count='...'&gt;...element text...&lt;/key&gt;...&lt;/_rootTag_&gt;
   */
  public XmlStringBuilder asXml(String rootTag) {
    final XmlStringBuilder result = new XmlStringBuilder(rootTag);
    final StringBuilder tag = new StringBuilder();

    final List<Frequency<String>> freqs = getFrequencies();
    for (Frequency<String> freq : freqs) {
      tag.append("key count='").append(freq.getFrequency()).append("'");
      result.addTagAndText(tag.toString(), freq.element, true);
      tag.setLength(0);
    }

    return result;
  }
}
