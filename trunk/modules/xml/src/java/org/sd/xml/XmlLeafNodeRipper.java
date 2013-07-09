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
import java.io.InputStream;
import java.io.IOException;
import java.util.List;
import java.util.Set;

/**
 * Utility class to rip only non-empty leaf text nodes from xml without
 * pre-interpreting the full xml structure.
 *
 * @author Spence Koehler
 */
public class XmlLeafNodeRipper implements XmlRipper {

  private XmlTextRipper textRipper;
  private TagStack tagStack;
  private int index;
  private boolean useTagEquivalents = false;

  public XmlLeafNodeRipper(InputStream inputStream) throws IOException {
    this(inputStream, true, HtmlHelper.DEFAULT_IGNORE_TAGS, false, null);
  }

  /**
   * Construct a default instance suitable for processing html.
   */
  public XmlLeafNodeRipper(InputStream inputStream, 
                           boolean isHtml, 
                           Set<String> ignoreTags, 
                           boolean keepEmpties, 
                           String[] tagsToSave) 
    throws IOException 
  {
    this(inputStream, isHtml, false, false, ignoreTags, keepEmpties, tagsToSave);
  }
  public XmlLeafNodeRipper(InputStream inputStream, 
                           boolean isHtml, boolean isValidated, boolean useTagEquiv,
                           Set<String> ignoreTags, 
                           boolean keepEmpties, 
                           String[] tagsToSave) 
    throws IOException 
  {
    this.textRipper = new XmlTextRipper(inputStream, isHtml,
                                        isHtml ? (isValidated ? new ValidatingHtmlTagStack(useTagEquiv) : 
                                                                new HtmlTagStack(useTagEquiv)) : 
                                                 new XmlTagStack(useTagEquiv),
                                        isHtml ? XmlFactory.HTML_TAG_PARSER_IGNORE_COMMENTS : 
                                                 XmlFactory.XML_TAG_PARSER_IGNORE_COMMENTS,
                                        ignoreTags, tagsToSave, 
                                        keepEmpties, false, useTagEquiv);
    this.useTagEquivalents = useTagEquiv;
    this.tagStack = null;
    this.index = -1;
  }
  
  public boolean hasNext() {
    return textRipper.hasNext();
  }

  public Tree<XmlLite.Data> next() {
    Tree<XmlLite.Data> result = null;
    this.tagStack = null;
    ++index;

    final String text = textRipper.next();
    if (text != null) {
      result = new Tree<XmlLite.Data>(new XmlLite.Text(text));
      result.getData().setContainer(result);
    }

    return result;
  }

  public void remove() {
    textRipper.remove();
  }

  /**
   * Get the current (last next element's) tag stack.
   */
  public TagStack getTagStack() {
    if (tagStack == null) {
      final List<XmlLite.Tag> tags = textRipper.getTags();
      if (tags != null) {
        final List<XmlLite.Tag> savedTags = textRipper.getSavedTags();
        tagStack = new ImmutableTagStack(tags, savedTags, useTagEquivalents);
      }
    }
    return tagStack;
  }

  /**
   * Get the index of the last node returned from next.
   */
  public int getIndex() {
    return index;
  }

  /**
   * Close this ripper, cleanly disposing of open references, etc.
   */
  public void close() {
    textRipper.close();
  }

  /**
   * Determine whether the stream has been read to its end.
   */
  public boolean finishedStream() {
    return textRipper.finishedStream();
  }
}
