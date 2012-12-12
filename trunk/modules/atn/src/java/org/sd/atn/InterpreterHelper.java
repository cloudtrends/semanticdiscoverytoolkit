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


import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.sd.util.tree.Tree;
import org.sd.util.tree.TreeUtil;
import org.sd.xml.DataProperties;
import org.sd.xml.DomDocument;
import org.sd.xml.DomElement;
import org.sd.xml.XmlFactory;

/**
 * Utility class for analyzing a grammar for building a ParseInterpreter.
 * <p>
 * @author Spence Koehler
 */
public class InterpreterHelper {
  
  private AtnGrammar grammar;
  private List<AtnRule> startRules;
  private Set<String> _terminalCategories;
  private Tree<NodeInfo> _categoryTree;
  private Map<String, UniqueKey> uniqueKeys;
  private Map<String, Tree<NodeInfo>> allNodeInfoNodes;

  public InterpreterHelper(AtnGrammar grammar) {
    this(grammar, null);
  }

  public InterpreterHelper(AtnGrammar grammar, AtnParseOptions parseOptions) {
    this.grammar = grammar;
    this.startRules = grammar.getStartRules(parseOptions);
    this._terminalCategories = null;
    this._categoryTree = null;
    this.uniqueKeys = new HashMap<String, UniqueKey>();
    this.allNodeInfoNodes = new HashMap<String, Tree<NodeInfo>>();
  }

  public AtnGrammar getGrammar() {
    return grammar;
  }

  public boolean isTerminalCategory(String category) {
    return getTerminalCategories().contains(category);
  }

  public Set<String> getTerminalCategories() {
    if (_terminalCategories == null) {
      _terminalCategories = buildTerminalCategories();
    }

    return _terminalCategories;
  }

  public Tree<NodeInfo> getCategoryTree() {
    if (_categoryTree == null) {
      _categoryTree = buildCategoryTree();
    }
    return _categoryTree;
  }

  
  private final Set<String> buildTerminalCategories() {
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

    return result;
  }

  private final Tree<NodeInfo> buildCategoryTree() {
    // NOTES:
    // - root holds non-null NodeInfo w/null data
    // - root's children are "top-level" (start) categories
    // - children are potential categories in parse trees under their parents across all rules

    final Tree<NodeInfo> root = new Tree<NodeInfo>(new NodeInfo(null));

    final List<RuleOrStepWrapper> startWrappers = new ArrayList<RuleOrStepWrapper>();
    for (AtnRule startRule : startRules) {
      startWrappers.add(new RuleOrStepWrapper(startRule));
    }

    doAddChildren(root, startWrappers);

    return root;
  }

  private final void doAddChildren(Tree<NodeInfo> parentNode, Collection<RuleOrStepWrapper> childWrappers) {
    for (Tree<NodeInfo> nodeInfoNode : buildNodeInfoNodes(childWrappers)) {
      parentNode.addChild(nodeInfoNode);

      final NodeInfo nodeInfo = nodeInfoNode.getData();
      if (!nodeInfo.hasReferenceNode()) {
        //TOOD: I'm here... recurse...
        final Set<RuleOrStepWrapper> nextChildWrappers = new LinkedHashSet<RuleOrStepWrapper>();
        for (RuleOrStepWrapper wrapper : nodeInfo.uniqueKey.wrappers) {
          // from each wrapper, find the next children (steps of rules or rules from steps)
          nextChildWrappers.addAll(wrapper.getNextWrappers(grammar));
        }

        // recurse
        doAddChildren(nodeInfoNode, nextChildWrappers);
      }
    }
  }

  private final List<Tree<NodeInfo>> buildNodeInfoNodes(Collection<RuleOrStepWrapper> wrappers) {
    // build the unique node info's for the wrappers (for child nodes)
    final List<Tree<NodeInfo>> result = new ArrayList<Tree<NodeInfo>>();
    final Map<String, UniqueKey> wrapperKeys = new LinkedHashMap<String, UniqueKey>();

    for (RuleOrStepWrapper wrapper : wrappers) {
      final UniqueKey uniqueKey = wrapper.getUniqueKey(uniqueKeys, getTerminalCategories());
      final String key = wrapper.getKey();
      wrapperKeys.put(key, uniqueKey);
    }

    for (UniqueKey uniqueKey : wrapperKeys.values()) {

      final NodeInfo nodeInfo = new NodeInfo(uniqueKey);
      final Tree<NodeInfo> curNode = new Tree<NodeInfo>(nodeInfo);

      Tree<NodeInfo> referenceNode = allNodeInfoNodes.get(uniqueKey.key);
      if (referenceNode != null) {
        // if already have a node for this unique key, set nodeInfo.referenceNode to it (and don't recurse)
        nodeInfo.setReferenceNode(referenceNode);
      }
      else {
        //otherwise, add this nodeInfo to the index to be referenced by other nodeInfos
        allNodeInfoNodes.put(uniqueKey.key, curNode);
      }

      result.add(curNode);
    }

    return result;
  }


  public static final class UniqueKey {
    public final String key;  // parse tree node label
    public final Set<RuleOrStepWrapper> wrappers;  // having same key
    private boolean repeats;
    private Set<String> equivalences;
    private boolean terminal;

