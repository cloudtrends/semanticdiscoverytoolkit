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
import org.sd.io.PersistablePublishable;
import org.sd.util.LineBuilder;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Container for a cache entry.
 * <p>
 * @author Spence Koehler
 */
public class CacheEntry extends PersistablePublishable {


//   public static final int URL              = 0;
//   public static final int LOCAL_NAME       = 1;
//   public static final int ACTUAL_LENGTH    = 2;
//   public static final int CRAWL_TIMESTAMP  = 3;
//   public static final int CONTENT_TYPE     = 4;
//   public static final int CONTENT_LENGTH   = 5;
//   public static final int LAST_MODIFIED    = 6;
//   public static final int REFERRING_URL    = 7;
//   public static final int REFERRING_TEXT   = 8;
//   public static final int TITLE_TEXT       = 9;
//   public static final int META_KEYWORDS    = 10;
//   public static final int META_DESCRIPTION = 11;
//   public static final int RESPONSE_CODE    = 12;
//   public static final int RESPONSE_MESSAGE = 13;
//   public static final int ERROR            = 14;

  //
  //NOTE: If member variables are changed here or in subclasses, update
  //      CURRENT_VERSION and keep track of deserializing old persisted
  //      instances!
  //
  private static final int CURRENT_VERSION = 3;
  

  private PageMetaData pageMetaData;
  private String localDir;
  private String localName;

  /**
   * Default constructor for publishable reconstruction.
   */
  public CacheEntry() {
  }

  /**
   * Create a cache entry for the crawledPage to reside within a cacheDir
   * under the given localDir and localName.
   */
  public CacheEntry(CrawledPage crawledPage, String localDir, String localName) {

    this.pageMetaData = crawledPage.getMetaData();
    this.localDir = localDir;
    this.localName = localName;
  }

  public PageMetaData getMetaData() {
    return pageMetaData;
  }

  public String getLocalDir() {
    return localDir;
  }

  public String getLocalName() {
    return localName;
  }

  public File getFile(File cacheDir) {
    final File subdir = localDir == null ? cacheDir : new File(cacheDir, localDir);
    return new File(subdir, localName);
  }


  /**
   * Get the current version.
   * <p>
   * Note that changes to subclasses as well as to this class will require
   * this value to change and proper handling in the write/read methods.
   */
  protected final int getCurrentVersion() {
    return CURRENT_VERSION;
  }


