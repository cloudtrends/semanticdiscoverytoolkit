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


import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * An abstract extractor implementation for extracting entities.
 * <p>
 * @author Spence Koehler
 */
public abstract class AbstractEntityExtractor extends AbstractBaseExtractor {

  /**
   * Get valid text for the entity from the docText.
   * <p>
   * NOTE: Implementations should set a property on the XmlLite.Data of
   *       the text node(s) containing the entity of key="extracted",
   *       value=getExtractionType() if the extractor is to be used
   *       as a pivot extractor. See PivotContextExtractor.
   *
   * @return valid entity text or null.
   */
  protected abstract String getEntityText(DocText docText);

  /**
   * Build the extraction result given a docText and non-null entityText (as
   * returned by getEntityText on the docText).
   */
  protected abstract List<Extraction> buildExtraction(DocText docText, String entityText);


  protected AbstractEntityExtractor(String extractionType, boolean needsDocTextCache) {
    super(extractionType, needsDocTextCache);
  }

  /**
   * Determine whether the given doc text should be accepted for extraction.
   */
  public boolean shouldExtract(DocText docText) {
    boolean result = false;

    final String entityText = getEntityText(docText);
    if (entityText != null) {
      docText.setProperty(getExtractionType(), entityText);
      result = true;
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
    if (die != null && die.get()) return null;

    //
    // if here, found entity during shouldExtract.
    //

//    String string = docText.getString();
    final String entityText = docText.getProperty(getExtractionType());

    final List<Extraction> result = buildExtraction(docText, entityText);
    return result;
  }
}
