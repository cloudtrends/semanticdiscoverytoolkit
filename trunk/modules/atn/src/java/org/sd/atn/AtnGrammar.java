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
package org.sd.atn;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.sd.token.Normalizer;
import org.sd.token.StandardNormalizer;
import org.sd.token.StandardNormalizerOptions;
import org.sd.token.Token;
import org.sd.xml.DomElement;
import org.sd.xml.DomNode;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Container for an ATN Grammar.
 * <p>
 * @author Spence Koehler
 */
public class AtnGrammar {
  
  private StandardNormalizer defaultNormalizer;
  StandardNormalizer getDefaultNormalizer() {
    return defaultNormalizer;
  }
  void setDefaultNormalizer(StandardNormalizer defaultNormalizer) {
    this.defaultNormalizer = defaultNormalizer;
  }

  private Map<String, Normalizer> id2Normalizer;
  Map<String, Normalizer> getId2Normalizer() {
    return id2Normalizer;
  }

  private Map<String, TokenFilter> id2TokenFilter;
  Map<String, TokenFilter> getId2TokenFilter() {
    return id2TokenFilter;
  }

  private Map<String, List<AtnStateTokenClassifier>> cat2Classifiers;
  Map<String, List<AtnStateTokenClassifier>> getCat2Classifiers() {
    return cat2Classifiers;
  }
  public List<AtnStateTokenClassifier> getClassifiers(String cat) {
    return cat2Classifiers.get(cat);
  }

  private Map<String, List<AtnRule>> cat2Rules;
  Map<String, List<AtnRule>> getCat2Rules() {
    return cat2Rules;
  }

  private ResourceManager resourceManager;
  ResourceManager getResourceManager() {
    return resourceManager;
  }

  private List<AtnRule> startRules;

  AtnGrammar(DomElement grammarNode, ResourceManager resourceManager) {

    //
    // expected format:
    //
    //   <grammar>
    //     <normalizer id='commonCase' default='true'>
    //       <options>
    //         <commonCase>true</commonCase>
    //         <replaceSymbolsWithWhite>true</replaceSymbolsWithWhite>
    //         <compactWhite>true</compact>
    //       </options>
    //     </normalizer>
    //
    //     <normalizer id='keepCase'>
    //       <options>
    //         <commonCase>false</commonCase>
    //         <replaceSymbolsWithWhite>true</replaceSymbolsWithWhite>
    //         <compactWhite>true</compact>
    //       </options>
    //     </normalizer>
    //
    //     <tokenFilter id='...'>
    //       <jclass>...</jclass>
    //       <class>...</class>
    //       <dll>...</dll>
    //     </tokenFilter>
    //
    //     <classifiers>
    //       <!-- -->
    //     </clasifiers>
    //
    //     <rules>
    //       <!-- -->
    //     </rules>
    //   </grammar>
    //

    this.resourceManager = resourceManager;

    // default to no normalization
    this.defaultNormalizer = null;

    // load normalizers
    this.id2Normalizer = loadNormalizers(grammarNode);

    // load tokenFilters
    this.id2TokenFilter = loadTokenFilters(grammarNode);

    // load classifiers
    this.cat2Classifiers = loadClassifiers(grammarNode, id2Normalizer);

    // load rules
    final DomElement rulesNode = (DomElement)grammarNode.selectSingleNode("rules");

    if (rulesNode == null) {
      throw new IllegalArgumentException("Grammar must have 'rules'!");
    }

    this.startRules = new ArrayList<AtnRule>();
    this.cat2Rules = loadRules(rulesNode, startRules);
  }

  public List<AtnRule> getStartRules(AtnParseOptions parseOptions) {

    List<AtnRule> result = null;

    if (parseOptions != null && parseOptions.hasStartRules()) {
      result = new ArrayList<AtnRule>();
      for (String ruleCategory : parseOptions.getStartRules()) {
        final List<AtnRule> rules = cat2Rules.get(ruleCategory);
        if (rules != null) {
          for (AtnRule rule : rules) {
            result.add(rule);
          }
        }
        //else: throw error? log message?
      }
    }
    else {
      result = startRules;
    }

    return result;
  }

  /**
   * Compute the minimum number of input tokens for this grammar as the
   * maximum of the minimum number of tokens required by each start rule.
   */
  public int computeMinNumTokens() {
    return computeMinNumTokens(new AtnParseOptions(resourceManager));
  }

  /**
   * Compute the minimum number of input tokens for this grammar as the
   * maximum of the minimum number of tokens required by each start rule.
   */
  public int computeMinNumTokens(AtnParseOptions parseOptions) {
    int result = 1;

    final List<AtnRule> startRules = getStartRules(parseOptions);
    for (AtnRule startRule : startRules) {
      final int ruleMinTokens = computeMinNumTokens(startRule);
      if (ruleMinTokens > result) result = ruleMinTokens;
    }

    return result;
  }

  Token getAcceptedToken(String tokenFilterId, Token token, boolean isRevised, Token prevToken, boolean doRevise, boolean doGetNext, AtnState curState) {
    Token result = token;

    if (tokenFilterId != null) {
      if (!id2TokenFilter.containsKey(tokenFilterId)) {
        throw new IllegalArgumentException("Unknown tokenFilterId '" + tokenFilterId + "'!");
      }

      final TokenFilter tokenFilter = id2TokenFilter.get(tokenFilterId);

      boolean tokenAccepted = false;
      while (result != null && !tokenAccepted) {
        final TokenFilterResult tokenFilterResult = tokenFilter.checkToken(result, isRevised, prevToken, curState);
        switch (tokenFilterResult) {
          case HALT:
            result = null;
            break;

          case IGNORE:
            result = doRevise ? result.getRevisedToken() : null;
            if (result == null) {
              final Token temp = result;
              result = doGetNext ? token.getNextToken() : null;
              token = temp;
              isRevised = false;
            }
            else isRevised = true;
            break;

          case ACCEPT:
            tokenAccepted = true;
            break;
        }
      }
    }

    return result;
  }

