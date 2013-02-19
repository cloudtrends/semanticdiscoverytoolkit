/*
    Copyright 2013 Semantic Discovery, Inc. (www.semanticdiscovery.com)

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


import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.HashSet;
import java.util.Set;
import org.sd.io.FileUtil;
import org.sd.util.DotMaker;
import org.sd.xml.DataProperties;
import org.sd.xml.DomDocument;
import org.sd.xml.DomElement;
import org.sd.xml.XmlFactory;

/**
 * Utility to generate a graphical (graphviz) view of a grammar.
 * <p>
 * @author Spence Koehler
 */
public class Grammar2Dot extends DotMaker {

  private AtnGrammar grammar;
  private AtnParseOptions parseOptions;

  public Grammar2Dot(AtnGrammar grammar) {
    this(grammar, null);
  }

  public Grammar2Dot(AtnGrammar grammar, AtnParseOptions parseOptions) {
    super();

    this.grammar = grammar;
    this.parseOptions = parseOptions;

    setGraphAttribute("rankdir", "LR");
    setNodeAttribute("shape", "ellipse"); //ellipse?
    setNodeAttribute("width", ".1");
    setNodeAttribute("height", ".1");
    setNodeAttribute("fontsize", "8");
    setEdgeAttribute("fontsize", "8");
  }

  protected void populateEdges() {

    final LinkedList<AtnRule> todo = new LinkedList<AtnRule>(grammar.getStartRules(parseOptions));
    final Set<AtnRule> done = new HashSet<AtnRule>();

    while (todo.size() > 0) {
      final AtnRule curRule = todo.removeFirst();
      if (!done.contains(curRule)) {
        done.add(curRule);
        populateEdges(curRule, todo);
      }
    }
  }

  private final void populateEdges(AtnRule curRule, LinkedList<AtnRule> todo) {
    final String label = curRule.getRuleName();
    int prevId = -1;
    for (int stepNum = 0; stepNum < curRule.getNumSteps(); ++stepNum) {
      final AtnRuleStep ruleStep = curRule.getStep(stepNum);
      if (prevId < 0) prevId = addId2Label(label + "-" + stepNum);
      final int nextId = addId2Label(label + "-" + (stepNum + 1));
      addEdge(prevId, nextId, ruleStep.getLabel());
      prevId = nextId;

      final List<AtnRule> moreRules = grammar.getCat2Rules().get(ruleStep.getCategory());
      if (moreRules != null) todo.addAll(moreRules);
    }
  }


  public static void main(String[] args) throws IOException {
    // Properties:
    //   grammarFile=path to grammarFile 
    //   dotFile=path to dotFile
    //
    final DataProperties options = new DataProperties(args);
    args = options.getRemainingArgs();

    if (!options.hasProperty("_disableLoad")) options.set("_disableLoad", true);
    final ResourceManager resourceManager = new ResourceManager(options);
    final File grammarFile = new File(options.getString("grammarFile"));
    final DomDocument grammarDocument = XmlFactory.loadDocument(grammarFile, false, options);
    final DomElement grammarElement = (DomElement)grammarDocument.getDocumentElement();
    final AtnGrammar grammar = new AtnGrammar(grammarElement, resourceManager);

    final File dotFile = new File(options.getString("dotFile"));
    final BufferedWriter writer = FileUtil.getWriter(dotFile);

    final Grammar2Dot grammar2Dot = new Grammar2Dot(grammar);
    grammar2Dot.writeDot(writer);

    writer.close();
  }
}
