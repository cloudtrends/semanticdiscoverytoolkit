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


import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import org.sd.atn.ResourceManager;
import org.sd.util.DotWrapper;
import org.sd.util.GeneralUtil;
import org.sd.util.tree.Tree;
import org.sd.util.tree.Tree2Dot;
import org.sd.util.tree.TreeUtil;
import org.sd.xml.DataProperties;
import org.sd.xml.DomDocument;
import org.sd.xml.DomElement;
import org.sd.xml.TreeDiffViewer;
import org.sd.xml.XmlFactory;
import org.sd.xml.XmlLite;

/**
 * Utility class for analyzing an atn grammar.
 * <p>
 * @author Spence Koehler
 */
public class AtnGrammarAnalyzer {
  
  private static final int MAX_BRANCH = 128;


  private AtnGrammar grammar;
  private AtnParseOptions parseOptions;
  private List<AtnRule> startRules;
  private Set<String> _terminalCategories;
  private boolean onlyConsistentCombos;
  private File imageDir;

  private static final ParseInterpreter identityInterpreter = new IdentityParseInterpreter(false);

  public AtnGrammarAnalyzer(AtnGrammar grammar) {
    this(grammar, null);
  }
    
  public AtnGrammarAnalyzer(AtnGrammar grammar, AtnParseOptions parseOptions) {
    this.grammar = grammar;
    this.parseOptions = parseOptions;
    this.startRules = grammar.getStartRules(parseOptions);
    this._terminalCategories = null;
    this.onlyConsistentCombos = true;
    this.imageDir = null;
  }

  public AtnGrammar getGrammar() {
    return grammar;
  }

  public AtnParseOptions getParseOptions() {
    return parseOptions;
  }

  public boolean onlyConsistentCombos() {
    return onlyConsistentCombos;
  }

  /**
   * Be wary of setting this to false because the combinatorics can (and will)
   * get out of hand for anything more than the most simplistic grammars!
   */
  public void setOnlyConsistentCombos(boolean onlyConsistentCombos) {
    this.onlyConsistentCombos = onlyConsistentCombos;
  }

  public File getImageDir() {
    return imageDir;
  }

  public void setImageDir(File imageDir) {
    this.imageDir = imageDir;
  }

  public List<AtnRule> getStartRules() {
    return startRules;
  }

  public void setStartRules(List<AtnRule> startRules) {
    this.startRules = startRules;
  }

  public Map<String, List<Tree<String>>> buildTrees(boolean infoMode, TextGenerator textGenerator) {
    return buildTrees(infoMode, textGenerator, null);
  }

