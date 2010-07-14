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
package org.sd.xml;


import org.sd.io.FileUtil;
import org.sd.util.tree.Tree;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Set;

/**
 * Utility class to rip tags from xml without pre-interpreting the full xml
 * structure.
 * <p>
 * @author Spence Koehler
 */
public class XmlTagRipper implements Iterator<XmlLite.Tag> {

  private boolean hitEnd;
  private XmlInputStream xmlInputStream;
  private boolean commonCase;
  private XmlLite.Tag next;
  private boolean curHitEncodingException;
  private boolean nextHitEncodingException;
  private XmlLite.Tag lastTag;
  private boolean didFirst;

  private XmlTagParser xmlTagParser;
  private Set<String> ignoreTags;

  /**
   * Construct an instance to rip text from the given xml file.
   */
  public XmlTagRipper(String filename) throws IOException {
    this(filename, false, null);
  }

  /**
   * Construct an instance to rip text from the given file, ignoring
   * certain tags.
   *
   * @param filename      The name of the file containing xml text to
   *                      rip.
   * @param isHtml        True if file is html and special script logic
   *                      is to be applied.
   * @param ignoreTags    Tags to ignore and skip.
   */
  public XmlTagRipper(String filename, boolean isHtml, Set<String> ignoreTags) throws IOException {
    this(FileUtil.getFile(filename), isHtml, ignoreTags);
  }

  /**
   * Construct an instance to rip text from the given xml file.
   */
  public XmlTagRipper(File xmlFile) throws IOException {
    this(xmlFile, false, null);
  }

  /**
   * Construct an instance to rip text from the given file, ignoring
   * certain tags.
   *
   * @param xmlFile       The file containing xml text to rip.
   * @param isHtml        True if file is html and special script logic
   *                      is to be applied.
   * @param ignoreTags    Tags to ignore and skip.
   */
  public XmlTagRipper(File xmlFile, boolean isHtml, Set<String> ignoreTags) throws IOException {
    this(FileUtil.getInputStream(xmlFile), isHtml, ignoreTags);
  }

  /**
   * Construct an instance to rip text from the given input stream, ignoring
   * certain tags.
   *
   * @param inputStream   The stream containing xml text to rip.
   * @param isHtml        True if file is html and special script logic
   *                      is to be applied.
   * @param ignoreTags    Tags to ignore and skip.
   */
  public XmlTagRipper(InputStream inputStream, boolean isHtml, Set<String> ignoreTags) throws IOException {
    this(inputStream, isHtml, ignoreTags, false);
  }

  /**
   * Construct an instance to rip text from the given input stream, ignoring
   * certain tags.
   *
   * @param inputStream   The stream containing xml text to rip.
   * @param isHtml        True if file is html and special script logic
   *                      is to be applied.
   * @param ignoreTags    Tags to ignore and skip.
   * @param requireXmlTag true if finding an xml tag in the stream is required.
   *                      NOTE: if this is true and an xml tag is not found, the ripper will behave
   *                      as if an encoding exception was hit and will appear empty.
   */
  public XmlTagRipper(InputStream inputStream, boolean isHtml, Set<String> ignoreTags, boolean requireXmlTag) throws IOException {
    this.hitEnd = false;
    this.xmlInputStream = new XmlInputStream(inputStream);
    this.commonCase = isHtml;
    if (requireXmlTag && !xmlInputStream.foundXmlTag()) {
      this.hitEnd = true;
      xmlInputStream.close();
      this.curHitEncodingException = true;
    }
    else {
      xmlInputStream.setThrowEncodingException(false);
      this.curHitEncodingException = false;
      this.nextHitEncodingException = false;
      this.lastTag = null;
      this.didFirst = false;
      this.xmlTagParser = isHtml ? XmlFactory.HTML_TAG_PARSER_IGNORE_COMMENTS : XmlFactory.XML_TAG_PARSER_IGNORE_COMMENTS;
      this.ignoreTags = ignoreTags;

      this.next = getNextTag();
    }
  }

  public boolean hasNext() {
    computeNextIfNeeded();
    return (this.next != null);
  }

  public void close() {
    if (!hitEnd) {
      this.hitEnd = true;
      try {
        xmlInputStream.close();
      }
      catch (IOException e) {
        throw new IllegalStateException(e);
      }
    }
  }

