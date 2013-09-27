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
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang.StringEscapeUtils;
import org.sd.util.SampleCollector;
import org.w3c.dom.NodeList;

/**
 * Container for collapsed histogram keys used as the elements in
 * CollapsedHistogram instances.
 * <p>
 * @author Spence Koehler
 */
public class CollapsedKeyContainer <T> implements Comparable<CollapsedKeyContainer<T>> {
  
  /**
   * Construct an instance from collapsed xml format.
   */
  public static CollapsedKeyContainer<String> loadFromXml(DomElement xml, int maxSamples) {

    String key = null;
    long origCount = 0;
    long binCount = 0;
    Map<String, String> attributes = null;

    // load attributes
    if (xml.hasAttributes()) {
      for (Map.Entry<String, String> attrEntry : xml.getDomAttributes().getAttributes().entrySet()) {
        final String attrKey = attrEntry.getKey();
        final String attrVal = attrEntry.getValue();
        if ("key".equals(attrKey)) {
          key = StringEscapeUtils.unescapeXml(attrVal);
        }
        else if ("origCount".equals(attrKey)) {
          origCount = Long.parseLong(attrVal);
        }
        else if ("binCount".equals(attrKey)) {
          binCount = Long.parseLong(attrVal);
        }
        else {
          if (attributes == null) attributes = new HashMap<String, String>();
          attributes.put(attrKey, StringEscapeUtils.unescapeXml(attrVal));
        }
      }
    }

    final CollapsedKeyContainer<String> result = new CollapsedKeyContainer<String>(key, origCount, binCount, maxSamples);
    result.setAttributes(attributes);

    if (maxSamples > 0) {
      final SampleCollector<String> sampleCollector = result.getSampleCollector();
      final NodeList childNodes = xml.getChildNodes();
      final int numNodes = childNodes.getLength();
      for (int childIdx = 0; childIdx < numNodes; ++childIdx) {
        final DomNode childNode = (DomNode)childNodes.item(childIdx);
        if (childNode.getNodeType() == DomNode.ELEMENT_NODE && "sample".equals(childNode.getLocalName())) {
          final String sampleKey = childNode.getAttributeValue("key", null);
          if (sampleKey != null) {
            sampleCollector.add(sampleKey);
          }
        }
      }
    }

    return result;
  }


  private T key;
  private long origCount;  // original freq count for key or keys
  private long binCount;   // original number of buckets w/freq=origCount
  private int maxSamples; // maximum number of samples to collect
  private SampleCollector<T> sampleCollector;
  private Map<String, String> attributes;

  /**
   * Construct an instance backed by a bin key.
   */
  public CollapsedKeyContainer(T key, long origCount) {
    this(key, origCount, 1, 0);
  }

  public CollapsedKeyContainer(T key, long origCount, long binCount, int maxSamples) {
    this.key = key;
    this.origCount = origCount;
    this.binCount = binCount;
    this.maxSamples = maxSamples;
    this.sampleCollector = (maxSamples > 0) ? new SampleCollector<T>(maxSamples) : null;
    this.attributes = null;
  }

  public void setAttributes(Map<String, String> attributes) {
    this.attributes = attributes;
  }

  public Map<String, String> getAttributes() {
    return attributes;
  }

  public boolean isCountKey() {
    return key == null;
  }

  public boolean isNormalKey() {
    return key != null;
  }

  public long getOrigCount() {
    return origCount;
  }

  public long getBinCount() {
    return binCount;
  }

  public void setBinCount(long binCount) {
    this.binCount = binCount;
  }

  public void incBinCount() {
    ++binCount;
  }

  public long getTotalCount() {
    return origCount * binCount;
  }

  /**
   * Get this instance's key.
   */
  public T getKey() {
    return key;
  }

  /**
   * Get the key if set or a sample if there is one.
   */
  public T getAnyKey() {
    T result = key;

    if (result == null && hasSamples()) {
      result = getSamples().get(0);
    }

    return result;
  }

  public void considerSample(T element) {
    if (sampleCollector != null) {
      sampleCollector.consider(element);
    }
  }

  public SampleCollector<T> getSampleCollector() {
    return sampleCollector;
  }

  public boolean hasSamples() {
    return sampleCollector != null && sampleCollector.getNumSamples() > 0;
  }

  public List<T> getSamples() {
    List<T> result = null;

    if (sampleCollector != null) {
      result = sampleCollector.getSamples();
    }

    return result;
  }

  public String buildKeyString() {
    final StringBuilder result = new StringBuilder();

    if (isNormalKey()) {
      result.append(key);
    }
    else {
      // ~"..." (M*N)
      if (sampleCollector != null && sampleCollector.getNumSamples() > 0) {
        result.
          append("~\"").
          append(sampleCollector.getSamples().get(0)).
          append("\" ");
      }
      result.
        append('(').
        append(origCount).append('*').append(binCount).
        append(')');
    }

    return result.toString();
  }

  public String toString() {
    final StringBuilder result = new StringBuilder();

    result.append(buildKeyString());

    if (isNormalKey()) {
      result.append('(').append(origCount).append(')');
    }

    return result.toString();
  }

  /**
   * Serialize this instance as xml into the builder.
   */
  public void asXml(XmlStringBuilder xmlBuilder) {
    // <cc key='...' origCount='...' binCount='...' ...attributes...>
    //   <sample key='...'/>
    //   ...
    // </cc>
    final StringBuilder tag = new StringBuilder();
    tag.append("cc");
    if (key != null) {
      tag.
        append(" key='").
        append(StringEscapeUtils.escapeXml(key.toString())).
        append("'");
    }
    tag.
      append(" origCount='").
      append(origCount).
      append("' binCount='").
      append(binCount).
      append("'");

    if (attributes != null) {
      for (Map.Entry<String, String> attrEntry : attributes.entrySet()) {
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

    if (hasSamples()) {
      xmlBuilder.addTag(tag.toString());
      for (T sample : sampleCollector.getSamples()) {
        if (sample != null) {
          tag.setLength(0);
          tag.
            append("sample key='").
            append(StringEscapeUtils.escapeXml(sample.toString())).
            append("'");
          xmlBuilder.addTagAndText(tag.toString(), null, true);
        }
      }
      xmlBuilder.addEndTag("cc");
    }
    else {
      xmlBuilder.addTagAndText(tag.toString(), null, true);
    }
  }

  public boolean equals(Object o) {
    boolean result = this == o;

    if (!result && o instanceof CollapsedKeyContainer) {
      final CollapsedKeyContainer other = (CollapsedKeyContainer)o;
      if ((this.key == other.key || (this.key != null && this.key.equals(other.key))) &&
          (this.origCount == other.origCount)) {
        result = true;
      }
    }

    return result;
  }

  public int hashCode() {
    int result = 1;

    if (key != null) {
      result = 17 * result + key.hashCode();
    }

    result = 17 * result + (int)origCount;

    return result;
  }

  public int compareTo(CollapsedKeyContainer<T> other) {
    // sort by descending origCount regardless of count -vs- normal keys
    long v = other.origCount - this.origCount;
    if(v < Integer.MIN_VALUE)
      return Integer.MIN_VALUE;
    else if(v > Integer.MAX_VALUE)
      return Integer.MAX_VALUE;
    else
      return (int)v;
  }
}
