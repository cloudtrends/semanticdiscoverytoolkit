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

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Container for crawled page meta data.
 * <p>
 * @author Spence Koehler
 */
public class PageMetaData extends PersistablePublishable {
  
  //
  //NOTE: If member variables are changed here or in subclasses, update
  //      CURRENT_VERSION and keep track of deserializing old persisted
  //      instances!
  //
  private static final int CURRENT_VERSION = 2;

  private String url;
  private int actualLength;
  private long crawlTimestamp;
  private String contentType;
  private int contentLength;
  private long lastModified;
  private List<String> referringUrls;
  private List<String> referringTexts;
  private String titleText;
  private String metaKeywords;
  private String metaDescription;
  private int responseCode;
  private String responseMessage;
  private String error;
  private long downloadHeaderTime;
  private long downloadContentTime;

  /**
   * Default constructor for publishable reconstruction.
   */
  public PageMetaData() {
  }

  public PageMetaData(String url, int responseCode, String responseMessage,
                      String contentType, int contentLength, long lastModified,
                      String referringUrl, String referringLinkText,
                      String titleText, String metaKeywords, String metaDescription,
                      String error, int actualLength,
                      long downloadHeaderTime, long downloadContentTime) {
    this.url = url;
    this.actualLength = actualLength;
    this.crawlTimestamp = System.currentTimeMillis();
    this.contentType = contentType;
    this.contentLength = contentLength;
    this.lastModified = lastModified;
    this.responseCode = responseCode;
    this.responseMessage = responseMessage;
    this.titleText = titleText;
    this.metaKeywords = metaKeywords;
    this.metaDescription = metaDescription;
    this.error = error;
    this.downloadHeaderTime = downloadHeaderTime;
    this.downloadContentTime = downloadContentTime;

    this.referringUrls = null;
    this.referringTexts = null;

    addReferringInfo(referringUrl, referringLinkText);
  }

  /**
   * Update the entry.
   *
   * @return true if there was a change; otherwise, false.
   */
  public boolean update(CrawledPage crawledPage) {
    boolean result = false;

    if (actualLength != crawledPage.getActualLength()) {
      actualLength = crawledPage.getActualLength();
      result = true;
    }

    if (crawlTimestamp != crawledPage.getTimestamp()) {
      crawlTimestamp = crawledPage.getTimestamp();
      result = true;
    }

    if (changed(contentType, crawledPage.getContentType())) {
      contentType = crawledPage.getContentType();
      result = true;
    }

    if (contentLength != crawledPage.getContentLength()) {
      contentLength = crawledPage.getContentLength();
      result = true;
    }

    if (lastModified != crawledPage.getLastModified()) {
      lastModified = crawledPage.getLastModified();
      result = true;
    }

    result |= addReferringInfo(crawledPage.getReferringUrl(), crawledPage.getReferringLinkText());

    if (changed(titleText, crawledPage.getTitle())) {
      titleText = crawledPage.getTitle();
      result = true;
    }

    if (changed(metaKeywords, crawledPage.getMetaKeywords())) {
      metaKeywords = crawledPage.getMetaKeywords();
      result = true;
    }

    if (changed(metaDescription, crawledPage.getMetaDescription())) {
      metaDescription = crawledPage.getMetaDescription();
      result = true;
    }

    if (responseCode != crawledPage.getResponseCode()) {
      responseCode = crawledPage.getResponseCode();
      result = true;
    }

    if (changed(responseMessage, crawledPage.getResponseMessage())) {
      responseMessage = crawledPage.getResponseMessage();
      result = true;
    }

    // only update error if something else changed.
    final Exception cperror = crawledPage.getError();
    final String errorString = cperror == null ? null : cperror.toString();
    if (result && changed(error, errorString)) {
      error = errorString;
      result = true;
    }

    return result;
  }

  private final boolean changed(String myString, String pageString) {
    boolean result =
      (myString == null && pageString != null) ||
      (myString != null && pageString == null);

    if (!result && myString != null) {
      result = !myString.equals(pageString);
    }

    return result;
  }

  public long getAge() {
    final long curtime = System.currentTimeMillis();
    return curtime - crawlTimestamp;
  }


