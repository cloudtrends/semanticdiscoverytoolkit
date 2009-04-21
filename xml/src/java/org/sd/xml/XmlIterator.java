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

/**
 * An iterator over 2nd level (root's children) xml elements.
 * <p>
 * This provides an incremental xml parser for processing xml data that are
 * too large to fit in memory all at once. It iterates over the children under
 * the root, holding each child in memory rather than the full xml tree.
 *
 * @author Spence Koehler
 */
public class XmlIterator implements Iterator<Tree<XmlLite.Data>> {

  private static final XmlLite XML_LITE = XmlFactory.XML_LITE_IGNORE_COMMENTS;  // ignore comments

  private boolean hitEnd;
  private XmlInputStream xmlInputStream;
  private Tree<XmlLite.Data> top;
  private String topTagName;
  private Tree<XmlLite.Data> next;
  private boolean curHitEncodingException;
  private boolean nextHitEncodingException;

  public XmlIterator(String filename) throws IOException {
    this(FileUtil.getFile(filename));
  }

  public XmlIterator(File file) throws IOException {
    this(file, false);
  }

  public XmlIterator(File file, boolean requireXmlTag) throws IOException {
    this.hitEnd = false;
    final InputStream inputStream = FileUtil.getInputStream(file);
    this.xmlInputStream = new XmlInputStream(inputStream);
    if (requireXmlTag && !xmlInputStream.foundXmlTag()) {
      this.hitEnd = true;
      xmlInputStream.close();
      this.curHitEncodingException = true;
    }
    else {
      xmlInputStream.setThrowEncodingException(false);
      this.top = XML_LITE.getTop(xmlInputStream);
      this.topTagName = top.getData().asTag().name;
      this.next = getNextChild();
      this.curHitEncodingException = false;
      this.nextHitEncodingException = false;
    }
  }

  public boolean hasNext() {
    return (this.next != null);
  }

  public Tree<XmlLite.Data> next() {
    final Tree<XmlLite.Data> result = next;
    this.curHitEncodingException = nextHitEncodingException;

    if (result != null) {
      this.next = getNextChild();
    }

    return result;
  }

  public void remove() {
    //do nothing.
  }

  public Tree<XmlLite.Data> getTop() {
    return top;
  }

  /**
   * Report whether the last element returned by "next" hit an encoding
   * exception during parsing.
   */
  public boolean hitEncodingException() {
    return curHitEncodingException;
  }

  private final Tree<XmlLite.Data> getNextChild() {
    if (hitEnd) return null;
    Tree<XmlLite.Data> result = readNextChild();

    // see if it looks like we hit the end.
    if (result != null && topTagName.equals(result.getData().asTag().name) && !result.hasChildren()) {

      // read one more to make sure.
      result = readNextChild();
      if (result == null || "".equals(result.getData().asTag().name)) {
        // yep, we're at the end.
        result = null;
      }
    }

    if (result == null) {
      // close xmlInputStream!
      this.hitEnd = true;
      try {
        xmlInputStream.close();
      }
      catch (IOException e) {
        throw new IllegalStateException(e);
      }
    }

    this.nextHitEncodingException = xmlInputStream.hitEncodingException(true);

    return result;
  }

  private final Tree<XmlLite.Data> readNextChild() {
    Tree<XmlLite.Data> result = null;

    try {
      result = XML_LITE.getNextChild(xmlInputStream);
    }
    catch (IOException e) {
      throw new IllegalStateException(e);
    }

    return result;
  }

  // dump the xml file, processed incrementally.
  public static void main(String[] args) throws IOException {
    final XmlIterator xmlIter = new XmlIterator(args[0]);

    final Tree<XmlLite.Data> top = xmlIter.getTop();
    final XmlLite.Tag topTag = top.getData().asTag();

    // dump the root
    System.out.println("<?xml version=\"1.0\"?>");
    System.out.print(topTag.toString());

    // dump each child
    while (xmlIter.hasNext()) {
      final Tree<XmlLite.Data> child = xmlIter.next();
      final String childString = XmlLite.asXml(child, false);

      if (xmlIter.hitEncodingException()) {
        System.err.println("***WARNING: hitEncodingException!:\n" + childString + "\n");
      }

      System.out.print(childString.trim());
    }

    // close the root.
    System.out.println("</" + topTag.name + ">");
  }
}
