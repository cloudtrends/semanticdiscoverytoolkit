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
import org.sd.util.tree.Tree;
import org.sd.xml.XmlLite;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * A crawled page processor that applies other processors to query result
 * pages from search engine hits.
 * <p>
 * @author Spence Koehler
 */
public class SearchEngineProcessor extends CrawledPageRipperProcessor {
  
  private List<CrawledPageRipperProcessor> processors;

  private PageCrawler pageCrawler;
  private List<String> queryStrings;
  private List<String> hitUrls;
  private List<SearchEngineSettings.HitSnippet> hitSnippets;
  private Map<String, SearchEngineSettings> name2settings;
  private String[] seNames;

  private List<ProcessorResult> results;

  /**
   * Properties:
   * <ul>
   * <li>processors -- comma-delimited list of classpaths to crawled page ripper processors (with properties constructors) to apply.</li>
   * <li>processorN (where N=1,2,3,...) -- claspaths to crawled page ripper processors (with properties constructors) to apply.</li>
   * <li>cacheDir -- (optional, default=null) directory for crawled page cache.</li>
   * <li>seSettings -- (optional, default=default cluster settings) search engine settings file.</li>
   * <li>seNames -- (optional, default=all se settings) comma-delimited list of names from seSettings to apply.</li>
   * </ul>
   * NOTE: The properties are passed to all processors instances on construction.
   */
  public SearchEngineProcessor(Properties properties) throws IOException {
    results = null;

    // initialize processors
    this.processors = new ArrayList<CrawledPageRipperProcessor>();

    final String processorsString = properties.getProperty("processors");
    if (processorsString != null) {
      final String[] pieces = processorsString.split(",");
      for (String piece : pieces) {
        final CrawledPageRipperProcessor processor = (CrawledPageRipperProcessor)ReflectUtil.buildInstance(piece, properties);
        processors.add(processor);
      }
    }

    int i = 1;
    while (true) {
      final String processorClass = properties.getProperty("processor" + i);
      if (processorClass == null) break;

      final CrawledPageRipperProcessor processor = (CrawledPageRipperProcessor)ReflectUtil.buildInstance(processorClass, properties);
      processors.add(processor);

      ++i;
    }

    // initialize vars
    this.pageCrawler = new PageCrawler(properties);
    this.queryStrings = new ArrayList<String>();
    this.hitUrls = new ArrayList<String>();
    this.hitSnippets = new ArrayList<SearchEngineSettings.HitSnippet>();

    // load search engine settings
    String seSettings = properties.getProperty("seSettings");
    this.name2settings = (seSettings == null) ?
      SearchEngineSettings.getDefaultSettings() :
      SearchEngineSettings.loadSettings(new File(seSettings));

    // load search engine setting names to apply
    final String seNamesString = properties.getProperty("seNames");
    this.seNames = (seNamesString != null) ? seNamesString.split("\\s*,\\s*") :
      name2settings.keySet().toArray(new String[name2settings.size()]);
  }

  public List<CrawledPageRipperProcessor> getProcessors() {
    return processors;
  }

  /**
   * Submit the given query and process its hits through this processor.
   */
  public void processQuery(String query, int numHits) throws IOException {
    boolean addedQuery = false;

    for (String seName : seNames) {
      final SearchEngineSettings settings = name2settings.get(seName);
      if (settings == null) continue;  //todo: record a warning

      final QueryPage queryPage = settings.submitQuery(query, numHits, pageCrawler);
      if (queryPage == null) {
        System.err.println(new Date() + ": WARNING : SearchEngineProcessor.processQuery(" + query + ") seName=" + seName + " got null queryPage!");
      }
      else {
        // collect hitSnippets
        final List<SearchEngineSettings.HitSnippet> snippets = settings.getHitSnippets(queryPage.getHitNodes());
        if (snippets != null) hitSnippets.addAll(snippets);

        if (queryPage.crawlAndProcessHits(this)) {
          if (!addedQuery) {
            queryStrings.add(query);
            addedQuery = true;
          }
        }
      }
    }
  }

  /**
   * Process the ripped node.
   *
   * @return true if successful; otherwise, false.
   */
  protected boolean processRippedNode(CrawledPage crawledPage, Tree<XmlLite.Data> rippedNode) {
    this.results = null;

    boolean result = false;

    for (CrawledPageRipperProcessor processor : processors) {
      result |= processor.processRippedNode(crawledPage, rippedNode);
    }

    return result;
  }

