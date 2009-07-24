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


import org.sd.extract.datetime.DateTimeInterpretation;
import org.sd.xml.TagStack;
import org.sd.xml.XmlLite;

import java.util.LinkedList;
import java.util.List;

/**
 * Container class for an extraction-based snippet.
 * <p>
 * @author Spence Koehler
 */
public class ExtractionSnippet {

  private LinkedList<DocText> texts;     // texts comprising the snippet.
  private List<Extraction> headings;     // headings to the snippet (in document order)
  private ExtractionGroup group;
  private Integer headingStrength;
  private Extraction primaryExtraction;
  private Extraction lastExtraction;
  private String structureKey;
  private int numNonEmptyTexts;

  private List<Post> _posts;

  public ExtractionSnippet(ExtractionGroup extractionGroup, DocText firstDocText) {
    this.texts = new LinkedList<DocText>();
    this.headings = getHeadings(firstDocText);

    this.group = extractionGroup;
    this.headingStrength = HeadingExtractor.getHeadingProperty(firstDocText);
    this.primaryExtraction = getExtraction(firstDocText, false, false);
    if (primaryExtraction != null) ExtractionProperties.setDateTimeProperties(primaryExtraction);
    this.lastExtraction = primaryExtraction;  // might be null.
    this.structureKey = primaryExtraction != null ? primaryExtraction.getStructureKey() : null;

    final String string = firstDocText.getString();
    this.numNonEmptyTexts = "".equals(string) ? 0 : 1;

    texts.add(firstDocText);
  }

  private ExtractionSnippet(ExtractionSnippet other, LinkedList<DocText> docTexts, Extraction primaryExtraction, int numNonEmptyTexts) {
    DocText firstDocText = null;
    for (DocText docText : docTexts) {
      if (!"".equals(docText.getString())) {
        firstDocText = docText;
        break;
      }
    }

    this.texts = docTexts;
    this.headings = getHeadings(firstDocText);
    this.group = other.group;
    this.headingStrength = HeadingExtractor.getHeadingProperty(firstDocText);
    this.primaryExtraction = primaryExtraction;
    if (primaryExtraction != null) ExtractionProperties.setDateTimeProperties(primaryExtraction);
    this.lastExtraction = primaryExtraction;
    this.structureKey = primaryExtraction.getStructureKey();
    this.numNonEmptyTexts = numNonEmptyTexts;
  }

  private final List<Extraction> getHeadings(DocText docText) {
    List<Extraction> result = null;

    final TextContainer textContainer = docText.getTextContainer();
    if (textContainer != null) {
      final HeadingOrganizer headingOrganizer = textContainer.getHeadingOrganizer();
      if (headingOrganizer != null) {
        final HeadingOrganizer.IndexHeadingStack headings = headingOrganizer.getHeadings(docText);
        if (headings != null) {
          result = headings.getHeadings();
        }
      }
    }

    return result;
  }

