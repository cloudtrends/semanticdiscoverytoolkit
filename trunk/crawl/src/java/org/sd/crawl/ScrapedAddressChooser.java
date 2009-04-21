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


import org.sd.io.FileIterator;
import org.sd.io.FileUtil;
import org.sd.text.DetailedUrl;
import org.sd.util.LineBuilder;
import org.sd.util.PropertiesParser;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * Utility to choose the "best" scraped address.
 * <p>
 * @author Spence Koehler
 */
public class ScrapedAddressChooser {
  
  protected final Properties properties;

  private String companyNameConstraint;
  private String streetAddressConstraint;
  private String localityConstraint;
  private String regionConstraint;
  private String postalCodeConstraint;
  private String telephoneConstraint;
  private String websiteConstraint;
  private String natureOfBusinessConstraint;

  private Map<String, String> query2host;

  public ScrapedAddressChooser(Properties properties) throws IOException {
    this.properties = properties;

    this.companyNameConstraint = properties.getProperty("companyName");
    this.streetAddressConstraint = properties.getProperty("streetAddress");
    this.localityConstraint = properties.getProperty("locality");
    this.regionConstraint = properties.getProperty("region");
    this.postalCodeConstraint = properties.getProperty("postalCode");
    this.telephoneConstraint = properties.getProperty("telephone");
    this.websiteConstraint = properties.getProperty("website");
    this.natureOfBusinessConstraint = properties.getProperty("natureOfBusiness");

    final String query2hostFile = properties.getProperty("query2hostFile");
    if (query2hostFile != null) {
      this.query2host = loadQuery2HostFile(new File(query2hostFile));
    }
  }

  private final Map<String, String> loadQuery2HostFile(File query2hostFile) throws IOException {
    final Map<String, String> result = new HashMap<String, String>();

    final BufferedReader reader = FileUtil.getReader(query2hostFile);
    String line = null;
    while ((line = reader.readLine()) != null) {
      if ("".equals(line) || line.charAt(0) == '#') continue;
      final String[] pieces = line.split("\\s*\\|\\s*");
      final String query = pieces[0];
      final String host = new DetailedUrl(pieces[1]).getHost(true, false, false);

      if (host != null && !"".equals(host)) {
        result.put(query, host);
      }
    }
    reader.close();

    return result;
  }

  /**
   * Iterate over the scraped file, writing the "best" scraped address for
   * each query group to the given writer.
   */
  public void chooseBest(File scrapedFile, BufferedWriter writer) throws IOException {

    final FileIterator<QueryGroup> fileIter = QueryGroup.getFileIterator(scrapedFile);

    try {
      while (fileIter.hasNext()) {
        final QueryGroup queryGroup = fileIter.next();
        final QueryGroup.ScrapedAddress scrapedAddress = chooseBest(queryGroup);

        final LineBuilder builder = new LineBuilder();

        if (scrapedAddress == null) {
          builder.append(queryGroup.query);
        }
        else {
          builder.appendBuilt(scrapedAddress.toString());
        }

        writer.write(builder.toString());
        writer.newLine();
        writer.flush();
      }
    }
    finally {
      fileIter.close();
    }
  }

  public QueryGroup.ScrapedAddress chooseBest(QueryGroup queryGroup) {
    QueryGroup.ScrapedAddress result = null;

    // choose the top ranked scraped address with this instance's constraints
    //   (companyName, streetAddress, locality, region, postalCode, telephone, website, natureOfBusiness)
    final List<QueryGroup.ScrapedAddress> scrapedAddresses = queryGroup.getScrapedAddresses();  // sorted by rank

    for (QueryGroup.ScrapedAddress scrapedAddress : scrapedAddresses) {
      boolean meetsConstraints = true;
      
      if (meetsConstraints && companyNameConstraint != null) {
        meetsConstraints &= meetsCompanyNameConstraint(scrapedAddress);
      }
      if (meetsConstraints && streetAddressConstraint != null) {
        meetsConstraints &= meetsStreetAddressConstraint(scrapedAddress);
      }
      if (meetsConstraints && localityConstraint != null) {
        meetsConstraints &= meetsLocalityConstraint(scrapedAddress);
      }
      if (meetsConstraints && regionConstraint != null) {
        meetsConstraints &= meetsRegionConstraint(scrapedAddress);
      }
      if (meetsConstraints && postalCodeConstraint != null) {
        meetsConstraints &= meetsPostalCodeConstraint(scrapedAddress);
      }
      if (meetsConstraints && telephoneConstraint != null) {
        meetsConstraints &= meetsTelephoneConstraint(scrapedAddress);
      }
      if (meetsConstraints && websiteConstraint != null) {
        meetsConstraints &= meetsWebsiteConstraint(scrapedAddress);
      }
      if (meetsConstraints && natureOfBusinessConstraint != null) {
        meetsConstraints &= meetsNatureOfBusinessConstraint(scrapedAddress);
      }
      if (meetsConstraints && query2host != null) {
        meetsConstraints &= meetsHostConstraint(scrapedAddress);
      }

      if (meetsConstraints) {
        result = scrapedAddress;
        break;
      }
    }

    return result;
  }

