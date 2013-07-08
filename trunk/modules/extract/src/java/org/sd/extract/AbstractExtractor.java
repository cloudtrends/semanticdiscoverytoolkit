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


import org.sd.nlp.AbstractNormalizer;
import org.sd.nlp.BreakStrategy;
import org.sd.nlp.StringWrapper;

/**
 * Abstract implementation of the Extractor interface.
 * <p>
 * @author Spence Koehler
 */
public abstract class AbstractExtractor implements Extractor {

  private String extractionType;
  private TextAcceptor textAcceptor;
  private TextSplitter textSplitter;
  private boolean needsDocTextCache;
  private boolean stopAtFirst;
  private AbstractNormalizer normalizer;
  private BreakStrategy breakStrategy;
  private Disambiguator disambiguator;

  private String _didFirstPropertyName;

  protected AbstractExtractor(String extractionType, TextAcceptor textAcceptor, TextSplitter textSplitter,
                              boolean needsDocTextCache, boolean stopAtFirst, AbstractNormalizer normalizer,
                              BreakStrategy breakStrategy, Disambiguator disambiguator) {

    this.extractionType = extractionType;
    this.textAcceptor = textAcceptor;
    this.textSplitter = textSplitter;
    this.needsDocTextCache = needsDocTextCache;
    this.stopAtFirst = stopAtFirst;
    this.normalizer = normalizer;
    this.breakStrategy = breakStrategy;
    this.disambiguator = disambiguator;

    this._didFirstPropertyName = null;;
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
   * Determine whether the given doc text should be accepted for extraction.
   */
  public boolean shouldExtract(DocText docText) {
    boolean result = true;

    // check for stopAtFirst constraint
    if (stopAtFirst) {
      if (didFirst(docText)) {
        result = false;
      }
    }

    // check textAcceptor constraint
    if (result) {  // if not already ruled out
      if (textAcceptor != null) {
        result = textAcceptor.accept(docText);
      }
    }

    return result;
  }

  /**
   * Provide access to this extractor's normalizer.
   */
  public AbstractNormalizer getNormalizer() {
    return normalizer;
  }

  public String normalize(String string) {
    String result = null;

    if (normalizer != null) {
      result = normalizer.normalize(string).getNormalized();
    }
    else {
      result = string;
    }

    return result;
  }

  /**
   * Determine whether this extractor hasfinished processing the doc text
   * in the pipeline.
   */
  public boolean isFinishedWithDocText(DocText docText) {
    return "true".equals(docText.getProperty("haltPipeline"));
  }

  protected void setIsFinishedWithDocText(DocText docText) {
    docText.setProperty("haltPipeline", "true");
  }

  /**
   * Determine whether this extractor is finished processing the doc texts's
   * document.
   * <p>
   * At this level, this returns true if stopAtFirst and didFirst.
   * <p>
   * NOTE: Subclasses should extend this with any other criterea for being
   *       finished with a document.
   */
  public boolean isFinishedWithDocument(DocText docText) {
    return stopAtFirst && didFirst(docText);
  }

  /**
   * Get this extractor's disambiguator.
   */
  public Disambiguator getDisambiguator() {
    return disambiguator;
  }

  /**
   * Get the text acceptor.
   */
  protected TextAcceptor getTextAcceptor() {
    return textAcceptor;
  }

  /**
   * Get the text splitter.
   */
  protected TextSplitter getTextSplitter() {
    return textSplitter;
  }

  /**
   * Get the stop at first successful extraction flag.
   */
  protected boolean stopAtFirst() {
    return stopAtFirst;
  }

  /**
   * Get the break strategy.
   */
  public BreakStrategy getBreakStrategy() {
    return breakStrategy;
  }

  /**
   * Get the property name for whether this extractor has successfully
   * extracted an entity from a document.
   */
  protected final String getDidFirstPropertyName() {
    if (_didFirstPropertyName == null) {
      _didFirstPropertyName = extractionType + ".didFirst";
    }
    return _didFirstPropertyName;
  }

  /**
   * Determine whether this extractor has successfully extracted an entity
   * from the document of the doc text.
   */
  protected final boolean didFirst(DocText docText) {
    return (stopAtFirst &&  // this only matters if stopAtFirst is true.
            "true".equals(docText.getTextContainer().getProperty(getDidFirstPropertyName())));
  }

  /**
   * Notify processes that this extractor has successfully extracted an
   * entity from the document of the doc text.
   * <p>
   * Subclasses should call this method from the extract method implementation
   * after (the first or any) successful extraction.
   */
  protected final void setDidFirst(DocText docText) {
    if (stopAtFirst) {  // this only matters if stopAtFirst is true.
      docText.getTextContainer().setProperty(getDidFirstPropertyName(), "true");
    }
  }

  /**
   * Apply the text splitter to the doc text to find the pieces to be extracted.
   *
   * todo: This needs to be refactored to StringWrapper.SubString return values,
   *       which entails making LexicalTokenizers use the same and to be aware
   *       of substring boundaries.
   */
  protected StringWrapper[] extractTextStrings(DocText docText) {
    StringWrapper[] result = null;

    if (textSplitter == null) {
      final StringWrapper stringWrapper = (breakStrategy == null) ? docText.getStringWrapper() : docText.getStringWrapper(breakStrategy);
      result = new StringWrapper[]{stringWrapper};
    }
    else {
      result = textSplitter.split(docText, breakStrategy);
    }

    return result;
  }
}
