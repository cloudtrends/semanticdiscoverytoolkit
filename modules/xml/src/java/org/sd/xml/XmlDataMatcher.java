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


import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.sd.util.tree.NodePath;
import org.sd.util.tree.Tree;

/**
 * A NodePath.DataMatcher for matching xml data.
 * <p>
 * A data matcher to match node data according to the following pattern:
 * <p>
 * tag{attributes}/text
 * <p>
 * Where
 * <ul>
 * <li>'tag' is a tag name to match. okay if empty.</li>
 * <li>'attributes' (optional) is a comma-delimited list of attributes to match
 *     of the form 'attr' (to test for existing attribute) or 'attr=value' (to
 *     match if the attribute exists and has the value.)</li>
 * <li>'text' (optional) is text to match.</li>
 * </ul>
 * If any component starts with a tilde, '~', then the remainder is taken as a
 * regex pattern to match against; otherwise, the text is matched literally and
 * is case-insenstive.
 * <p>
 * If 'text' is preceded by an additional '/', then it is matched against all
 * text under the current node instead of just the current node's text.
 * <p>
 * Examples:
 * <ul>
 * <li>td -- match a 'td' tag</li>
 * <li>td{id} -- match a 'td' tag having an 'id' attribute</li>
 * <li>td{id,class=foo} -- match a 'td' tag having an 'id' attribute and a
 *    'class' attribute whose value is 'foo'.</li>
 * <li>td{id,class=foo}//bar -- same as previous, except only match if the
 *     text under the td node is "bar".</li>
 * <li>/bar -- matches a text node equalling "bar"</li>
 * <li>/~^.*\bbar\b.*$ -- matches a text node containing the word "bar"</li>
 * <li>td{class=~^.*bar.*$} -- matches a td node with a class attribute whose
 *     value contains the substring "bar".</li>
 * </ul>
 *
 * @author Spence Koehler
 */
public class XmlDataMatcher implements NodePath.DataMatcher<XmlLite.Data> {
  
  private Constituent tagConstituent;
  private List<AttributeConstituent> attrConstituents;
  private Constituent textConstituent;
  private boolean allText;

  public XmlDataMatcher(String pattern) {
    final int len = pattern.length();
    final int lcbpos = pattern.indexOf('{');
    int spos = pattern.indexOf('/');

    int endTagPos = len;
    if (lcbpos >= 0) {  // has curly brace (attributes block)
      endTagPos = lcbpos;
    }
    else if (spos >= 0) {  // has slash (text block) with no curly brace
      endTagPos = spos;
    }

    // set up tag constituent
    this.tagConstituent = new Constituent(pattern, 0, endTagPos);

    // set up attributes constituents
    this.attrConstituents = null;
    if (lcbpos >= 0) {
      final int rcbpos = pattern.indexOf('}', lcbpos + 1);
      if (rcbpos < 0) throw new IllegalArgumentException("Bad pattern! no matching '}' found!");

      this.attrConstituents = new ArrayList<AttributeConstituent>();
      final String attrsString = pattern.substring(lcbpos + 1, rcbpos);
      final String[] attrs = attrsString.split("\\s*,\\s*");
      for (String attr : attrs) {
        this.attrConstituents.add(new AttributeConstituent(attr));
      }
    }

    // set up text constituent
    this.textConstituent = null;
    this.allText = false;
    if (spos >= 0) {
      if (spos + 1 < len && pattern.charAt(spos + 1) == '/') {
        this.allText = true;
        ++spos;
      }
      this.textConstituent = new Constituent(pattern, spos + 1, len);
    }

    // NOTE: it doesn't make sense to have a tag(or attrs) AND text w/out
    //       allText=true, so allow 'syntax shortcut' of a single '/' when
    //       there is tag name or attributes to match with text.
  }

