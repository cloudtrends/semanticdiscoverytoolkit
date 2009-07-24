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


import org.sd.io.FileUtil;
import org.sd.text.DetailedUrl;
import org.sd.util.LineBuilder;
import org.sd.util.PropertiesParser;
import org.sd.util.StringUtil;
import org.sd.util.Timer;
import org.sd.util.tree.Tree;
import org.sd.util.tree.TraversalIterator;
import org.sd.xml.XmlFactory;
import org.sd.xml.XmlLite;
import org.sd.xml.XmlTreeHelper;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.Random;

/**
 * Utility to scrape addresses from query results.
 * <p>
 * @author Spence Koehler
 */
public class AddressScraper {

  private long fetchRate;
  private Random random;
  private Timer timer;
  private int flux;

  public AddressScraper(long fetchRate) {
    this.fetchRate = fetchRate;
    this.timer = fetchRate > 0 ? new Timer(fetchRate) : null;
    this.random = (timer == null) ? null : new Random();
    this.flux = (int)(fetchRate >> 3);  // fluctuate by about an eigth of the time
  }

// query structure:
//
//   for usa urls: http://maps.google.com/maps?f=q&hl=en&geocode=&q=<URL>
//   for co.uk urls: http://maps.google.co.uk/maps?f=q&hl=en&geocode=&q=<URL>
//
// extract from results:
//
//  div class="bn" contains <a href="...">CompanyName</a>
//
//   span class="adr" (container)
//     span class="street-address"
//     span class="locality"
//     span class="region" (US only)
//     span class="postal-code" (UK only)
//   span class="tel"
//
//   div class="sa"  (nature of business)
//
//  <a class=a href="X">Directions</a> where X holds ...&amp;daddr=t1+t2+...+tN...&amp;...
//            and term after locality (or before a term beginning with a left paren) is the post code (if don't find span class="postal-code")
//            and term after post code is the company name (in parentheses)
//  

// traverse the xml. keep div class="bn" text as potential company name.
// if see span class="adr", then look for and grab address fields
// once we see text (under "a" tag) "Directions", grab the href value and complete the record.


  public void processBatch(File batchFile, PageCrawler pageCrawler, File outputFile, int maxHits) throws IOException {
    final BufferedReader reader = FileUtil.getReader(batchFile);
    String line = null;

    final BufferedWriter writer = FileUtil.getWriter(outputFile);
    boolean skipGovernor = true;

    int numVisited = 0;
    while ((line = reader.readLine()) != null) {
      line = line.trim();
      if ("".equals(line) || line.charAt(0) == '#') continue;
      
      final List<Address> addresses = grabAndScrapeAddressesBySearchString(line, pageCrawler, maxHits);
      dumpOutput(line, addresses, writer);
      ++numVisited;

      if ((numVisited % 200) == 0) {
        System.out.println(new Date() + ": AddressScraper.processBatch processed " + numVisited + " queries.");
      }
    }

    writer.close();
    reader.close();
  }

  private final void doWait() {
    // governor on the rate of fetching
    if (timer != null) {
      while (!timer.reachedTimerMillis()) {
        final long sleepTime = timer.getRemainingTime() + random.nextInt(flux);
        if (sleepTime > 0) {
//          System.out.println(new Date() + ": ...sleeping " + sleepTime);
          try {
            Thread.sleep(sleepTime);
          }
          catch (InterruptedException e) {
            break;
          }
        }
      }
    }
  }

  private final boolean dumpOutput(String query, List<Address> addresses, BufferedWriter writer) throws IOException {
    boolean result = false;
    if (addresses == null || addresses.size() == 0) {
      writer.write(query);
      writer.newLine();
    }
    else {
      for (Address address : addresses) {
        result |= address.isFromCachedPage();

        final LineBuilder builder = new LineBuilder();
        builder.
          append(query).
          append(address.getRank()).
          appendBuilt(address.toString());

        writer.write(builder.toString());
        writer.newLine();
      }
    }
    writer.flush();
    return result;
  }

