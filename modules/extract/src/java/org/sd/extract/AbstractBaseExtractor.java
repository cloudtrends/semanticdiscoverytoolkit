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

/**
 * An abstract base extractor implementation.
 * <p>
 * @author Spence Koehler
 */
public abstract class AbstractBaseExtractor implements Extractor {
  
  private String extractionType;
  private boolean needsDocTextCache;
  private Disambiguator _disambiguator;
  private final Object disambiguatorMutex = new Object();

  protected AbstractBaseExtractor(String extractionType, boolean needsDocTextCache) {
    this.extractionType = extractionType;
    this.needsDocTextCache = needsDocTextCache;
    this._disambiguator = null;
  }

  /**
   * Get this extractor's type designator.
   */
  public String getExtractionType() {
    return extractionType;
  }

  /**
   * Determine whether this extractor needs the doc text's text container to
   * cache doc text instances.
   */
  public boolean needsDocTextCache() {
    return needsDocTextCache;
  }

  /**
   * Provide access to this extractor's normalizer.
   * <p>
   * Extenders can override this whose default behavior is to always return null.
   */
  public Normalizer getNormalizer() {
    return null;
  }

  /**
   * Determine whether this extractor hasfinished processing the doc text
   * in the pipeline.
   * <p>
   * Extenders can override this whose default behavior is to always return false.
   */
  public boolean isFinishedWithDocText(DocText docText) {
    return false;
  }

  /**
   * Determine whether this extractor is finished processing the doc texts's
   * document.
   * <p>
   * Extenders can override this whose default behavior is to always return false.
   */
  public boolean isFinishedWithDocument(DocText docText) {
    return false;
  }

  /**
   * Get this extractor's disambiguator.
   * <p>
   * Extenders can override this whose default behavior is to always return a default
   * first interpretation disambiguator.
   */
  public Disambiguator getDisambiguator() {
    synchronized (disambiguatorMutex) {
      if (_disambiguator == null) {
        _disambiguator = new FirstInterpretationDisambiguator(DefaultInterpreter.getInstance());
      }
    }

    return _disambiguator;
  }
}
