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


import org.sd.cio.MessageHelper;
import org.sd.io.FileUtil;
import org.sd.io.Publishable;
import org.sd.text.DetailedUrl;
import org.sd.util.LineBuilder;
import org.sd.util.PropertiesParser;
import org.sd.util.StringUtil;
import org.sd.util.tree.Tree;
import org.sd.xml.XPathApplicator;
import org.sd.xml.XmlFactory;
import org.sd.xml.XmlLite;
import org.sd.xml.XmlTreeHelper;

import java.io.BufferedReader;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Container for settings for a search engine.
 * <p>
 * @author Spence Koehler
 */
public class SearchEngineSettings implements HitScraper {
  
  private static Map<String, SearchEngineSettings> DEFAULT_SE_SETTINGS = null;
  private static final Object LOAD_MUTEX = new Object();

  /**
   * Get default search engine settings map.
   */
  public static final Map<String, SearchEngineSettings> getDefaultSettings() throws IOException {
    synchronized (LOAD_MUTEX) {
      if (DEFAULT_SE_SETTINGS == null) {
        final File seSettingsFile = new File(getRootDir() + "resources/settings/default_search_engine_settings.txt");
        if (seSettingsFile.exists()) {
          DEFAULT_SE_SETTINGS = SearchEngineSettings.loadSettings(seSettingsFile);
        }
      }
    }
    return DEFAULT_SE_SETTINGS;
  }

  private static final String getRootDir() {
    final String path = FileUtil.getFilename(SearchEngineSettings.class, ".");
    final int pos = path.indexOf("/crawl/");
    return (pos >= 0) ? path.substring(0, pos + 7) : null;
  }

  /**
   * Get default search engine settings for the given name (e.g. "google", "google-blog", ...).
   * <p>
   * See the default settings file at resources/settings/default_search_engine_settings.txt
   */
  public static final SearchEngineSettings getDefaultSettings(String name) throws IOException {
    final Map<String, SearchEngineSettings> name2settings = getDefaultSettings();
    return name2settings.get(name);
  }


  /**
   * Load a search engine settings file with fields of the form:
   *
   * name | queryPrefix | queryPostfix | resultLinkKey | nextResultsPageKey
   *
   * Where
   * <ul>
   * <li>name -- is the name and key for the setting.</li>
   * <li>queryPrefix -- is the constant portion of a query url before a query's text.</li>
   * <li>queryPostfix -- is the constant portion of a query url after a query's text.</li>
   * <li>resultLinkKey -- is the (sd) xPath to identify result (hit) links from the search engine.</li>
   * <li>nextResutlsPageKey -- is the (sd) xPath to identify getting the "next" page of results from the search engine.</li>
   * <li>lowContentBrNum -- is the number of the br node in the content at which the body text starts.</li>
   * <li>highContentBrNum -- is the number of the br node in the content at which the body text ends.</li>
   * </ul>
   *
   * Note that blank lines and lines beginning with '#' are ignored.
   */
  public static final Map<String, SearchEngineSettings> loadSettings(File settingsFile) throws IOException {
    final Map<String, SearchEngineSettings> result = new HashMap<String, SearchEngineSettings>();

    final BufferedReader reader = FileUtil.getReader(settingsFile);
    String line = null;
    while ((line = reader.readLine()) != null) {
      if (!"".equals(line) && line.charAt(0) != '#') {
        final SearchEngineSettings settings = new SearchEngineSettings(line);
        result.put(settings.name, settings);
      }
    }
    reader.close();

    return result;
  }

//  public static final SearchEngineSettings GOOGLE_SETTINGS = new SearchEngineSettings(
//    "google|http://www.google.com/search?hl=en&q=|&btnG=Search|**.body.div.div.ol.li.h3.a@href|**.body.table.tr.td.td.td.td.td.td.td.td.td.td.td.td.a@href");


  public final String name;
  public final String queryPrefix;
  public final String queryPostfix;
  public final String resultLinkKey;
  public final String nextResultsPageKey;
  public final int lowContentBrNum;
  public final int highContentBrNum;

  private XPathApplicator xpathApplicator;
  private String protocolAndHost;

