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


import org.sd.xml.TagStack;
import org.sd.xml.XmlLite;
import org.sd.util.tree.Tree;

/**
 * Container for extraction properties.
 * <p>
 * @author Spence Koehler
 */
public class ExtractionProperties {

  /**
   * Property set on xmlNodes with value Map<String, Extraction> mapping 
   * extraction type to its extraction for extractions associated with
   * the node.
   */
  public static final String EXTRACTIONS_PROPERTY = "t2e";  // type2extraction


  /**
   * Property set on xmlNodes that is present with a "true" value (or absent)
   * to indicate a non-body extraction for a post.
   */
  public static final String NON_BODY_EXTRACTION = "nbx";  // non-body extraction


  /**
   * Attribute for xml nodes whose value is "true" when the node holds
   * an applicaable date/time extraction.
   */
  public static final String DATE_TIME_EXTRACTION_ATTRIBUTE = "sd:dtx";


  /**
   * Mark the underlying xml with info identifying the date/time extraction.
   */
  public static final void setDateTimeProperties(Extraction extraction) {
    if (extraction != null) {
      final DocText docText = extraction.getDocText();
      if (docText != null) {
        Tree<XmlLite.Data> xmlNode = docText.getXmlNode();
        if (xmlNode != null) {
          // set NON_BODY_EXTRACTION property
          final XmlLite.Data xmlData = xmlNode.getData();
          xmlData.setProperty(NON_BODY_EXTRACTION, true);

          // set DATE_TIME_EXTRACTION attribute
          XmlLite.Tag xmlTag = xmlData.asTag();
          if (xmlTag == null) {
            // Set the attribute on the text node's parent
            xmlNode = xmlNode.getParent();
            if (xmlNode != null) {
              xmlTag = xmlNode.getData().asTag();
            }
            else {
              final TagStack tagStack = docText.getTagStack();
              if (tagStack != null && tagStack.depth() > 0) {
                xmlTag = tagStack.getTag(tagStack.depth() - 1);
              }
            }
          }

          if (xmlTag != null) {
            xmlTag.setAttribute(DATE_TIME_EXTRACTION_ATTRIBUTE, "true");
          }
          else {
            System.err.println("***WARNING: can't set dateTimeExtractionAttribute!");
          }
        }
      }
    }
  }
}
