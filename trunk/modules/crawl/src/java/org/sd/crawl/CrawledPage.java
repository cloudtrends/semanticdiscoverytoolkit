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
import org.sd.util.tree.TraversalIterator;
import org.sd.util.tree.Tree;
import org.sd.xml.Encoding;
import org.sd.xml.XPathApplicator;
import org.sd.xml.XmlFactory;
import org.sd.xml.XmlLite;
import org.sd.xml.XmlTreeHelper;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Container for a crawled page.
 * <p>
 * @author Spence Koehler
 */
public class CrawledPage implements Publishable {

  private String url;
  private int responseCode;
  private String responseMessage;
  private String contentType;
  private int contentLength;
  private long lastModified;
  private String referringUrl;
  private String referringLinkText;
  private int tryNum;
  private String content;
  private Exception exception;
  private long timestamp;
  private File file;
  private boolean fromCache;
  private long downloadHeaderTime;
  private long downloadContentTime;

  private Tree<XmlLite.Data> xmlTree;
  private String title;
  private String keywords;
  private String description;
  private int actualLength;

  private PageMetaData _metaData;
  private List<Link> _links;
  private DetailedUrl _dUrl;

  private static final XPathApplicator XPA = new XPathApplicator();

  // Pattern to catch errors that come in the form:
  //   "java.io.IOException: Server returned HTTP response code: 403 for URL: http://..."
  private static final Pattern ERROR_CODE_PATTERN = Pattern.compile("^.*code: (\\d+)$");


  /**
   * Constructor for publishable reconstruction only.
   */
  public CrawledPage() {
  }

  public CrawledPage(String url,
                     int responseCode, String responseMessage,
                     String contentType, int contentLength, long lastModified,
                     String referringUrl, String referringLinkText, int tryNum,
                     long downloadHeaderTime) {
    this.url = url;                    // assume this is a clean url
    this.responseCode = responseCode;
    this.responseMessage = responseMessage;
    this.contentType = contentType;
    this.contentLength = contentLength;
    this.lastModified = lastModified;
    this.referringUrl = referringUrl;
    this.referringLinkText = referringLinkText;
    this.tryNum = tryNum;
    this.downloadHeaderTime = downloadHeaderTime;
    this.downloadContentTime = 0;
    this.content = null;
    this.exception = null;
    this.timestamp = System.currentTimeMillis();
    this.file = null;
    this.fromCache = false;
    this.xmlTree = null;

    if (referringUrl != null || referringLinkText != null) {
      if (referringUrl == null) referringUrl = "";
      if (referringLinkText == null) referringLinkText = "";
    }
    if ("".equals(referringUrl) && "".equals(referringLinkText)) {
      referringUrl = null;
      referringLinkText = null;
    }
  }

  public CrawledPage(File file, PageMetaData metaData) {
    init(metaData);
    this.fromCache = true;
    this.file = file;
  }

  private final void init(PageMetaData metaData) {
    this._metaData = metaData;

    if (metaData == null) return;

    this.url = metaData.getUrl();
    this.responseCode = metaData.getResponseCode();
    this.responseMessage = metaData.getResponseMessage();
    this.contentType = metaData.getContentType();
    this.contentLength = metaData.getContentLength();
    this.actualLength = metaData.getActualLength();
    this.lastModified = metaData.getLastModified();
    this.timestamp = metaData.getCrawlTimestamp();
    this.referringUrl = metaData.getReferringUrl();
    this.referringLinkText = metaData.getReferringText();
    this.downloadHeaderTime = metaData.getDownloadHeaderTime();
    this.downloadContentTime = metaData.getDownloadContentTime();

    this.content = null;
    this.exception = null;
    this.xmlTree = null;
    this.file = null;
    this.fromCache = false;

    this.title = metaData.getTitleText();
    this.keywords = metaData.getMetaKeywords();
    this.description = metaData.getMetaDescription();
  }
                     
  /**
   * Wrap a file as a crawled page.
   * <p>
   * Note that most of the non-content functionality of the class will be
   * disabled for the file.
   */
  public CrawledPage(File file) {
    this.file = file;
    this.responseCode = -1;
    this.timestamp = System.currentTimeMillis();
  }

  /**
   * Wrap an exception as a crawled page.
   * <p>
   * Note that most of the non-content functionality of the class will be
   * disabled for the instance.
   */
  public CrawledPage(String url, Exception exception) {
    this.url = url;
    this.exception = exception;
    this.responseCode = -1;
    this.timestamp = System.currentTimeMillis();

    if (exception != null) {
      final String estring = exception.toString();
      final Matcher m = ERROR_CODE_PATTERN.matcher(estring);
      if (m.matches()) {
        this.responseCode = Integer.parseInt(m.group(1));
      }
    }
  }

