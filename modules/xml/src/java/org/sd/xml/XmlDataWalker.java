/*
    Copyright 2010 Semantic Discovery, Inc. (www.semanticdiscovery.com)

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


import java.io.IOException;
import java.io.InputStream;

/**
 * Utility to walk along each xml node, represented as an XmlData instance.
 * <p>
 * 
 * @author Spence Koehler
 */
public class XmlDataWalker {
  
  public enum Directive { VISIT_SKIPPED, NEXT_XML_DATA };

  public interface Visitor {

    /**
     * Visit each terminal xml node, whether it has non-empty text or not.
     * <p>
     * Note that all of the node's parents will be available in the xmlData's tagStack.
     * All tags that were passed in getting to the terminal tag (at the end of the
     * tagStack = tagStack.getTag(tagStack.depth() - 1) are those from xmlData's
     * blockTag (inclusive) to the end of the tagStack. Each of these tags will
     * be visited through visitStackTag if visitNext returns Directive.VISIT_SKIPPED.
     * <p>
     * Otherwise, the next terminal xmlData will be visited with visitNext.
     */
    public Directive visitNext(XmlData xmlData);

    /**
     * Visitor function to apply on each tag in the xmlData's tagStack from
     * xmlData's blockTag (inclusive) to xmlData's tagStack's end tag (inclusive).
     * <p>
     * After each unvisited stack tag has been visited, visitNext will be called
     * on the next terminal (terminated) xml tag.
     */
    public void visitStackTag(XmlData xmlData, int tagIndex);

    /**
     * After visitStackTag has been called on each skipped tag, revisit the
     * xmlData before continuing on to the next.
     * <p>
     * Note that this is only called in cases where visitNext returned
     * VISIT_SKIPPED and after visitStackTag has been called on each skipped
     * tag.
     */
    public void revisitXmlData(XmlData xmlData);
  }


  public static final void visit(InputStream inputStream, Visitor visitor) throws IOException {
    final XmlLeafNodeRipper ripper = new XmlLeafNodeRipper(inputStream, false, null, true, null);
    final XmlDataRipper dataRipper = new XmlDataRipper(ripper);

    while (dataRipper.hasNext()) {
      final XmlData xmlData = dataRipper.next();

      final Directive directive = visitor.visitNext(xmlData);

      if (directive == Directive.NEXT_XML_DATA) {
        continue;
      }
      else if (directive == Directive.VISIT_SKIPPED) {
        for (int tagIndex = xmlData.tagStack.hasTag(xmlData.getBlockTag());
             tagIndex < xmlData.tagStack.depth();
             ++tagIndex) {
          visitor.visitStackTag(xmlData, tagIndex);
        }
        visitor.revisitXmlData(xmlData);
      }
    }

    dataRipper.close();
  }
}