  /**
   * Load settings from a line of the form:
   *   name | queryPrefix | queryPostfix | resultLinkKey | nextResultsPageKey
   */
  public SearchEngineSettings(String settingsLine) {
    final String[] fields = StringUtil.splitFields(settingsLine, 7);

    this.name = fields[0];
    this.queryPrefix = fields[1];
    this.queryPostfix = fields[2];
    this.resultLinkKey = fields[3];
    this.nextResultsPageKey = fields[4];
    this.lowContentBrNum = Integer.parseInt(fields[5]);
    this.highContentBrNum = Integer.parseInt(fields[6]);

    this.xpathApplicator = new XPathApplicator();
    this.protocolAndHost = null;  // protocol + host
  }

//   public Tree<XmlLite.Data> getXmlTree(InputStream inputStream) throws IOException {
//     return XmlFactory.readXmlTree(inputStream, Encoding.UTF8, true/*ignoreComments*/,  true/*htmlFlag*/, null, false);
//   }

  public List<String> getHitUrls(Tree<XmlLite.Data> xmlTree) {
    return xpathApplicator.getText(resultLinkKey, xmlTree, true/*all*/);
  }

  public List<Tree<XmlLite.Data>> getHitLinkNodes(Tree<XmlLite.Data> xmlTree) {
    return xpathApplicator.getNodes(resultLinkKey, xmlTree);
  }

  public List<HitSnippet> getHitSnippets(Tree<XmlLite.Data> xmlTree) {
    final List<Tree<XmlLite.Data>> linkNodes = getHitLinkNodes(xmlTree);
    return getHitSnippets(linkNodes);
  }

  public List<HitSnippet> getHitSnippets(List<Tree<XmlLite.Data>> linkNodes) {
    List<HitSnippet> result = null;

    if (linkNodes != null) {
      result = new ArrayList<HitSnippet>();
      for (Tree<XmlLite.Data> linkNode : linkNodes) {
        final Tree<XmlLite.Data> contentNode = XmlTreeHelper.getNextNode(linkNode);
        result.add(new HitSnippet(linkNode, contentNode, lowContentBrNum, highContentBrNum));
      }
    }

    return result;
  }

  public String getNextResultsPageUrl(Tree<XmlLite.Data> xmlTree) {
    String result = null;

    final List<String> nextLinks = xpathApplicator.getText(nextResultsPageKey, xmlTree, false/*all*/);
    if (nextLinks != null && nextLinks.size() > 0) {
      result = nextLinks.get(0);
      if (result.startsWith("/")) {
        result = getHostPrefix() + result;
      }
    }

    return result;
  }

  private final String getHostPrefix() {
    if (protocolAndHost == null) {
      final DetailedUrl dUrl = new DetailedUrl(queryPrefix);
      protocolAndHost = dUrl.getProtocol(true) + dUrl.getHost(true, true, true);
    }
    return protocolAndHost;
  }


  /**
   * Submit a search to this instance's search engine and scrape up to numHits
   * inks from the results using the given crawler.
   */
  public QueryPage submitQuery(String query, int numHits, PageCrawler pageCrawler) {

    String queryUrl = CrawlUtil.buildQueryUrl(queryPrefix, query, queryPostfix);
    final QueryPage result = new QueryPage(query, queryUrl, numHits, pageCrawler);

    while (true) {
      final CrawledPage queryResultsPage = pageCrawler.fetch(queryUrl);
      queryUrl = result.incorporateCrawledPage(queryResultsPage, this);
      if (queryUrl == null) break;
    }

    return result;
  }

  public QueryPage loadQueryPage(CrawledPage crawledPage, int maxHits) {
    final QueryPage result = new QueryPage(maxHits);
    result.incorporateCrawledPage(crawledPage, this);
    return result;
  }


  /**
   * Container class to hold the snippet of a hit from a query.
   */
  public static final class HitSnippet implements Publishable {
    private Tree<XmlLite.Data> linkNode;
    private Tree<XmlLite.Data> contentNode;
    private int lowBrNum;
    private int highBrNum;

    
    /**
     * Default constructor for publishable reconstruction only.
     */
    public HitSnippet() {
    }

    /**
     * 
     */
    public HitSnippet(String hitSnippet) {
    }

    // body in content is between br tags encountered from lowBrNum until highBrNum (0-based)
    HitSnippet(Tree<XmlLite.Data> linkNode, Tree<XmlLite.Data> contentNode, int lowBrNum, int highBrNum) {
      this.linkNode = linkNode;
      this.contentNode = contentNode;
      this.lowBrNum = lowBrNum;
      this.highBrNum = highBrNum;
    }

    /**
     * Get the url referenced by this hit.
     */
    public String getLinkUrl() {
      return XmlTreeHelper.getAttribute(linkNode, "href");
    }