  public void setReferringInfo(List<String> referringUrls, List<String> referringTexts) {
    this.referringUrls = referringUrls;
    this.referringTexts = referringTexts;
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public int getActualLength() {
    return actualLength;
  }

  public long getCrawlTimestamp() {
    return crawlTimestamp;
  }

  public void setCrawlTimestamp(long timestamp) {
    this.crawlTimestamp = timestamp;
  }

  public String getContentType() {
    return contentType;
  }

  public int getContentLength() {
    return contentLength;
  }

  public long getLastModified() {
    return lastModified;
  }

  public List<String> getReferringUrls() {
    return referringUrls;
  }

  public List<String> getReferringTexts() {
    return referringTexts;
  }

  public String getReferringUrl() {
    String result = null;

    if (referringUrls != null && referringUrls.size() > 0) {
      result = referringUrls.get(0);
    }

    return result;
  }

  public String getReferringText() {
    String result = null;

    if (referringTexts != null && referringTexts.size() > 0) {
      result = referringTexts.get(0);
    }

    return result;
  }

  public String getTitleText() {
    return this.titleText;
  }

  public String getMetaKeywords() {
    return this.metaKeywords;
  }

  public String getMetaDescription() {
    return this.metaDescription;
  }

  public int getResponseCode() {
    return responseCode;
  }

  public String getResponseMessage() {
    return responseMessage;
  }

  public void setError(String error) {
    this.error = error;
  }

  public String getError() {
    return error;
  }

  public long getDownloadHeaderTime() {
    return downloadHeaderTime;
  }

  public void setDownloadContentTime(long downloadContentTime) {
    this.downloadContentTime = downloadContentTime;
  }

  public long getDownloadContentTime() {
    return downloadContentTime;
  }


  public final boolean addReferringInfo(String referringUrl, String referringText) {
    boolean result = false;

    // don't add nulls, just empties.
    if (referringUrl == null) referringUrl = "";
    if (referringText == null) referringText = "";

    if ("".equals(referringUrl) && "".equals(referringText)) return result;

    result = true;

    // add only if the pair doesn't exist
    if (referringUrls != null && referringTexts != null) {
      int index = 0;
      for (String refUrl : referringUrls) {
        if (referringUrl.equals(refUrl)) {
          final String refText = referringTexts.get(index);
          if (referringText.equals(refText)) {
            result = false;
            break;
          }
        }
        ++index;
      }
    }

    if (result) {
      if (referringUrls == null) referringUrls = new ArrayList<String>();
      if (referringTexts == null) referringTexts = new ArrayList<String>();

      referringUrls.add(referringUrl);
      referringTexts.add(referringText);
    }

    return result;
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
    MessageHelper.writeString(dataOutput, url);
    dataOutput.writeInt(actualLength);
    dataOutput.writeLong(crawlTimestamp);
    MessageHelper.writeString(dataOutput, contentType);
    dataOutput.writeInt(contentLength);
    dataOutput.writeLong(lastModified);

    if (referringUrls == null) {
      dataOutput.writeInt(-1);
    }
    else {
      dataOutput.writeInt(referringUrls.size());
      for (String referringUrl : referringUrls) {
        MessageHelper.writeString(dataOutput, referringUrl);
      }
    }

    if (referringTexts == null) {
      dataOutput.writeInt(-1);
    }
    else {
      dataOutput.writeInt(referringTexts.size());
      for (String referringText : referringTexts) {
        MessageHelper.writeString(dataOutput, referringText);
      }
    }

    MessageHelper.writeString(dataOutput, titleText);
    MessageHelper.writeString(dataOutput, metaKeywords);
    MessageHelper.writeString(dataOutput, metaDescription);
    dataOutput.writeInt(responseCode);
    MessageHelper.writeString(dataOutput, responseMessage);
    MessageHelper.writeString(dataOutput, error);

    dataOutput.writeLong(downloadHeaderTime);
    dataOutput.writeLong(downloadContentTime);
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
    if (version == 1 || version == 2) {
      readVersion1(dataInput, version);
    }
    else {
      badVersion(version);
    }
  }

  private final void readVersion1(DataInput dataInput, int version) throws IOException {
    this.url = MessageHelper.readString(dataInput);
    this.actualLength = dataInput.readInt();
    this.crawlTimestamp = dataInput.readLong();
    this.contentType = MessageHelper.readString(dataInput);
    this.contentLength = dataInput.readInt();
    this.lastModified = dataInput.readLong();

    final int referringUrlsLen = dataInput.readInt();
    if (referringUrlsLen >= 0) {
      this.referringUrls = new ArrayList<String>();
      for (int i = 0; i < referringUrlsLen; ++i) {
        this.referringUrls.add(MessageHelper.readString(dataInput));
      }
    }

    final int referringTextsLen = dataInput.readInt();
    if (referringTextsLen >= 0) {
      this.referringTexts = new ArrayList<String>();
      for (int i = 0; i < referringTextsLen; ++i) {
        this.referringTexts.add(MessageHelper.readString(dataInput));
      }
    }

    this.titleText = MessageHelper.readString(dataInput);
    this.metaKeywords = MessageHelper.readString(dataInput);
    this.metaDescription = MessageHelper.readString(dataInput);
    this.responseCode = dataInput.readInt();
    this.responseMessage = MessageHelper.readString(dataInput);
    this.error = MessageHelper.readString(dataInput);

    // for version 2, we only added the download times
    if (version == 2) {
      this.downloadHeaderTime = dataInput.readLong();
      this.downloadContentTime = dataInput.readLong();
    }
    else {
      this.downloadHeaderTime = 0L;
      this.downloadContentTime = 0L;
    }
  }
}
