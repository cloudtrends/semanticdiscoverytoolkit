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


import org.sd.xml.XmlLite;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A text container that holds a single line.
 *
 * @author Spence Koehler
 */
public class SingleLineTextContainer implements TextContainer {
  
  private String name;
  private String line;
  private DocText docText;
  private final AtomicBoolean isClosed = new AtomicBoolean(false);
  private Map<String, String> properties;
  private ExtractionResults extractionResults;

  public SingleLineTextContainer(String name, String line) {
    this.name = name;
    this.line = line;
    this.docText = null;
    this.properties = null;
    this.extractionResults = null;
  }

  /**
   * Get this text container's name.
   */
  public String getName() {
    return name;
  }

  /**
   * Determine whether this container has more text to get from next().
   */
  public boolean hasNext() {
    return !isClosed.get() && docText == null && line != null;
  }

  /**
   * Get the next available text from the document.
   */
  public DocText next() {
    DocText result = null;

    if (hasNext()) {
      this.docText = new DocText(this, line, false);
      result = this.docText;
    }

    return result;
  }

  /**
   * Remove the last docText retrieved from the cache and decrement the
   * count of doc text instances.
   * <p>
   * If this instance is non-caching, the count is still decremented as a
   * side effect.
   * <p>
   * Note that the underlying document is never altered.
   */
  public void remove() {
    if (!isClosed.get()) this.docText = null;
  }

  /**
   * Close this container.
   * <p>
   * It is important that this method be called when finished iterating over
   * the text (through hasNext and next) to properly release resources.
   * <p>
   * This method should typically be called in a finally block for the
   * hasNext/next iteration.
   * <p>
   * Once this has been called, hasNext() will return false and next() will
   * return null, but all other methods will retain their defined behavior.
   */
  public void close() {
    isClosed.set(true);
  }

  /**
   * Determine whether this instance is caching docTexts.
   */
  public boolean isCaching() {
    return true;
  }

  /**
   * Determine whether the entire document has been visited through the
   * iterator methods (next/hasNext).
   * <p>
   * Note that once the close method has been called, this value can never
   * change from false to true.
   */
  public boolean isComplete() {
    return docText != null;
  }

  /**
   * Get the number of DocText instances that have been iterated over so far,
   * less those that have been "removed".
   */
  public int getCount() {
    return docText == null ? 0 : 1;
  }

  /**
   * Get the nth cached docText.
   * <p>
   * If this instance is non-caching (i.e. isCaching returns false) or if
   * the index to retrieve is greater than or equal to the current count
   * (as returned by getCount,) then this will return null.
   * <p>
   * Note that 
   */
  public DocText getDocText(int index) {
    DocText result = null;

    if (docText != null && index == 0) {
      result = docText;
    }

    return result;
  }

  /**
   * Convert a docText from this container into another container.
   * <p>
   * This is used to be able to treat each docText from a container as high-
   * level, iterable input in its own right if this makes sense for the
   * docText.
   * 
   * @return the docText as a container or null.
   */
  public TextContainer convertToTextContainer(DocText docText, boolean keepDocTexts) {
    return null;
  }

  /**
   * Get the docTexts that share the same xml data instance in their tag path
   * or xml tree.
   * <p>
   * Note that this uses instance equality (==) and not object equality
   * (.equals) as the match criterion.
   * <p>
   * If this instance is not caching doc texts, the result will be null.
   */
  public List<DocText> getDocTexts(XmlLite.Data xmlData) {
    final List<DocText> result = new ArrayList<DocText>();

    if (docText != null && docText.getXmlData() == xmlData) {
      result.add(docText);
    }

    return result;
  }

  /**
   * Clear lazily loaded caches in all cached docText or extraction result
   * docText instances.
   */
  public void compact() {
    if (docText != null) docText.compact();
  }

  /**
   * Remove the given docText from the cache and decrement the count of doc
   * text instances.
   * <p>
   * If this instance is non-caching, the number of instances will be
   * decremented down to zero, returning true, and then will return false
   * regardless of the docText instance.
   * <p>
   * If this instance is caching, then only if the doc text instance is in
   * the cache will it be removed and the count decremented.
   *
   * @return true if the count was decremented; otherwise, false.
   */
  public boolean remove(DocText docText) {
    boolean result = false;

    if (this.docText != null && this.docText == docText) {
      this.docText = null;
      close();
      result = true;
    }
    
    return result;
  }

  /**
   * Set a property in this text container.
   */
  public void setProperty(String propertyName, String propertyValue) {
    if (properties == null) {
      properties = new HashMap<String, String>();
    }
    properties.put(propertyName, propertyValue);
  }

  /**
   * Retrieve a property from this container.
   */
  public String getProperty(String propertyName) {
    return (properties == null) ? null : properties.get(propertyName);
  }

  /**
   * Clear this container's properties.
   */
  public void clearProperties() {
    if (properties != null) properties.clear();
  }

  /**
   * Get this container's properties.
   */
  public Map<String, String> getProperties() {
    if (properties == null) {
      properties = new HashMap<String, String>();
    }
    return this.properties;
  }

  /**
   * Get the extraction results instance associated with this text container.
   * <p>
   * If one has not been created yet, return null; unless the create flag is
   * true.
   *
   * @param create  flag to force creation when an instance doesn't exist.
   *
   * @return the extraction results or null.
   */
  public ExtractionResults getExtractionResults(boolean create) {
    if (this.extractionResults == null && create) {
      this.extractionResults = new ExtractionResults();
    }

    return extractionResults;
  }

  /**
   * Add the extraction to this instance's extraction results, creating the
   * results instance if necessary.
   *
   * @param extraction  The extraction to add to the extraction results.
   * @param extractor   The extractor used to generate the extraction.
   *
   * @return the extraction results instance.
   */
  public ExtractionResults addExtraction(Extraction extraction, Extractor extractor) {
    final ExtractionResults result = getExtractionResults(true);
    result.addExtraction(extraction);
    result.addDisambiguatorIfNeeded(extractor);

    return result;
  }

  /**
   * Convenience method to get the heading organizer from the extraction results.
   *
   * @return the ExtractionResults' headingOrganizer.
   */
  public HeadingOrganizer getHeadingOrganizer() {
    final ExtractionResults result = getExtractionResults(true);
    return extractionResults.getHeadingOrganizer();
  }

  /**
   * Convenience method to get the number of non-empty paths between startIndex
   * (inclusive) and endIndex (exclusive).
   */
  public int numNonEmptyPaths(int startIndex, int endIndex) {
    return (docText != null) ? 1 : 0;
  }
}
