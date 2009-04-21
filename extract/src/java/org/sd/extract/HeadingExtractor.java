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

import org.sd.nlp.Normalizer;
import org.sd.util.tree.Tree;
import org.sd.xml.TagStack;
import org.sd.xml.XmlLite;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * An extractor for html headings.
 * <p>
 * @author Spence Koehler
 */
public class HeadingExtractor implements Extractor {
  
  public static final String EXTRACTION_TYPE = "HtmlHeading";

  public static final Integer getHeadingProperty(DocText docText) {
    return docText.getXmlData().getHeadingStrength();

/*
    Integer result = null;

    final String stringValue = docText.getProperty(EXTRACTION_TYPE);
    if (stringValue != null) {
      result = new Integer(stringValue);
    }

    return result;
*/
  }

  public static final void setHeadingProperty(DocText docText, Integer headingStrength) {
    docText.getXmlData().setHeadingStrength(headingStrength);

    if (headingStrength == null) {
      // unset property
      docText.setProperty(EXTRACTION_TYPE, null);
    }
    else {
      docText.setProperty(EXTRACTION_TYPE, Integer.toString(headingStrength));
    }
  }

  /**
   * Determine whether the strength to test is under the base strength.
   * <p>
   * A strength is under another if it is strictly less than the other.
   * <p>
   * Note that 'null' is considered to be 'zero' and 'zero' is considered
   * to be strictly less than 'zero'.
   *
   * @return true if the strengthToTest is 'under' the baseStrength.
   */
  public static final boolean isUnder(Integer strengthToTest, Integer baseStrength) {
    boolean result = false;

    final int base = (baseStrength == null) ? 0 : baseStrength;
    final int strength = (strengthToTest == null) ? 0 : strengthToTest;

    return (base == 0) ? (strength <= base) : (strength < base);
  }

  /** Any text found under these tags are considered headings. */
  public static final String[] DEFAULT_HEADING_TAGS = new String[] {
    // since these are also non_consecutive_text_tags, they  are always headings
    "title", "h1", "h2", "h3", "h4", "h5", "h6", "hr", "thead", "th",

    // these are only considered as heading when found over non consecutive text
    "font@size", "em", "b", "strong"
  };

  /**
   * Default mappings from heading values to heading 'strength':
   * <p>
   *        h       h1   h2   h3        h4   h5  h6  <p>
   *   size-N        6    5    4    3    2    1   0  <p>
   *   size-%      200  150  120  100   80   70  60  <p>
   *                hr                    thead  th  <p>
   *                                             em  <p>
   *                                         strong  <p>
   *                                              b  <p>
   *          title
   * strength   7    6    5    4    0    3    2   1  <p>
   * 
   */
  public static final Map<String, Integer> VALUE_TO_STRENGTH = new HashMap<String, Integer>();
  static {
    VALUE_TO_STRENGTH.put("h1", 6);
    VALUE_TO_STRENGTH.put("h2", 5);
    VALUE_TO_STRENGTH.put("h3", 4);
    VALUE_TO_STRENGTH.put("h4", 3);
    VALUE_TO_STRENGTH.put("h5", 2);
    VALUE_TO_STRENGTH.put("h6", 1);
    VALUE_TO_STRENGTH.put("hr", 6);

    VALUE_TO_STRENGTH.put("200%", 6);
    VALUE_TO_STRENGTH.put("150%", 5);
    VALUE_TO_STRENGTH.put("120%", 4);
    VALUE_TO_STRENGTH.put("100%", 0);
    VALUE_TO_STRENGTH.put( "80%", 3);
    VALUE_TO_STRENGTH.put( "70%", 2);
    VALUE_TO_STRENGTH.put( "60%", 1);

//     VALUE_TO_STRENGTH.put("6", 6);
//     VALUE_TO_STRENGTH.put("5", 5);
//     VALUE_TO_STRENGTH.put("4", 4);
// //    VALUE_TO_STRENGTH.put("3", 0);
// //    VALUE_TO_STRENGTH.put("2", 3);
// //    VALUE_TO_STRENGTH.put("1", 2);

    VALUE_TO_STRENGTH.put("+3", 6);
    VALUE_TO_STRENGTH.put("+2", 5);
    VALUE_TO_STRENGTH.put("+1", 4);
    VALUE_TO_STRENGTH.put("1", 0);
    VALUE_TO_STRENGTH.put("-1", 0);
    VALUE_TO_STRENGTH.put("-2", 0);

    VALUE_TO_STRENGTH.put("thead", 0);
    VALUE_TO_STRENGTH.put("th", 0);
    VALUE_TO_STRENGTH.put("em", 0);
    VALUE_TO_STRENGTH.put("strong", 0);
    VALUE_TO_STRENGTH.put("b", 0);

    VALUE_TO_STRENGTH.put("title", 7);
  }

  private static final HeadingExtractor DEFAULT_INSTANCE = new HeadingExtractor();

