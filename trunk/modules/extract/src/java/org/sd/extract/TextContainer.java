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

import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Caching iterator iterface around a text document tunable for memory and
 * performance, particulary with potentially massive text documents.
 * <p>
 * @author Spence Koehler
 */
public interface TextContainer extends Iterator<DocText> {
  
  /**
   * Get this text container's name.
   */
  public String getName();

  /**
   * Determine whether this container has more text to get from next().
   */
  public boolean hasNext();

  /**
   * Get the next available text from the document.
   */
  public DocText next();

  /**
   * Remove the last docText retrieved from the cache and decrement the
   * count of doc text instances.
   * <p>
   * If this instance is non-caching, the count is still decremented as a
   * side effect.
   * <p>
   * Note that the underlying document is never altered.
   */
  public void remove();

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
  public void close();

  /**
   * Determine whether this instance is caching docTexts.
   */
  public boolean isCaching();

  /**
   * Determine whether the entire document has been visited through the
   * iterator methods (next/hasNext).
   * <p>
   * Note that once the close method has been called, this value can never
   * change from false to true.
   */
  public boolean isComplete();

  /**
   * Get the number of DocText instances that have been iterated over so far,
   * less those that have been "removed".
   */
  public int getCount();

  /**
   * Get the nth cached docText.
   * <p>
   * If this instance is non-caching (i.e. isCaching returns false) or if
   * the index to retrieve is greater than or equal to the current count
   * (as returned by getCount,) then this will return null.
   * <p>
   * Note that 
   */
  public DocText getDocText(int index);

  /**
   * Convert a docText from this container into another container.
   * <p>
   * This is used to be able to treat each docText from a container as high-
   * level, iterable input in its own right if this makes sense for the
   * docText.
   * 
   * @return the docText as a container or null.
   */
  public TextContainer convertToTextContainer(DocText docText, boolean keepDocTexts);

  /**
   * Get the docTexts that share the same xml data instance in their tag path
   * or xml tree.
   * <p>
   * Note that this uses instance equality (==) and not object equality
   * (.equals) as the match criterion.
   * <p>
   * If this instance is not caching doc texts, the result will be null.
   */
  public List<DocText> getDocTexts(XmlLite.Data xmlData);

  /**
   * Clear lazily loaded caches in all cached docText or extraction result
   * docText instances.
   */
  public void compact();

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
  public boolean remove(DocText docText);

  /**
   * Set a property in this text container.
   */
  public void setProperty(String propertyName, String propertyValue);

  /**
   * Retrieve a property from this container.
   */
  public String getProperty(String propertyName);

  /**
   * Clear this container's properties.
   */
  public void clearProperties();

  /**
   * Get this container's properties.
   */
  public Map<String, String> getProperties();

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
  public ExtractionResults getExtractionResults(boolean create);

  /**
   * Add the extraction to this instance's extraction results, creating the
   * results instance if necessary.
   *
   * @param extraction  The extraction to add to the extraction results.
   * @param extractor   The extractor used to generate the extraction.
   *
   * @return the extraction results instance.
   */
  public ExtractionResults addExtraction(Extraction extraction, Extractor extractor);

  /**
   * Convenience method to get the heading organizer from the extraction results.
   *
   * @return the ExtractionResults' headingOrganizer.
   */
  public HeadingOrganizer getHeadingOrganizer();

  /**
   * Convenience method to get the number of non-empty paths between startIndex
   * (inclusive) and endIndex (exclusive).
   */
  public int numNonEmptyPaths(int startIndex, int endIndex);
}
