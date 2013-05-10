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


import org.sd.util.tree.Tree;
import org.sd.xml.XmlLite;
import org.sd.util.StringUtil;
import org.sd.util.WordIterator;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A utility class to deal with html peculiarities.
 * <p>
 * @author Spence Koehler
 */
public class HtmlHelper {
  
  private static final boolean IGNORE_HEADINGS_456 = false;

  /**
   * Default tags (whose text) to ignore in the tree.
   */
  public static final String[] DEFAULT_IGNORE_TAG_STRINGS = new String[] {
    /*"meta", "link",*/ "style", "script", /*"form",*/ "select", "input", "option",
    "optgroup", "textarea", "button", "iframe", 
  };
  public static final Set<String> DEFAULT_IGNORE_TAGS = new HashSet<String>();
  static {
    for (String dits : DEFAULT_IGNORE_TAG_STRINGS) {
      DEFAULT_IGNORE_TAGS.add(dits);
    }
  }

//   /** Text found under these tags cannot be consecutive with the next text node. */
//   public static final String[] DEFAULT_NON_CONSECUTIVE_TEXT_TAGS = new String[] {
//     "title", "h1", "h2", "h3", "h4", "h5", "h6", "pre", "address", "code",
//     "li", "dt", "dd", "caption", "thead", "tfoot", "th"
//   };

  /**
   * Default tag names whose attributes should be ignored when creating
   * a key to match tags. All other tag attributes will be included in
   * generating a key.
   */
  public static final String[] DEFAULT_IGNORE_TAG_ATTRIBUTES = new String[] {
    "a", "image", "table", "tr", "td",
  };

  /** Any text found under these tags are considered headings. */
  public static final String[] DEFAULT_HEADING_TAGS = new String[] {
    // since these are also non_consecutive_text_tags, they  are always headings
    "title", "h1", "h2", "h3", "h4", "h5", "h6", "hr", "thead", "th",

    // these are only considered as heading when found over non consecutive text
    "font@size", "em", "b", "strong", "i", "a"
  };

  /** These tags may modify the heading strength if nested in another element. */
  public static final String[] CUMULATIVE_HEADING_STRENGTH_TAG_STRINGS = 
    new String[] 
  {
    "em", "b", "strong", "i", "a"
  };
  public static final Set<String> CUMULATIVE_HEADING_STRENGTH_TAGS = new HashSet<String>();
  static {
    for (String str : CUMULATIVE_HEADING_STRENGTH_TAG_STRINGS) {
      CUMULATIVE_HEADING_STRENGTH_TAGS.add(str);
    }
  }

  /** These tags are considered block-element. */
  public static final String[] DEFAULT_BLOCK_TAG_STRINGS = 
    new String[] 
  {
    "address",
    "blockquote",
    "dir",
    "div",
    "dl",
    "fieldset",
    "form",
    "h1",
    "h2",
    "h3",
    "h4",
    "h5",
    "h6",
    "hr",
    "isindex",
    "menu",
    "noframes",
    "noscript",
    "ol",
    //"p",
    "pre",
    "table",
    "ul",

    "html",
    "head",
    "body",

    //"dd",
    //"dt",
    "frameset",
    //"li",
    //"tbody",
    //"td",
    //"tfoot",
    //"th",
    //"thead",
    //"tr",

    //"applet",
    //"iframe",
    //"layer",
    //"legend",
    //"object",

    //"center",
  };
  public static final Set<String> DEFAULT_BLOCK_TAGS = new HashSet<String>();
  static {
    for (String str : DEFAULT_BLOCK_TAG_STRINGS) {
      DEFAULT_BLOCK_TAGS.add(str);
    }
  }

  /** These tags are considered block-element, but may appear inline */
  public static final String[] EXTENDED_BLOCK_TAG_STRINGS = 
    new String[] 
  {
    "center",
  };
  public static final Set<String> EXTENDED_BLOCK_TAGS = new HashSet<String>();
  static {
    for (String str : DEFAULT_BLOCK_TAG_STRINGS) {
      EXTENDED_BLOCK_TAGS.add(str);
    }
    for (String str : EXTENDED_BLOCK_TAG_STRINGS) {
      EXTENDED_BLOCK_TAGS.add(str);
    }
  }