    public UniqueKey(String key, boolean isTerminal) {
      this.key = key;
      this.wrappers = new LinkedHashSet<RuleOrStepWrapper>();
      this.repeats = false;
      this.equivalences = null;
      this.terminal = isTerminal;
    }

    public void addWrapper(RuleOrStepWrapper wrapper) {
      wrappers.add(wrapper);
      repeats |= wrapper.repeats();
      final String equiv = wrapper.getEquivalence();
      if (equiv != null) {
        if (equivalences == null) equivalences = new LinkedHashSet<String>();
        equivalences.add(equiv);
      }
    }

    public void setIsTerminal(boolean terminal) {
      this.terminal = terminal;
    }

    public boolean isTerminal() {
      return terminal;
    }

    public boolean repeats() {
      return repeats;
    }

    public Set<String> getEquivalences() {
      return equivalences;
    }
  }

  public static final class NodeInfo {
    public final UniqueKey uniqueKey;
    private Tree<NodeInfo> referenceNode;  // cyclical reference

    public NodeInfo(UniqueKey uniqueKey) {
      this.uniqueKey = uniqueKey;
      this.referenceNode = null;
    }

    public void setReferenceNode(Tree<NodeInfo> referenceNode) {
      this.referenceNode = referenceNode;
    }

    public boolean hasReferenceNode() {
      // e.g. isCyclacle
      return referenceNode != null;
    }

    public Tree<NodeInfo> getReferenceNode() {
      return referenceNode;
    }

    public String toString() {
      final StringBuilder result = new StringBuilder();

      // label
      if (uniqueKey == null) {
        result.append("NIL");
      }
      else {
        result.append(uniqueKey.key);
      }

      // is reference
      if (hasReferenceNode() && !uniqueKey.isTerminal()) {
        result.insert(0, '&');
      }

      // repeats
      if (uniqueKey != null && uniqueKey.repeats()) {
        result.append('+');
      }

      // equivalences " =[X1,...,Xn]"
      if (uniqueKey != null) {
        final Set<String> equivalences = uniqueKey.getEquivalences();
        if (equivalences != null) {
          result.append(" =[");
          int equivNum = 0;
          for (String equivalence : equivalences) {
            if (equivNum++ > 0) result.append(',');
            result.append(equivalence);
          }
          result.append(']');
        }
      }

      return result.toString();
    }
  }

  private static final class RuleOrStepWrapper {
    public final AtnRule rule;
    public final AtnRuleStep step;
    private boolean addedToUniqueKey;

    private RuleOrStepWrapper(AtnRule rule) {
      this.rule = rule;
      this.step = null;
      this.addedToUniqueKey = false;
    }

    private RuleOrStepWrapper(AtnRuleStep step) {
      this.rule = null;
      this.step = step;
      this.addedToUniqueKey = false;
    }

    public String getKey() {
      String key = null;

      //NOTE: this is the String that will show up in a generated parse tree node

      if (rule != null) {
        key = rule.getRuleName();
      }
      else if (step != null) {
        key = step.getLabel();
      }

      return key;
    }

    /**
     * Get the unique key for this instance.
     */
    public UniqueKey getUniqueKey(Map<String, UniqueKey> uniqueKeys, Set<String> terminalCategories) {
      final String key = getKey();
      UniqueKey result = uniqueKeys.get(key);

      if (result == null) {
        result = new UniqueKey(key, terminalCategories.contains(key));
        uniqueKeys.put(key, result);
      }

      if (!addedToUniqueKey) {
        result.addWrapper(this);
        this.addedToUniqueKey = true;
      }

      return result;
    }

    public boolean repeats() {
      boolean result = false;

      if (step != null) {
        result = step.repeats();
      }

      return result;
    }

    public String getEquivalence() {
      String result = null;

      if (step != null) {
        final String equiv = step.getCategory();
        if (!equiv.equals(step.getLabel())) {
          result = equiv;
        }
      }

      return result;
    }

    public List<RuleOrStepWrapper> getNextWrappers(AtnGrammar grammar) {
      final List<RuleOrStepWrapper> result = new ArrayList<RuleOrStepWrapper>();

      addNextWrappers(result, this.rule);

      if (step != null) {
        // find the rules this step leads to
        final List<AtnRule> nextRules = grammar.getCat2Rules().get(step.getCategory());
        if (nextRules != null) {
          for (AtnRule rule : nextRules) {
            addNextWrappers(result, rule);
          }
        }
      }

      return result;
    }

    private final void addNextWrappers(List<RuleOrStepWrapper> result, AtnRule rule) {
      if (rule != null) {
        // collect this rule's steps
        for (AtnRuleStep step : rule.getSteps()) {
          result.add(new RuleOrStepWrapper(step));
        }
      }
    }

    public boolean equals(Object other) {
      boolean result = this == other;

      if (!result && other instanceof RuleOrStepWrapper) {
        final RuleOrStepWrapper otherWrapper = (RuleOrStepWrapper)other;
        result = (this.rule == otherWrapper.rule && this.step == otherWrapper.step);
      }

      return result;
    }

    public int hashCode() {
      int result = 11;

      if (rule != null) result = result * 11 + rule.hashCode();
      if (step != null) result = result * 11 + step.hashCode();

      return result;
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
    final InterpreterHelper helper = new InterpreterHelper(grammar);

    final Tree<NodeInfo> categoryTree = helper.getCategoryTree();
    System.out.println(TreeUtil.prettyPrint(categoryTree));
  }
}
