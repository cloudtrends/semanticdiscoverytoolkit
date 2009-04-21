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


import org.sd.util.PropertiesParser;
import org.sd.util.ReflectUtil;
import org.sd.util.StringUtil;
import org.sd.util.tree.TraversalIterator;
import org.sd.util.tree.Tree;
import org.sd.util.tree.TreeUtil;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.Set;

/**
 * Engine for crawling a site using a page crawler.
 * <p>
 * @author Spence Koehler
 */
public class SiteCrawler {


  public static final String DEFAULT_USER_AGENT = "sdtoolkit/1.0 (http://code.google.com/p/sdtoolkit/wiki/Robots)";


  private PageCrawler pageCrawler;
  private LinkFollower linkFollower;

  /**
   * Construct a page crawler and this site crawler using the given properties.
   */
  public SiteCrawler(Properties properties) {
    this(new PageCrawler(properties), properties);
  }
  
  /**
   * Construct with the given page crawler using the given properties.
   * <p>
   * Properties:
   * <ul>
   * <li>linkFollower -- string for constructing the link follower instance to use
   *                     for accepting links to follow. Default is an
   *                     HtmlLinkFollower. Note that the properties are also
   *                     used in the link follower's construction.
   * </ul>
   */
  public SiteCrawler(PageCrawler pageCrawler, Properties properties) {
    this.pageCrawler = pageCrawler;

    // Set the default user agent for site crawling if unspecified.
    if (properties.getProperty("userAgent") == null) {
      this.pageCrawler.getCrawlSettings().setUserAgent(DEFAULT_USER_AGENT);
    }

    final String linkFollowerString = properties.getProperty("linkFollower", "org.sd.crawl.HtmlLinkFollower");
    this.linkFollower = (LinkFollower)ReflectUtil.buildInstance(linkFollowerString, properties);
  }

  public Tree<SiteData> crawl(String startingUrl) {
    final UrlData startUrl = new UrlData(startingUrl);
    final SiteData rootData = new SiteData(startUrl);
    final Tree<SiteData> result = new Tree<SiteData>(rootData);
    final SiteContext siteContext = new SiteContext(startUrl);

    // override the pageCrawler's cacheDir, adding a level identifying the site being crawled.
    final CrawlSettings crawlSettings = new CrawlSettings(pageCrawler.getCrawlSettings());
    crawlSettings.setCacheDir(buildSiteCacheDir(startUrl, crawlSettings.getCacheDir()));

    // always fetch the root page
    final CrawledPage rootPage = pageCrawler.fetch(startUrl, crawlSettings);
    final SiteInfo siteInfo = followLinks(result, rootPage, siteContext, crawlSettings);

    // close the CrawlCache associated with the (temporary/overriding) cacheDir
    crawlSettings.closeCrawlCache();

//todo: either write out a sitegraph file while crawling or have a utility to reconstruct (or both/parameterize)?
//todo: return combination of siteInfo and result.
    return result;
  }

  /**
   * Build the directory for the site's cache dir.
   */
  protected File buildSiteCacheDir(UrlData startUrl, File baseDir) {
    final String dirname = StringUtil.urlQueryEscape(CrawlUtil.normalize(startUrl.getDetailedUrl()));
    return new File(baseDir, dirname);
  }

  /**
   * Follow the links off the given page, building the siteTree.
   */
  private final SiteInfo followLinks(Tree<SiteData> siteTree, CrawledPage curPage, SiteContext siteContext, CrawlSettings crawlSettings) {
    final SiteInfo siteInfo = SiteInfo.buildSiteInfo();

    if (curPage == null) return siteInfo;

    SiteData siteData = siteTree.getData();
    siteData.setLinks(curPage.getLinks());
    siteData.setPageMetaData(curPage.getMetaData());
    siteInfo.addFetchedPage(curPage, siteData.getCrawlDepth(), null);

    final LinkedList<Tree<SiteData>> queue = new LinkedList<Tree<SiteData>>();
    queue.add(siteTree);

    while (queue.size() > 0) {
      final Tree<SiteData> node = queue.removeFirst();
      siteData = node.getData();
      final List<Link> links = siteData.getLinks();

      if (links == null) continue;

      for (Link link : links) {
        if (linkFollower.shouldFetchLink(siteContext, siteData, link)) {
          final CrawledPage nextPage = fetchLink(link, crawlSettings);
          if (nextPage != null && !nextPage.hasError() && nextPage.hasContent() && nextPage.getResponseCode() < 300) {
            final SiteData nextData = siteData.buildNext(link, nextPage.getMetaData());
            final Tree<SiteData> child = node.addChild(nextData);

            final UrlData destUrl = link.getDestUrl();
            if (siteContext.addVisited(destUrl.getCleanString())) {
              siteInfo.addFetchedPage(nextPage, nextData.getCrawlDepth(), destUrl.getDetailedUrl());

              final LinkFollower.Priority priority = linkFollower.shouldFollowLink(siteContext, siteData, link);
              if (priority != null) {
                nextData.setLinks(nextPage.getLinks());
                switch (priority) {
                  case NORMAL :
                    queue.addLast(child);   // add last for breadth first crawling
                    break;
                  case HIGH :
                    queue.addFirst(child);  // push to front of queue for prioritized crawling
                    break;
                }
              }
              else {
                siteInfo.addUnfollowedLinks(nextPage.getLinks(), siteContext);
              }
            }
            else {
              siteInfo.addRevisitedPage(nextPage, nextData.getCrawlDepth(), destUrl.getDetailedUrl());
            }
          }
          else {
            siteInfo.addFailedLink(link, nextPage);
          }
        }
        else {
          siteInfo.addIgnoredLink(link);
        }
      }
    }

    return siteInfo;
  }

