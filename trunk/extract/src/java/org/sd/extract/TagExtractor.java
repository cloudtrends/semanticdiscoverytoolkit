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
import org.sd.xml.TagStack;
import org.sd.xml.XmlLite;
import org.sd.xml.XmlTreeHelper;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * An arbitrary tag extractor.
 * <p>
 * @author Spence Koehler
 */
public class TagExtractor extends AbstractExtractor {
  
  private TagNodeAcceptor tagNodeAcceptor;
  private Set<String> tagNames;

  public TagExtractor(String extractionType, String[] tagNames, TagNodeAcceptor tagNodeAcceptor) {
    this(extractionType, tagNames, tagNodeAcceptor, false, false, null);
  }

  public TagExtractor(String extractionType, String[] tagNames, TagNodeAcceptor tagNodeAcceptor,
                       boolean needsCache, boolean stopAtFirst, Disambiguator disambiguator) {
    super(extractionType, null, null, needsCache, stopAtFirst, null, null, disambiguator);

    this.tagNodeAcceptor = tagNodeAcceptor;

    this.tagNames = new HashSet<String>();
    for (String tagName : tagNames) {
      this.tagNames.add(tagName);
    }
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
    List<Extraction> result = null;

    // perform the extraction
    final TagStack tagStack = docText.getTagStack();
    final Tree<XmlLite.Data> xmlNode = docText.getXmlNode();
    final int tagPos = tagStack.hasTag(tagNames);

    if (tagPos >= 0) {
      // build a tree from the tag stack
      final Tree<XmlLite.Data> tagNode = XmlTreeHelper.buildXmlTree(tagStack, xmlNode, tagPos);

      // send the node through the tagNodeAcceptor
      if (tagNodeAcceptor == null || tagNodeAcceptor.acceptTagNode(tagNode)) {
        // add an extraction to result.
        if (result == null) result = new ArrayList<Extraction>();
        result.add(new Extraction(getExtractionType(), docText, 1.0, new ExtractionXmlData(tagNode)));
      }
    }
    else {
      final List<Tree<XmlLite.Data>> tagNodes = XmlTreeHelper.findTags(xmlNode, tagNames, false);
      if (tagNodes != null) {
        // send the nodes through the tagNodeAcceptor
        for (Tree<XmlLite.Data> tagNode : tagNodes) {
          if (die != null && die.get()) return null;
          if (tagNodeAcceptor == null || tagNodeAcceptor.acceptTagNode(tagNode)) {
            // add an extraction to result.
            if (result == null) result = new ArrayList<Extraction>();
            result.add(new Extraction(getExtractionType(), docText, 1.0, new ExtractionXmlData(tagNode)));
          }
        }
      }
    }

    return result;
  }
}
