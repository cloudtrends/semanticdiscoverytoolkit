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


import org.sd.nlp.Normalizer;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A "mini extractor" to apply to a doc text.
 * <p>
 * @author Spence Koehler
 */
public interface Extractor {

  /**
   * Get this extractor's type designator.
   */
  public String getExtractionType();

  /**
   * Determine whether this extractor needs the doc text's text container to
   * cache doc text instances.
   */
  public boolean needsDocTextCache();

  /**
   * Determine whether the given doc text should be accepted for extraction.
   */
  public boolean shouldExtract(DocText docText);

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
  public List<Extraction> extract(DocText docText, AtomicBoolean die);

  /**
   * Provide access to this extractor's normalizer.
   */
  public Normalizer getNormalizer();

  /**
   * Determine whether this extractor hasfinished processing the doc text
   * in the pipeline.
   */
  public boolean isFinishedWithDocText(DocText docText);

  /**
   * Determine whether this extractor is finished processing the doc texts's
   * document.
   */
  public boolean isFinishedWithDocument(DocText docText);

  /**
   * Get this extractor's disambiguator.
   */
  public Disambiguator getDisambiguator();
}