  private final void computeNextIfNeeded() {
    if (next == null && (lastTag != null || !didFirst)) {
      next = getNextTag();
      lastTag = null;
      didFirst = true;

      if (next == null) {
        close();
      }
    }
  }

  public XmlLite.Tag next() {
    computeNextIfNeeded();

    final XmlLite.Tag result = next;
    this.lastTag = next;
    this.curHitEncodingException = nextHitEncodingException;
    this.next = null;

    return result;
  }

  public void remove() {
    //do nothing.
  }

  /**
   * Report whether the last string returned by "next" hit an encoding
   * exception during parsing.
   */
  public boolean hitEncodingException() {
    return curHitEncodingException;
  }

  /**
   * Rip the full node whose root is the tag last returned by "next" using
   * the given xmlLite instance to parse.
   * <p>
   * NOTE: After ripping a node, iteration will continue after the node,
   *       not after the last returned tag. There is a buffer limit of
   *       32K such that if in reading to the next tag this size is
   *       exceeded, the node cannot be ripped.
   */
  public Tree<XmlLite.Data> ripNode(XmlLite xmlLite) {
    Tree<XmlLite.Data> result = null;

    if (lastTag != null) {
      // can now read to the end of the node.
      result = readNode(lastTag, xmlLite);
    }

    return result;
  }

  /**
   * Rip the text under the current tag using default html parsing.
   */
  public String ripText() {
    return ripText(XmlFactory.HTML_LITE_IGNORE_COMMENTS);
  }

  /**
   * Rip the text under the current tag unsing the given parsing.
   */
  public String ripText(XmlLite xmlLite) {
    String result = null;

    final Tree<XmlLite.Data> node = ripNode(xmlLite);
    if (node != null) {
      result = XmlTreeHelper.getAllText(node);
    }

    return result;
  }

  private final XmlLite.Tag getNextTag() {
    if (hitEnd) return null;
    XmlLite.Tag result = readNextTag();

    // see if we hit the end.
    if (result == null) {
      // close xmlInputStream!
      close();
    }

    this.nextHitEncodingException = xmlInputStream.hitEncodingException(true);

    return result;
  }

  /**
   * Read through the next tag.
   */
  private final XmlLite.Tag readNextTag() {
    XmlLite.Tag result = null;
    final StringBuilder text = null;
    final StringBuilder tags = new StringBuilder();

    try {
      while (result == null) {
        // read up to an open tag
        if (xmlInputStream.readToChar('<', text, -1) < 0) {
          // no more open tags
          break;
        }

        // read tag
        final XmlTagParser.TagResult tagResult = xmlTagParser.readTag(xmlInputStream, tags, false, commonCase);

        if (tagResult != null) {
          if (tagResult.hasTag()) {
            result = tagResult.getTag();
          }
          if (tagResult.hitEndOfStream()) {
            break;  // hit end of stream. time to go.
          }
        }
      }
    }
    catch (IOException e) {
      throw new IllegalStateException(e);
    }

    return result;
  }

  /**
   * Given that we have just read the given tag from the stream, read in the
   * xml node of this tag.
   * <p>
   * NOTE: This must be called before "hasNext" in order to get the node
   *       for the tag last returned by "next".
   */
  private final Tree<XmlLite.Data> readNode(XmlLite.Tag tag, XmlLite xmlLite) {
    Tree<XmlLite.Data> result = new Tree<XmlLite.Data>(tag);
    tag.setContainer(result);

    if (!tag.isSelfTerminating()) {
      try {
        xmlLite.readFullNode(xmlInputStream, result);
      }
      catch (IOException e) {
        result = null;
      }
    }

/*
    while (true) {
      try {
        final Tree<XmlLite.Data> child = xmlLite.getNextChild(xmlInputStream);
        if (child != null) {
          result.addChild(child);
        }
        else {
          // time to quit.
          break;
        }
      }
      catch (IOException e) {
        // time to quit.
        result = null;
        break;
      }
    }
*/

    return result;
  }

  // dump the html file text paths, processed incrementally.
  public static void main(String[] args) throws IOException {
    final XmlTagRipper ripper = new XmlTagRipper(args[0], true, null);

    // dump each string
    while (ripper.hasNext()) {
      final XmlLite.Tag tag = ripper.next();

      if (ripper.hitEncodingException()) {
        System.err.println("***WARNING: hitEncodingException!:\n" + tag + "\n");
      }

      System.out.println(tag);
    }
  }
}
