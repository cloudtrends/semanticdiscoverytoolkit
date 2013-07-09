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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Utility class to rip text from xml without pre-interpreting the full xml
 * structure.
 * <p>
 * @author Spence Koehler
 */
public class XmlTextRipper implements Iterator<String> {

  /**
   * Convenience factory method to build a tag-preserving xml ripper over html
   * data.
   */
  public static final XmlTextRipper buildHtmlRipper(File htmlFile) throws IOException {
    return buildHtmlRipper(htmlFile, false);
  }

  /**
   * Convenience factory method to build a tag-preserving xml ripper over html
   * data.
   */
  public static final XmlTextRipper buildHtmlRipper(File htmlFile, boolean keepEmpties) throws IOException {
    return buildHtmlRipper(htmlFile, keepEmpties, false);
  }

  /**
   * Convenience factory method to build a tag-preserving xml ripper over html
   * data.
   */
  public static final XmlTextRipper buildHtmlRipper(File htmlFile, boolean keepEmpties, boolean requireXmlTag) throws IOException {
    return new XmlTextRipper(FileUtil.getInputStream(htmlFile), true, new HtmlTagStack(), 
                             XmlFactory.HTML_TAG_PARSER_IGNORE_COMMENTS, 
                             HtmlHelper.DEFAULT_IGNORE_TAGS,
                             new String[] {"meta"}, keepEmpties, requireXmlTag, false);
  }

  public static final XmlTextRipper buildXmlRipper(File xmlFile, boolean keepEmpties) throws IOException {
    return new XmlTextRipper(FileUtil.getInputStream(xmlFile), false, new XmlTagStack(),
                             XmlFactory.XML_TAG_PARSER_IGNORE_COMMENTS, null, null,
                             keepEmpties);
  }

  private boolean hitEnd;
  private XmlInputStream xmlInputStream;
  private String next;
  private boolean curHitEncodingException;
  private boolean nextHitEncodingException;
  private MutableTagStack tagStack;
  private XmlTagParser xmlTagParser;
  private Set<String> ignoreTags;
  private List<XmlLite.Tag> curTags;  // tags from just above text up to the root.
  private TagStack curTagStack;
  private XmlLite.Tag nextTag;
  private String popTag;
  private boolean finishedStream;
  private Set<String> tagsToSave;
  private List<XmlLite.Tag> savedTags;
  private boolean keepEmpties;
  private boolean sawBeginTag;
  private boolean commonCase;
  private boolean useTagEquivalents;

  /**
   * Construct an instance to rip text from the given xml file without
   * keeping track of tags.
   */
  public XmlTextRipper(String filename, boolean commonCase) throws IOException {
    this(FileUtil.getInputStream(filename), commonCase, null, null, null, null, false);
  }

  /**
   * Construct an instance to rip text from the given xml file without
   * keeping track of tags.
   */
  public XmlTextRipper(File xmlFile, boolean commonCase) throws IOException {
    this(FileUtil.getInputStream(xmlFile), commonCase, null, null, null, null, false);
  }

  /**
   * Construct an instance to rip text from the given xml file and keep
   * track of tags over the text.
   *
   * @param inputStream   The input stream for the xml data.
   * @param commonCase    Whether tags should be treated as common-cased.
   * @param tagStack      The tag stack to use to keep track of tags.
   *                      If null, then no tags will be kept; but if
   *                      xmlTagParser is non-null then tags will be
   *                      parsed (skipped) according to the tag parser's
   *                      rules.
   * @param xmlTagParser  The xml tag parser to use to parse tags.
   *                      If null, no tags will be kept.
   * @param ignoreTags    Tags to ignore the text under. Only applied
   *                      when tagStack and xmlTagParser are non-null.
   * @param tagsToSave    Tags (by name) to save when encountered.
   * @param keepEmpties   True to return nodes with empty text. This allows
   *                      tags like embed, for example, to be captured;
   *                      otherwise, only non-empty text nodes will be
   *                      returned.
   */
  public XmlTextRipper(InputStream inputStream, boolean commonCase, 
                       MutableTagStack tagStack, XmlTagParser xmlTagParser,
                       Set<String> ignoreTags, String[] tagsToSave, boolean keepEmpties) 
    throws IOException 
  {
    this(inputStream, commonCase, 
         tagStack, xmlTagParser, 
         ignoreTags, tagsToSave, keepEmpties, false, false);
  }

