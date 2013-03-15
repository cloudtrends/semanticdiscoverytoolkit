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


import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import org.sd.util.tree.Tree;
import org.sd.xml.DataProperties;

/**
 * Base class for analyzing a parse interpreter through a grammarAnalyzer.
 * <p>
 * @author Spence Koehler
 */
public class ParseAnalyzer {
  
  private DataProperties options;
  private AtnParseRunner parseRunner;
  private ResourceManager resourceManager;
  private AtnGrammarAnalyzer grammarAnalyzer;
  private TextGenerator textGenerator;
  private boolean infoMode;
  private ParseInterpreter parseInterpreter;

  public ParseAnalyzer() {
    this.options = null;
  }

  // Properties:
  //  parseConfig -- (required) path to parseConfig file with grammar to analyze
  //  resourcesDir -- (required) path to resources (e.g. "${HOME}/co/ancestry/resources")
  //  grammarId -- (required) identifies grammar to analyze in form of cpId:pId
  //  infoMode -- (optional, default=true) true to denote grammar info;
  //              false to mirror generated parses.
  //  exampleFile -- (optional) path to tab-delimited file with terminal categories and example words.
  //  textGenerator -- (optional, default=null) classpath to text generator to be built through the resourceManager
  public ParseAnalyzer(DataProperties options) throws IOException {
    this.options = options;

    final AtnParseRunner parseRunner = new AtnParseRunner(options);
    final String grammarId = options.getString("grammarId");
    final boolean infoMode = options.getBoolean("infoMode", true);

    init(parseRunner, grammarId, infoMode);
  }

  public final void init(AtnParseRunner parseRunner, String grammarId, boolean infoMode) throws IOException {
    this.parseRunner = parseRunner;
    this.infoMode = infoMode;

    final ParseConfig parseConfig = parseRunner.getParseConfig();
    this.resourceManager = parseConfig.getResourceManager();
      
    final String[] idPieces = grammarId.split(":");
    final AtnParserWrapper parserWrapper = parseConfig.getId2CompoundParser().get(idPieces[0]).getParserWrapper(idPieces[1]);
    final AtnGrammar grammar = parserWrapper.getParser().getGrammar();
    final AtnParseOptions parseOptions = parserWrapper.getParseOptions();
    this.grammarAnalyzer = new AtnGrammarAnalyzer(grammar, parseOptions);
    
    this.textGenerator = AtnGrammarAnalyzer.buildTextGenerator(resourceManager);

    this.parseInterpreter = grammarAnalyzer.getParseInterpreter();
  }

  public DataProperties getOptions() {
    return options;
  }

  public AtnParseRunner getParseRunner() {
    return parseRunner;
  }

  public ResourceManager getResourceManager() {
    return resourceManager;
  }

  public AtnGrammarAnalyzer getGrammarAnalyzer() {
    return grammarAnalyzer;
  }

  public TextGenerator getTextGenerator() {
    return textGenerator;
  }

  public void setTextGenerator(TextGenerator textGenerator) {
    this.textGenerator = textGenerator;
  }

  public boolean getInfoMode() {
    return infoMode;
  }

  public void setInfoMode(boolean infoMode) {
    this.infoMode = infoMode;
  }

  public ParseInterpreter getParseInterpreter() {
    return parseInterpreter;
  }

  public Parse getHardwiredParse(Tree<String> tree) {
    return grammarAnalyzer.getHardwiredParse(tree, textGenerator);
  }

  public List<Parse> generateParses(Tree<String> tree, AtomicBoolean die) throws IOException {
    return grammarAnalyzer.generateParses(parseRunner, tree, textGenerator, die);
  }

  public Map<String, List<Tree<String>>> buildTrees() {
    return grammarAnalyzer.buildTrees(infoMode, textGenerator);
  }

  public void analyzeGrammarTrees() {
    final Map<String, List<Tree<String>>> treeMaps = buildTrees();

    for (Map.Entry<String, List<Tree<String>>> entry : treeMaps.entrySet()) {
      final String id = entry.getKey();
      final List<Tree<String>> trees = entry.getValue();

      analyzeGrammarTrees(id, trees);
    }
  }

  protected void analyzeGrammarTrees(String id, List<Tree<String>> trees) {
    final Set<String> interpXmlStrings = getInterpXml(trees);
    System.out.println("For id=" + id + ", found " + interpXmlStrings.size() + " unique interps from " + trees.size() + " trees.");

    int idx = 0;
    for (String interpXmlString : interpXmlStrings) {
      System.out.println("\t" + idx + ": " + interpXmlString);
      ++idx;
      //break;
    }

    for (Tree<String> tree : trees) {
      analyzeGrammarTree(id, tree);
    }
  }

  protected void analyzeGrammarTree(String id, Tree<String> tree) {
    final Parse parse = getHardwiredParse(tree);

    //final boolean stopHere = true;
  }

  public Set<String> getInterpXml(List<Tree<String>> trees) {
    return getInterpXml(trees, parseInterpreter);
  }

  public Set<String> getInterpXml(List<Tree<String>> trees, ParseInterpreter parseInterpreter) {
    final Set<String> result = new HashSet<String>();

    for (Tree<String> tree : trees) {
      final List<ParseInterpretation> interps = getInterps(tree, parseInterpreter);
      if (interps != null) {
        for (ParseInterpretation interp : interps) {
          result.add(interp.getInterpXml());
        }
      }
    }

    return result;
  }

  public List<ParseInterpretation> getInterps(Tree<String> tree) {
    return getInterps(tree, parseInterpreter);
  }

  public List<ParseInterpretation> getInterps(Tree<String> tree, ParseInterpreter parseInterpreter) {
    final Parse parse = getHardwiredParse(tree);
    return parseInterpreter.getInterpretations(parse, null);
  }


  public static void main(String[] args) throws IOException {
    final DataProperties options = new DataProperties(args);
    final ParseAnalyzer analyzer = new ParseAnalyzer(options);
    analyzer.analyzeGrammarTrees();
  }
}
