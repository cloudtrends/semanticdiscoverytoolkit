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

/**
 * Container for link information found on a page.
 * <p>
 * @author Spence Koehler
 */
public class Link {
  
  private String sourceUrl;
  private String tagName;
  private int depthInc;  // frames don't increment the depth.
  private boolean isLocal;
  private String linkUrl;
  private String linkText;

  private UrlData _destUrl;
  private DetailedUrl _dUrl;
  private String _ext;

  /**
   * Construct with the given context.
   */
  public Link(String sourceUrl, String tagName, String href, String linkText, int depthInc) {
    this.sourceUrl = sourceUrl;
    this.tagName = tagName;
    this.depthInc = depthInc;
    this.isLocal = (href.indexOf(':') < 0) || matchesHost(sourceUrl, href);
    this.linkUrl = href;
    this.linkText = linkText;
  }

  /**
   * Get the source url for this link.
   */
  public String getSourceUrl() {
    return sourceUrl;
  }

  /**
   * Get the name of the tag that held this link in its source.
   */
  public String getTagName() {
    return tagName;
  }

  /**
   * Get this link's depth increment.
   */
  public int getDepthInc() {
    return depthInc;
  }

  /**
   * Determine whether the link is local.
   */
  public boolean isLocal() {
    return isLocal;
  }

  /**
   * Get this link's (original) link url.
   * <p>
   * Note: if local, then protocol and domain will be missing.
   */
  public String getLinkUrl() {
    return linkUrl;
  }

  /**
   * Get the text accompanying this link.
   */
  public String getLinkText() {
    return linkText;
  }

  /**
   * Get the full link destination url (suitable for crawling) for this link.
   */
  public UrlData getDestUrl() {
    if (_destUrl == null) {
      if (isLocal && linkUrl.indexOf(':') < 0) {
        final String destUrlString = buildUrl(sourceUrl, linkUrl);
        _destUrl = new UrlData(destUrlString);
      }
      else {
        _destUrl = new UrlData(linkUrl);
      }
    }

    return _destUrl;
  }

  public DetailedUrl getDestDetailedUrl() {
    if (_dUrl == null) {
      _dUrl = getDestUrl().getDetailedUrl();
    }
    return _dUrl;
  }

  /**
   * Get the extension of the destination.
   * <p>
   * NOTE: the result will include the "." (i.e. ".html") and could be empty (as in "http://www.google.com").
   */
  public String getDestExt() {
    if (_ext == null) {
      final DetailedUrl dUrl = getDestDetailedUrl();
      final String ext = dUrl.getTargetExtension(true);
      _ext = (ext != null) ? ext.toLowerCase() : "";
    }

    return _ext;
  }

  public String toString() {
    final StringBuilder result = new StringBuilder();

    result.
      append("dest=").
      append(getDestUrl().toString()).
      append(" (tag=").
      append(tagName).
      append(", local=").
      append(isLocal).
      append(", depth=").
      append(depthInc).
      append(", text=").
      append(linkText).
      append(")");

    return result.toString();
  }

  /**
   * Build a full url for the relative url wrt the source url.
   */
  public static final String buildUrl(String sourceUrl, String relativeUrl) {
    if (sourceUrl == null) return relativeUrl;

    final StringBuilder result = new StringBuilder();

    final int cssPos = sourceUrl.indexOf("://");
    final int spos = sourceUrl.lastIndexOf('/');
    final String baseUrl = (spos == cssPos + 2) ? sourceUrl + "/" : sourceUrl.substring(0, spos + 1);
    result.append(baseUrl).append(relativeUrl);

    return result.toString();
  }

  /**
   * Determine whether the other url matches the host.
   */
  public static final boolean matchesHost(String hostUrl, String otherUrl) {
    if (hostUrl == null || otherUrl == null) return false;

    final DetailedUrl dHostUrl = new DetailedUrl(hostUrl);
    final DetailedUrl dOtherUrl = new DetailedUrl(otherUrl);

    return dHostUrl.getHost(true, false, false).equals(dOtherUrl.getHost(true, false, false));
  }
}
