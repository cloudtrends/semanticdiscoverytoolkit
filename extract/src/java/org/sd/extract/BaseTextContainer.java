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


import org.sd.util.LookAheadIterator;
import org.sd.xml.XmlLite;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Base implementation of the text container interface.
 * <p>
 * @author Spence Koehler
 */
public abstract class BaseTextContainer <T> implements TextContainer {
  
  /**
   * Build an iterator for this container over the data of type T.
   */
  protected abstract Iterator<T> buildIterator();

  /**
   * Build a doc text instance for the datum.
   */
  protected abstract DocText buildDocText(T datum, int index);


  private LookAheadIterator<T> _lookAheadIterator;
  private int count;
  private List<DocText> cache;
  private DocText latestDocText;
  private Map<String, String> properties;
  private ExtractionResults extractionResults;

  /**
   * Vanilla constructor for xml.
   */
  protected BaseTextContainer(boolean keepDocTexts) {
    this._lookAheadIterator = null;
    this.count = 0;
    this.cache = keepDocTexts ? new ArrayList<DocText>() : null;
    this.latestDocText = null;
    this.properties = null;
    this.extractionResults = null;
  }

  protected final LookAheadIterator<T> getLookAheadIterator() {
    if (_lookAheadIterator == null) {
      _lookAheadIterator = new LookAheadIterator<T>(buildIterator());
    }
    return _lookAheadIterator;
  }

  /**
   * Determine whether this container has more text to get from next().
   */
  public boolean hasNext() {
    return getLookAheadIterator().hasNext();
  }

  /**
   * Get the next available text from the document.
   */
  public DocText next() {
    DocText result = null;

    if (getLookAheadIterator().hasNext()) {
      final T datum = getLookAheadIterator().next();
      result = buildDocText(datum, count);

      if (cache != null) {
        if (result.getPathIndex() >= cache.size()) {
          cache.add(result);
        }
      }
      ++count;
    }

    if (cache != null) {
      this.latestDocText = result;
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
    if (cache == null && count > 0) {
      --count;
    }
    else if (this.latestDocText != null) {
      remove(latestDocText);
    }
  }

  /**
   * Determine whether this instance is caching docTexts.
   */
  public boolean isCaching() {
    return (cache != null);
  }

  /**
   * Get the number of DocText instances that have been iterated over so far,
   * less those that have been "removed".
   */
  public int getCount() {
    return count;
  }

  /**
   * Get the nth cached docText.
   * <p>
   * If this instance is non-caching (i.e. isCaching returns false) or if
   * the index is less than zero (as returned by getCount,) then this will
   * return null.
   * <p>
   * This impl will "look ahead" to retrieve data from xml nodes not yet
   * iterated over without affecting the current iteration.
   */
  public DocText getDocText(int index) {
    DocText result = null;

    if (cache != null && index >= 0) {
      if (index >= cache.size()) {
        // create cache entries
        final int numNeeded = cache.size() - index + 1;
        final int offset = cache.size() - count;
        for (int i = 1; i <= numNeeded; ++i) {
          final T datum = getLookAheadIterator().lookAhead(i + offset);
          if (datum == null) break;
          cache.add(buildDocText(datum, index));
        }
      }

      if (index < cache.size()) {
        result = cache.get(index);
      }
    }

    return result;
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
    List<DocText> result = null;

    if (cache != null) {
      for (DocText docText : cache) {
        if (docText.hasData(xmlData)) {
          if (result == null) result = new ArrayList<DocText>();
          result.add(docText);
        }
      }
    }

    return result;
  }

  /**
   * Clear lazily loaded caches in all cached docText or extraction result
   * docText instances.
   */
  public void compact() {
    if (cache != null) {
      for (DocText docText : cache) {
        docText.compact();
      }
    }
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

    if (cache != null) {
      result = cache.remove(docText);
    }
    else {
      if (count > 0) {
        result = true;
      }
    }

    if (result) --count;

    return result;
  }

  /**
   * Set a property in this text container.
   */
  public void setProperty(String propertyName, String propertyValue) {
    if (properties == null) properties = new HashMap<String, String>();
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
    properties = null;
  }

  /**
   * Get this container's properties.
   */
  public Map<String, String> getProperties() {
    return properties;
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
    return endIndex - startIndex;
  }
}