  /**
   * Run the post ripping hook.
   */
  protected void postRippingHook(CrawledPage crawledPage) {

    if (crawledPage != null && crawledPage.hasContent()) {
      this.hitUrls.add(crawledPage.getUrl());
    }

    for (CrawledPageRipperProcessor processor : processors) {
      processor.postRippingHook(crawledPage);
    }
  }

  /**
   * Access method to dump results after running.
   */
  protected void dumpResults() {
    System.out.println("\nqueries");
    for (String query : queryStrings) {
      System.out.println("\t" + query);
    }

    System.out.println("\nhitUrls");
    for (String hitUrl : hitUrls) {
      System.out.println("\t" + hitUrl);
    }

    System.out.println("\nsnippets");
    for (SearchEngineSettings.HitSnippet snippet : hitSnippets) {
      System.out.println("\t" + snippet);
    }

    int index = 0;
    for (CrawledPageRipperProcessor processor : processors) {
      System.out.println("\nprocessor #" + index);
      processor.dumpResults();
      ++index;
    }
  }

  /**
   * Get this processor's results.
   */
  public List<ProcessorResult> getResults() {
    if (results == null) {
      results = new ArrayList<ProcessorResult>();

      // create and add a processor results based on an ordered set of queryStrings
      results.add(new OrderedSetProcessorResult("queryStrings", queryStrings));

      // create and add a processor results based on an ordered set of hitUrls
      results.add(new OrderedSetProcessorResult("hitUrls", hitUrls));

      // create and add a snippet processor results
      results.add(new SnippetProcessorResult("snippets", hitSnippets));

      // add all results from all processors
      for (CrawledPageRipperProcessor processor : processors) {
        results.addAll(processor.getResults());
      }
    }
    return results;
  }


  /**
   * Properties:
   * <ul>
   * <li>queryString -- (required; null result if missing) ###-delimitted list of queries to pose</li>
   * <li>numHits -- (optional, default=10) number of hits to analyze per query per search engine</li>
   * <li>processors -- comma-delimited list of classpaths to crawled page ripper processors (with properties constructors) to apply.</li>
   * <li>processorN (where N=1,2,3,...) -- claspaths to crawled page ripper processors (with properties constructors) to apply.</li>
   * <li>cacheDir -- (optional, default=null) directory for crawled page cache.</li>
   * <li>seSettings -- (optional, default=default cluster settings) search engine settings file.</li>
   * <li>seNames -- (optional, default=all se settings) comma-delimited list of names from seSettings to apply.</li>
   * </ul>
   * NOTE: The properties are passed to all processors instances on construction.
   */
  public static final List<ProcessorResult> createAndRun(Properties properties) {
    List<ProcessorResult> result = null;

    final String queryString = properties.getProperty("queryString");
    if (queryString != null && !"".equals(queryString)) {
      final String[] queries = queryString.split("\\s*###\\s*");
      final int numHits = Integer.parseInt(properties.getProperty("numHits", "10"));

      try {
        final SearchEngineProcessor processor = new SearchEngineProcessor(properties);

        for (String query : queries) {
          System.out.println(new Date() + ": SearchEngineProcessor numHits=" + numHits + " query=" + query);
          processor.processQuery(query, numHits);
        }

        result = processor.getResults();
      }
      catch (IOException e) {
        throw new IllegalStateException(e);
      }
    }

    return result;
  }


  //java -Xmx640m org.sd.crawl.SearchEngineProcessor processor1=org.sd.crawl.NGramProcessor cacheDir="/home/sbk/tmp/sdcrawler/cache" numHits=25 limitType="freq" nGramLimit="3" queries="Software Engineering Jobs in Salt Lake City UT";
  public static final void main(String[] args) throws IOException {
    //Properties:
    // seSettings   -- path to search engine settings file.
    // queries      -- ###-delimitted list of queries to pose
    // numHits      -- (optional, default=10) number of hits to analyze per query per search engine
    // seNames      -- (optional, default=all) comma-delimitted list of search engines to query
    // limitType    -- (optional, default="count") either "freq" or "count"
    // nGramLimit   -- (optional, default=10) number of top N-grams to show or minFreq of N-grams to show depending on limitType

    final PropertiesParser pp = new PropertiesParser(args);
    final Properties properties = pp.getProperties();

    final SearchEngineProcessor processor = new SearchEngineProcessor(properties);
    
    final String[] queries = properties.getProperty("queries").split("\\s*###\\s*");
    final int numHits = Integer.parseInt(properties.getProperty("numHits", "10"));

    for (String query : queries) {
      processor.processQuery(query, numHits);
    }

    processor.dumpResults();
  }
}