  public PageMetaData getMetaData() {
    if (_metaData == null) {
      _metaData = new PageMetaData(url, responseCode, responseMessage,
                                   contentType, contentLength, lastModified,
                                   referringUrl, referringLinkText,
                                   getTitle(), getMetaKeywords(), getMetaDescription(),
                                   exception == null ? null : exception.toString(),
                                   getActualLength(), downloadHeaderTime, downloadContentTime);
      _metaData.setCrawlTimestamp(timestamp);
    }
    return _metaData;
  }

  /**
   * Determine whether this page was retrieved from the cache (as opposed to
   * newly crawled).
   */
  public boolean isFromCache() {
    return fromCache;
  }

  /**
   * Dump this page's content to the file.
   * <p>
   * If originalContent is non-null, write it as this page's content;
   * otherwise, write the content from this page.
   */
  public void dumpContent(File toFile, String originalContent) throws IOException {
    if (content != null || originalContent != null) {
      final BufferedWriter writer = FileUtil.getWriter(toFile);
      if (originalContent != null) {
        writer.write(originalContent);
      }
      else if (xmlTree != null) {
        XmlLite.writeXml(xmlTree, writer);
      }
      else {
        writer.write(content);
      }
      writer.flush();
      writer.close();
    }
    this.file = toFile;
  }

  /**
   * Get this instance's file (if it has one).
   *
   * @return the file or null.
   */
  public File getFile() {
    return file;
  }

  /**
   * Get this instance's url.
   */
  public String getUrl() {
    return url;
  }

  public DetailedUrl getDetailedUrl() {
    if (_dUrl == null) {
      _dUrl = new DetailedUrl(url);
    }
    return _dUrl;
  }

  /**
   * Get this instance's response code.
   */
  public int getResponseCode() {
    return responseCode;
  }

  /**
   * Get this instance's response message.
   */
  public String getResponseMessage() {
    return responseMessage;
  }

  /**
   * Get the number of the try that succeeded in fetching this page.
   */
  public int getTryNum() {
    return tryNum;
  }

  /**
   * Get this instance's content type.
   */
  public String getContentType() {
    return contentType;
  }

  /**
   * Get the content length (as reported by server).
   */
  public int getContentLength() {
    return contentLength;
  }

  /**
   * Get this instance's actual content length.
   */
  public int getActualLength() {
    if (actualLength == 0 && hasContent()) {
      if (content != null) {
        actualLength = content.length();
      }
      else {
        actualLength = (int)file.length();
      }
    }

    return actualLength;
  }

  /**
   * Get the last modified time (as reported by server from which this page was retrieved.)
   */
  public long getLastModified() {
    return lastModified;
  }

  /**
   * Get this instance's referring url.
   */
  public String getReferringUrl() {
    return referringUrl;
  }

  /**
   * Get this instance's referring link text.
   */
  public String getReferringLinkText() {
    return referringLinkText;
  }

  public void setReferringInfo(String referringUrl, String referringText) {
    this.referringUrl = referringUrl;
    this.referringLinkText = referringText;
  }

  /**
   * Set this instance's content.
   */
  public void setContent(String content) {
    this.content = content;
  }

  /**
   * Set this instance's error.
   */
  public void setError(Exception exception) {
    this.exception = exception;

    if (_metaData != null) {
      _metaData.setError(exception == null ? null : exception.toString());
    }
  }

  public void setDownloadContentTime(long downloadContentTime) {
    this.downloadContentTime = downloadContentTime;

    if (_metaData != null) {
      _metaData.setDownloadContentTime(downloadContentTime);
    }
  }

  /**
   * Determine whether this instance has content.
   */
  public boolean hasContent() {
    return content != null || (file != null && file.exists() && file.length() > 0);
  }

  /**
   * Get this instance's content.
   */
  public String getContent() {
    String result = content;

    if (result == null && file != null && file.exists() && file.length() > 0) {
      try {
        content = FileUtil.readAsString(file, null);
      }
      catch (IOException e) {
        exception = e;
      }
    }

    return content;
  }

  /**
   * Determine whether this instance has an error.
   */
  public boolean hasError() {
    return exception != null;
  }
  
  /**
   * Get this instance's error.
   */
  public Exception getError() {
    return exception;
  }

  /**
   * Get the timestamp for this page.
   */
  public long getTimestamp() {
    return timestamp;
  }

  /**
   * Get an input stream over the crawled content, or null.
   */
  public InputStream getInputStream() throws IOException {
    InputStream result = null;

    if (content != null) {
      result = new ByteArrayInputStream(content.getBytes());
    }
    else if (file != null && file.exists() && file.length() > 0) {
      result = FileUtil.getInputStream(file);
    }

    return result;
  }

