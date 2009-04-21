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
package org.sd.text.lucene;


import java.io.File;
import java.io.IOException;

/**
 * A searcher over a lucene store that usually keeps the store closed.
 * <p>
 * This type of searcher is intended to be used in the context where fast
 * responses are sacrificed for the ability to have a large number of
 * indexes active.
 *
 * @author Spence Koehler
 */
public class TransientSearcher extends BaseSearcher {
  
  /**
   * Construct a lucene searcher.
   *
   * @param dirPath  The path to the lucene store directory.
   */
  public TransientSearcher(String dirPath) {
    super(dirPath);
  }

  /**
   * Construct a lucene searcher.
   *
   * @param dirPath  The file of the lucene store directory.
   */
  public TransientSearcher(File dirPath) {
    super(dirPath);
  }

  /**
   * Get this searcher's resources.
   */
  protected SearchResources getResources() {
    // always construct a new instance
    SearchResources resources = null;

    try {
      resources = new SearchResources(getDirPath());
    }
    catch (IOException e) {
      throw new IllegalStateException(e);
    }

    return resources;
  }

  /**
   * Release the resources (if meaninful) after performing a search or
   * executing a search strategy.
   */
  protected void releaseResources(SearchResources resources) {
    // always close the new instance.
    if (resources != null) {
      try {
        resources.close();
      }
      catch (IOException e) {
        throw new IllegalStateException(e);
      }
    }
  }
}
