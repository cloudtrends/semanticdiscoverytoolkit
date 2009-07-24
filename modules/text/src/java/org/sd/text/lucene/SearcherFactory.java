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

/**
 * Factory for searcher and searcher type instances.
 * <p>
 * @author Spence Koehler
 */
public class SearcherFactory {

  /**
   * Get the searcher type for the given string, defaulting to
   * SearcherType.CONSTANT if the type is nonsensical.
   */
  public static final SearcherType getSearcherType(String type) {
    SearcherType result = SearcherType.CONSTANT;  // default

    if (type != null && !"".equals(type)) {
      if ("TRANSIENT".equalsIgnoreCase(type)) {
        result = SearcherType.TRANSIENT;
      }
    }

    return result;
  }

  /**
   * Get a searcher over the given directory according to type.
   */
  public static final Searcher getSearcher(File luceneDir, SearcherType searcherType) {
    Searcher result = null;

    if (luceneDir != null && luceneDir.exists()) {
      switch (searcherType) {
        case TRANSIENT :
          result = new TransientSearcher(luceneDir);
          break;

        case CONSTANT :
        default:
          result = new LuceneSearcher(luceneDir);
          break;
      }
    }

    return result;
  }
}