  /**
   * Normalize the hrefs in this page's content such that relative links are
   * made to be absolute.
   */
  public void normalizeHrefs() {
    final Tree<XmlLite.Data> xmlTree = getXmlTree();

    if (xmlTree != null) {
      final StringBuilder absoluteUrl = new StringBuilder();

      for (TraversalIterator<XmlLite.Data> iter = xmlTree.iterator(Tree.Traversal.DEPTH_FIRST); iter.hasNext(); ) {
        final Tree<XmlLite.Data> curNode = iter.next();
        final List<Tree<XmlLite.Data>> children = curNode.getChildren();
        if (children != null) {
          for (Tree<XmlLite.Data> child : children) {
            final XmlLite.Tag tag = child.getData().asTag();
            if (tag != null) {
              String tagAttribute = null;

              if ("a".equals(tag.name)) {
                // a href (linkText=text)
                tagAttribute = "href";
              }
              else if ("frame".equals(tag.name) || "iframe".equals(tag.name)) {
                // frame src (linkText=title att), iframe src (linkText=title att)
                tagAttribute = "src";
              }
              else if ("img".equals(tag.name)) {
                // img src (linkText=alt att)
                tagAttribute = "src";
              }
              else if ("link".equals(tag.name)) {
                // link href (linkText=rel att (i.e. "stylesheet")
                tagAttribute = "href";
              }
              else if ("script".equals(tag.name)) {
                // script src (linkText=language att (i.e. "javascript")
                tagAttribute = "src";
              }
              else if ("meta".equals(tag.name)) {
                final String httpEquiv = tag.getAttribute("http-equiv");
                if (httpEquiv != null && "refresh".equals(httpEquiv.toLowerCase())) {
                  final String content = tag.getAttribute("content");
                  final int urlPos = content.indexOf("url=");
                  if (urlPos >= 0) {
                    final String linkUrl = content.substring(urlPos + 4);
                    if (linkUrl != null && !"".equals(linkUrl)) {
                      if (absoluteUrl.length() > 0) absoluteUrl.setLength(0);
                      absoluteUrl.append(content.substring(0, urlPos + 4));
                      if (relativeToAbsolute(linkUrl, absoluteUrl)) {
                        tag.setAttribute("content", absoluteUrl.toString());
                      }
                    }
                  }
                }
              }
              else if ("embed".equals(tag.name)) {
                tagAttribute = "src";
              }

              if (tagAttribute != null) {
                final String linkUrl = tag.getAttribute(tagAttribute);
                if (linkUrl != null && !"".equals(linkUrl)) {
                  if (absoluteUrl.length() > 0) absoluteUrl.setLength(0);
                  if (relativeToAbsolute(linkUrl, absoluteUrl)) {
                    tag.setAttribute(tagAttribute, absoluteUrl.toString());
                  }
                }
              }
            }
          }
        }
      }
    }
  }

  /**
   * Get the xmlTree for this page.
   * <p>
   * Note that the tree is only available if there is content and no error.
   *
   * @return the xmlTree or null.
   */
  public Tree<XmlLite.Data> getXmlTree() {
    if (xmlTree == null && hasContent() && exception == null) {
      try {
        final InputStream inputStream = getInputStream();
        xmlTree = XmlFactory.readXmlTree(inputStream, Encoding.UTF8, true/*ignoreComments*/,  true/*htmlFlag*/, null, false);
        inputStream.close();
      }
      catch (IOException e) {
        exception = e;
      }
    }
    return xmlTree;
  }

  /**
   * Get this page's title.
   */
  public final String getTitle() {
    if (title == null && hasContent()) {
      if (true/*remove this when implement else*/ || content != null) {
        title = getXpathString("html.head.title");
      }
      else {
        //todo: use a ripper to get it from the file
      }
    }
    return title;
  }

  /**
   * Get this page's keywords.
   */
  public final String getMetaKeywords() {
    if (keywords == null && hasContent()) {
      if (true/*remove this when implement else*/ || content != null) {
        keywords = getXpathString("html.head.meta@keywords");
      }
      else {
        //todo: use a ripper to get it from the file
      }
    }
    return keywords;
  }

  /**
   * Get this page's description.
   */
  public final String getMetaDescription() {
    if (description == null && hasContent()) {
      if (true/*remove this when implement else*/ || content != null) {
        description = getXpathString("html.head.meta@description");
      }
      else {
        //todo: use a ripper to get it from the file
      }
    }
    return description;
  }

  /**
   * Get all text from matches to the given xpath, concatenated with a comma+space.
   */
  private final String getXpathString(String xpathString) {
    String result = null;

    final Tree<XmlLite.Data> xmlTree = getXmlTree();
    if (xmlTree != null) {
      result = XPA.getAllText(xpathString, xmlTree, ", ");
    }

    return result;
  }