  /** These tags are considered block-element. */
  public static final String[] DEFAULT_INLINE_TAG_STRINGS = 
    new String[] 
  {
    "a",
    "abbr",
    "acronym",
    "b",
    "basefont",
    "bdo",
    "big",
    "br",
    "cite",
    "code",
    "dfn",
    "em",
    "font",
    "i",
    "img",
    "input",
    "kbd",
    "label",
    "q",
    "s",
    "samp",
    "select",
    "small",
    "span",
    "strike",
    "strong",
    "sub",
    "sup",
    "textarea",
    "tt",
    "u",
    "var",
    
    "nobr",
  };
  public static final Set<String> DEFAULT_INLINE_TAGS = new HashSet<String>();
  static {
    for (String str : DEFAULT_INLINE_TAG_STRINGS) {
      DEFAULT_INLINE_TAGS.add(str);
    }
  }

  /** These tags are considered block-element. */
  public static final String[] DEFAULT_BLOCK_INLINE_TAG_STRINGS = 
    new String[] 
  {
    "applet",
    "button",
    "del",
    "iframe",
    "ins",
    "map",
    "object",
    "script",
  };
  public static final Set<String> DEFAULT_BLOCK_INLINE_TAGS = new HashSet<String>();
  static {
    for (String str : DEFAULT_BLOCK_INLINE_TAG_STRINGS) {
      DEFAULT_BLOCK_INLINE_TAGS.add(str);
    }
  }

  /** These tags are either block level elements or do not have a type. */
  public static final String[][] RESTRICTED_NESTING_TAGS = 
    new String[][]
  {
    {
      "a", 
      "tt", "i", "b", "big", "small", 
      "em", "strong", "dfn", "code", "samp", "kbd", "var", "cite", "abbr", "acronym", 
      /*"a",*/ "img", "object", "br", "script", "map", "q", "sub", "sup", "font", "span", "bdo",
      "input", "select", "textarea", "label", "button",
    },
  };
  public static final Map<String, Set<String>> RESTRICTED_NESTING_TAG_MAP = new HashMap<String, Set<String>>();
  static {
    for (String[] tags : RESTRICTED_NESTING_TAGS) 
    {
      String tag = tags[0];
      Set<String> allowed = new HashSet<String>();
      for(int i = 1; i < tags.length; i++)
        allowed.add(tags[i]);

      RESTRICTED_NESTING_TAG_MAP.put(tag, allowed);
    }
  }

  /** These tags are inline and can break text blocks */
  public static final String[] INLINE_BREAK_TAG_STRINGS = 
    new String[] { "br", "hr", };
  public static final Set<String> INLINE_BREAK_TAGS = new HashSet<String>();
  static {
    for (String str : INLINE_BREAK_TAG_STRINGS) {
      INLINE_BREAK_TAGS.add(str);
    }
  }

  public static final boolean 
    isNestingAllowed(XmlLite.Tag parent, XmlLite.Tag current) 
  {
    boolean result = true;

    if(parent == null || current == null)
      return true;

    // only inline tags allowed in other inline tags
    if("a".equals(parent.name) && "a".equals(current.name))
      result = false;
    else if(DEFAULT_INLINE_TAGS.contains(parent.name) && 
       DEFAULT_BLOCK_TAGS.contains(current.name))
      result = false;
    
    return result;
  }


