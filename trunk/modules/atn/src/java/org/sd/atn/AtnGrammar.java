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


import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.sd.token.Feature;
import org.sd.token.Features;
import org.sd.token.Normalizer;
import org.sd.token.StandardNormalizer;
import org.sd.token.StandardNormalizerOptions;
import org.sd.token.Token;
import org.sd.util.Usage;
import org.sd.xml.DataProperties;
import org.sd.xml.DomDocument;
import org.sd.xml.DomElement;
import org.sd.xml.DomNode;
import org.sd.xml.DomUtil;
import org.sd.xml.XmlFactory;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Container for an ATN Grammar.
 * <p>
 * @author Spence Koehler
 */
@Usage(notes =
      "ATN Grammars are of the form:\n" +
      "\n" +
      "<grammar>\n" +
      "  <normalizer id='commonCase' default='true'>\n" +
      "    <options>\n" +
      "      <commonCase>true</commonCase>\n" +
      "      <replaceSymbolsWithWhite>true</replaceSymbolsWithWhite>\n" +
      "      <compactWhite>true</compact>\n" +
      "    </options>\n" +
      "  </normalizer>\n" +
      "\n" +
      "  <normalizer id='keepCase'>\n" +
      "    <options>\n" +
      "      <commonCase>false</commonCase>\n" +
      "      <replaceSymbolsWithWhite>true</replaceSymbolsWithWhite>\n" +
      "      <compactWhite>true</compact>\n" +
      "    </options>\n" +
      "  </normalizer>\n" +
      "\n" +
      "  <tokenFilter id='...'>\n" +
      "    <jclass>...</jclass>\n" +
      "    <class>...</class>\n" +
      "    <dll>...</dll>\n" +
      "  </tokenFilter>\n" +
      "\n" +
      "  <classifiers>\n" +
      "    <!-- -->\n" +
      "  </classifiers>\n" +
      "\n" +
      "  <immutables>\n" +
      "    <!-- -->\n" +
      "  </immutables>\n" +
      "\n" +
      "  <rules>\n" +
      "    <!-- -->\n" +
      "  </rules>\n" +
      "</grammar>"
  )
public class AtnGrammar {
  
  /**
   * Given a dom element identifying a grammar, get the identified
   * grammar element.
   * <p>
   * Note that this is either the given element itself (when it has
   * child nodes defining the grammar) or it is the top node loaded
   * from the grammar file identified by the grammarIdElt's text.
   */
  public static final DomElement getGrammarElement(DomElement grammarIdElt) {
    DomElement grammarElement = grammarIdElt;

    if (grammarIdElt != null) {
      if (DomUtil.getFirstChild(grammarIdElt) == null) {
        final DataProperties dataProperties = grammarElement.getDataProperties();
        final String grammarFilename = grammarIdElt.getTextContent();
        final File grammarFile = dataProperties == null ? new File(grammarFilename) : dataProperties.getWorkingFile(grammarFilename, "workingDir");

        try {
          final DomDocument domDocument = XmlFactory.loadDocument(grammarFile, false, dataProperties);
          grammarElement = (DomElement)domDocument.getDocumentElement();

          if (GlobalConfig.verboseLoad()) {
            System.out.println(new Date() + ": AtnGrammar file(" + grammarFile + ")");
          }
        }
        catch (IOException e) {
          throw new IllegalStateException(e);
        }
      }
    }

    return grammarElement;
  }


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

  private List<Feature> immutables;
  List<Feature> getImmutables() {
    return immutables;
  }
  boolean isImmutable(Token token) {
    boolean result = false;

    if (immutables != null && token.hasFeatures()) {
      final Features features = token.getFeatures();
      for (Feature immutable : immutables) {
        if (features.hasFeature(immutable)) {
          result = true;
          break;
        }
      }
    }

    return result;
  }

  private ResourceManager resourceManager;
  ResourceManager getResourceManager() {
    return resourceManager;
  }

  private List<DomElement> grammarNodes;
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
    //     </classifiers>
    //
    //     <rules>
    //       <!-- -->
    //     </rules>
    //   </grammar>
    //

    this.resourceManager = resourceManager;

    // default to no normalization
    this.defaultNormalizer = null;

    this.id2Normalizer = new HashMap<String, Normalizer>();
    resourceManager.setId2Normalizer(id2Normalizer);

    this.id2TokenFilter = new HashMap<String, TokenFilter>();
    this.cat2Classifiers = new HashMap<String, List<AtnStateTokenClassifier>>();

    this.startRules = new ArrayList<AtnRule>();

    this.cat2Rules = new HashMap<String, List<AtnRule>>();

