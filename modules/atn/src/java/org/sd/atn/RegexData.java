/*
    Copyright 2011 Semantic Discovery, Inc. (www.semanticdiscovery.com)

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
package org.sd.atn;


import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.sd.token.Token;
import org.sd.xml.DomElement;
import org.w3c.dom.NodeList;

/**
 * Auxiliary class for managing regex data for classifying text.
 * <p>
 * @author Spence Koehler
 */
public class RegexData {
  
  public enum MatchType { MATCHES, LOOKING_AT, FIND };


  static final Map<String, String> EMPTY_ATTRIBUTES = new HashMap<String, String>();


  //
  // <regex
  //   type='matches/lookingat/find'
  //   ldelim='true/false'
  //   rdelim='true/false'
  //   reverse='true/false'
  //   require='true/false'
  //   preText='true/false'
  //   postText='true/false'
  //   fullText='true/false'
  //   caseInsensitive='true/false'
  //   emptyResult='true/false'
  //   groupN='classification'>...regular expression...</regex>
  //
  //  regex element
  //
  //  attributes:
  //  - type -- "matches" (default), "lookingat", or "find" to specify type of regex match
  //  - ldelim -- "false" (default), or "true" to specify whether pre-token delims are included (post normalization) in matching
  //  - rdelim -- "false" (default), or "true" to specify whether post-token delims are included (post normalization) in matching
  //  - reverse -- "false" (default), or "true" to specify success when regex match fails
  //  - require -- "false" (default), or "true" to specify success only when this regex matches (or fails to match when reverse=true)
  //  - preText -- "false" (default), or "true" to specify to match against all text preceding the token
  //  - postText -- "false" (default), or "true" to specify to match against all text after the token
  //  - fullText -- "false" (default), or "true" to specify to match against all tokenizer text
  //  - caseInsensitive -- "false" (default), or "true" to designate a case-insensitive match
  //  - emptyResult -- "false" (default), or "true" for the result to return when the text is empty</li>
  //  - groupN -- where N is a valid group integer for specifying a classification for the matched group
  //
  //  text content:
  //  - regular expression
  //

  /**
   * Factory method to load RegexData instances from "regex" elements under the
   * given (e.g. "regexes") element.
   *
   * @return null if regexesNode is null; otherwise, a (possibly empty) list of
   *         RegexData instances, one for each "regex" element under regexesNode.
   */
  public static List<RegexData> load(DomElement regexesNode) {
    if (regexesNode == null) return null;

    final List<RegexData> result = new ArrayList<RegexData>();

    final NodeList regexNodes = regexesNode.selectNodes("regex");
    for (int i = 0; i < regexNodes.getLength(); ++i) {
      final DomElement regexElement = (DomElement)(regexNodes.item(i));
      result.add(new RegexData(regexElement));
    }

    return result;
  }


  private Pattern pattern;
  private MatchType matchType;
  private Map<Integer, String> group2attr;
  private boolean ldelim;
  private boolean rdelim;
  private boolean reverse;
  private boolean require;
  private boolean preText;
  private boolean postText;
  private boolean fullText;
  private boolean caseInsensitive;
  private boolean emptyResult;

  /**
   * Load from a regex element having the form:
   * <p>
   * &lt;regex
   *   type='matches/lookingat/find'
   *   ldelim='true/false'
   *   rdelim='true/false'
   *   reverse='true/false'
   *   require='true/false'
   *   preText='true/false'
   *   postText='true/false'
   *   fullText='true/false'
   *   caseInsensitive='true/false'
   *   emptyResult='true/false'
   *   groupN='classification'&gt;...regular expression...&lt;/regex&gt;
   * <p>
   * Where
   * <ul>
   * <li>'type' is the type of regex match to perform;</li>
   * <li>when 'ldelim' is true, token predelims are included in the match text;</li>
   * <li>when 'rdelim' is true, token postdelims are included in the match text;</li>
   * <li>when 'reverse' is true, the match succeeds when the regex fails to match;</li>
   * <li>when 'require' is true, the regex match is mandatory for success;</li>
   * <li>when 'preText' is true, the regex is applied to all text preceding the token</li>
   * <li>when 'postText' is true, the regex is applied to all text following the token</li>
   * <li>when 'caseInsensitive' is true, the case-insensitive flag is set</li>
   * <li>when 'fullText' is true, the regex is applied to all tokenizer text</li>
   * <li>'emptyResult' holds the result, true or false (default), to return when the text is empty</li>
   * <li>a token feature name 'classification' is assigned the text of matched 'groupN' for the token;</li>
   * <li>element text is the regular expression.</li>
   * </ul>
   */
  public RegexData(DomElement regexElement) {
    this.caseInsensitive = regexElement.getAttributeBoolean("caseInsensitive", false);

    final String regex = regexElement.getTextContent();
    this.pattern = caseInsensitive ? Pattern.compile(regex, Pattern.CASE_INSENSITIVE) : Pattern.compile(regex);

    // matchType
    final String type = regexElement.getAttributeValue("type", "matches").toLowerCase();
    this.matchType = "lookingat".equals(type) ? MatchType.LOOKING_AT : "find".equals(type) ? MatchType.FIND : MatchType.MATCHES;

    // ldelim, rdelim
    this.ldelim = regexElement.getAttributeBoolean("ldelim", false);
    this.rdelim = regexElement.getAttributeBoolean("rdelim", false);

    // reverse, require
    this.reverse = regexElement.getAttributeBoolean("reverse", false);
    this.require = regexElement.getAttributeBoolean("require", false);

    // preText, postText, fullText
    this.preText = regexElement.getAttributeBoolean("preText", false);
    this.postText = regexElement.getAttributeBoolean("postText", false);
    this.fullText = regexElement.getAttributeBoolean("fullText", false);

    // group2attr
    final Map<String, String> attrs = regexElement.getDomAttributes().getAttributes();
    this.group2attr = new TreeMap<Integer, String>();
    for (Map.Entry<String, String> attrEntry : attrs.entrySet()) {
      final String key = attrEntry.getKey();
      if (key.startsWith("group")) {
        try {
          final Integer groupNum = new Integer(key.substring(5));
          group2attr.put(groupNum, attrEntry.getValue());
        }
        catch (NumberFormatException e) {
          //ignore non-number groupN attrs
        }
      }
    }
    if (group2attr.size() == 0 && regexElement.getParentNode() != null && regexElement.getParentNode().getParentNode() != null) {
      // default to group0=classifierName (= regex.parent.parent.name)
      group2attr.put(0, regexElement.getParentNode().getParentNode().getLocalName());
    }
  }

