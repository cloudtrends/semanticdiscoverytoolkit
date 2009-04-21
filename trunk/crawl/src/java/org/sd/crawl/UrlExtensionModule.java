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


import org.sd.text.DetailedUrl;
import org.sd.util.Histogram;

import java.util.HashSet;
import java.util.Set;

/**
 * A site info module for collecting extension stats.
 * <p>
 * @author Spence Koehler
 */
public abstract class UrlExtensionModule extends BaseSiteInfoModule {
  
  protected UrlExtensionModule() {
    super();
  }

  /**
   * Add the url's extension to the histogram.
   */
  protected final boolean addUrl(DetailedUrl dUrl) {
    final String hstring = dUrl != null ? dUrl.getTargetExtension(true/*normalized*/) : null;
    final String url = dUrl != null ? dUrl.getNormalizedUrl() : null;

    return super.addString(hstring, url);
  }

  /**
   * Add the link's destination url's extension to the histogram.
   */
  protected final boolean addLink(Link link) {
    final DetailedUrl dUrl = link.getDestUrl().getDetailedUrl();
    return addUrl(dUrl);
  }
}