  public boolean matches(Tree<XmlLite.Data> node) {
    boolean result = false;

    String text = null;
    final XmlLite.Data dataInNode = node.getData();
    final XmlLite.Tag tag = dataInNode.asTag();
    if (tag != null) {  // check against tag constituent
      final String tagName = tag.name;
      result = tagConstituent.matches(tagName);

      if (result && attrConstituents != null) {  // check against attributes
        // set result to false if we fail to match attributes
        result = false;

        for (AttributeConstituent attrConstituent : attrConstituents) {
          if (attrConstituent.matches(tag)) {
            result = true;
            break;
          }
        }
      }

      if (result && textConstituent != null) {  // check against text constituent
        //NOTE: allText always considered to be 'true' here
        text = XmlTreeHelper.getAllText(node);
      }
    }
    else {  // check against text constituent
      if (textConstituent != null) {
        if (allText) {
          text = XmlTreeHelper.getAllText(node);
        }
        else {
          final XmlLite.Text xmlText = dataInNode.asText();
          if (xmlText != null) {
            text = xmlText.text;
          }
        }
      }
    }

    if (textConstituent != null) {
      if (text != null && !"".equals(text)) {
        result = textConstituent.matches(text);
      }
      else {
        result = false;
      }
    }

    return result;
  }

  public String getTagString() {
    return tagConstituent.getString();
  }


  /**
   * Container for a constituent to match against.
   */
  private static final class Constituent {
    private String string;  // literal string to match against
    private Pattern pattern;  // pattern to match against

    Constituent(String pattern) {
      this(pattern, 0, pattern.length());
    }

    Constituent(String pattern, int startPos, int endPos) {
      this.string = null;
      this.pattern = null;

      if (endPos > startPos) {
        if (pattern.charAt(startPos) == '~' && endPos > startPos + 1) {  // have pattern instead of string
          this.pattern = Pattern.compile(pattern.substring(startPos + 1, endPos));
        }
        else {  // have string to match (case-insensitive)
          this.string = pattern.substring(startPos, endPos).toLowerCase();
        }
      }
    }

    public boolean isEmpty() {
      return string == null && pattern == null;
    }

    public String getString() {
      return string;
    }

    public Pattern getPattern() {
      return pattern;
    }

    public boolean matches(String text) {
      boolean result = true;  // empty constituent always matches

      if (string != null) {
        result = string.equalsIgnoreCase(text);
      }
      else if (pattern != null) {
        result = pattern.matcher(text).find();
      }

      return result;
    }
  }

  private static final class AttributeConstituent {
    private Constituent attr;
    private Constituent value;

    AttributeConstituent(String attrPattern) {
      this.attr = null;
      this.value = null;

      // split on '='
      final String[] pieces = attrPattern.split("\\s*=\\s*");
      this.attr = new Constituent(pieces[0]);
      if (pieces.length > 1) {
        this.value = new Constituent(pieces[1]);
      }
    }

    public boolean matches(XmlLite.Tag tag) {
      boolean result = false;

      if (attr.isEmpty()) {
        if (value != null) {
          // hafta match value part against all att/vals
          for (Map.Entry<String, String> attrEntry : tag.getAttributeEntries()) {
            if (value.matches(attrEntry.getValue())) {
              result = true;
              break;
            }
          }
        }
      }
      else {
        final String valueString = attributeMatches(tag);

        if (value != null) {  // use value constituent to match
          if (valueString != null) {
            result = value.matches(valueString);
          }
        }
        else {
          result = true;  // match any value
        }
      }

      return result;
    }

    /**
     * Find a matching attribute, returning its value.
     *
     * @return the matching attribute's value or null if nothing matches.
     */
    private final String attributeMatches(XmlLite.Tag tag) {
      String result = null;

      final String string = attr.getString();
      if (string != null) {
        // efficiency shortcut
        result = tag.getAttribute(string);
      }
      else {  // use pattern
        // walk through all attributes
        for (Map.Entry<String, String> attrEntry : tag.getAttributeEntries()) {
          if (attr.matches(attrEntry.getKey())) {
            result = attrEntry.getValue();
            break;
          }
        }
      }

      return result;
    }
  }
}