  public String toString() {
    final LineBuilder result = new LineBuilder();

    result.
      append(url).
      append(responseCode).
      append(responseMessage).
      append(contentType).
      append(contentLength).
      append(lastModified).
      append(referringUrl).
      append(referringLinkText).
      append(exception == null ? "" : exception.toString()).
      append(timestamp).
      append(file == null ? "" : file.getName()).
      append(getTitle()).
      append(getMetaKeywords()).
      append(getMetaDescription()).
      append(actualLength);

    return result.toString();
  }

  /**
   * Write this message to the dataOutput stream such that this message
   * can be completely reconstructed through this.read(dataInput).
   *
   * @param dataOutput  the data output to write to.
   */
  public void write(DataOutput dataOutput) throws IOException {
    final PageMetaData metaData = getMetaData();
    MessageHelper.writePublishable(dataOutput, metaData);

    final String content = getContent();
    MessageHelper.writeString(dataOutput, content);

    dataOutput.writeBoolean(fromCache);
    MessageHelper.writeString(dataOutput, file == null ? null : file.getAbsolutePath());
    dataOutput.writeInt(tryNum);
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
    final PageMetaData metaData = (PageMetaData)MessageHelper.readPublishable(dataInput);
    init(metaData);

    this.content = MessageHelper.readString(dataInput);
    this.fromCache = dataInput.readBoolean();
    final String filename = MessageHelper.readString(dataInput);
    this.file = (filename == null) ? null : new File(filename);
    this.tryNum = dataInput.readInt();
  }

  /**
   * Get the links off of this crawled page.
   */
  public List<Link> getLinks() {
    if (_links == null) {
      _links = new ArrayList<Link>();

      final Tree<XmlLite.Data> xmlTree = getXmlTree();
      if (xmlTree != null) {
        for (TraversalIterator<XmlLite.Data> iter = xmlTree.iterator(Tree.Traversal.DEPTH_FIRST); iter.hasNext(); ) {
          final Tree<XmlLite.Data> curNode = iter.next();
          final List<Tree<XmlLite.Data>> children = curNode.getChildren();
          if (children != null) {
            for (Tree<XmlLite.Data> child : children) {
              final XmlLite.Tag tag = child.getData().asTag();
              if (tag != null) {
                String linkUrl = null;
                String linkText = null;
                int depthInc = 0;

                if ("a".equals(tag.name)) {
                  // a href (linkText=text)
                  linkUrl = tag.getAttribute("href");
                  linkText = XmlTreeHelper.getAllText(child);
                  depthInc = 1;
                }
                else if ("frame".equals(tag.name) || "iframe".equals(tag.name)) {
                  // frame src (linkText=title att), iframe src (linkText=title att)
                  linkUrl = tag.getAttribute("src");
                  linkText = tag.getAttribute("title");
                  // depthInc remains 0
                }
                else if ("img".equals(tag.name)) {
                  // img src (linkText=alt att)
                  linkUrl = tag.getAttribute("src");
                  linkText = tag.getAttribute("alt");
                  depthInc = 1;
                }
                else if ("link".equals(tag.name)) {
                  // link href (linkText=rel att (i.e. "stylesheet")
                  linkUrl = tag.getAttribute("href");
                  linkText = tag.getAttribute("rel");
                  // depthInc remains 0
                }
                else if ("script".equals(tag.name)) {
                  // script src (linkText=language att (i.e. "javascript")
                  linkUrl = tag.getAttribute("src");
                  linkText = tag.getAttribute("language");
                  // depthInc remains 0
                }
                else if ("meta".equals(tag.name)) {
                  final String httpEquiv = tag.getAttribute("http-equiv");
                  if (httpEquiv != null && "refresh".equals(httpEquiv.toLowerCase())) {
                    final String content = tag.getAttribute("content");
                    final int urlPos = content.indexOf("url=");
                    if (urlPos >= 0) {
                      linkUrl = content.substring(urlPos + 4);
                      linkText = httpEquiv;
                      // depthInc remains 0
                    }
                  }
                }
                else if ("embed".equals(tag.name)) {
                  linkUrl = tag.getAttribute("src");
                  linkText = "";
                }

                if (linkUrl != null && !"".equals(linkUrl)) {
                  _links.add(new Link(url, tag.name, linkUrl, linkText, depthInc));
                }
              }
            }
          }
        }
      }
    }
    return _links;
  }

  /**
   * If necessary, convert the href from relative to absolute according to
   * this page's url.
   * <p>
   * If the href is already absolute, then false will be returned and result
   * will be unchanged.
   *
   * @return true if converted and the absolute url is in result.
   */
  private final boolean relativeToAbsolute(String href, StringBuilder result) {
    final boolean isRelative = (href.indexOf(':') < 0);

    if (isRelative) {
      result.append(Link.buildUrl(url, href));
    }

    return isRelative;
  }
}
