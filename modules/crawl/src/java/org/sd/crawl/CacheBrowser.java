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


import org.sd.bdb.BerkeleyDb;
import org.sd.bdb.DbHandle;
import org.sd.bdb.DbIterator;
import org.sd.bdb.DbTraversal;
import org.sd.bdb.StringKeyValuePair;
import org.sd.util.DateUtil;
import org.sd.util.LineBuilder;
import org.sd.util.PropertiesParser;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Properties;

/**
 * Utility to browse a crawl cache.
 * <p>
 * @author Spence Koehler
 */
public class CacheBrowser {

  public static final String[] DEFAULT_FIELDS = new String[] {
    "url", "crawlTimestamp", "localDir", "localName", "error",
  };


  private File cacheDir;
  private File mapDir;
  private DbHandle dbHandle;

  public CacheBrowser(File cacheDir) {
    this.cacheDir = cacheDir;
    this.mapDir = new File(cacheDir, CacheMap.DB_MAP_DIR);
    final BerkeleyDb db = BerkeleyDb.getInstance(mapDir, false, true);
    this.dbHandle = db.get(CacheMap.DB_MAP_NAME, false);
  }

  public void dumpEntries(String fieldsStrings) {
    final String[] fields = (fieldsStrings == null) ? DEFAULT_FIELDS : fieldsStrings.split("\\s*,\\s*");

    dumpHeader(fields);

    final DbIterator<StringKeyValuePair> iter = dbHandle.iterator(DbTraversal.KEY_ORDER);
    while (iter.hasNext()) {
      final StringKeyValuePair kvPair = iter.next();
      if (kvPair == null) continue;

      final CacheEntry cacheEntry = (CacheEntry)kvPair.getPublishable();
      dumpEntry(fields, cacheEntry);
    }

    iter.close();
  }

  public void close() {
    
  }

  private final void dumpHeader(String[] fields) {
    final LineBuilder builder = new LineBuilder();
    for (String field : fields) builder.append(field);
    System.out.println(builder.toString());
  }
      
  private final void dumpEntry(String[] fields, CacheEntry cacheEntry) {
    final LineBuilder builder = new LineBuilder();
    for (String field : fields) {
      builder.append(getFieldData(field, cacheEntry));
    }
    System.out.println(builder.toString());
  }

  private final String getFieldData(String field, CacheEntry cacheEntry) {
    String result = null;
    final PageMetaData metaData = cacheEntry.getMetaData();

    if ("url".equals(field)) {
      result = metaData.getUrl();
    }
    else if ("actualLength".equals(field)) {
      result = Integer.toString(metaData.getActualLength());
    }
    else if ("crawlTimestamp".equals(field)) {
      result = getDateString(metaData.getCrawlTimestamp());
    }
    else if ("contentType".equals(field)) {
      result = metaData.getContentType();
    }
    else if ("contentLength".equals(field)) {
      result = Integer.toString(metaData.getContentLength());
    }
    else if ("lastModified".equals(field)) {
      result = getDateString(metaData.getLastModified());
    }
    else if ("referringUrls".equals(field)) {
      result = getListString(metaData.getReferringUrls());
    }
    else if ("referringTexts".equals(field)) {
      result = getListString(metaData.getReferringTexts());
    }
    else if ("titleText".equals(field)) {
      result = metaData.getTitleText();
    }
    else if ("metaKeywords".equals(field)) {
      result = metaData.getMetaKeywords();
    }
    else if ("metaDescription".equals(field)) {
      result = metaData.getMetaDescription();
    }
    else if ("responseCode".equals(field)) {
      result = Integer.toString(metaData.getResponseCode());
    }
    else if ("responseMessage".equals(field)) {
      result = metaData.getResponseMessage();
    }
    else if ("error".equals(field)) {
      result = metaData.getError();
    }
    else if ("downloadHeaderTime".equals(field)) {
      result = getDateString(metaData.getDownloadHeaderTime());
    }
    else if ("downloadContentTime".equals(field)) {
      result = getDateString(metaData.getDownloadContentTime());
    }
    else if ("localDir".equals(field)) {
      result = cacheEntry.getLocalDir();
    }
    else if ("localName".equals(field)) {
      result = cacheEntry.getLocalName();
    }

    return result;
  }

  private final String getDateString(long timestamp) {
    return DateUtil.buildDateString(timestamp);
//    return new Date(timestamp).toString();
  }

  private String getListString(List<String> list) {
    final StringBuilder result = new StringBuilder();

    if (list != null) {
      for (String item : list) {
        if (result.length() > 0) result.append(',');
        result.append(item);
      }
    }

    return result.toString();
  }


  public static final void main(String[] args) throws IOException {
    // Properties:
    //
    // cacheDir -- Cache directory to browse
    //
    // fields -- comma delimited list of fields to dump from the cach map from among
    //           {url, actualLength, crawlTimestamp, contentType, contentLength, lastModified, 
    //            referringUrls, referringTexts, titleText, metaKeywords, metaDescription, 
    //            responseCode, responseMessage, error, downloadHeaderTime, downloadContentTime,
    //            localDir,localName}

    final PropertiesParser pp = new PropertiesParser(args);
    final Properties properties = pp.getProperties();
    args = pp.getArgs();

    final File cacheDir = new File(properties.getProperty("cacheDir"));
    final String fieldsString = properties.getProperty("fields");

    final CacheBrowser browser = new CacheBrowser(cacheDir);
    browser.dumpEntries(fieldsString);
    
  }
}