  /**
   * Construct an instance to rip text from the given xml file and keep
   * track of tags over the text.
   *
   * @param inputStream   The input stream for the xml data.
   * @param commonCase    Whether tags should be treated as common-cased.
   * @param tagStack      The tag stack to use to keep track of tags.
   *                      If null, then no tags will be kept; but if
   *                      xmlTagParser is non-null then tags will be
   *                      parsed (skipped) according to the tag parser's
   *                      rules.
   * @param xmlTagParser  The xml tag parser to use to parse tags.
   *                      If null, no tags will be kept.
   * @param ignoreTags    Tags to ignore the text under. Only applied
   *                      when tagStack and xmlTagParser are non-null.
   * @param tagsToSave    Tags (by name) to save when encountered.
   * @param keepEmpties   True to return nodes with empty text. This allows
   *                      tags like embed, for example, to be captured;
   *                      otherwise, only non-empty text nodes will be
   *                      returned.
   * @param requireXmlTag true if finding an xml tag in the stream is required.
   *                      NOTE: if this is true and an xml tag is not found, the ripper will behave
   *                      as if an encoding exception was hit and will appear empty.
   * @param useTagEquiv   true to use equivalent tag for comparison instead of reference copy
   *                      if this flag is set, the tag stack will create a copy, instead
   *                      of a direct reference
   */
  public XmlTextRipper(InputStream inputStream, boolean commonCase, 
                       MutableTagStack tagStack, XmlTagParser xmlTagParser,
                       Set<String> ignoreTags, String[] tagsToSave, 
                       boolean keepEmpties, boolean requireXmlTag, boolean useTagEquiv) 
    throws IOException 
  {
    this.hitEnd = false;
    this.xmlInputStream = new XmlInputStream(inputStream);
    this.commonCase = commonCase;
    if (requireXmlTag && !xmlInputStream.foundXmlTag()) {
      this.hitEnd = true;
      xmlInputStream.close();
      this.curHitEncodingException = true;
    }
    else {
      xmlInputStream.setThrowEncodingException(false);
      this.curHitEncodingException = false;
      this.nextHitEncodingException = false;
      this.tagStack = tagStack;
      this.xmlTagParser = xmlTagParser;
      this.ignoreTags = ignoreTags;
      this.curTags = null;
      this.curTagStack = null;
      this.nextTag = null;
      this.popTag = null;
      this.finishedStream = false;
      this.tagsToSave = null;
      this.savedTags = null;
      this.keepEmpties = keepEmpties;
      this.sawBeginTag = false;
      this.useTagEquivalents = useTagEquiv;

      if (tagsToSave != null) {
        this.tagsToSave = new HashSet<String>();
        for (String tagToSave : tagsToSave) {
          this.tagsToSave.add(tagToSave);
        }
        this.savedTags = new ArrayList<XmlLite.Tag>();
      }

      this.next = getNextString();
    }
  }

  public boolean hasNext() {
    return (this.next != null);
  }

