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


import org.sd.util.Histogram;

import java.util.HashSet;
import java.util.Set;

/**
 * A base implementation of a site info module for keeping track of
 * histogram and url data.
 * <p>
 * @author Spence Koehler
 */
public abstract class BaseSiteInfoModule implements SiteInfoModule {
  
  private Histogram<String> histogram;
  private Set<String> fullUrls;
  private int totalSubmittedCount;
  private int totalAddedCount;

  protected BaseSiteInfoModule() {
    this.histogram = new Histogram<String>();
    this.fullUrls = new HashSet<String>();
    this.totalSubmittedCount = 0;
    this.totalAddedCount = 0;
  }

  /**
   * Add a string to the histogram.
   * <p>
   * Adding a null hstring does not affect the histogram, but increments
   * the totalSubmittedCount; non-null hstrings (including empty) are added
   * to the histogram and the totalAddedCount is incremented.
   * <p>
   * The url is added only if it and the hstring are non-null and the url
   * itself is non-empty.
   * 
   * @return true if the hstring was added to the histogram; otherwise, false.
   */
  protected final boolean addString(String hstring, String url) {
    boolean result = false;

    ++totalSubmittedCount;
    if (hstring != null) {
      histogram.add(hstring);  // go ahead and add whether "empty" or not
      ++totalAddedCount;
      result = true;

      if (url != null && !"".equals(url)) {
        fullUrls.add(url);
      }
    }

    return result;
  }

  /**
   * Get the extensions histogram.
   */
  public Histogram<String> getHistogram() {
    return histogram;
  }

  /**
   * Get the urls collected through this module.
   */
  public Set<String> getUrls() {
    return fullUrls;
  }


  /**
   * Get the total number of urls submitted for adding to the histogram.
   */
  protected final int getTotalSubmittedCount() {
    return totalSubmittedCount;
  }

  /**
   * Get the total number of urls actually added to the histogram.
   */
  protected final int getTotalAddedCount() {
    return totalAddedCount;
  }
}