  /**
   * Scrape addresses from the given htmlFile saved from a map query.
   */
  public List<Address> scrapeAddresses(File htmlFile) throws IOException {
    List<Address> result = null;

    final Tree<XmlLite.Data> xmlTree = XmlFactory.readXmlTree(htmlFile, true, true, false);
    if (xmlTree != null) {
      final ScrapedData scrapedData = scrapeAddresses(xmlTree, true, 0);
      result = scrapedData.addresses;
    }

    return result;
  }

  /**
   * Scrape using an already formed query string.
   *
   * @param maxHits  -1 to grab all from first page; 0 to grab unlimited; +N to grab at most N hits.
   */
  public List<Address> grabAndScrapeAddressesByQuery(String fullQueryUrl, PageCrawler pageCrawler, int maxHits) {
    final List<Address> result = new ArrayList<Address>();

    boolean keepGoing = fullQueryUrl != null && !"".equals(fullQueryUrl);
    while (keepGoing) {

      final CrawledPage crawledPage = pageCrawler.fetch(fullQueryUrl);

      if (crawledPage != null) {

        if (!crawledPage.isFromCache()) {
//          System.out.println(new Date() + ": fetched '" + crawledPage.getUrl());
          doWait();
        }

        if (!crawledPage.isFromCache() && crawledPage.getTitle() != null && crawledPage.getTitle().startsWith("403 Forbidden")) {
          throw new IllegalStateException("Drats! Foiled again! -- got Forbidden code for url=" +
                                          fullQueryUrl + " file=" + crawledPage.getFile());
        }

        final Tree<XmlLite.Data> xmlTree = crawledPage.getXmlTree();
        if (xmlTree != null) {
          final ScrapedData scrapedData = scrapeAddresses(xmlTree, crawledPage.isFromCache(), result.size());
          for (Address address : scrapedData.addresses) {
            result.add(address);
            if (maxHits > 0 && result.size() >= maxHits) {
              keepGoing = false;
            }
          }

          if (maxHits >= 0 && scrapedData.hasNextUrl() && (maxHits == 0 || result.size() < maxHits)) {
            fullQueryUrl = scrapedData.getNextUrl();
          }
          else {
            keepGoing = false;
          }
        }
      }
    }

    return result;
  }

  public List<Address> grabAndScrapeAddressesByCompanyUrl(String companyUrl, PageCrawler pageCrawler, int maxHits) {
    List<Address> result = null;

    final String cleanUrl = cleanUrl(companyUrl);

    String queryUrl = null;
    boolean didCoUk = false;

    if (cleanUrl.indexOf(".co.uk") >= 0) {
      queryUrl = buildCoUkQuery(cleanUrl);
      didCoUk = true;
    }
    else {
      queryUrl = buildQuery(cleanUrl);
    }
    
    result = grabAndScrapeAddressesByQuery(queryUrl, pageCrawler, maxHits);
    if ((result == null || result.size() == 0) && !didCoUk) {
      queryUrl = buildCoUkQuery(cleanUrl);
      result = grabAndScrapeAddressesByQuery(queryUrl, pageCrawler, maxHits);
    }

    return result;
  }

  public List<Address> grabAndScrapeAddressesBySearchString(String searchString, PageCrawler pageCrawler, int maxHits) {
    List<Address> result = null;

    final String query = StringUtil.urlQueryEscape(searchString);

    String queryUrl = null;
    queryUrl = buildQuery(query);
    
    result = grabAndScrapeAddressesByQuery(queryUrl, pageCrawler, maxHits);

    return result;

  }

  private final String cleanUrl(String companyUrl) {
    final DetailedUrl dUrl = new DetailedUrl(companyUrl);
    return dUrl.getHost(true, true, false);
  }

