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
package org.sd.xml;

import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.parsers.*;
import org.xml.sax.*;
import org.w3c.dom.*;

import java.util.*;
import java.io.*;

/**
 * XML Pretty-Printing utility functions
 * <p>
 * @author Ryan McGuire
 */
public class XmlPrettyPrint {

  /**
   * Dump an XML document object to an OutputStream in a pretty format
   *
   * @param doc      The XML document object
   * @param out      The OutputStream to dump to
   */
  public final static void serialize(Document doc, OutputStream out) throws TransformerException {
    
    TransformerFactory tfactory = TransformerFactory.newInstance();
    Transformer serializer;
    serializer = tfactory.newTransformer();
    //Setup indenting to "pretty print"
    serializer.setOutputProperty(OutputKeys.INDENT, "yes");
    serializer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
    serializer.transform(new DOMSource(doc), new StreamResult(out));
  }

  /**
   * Format an XML string in a pretty format
   *
   * @param xml      The XML String
   *
   * @return a pretty formated xml String
   */
  public final static String getString(String xml) throws ParserConfigurationException, SAXException, IOException, TransformerException{
    ByteArrayOutputStream pretty_xml = new ByteArrayOutputStream();

    //Prepare the xml string for input
    StringReader reader = new StringReader(xml);
    InputSource input_source = new InputSource(reader);
    DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
    Document doc = builder.parse(input_source);
    serialize(doc, pretty_xml);
    return pretty_xml.toString();
  }
}
