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
package org.sd.token.plugin;


import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.sd.atn.ResourceManager;
import org.sd.token.AbstractTokenClassifier;
import org.sd.token.Normalizer;
import org.sd.token.Token;
import org.sd.xml.DomElement;
import org.w3c.dom.NodeList;

/**
 * A classifier based on one or more regex patterns.
 * <p>
 * @author Spence Koehler
 */
public class RegexClassifier extends AbstractTokenClassifier {
  
  private List<RegexData> regexes;

  public RegexClassifier(DomElement classifierIdElement, ResourceManager resourceManager, Map<String, Normalizer> id2Normalizer) {
    super(classifierIdElement, id2Normalizer);
    init(classifierIdElement);
  }

  private final void init(DomElement classifierIdElement) {
    this.regexes = new ArrayList<RegexData>();

    final DomElement regexesNode = (DomElement)classifierIdElement.selectSingleNode("regexes");

    if (regexesNode != null) {
      final NodeList regexNodes = regexesNode.selectNodes("regex");
      for (int i = 0; i < regexNodes.getLength(); ++i) {
        final DomElement regexElement = (DomElement)(regexNodes.item(i));
        this.regexes.add(new RegexData(regexElement));
      }
    }
  }

  public boolean doClassify(Token token) {
    boolean result = false;

    final String text = getNormalizedText(token);
    for (RegexData regexData : regexes) {
      if (regexData.matches(text, token)) {
        result = true;
        break;
      }
    }

    return result;
  }


  private enum MatchType { MATCHES, LOOKING_AT, FIND };

  private static final class RegexData {
    private Pattern pattern;
    private MatchType matchType;
    private Map<Integer, String> group2attr;

    RegexData(DomElement regexElement) {
      final String regex = regexElement.getTextContent();
      this.pattern = Pattern.compile(regex);

      // matchType
      final String type = regexElement.getAttributeValue("type", "matches").toLowerCase();
      this.matchType = "lookingat".equals(type) ? MatchType.LOOKING_AT : "find".equals(type) ? MatchType.FIND : MatchType.MATCHES;

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
            token.setFeature(attr, value, this);
          }
        }
      }

      return result;
    }
  }
}
