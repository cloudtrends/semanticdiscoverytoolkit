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


import java.util.Arrays;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

/**
 * 
 * <p>
 * @author Spence Koehler
 */
public class HtmlLinkFollower implements LinkFollower {
  
  // see http://en.wikipedia.org/wiki/List_of_file_formats

  public static final String HTML_STATIC_EXTENSIONS = ".html,.htm,.xhtml,.xht,.xml,.mht,.mhtml";
  public static final String HTML_DYNAMIC_EXTENSIONS = ".asp,.aspx,.adp,.bml,.cfm,.cgi,.ihtml,.jsp,.las,.lasso,.lassoapp,.pl,.php,.phtml,.shtml";
  public static final String HTML_EXTENSIONS = HTML_STATIC_EXTENSIONS + "," + HTML_DYNAMIC_EXTENSIONS;

//todo: complete these lists
  public static final String NON_HTML_DOC_EXTENSIONS = ".css,.js,.txt,.csv,.pdf,.doc,.wpd,.odm,.odt,.ott";
  public static final String MULTIMEDIA_EXTENSIONS = ".jpg,.png,.gif,.wav,.mp3,.swf,.ra,.rm,.mpg,.mpeg,.jfif,.mp3,.mp4,.pls,.id3,.ape,.tif,.tiff,.jpeg,.bmp";
  public static final String ARCHIVE_EXTENSIONS = ".zip,.tgz,.tar,.gz,.bz2,.gzip,.jar,.arc";

  public static final String DEFAULT_FETCH_EXTS = ".css";
  public static final String DEFAULT_FOLLOW_EXTS = HTML_EXTENSIONS;
  public static final String DEFAULT_IGNORE_EXTS = MULTIMEDIA_EXTENSIONS + "," + ARCHIVE_EXTENSIONS;


  private int maxDepth;
  private int maxPages;
  private Set<String> fetchExtensions;
  private Set<String> followExtensions;
  private Set<String> ignoreExtensions;
  private boolean localOnly;
  private boolean deeperOnly;

  /**
   * Properties:
   * <ul>
   * <li>maxDepth -- maximum distance from starting page to crawl, where 0 will
   *                 retrieve only the starting page, 1 will retrieve the
   *                 starting page and all pages linked from it, etc. If -1
   *                 (which is the default,) then there is no depth limit.</li>
   * <li>maxPages -- maximum number of pages to retrieve, including the starting
   *                 page. If -1, then there is no limit (which is the default.)
   * <li>fetchExts -- comma delimitted list of extensions to fetch, not including
   *                  those to be followed.
   *                  default=".css,.txt,.pdf"</li>
   * <li>followExts -- comma delimitted list of extensions to follow, not including
   *                   those to be fetched.
   *                   default=".html,.htm,.xhtml,.xhtm"</li>
   * <li>ignoreExts -- comma delimitted list of extensions to ignore.
   *                   default=".jpg,.png,.gif,.wav,.mp3,.swf"</li>
   * <li>localOnly -- "true" (default="true") if we are to only follow on-site links.</li>
   * <li>deeperOnly -- "true" (default="true") if, when we have an on-site link, we are to
   *                    only follow it if it is "deeper" than the original.</li>
   * </ul>
   */
  public HtmlLinkFollower(Properties properties) {
    this.maxDepth = Integer.parseInt(properties.getProperty("maxDepth", "-1"));
    this.maxPages = Integer.parseInt(properties.getProperty("maxPages", "-1"));

    final String fetchExts = properties.getProperty("fetchExts", DEFAULT_FETCH_EXTS);
    final String followExts = properties.getProperty("followExts", DEFAULT_FOLLOW_EXTS);
    final String ignoreExts = properties.getProperty("ignoreExts", DEFAULT_IGNORE_EXTS);

    this.fetchExtensions = buildExtensionSet(fetchExts);
    this.followExtensions = buildExtensionSet(followExts);
    this.ignoreExtensions = buildExtensionSet(ignoreExts);
    
    this.localOnly = "true".equals(properties.getProperty("localOnly", "true"));
    this.deeperOnly = "true".equals(properties.getProperty("deeperOnly", "true"));
  }


  /**
   * Determine whether to fetch the given link.
   * <p>
   * Note that some links will only be fetched and not followed.
   *
   * @return true to fetch the destination url of the given link.
   */
  public boolean shouldFetchLink(SiteContext siteContext, SiteData siteData, Link link) {

    if (link.isLocal()) {
      // only fetch links that go "deeper" than the start
      if (deeperOnly && !isDeeper(siteContext.getStartUrl(), link)) return false;
    }
    else if (localOnly) {
      // only fetch local links
      return false;
    }

    final String ext = link.getDestExt();

    boolean result = fetchExtensions == null || fetchExtensions.contains(ext);

    if (!result) {
      result = shouldFollowLink(siteContext, siteData, link) != null;
    }

    return result;
  }

  /**
   * Determine whether to follow links from the target of the given link.
   * <p>
   * Given that the link has been fetched, determine whether its links should
   * be followed.
   *
   * @return null to NOT follow the links on the destination url of the given link;
   *         otherwise, return a NORMAL priority to follow in normal breadth-first
   *         fashion or HIGH priority to follow the link sooner.
   */
  public Priority shouldFollowLink(SiteContext siteContext, SiteData siteData, Link link) {
    final String ext = link.getDestExt();

    boolean result =
      (maxPages < 0 || siteContext.getNumVisited() < maxPages) &&
      (maxDepth < 0 || ((siteData.getCrawlDepth() + link.getDepthInc()) <= maxDepth)) &&
      (followExtensions == null || followExtensions.contains(ext)) &&
      (ignoreExtensions == null || !ignoreExtensions.contains(ext));

//todo: add logic to follow i.e. "product" links sooner.

    return result ? Priority.NORMAL : null;
  }

  /**
   * Given that the link is local (relative) to top, determine whether it is
   * "deeper".
   */
  protected boolean isDeeper(UrlData top, Link link) {
    final String topPath = top.getDetailedUrl().getPath(true);
    final String linkPath = link.getDestUrl().getDetailedUrl().getPath(true);

    return linkPath.startsWith(topPath);
  }

  private final Set<String> buildExtensionSet(String string) {
    return string == null || "".equals(string) ? null : new HashSet<String>(Arrays.asList(string.toLowerCase().split("\\s*,\\s*")));
  }
}
