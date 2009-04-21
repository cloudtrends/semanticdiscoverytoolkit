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
package org.sd.crawl;


import java.io.IOException;
import java.util.List;

/**
 * Interface for processing a CrawledPage.
 * <p>
 * This is used, for example, as a strategy for processing
 * "hit" pages from SearchEngineSettings.
 * 
 * @author Spence Koehler
 */
public interface CrawledPageProcessor {
  
  /**
   * Process the given crawled page.
   *
   * @return true if successful; otherwise, false.
   */
  public boolean processPage(CrawledPage crawledPage) throws IOException;

  /**
   * Get this processor's results.
   */
  public List<ProcessorResult> getResults();

}
