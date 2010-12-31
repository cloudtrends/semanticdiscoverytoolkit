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


import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.sd.atn.ResourceManager;
import org.sd.util.GeneralUtil;
import org.sd.util.tree.Tree;
import org.sd.util.tree.TreeUtil;
import org.sd.xml.DataProperties;
import org.sd.xml.DomDocument;
import org.sd.xml.DomElement;
import org.sd.xml.XmlFactory;

/**
 * Utility class for analyzing an atn grammar.
 * <p>
 * @author Spence Koehler
 */
public class AtnGrammarAnalyzer {
  
  private AtnGrammar grammar;
  private AtnParseOptions parseOptions;
  private List<AtnRule> startRules;
  private Set<String> _terminalCategories;

  public AtnGrammarAnalyzer(AtnGrammar grammar) {
    this(grammar, null);
  }
    
  public AtnGrammarAnalyzer(AtnGrammar grammar, AtnParseOptions parseOptions) {
    this.grammar = grammar;
    this.parseOptions = parseOptions;
    this.startRules = grammar.getStartRules(parseOptions);
    this._terminalCategories = null;
  }

  public AtnGrammar getGrammar() {
    return grammar;
  }

  public AtnParseOptions getParseOptions() {
    return parseOptions;
  }

  public Map<String, List<Tree<String>>> buildTrees(boolean infoMode, TextGenerator textGenerator) {
    final Map<String, List<Tree<String>>> result = new HashMap<String, List<Tree<String>>>();

    for (AtnRule startRule : startRules) {
      final String ruleData = buildVisualRuleName(startRule, infoMode);

      if (textGenerator != null) textGenerator.startRule(this, startRule);
      final List<Tree<String>> trees = buildTrees(startRule, ruleData, infoMode, textGenerator, new HashSet<String>());
      if (textGenerator != null) textGenerator.endRule(this, startRule, trees);

      String key = startRule.getRuleId();
      if (key == null || "".equals(key)) key = startRule.getRuleName();
      List<Tree<String>> curTrees = result.get(key);
      if (curTrees == null) {
        curTrees = new ArrayList<Tree<String>>();
        result.put(key, curTrees);
      }
      curTrees.addAll(trees);
    }

    return result;
  }

  public Set<String> getTerminalCategories() {
    if (_terminalCategories == null) {
      final Set<String> result = new HashSet<String>();

      final Map<String, List<AtnRule>> cat2Rules = grammar.getCat2Rules();

      for (List<AtnRule> rules : cat2Rules.values()) {
        for (AtnRule rule : rules) {
          for (AtnRuleStep step : rule.getSteps()) {
            final String category = step.getCategory();
            if (!cat2Rules.containsKey(category)) {
              result.add(category);
            }
          }
        }
      }

      _terminalCategories = result;
    }

    return _terminalCategories;
  }

  public Parse getHardwiredParse(Tree<String> tree, TextGenerator textGenerator) {
    final String ruleId = (String)tree.getAttributes().get("_ruleId");
    final String parsedText = textGenerator == null ? tree.getLeafText() : textGenerator.getText(this, tree);
    return new Parse(ruleId, tree, parsedText);
  }

  public List<Parse> generateParses(AtnParseRunner parseRunner, Tree<String> tree, TextGenerator textGenerator) throws IOException {
    List<Parse> result = null;

    if (parseRunner != null) {
      final String input = textGenerator == null ? tree.getLeafText() : textGenerator.getText(this, tree);
      final ParseOutputCollector output = parseRunner.parseInputString(input);
      result = output.getParses();
    }

    return result;
  }


  public ParseInterpreter getParseInterpreter() {
    ParseInterpreter result = null;

    if (parseOptions != null) {
      result = parseOptions.getParseInterpreter();
    }

    return result;
  }


  private final List<Tree<String>> buildTrees(AtnRule rule, String ruleData, boolean infoMode, TextGenerator textGenerator, Set<String> ruleCategories) {
    final List<Tree<String>> result = new ArrayList<Tree<String>>();

    final String ruleCategory = rule.getRuleName();
    final String ruleId = rule.getRuleId();

    if (ruleCategories.contains(ruleCategory)) {
      // handle circular repeat
      final Tree<String> ruleTree = new Tree<String>(ruleData + "@");
      result.add(ruleTree);
      if (ruleId != null) ruleTree.getAttributes().put("_ruleId", ruleId);
    }
    else {
      ruleCategories.add(ruleCategory);
      final List<Collection<Tree<String>>> stepTreesList = buildStepTrees(rule, infoMode, textGenerator, ruleCategories);
      for (Collection<Tree<String>> stepTrees : stepTreesList) {
        final Tree<String> ruleTree = new Tree<String>(ruleData);
        for (Tree<String> stepTree : stepTrees) {
          ruleTree.addChild(stepTree);
        }
        result.add(ruleTree);
        if (ruleId != null) ruleTree.getAttributes().put("_ruleId", ruleId);
      }
    }

    return result;
  }