  public boolean finishedStream() {
    return finishedStream;
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

  public String next() {
    final String result = next;
    this.curHitEncodingException = nextHitEncodingException;
    if (tagStack != null) {
      if(!useTagEquivalents)
        this.curTags = tagStack.getTags();
      else
      {
        List<XmlLite.Tag> tags = tagStack.getTags();
        if(tags == null)
          this.curTags = null;
        else
        {
          this.curTags = new ArrayList<XmlLite.Tag>();
          for(XmlLite.Tag tag : tags)
            this.curTags.add(new XmlLite.Tag(tag));
        }
      }
    }
    this.curTagStack = null;

    if (result != null) {
      this.next = getNextString();
      if (next == null) close();
    }

    return result;
  }

  public void remove() {
    //do nothing.
  }

  /**
   * Get the tags over the text of the last string returned by "next"
   * if this instance is configured to preserve tags. Tags are ordered
   * from root to text.
   *
   * @return the tags or null.
   */
  public List<XmlLite.Tag> getTags() {
    return curTags;
  }

  /**
   * Get the tag stack over the text of the last string returned by "next"
   * if this instance is configured to preserve tags.
   *
   * @return the tag stack or null.
   */
  public TagStack getTagStack() {
    if (curTagStack == null && curTags != null) {
      this.curTagStack = new ImmutableTagStack(curTags, getSavedTags(), useTagEquivalents);
    }
    return curTagStack;
  }

  /**
   * Get the tags that have been saved while parsing the xml.
   * <p>
   * NOTE: Even ignored tags can/will be saved.
   * <p>
   * NOTE: If there are no tagsToSave or if no tag to save has been encountered
   *       yet while iterating over the xml, then the result will be null; 
   *       otherwise, all instances of tags that match a tag to save since the
   *       last retrieval will be returned.
   *
   * @return saved tags or null.
   */
  public List<XmlLite.Tag> getSavedTags() {
    List<XmlLite.Tag> result = null;

    if (savedTags != null && savedTags.size() > 0) {
      result = new ArrayList<XmlLite.Tag>(savedTags);
      savedTags.clear();
    }

    return result;
  }

  /**
   * Report whether the last string returned by "next" hit an encoding
   * exception during parsing.
   */
  public boolean hitEncodingException() {
    return curHitEncodingException;
  }

  private final String getNextString() {
    if (hitEnd) return null;
    String result = readNextString();

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
  private final String readNextString() {
    String result = null;
    final StringBuilder text = new StringBuilder();
    final StringBuilder tags = (tagStack != null) ? new StringBuilder() : null;

    boolean savePopForNextTime = false;
    if (nextTag != null) {
      tagStack.pushTag(nextTag);
      if (keepEmpties && nextTag.isSelfTerminating()) {
        savePopForNextTime = true;
        result = "";
      }
      nextTag = null;
    }
    if (popTag != null) {
      if (!savePopForNextTime) {
        tagStack.popTag(popTag);
        popTag = null;
      }
    }

    try {
      while (result == null) {
        // read up to an open tag
        boolean done = false;
        if (xmlInputStream.readToChar('<', text, -1) < 0) {
          // no more open tags
          done = true;
        }

        // we're at an open tag or the end of stream with text accumulating in text
        final String curText = text.toString().trim();
        if (curText.length() > 0) {
          if (tagStack != null && (tagStack.hasTag(ignoreTags) >= 0)) {
            // found text, but must ignore. act like we didn't find any.
            text.setLength(0);
          }
          else {
            // collected non-empty text, so we can be done (after reading to close tag)
            result = XmlLite.fixText(curText);
          }
        }

        if (done) break;  // can go now, there will be no close tag

        // read over tag
        if (xmlTagParser == null) {
          if (xmlInputStream.readToChar('>', null, -1) < 0) {
            // hit end of stream. time to go.
            break;
          }
        }
        else {
          final XmlTagParser.TagResult tagResult = xmlTagParser.readTag(xmlInputStream, tags, false, commonCase);

          if (tagStack != null && tagResult != null) {
            if (tagResult.hasTag() && !tagResult.hasScript() && !tagResult.hasStyle()) {
              final XmlLite.Tag theTag = tagResult.getTag();

              sawBeginTag = true;

              // save tag if warranted
              if (tagsToSave != null && tagsToSave.contains(theTag.name)) {
                savedTags.add(theTag);
              }

              if (result == null) {
                // didn't find any usable text yet, so this is a preTag
                tagStack.pushTag(theTag);
              }
              else {
                // this is the tag after the text, so it'll apply the next go'round.
                nextTag = theTag;
              }
            }

            if (tagResult.hasEndTag()) {
              final String endTag = tagResult.getEndTag();
              if (result == null) {
                if (keepEmpties && sawBeginTag) {
                  popTag = endTag;
                  result = "";
                }
                else {
                  // didn't find any usable text yet, so we can pop now.
                  tagStack.popTag(endTag);
                }
              }
              else {
                // found usable text, so it's not time to pop yet.
                popTag = endTag;
              }

              sawBeginTag = false;
            }

            if (tagResult.hitEndOfStream()) {
              this.finishedStream = true;
              break;  // hit end of stream. time to go.
            }
          }
        }
      }
    }
    catch (IOException e) {
      throw new IllegalStateException(e);
    }

    return result;
  }

  public static final String buildPathString(String text, List<XmlLite.Tag> tags) {
    final StringBuilder result = new StringBuilder();

    for (XmlLite.Tag tag : tags) {
      if (result.length() > 0) result.append('.');
      result.append(tag.name);
    }
    if (result.length() > 0) result.append('.');
    result.append("text=").append(text);

    return result.toString();
  }

  // dump the html file text paths, processed incrementally.
  public static void main(String[] args) throws IOException {
    final XmlTextRipper ripper = new XmlTextRipper(FileUtil.getInputStream(args[0]), true, 
                                                   new HtmlTagStack(), 
                                                   XmlFactory.HTML_TAG_PARSER_IGNORE_COMMENTS, 
                                                   HtmlHelper.DEFAULT_IGNORE_TAGS, 
                                                   null, true);

    // dump each string
    while (ripper.hasNext()) {
      final String text = ripper.next();
      final List<XmlLite.Tag> tags = ripper.getTags();

      if (ripper.hitEncodingException()) {
        System.err.println("***WARNING: hitEncodingException!:\n" + text + "\n");
      }

      System.out.println(buildPathString(text, tags));
    }
  }
}