  private final String buildCoUkQuery(String cleanUrl) {
    return "http://maps.google.co.uk/maps?f=q&hl=en&geocode=&q=" + cleanUrl;
  }

  private final String buildQuery(String cleanUrl) {
    return "http://maps.google.com/maps?f=q&hl=en&geocode=&q=" + cleanUrl;
  }

  public ScrapedData scrapeAddresses(Tree<XmlLite.Data> xmlTree, boolean isFromCache, int startRank) {
    final ScrapedData addresses = new ScrapedData(startRank);

    String companyName = null;
    String href = null;
    for (TraversalIterator<XmlLite.Data> iter = xmlTree.iterator(Tree.Traversal.DEPTH_FIRST); iter.hasNext(); ) {
      final Tree<XmlLite.Data> curNode = iter.next();

      final XmlLite.Tag tag = curNode.getData().asTag();
      if (tag != null) {
        final String tagClass = tag.getAttribute("class");
        if ("div".equals(tag.name)) {
          if ("bn".equals(tagClass)) {
            final Tree<XmlLite.Data> aNode = XmlTreeHelper.findNode(curNode, "a");
            if (aNode != null) {
              companyName = XmlTreeHelper.getAllText(aNode);
              iter.skip();
            }
          }
        }
        else if ("span".equals(tag.name)) {
          if ("adr".equals(tagClass)) {
            final Address address = scrapeAddress(iter, companyName);
            if (address != null) {
              addresses.add(address, isFromCache);
            }
          }
        }
        else if ("a".equals(tag.name) && !addresses.hasNextUrl()) {
          // save href for when we see "Next" text
          href = tag.getAttribute("href");
        }
      }
      else if (!addresses.hasNextUrl()) {
        final XmlLite.Text text = curNode.getData().asText();
        if (text != null && "Next".equals(text.text)) {
          if (href.startsWith("/")) href = "http://maps.google.com" + href;
          addresses.setNextUrl(href);
        }
      }
    }

    return addresses;
  }

  private final Address scrapeAddress(TraversalIterator<XmlLite.Data> iter, String companyName) {
    String streetAddress = null;
    String locality = null;
    String region = null;
    String postalCode = null;
    String tel = null;
    String website = null;
    String natureOfBusiness = null;

    while (iter.hasNext()) {
      final Tree<XmlLite.Data> curNode = iter.next();

      final XmlLite.Tag tag = curNode.getData().asTag();
      if (tag != null) {
        if ("span".equals(tag.name)) {
          final String tagClass = tag.getAttribute("class");

          if ("street-address".equals(tagClass)) {
            streetAddress = XmlTreeHelper.getAllText(curNode);
            iter.skip();
          }
          else if ("locality".equals(tagClass)) {
            locality = XmlTreeHelper.getAllText(curNode);
            iter.skip();
          }
          else if ("region".equals(tagClass)) {
            region = XmlTreeHelper.getAllText(curNode);
            iter.skip();
          }
          else if ("postal-code".equals(tagClass)) {
            postalCode = XmlTreeHelper.getAllText(curNode);
            iter.skip();
          }
          else if ("tel".equals(tagClass)) {
            tel = XmlTreeHelper.getAllText(curNode);
            iter.skip();
          }
        }
        else if ("div".equals(tag.name)) {
          final String tagClass = tag.getAttribute("class");
          if ("sa".equals(tagClass)) {
            // grab first span
            final Tree<XmlLite.Data> spanNode = XmlTreeHelper.findNode(curNode, "span");
            if (spanNode != null) {
              natureOfBusiness = XmlTreeHelper.getAllText(spanNode);
              iter.skip();
            }
          }
        }
        else if ("a".equals(tag.name)) {
          final String aText = XmlTreeHelper.getAllText(curNode);
          iter.skip();

          if ("Directions".equals(aText)) {
            if (postalCode == null && region != null) {
              final String href = tag.getAttribute("href");

              // pull out postal-code from href
              postalCode = extractHrefPostalCode(href, region);
            }

            break;
          }
          else if ("Website".equals(aText)) {
            website = extractLocalUrl(tag.getAttribute("href"));
          }
        }
      }
    }

    return buildAddress(companyName, streetAddress, locality, region, postalCode, tel, website, natureOfBusiness);
  }

