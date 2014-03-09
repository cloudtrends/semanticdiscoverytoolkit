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
import org.sd.util.PathWrapper;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Container for url information.
 * <p>
 * @author Spence Koehler
 */
public class UrlData {

  private String urlString;
  private URL _url;
  private Exception _error;
  private DetailedUrl _dUrl;
  private String _cleanString;
  private Long crawlDelay;
  private Long forceRefreshTime;

  private String referringUrl;
  private String referringLinkText;

  private String requestMethod;
  private Map<String, String> requestProperties;

  /**
   * Construct with the given url string.
   */
  public UrlData(String urlString) {
    this(urlString, null, null);
  }

  /**
   * Construct with the given url string and referring info.
   */
  public UrlData(String urlString, String referringUrl, String referringLinkText) {
    this.urlString = urlString;
    this.referringUrl = referringUrl;
    this.referringLinkText = referringLinkText;

    this._url = null;
    this._error = null;
    this._dUrl = null;
    this._cleanString = null;
    this.crawlDelay = null;
    this.forceRefreshTime = null;

    this.requestMethod = null;
    this.requestProperties = null;
  }

  /**
   * Construct with the given url.
   */
  public UrlData(URL url) {
    this.urlString = url == null ? null : url.toString();
    this._url = url;

    this._error = null;
    this._dUrl = null;
    this._cleanString = this.urlString;
    this.crawlDelay = null;
    this.forceRefreshTime = null;

    this.referringUrl = null;
    this.referringLinkText = null;

    this.requestMethod = null;
    this.requestProperties = null;
  }

  /**
   * Set this instance's referring url.
   */
  public void setReferringUrl(String referringUrl) {
    this.referringUrl = referringUrl;
  }

  /**
   * Get this instance's referring url.
   */
  public String getReferringUrl() {
    return referringUrl;
  }

  /**
   * Set this instance's referring link text.
   */
  public void setReferringLinkText(String referringLinkText) {
    this.referringLinkText = referringLinkText;
  }

  /**
   * Get this instance's referring link text.
   */
  public String getReferringLinkText() {
    return referringLinkText;
  }

  public boolean hasRequestMethod() {
    return requestMethod != null && !"".equals(requestMethod);
  }

  public String getRequestMethod() {
    return requestMethod;
  }

  public void setRequestMethod(String requestMethod) {
    this.requestMethod = requestMethod;
  }

  public boolean hasRequestProperties() {
    return requestProperties != null && requestProperties.size() > 0;
  }

  public void setRequestProperty(String property, String value) {
    if (requestProperties == null) requestProperties = new LinkedHashMap<String, String>();
    requestProperties.put(property, value);
  }

  public Map<String, String> getRequestProperties() {
    return requestProperties;
  }

  /**
   * Get the url string.
   */
  public String getUrlString() {
    return urlString;
  }

  /**
   * Get the url.
   */
  public URL getUrl() {
    if (_url == null && _error == null) {
      final DetailedUrl dUrl = getDetailedUrl();
      if (dUrl != null) {
        _url = dUrl.asUrl();
        if (_url == null) {
          _error = dUrl.getUrlError();
        }
      }
    }
    return _url;
  }

  /**
   * Get the error in constructing the url instance.
   */
  public Exception getError() {
    getUrl();  // force computation
    return _error;
  }

  /**
   * Get the detailed url instance for this instance.
   */
  public DetailedUrl getDetailedUrl() {
    if (_dUrl == null && urlString != null) {
      this._dUrl = new DetailedUrl(urlString);
    }
    return _dUrl;
  }

  /**
   * Get the clean (fully normalized) url string.
   */
  public String getCleanString() {
    if (_cleanString == null) {
      final DetailedUrl dUrl = getDetailedUrl();
      this._cleanString = new PathWrapper(dUrl.getNormalizedUrlNoAnchor()).getPath();
    }
    return _cleanString;
  }

  /**
   * Set the value of crawlDelay for overriding the settings.
   *
   * @param crawlDelay  the value to use instead of that from the settings;
   *                    if null, then this will not override the settings.
   */
  public void setOverrideCrawlDelay(Long crawlDelay) {
    this.crawlDelay = crawlDelay;
  }

  /**
   * Get the value of the crawlDelay override for this instance.
   * <p>
   * If null, then there is no override.
   */
  public Long getOverrideCrawlDelay() {
    return crawlDelay;
  }

  /**
   * Set the value of the forceRefreshTime for overriding the settings.
   * <p>
   * @param forceRefreshTime  the value to use instead of that from the settings;
   *                          if null, then this will not override the settings.
   */
  public void setOverrideForceRefreshTime(Long forceRefreshTime) {
    this.forceRefreshTime = forceRefreshTime;
  }

  /**
   * Get the value of the forceRefreshTime override for this instance.
   * <p>
   * If null, then there is no override.
   */
  public Long getOverrideForceRefreshTime() {
    return forceRefreshTime;
  }

  public String toString() {
    return getCleanString();
  }
}
