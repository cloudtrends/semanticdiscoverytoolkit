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
package org.sd.cluster.config;


import org.sd.cluster.io.Message;
import org.sd.cluster.io.Response;
import org.sd.cluster.io.XmlResponse;
import org.sd.xml.DomElement;
import org.sd.xml.XmlStringBuilder;
import org.w3c.dom.NodeList;

/**
 * Utility methods for working with XmlResponse instances.
 * <p>
 * @author Spence Koehler
 */
public class XmlResponseUtil {
  
  /**
   * Use the console to send the message that results in an XmlResponse to
   * each node in the indicated group (by name). Consolidate all XmlElements
   * under each root ("response") element in the XmlResponses into the given
   * xmlResult (presumably under its "response" element.)
   */
  public static final Response[] consolidateResponses(XmlStringBuilder xmlResult, Console console,
                                                      Message message, String group, int timeout,
                                                      boolean requireAllResponses) throws ClusterException {

    final Response[] responses = console.sendMessageToNodes(message, group, timeout, requireAllResponses);

    // compile (collapse/consolidate) into a single result to return
    if (responses != null) {
      for (Response response : responses) {
        if (response instanceof XmlResponse) {
          final XmlResponse xmlResponse = (XmlResponse)response;
          final DomElement domElement = xmlResponse.getXmlElement();
          final NodeList children = domElement.getChildNodes();
          final int numChildren = children.getLength();
          for (int childNum = 0; childNum < numChildren; ++childNum) {
            final DomElement childElement = (DomElement)children.item(childNum);
            xmlResult.addElement(childElement);
          }
        }
      }
    }

    return responses;
  }
}