  public static int MIN_USABLE_STRENGTH = 1;
  public static int MAX_STRENGTH = 7;

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
  public static final Map<String, Integer> VALUE_TO_STRENGTH_456 = new HashMap<String, Integer>();
  public static final Map<String, Integer> VALUE_TO_STRENGTH_NO_456 = new HashMap<String, Integer>();
  static {
    // if IGNORE_HEADINGS_456 == true
    VALUE_TO_STRENGTH_NO_456.put("h1", 6);
    VALUE_TO_STRENGTH_NO_456.put("h2", 5);
    VALUE_TO_STRENGTH_NO_456.put("h3", 4);
    VALUE_TO_STRENGTH_NO_456.put("h4", 3);
    VALUE_TO_STRENGTH_NO_456.put("h5", 2);
    VALUE_TO_STRENGTH_NO_456.put("h6", 1);
    VALUE_TO_STRENGTH_NO_456.put("hr", 6);
    
    VALUE_TO_STRENGTH_NO_456.put("200%", 6);
    VALUE_TO_STRENGTH_NO_456.put("150%", 5);
    VALUE_TO_STRENGTH_NO_456.put("120%", 4);
    VALUE_TO_STRENGTH_NO_456.put("100%", 0);
    VALUE_TO_STRENGTH_NO_456.put( "80%", 3);
    VALUE_TO_STRENGTH_NO_456.put( "70%", 2);
    VALUE_TO_STRENGTH_NO_456.put( "60%", 1);
    
    VALUE_TO_STRENGTH_NO_456.put("+3", 6);
    VALUE_TO_STRENGTH_NO_456.put("+2", 5);
    VALUE_TO_STRENGTH_NO_456.put("+1", 4);
    VALUE_TO_STRENGTH_NO_456.put("1", 0);
    VALUE_TO_STRENGTH_NO_456.put("-1", 2);
    VALUE_TO_STRENGTH_NO_456.put("-2", 1);
    VALUE_TO_STRENGTH_NO_456.put("7", 7);
    VALUE_TO_STRENGTH_NO_456.put("6", 6);
    VALUE_TO_STRENGTH_NO_456.put("5", 5);
    VALUE_TO_STRENGTH_NO_456.put("4", 3);
    VALUE_TO_STRENGTH_NO_456.put("3", 0);
    VALUE_TO_STRENGTH_NO_456.put("2", -1);
    //VALUE_TO_STRENGTH_NO_456.put("1", -2);
    
    VALUE_TO_STRENGTH_NO_456.put("thead", 2);
    VALUE_TO_STRENGTH_NO_456.put("th", 1);
    VALUE_TO_STRENGTH_NO_456.put("em", 1);
    VALUE_TO_STRENGTH_NO_456.put("i", 1);
    VALUE_TO_STRENGTH_NO_456.put("strong", 1);
    VALUE_TO_STRENGTH_NO_456.put("b", 1);
    VALUE_TO_STRENGTH_NO_456.put("a", 1);
    
    VALUE_TO_STRENGTH_NO_456.put("title", 7);

    // if IGNORE_HEADINGS_456 == false
    VALUE_TO_STRENGTH_456.put("h1", 6);
    VALUE_TO_STRENGTH_456.put("h2", 5);
    VALUE_TO_STRENGTH_456.put("h3", 4);
    VALUE_TO_STRENGTH_456.put("h4", 3);
    VALUE_TO_STRENGTH_456.put("h5", 2);
    VALUE_TO_STRENGTH_456.put("h6", 1);
    VALUE_TO_STRENGTH_456.put("hr", 6);
    
    VALUE_TO_STRENGTH_456.put("200%", 6);
    VALUE_TO_STRENGTH_456.put("150%", 5);
    VALUE_TO_STRENGTH_456.put("120%", 4);
    VALUE_TO_STRENGTH_456.put("100%", 0);
    VALUE_TO_STRENGTH_456.put( "80%", 3);
    VALUE_TO_STRENGTH_456.put( "70%", 2);
    VALUE_TO_STRENGTH_456.put( "60%", 1);
    
    VALUE_TO_STRENGTH_456.put("+3", 6);
    VALUE_TO_STRENGTH_456.put("+2", 5);
    VALUE_TO_STRENGTH_456.put("+1", 4);
    VALUE_TO_STRENGTH_456.put("1", 0);
    VALUE_TO_STRENGTH_456.put("-1", 0);
    VALUE_TO_STRENGTH_456.put("-2", 0);
    VALUE_TO_STRENGTH_456.put("7", 4);
    VALUE_TO_STRENGTH_456.put("6", 3);
    VALUE_TO_STRENGTH_456.put("5", 2);
    VALUE_TO_STRENGTH_456.put("4", 1);
    VALUE_TO_STRENGTH_456.put("3", 0);
    VALUE_TO_STRENGTH_456.put("2", 0);
    
    VALUE_TO_STRENGTH_456.put("thead", 0);
    VALUE_TO_STRENGTH_456.put("th", 0);
    VALUE_TO_STRENGTH_456.put("em", 0);
    VALUE_TO_STRENGTH_456.put("i", 0);
    VALUE_TO_STRENGTH_456.put("strong", 0);
    VALUE_TO_STRENGTH_456.put("b", 0);
    VALUE_TO_STRENGTH_456.put("a", 0);
    
    VALUE_TO_STRENGTH_456.put("title", 7);

    if (IGNORE_HEADINGS_456) {
      for(Map.Entry<String, Integer> entry : VALUE_TO_STRENGTH_NO_456.entrySet())
        VALUE_TO_STRENGTH.put(entry.getKey(), entry.getValue());

      MIN_USABLE_STRENGTH = 3;
      MAX_STRENGTH = 7;
    }
    else {
      for(Map.Entry<String, Integer> entry : VALUE_TO_STRENGTH_456.entrySet())
        VALUE_TO_STRENGTH.put(entry.getKey(), entry.getValue());

      MIN_USABLE_STRENGTH = 1;
      MAX_STRENGTH = 7;
    }
  }

