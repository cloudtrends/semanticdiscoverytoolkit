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
package org.sd.extract;


import org.sd.xml.TagStack;
import org.sd.xml.XmlLite;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class to organize extracted headings and relate them to text.
 * <p>
 * @author Spence Koehler
 */
public class HeadingOrganizer {
  
  private List<Extraction> extractedHeadings;
  private Map<Integer, IndexHeadingStack> pathIndex2Headings;
  private Map<String, Extraction> key2colHeading;
  private Map<String, Extraction> key2rowHeading;
  private IndexHeadingStack lastHeadingStack;

  /**
   * Construct with heading extractions.
   * <p>
   * Heading extractions are typically generated through a HeadingExtractor and
   * collected regardless of pathKey (i.e.: through
   * extractionResults.getExtractions(headingExtractor.getExtractionType())).
   */
  public HeadingOrganizer(List<Extraction>  extractedHeadings) {
    if (extractedHeadings != null) {
      Collections.sort(extractedHeadings, DocumentOrderExtractionComparator.getInstance());
      this.extractedHeadings = extractedHeadings;
    }
    else {
      this.extractedHeadings = null;
    }
    this.pathIndex2Headings = new HashMap<Integer, IndexHeadingStack>();
    this.key2colHeading = null;
    this.key2rowHeading = null;

    init();
  }

  private final void init() {
    if (extractedHeadings == null) {
      this.lastHeadingStack = null;
      return;
    }

    LinkedList<Extraction> curHeadings = null;
    int lastPathIndex = 0;

    for (Extraction e : extractedHeadings) {
      final String pathKey = e.getDocText().getPathKey();

      if (isKeyHeading(pathKey)) {
        initKeyHeading(e, pathKey);
      }
      else {
        // init index heading
        final int pathIndex = e.getDocText().getPathIndex();
        final double weight = e.getWeight();

        if (curHeadings == null || (weight < curHeadings.peekLast().getWeight())) {
          // map indices from lastPathIndex (inclusive) to pathIndex (exclusive) to the current list.
          if (curHeadings != null) {
            mapHeadings(curHeadings, lastPathIndex + 1, pathIndex + 1);
            lastPathIndex = pathIndex;
          }

          // tack a new or lower weight onto the end.
          if (curHeadings == null) curHeadings = new LinkedList<Extraction>();
        }
        else {
          mapHeadings(curHeadings, lastPathIndex + 1, pathIndex);
          lastPathIndex = pathIndex;

          // A higher or equal weighted heading trumps the last heading.
          curHeadings.removeLast();
          while (curHeadings.size() > 0 && curHeadings.peekLast().getWeight() <= weight) {
            curHeadings.removeLast();
          }

          mapHeadings(curHeadings, pathIndex, pathIndex + 1);

        }
        curHeadings.addLast(e);
      }
    }

    // instead of mapping all remaining indexes, just keep the 'last' once.
    this.lastHeadingStack = new IndexHeadingStack(curHeadings, lastPathIndex, -1);
  }

  // A key heading path is a path that ends in ".tdN" (where N is 1 or more digits) just
  // before the tag that makes the heading.
  private static final Pattern KEY_HEADING_PATTERN = Pattern.compile("^.*\\.td\\d+\\.[^\\.]+$");

  private final boolean isKeyHeading(String pathKey) {
    final Matcher m = KEY_HEADING_PATTERN.matcher(pathKey);
    return m.matches();
  }

  private final void initKeyHeading(Extraction e, String pathKey) {
    final boolean isColHeading = isColHeading(e);
    pathKey = pathKey.substring(0, pathKey.lastIndexOf('.'));  // lop off the 'heading' tag.
    //note: could also get this from e.getDocText().getTagStack().toString() if we wanted.

    if (isColHeading) {
      if (key2colHeading == null) key2colHeading = new HashMap<String, Extraction>();
      key2colHeading.put(pathKey, e);
    }
    else {
      if (key2rowHeading == null) key2rowHeading = new HashMap<String, Extraction>();
      key2rowHeading.put(pathKey, e);
    }
  }

  private final boolean isColHeading(Extraction e) {
    boolean result = true;

    // determine whether col (row == 0) or row (row > 0) heading
    // by walking up the tag stack looking for the first tr.
    final XmlLite.Tag trTag = findTag(e, "tr");
    if (trTag != null) {
      result = (trTag.getChildNum() == 0);
    }

    return result;
  }

  private static final XmlLite.Tag findTag(Extraction e, String tagName) {
    XmlLite.Tag result = null;

    final DocText docText = e.getDocText();

    final TagStack tagStack = docText.getTagStack();
    final List<XmlLite.Tag> tags = tagStack.getTags();
    final int ntags = tags.size();

    for (int i = ntags - 1; i >= 0; --i) {
      final XmlLite.Tag tag = tags.get(i);
      if (tagName.equals(tag.name)) {
        result = tag;
        break;
      }
    }

    return result;
  }

  private final int mapHeadings(List<Extraction> headings, int startIndex, int endIndex) {
    final List<Extraction> theHeadings = new ArrayList<Extraction>(headings);  // make a copy
    final IndexHeadingStack headingStack = new IndexHeadingStack(theHeadings, startIndex, endIndex);
    for (int i = startIndex; i < endIndex; ++i) {
      pathIndex2Headings.put(i, headingStack);
    }
    return endIndex;
  }

  public boolean hasKeyHeading(String pathKey) {
    if (key2colHeading == null && key2rowHeading == null) return false;

    boolean result = false;

    if (key2colHeading != null) {
      result = key2colHeading.containsKey(pathKey);
    }

    if (!result && key2rowHeading != null) {
      result = key2rowHeading.containsKey(pathKey);
    }

    return result;
  }