  private final String extractLocalUrl(String href) {
    String result = null;

    final int qPos = href.indexOf("q=");
    if (qPos >= 0) {
      int ampPos = href.indexOf('&', qPos + 1);
      if (ampPos < 0) ampPos = href.length();
      result = href.substring(qPos + 2, ampPos);
    }

    return result;
  }

  private final String extractHrefPostalCode(String href, String region) {
    String result = null;

    final int startPos = href.indexOf("&daddr=");
    if (startPos > 0) {
      final int endPos = href.indexOf("&", startPos + 11);
      href = href.substring(startPos + 11, endPos);
      region = region.replaceAll("\\s+", "+");
      final int regionPos = href.indexOf(region);

      if (regionPos >= 0) {
        final int postStartPos = regionPos + region.length() + 1;
        int postEndPos = href.indexOf('(', postStartPos);

        if (postEndPos >= 0) {
          result = href.substring(postStartPos, postEndPos);
        }
        else {
          result = href.substring(postStartPos);
        }
        
        result = result.replaceAll("\\+", " ").trim();
      }
    }

    return result;
  }

  private final Address buildAddress(String companyName, String streetAddress,
                                     String locality, String region, String postalCode,
                                     String tel, String website, String natureOfBusiness) {
    Address result = null;

    if (streetAddress != null) {
      result = new Address(companyName, streetAddress, locality, region, postalCode, tel, website, natureOfBusiness);
    }

    return result;
  }


  public static final class Address {
    public final String companyName;
    public final String streetAddress;
    public final String locality;
    public final String region;
    public final String postalCode;
    public final String tel;
    public final String website;
    public final String natureOfBusiness;

    private int rank;
    private boolean fromCachedPage;

    public Address(String companyName, String streetAddress, String locality, String region, String postalCode, String tel, String website, String natureOfBusiness) {
      this.companyName = companyName;
      this.streetAddress = streetAddress;
      this.locality = locality;
      this.region = region;
      this.postalCode = postalCode;
      this.tel = tel;
      this.website = website;
      this.natureOfBusiness = natureOfBusiness;

      this.rank = 0;
      this.fromCachedPage = false;
    }

    public void setRank(int rank) {
      this.rank = rank;
    }

    public int getRank() {
      return this.rank;
    }

    public void setFromCachedPage(boolean fromCachedPage) {
      this.fromCachedPage = fromCachedPage;
    }

    public boolean isFromCachedPage() {
      return fromCachedPage;
    }

    public String toString() {
      final LineBuilder result = new LineBuilder();

      result.
        append(rank).
        append(companyName).
        append(streetAddress).
        append(locality).
        append(region).
        append(postalCode).
        append(tel).
        append(website).
        append(natureOfBusiness);

      return result.toString();
    }
  }

  private static final class ScrapedData {
    public final List<Address> addresses;
    private int startRank;
    private String nextUrl;

    public ScrapedData(int startRank) {
      this.addresses = new ArrayList<Address>();
      this.startRank = startRank;
      this.nextUrl = null;
    }

    public void add(Address address, boolean isFromCache) {
      if (address != null) {
        address.setRank(startRank + addresses.size());
        address.setFromCachedPage(isFromCache);
        addresses.add(address);
      }
    }

    public boolean hasNextUrl() {
      return nextUrl != null;
    }

    public String getNextUrl() {
      return nextUrl;
    }

    public void setNextUrl(String nextUrl) {
      this.nextUrl = nextUrl;
    }
  }