  public boolean isRequired() {
    return require;
  }

  public boolean matches(String text, Token token) {
    return matches(text, token, true);
  }

  public boolean matches(String text, Token token, boolean addTokenFeature) {

    if (token != null && (preText || postText || fullText)) {
      final StringBuilder theText = new StringBuilder();
      if (preText) {
        theText.append(token.getTokenizer().getPriorText(token));
        if (ldelim || rdelim) {
          theText.append(token.getPreDelim());
        }
      }
      else if (postText) {
        theText.append(token.getTokenizer().getNextText(token));
        if (ldelim || rdelim) {
          theText.insert(0, token.getPostDelim());
        }
      }
      else {
        theText.append(token.getTokenizer().getText());
      }
      text = theText.toString();
    }
    else if (token != null && (ldelim || rdelim)) {
      final StringBuilder delimText = new StringBuilder(text);
      if (ldelim) {
        delimText.insert(0, token.getPreDelim());
      }
      if (rdelim) {
        delimText.append(token.getPostDelim());
      }
      text = delimText.toString();
    }

    boolean result = false;

    if ("".equals(text)) {
      result = emptyResult;
    }
    else {
      final MatchResult matches = patternMatches(text);

      if (matches.m.matches() && token != null && addTokenFeature) {
        for (Map.Entry<Integer, String> entry : group2attr.entrySet()) {
          final Integer group = entry.getKey();

          final String value = matches.m.group(group);
          if (value != null) {
            final String attr = entry.getValue();
            token.setFeature(attr, value, this);
          }
        }
      }

      result = matches.matches;
    }

    return result;
  }

  /**
   * If the text matches, return a non-null result populated with features.
   */
  public Map<String, String> matches(String text) {
    return matches(text, (Map<String, String>)null, true);
  }

  /**
   * If the text matches, populate result with features.
   */
  public Map<String, String> matches(String text, Map<String, String> result, boolean guaranteeNonNullResult) {

    final MatchResult matches = patternMatches(text);

    if (matches.matches) {
      result = matches.getAttributes(result);
      if (result == null && guaranteeNonNullResult) result = EMPTY_ATTRIBUTES;
    }

    return result;
  }

  public MatchResult patternMatches(String text) {
    boolean result = false;
    final Matcher m = pattern.matcher(text);

    switch (matchType) {
    case LOOKING_AT :
      result = m.lookingAt(); break;
    case FIND :
      result = m.find(); break;
    default :
      result = m.matches();
    }

    if (reverse) {
      result = !result;
    }

    return new MatchResult(m, result, group2attr);
  }

  public static final class MatchResult {
    public final Matcher m;
    public final boolean matches;
    private Map<Integer, String> group2attr;

    public MatchResult(Matcher m, boolean matches, Map<Integer, String> group2attr) {
      this.m = m;
      this.matches = matches;
      this.group2attr = group2attr;
    }

    /**
     * Extract the matcher's attributes if it matched.
     */
    public Map<String, String> getAttributes(Map<String, String> result) {
      if (m.matches() && group2attr != null) {
        for (Map.Entry<Integer, String> entry : group2attr.entrySet()) {
          final Integer group = entry.getKey();

          final String value = m.group(group);
          if (value != null) {
            final String attr = entry.getValue();
            if (result == null) result = new HashMap<String, String>();
            result.put(attr, value);
          }
        }
      }
      return result;
    }
  }
}