  public ExtractionSnippet addDocText(DocText docText) {
    ExtractionSnippet result = this;

    // add docText to this snippet if it is under the first docText's heading
    // as long as there is no primary extraction conflict and we don't cross
    // a table boundary.

    final Integer curHeadingStrength = HeadingExtractor.getHeadingProperty(docText);
    final Extraction extraction = getExtraction(docText, false, true);
    final String string = docText.getString();

    // always add an empty to this snippet
    if ("".equals(string)) {
      texts.add(docText);
    }
    // add in as long as there isn't a conflicting extraction
    else if (extraction != null && primaryExtraction != null) {
      // conflict -- can't add to this snippet! split into a new snippet.
      final LinkedList<DocText> nextTexts = new LinkedList<DocText>();
      int numNonEmptyNextTexts = 1;
      nextTexts.add(docText);

      final TagStack tagStack = docText.getTagStack();
      final TagStack lastTagStack = lastExtraction.getDocText().getTagStack();

      final int divergeIndex = tagStack.findFirstDivergentTag(lastTagStack);
      if (divergeIndex >= 0 && divergeIndex < tagStack.depth()) {
        final XmlLite.Data divergeTag = tagStack.getTag(divergeIndex);
        while (texts.size() > 0) {
          final DocText text = texts.removeLast();
          final TagStack curStack = text.getTagStack();
          final XmlLite.Data curTag = curStack.getTag(divergeIndex);
          if (divergeTag != curTag) {  // done looking
            texts.addLast(text);  // put it back
            break;                // and kick out
          }
          else {
            if (!"".equals(text.getString())) {
              --numNonEmptyTexts;
              ++numNonEmptyNextTexts;
            }
            nextTexts.addFirst(text);  // insert to front
          }
        }
      }
      lastExtraction = extraction;

      result = new ExtractionSnippet(this, nextTexts, extraction, numNonEmptyNextTexts);
    }
    else if (HeadingExtractor.isUnder(curHeadingStrength, headingStrength) ||
             primaryExtractionSeemsLikeHeadingTo(docText, curHeadingStrength)) {

      if (extraction != null) {
        if (structureKey == null) {
          structureKey = extraction.getStructureKey();
        }
        primaryExtraction = extraction;
        ExtractionProperties.setDateTimeProperties(extraction);

        lastExtraction = extraction;
      }
      texts.add(docText);
      if (!"".equals(string)) ++numNonEmptyTexts;
    }
    else {
      result = new ExtractionSnippet(group, docText);
    }

    return result;
  }

  private final Extraction getExtraction(DocText docText, boolean verifyPathKey, boolean verifyParseKey) {
    Extraction result = null;

    if (!verifyPathKey || group.acceptPathKey(docText.getPathKey())) {
      result = docText.getExtraction(group.getExtractionType());

      if (result != null && verifyParseKey && structureKey != null) {
        if (!structureKey.equals(result.getStructureKey())) {
          result = null;
        }
      }
    }
    return result;
  }

  private final boolean primaryExtractionSeemsLikeHeadingTo(DocText docText, Integer curHeadingStrength) {
    boolean result = false;

    if (primaryExtraction != null && numNonEmptyTexts == 1 && curHeadingStrength != null) {
      // the first doc text was the primary extraction and the next is a heading.
      result = true;
      this.headingStrength = curHeadingStrength;  // set this heading level for boundaries.
    }

    return result;
  }

  public List<Extraction> getHeadings() {
    return headings;
  }

  public List<DocText> getBody() {
    return texts;
  }

  public Extraction getPrimaryExtraction() {
    return primaryExtraction;
  }

  public DateTimeInterpretation getDateTimeInterpretation() {
    DateTimeInterpretation result = null;

    if (primaryExtraction != null) {
      result = (DateTimeInterpretation)primaryExtraction.getInterpretation();
    }

    return result;
  }

  public String toString() {
    final StringBuilder result = new StringBuilder();

    result.append(group.getExtractionType()).append(": ").append(primaryExtraction);

    for (Extraction heading : headings) {
      result.append("\n\t(H)\t").append(heading);
    }

    for (DocText text : texts) {
      String key = (primaryExtraction != null && primaryExtraction == getExtraction(text, false, false)) ? "(*)" : "   ";
      result.append("\n").append(key).append("\t").append(text);
    }

    return result.toString();
  }

  public List<Post> getPosts() {
    if (_posts == null) {
      _posts = divideIntoPosts();
    }
    return _posts;
  }

  private final List<Post> divideIntoPosts() {
    final List<Post> result = new LinkedList<Post>();
    Post curPost = null;
    Post lastPost = null;

    for (DocText text : texts) {
      if (curPost == null) {
        curPost = new Post(this, text);
      }
      else {
        curPost = curPost.addDocText(text);
      }

      if (lastPost != curPost) {
        result.add(curPost);
        lastPost = curPost;
      }
    }

    return result;
  }
}