  private Set<String> ignoreTags;
  private Set<String> headingTags;
  private Map<String, String> tag2attribute;  // i.e. font@size -> font -> size
  private Map<String, Integer> hVal2Strength;
  private Set<String> ignoreTagAttributes;

  /**
   * Construct a new block builder using defaults.
   */
  public HtmlHelper() {
    this(DEFAULT_IGNORE_TAG_STRINGS, DEFAULT_HEADING_TAGS, VALUE_TO_STRENGTH, DEFAULT_IGNORE_TAG_ATTRIBUTES);
  }

  public HtmlHelper(boolean ignoreHeadings456) {
    this(DEFAULT_IGNORE_TAG_STRINGS, 
         DEFAULT_HEADING_TAGS, 
         (ignoreHeadings456 ? VALUE_TO_STRENGTH_NO_456 : VALUE_TO_STRENGTH_456), 
         DEFAULT_IGNORE_TAG_ATTRIBUTES);
  }

  /**
   * Construct a new block builder with the given settings.
   */
  public HtmlHelper(String[] ignoreTags,
                    String[] headingTags,
                    Map<String, Integer> hVal2Strength,
                    String[] ignoreTagAttributes) {

    this.ignoreTags = XmlLite.buildTagSet(ignoreTags);
    buildHeadingTags(headingTags);
    this.hVal2Strength = hVal2Strength;
    this.ignoreTagAttributes = XmlLite.buildTagSet(ignoreTagAttributes);
  }
  
  private final void buildHeadingTags(String[] headingTags) {
    this.headingTags = new HashSet<String>();
    this.tag2attribute = new HashMap<String, String>();

    for (String headingTag : headingTags) {
      final String[] tagAtt = headingTag.split("@");
      this.headingTags.add(tagAtt[0]);
      if (tagAtt.length == 2) this.tag2attribute.put(tagAtt[0], tagAtt[1]);
    }
  }

  /**
   * Compute the maximum heading strength of the path between deep and shallow
   * nodes (inclusive).
   */
  public final int computeHeadingStrength(Tree<XmlLite.Data> deepNode, Tree<XmlLite.Data> shallowNode) {
    int result = 0;

    while (deepNode != null) {
      final int curResult = computeHeadingStrength(deepNode);
      if (curResult > result) result = curResult;

      if (deepNode == shallowNode) break;
      deepNode = deepNode.getParent();
    }

    return result;
  }