  protected boolean meetsCompanyNameConstraint(QueryGroup.ScrapedAddress scrapedAddress) {
    return companyNameConstraint.equals(scrapedAddress.companyName);
  }

  protected boolean meetsStreetAddressConstraint(QueryGroup.ScrapedAddress scrapedAddress) {
    return streetAddressConstraint.equals(scrapedAddress.streetAddress);
  }

  protected boolean meetsLocalityConstraint(QueryGroup.ScrapedAddress scrapedAddress) {
    return localityConstraint.equals(scrapedAddress.locality);
  }

  protected boolean meetsRegionConstraint(QueryGroup.ScrapedAddress scrapedAddress) {
    return regionConstraint.equals(scrapedAddress.region);
  }

  protected boolean meetsPostalCodeConstraint(QueryGroup.ScrapedAddress scrapedAddress) {
    return postalCodeConstraint.equals(scrapedAddress.postalCode);
  }

  protected boolean meetsTelephoneConstraint(QueryGroup.ScrapedAddress scrapedAddress) {
    return telephoneConstraint.equals(scrapedAddress.telephone);
  }

  protected boolean meetsWebsiteConstraint(QueryGroup.ScrapedAddress scrapedAddress) {
    return websiteConstraint.equals(scrapedAddress.website);
  }

  protected boolean meetsNatureOfBusinessConstraint(QueryGroup.ScrapedAddress scrapedAddress) {
    return natureOfBusinessConstraint.equals(scrapedAddress.natureOfBusiness);
  }

  protected boolean meetsHostConstraint(QueryGroup.ScrapedAddress scrapedAddress) {
    boolean result = false;

    if (scrapedAddress.website != null && !"".equals(scrapedAddress.website)) {
      final DetailedUrl dUrl = new DetailedUrl(scrapedAddress.website);
      final String normalizedHost = dUrl.getHost(true, false, false);
      final String hostConstraint = query2host.get(scrapedAddress.getQuery());

      result = (hostConstraint != null) ? hostConstraint.equals(normalizedHost) : true;
    }

    return result;
  }


  //java -Xmx640m org.sd.crawl.ScrapedAddressChooser scrapedFile=/home/sbk/sd/resources/seed/sd1364dm/sd1364-company-name.scraped.txt.gz outputFile=/home/sbk/sd/resources/seed/sd1364dm/sd1364-company-name.scraped.best.txt.gz query2hostFile=/home/sbk/sd/resources/seed/sd1364dm/query2host.txt
  public static final void main(String[] args) throws IOException {
    // Properties:
    //   scrapedFile -- path to file holding scraped address data
    //   outputFile -- path to (best scraped address) output file
    //
    //   ...constraints (strings to match)...
    //  query2hostFile -- path to file mapping queries to urls for constraining hosts
    //
    final PropertiesParser pp = new PropertiesParser(args);
    final Properties properties = pp.getProperties();

    final File scrapedFile = new File(properties.getProperty("scrapedFile"));
    final File outputFile = new File(properties.getProperty("outputFile"));

    final ScrapedAddressChooser chooser = new ScrapedAddressChooser(properties);

    final BufferedWriter writer = FileUtil.getWriter(outputFile);
    chooser.chooseBest(scrapedFile, writer);
    writer.close();
  }
}
