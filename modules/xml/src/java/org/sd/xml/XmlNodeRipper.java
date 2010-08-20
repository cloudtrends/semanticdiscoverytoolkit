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
import java.util.Set;

/**
 * Utility class to rip complete text nodes from xml without pre-interpreting
 * the full xml structure.
 * <p>
 * A complete text node is a node that contains non-empty text up to an end
 * tag. For example, given "<a>Foo<b>Bar<c/></b>Baz</a>", "<a>" is a complete
 * text node containing text "Foo", "Bar", and "Baz"; alternatively, given
 * "<a>Foo</a><b>Bar</b><c/><a>Baz</a>", {"a", "b", "", and "a"} are the complete
 * text nodes.
 *
 * @author Spence Koehler
 */
public class XmlNodeRipper implements XmlRipper {

  private static final boolean DONT_IGNORE_EMPTIES = true;

  private boolean hitEnd;
  private XmlInputStream xmlInputStream;
  private boolean commonCase;
  private Tree<XmlLite.Data> next;
  private boolean curHitEncodingException;
  private boolean nextHitEncodingException;
  private Tree<XmlLite.Data> lastNode;
  private int index;  // index of ripped node.
  private boolean finishedStream;

  private MutableTagStack tagStack;
  private XmlTagParser xmlTagParser;
  private Set<String> ignoreTags;

  public XmlNodeRipper(String xmlFile, boolean isHtml, Set<String> ignoreTags) throws IOException {
    this(FileUtil.getFile(xmlFile), isHtml, ignoreTags);
  }

  public XmlNodeRipper(File xmlFile, boolean isHtml, Set<String> ignoreTags) throws IOException {
    this(xmlFile, isHtml, ignoreTags, false);
  }

  public XmlNodeRipper(File xmlFile, boolean isHtml, Set<String> ignoreTags, boolean requireXmlTag) throws IOException {
    this.hitEnd = false;
    final InputStream inputStream = FileUtil.getInputStream(xmlFile);
    init(inputStream, isHtml, ignoreTags, requireXmlTag);
  }

  public XmlNodeRipper(InputStream inputStream, boolean isHtml, Set<String> ignoreTags, boolean requireXmlTag) throws IOException {
    init(inputStream, isHtml, ignoreTags, requireXmlTag);
  }

  private final void init(InputStream inputStream, boolean isHtml, Set<String> ignoreTags, boolean requireXmlTag) throws IOException {
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
      this.lastNode = null;
      this.index = -1;
      this.finishedStream = false;
      this.tagStack = isHtml ? new HtmlTagStack() : new XmlTagStack();
      this.xmlTagParser = isHtml ? XmlFactory.HTML_TAG_PARSER_IGNORE_COMMENTS : XmlFactory.XML_TAG_PARSER_IGNORE_COMMENTS;
      this.ignoreTags = ignoreTags;
    }