  public static final class GoogleLineFixer implements LineFixer {
    public String fixLine(String line) {
      // created to fix (hopefully temporary condition) where google lines have
      // an unterminated comment after "<style..." in the html header.
      if (line.startsWith("<style") && line.endsWith("<!--")) {
        line = "<style type=text/css>";
      }
      return line;
    }
  }


  // java -Xmx640m org.sd.crawl.AddressScraper htmlFile=~/tmp/crawl/google.sd-map.html
  // java -Xmx640m org.sd.crawl.AddressScraper htmlFile=~/tmp/crawl/google-uk.cobra-map.html
  // java -Xmx640m org.sd.crawl.AddressScraper companyUrl=www.google.com cacheDir=~/tmp/crawl/cache
  public static final void main(String[] args) throws IOException {
    // Properties:
    //
    //   htmlFile  -- an html page to scrape (saved from a map query for the url)
    //   fullUrl -- full url to send as the query to scrape an address from
    //   query -- query or search string to send to scrape addresses from
    //   companyUrl -- url of a company to form into a query to submit and scrape
    //
    //   batchFile -- batch of companyUrl strings (one per line)
    //   crawlDelay -- (default=1500) rate (in millis) at which to fetch each batch item
    //   outputFile -- file to which output should be dumped
    //
    //   cacheDir -- cache directory to use with PageCrawler
    //   maxHits -- maximum number of hits to grab (default is -1, meaning just the first page's hits)
    //

    final PropertiesParser pp = new PropertiesParser(args);
    final Properties properties = pp.getProperties();

    if (properties.getProperty("crawlDelay") == null) {
      String crawlDelay = "1500";

      if (properties.getProperty("fetchRate") != null) {
        System.out.println("*** WARNING: 'fetchRate' has been deprecated. Use 'crawlDelay' instead in the future!");
        crawlDelay = properties.getProperty("fetchRate");
      }

      // set this for the crawler, which also uses it
      properties.setProperty("crawlDelay", crawlDelay);
    }

    boolean showAddresses = true;
    List<Address> addresses = null;
    final long fetchRate = Long.parseLong(properties.getProperty("crawlDelay", "1500"));
    final AddressScraper addressScraper = new AddressScraper(fetchRate);

    if (properties.getProperty("htmlFile") != null) {
      addresses = addressScraper.scrapeAddresses(new File(properties.getProperty("htmlFile")));
    }
    else {
      final int maxHits = Integer.parseInt(properties.getProperty("maxHits", "-1"));
      final PageCrawler pageCrawler = new PageCrawler(properties);
      pageCrawler.setLineFixer(new GoogleLineFixer());

      if (properties.getProperty("fullUrl") != null) {
        addresses = addressScraper.grabAndScrapeAddressesByQuery(properties.getProperty("fullUrl"), pageCrawler, maxHits);
      }
      else if (properties.getProperty("companyUrl") != null) {
        addresses = addressScraper.grabAndScrapeAddressesByCompanyUrl(properties.getProperty("companyUrl"), pageCrawler, maxHits);
      }
      else if (properties.getProperty("query") != null) {
        addresses = addressScraper.grabAndScrapeAddressesBySearchString(properties.getProperty("query"), pageCrawler, maxHits);
      }
      else if (properties.getProperty("batchFile") != null) {
        final File batchFile = new File(properties.getProperty("batchFile"));
        final File outputFile = new File(properties.getProperty("outputFile"));

        addressScraper.processBatch(batchFile, pageCrawler, outputFile, maxHits);
        
        showAddresses = false;
      }
    }

    if (showAddresses) {
      if (addresses != null) {
        System.out.println("Found " + addresses.size() + " addresses:");
        int index = 0;
        for (Address address : addresses) {
          System.out.println(index + ": " + address + "\n");
          ++index;
        }
      }
      else {
        System.out.println("No addresses found. properties=" + properties);
      }
    }
  }
}
