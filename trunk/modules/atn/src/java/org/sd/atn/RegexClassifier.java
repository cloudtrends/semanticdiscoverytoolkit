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


import java.util.List;
import java.util.Map;
import org.sd.atn.ResourceManager;
import org.sd.token.Normalizer;
import org.sd.token.Token;
import org.sd.util.Usage;
import org.sd.xml.DomElement;

/**
 * A classifier based on one or more regex patterns.
 * <p>
 * @author Spence Koehler
 */
@Usage(notes =
       "The functionaly of this org.sd.atn.AbstractAtnStateTokenClassifier\n" +
       "has been usurped by org.sd.atn.RoteListClassifier. Use it instead!"
  )
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
    final DomElement regexesNode = (DomElement)classifierIdElement.selectSingleNode(regexesNodeName);
    this.regexes = RegexData.load(regexesNode);
  }

  public boolean isEmpty() {
    return regexes == null || regexes.size() == 0;
  }

  public boolean doClassify(Token token, AtnState atnState) {
    final String text = getTokenClassifierHelper().getNormalizedText(token);
    return doClassification(text, token);
  }

  protected Map<String, String> doClassify(String text) {
    return doClassification(text, null) ? EMPTY_MAP : null;
  }

  private final boolean doClassification(String text, Token token) {
    boolean result = false;

    if (!isEmpty()) {
      for (RegexData regexData : regexes) {
        if (regexData.matches(text, token)) {
          result = true;
          break;
        }
      }
    }
    else {
      System.err.println("WARNING: RegexClassifier has no regexes. Probable initialization syntax error.");
    }

    return result;
  }

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
  //  - groupN -- where N is a valid group integer for specifying a classification for the matched group
  //
  //  text content:
  //  - regular expression
  //
}
