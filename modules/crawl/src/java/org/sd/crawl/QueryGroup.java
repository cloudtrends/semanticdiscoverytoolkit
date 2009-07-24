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


import org.sd.io.FileChunk;
import org.sd.io.FileChunkFactory;
import org.sd.io.FileIterator;
import org.sd.util.LineBuilder;
import org.sd.util.StringUtil;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * A FileChunk for grouping query results for use with FileIterator.
 * <p>
 * @author Spence Koehler
 */
public class QueryGroup implements FileChunk {

  public static final QueryGroupFactory QUERY_GROUP_FACTORY = new QueryGroupFactory();

  public static final FileIterator<QueryGroup> getFileIterator(File file) throws IOException {
    return new FileIterator<QueryGroup>(file, QUERY_GROUP_FACTORY);
  }


  public static final int NUM_FIELDS = 10;

  public static final int QUERY = 0;
  public static final int RANK = 1;
  public static final int COMPANY_NAME = 2;
  public static final int STREET_ADDRESS = 3;
  public static final int LOCALITY = 4;
  public static final int REGION = 5;
  public static final int POSTAL_CODE = 6;
  public static final int TELEPHONE = 7;
  public static final int WEBSITE = 8;
  public static final int NATURE_OF_BUSINESS = 9;

  private String fileLine;

  public final long queryId;
  public final String query;
  public final List<ScrapedAddress> scrapedAddresses;

  private final AtomicLong nextQueryId = new AtomicLong(0L);
  private boolean unsorted;

  public QueryGroup(String fileLine) {
    this.fileLine = fileLine;
    this.queryId = nextQueryId.getAndIncrement();
    this.unsorted = false;

    final String[] pieces = StringUtil.splitFields(fileLine, NUM_FIELDS);
    this.query = pieces[0];

    this.scrapedAddresses = new ArrayList<ScrapedAddress>();
    this.scrapedAddresses.add(new ScrapedAddress(pieces, this));
  }

  /**
   * Add the file line to this chunk if possible.
   * <p>
   * If the fileLine does not belong in this chunk, return false.
   *
   * @return true if the fileLine belongs in this chunk; otherwise, false.
   */
  public boolean addLine(String fileLine) {
    boolean result = false;

    final String[] pieces = StringUtil.splitFields(fileLine, NUM_FIELDS);
    if (query.equals(pieces[0])) {
      result = true;
      this.scrapedAddresses.add(new ScrapedAddress(pieces, this));
      unsorted = true;
    }

    return result;
  }

  /**
   * Get this chunk's "line".
   */
  public String getLine() {
    return fileLine;
  }

  public final List<ScrapedAddress> getScrapedAddresses() {
    if (unsorted) {
      Collections.sort(scrapedAddresses);
      unsorted = false;
    }
    return scrapedAddresses;
  }


  /**
   * Container for scraped addresses.
   */
  public static final class ScrapedAddress implements Comparable<ScrapedAddress> {
    public final String[] pieces;
    public final QueryGroup container;

    public final int rank;
    public final String companyName;
    public final String streetAddress;
    public final String locality;
    public final String region;
    public final String postalCode;
    public final String telephone;
    public final String website;
    public final String natureOfBusiness;

    public ScrapedAddress(String[] pieces, QueryGroup container) {
      this.pieces = pieces;
      this.container = container;

      this.rank = "".equals(pieces[RANK]) ? 0 : Integer.parseInt(pieces[RANK]);
      this.companyName = pieces[COMPANY_NAME];
      this.streetAddress = pieces[STREET_ADDRESS];
      this.locality = pieces[LOCALITY];
      this.region = pieces[REGION];
      this.postalCode = pieces[POSTAL_CODE];
      this.telephone = pieces[TELEPHONE];
      this.website = pieces[WEBSITE];
      this.natureOfBusiness = pieces[NATURE_OF_BUSINESS];
    }

    public String getQuery() {
      return pieces[0];
    }

    public int compareTo(ScrapedAddress other) {
      return this.rank - other.rank;
    }

    public String toString() {
      final LineBuilder result = new LineBuilder();

      for (String piece : pieces) {
        result.append(piece);
      }

      return result.toString();
    }
  }

  /**
   * FileChunkFactory for building EmailDomain instances through FileIterator.
   */
  public static final class QueryGroupFactory implements FileChunkFactory<QueryGroup> {

    // singleton
    private QueryGroupFactory() {
    }

    /**
     * Build a file chunk instance from its first file line.
     * <p>
     * A file chunk is a container for "chunks" from a file that is being iterated
     * over using a FileIterator.
     */
    public QueryGroup buildFileChunk(String fileLine) {
      return new QueryGroup(fileLine);
    }
  }
}
