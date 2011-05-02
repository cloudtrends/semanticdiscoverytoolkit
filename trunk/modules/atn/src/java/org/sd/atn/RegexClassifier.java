/*
    Copyright 2010 Semantic Discovery, Inc. (www.semanticdiscovery.com)

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
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.sd.atn.ResourceManager;
import org.sd.token.Normalizer;
import org.sd.token.Token;
import org.sd.xml.DomElement;
import org.w3c.dom.NodeList;

/**
 * A classifier based on one or more regex patterns.
 * <p>
 * @author Spence Koehler
 */
public class RegexClassifier extends AbstractAtnStateTokenClassifier {
  
  public static final String DEFAULT_REGEXES_NODE_NAME = "regexes";


  private List<RegexData> regexes;

  public RegexClassifier(DomElement classifierIdElement, ResourceManager resourceManager, Map<String, Normalizer> id2Normalizer) {
    this(classifierIdElement, resourceManager, id2Normalizer, DEFAULT_REGEXES_NODE_NAME);
  }

  public RegexClassifier(DomElement classifierIdElement, ResourceManager resourceManager, Map<String, Normalizer> id2Normalizer,
                         String regexesNodeName) {
    super(classifierIdElement, id2Normalizer);
    init(classifierIdElement, regexesNodeName);
  }

  private final void init(DomElement classifierIdElement, String regexesNodeName) {
    this.regexes = new ArrayList<RegexData>();

    final DomElement regexesNode = (DomElement)classifierIdElement.selectSingleNode(regexesNodeName);

    if (regexesNode != null) {
      final NodeList regexNodes = regexesNode.selectNodes("regex");
      for (int i = 0; i < regexNodes.getLength(); ++i) {
        final DomElement regexElement = (DomElement)(regexNodes.item(i));
        this.regexes.add(new RegexData(regexElement));
      }
    }
  }

  public boolean isEmpty() {
    return regexes == null || regexes.size() == 0;
  }

  public boolean doClassify(Token token) {
    final String text = getNormalizedText(token);
    return doClassify(text, token);
  }

  public boolean doClassify(String text) {
    return doClassify(text, null);
  }

  private final boolean doClassify(String text, Token token) {
    boolean result = false;

    for (RegexData regexData : regexes) {
      if (regexData.matches(text, token)) {
        result = true;
        break;
      }
    }

    return result;
  }


  //
  // <regex
  //   type='matches/lookingat/find'
  //   ldelim='true/false'
  //   rdelim='true/false'
  //   groupN='classification'>...regular expression...</regex>
  //
  //  regex element
  //
  //  attributes:
  //  - type -- "matches" (default), "lookingat", or "find" to specify type of regex match
  //  - ldelim -- "false" (default), or "true" to specify whether pre-token delims are included (post normalization) in matching
  //  - rdelim -- "false" (default), or "true" to specify whether post-token delims are included (post normalization) in matching
  //  - groupN -- where N is a valid group integer for specifying a classification for the matched group
  //
  //  text content:
  //  - regular expression
  //


  private enum MatchType { MATCHES, LOOKING_AT, FIND };

  private static final class RegexData {
    private Pattern pattern;
    private MatchType matchType;
    private Map<Integer, String> group2attr;
    private boolean ldelim;
    private boolean rdelim;

    RegexData(DomElement regexElement) {
      final String regex = regexElement.getTextContent();
      this.pattern = Pattern.compile(regex);

      // matchType
      final String type = regexElement.getAttributeValue("type", "matches").toLowerCase();
      this.matchType = "lookingat".equals(type) ? MatchType.LOOKING_AT : "find".equals(type) ? MatchType.FIND : MatchType.MATCHES;

      // ldelim, rdelim
      this.ldelim = regexElement.getAttributeBoolean("ldelim", false);
      this.rdelim = regexElement.getAttributeBoolean("rdelim", false);

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
    }

    public boolean matches(String text, Token token) {
      boolean result = false;

      if (token != null && (ldelim || rdelim)) {
        final StringBuilder delimText = new StringBuilder(text);
        if (ldelim) {
          delimText.insert(0, token.getPreDelim());
        }
        if (rdelim) {
          delimText.append(token.getPostDelim());
        }
        text = delimText.toString();
      }

      final Matcher m = pattern.matcher(text);

      switch (matchType) {
        case LOOKING_AT :
          result = m.lookingAt(); break;
        case FIND :
          result = m.find(); break;
        default :
          result = m.matches();
      }

      if (result) {
        for (Map.Entry<Integer, String> entry : group2attr.entrySet()) {
          final Integer group = entry.getKey();

          final String value = m.group(group);
          if (value != null) {
            final String attr = entry.getValue();
            if (token != null) token.setFeature(attr, value, this);
          }
        }
      }

      return result;
    }
  }
}