  private final List<Collection<Tree<String>>> buildStepTrees(AtnRule rule, boolean infoMode, TextGenerator textGenerator, Set<String> ruleCategories) {
    final List<Collection<Tree<String>>> stepTreesList = new ArrayList<Collection<Tree<String>>>();

    int stepnum = 0;
    for (AtnRuleStep step : rule.getSteps()) {
      final String stepData = buildVisualRuleStepName(step, infoMode, stepnum++);
      final List<Tree<String>> stepTrees = buildTrees(step, stepData, infoMode, textGenerator, ruleCategories);
      stepTreesList.add(stepTrees);
    }

    return GeneralUtil.combine(stepTreesList);
  }

  private final List<Tree<String>> buildTrees(AtnRuleStep step, String stepData, boolean infoMode, TextGenerator textGenerator, Set<String> ruleCategories) {
    final List<Tree<String>> result = new ArrayList<Tree<String>>();

    final String stepCategory = step.getCategory();
    final List<AtnRule> rules = grammar.getCat2Rules().get(stepCategory);
    if (rules == null) {
      final Tree<String> stepNode = new Tree<String>(stepData);
      result.add(stepNode);

      if (textGenerator != null) {
        final String example = textGenerator.getText(this, step);
        stepNode.addChild(example == null ? "" : example);
      }
    }
    else {
      for (AtnRule rule : rules) {
        result.addAll(buildTrees(rule, stepData, infoMode, textGenerator, new HashSet<String>(ruleCategories)));
      }
    }

    return result;
  }

  /**
   * Rule Name Form:
   * <p>
   * name/id
   * <ul>
   * <li>name -- the rule name</li>
   * <li>id -- the rule ID, if present</li>
   * </ul>
   */
  private final String buildVisualRuleName(AtnRule rule, boolean infoMode) {
    final StringBuilder result = new StringBuilder();

    result.append(rule.getRuleName());

    if (infoMode) {
      if (rule.getRuleId() != null && !"".equals(rule.getRuleId())) {
        result.append('/').append(rule.getRuleId());
      }
    }

    return result.toString();
  }

  /**
   * Rule Step Name Form:
   * <p>
   * ~name/label*_sD_r(c)_u(c).
   * <ul>
   * <li>~ -- present if token is not consumed</li>
   * <li>name -- step name (category to match) All-Caps if steps token is ignored.</li>
   * <li>label -- step label if present.</li>
   * <li>_sD -- skip D tokens if D &gt; 0</li>
   * <li>_r(c) -- if category, c, is required</li>
   * <li>_u(c) -- if unless  category, c</li>
   * <li>. -- if terminal</li>
   * </ul>
   */
  private final String buildVisualRuleStepName(AtnRuleStep step, boolean infoMode, int stepnum) {
    final StringBuilder result = new StringBuilder();

    if (!infoMode) {
      result.append(step.getLabel());
      return result.toString();
    }

    // consume
    if (!step.consumeToken()) {
      result.append('~');
    }

    // name
    String name = step.getCategory();

    if (step.getIgnoreToken()) {
      name = name.toUpperCase();
    }

    result.append(name);
    
    // /label
    final String label = step.getLabel();
    if (label != step.getCategory()) {
      result.append('/').append(label);
    }

    // skip
    if (step.getSkip() != 0) {
      result.append("_s").append(step.getSkip());
    }

    // require
    if (step.getRequire() != null) {
      result.append("_r(").append(step.getRequire()).append(")");
    }

    // unless
    if (step.getUnless() != null) {
      result.append("_u(").append(step.getUnless()).append(")");
    }

    // repeat
    char c = (char)0;
    if (step.isOptional()) {
      c = step.repeats() ? '*' : '?';
    }
    else {
      if (step.repeats()) {
        c = '+';
      }
    }

    if (c != 0) {
      result.append(c);
    }

    // terminal
    if (step.isTerminal()) {
      result.append('.');
    }

    return result.toString();
  }

  String prettyPrint(Tree<String> tree, TextGenerator textGenerator) {
    final StringBuilder result = new StringBuilder();

    result.
      append('\n').
      append(TreeUtil.prettyPrint(tree)).
      append('\n').
      append(textGenerator == null ? tree.getLeafText() : textGenerator.getText(this, tree));
      
    return result.toString();
  }