  private final CrawledPage fetchLink(Link link, CrawlSettings crawlSettings) {
    final CrawledPage result = pageCrawler.fetch(link.getDestUrl(), crawlSettings);

    // need to update page in crawl cache with referring info
    result.setReferringInfo(link.getSourceUrl(), link.getLinkText());
    pageCrawler.updateCache(result, crawlSettings, true, false);

    return result;
  }


  public static final void uniquify(Tree<SiteData> siteTree) {
    final Set<String> urls = new HashSet<String>();
    for (TraversalIterator<SiteData> iter = siteTree.iterator(Tree.Traversal.BREADTH_FIRST); iter.hasNext(); ) {
      final Tree<SiteData> curNode = iter.next();
      final SiteData siteData = curNode.getData();
      final String url = siteData.getUrlData().getCleanString();
      if (urls.contains(url)) {
        if (!curNode.hasChildren()) curNode.prune(true, true);
      }
      else urls.add(url);
    }
  }

  public static final void main(String[] args) throws IOException {
    //
    // Properties:
    //
    // mode -- (optional, default=null) High-level mode to preconfigure
    //         default settings wherever none are specified.
    //          recognized modes are:
    //           "corpinfo" -- to use corpinfo crawler default settings
    //           "complete" -- to crawl everything
    //           "alldoc" -- all non-multimedia documents
    //           ...todo... add more modes as needed
    //
    // cacheDir -- (base) directory for crawl caches
    // linkFollower -- (optional, default=HtmlLinkFollower)
    //   maxDepth -- (optional, default=-1 for unlimited, startingPageOnly==0)
    //   maxPages -- (optional, default=-1 for unlimited)
    //   fetchExts -- (optional, default=".css,.txt,.pdf")
    //   followExts -- (optional, default=".html,.htm,.xhtml,.xhtm,.php,.js,.asp")
    //   ignoreExts -- (optional, default=".jpg,.png,.gif,.wav,.mp3,.swf")
    //   localOnly -- (optional, default="true")
    //   deeperOnly -- (optional, default="true")
    //
    // PageCrawlerProperties
    //   connectTimeout -- (optional, default="4500" millis)
    //   readTimeout -- (optional, default="4500" millis)
    //   numRetries -- (optional, default="3")
    //   forceRefreshTime -- (optional, default="0" for always use cached item)
    //   allowRedirects -- (optional, default="true")
    //   skipContent -- (optional, default="false")
    //   verbose -- (optional, default="false")
    //
    // args
    //   urls to crawl
    //

    final PropertiesParser pp = new PropertiesParser(args);
    final Properties properties = pp.getProperties();
    args = pp.getArgs();

    final String mode = properties.getProperty("mode");
    if ("corpinfo".equals(mode)) {
      if (properties.getProperty("fetchExts") == null) properties.setProperty("fetchExts", ".css");
      if (properties.getProperty("followExts") == null) properties.setProperty("followExts", HtmlLinkFollower.HTML_EXTENSIONS);
//      if (properties.getProperty("verbose") == null) properties.setProperty("verbose", "true");
    }
    else if ("complete".equals(mode)) {
      if (properties.getProperty("fetchExts") == null) properties.setProperty("fetchExts", "");
      if (properties.getProperty("followExts") == null) properties.setProperty("followExts", "");
      if (properties.getProperty("ignoreExts") == null) properties.setProperty("ignoreExts", "");
      if (properties.getProperty("maxDepth") == null) properties.setProperty("maxDepth", "-1");
      if (properties.getProperty("maxPages") == null) properties.setProperty("maxPages", "-1");
    }
    else if ("alldoc".equals(mode)) {  // all non-multimedia documents
      if (properties.getProperty("fetchExts") == null) properties.setProperty("fetchExts", "");
      if (properties.getProperty("followExts") == null) properties.setProperty("followExts", "");
      // use default ignore exts, which ignores multimedia
      if (properties.getProperty("maxDepth") == null) properties.setProperty("maxDepth", "-1");
      if (properties.getProperty("maxPages") == null) properties.setProperty("maxPages", "-1");
    }

    System.out.println("properties=" + properties);

    final SiteCrawler siteCrawler = new SiteCrawler(properties);

    for (String arg : args) {
      System.out.println();
      System.out.println(new Date() + ": SiteCrawler crawling '" + arg + "'...");
      final Tree<SiteData> siteData = siteCrawler.crawl(arg);
      uniquify(siteData);

      System.out.println(TreeUtil.prettyPrint(siteData, "", false));

      System.out.println(new Date() + ": SiteCrawler crawled '" + arg + "'.");
    }
  }
}