    /**
     * Get the link text referenced by this hit.
     */
    public String getLinkText() {
      return XmlTreeHelper.getAllText(linkNode);
    }

    /**
     * Get the snippet referenced by this hit.
     *
     * @param bodyOnly  if true, then exclude footer text.
     */
    public String getContent(boolean bodyOnly) {
      final StringBuilder result = new StringBuilder();

      if (contentNode != null) {
        int brNum = 0;
        for (Iterator<Tree<XmlLite.Data>> iter = contentNode.iterator(Tree.Traversal.DEPTH_FIRST); iter.hasNext(); ) {
          final Tree<XmlLite.Data> curNode = iter.next();
          if (bodyOnly && "br".equals(XmlTreeHelper.getTagName(curNode))) {
            ++brNum;
            if (brNum >= highBrNum) break;
          }
          if (bodyOnly && brNum < lowBrNum) continue;
          final XmlLite.Text nodeText = curNode.getData().asText();
          if (nodeText != null && nodeText.text != null && nodeText.text.length() > 0) {
            if (result.length() > 0) result.append(' ');
            result.append(nodeText.text);
          }
        }
      }

      return result.toString();
    }

    public String toString() {
      final LineBuilder result = new LineBuilder();

      result.
        appendUrl(getLinkUrl()).
        append(getLinkText()).
        append(getContent(true));

      return result.toString();
    }

    /**
     * Write this message to the dataOutput stream such that this message
     * can be completely reconstructed through this.read(dataInput).
     *
     * @param dataOutput  the data output to write to.
     */
    public void write(DataOutput dataOutput) throws IOException {
      MessageHelper.writeXmlTree(dataOutput, linkNode, true);
      MessageHelper.writeXmlTree(dataOutput, contentNode, true);
      dataOutput.writeInt(lowBrNum);
      dataOutput.writeInt(highBrNum);
    }

    /**
     * Read this message's contents from the dataInput stream that was written by
     * this.write(dataOutput).
     * <p>
     * NOTE: this requires all implementing classes to have a default constructor
     *       with no args.
     *
     * @param dataInput  the data output to write to.
     */
    public void read(DataInput dataInput) throws IOException {
      this.linkNode = MessageHelper.readXmlTree(dataInput);
      this.contentNode = MessageHelper.readXmlTree(dataInput);
      this.lowBrNum = dataInput.readInt();
      this.highBrNum = dataInput.readInt();
    }
  }


  public static final void main(String[] args) throws IOException {
    // properties
    //
    //   settingsFile -- (required) path to settings file
    //   settingName -- (required) name of setting to use
    //
    //   htmlFile -- htmlFile to apply hit extraction to
    //   query -- query text to submit and apply hit extraction to
    //   cacheDir -- crawl cache directory path to use if submitting queryString instead of htmlFile
    //   numHits -- (default=10) number of hits to scrape (note: this main will only process first page)
    //

    final PropertiesParser pp = new PropertiesParser(args);
    final Properties properties = pp.getProperties();

    final String settingsFilePath = properties.getProperty("settingsFile");
    if (settingsFilePath == null) {
      throw new IllegalArgumentException("Must define 'settingsFile' property!");
    }
    final String settingName = properties.getProperty("settingName");
    if (settingName == null) {
      throw new IllegalArgumentException("Must define 'settingName' property!");
    }

    final Map<String, SearchEngineSettings> seSettings = SearchEngineSettings.loadSettings(new File(settingsFilePath));
    final SearchEngineSettings seSetting = seSettings.get(settingName);

    final int numHits = Integer.parseInt(properties.getProperty("numHits", "10"));
    
    Tree<XmlLite.Data> xmlTree = null;

    final String htmlFileString = properties.getProperty("htmlFile");
    if (htmlFileString != null) {
      xmlTree = XmlFactory.readXmlTree(new File(htmlFileString), true, true, false);
    }
    else {
      final String query = properties.getProperty("query");
      final PageCrawler pageCrawler = new PageCrawler(properties);
      final QueryPage queryPage = seSetting.submitQuery(query, numHits, pageCrawler);
      if (queryPage != null) {
        final CrawledPage crawledPage = queryPage.getQueryResultsPage();
        xmlTree = crawledPage.getXmlTree();
      }
    }

    if (xmlTree != null) {
      final List<HitSnippet> snippets = seSetting.getHitSnippets(xmlTree);

      if (snippets != null) {
        int index = 0;
        for (HitSnippet snippet : snippets) {
          System.out.println(index + ": " + snippet);
          ++index;
        }
      }
    }
  }
}