  /**
   * Get the default heading extractor instance.
   */
  public static final HeadingExtractor getDefaultInstance() {
    return DEFAULT_INSTANCE;
  }

  private Map<String, String> tag2attribute;  // i.e. font@size -> font -> size
  private Map<String, Integer> hVal2Strength;

  /**
   * Construct using default values.
   */
  private HeadingExtractor() {
    this(DEFAULT_HEADING_TAGS, VALUE_TO_STRENGTH);
  }

  /**
   * Construct using the given values.
   */
  public HeadingExtractor(String[] headingTags, Map<String, Integer> hVal2Strength) {
    this.tag2attribute = new HashMap<String, String>();
    this.hVal2Strength = hVal2Strength;

    for (String headingTag : headingTags) {
      final String[] tagAtt = headingTag.split("@");
      if (tagAtt.length == 2) {
        this.tag2attribute.put(tagAtt[0], tagAtt[1]);
      }
      else {
        this.tag2attribute.put(tagAtt[0], null);
      }
    }
  }

  /**
   * Get this extractor's type designator.
   */
  public String getExtractionType() {
    return EXTRACTION_TYPE;
  }

  /**
   * Determine whether this extractor needs the doc text's text container to
   * cache doc text instances.
   */
  public boolean needsDocTextCache() {
    return false;
  }

  /**
   * Determine whether the given doc text should be accepted for extraction.
   */
  public boolean shouldExtract(DocText docText) {
    boolean result = false;

    final Tree<XmlLite.Data> xmlNode = docText.getXmlNode();
    XmlLite.Tag theTag = null;

    if (xmlNode != null) {
      final XmlLite.Tag tag = xmlNode.getData().asTag();
      if (tag != null) {
        result = tag2attribute.containsKey(tag.name);
        if (result) theTag = tag;
      }
    }

    if (!result) {
      final TagStack tagStack = docText.getTagStack();
      final Integer strength = getHeadingStrength(tagStack);

      if (strength != null) {
        result = true;
        setHeadingProperty(docText, strength);
      }
    }

    return result;
  }

  /**
   * Perform the extraction on the doc text.
   * 
   * @param docText  The docText to extract from.
   * @param die      Trigger to halt processing. Needs to be monitored and
   *                 obeyed; but can also be set from within the implementation.
   *                 Use with care!
   *
   * @return one or more extractions or null.
   */
  public List<Extraction> extract(DocText docText, AtomicBoolean die) {

    // coordinated with shouldExtract. If we're here, we have a strength.

    final Integer strength = getHeadingProperty(docText);
    final List<Extraction> result = new ArrayList<Extraction>();
    result.add(new Extraction(getExtractionType(), docText, strength));

    return result;
  }

  /**
   * Get the heading strength.
   */
  public Integer getHeadingStrength(TagStack tagStack) {
    Integer result = null;
    XmlLite.Tag theTag = null;
    boolean found = false;

    final List<XmlLite.Tag> tags = tagStack.getTags();
    for (int index = tags.size() - 1; index >= 0 && !found; --index) {
      final XmlLite.Tag tag = tags.get(index);
      found = tag2attribute.containsKey(tag.name);
      if (found) theTag = tag;
    }

    if (theTag != null) {
      result = getHeadingStrength(theTag);
    }

    return result;
  }

  /**
   * Get the heading strength.
   */
  public Integer getHeadingStrength(XmlLite.Tag tag) {
    Integer result = null;

    final String att = tag2attribute.get(tag.name);  // check for attribute (i.e. font@size=)
    final String value = (att == null) ? tag.name : tag.getAttribute(att);
    if (value != null) {  // found attribute or using tag name
      result = hVal2Strength.get(value);
    }

    return result;
  }

  /**
   * Provide access to this extractor's normalizer.
   */
  public Normalizer getNormalizer() {
    return null;
  }

  /**
   * Determine whether this extractor hasfinished processing the doc text
   * in the pipeline.
   */
  public boolean isFinishedWithDocText(DocText docText) {
    return false;
  }

  /**
   * Determine whether this extractor is finished processing the doc texts's
   * document.
   */
  public boolean isFinishedWithDocument(DocText docText) {
    return false;
  }

  /**
   * Get this extractor's disambiguator.
   */
  public Disambiguator getDisambiguator() {
    return null;
  }

  public static void main(String[] args) throws IOException {
    // arg0: html file
    final ExtractionPipeline megaExtractor = ExtractionPipeline.buildDefaultHtmlPipeline(true, false, false, (Extractor[])null);
    
    final File arg0 = new File(args[0]);
    if (arg0.isDirectory()) {
      megaExtractor.mainRunner(arg0.listFiles(), HeadingExtractor.EXTRACTION_TYPE);
    } else {
      megaExtractor.mainRunner(args, HeadingExtractor.EXTRACTION_TYPE);
    }
  }
}