  public Map<String, List<Tree<String>>> buildTrees(boolean infoMode, TextGenerator textGenerator, List<String> pivotCategories) {
    final Map<String, List<Tree<String>>> result = new HashMap<String, List<Tree<String>>>();

    for (AtnRule startRule : startRules) {
      final List<Map<String, Integer>> consistentCombos = buildConsistentCombos(startRule);
      final String ruleData = buildVisualRuleName(startRule, infoMode);

      for (Map<String, Integer> consistentCombo : consistentCombos) {
        doBuildTrees(startRule, ruleData, infoMode, textGenerator, pivotCategories, consistentCombo, result);
      }
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

  public List<Parse> generateParses(AtnParseRunner parseRunner, Tree<String> tree,
                                    TextGenerator textGenerator, AtomicBoolean die) throws IOException {
    List<Parse> result = null;

    if (parseRunner != null) {
      final String input = textGenerator == null ? tree.getLeafText() : textGenerator.getText(this, tree);
      final ParseOutputCollector output = parseRunner.parseInputString(input, null, die);
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


  private final void doBuildTrees(AtnRule startRule, String ruleData, boolean infoMode,
                                  TextGenerator textGenerator,
                                  List<String> pivotCategories,
                                  Map<String, Integer> consistentCategories,
                                  Map<String, List<Tree<String>>> result) {

    final Set<String> ruleCategories = pivotCategories == null ? new HashSet<String>() : new HashSet<String>(pivotCategories);

    if (textGenerator != null) textGenerator.startRule(this, startRule);
    final List<Tree<String>> trees = buildTrees(startRule, ruleData, infoMode, textGenerator, ruleCategories, consistentCategories);
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

  /**
   * @param rule holds the current rule
   * @param ruleData holds the current rule's node name text
   * @param infoMode controls the detail of the node names
   * @param ruleCategories detects circular rules
   * @param consistentCategories enforces only building trees with the same version of a branching rule
   */
  private final List<Tree<String>> buildTrees(AtnRule rule, String ruleData, boolean infoMode, TextGenerator textGenerator, Set<String> ruleCategories, Map<String, Integer> consistentCategories) {
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
      final List<Collection<Tree<String>>> stepTreesList = buildStepTrees(rule, infoMode, textGenerator, ruleCategories, consistentCategories);
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

  private final List<Collection<Tree<String>>> buildStepTrees(AtnRule rule, boolean infoMode, TextGenerator textGenerator, Set<String> ruleCategories, Map<String, Integer> consistentCategories) {
    final List<Collection<Tree<String>>> stepTreesList = new ArrayList<Collection<Tree<String>>>();

    int stepnum = 0;
    for (AtnRuleStep step : rule.getSteps()) {
      final String stepData = buildVisualRuleStepName(step, infoMode, stepnum++);
      List<Tree<String>> stepTrees = buildTrees(step, stepData, infoMode, textGenerator, ruleCategories, consistentCategories);
      if (stepTrees.size() > MAX_BRANCH) {
//System.out.println("rule '" + rule.getRuleName() + "' step '" + step.getCategory() + "' has " + stepTrees.size() + " stepTrees. Pruning to " + MAX_BRANCH);
        stepTrees = stepTrees.subList(0, MAX_BRANCH);
      }
      for (Iterator<Tree<String>> iter = stepTrees.iterator(); iter.hasNext(); ) {
        final Tree<String> curStepTree = iter.next();
        boolean isDupe = false;
        for (Tree<String> possibleDupe : stepTrees) {
          if (curStepTree == possibleDupe) continue;
          if (curStepTree.equals(possibleDupe)) {
            isDupe = true;
            break;
          }
        }
        if (isDupe) iter.remove();
      }
      if (stepTrees.size() > 0) {
        stepTreesList.add(stepTrees);
      }
    }

//System.out.println("\tcombining stepTreesList!");
    return GeneralUtil.combine(stepTreesList);
  }

  private final List<Tree<String>> buildTrees(AtnRuleStep step, String stepData, boolean infoMode, TextGenerator textGenerator, Set<String> ruleCategories, Map<String, Integer> consistentCategories) {
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
      boolean done = false;
      if (rules.size() > 1) {
        // found a branching rule
        if (consistentCategories != null && consistentCategories.get(stepCategory) != null) {
          // only follow consistent branch
          final int branchNum = consistentCategories.get(stepCategory);
//System.out.println("Category '" + stepCategory + "' branches *" + rules.size() + " following " + branchNum);
          final AtnRule rule = rules.get(branchNum);
          result.addAll(buildTrees(rule, stepData, infoMode, textGenerator, new HashSet<String>(ruleCategories), consistentCategories));

          done = true;
        }
//System.out.println("Category '" + stepCategory + "' branches *" + rules.size());
      }

      if (!done) {
        for (AtnRule rule : rules) {
          result.addAll(buildTrees(rule, stepData, infoMode, textGenerator, new HashSet<String>(ruleCategories), consistentCategories));
        }
      }
    }

    return result;
  }

  private final List<Map<String, Integer>> buildConsistentCombos(AtnRule rule) {
    List<Map<String, Integer>> result = null;

    if (onlyConsistentCombos) {
      result = getConsistentCategoryCombos(rule);
    }
    else {
      result = new ArrayList<Map<String, Integer>>();
    }

    if (result.size() == 0) {
      result.add(null);
    }

    return result;
  }

  private final List<Map<String, Integer>> getConsistentCategoryCombos(AtnRule rule) {
    final List<String> branchingCats = getBranchingCategories(rule);
    final List<Collection<CatBranch>> catBranches = generateCatBranches(branchingCats);
    final List<Collection<CatBranch>> combos = GeneralUtil.combine(catBranches);

    final List<Map<String, Integer>> result = new ArrayList<Map<String, Integer>>();

    for (Collection<CatBranch> combo : combos) {
      final Map<String, Integer> map = new HashMap<String, Integer>();      

      for (CatBranch catBranch : combo) {
        map.put(catBranch.cat, catBranch.branch);
      }

      result.add(map);
    }

    return result;
  }

  private static final class CatBranch {
    public final String cat;
    public final int branch;

    CatBranch(String cat, int branch) {
      this.cat = cat;
      this.branch = branch;
    }
  }

  public List<String> getBranchingCategories() {
    return getBranchingCategories(null);
  }

  public List<String> getBranchingCategories(AtnRule rule) {
    List<String> result = null;

    if (rule == null) {
      result = findAllBranchingCategories();
    }
    else {
      result = new ArrayList<String>();
      findBranchingCategories(rule, result, new HashSet<String>());
//System.out.println("Branching categories for '" + rule.getRuleName() + "' are: " + result);
    }

    return result;
  }

  private final void findBranchingCategories(AtnRule rule, List<String> result, Set<String> ruleCategories) {
    for (AtnRuleStep step : rule.getSteps()) {
      final String category = step.getCategory();
      if (!ruleCategories.contains(category)) {
        final List<AtnRule> rules = grammar.getCat2Rules().get(category);
        ruleCategories.add(category);
        if (rules != null) {
          if (rules.size() > 1) {
            result.add(category);
          }
          for (AtnRule nextRule : rules) {
            findBranchingCategories(nextRule, result, ruleCategories);
          }
        }
      }
    }
  }

  private final List<String> findAllBranchingCategories() {
    final List<String> result = new ArrayList<String>();

    for (Map.Entry<String, List<AtnRule>> entry : grammar.getCat2Rules().entrySet()) {
      final String key = entry.getKey();
      final List<AtnRule> rules = entry.getValue();

      if (rules.size() > 1) {
        result.add(key);
      }
    }

    return result;
  }

  private final List<Collection<CatBranch>> generateCatBranches(List<String> branchingCats) {
    final List<Collection<CatBranch>> result = new ArrayList<Collection<CatBranch>>();
    for (String branchingCat : branchingCats) {
      final List<CatBranch> catBranches = new ArrayList<CatBranch>();
      final int branchCount = grammar.getCat2Rules().get(branchingCat).size();
      for (int branchNum = 0; branchNum < branchCount; ++branchNum) {
        catBranches.add(new CatBranch(branchingCat, branchNum));
      }
      result.add(catBranches);
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
    if (step.getRequireString() != null) {
      result.append("_r(").append(step.getRequireString()).append(")");
    }

    // unless
    if (step.getUnlessString() != null) {
      result.append("_u(").append(step.getUnlessString()).append(")");
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

  private final void writeTreeImage(Tree<String> tree, String label) throws IOException {
    if (imageDir != null) {
      final Tree2Dot<String> tree2dot = new Tree2Dot<String>(tree);
      final DotWrapper dotWrapper = new DotWrapper(tree2dot, imageDir, label + ".");
      final File imageFile = dotWrapper.getImageFile();
      System.out.println("Created imageFile " + imageFile.getAbsolutePath());
    }
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

  String diff(Tree<String> tree1, Tree<String> tree2) {
    String result = null;

    final Tree<XmlLite.Data> xmlTree1 = asXmlTree(tree1, null);
    final Tree<XmlLite.Data> xmlTree2 = asXmlTree(tree2, null);
    final ByteArrayOutputStream bytesOut = new ByteArrayOutputStream();
    final PrintStream out = new PrintStream(bytesOut);
    try {
      TreeDiffViewer.view2(xmlTree1, xmlTree2, out);
      result = bytesOut.toString("UTF-8");
    }
    catch (Exception e) {
      // eat it
    }
    finally {
      out.close();
      try {
        bytesOut.close();
      }
      catch (IOException e) {
        // eat it
      }
    }

    return result;
  }

  public Tree<XmlLite.Data> asXmlTree(Tree<String> tree, TextGenerator textGenerator) {
    final Parse parse = getHardwiredParse(tree, textGenerator);
    final List<ParseInterpretation> interps = identityInterpreter.getInterpretations(parse, null);
    final ParseInterpretation interp = interps.get(0);
    return interp.getInterpTree();
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


  public void handleTreeMaps(String header, Map<String, List<Tree<String>>> treeMaps, TextGenerator textGenerator, String label) throws IOException {
    System.out.println("\n" + header);
    for (Map.Entry<String, List<Tree<String>>> entry : treeMaps.entrySet()) {
      final String id = entry.getKey();
      final List<Tree<String>> trees = entry.getValue();
      System.out.println(id + " has " + trees.size() + " trees");

      Tree<String> firstTree = null;
      int treeNum = 0;
      for (Tree<String> tree : trees) {
        if (firstTree == null) firstTree = tree;

        // if label != null && imageDir != null, write out image
        if (label != null) {
          writeTreeImage(tree, label + "." + id + "." + (treeNum++));
        }

        //System.out.println(prettyPrint(tree, textGenerator) + "\n");
//        if ("person.birthDeath".equals(id)) {
        final boolean stopAtEachTreeHere = true;
//        }
      }

      final boolean stopAtEachIdHere = true;
    }
  }

  public static void main(String[] args) throws IOException {
    // 
    // Common Properties:
    //  infoMode -- (optional, default=true) true to denote grammar info;
    //              false to mirror generated parses.
    //  exampleFile -- (optional) path to tab-delimited file with terminal categories and example words.
    //  textGenerator -- (optional, default=null) classpath to text generator to be built through the resourceManager
    //  consistent -- (optional, default=true) true to only generate consistent trees
    //  pivot -- (optional) comma-delimited list of pivot categories
    //  imageDir -- (optional) path to directory to dump images
    //
    // Properties: (Mode 1: just analyze the grammar file, no plugin resources will be loaded)
    //  grammarFile -- (required) path to Grammar file to analyze
    //
    // Properties: (Mode 2: analyze a specific grammar loaded through a parseConfig with its resources)
    //  parseConfig -- (required) path to parseConfig file with grammar to analyze
    //  resourcesDir -- (required) path to resources (e.g. "${HOME}/co/ancestry/resources")
    //  grammarId -- (required) identifies grammar to analyze in form of cpId:pId
    //

    final DataProperties options = new DataProperties(args);

    ResourceManager resourceManager = null;
    AtnGrammarAnalyzer analyzer = null;

    final boolean consistent = options.getBoolean("consistent", true);

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

    final String imageDirString = options.getString("imageDir", null);
    if (imageDirString != null) {
      final File imageDir = new File(imageDirString);
      analyzer.setImageDir(imageDir);
    }

    analyzer.setOnlyConsistentCombos(consistent);
    final Set<String> terminalCategories = analyzer.getTerminalCategories();
    System.out.println("Found " + terminalCategories.size() + " terminalCategories:");
    for (String terminalCategory : terminalCategories) {
      System.out.println("\t" + terminalCategory);
    }

    final TextGenerator textGenerator = buildTextGenerator(resourceManager);
    final boolean infoMode = options.getBoolean("infoMode", true);

    // check for pivot
    final String pivotString = options.getString("pivot", null);
    if (pivotString != null) {
      final List<String> pivotCategories = new ArrayList<String>();
      final String[] pivots = pivotString.split("\\s*,\\s*");
      for (String pivot : pivots) {
        pivotCategories.add(pivot);
      }

      // Generate trees down to pivots
      final Map<String, List<Tree<String>>> treeMaps = analyzer.buildTrees(infoMode, textGenerator, pivotCategories);
      analyzer.handleTreeMaps("Main trees with pivots: " + pivotCategories, treeMaps, textGenerator, "topLevel");

      // Generate trees for each pivot
      for (String pivot : pivots) {
        final List<AtnRule> startRules = analyzer.getGrammar().getCat2Rules().get(pivot);
        if (startRules != null) {
          final List<String> curPivotCategories = new ArrayList<String>(pivotCategories);
          curPivotCategories.remove(pivot);
          analyzer.setStartRules(startRules);
          final Map<String, List<Tree<String>>> pivotTreeMaps = analyzer.buildTrees(infoMode, textGenerator, curPivotCategories);
          analyzer.handleTreeMaps("'" + pivot + "' pivot trees", pivotTreeMaps, textGenerator, pivot);
        }
      }
    }
    else {
      // no pivot, generate and show all trees

      final Map<String, List<Tree<String>>> treeMaps = analyzer.buildTrees(infoMode, textGenerator);
      analyzer.handleTreeMaps("Non-pivot trees", treeMaps, textGenerator, null);
    }
  }
}