  /**
   * Get the extracted headings for the given text from top (highest/heaviest)
   * to bottom (deepest/lightest).
   *
   * @return the headings or null.
   */
  public IndexHeadingStack getHeadings(DocText docText) {
    if (this.lastHeadingStack == null) return null;

    IndexHeadingStack result = null;

    final int pathIndex = docText.getPathIndex();
    if (pathIndex >= lastHeadingStack.getStartIndex()) {
      result = lastHeadingStack;
    }
    else {
      result = pathIndex2Headings.get(docText.getPathIndex());
    }

    return result;
  }

  public KeyHeadingStack getKeyHeadings(String pathKey) {
    if (key2colHeading == null && key2rowHeading == null) return null;

    //build a heading stack based on key headings that exist for portions of the path key.
    KeyHeadingStack result = null;

    // loop up to first tdN+, up to next tdN+, ...
    for (int tdIndex = nextTdPostIndex(pathKey, -1); tdIndex >= 0; tdIndex = nextTdPostIndex(pathKey, tdIndex)) {
      final String key = pathKey.substring(0, tdIndex);

      if (key2rowHeading != null) {
        final Extraction rowHeading = key2rowHeading.get(key);
        if (rowHeading != null) {
          if (result == null) result = new KeyHeadingStack(getHeadings(rowHeading.getDocText()));
          result.addRowHeading(rowHeading);
        }
      }
      if (key2colHeading != null) {
        final Extraction colHeading = key2colHeading.get(key);
        if (colHeading != null) {
          if (result == null) result = new KeyHeadingStack(getHeadings(colHeading.getDocText()));
          result.addColHeading(colHeading);
        }
      }
    }

    return result;
  }

  // return index of '.' following a valid td tag in the path, where
  // a valid td tag is td followed by 1 or more digits and a period or
  // end of string.
  //
  // return -1 if not found.
  private final int nextTdPostIndex(String pathKey, int lastTdIndex) {
    int result = -1;
    int tdIndex = pathKey.indexOf(".td", lastTdIndex + 1);

    if (tdIndex >= 0) {
      boolean gotDigit = false;
      char c = (char)0;
      final int len = pathKey.length();
      tdIndex += 3;

      while (tdIndex < len) {
        c = pathKey.charAt(tdIndex);
        if (c == '.') {
          break;
        }
        else if (c < '0' || c > '9') {
          break;
        }
        else {
          gotDigit = true;
        }
        ++tdIndex;
      }
      if (gotDigit && (tdIndex == len || c == '.')) {
        result = tdIndex;
      }
    }

    return result;
  }

  /**
   * Container class to hold a stack of headings.
   */
  public static abstract class HeadingStack {
    private List<Extraction> headings;  // ordered from highest to lowest

    protected HeadingStack(List<Extraction> headings) {
      this.headings = headings;
    }

    protected HeadingStack(HeadingStack headingStack) {
      if (headingStack == null || headingStack.getHeadings() == null) {
        this.headings = new ArrayList<Extraction>();
      }
      else {
        // copy stack's headings
        this.headings = new ArrayList<Extraction>(headingStack.getHeadings());
      }
    }

    protected void addHeading(Extraction h) {
      headings.add(h);
    }

    /**
     * Get the headings in this stack ordered from highest to lowest.
     */
    public List<Extraction> getHeadings() {
      return headings;
    }

    /**
     * Get the headings as a semi-colon delimited string ordered from highest
     * to lowest.
     * <p>
     * Note: Any semi-colons in the heading text will be replaced with commas.
     */
    public String getHeadingsString() {
      if (headings == null) return "";

      final StringBuilder result = new StringBuilder();

      for (Extraction heading : headings) {
        if (result.length() > 0) result.append(';');
        result.append(heading.getText().replaceAll(";", ","));
      }

      return result.toString();
    }
  }

  /**
   * Container class to hold a stack of headings with their range of influence.
   */
  public static final class IndexHeadingStack extends HeadingStack {
    private int startIndex;  // last heading's index
    private int endIndex;    // next heading stack's index, -1 for no more stacks

    /**
     * Construct a heading stack whose range of influence is between startIndex
     * (exclusive) and endIndex (exclusive).
     * <p>
     * Note that startIndex is the pathIndex of the last heading in the stack.
     */
    public IndexHeadingStack(List<Extraction> headings, int startIndex, int endIndex) {
      super(headings);
      this.startIndex = startIndex;
      this.endIndex = endIndex;
    }

    /**
     * Get the start index for this heading stack's range of influence.
     * <p>
     * NOTE: This will be the path index of the lowest heading in the stack.
     */
    public int getStartIndex() {
      return startIndex;
    }

    /**
     * Get the end index for this heading stack's range of influence.
     * <p>
     * NOTE: This will be the path index of the next heading stack's top
     *       heading, or indicates the end of the document if -1.
     */
    public int getEndIndex() {
      return endIndex;
    }
  }

  /**
   * Heading stack for key headings including the key headings' index
   * headings.
   */
  public static final class KeyHeadingStack extends HeadingStack {
    private XmlLite.Tag tag;  // encompassing tag for this stack

    public KeyHeadingStack(IndexHeadingStack headingStack) {
      super(headingStack);
      this.tag = null;
    }

    public void addRowHeading(Extraction h) {
      super.addHeading(h);

      if (tag == null) {
        // grab the heading's row tag
        this.tag = findTag(h, "tr");
      }
    }

    public void addColHeading(Extraction h) {
      super.addHeading(h);

      if (tag == null) {
        // grab the heading's table tag
        this.tag = findTag(h, "table");
      }
    }

    /**
     * Get the tag that encompasses this stack's data.
     */
    public XmlLite.Tag getTag() {
      return tag;
    }
  }
}