  //
  // Properties:
  //  grammarFile -- (required) path to Grammar file to analyze
  //
  public static final AtnGrammarAnalyzer buildInstance(ResourceManager resourceManager) throws IOException {
    final DataProperties options = resourceManager.getOptions();
    final File grammarFile = new File(options.getString("grammarFile"));
    final DomDocument grammarDocument = XmlFactory.loadDocument(grammarFile, false, options);
    final DomElement grammarElement = (DomElement)grammarDocument.getDocumentElement();
    final AtnGrammar grammar = new AtnGrammar(grammarElement, resourceManager);

    final AtnGrammarAnalyzer analyzer = new AtnGrammarAnalyzer(grammar);

    return analyzer;
  }

  //
  // Properties:
  //  exampleFile -- (optional) path to tab-delimited file with terminal categories and example words.
  //  textGenerator -- (optional, default=null) classpath to text generator to be built through the resourceManager
  //
  public static final TextGenerator buildTextGenerator(ResourceManager resourceManager) throws IOException {
    TextGenerator result = null;

    final DataProperties options = resourceManager.getOptions();

    final String exampleFilename = options.getString("exampleFile", null);
    final File exampleFile = (exampleFilename == null) ? null : new File(exampleFilename);

    final String textGeneratorString = options.getString("textGenerator", null);
    if (textGeneratorString == null) {
      if (exampleFile != null) {
        result = new StaticTextGenerator(exampleFile);
      }
    }
    else {
      Boolean restoreDisableLoad = null;
      if (options.hasProperty("_disableLoad")) {
        restoreDisableLoad = options.getBoolean("_disableLoad");
        resourceManager.setDisableLoad(false);
      }
      result = (TextGenerator)resourceManager.getResourceByClass(textGeneratorString);
      if (restoreDisableLoad != null) {
        resourceManager.setDisableLoad(restoreDisableLoad);
      }
    }

    return result;
  }

  public static void main(String[] args) throws IOException {
    //
    // Properties: (Mode 1: just analyze the grammar file, no plugin resources will be loaded)
    //  grammarFile -- (required) path to Grammar file to analyze
    //  infoMode -- (optional, default=true) true to denote grammar info;
    //              false to mirror generated parses.
    //  exampleFile -- (optional) path to tab-delimited file with terminal categories and example words.
    //  textGenerator -- (optional, default=null) classpath to text generator to be built through the resourceManager
    //
    // Properties: (Mode 2: analyze a specific grammar loaded through a parseConfig with its resources)
    //  parseConfig -- (required) path to parseConfig file with grammar to analyze
    //  resourcesDir -- (required) path to resources (e.g. "${HOME}/co/ancestry/resources")
    //  grammarId -- (required) identifies grammar to analyze in form of cpId:pId
    //  infoMode -- (optional, default=true) true to denote grammar info;
    //              false to mirror generated parses.
    //  exampleFile -- (optional) path to tab-delimited file with terminal categories and example words.
    //  textGenerator -- (optional, default=null) classpath to text generator to be built through the resourceManager
    //

    final DataProperties options = new DataProperties(args);

    ResourceManager resourceManager = null;
    AtnGrammarAnalyzer analyzer = null;

    if (options.hasProperty("parseConfig")) {
      // Mode 2
      final AtnParseRunner parseRunner = new AtnParseRunner(options);
      final ParseConfig parseConfig = parseRunner.getParseConfig();
      resourceManager = parseConfig.getResourceManager();
      
      final String grammarId = options.getString("grammarId");
      final String[] idPieces = grammarId.split(":");
      final AtnParserWrapper parserWrapper = parseConfig.getId2CompoundParser().get(idPieces[0]).getParserWrapper(idPieces[1]);
      final AtnGrammar grammar = parserWrapper.getParser().getGrammar();
      final AtnParseOptions parseOptions = parserWrapper.getParseOptions();
      analyzer = new AtnGrammarAnalyzer(grammar, parseOptions);
    }
    else {
      // Mode 1
      if (!options.hasProperty("_disableLoad")) options.set("_disableLoad", true);
      resourceManager = new ResourceManager(options);
      analyzer = buildInstance(resourceManager);
    }

    final Set<String> terminalCategories = analyzer.getTerminalCategories();
    System.out.println("Found " + terminalCategories.size() + " terminalCategories:");
    for (String terminalCategory : terminalCategories) {
      System.out.println("\t" + terminalCategory);
    }

    final TextGenerator textGenerator = buildTextGenerator(resourceManager);
    final boolean infoMode = options.getBoolean("infoMode", true);
    final Map<String, List<Tree<String>>> treeMaps = analyzer.buildTrees(infoMode, textGenerator);

    for (Map.Entry<String, List<Tree<String>>> entry : treeMaps.entrySet()) {
      final String id = entry.getKey();
      final List<Tree<String>> trees = entry.getValue();
      System.out.println(id + " has " + trees.size() + " trees");

      for (Tree<String> tree : trees) {
        //System.out.println(prettyPrint(tree, textGenerator) + "\n");
        final boolean stopAtEachTreeHere = true;
      }

      final boolean stopAtEachIdHere = true;
    }
  }
}