  /**
   * Write this message to the dataOutput stream such that this message
   * can be completely reconstructed through this.read(dataInput).
   *
   * @param dataOutput  the data output to write to.
   */
  protected void writeCurrentVersion(DataOutput dataOutput) throws IOException {
    MessageHelper.writePublishable(dataOutput, pageMetaData);
    MessageHelper.writeString(dataOutput, localDir);
    MessageHelper.writeString(dataOutput, localName);
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
  protected void readVersion(int version, DataInput dataInput) throws IOException {
    if (version == 1) {
      readVersion1(dataInput);
    }
    else if (version == 2) {
      readVersion2(dataInput);
    }
    else if (version == 3) {
      readVersion3(dataInput);
    }
    else {
      badVersion(version);
    }
  }

  private final void readVersion1(DataInput dataInput) throws IOException {
    final String url = MessageHelper.readString(dataInput);
    this.localDir = null;
    this.localName = MessageHelper.readString(dataInput);
    final int actualLength = dataInput.readInt();
    final long crawlTimestamp = dataInput.readLong();
    final String contentType = MessageHelper.readString(dataInput);
    final int contentLength = dataInput.readInt();
    final long lastModified = dataInput.readLong();

    List<String> referringUrls = null;
    final int referringUrlsLen = dataInput.readInt();
    if (referringUrlsLen >= 0) {
      referringUrls = new ArrayList<String>();
      for (int i = 0; i < referringUrlsLen; ++i) {
        referringUrls.add(MessageHelper.readString(dataInput));
      }
    }

    List<String> referringTexts = null;
    final int referringTextsLen = dataInput.readInt();
    if (referringTextsLen >= 0) {
      referringTexts = new ArrayList<String>();
      for (int i = 0; i < referringTextsLen; ++i) {
        referringTexts.add(MessageHelper.readString(dataInput));
      }
    }

    final String titleText = MessageHelper.readString(dataInput);
    final String metaKeywords = MessageHelper.readString(dataInput);
    final String metaDescription = MessageHelper.readString(dataInput);
    final int responseCode = dataInput.readInt();
    final String responseMessage = MessageHelper.readString(dataInput);
    final String error = MessageHelper.readString(dataInput);

    this.pageMetaData = new PageMetaData(url, responseCode, responseMessage,
                                         contentType, contentLength, lastModified,
                                         null, null, titleText, metaKeywords, metaDescription,
                                         error, actualLength, 0L, 0L);
    pageMetaData.setCrawlTimestamp(crawlTimestamp);
    pageMetaData.setReferringInfo(referringUrls, referringTexts);
  }

  // version2 added localDir
  private final void readVersion2(DataInput dataInput) throws IOException {
    final String url = MessageHelper.readString(dataInput);
    this.localDir = MessageHelper.readString(dataInput);
    this.localName = MessageHelper.readString(dataInput);
    final int actualLength = dataInput.readInt();
    final long crawlTimestamp = dataInput.readLong();
    final String contentType = MessageHelper.readString(dataInput);
    final int contentLength = dataInput.readInt();
    final long lastModified = dataInput.readLong();

    List<String> referringUrls = null;
    final int referringUrlsLen = dataInput.readInt();
    if (referringUrlsLen >= 0) {
      referringUrls = new ArrayList<String>();
      for (int i = 0; i < referringUrlsLen; ++i) {
        referringUrls.add(MessageHelper.readString(dataInput));
      }
    }

    List<String> referringTexts = null;
    final int referringTextsLen = dataInput.readInt();
    if (referringTextsLen >= 0) {
      referringTexts = new ArrayList<String>();
      for (int i = 0; i < referringTextsLen; ++i) {
        referringTexts.add(MessageHelper.readString(dataInput));
      }
    }

    final String titleText = MessageHelper.readString(dataInput);
    final String metaKeywords = MessageHelper.readString(dataInput);
    final String metaDescription = MessageHelper.readString(dataInput);
    final int responseCode = dataInput.readInt();
    final String responseMessage = MessageHelper.readString(dataInput);
    final String error = MessageHelper.readString(dataInput);


    this.pageMetaData = new PageMetaData(url, responseCode, responseMessage,
                                         contentType, contentLength, lastModified,
                                         null, null, titleText, metaKeywords, metaDescription,
                                         error, actualLength, 0L, 0L);
    pageMetaData.setCrawlTimestamp(crawlTimestamp);
    pageMetaData.setReferringInfo(referringUrls, referringTexts);
  }

  private final void readVersion3(DataInput dataInput) throws IOException {
    this.pageMetaData = (PageMetaData)MessageHelper.readPublishable(dataInput);
    this.localDir = MessageHelper.readString(dataInput);
    this.localName = MessageHelper.readString(dataInput);
  }


  public String toString() {
    final LineBuilder result = new LineBuilder();

    final String theLocalName = (localDir == null) ? localName : localDir + "/" + localName;

    result.
      append(pageMetaData.getUrl()).
      append(theLocalName).
      append(pageMetaData.getActualLength()).
      append(pageMetaData.getCrawlTimestamp()).
      append(pageMetaData.getContentType()).
      append(pageMetaData.getContentLength()).
//referringUrls
//referringTexts
      append(pageMetaData.getTitleText()).
      append(pageMetaData.getMetaKeywords()).
      append(pageMetaData.getMetaDescription()).
      append(pageMetaData.getResponseCode()).
      append(pageMetaData.getResponseMessage()).
      append(pageMetaData.getError());

    return result.toString();
  }
}
