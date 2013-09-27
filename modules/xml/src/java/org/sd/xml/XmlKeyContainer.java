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


import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang.StringEscapeUtils;

/**
 * Container class for key information for an xml histogram entry as used
 * in XmlHistogramIterator to encapsulate the information.
 * <p>
 * @author Spence Koehler
 */
public class XmlKeyContainer {
  
  private String key;
  private long count;
  private Map<String, String> attributes;

  public XmlKeyContainer(String key, Map<String, String> attributes) {
    this.key = key;
    this.count = 0;
    this.attributes = null;

    if (attributes != null) {
      this.attributes = new HashMap<String, String>();
      for (Map.Entry<String, String> att : attributes.entrySet()) {
        final String attribute = att.getKey();
        final String val = StringEscapeUtils.unescapeXml(att.getValue());

        if ("count".equals(att.getKey())) {
          try {
            this.count = Long.parseLong(val);
          }
          catch (Exception e) {}
        }
        else {
          this.attributes.put(attribute, val);
        }
      }
    }
  }

  public String getKey() {
    return key;
  }

  public long getCount() {
    return count;
  }

  public Map<String, String> getAttributes() {
    return attributes;
  }
}