  // take a peek at the text and see if it is a manual divider (== hr)
  // (i.e. if the user wrote a string of dashes)
  public final int manualHeadingStrength(Tree<XmlLite.Data> textNode) {
    int result = 0;
    if (isManualDivider(textNode)) {
      result = hVal2Strength.get("hr");
    }
    return result;
  }

  public final int computeHeadingStrength(Tree<XmlLite.Data> headingNode) {
    final XmlLite.Tag tag = headingNode.getData().asTag();
    return computeHeadingStrength(tag);
  }

  public final int computeHeadingStrength(XmlLite.Tag tag) {
    int result = 0;

    final String att = tag2attribute.get(tag.name);  // check for attribute (i.e. font@size=)
    final String value = (att == null) ? tag.name : tag.getAttribute(att);
    if (value != null) {  // found attribute or using tag name
      final Integer strength = hVal2Strength.get(value);
      if (strength != null) result = strength;
    }

    return result;
  }

  public final int computeHeadingStrength(Path path) {
    return computeHeadingStrength(path, false);
  }
  public final int computeHeadingStrength(Path path, boolean useCumulativeTags)
  {
    int result = 0;
    if(path.hasTagStack())
    {
      int cumulativeTagsStrength = 0;
      for(XmlLite.Tag tag : path.getTagStack().getTags())
      {
        int strength = computeHeadingStrength(tag);
        if(useCumulativeTags && 
           CUMULATIVE_HEADING_STRENGTH_TAGS.contains(tag.name))
        {
          cumulativeTagsStrength += strength;
        }
        else
        {
          if(strength > result)
            result = strength;
        }
      }

      if(useCumulativeTags)
        result += cumulativeTagsStrength;
    }

    return result;
  }

  private final boolean isManualDivider(Tree<XmlLite.Data> node) {
    boolean result = false;

    final Tree<XmlLite.Data> textNode = diveForText(node);
    if (textNode != null) {
      final String text = textNode.getData().asText().text;
      if (text.length() > 3) {
        result = true;
        for (int i = 0; i < 4; ++i) {
          if (text.charAt(i) != '-' && text.charAt(i) != '_') { 
            result = false;
            break;
          }
        }
      }
    }

    return result;
  }

  public static final Tree<XmlLite.Data> diveForText(Tree<XmlLite.Data> xmlNode) {
    Tree<XmlLite.Data> textNode = null;
    final List<Tree<XmlLite.Data>> children = xmlNode.getChildren();
    if (children == null || children.size() == 0) {
      if (xmlNode.getData().asText() != null) {
        textNode = xmlNode;
      }
    }
    else if (children.size() == 1) {
      final Tree<XmlLite.Data> child = children.get(0);
      textNode = diveForText(child);
    }
    return textNode;
  }

  /**
   * Determine whether the given node has a tag designated to be ignored.
   */
  public final boolean ignoreTag(Tree<XmlLite.Data> node) {
    boolean result = false;

    final XmlLite.Tag tag = node.getData().asTag();

    if (tag != null) {
      result = ignoreTags.contains(tag.name);
    }

    return result;
  }

  /**
   * Determine whether the given tag is designated to be ignored.
   */
  public final boolean ignoreTag(XmlLite.Tag tag) {
    boolean result = false;

    if (tag != null) {
      result = ignoreTags.contains(tag.name);
    }

    return result;
  }

  public final boolean ignoreTag(String tagName) {
    return ignoreTags.contains(tagName);
  }

  public final String getTagKey(XmlLite.Tag tag) {
    String result = null;

    if (tag != null) {
      result = buildTagKey(tag, ignoreTagAttributes.contains(tag.name));
    }

    return result;
  }

  protected static final String buildTagKey(XmlLite.Tag tag, boolean ignoreAttributes) {
    if (ignoreAttributes) return tag.name;

    final StringBuilder result = new StringBuilder();

    result.append(tag.name);
    for (Map.Entry<String, String> attval : tag.getAttributeEntries()) {
      result.append(' ').
        append(attval.getKey()).
        append('=').
        append(attval.getValue());
    }

    return result.toString();
  }
}