  /**
   * Compute the minimum number of input tokens for this grammar rule by
   * adding the number of tokens for each non-optional step until the
   * first terminal step.
   */
  private int computeMinNumTokens(AtnRule atnRule) {
    int result = 0;

    for (AtnRuleStep ruleStep : atnRule.getSteps()) {
      if (!ruleStep.isOptional() && ruleStep.consumeToken()) {
        final List<AtnRule> stepRules = cat2Rules.get(ruleStep.getCategory());
        if (stepRules != null) {
          int minStepTokens = 1;
          for (AtnRule stepRule : stepRules) {
            final int curStepTokens = computeMinNumTokens(stepRule);
            if (curStepTokens > minStepTokens) minStepTokens = curStepTokens;
          }
          result += minStepTokens;
        }
        else ++result;
      }

      if (ruleStep.isTerminal()) break;
    }

    return result;
  }

  private Map<String, Normalizer> loadNormalizers(DomNode grammarNode) {
    final Map<String, Normalizer> result = new HashMap<String, Normalizer>();

    NodeList normalizerNodes = grammarNode.selectNodes("normalizer");
    for (int i = 0; i < normalizerNodes.getLength(); ++i) {
      final DomElement normalizerElement = (DomElement)normalizerNodes.item(i);
      final DomElement optionsElement = (DomElement)normalizerElement.selectSingleNode("options");
      final StandardNormalizerOptions options = new StandardNormalizerOptions(optionsElement);
      final StandardNormalizer normalizer = new StandardNormalizer(options);

      final String normalizerId = normalizerElement.getAttribute("id");
      result.put(normalizerId, normalizer);

      if (normalizerElement.getAttributeBoolean("default", false)) {
        defaultNormalizer = normalizer;
      }
    }

    return result;
  }

  private Map<String, TokenFilter> loadTokenFilters(DomNode grammarNode) {
    final Map<String, TokenFilter> result = new HashMap<String, TokenFilter>();

    final NodeList tokenFilterNodes = grammarNode.selectNodes("tokenFilter");
    for (int i = 0; i < tokenFilterNodes.getLength(); ++i) {
      final DomElement tokenFilterElement = (DomElement)tokenFilterNodes.item(i);
      final TokenFilter tokenFilter = (TokenFilter)resourceManager.getResource(tokenFilterElement);
      final String tokenFilterId = tokenFilterElement.getAttribute("id");
      result.put(tokenFilterId, tokenFilter);
    }

    return result;
  }

  private Map<String, List<AtnStateTokenClassifier>> loadClassifiers(DomElement grammarNode, Map<String, Normalizer> id2Normalizer) {
    Map<String, List<AtnStateTokenClassifier>> result = new HashMap<String, List<AtnStateTokenClassifier>>();

    //
    // Classifier elements under the grammar node should have the form:
    //
    // <classifiers>
    //   <classifierId>
    //     <jclass>java-classifier-class</jclass>
    //     <classifierClass>qualified-type-of-classifier</dll>
    //     <dll>optional-path-to-dll</dll>
    //     ...any other properties required by classifier class on construction,
    //        which will receive the classifierId node...
    //     <maxWordCount>maxWordCount</maxWordCount>
    //
    //     <normalizer id='id'/>
    // or
    //     <normalizer>
    //       <options>
    //         ...
    //       </options>
    //     </normalizer>
    //
    //   </classifierId>
    // </classifiers>
    //

    DomElement classifiersNode = (DomElement)grammarNode.selectSingleNode("classifiers");
    if (classifiersNode != null) {
      NodeList classifierNodes = classifiersNode.getChildNodes();
      for (int i = 0; i < classifierNodes.getLength(); ++i) {
        final DomElement classifierElement = (DomElement)classifierNodes.item(i);
        final AtnStateTokenClassifier classifier =
          (AtnStateTokenClassifier)resourceManager.getResource(classifierElement, new Object[] { id2Normalizer });
        final String classifierId = classifierElement.getLocalName();

        List<AtnStateTokenClassifier> classifiers = result.get(classifierId);
        if (classifiers == null) {
          classifiers = new ArrayList<AtnStateTokenClassifier>();
          result.put(classifierId, classifiers);
        }

        if (classifier != null) {
          classifiers.add(classifier);
        }
        else {
          System.out.println("***WARNING: Couldn't load classifier '" + classifierId + "'!");
        }
      }
    }

    return result;
  }

  private Map<String, List<AtnRule>> loadRules(DomElement rulesElement, List<AtnRule> startRules) {
    final Map<String, List<AtnRule>> result = new HashMap<String, List<AtnRule>>();

    final NodeList childNodes = rulesElement.getChildNodes();
    for (int i = 0; i < childNodes.getLength(); ++i) {
      final Node curNode = childNodes.item(i);
      if (curNode.getNodeType() != DomNode.ELEMENT_NODE) continue;

      final DomElement ruleElement = (DomElement)curNode;
      final AtnRule rule = new AtnRule(this, ruleElement, resourceManager);
      final String ruleCategory = ruleElement.getLocalName();

      List<AtnRule> rules = result.get(ruleCategory);
      if (rules == null) {
        rules = new ArrayList<AtnRule>();
        result.put(ruleCategory, rules);
      }
      rules.add(rule);

      if (rule.isStart()) startRules.add(rule);
    }

    return result;
  }
}