    this.next = null;
  }

  public boolean hasNext() {
    if (hitEnd) return false;
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

  public Tree<XmlLite.Data> next() {
    computeNextIfNeeded();

    final Tree<XmlLite.Data> result = next;
    this.lastNode = next;
    this.curHitEncodingException = nextHitEncodingException;
    this.next = null;

    return result;
  }

  public void remove() {
    //do nothing.
  }

  public boolean hitEncodingException() {
    return curHitEncodingException;
  }

  /**
   * Get the index of the last node returned from next.
   */
  public int getIndex() {
    return index;
  }

  public TagStack getTagStack() {
    return new ImmutableTagStack(tagStack);
  }

  private final void computeNextIfNeeded() {
    if (next == null && (lastNode != null || (index == -1))) {
      next = getNextNode();
      lastNode = null;
      ++index;

      if (next == null) {
        close();
      }
    }
  }

  private final Tree<XmlLite.Data> getNextNode() {
    if (hitEnd) return null;
    Tree<XmlLite.Data> result = readNextNode();

    // see if we hit the end.
    if (result == null) {
      // close xmlInputStream!
      close();
    }

    this.nextHitEncodingException = xmlInputStream.hitEncodingException(true);

    return result;
  }

  /**
   * Read through the next closed off text.
   */
  private final Tree<XmlLite.Data> readNextNode() {
    Tree<XmlLite.Data> result = null;
    final StringBuilder text = new StringBuilder();
    final StringBuilder tags = new StringBuilder();
    
    try {
      while (true) {
        // read up to an open tag
        if (xmlInputStream.readToChar('<', text, -1) < 0) {
          // no more open tags
          break;
        }

        // read tag
        final XmlTagParser.TagResult tagResult = xmlTagParser.readTag(xmlInputStream, tags, false, commonCase);

        if (tagResult != null) {
          // if we've not found any text, this'll go on the tag stack; otherwise into the tree.
          final boolean hasIgnoreTag = hasIgnoreTag(result) || tagStack.hasTag(ignoreTags) >= 0;
          final String theText = hasIgnoreTag ? "" : XmlLite.fixText(text.toString());
          text.setLength(0);  // reset the text buffer

          if (theText.length() > 0) {
            if (result == null) {
              final XmlLite.Tag theTag = tagStack.popTag();
              if (theTag != null) {
                result = new Tree<XmlLite.Data>(theTag);
                theTag.setContainer(result);
              }
              // else, ignore text before the first open tag.
            }

            if (result != null) {
              Tree<XmlLite.Data> tchild = result.addChild(new XmlLite.Text(theText));
              tchild.getData().setContainer(tchild);
            }
          }

          if (tagResult.hasTag()) {
            final XmlLite.Tag theTag = tagResult.getTag();

            if (result == null && theText.length() == 0 && !theTag.isSelfTerminating()) {
              // didn't find anything usable yet, so this is a preTag
              tagStack.pushTag(theTag);
            }
            else {
              // this is an open tag after usable text, so it needs to be built into the tree.
              final boolean addEmpty = DONT_IGNORE_EMPTIES && theTag.isSelfTerminating();

              if (result == null) {
                if (theText.length() > 0 || DONT_IGNORE_EMPTIES) {
                  result = new Tree<XmlLite.Data>(theTag);
                  theTag.setContainer(result);
                
                  if (addEmpty) {
                    Tree<XmlLite.Data> child = result.addChild(new XmlLite.Text(""));
                    child.getData().setContainer(child);
                    break;
                  }
                }
              }
              else {
                if (addEmpty) {
                  final Tree<XmlLite.Data> child = result.addChild(theTag);
                  theTag.setContainer(child);
                  final Tree<XmlLite.Data> tchild = child.addChild(new XmlLite.Text(""));
                  tchild.getData().setContainer(tchild);
                }
                else {
                  result = result.addChild(theTag);
                  theTag.setContainer(result);
                }
              }
            }
          }

          if (tagResult.hasEndTag() && !tagResult.hasTag()) {
            // close off current child; don't stop 'til we end the root
            final String endTag = tagResult.getEndTag();
            if (result == null) {
              // didn't find any usable text yet, so we can pop now.
              final XmlLite.Tag theTag = tagStack.popTag(endTag);

              // if !ignoreEmpties, we should return a node with the last
              // tagStack tag; otherwise, just keep going.
/*
//NOTE: Leave this section commented out so that we dont' try to put it (or smthng like it) back in.
//      (or, better yet, create a test that fails if something like this is put back in!)
//Including this gives us an empty text for every end tag -- even when we have had text for the tag under other tags.
//Excluding this still gives us the behavior of getting empty text for elements we wish to extract from (like meta).
              if (DONT_IGNORE_EMPTIES) {
                // this'll let us capture i.e. META tag content.
                result = new Tree<XmlLite.Data>(theTag);
                result.addChild(new XmlLite.Text(""));
                break;
              }
*/
            }
            else {
              // found usable text, so it's time to close the text node and exit if we've closed up to the root.
              final Tree<XmlLite.Data> parent = XmlLite.findTag(result, endTag);
              if (parent == null || parent.getParent() == null) {
                break;  // result holds the root.
              }
              else {
                result = parent.getParent();  // keep going.
              }
            }
          }

          if (tagResult.hitEndOfStream()) {
            finishedStream = true;
            break;  // hit end of stream. time to go.
          }
        }
      }
    }
    catch (IOException e) {
      throw new IllegalStateException(e);
    }

    if (result != null && result.getParent() != null) result = result.getRoot();

    return result;
  }

  /**
   * Determine whether the stream has been read to its end.
   */
  public boolean finishedStream() {
    return finishedStream;
  }

  private final boolean hasIgnoreTag(Tree<XmlLite.Data> node) {
    boolean result = false;
    if (ignoreTags == null) return result;

    while (node != null) {
      final XmlLite.Tag tag = node.getData().asTag();
      if (tag != null && ignoreTags.contains(tag.name)) {
        result = true;
        break;
      }
      node = node.getParent();
    }

    return result;
  }

  // dump the html file text paths, processed incrementally.
  public static void main(String[] args) throws IOException {
    final XmlNodeRipper ripper = new XmlNodeRipper(args[0], true, HtmlHelper.DEFAULT_IGNORE_TAGS);

    // dump each string
    while (ripper.hasNext()) {
      final Tree<XmlLite.Data> node = ripper.next();
      final TagStack tagStack = ripper.getTagStack();

      if (ripper.hitEncodingException()) {
        System.err.println("***WARNING: hitEncodingException! :");
      }

      System.out.println("index=" + ripper.getIndex() +
                         ",stack=" + tagStack +
                         ", xml=" + XmlLite.asXml(node, false).replaceAll("\\s+", " ") +
                         ", text='" + XmlTreeHelper.getAllText(node) + "'");
    }

    ripper.close();
  }
}
