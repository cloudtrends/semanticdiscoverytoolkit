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
package org.sd.extract;


import org.sd.util.tree.Tree;
import org.sd.xml.XmlLite;
import org.sd.xml.XmlTreeHelper;

import java.io.IOException;

/**
 * Container for extracted xml data.
 * <p>
 * @author Spence Koehler
 */
public class ExtractionXmlData extends AbstractExtractionData {

  private Tree<XmlLite.Data> xmlTree;

  public ExtractionXmlData(Tree<XmlLite.Data> xmlTree) {
    this.xmlTree = xmlTree;
  }

  public ExtractionXmlData asXmlData() {
    return this;
  }

  public Tree<XmlLite.Data> getXmlTree() {
    return xmlTree;
  }

  public String toString() {
    String result = null;

    try {
      result = XmlLite.asXml(xmlTree, false);
    }
    catch (IOException e) {
      result = "";
    }

    return result;
  }

  /**
   * The extracted string is the text under the xml node.
   */
  public String getExtractedString() {
    return XmlTreeHelper.getAllText(xmlTree);
  }
}
