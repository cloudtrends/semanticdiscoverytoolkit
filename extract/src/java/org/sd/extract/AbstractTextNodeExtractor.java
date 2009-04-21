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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * An abstract extractor that operates on every text node in each docText.
 * <p>
 * NOTE: extenders of this class are extractors suitable for a PivotContextExtractor.
 *
 * @author Spence Koehler
 */
public abstract class AbstractTextNodeExtractor extends AbstractBaseExtractor {

  /**
   * Determine whether the non-empty text from the textNode in the docText
   * has a valid entity.
   *
   * @return true if the text has a valid entity; otherwise, false.
   */
  protected abstract boolean hasEntity(DocText docText, Tree<XmlLite.Data> textNode, String text);

  /**
   * Add extraction(s) to the result from the identified entityText found
   * in the docText.
   */
  protected abstract void addTextExtraction(List<Extraction> result, DocText docText, Tree<XmlLite.Data> textNode, String entityText);


  protected AbstractTextNodeExtractor(String extractionType, boolean needsDocTextCache) {
    super(extractionType, needsDocTextCache);
  }

  /**
   * Determine whether the given doc text should be accepted for extraction.
   */
  public boolean shouldExtract(DocText docText) {
    boolean result = false;

    final Tree<XmlLite.Data> xmlNode = docText.getXmlNode();
    for (Iterator<Tree<XmlLite.Data>> iter = xmlNode.iterator(Tree.Traversal.DEPTH_FIRST); iter.hasNext(); ) {
      final Tree<XmlLite.Data> curNode = iter.next();
      final XmlLite.Text nodeText = curNode.getData().asText();
      if (nodeText != null && nodeText.text != null && nodeText.text.length() > 0) {
        final boolean hasEntity = hasEntity(docText, curNode, nodeText.text);
        if (hasEntity) {
          nodeText.setProperty("extracted", getExtractionType());
          result = true;
        }
      }
    }

    return result;
  }

  /**
   * Perform the extraction on the doc text.
   * 
   * @param docText  The docText to extract from.
   * @param die      Trigger to halt processing. Needs to be monitored and
   *                 obeyed; but can also be set from within the implementation.
   *                 Use with care!
   *
   * @return one or more extractions or null.
   */
  public List<Extraction> extract(DocText docText, AtomicBoolean die) {
    List<Extraction> result = new ArrayList<Extraction>();

    //
    // if here, found entity during shouldExtract.
    //

    final Tree<XmlLite.Data> xmlNode = docText.getXmlNode();
    for (Iterator<Tree<XmlLite.Data>> iter = xmlNode.iterator(Tree.Traversal.DEPTH_FIRST); iter.hasNext(); ) {
      if (die != null && die.get()) break;

      final Tree<XmlLite.Data> curNode = iter.next();
      final XmlLite.Text nodeText = curNode.getData().asText();
      if (getExtractionType().equals(nodeText.getProperty("extracted"))) {
        addTextExtraction(result, docText, curNode, nodeText.text);
      }
    }

    return result;
  }
}