    doSupplement(grammarNode);
  }
  
  private final void doSupplement(DomElement grammarNode) {
    if (grammarNodes == null) grammarNodes = new ArrayList<DomElement>();
    grammarNodes.add(grammarNode);

    // load normalizers
    loadNormalizers(grammarNode);

    // load tokenFilters
    loadTokenFilters(grammarNode);

    // load classifiers
    loadClassifiers(grammarNode);

    // load immutables
    loadImmutables(grammarNode);

    // load rules
    final DomElement rulesNode = (DomElement)grammarNode.selectSingleNode("rules");

    if (rulesNode == null) {
      throw new IllegalArgumentException("Grammar must have 'rules'!");
    }

    loadRules(rulesNode);
  }

  /**
   * Supplement this grammar with additional configuration formatted the same
   * as an initialization node.
   */
  public void supplement(DomNode supplementNode) {
    final DomElement grammarElement = getGrammarElement((DomElement)supplementNode);
    doSupplement(grammarElement);
  }

  /**
   * Get the grammar xml nodes (primary and supplements) defining this grammar.
   */
  public List<DomElement> getGrammarNodes() {
    return grammarNodes;
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

    if (result == null || result.size() == 0) {
      if (GlobalConfig.verboseLoad()) {
        System.out.println("***WARNING: No startRules found or specified!");
      }
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

  /**
   * Compute the largest (non-zero) maximum word count across all classifiers.
   */
  public int computeMaxWordCount() {
    int result = 0;

    for (List<AtnStateTokenClassifier> cList : cat2Classifiers.values()) {
      for (AtnStateTokenClassifier classifier : cList) {
        final int curMaxWordCount = classifier.getMaxWordCount();
        result = Math.max(result, curMaxWordCount);
      }
    }
    
    return result;
  }

  public String getRulesString(AtnParseOptions parseOptions) {
    final StringBuilder result = new StringBuilder();

    final LinkedList<AtnRule> todo = new LinkedList<AtnRule>(getStartRules(parseOptions));
    final Set<AtnRule> done = new HashSet<AtnRule>();

    while (todo.size() > 0) {
      final AtnRule curRule = todo.removeFirst();
      if (!done.contains(curRule)) {
        done.add(curRule);
        getRuleString(result, curRule, todo);
      }
    }

    return result.toString();
  }

  private final void getRuleString(StringBuilder result, AtnRule curRule, LinkedList<AtnRule> todo) {
    result.append(curRule.getRuleName()).append(" <--");
    for (int stepNum = 0; stepNum < curRule.getNumSteps(); ++stepNum) {
      final AtnRuleStep ruleStep = curRule.getStep(stepNum);
      result.append(" ").append(ruleStep.toString());

      final List<AtnRule> moreRules = cat2Rules.get(ruleStep.getCategory());
      if (moreRules != null) todo.addAll(moreRules);
    }
    result.append("\n");
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

  private void loadNormalizers(DomNode grammarNode) {

    NodeList normalizerNodes = grammarNode.selectNodes("normalizer");
    for (int i = 0; i < normalizerNodes.getLength(); ++i) {
      final DomElement normalizerElement = (DomElement)normalizerNodes.item(i);
      final DomElement optionsElement = (DomElement)normalizerElement.selectSingleNode("options");
      final StandardNormalizerOptions options = new StandardNormalizerOptions(optionsElement);
      final StandardNormalizer normalizer = new StandardNormalizer(options);

      final String normalizerId = normalizerElement.getAttribute("id");
      this.id2Normalizer.put(normalizerId, normalizer);

      if (normalizerElement.getAttributeBoolean("default", false)) {
        defaultNormalizer = normalizer;
      }
    }
  }

  private void loadTokenFilters(DomNode grammarNode) {

    final NodeList tokenFilterNodes = grammarNode.selectNodes("tokenFilter");
    for (int i = 0; i < tokenFilterNodes.getLength(); ++i) {
      final DomElement tokenFilterElement = (DomElement)tokenFilterNodes.item(i);
      final TokenFilter tokenFilter = (TokenFilter)resourceManager.getResource(tokenFilterElement);
      final String tokenFilterId = tokenFilterElement.getAttribute("id");
      this.id2TokenFilter.put(tokenFilterId, tokenFilter);
    }
  }

  private void loadClassifiers(DomNode grammarNode) {

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
          (AtnStateTokenClassifier)resourceManager.getResource(classifierElement, new Object[] { this.id2Normalizer });
        final String classifierId = classifierElement.getLocalName();

        List<AtnStateTokenClassifier> classifiers = this.cat2Classifiers.get(classifierId);
        if (classifiers == null) {
          classifiers = new ArrayList<AtnStateTokenClassifier>();
          this.cat2Classifiers.put(classifierId, classifiers);
        }

        if (classifier != null) {
          if (!classifiers.contains(classifier)) {
            classifiers.add(classifier);
          }
          else {
            if (GlobalConfig.verboseLoad()) {
              System.out.println(new Date() + ": AtnGrammar supplementing classifier (" + classifierId + ")");
            }
            classifier.supplement(classifierElement);
          }
        }
        else {
          if (GlobalConfig.verboseLoad()) {
            System.out.println("***WARNING: Couldn't load classifier '" + classifierId + "'!");
          }
        }
      }
    }
  }

  private void loadImmutables(DomNode grammarNode) {

    //<immutables>
    //  <...immutable-feature-name... />
    //  ...
    //</immutables>


    DomElement immutablesElement = (DomElement)grammarNode.selectSingleNode("immutables");
    if (immutablesElement == null) return;

    final NodeList childNodes = immutablesElement.getChildNodes();
    for (int i = 0; i < childNodes.getLength(); ++i) {
      final Node curNode = childNodes.item(i);
      if (curNode.getNodeType() != DomNode.ELEMENT_NODE) continue;
      if (this.immutables == null) this.immutables = new ArrayList<Feature>();
      final Feature feature = buildImmutableFeature((DomElement)curNode);
      if (feature != null) this.immutables.add(feature);
    }
  }

  private final Feature buildImmutableFeature(DomElement immutableElt) {
    Feature result = null;

    final String featureType = immutableElt.getLocalName();
    String valueType = immutableElt.getAttribute("valueType");
    String pValue = immutableElt.getAttribute("p");
    String value = immutableElt.getTextContent();


    // handle default case: when just an elt name w/no attributes or text content
    // create a feature of type=name, value=(Boolean)true, p=1.0 to match against
    // AtnParseBasedTokenizer feature for a parse from a prior grammar.
    if ((value == null || "".equals(value)) && valueType == null && (pValue == null || "".equals(pValue))) {
      result = new Feature();
      result.setType(featureType);
      result.setValue(new Boolean(true));
      result.setP(1.0);
    }
    else {
      //todo: support building other features if/when necessary
    }

    return result;
  }

  private void loadRules(DomElement rulesElement) {

    final NodeList childNodes = rulesElement.getChildNodes();
    for (int i = 0; i < childNodes.getLength(); ++i) {
      final Node curNode = childNodes.item(i);
      if (curNode.getNodeType() != DomNode.ELEMENT_NODE) continue;

      final DomElement ruleElement = (DomElement)curNode;
      final boolean remove = ruleElement.getAttributeBoolean("remove", false);
      final AtnRule rule = new AtnRule(this, ruleElement, resourceManager);
      final String ruleCategory = ruleElement.getLocalName();
      final boolean override = ruleElement.getAttributeBoolean("override", false);

      List<AtnRule> rules = this.cat2Rules.get(ruleCategory);
      if (rules == null) {
        if (!remove) {
          rules = new ArrayList<AtnRule>();
          this.cat2Rules.put(ruleCategory, rules);
        }
      }
      else if (override || remove) {
        final String id = rule.getRuleId();

        // find and remove the rule being overridden or removed
        for (Iterator<AtnRule> ruleIter = rules.iterator(); ruleIter.hasNext(); ) {
          final AtnRule curRule = ruleIter.next();
          if (id.equals(curRule.getRuleId())) {
            ruleIter.remove();
            startRules.remove(curRule);
            break;
          }
        }
      }

      if (!remove) {
        rules.add(rule);
        if (rule.isStart()) this.startRules.add(rule);
      }
    }
  }


  public static void main(String[] args) throws IOException {
    // Properties:
    //   grammarFile=path to grammarFile 
    //
    final DataProperties options = new DataProperties(args);
    args = options.getRemainingArgs();

    if (!options.hasProperty("_disableLoad")) options.set("_disableLoad", true);
    final ResourceManager resourceManager = new ResourceManager(options);
    final File grammarFile = new File(options.getString("grammarFile"));
    final DomDocument grammarDocument = XmlFactory.loadDocument(grammarFile, false, options);
    final DomElement grammarElement = (DomElement)grammarDocument.getDocumentElement();
    final AtnGrammar grammar = new AtnGrammar(grammarElement, resourceManager);

    System.out.println("Grammar: " + grammarFile.getAbsolutePath());
    System.out.println(grammar.getRulesString(null));
  }
}
