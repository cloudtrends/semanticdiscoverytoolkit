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
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.sd.token.Token;
import org.sd.util.Usage;
import org.sd.util.tree.NodePath;
import org.sd.util.tree.Tree;
import org.sd.xml.DomElement;
import org.sd.xml.DomNode;

/**
 * AtnParseSelector that chooses the longest parse.
 * <p>
 * @author Spence Koehler
 */
@Usage(notes =
       "An org.sd.atn.AtnParseSelector implementation that\n" +
       "chooses the longest parse(s), meaning those that cover the\n" +
       "most input"
  )
public class LongestParseSelector implements AtnParseSelector {
  
  private boolean simplest;
  private boolean onlyfirst;
  private NodePath<String> weightPath;
  private List<NodePath<String>> preferPaths;

  /**
   * Attribute 'simplest' (default 'true') accepts only longest parses with
   * the fewest number of nodes in their parse tree.
   * <p>
   * Attribute 'onlyfirst' (default 'false') accepts only the first (longest/
   * simplest) parse.
   * <p>
   * Attribute 'weight' (default null) chooses the parse with the "weightier"
   * parse (more characters) under the given node path.
   * <p>
   * Attribute 'prefer' (default null) specifies preferrence order of parses
   * having (comma delimited) node path matches.
   */
  public LongestParseSelector(DomNode domNode, ResourceManager resourceManager) {
    final DomElement domElement = (DomElement)domNode;
    this.simplest = domElement.getAttributeBoolean("simplest", true);
    this.onlyfirst = domElement.getAttributeBoolean("onlyfirst", false);
    this.weightPath = null;
    this.preferPaths = buildNodePaths(domElement.getAttributeValue("prefer", null));

    final String weightString = domElement.getAttributeValue("weight", null);
    if (weightString != null) {
      this.weightPath = new NodePath<String>(weightString);
    }
  }

  LongestParseSelector(boolean simplest, boolean onlyfirst) {
    this.simplest = simplest;
    this.onlyfirst = onlyfirst;
    this.weightPath = null;
  }

  protected boolean getSimplest() {
    return simplest;
  }

  protected void setSimplest(boolean simplest) {
    this.simplest = simplest;
  }

  protected boolean getOnlyFirst() {
    return onlyfirst;
  }

  protected void setOnlyFirst(boolean onlyfirst) {
    this.onlyfirst = onlyfirst;
  }

  public List<AtnParse> selectParses(AtnParseResult parseResult) {
    final List<ParseData> parseDatas = new ArrayList<ParseData>();

    for (int parseIndex = 0; parseIndex < parseResult.getNumParses(); ++parseIndex) {
      final AtnParse parse = parseResult.getParse(parseIndex);
      if (!parse.getSelected()) continue;
      parseDatas.add(new ParseData(parse, simplest, weightPath, preferPaths));
    }

    try {
      Collections.sort(parseDatas);
    }
    catch (IllegalArgumentException e) {
      //TODO: simplify ParseData compare so this doesn't happen.
      //      in the meantime, it seems to happen in inconsequential cases.
    }

    final Set<Integer> selected = new HashSet<Integer>();

    ParseData lastParseData = null;
    for (ParseData parseData : parseDatas) {
      if (lastParseData == null) {
        selected.add(parseData.getAtnParse().getParseNum());
      }
      else if (parseData.compareTo(lastParseData) != 0) {
        break;
      }
      else {
        selected.add(parseData.getAtnParse().getParseNum());
      }

      lastParseData = parseData;
    }


    final List<AtnParse> result = new ArrayList<AtnParse>();
    String note = "Not longest";

    // collect non-duplicate selected parses
    for (int parseIndex = 0; parseIndex < parseResult.getNumParses(); ++parseIndex) {
      final AtnParse parse = parseResult.getParse(parseIndex);
      boolean select = selected.contains(parse.getParseNum());

      if (select && isDuplicate(parse, result)) {
        note = "Is duplicate";
        select = false;
      }

      if (onlyfirst && select && result.size() > 0) {
        note = "Isn't first";
        select = false;
      }

      parse.setSelected(select);
      if (select) result.add(parse);
      else parse.addNote(note);
    }

    return result;
  }

  private static final boolean isDuplicate(AtnParse parse, List<AtnParse> parses) {
    boolean result = false;

    final Tree<String> parseTree = parse.getParseTree();

    for (AtnParse curParse : parses) {
      final Tree<String> curParseTree = curParse.getParseTree();
      if (parseTree.equals(curParseTree)) {
        result = true;
        break;
      }
    }

    return result;
  }

  private static final List<NodePath<String>> buildNodePaths(String pathString) {
    List<NodePath<String>> result = null;

    if (pathString != null && !"".equals(pathString)) {
      result = new ArrayList<NodePath<String>>();

      final String[] paths = pathString.split("\\s*,\\s*");
      for (String path : paths) {
        result.add(new NodePath<String>(path));
      }
    }

    return result;
  }


  private static final class ParseData implements Comparable<ParseData> {
    private AtnParse atnParse;
    private boolean simplest;
    private NodePath<String> weightPath;
    private List<NodePath<String>> preferData;

    private List<Token> skipTokens;
    private int skipCount;
    private int length;
    private int complexity;      // the lower, the simpler
    private int diversity;       // num different tags
    private int weight;          // prefer higher
    private int preferValue;     // prefer lower (when >= 0)
    private int prefPerplexity;  // require equality when simplest + prefer
    
    private boolean computedRulePattern = false;
    private String _rulePattern = null;

    ParseData(AtnParse atnParse, boolean simplest, NodePath<String> weightPath, List<NodePath<String>> preferData) {
      this.atnParse = atnParse;
      this.simplest = simplest;
      this.weightPath = weightPath;
      this.preferData = preferData;

      this.skipTokens = null;
      this.skipCount = 0;
      this.length = 0;
      this.complexity = 0;
      this.diversity = 0;
      this.weight = 0;
      this.preferValue = -1;
      this.prefPerplexity = 0;

      initialize();
    }

    private final void initialize() {
      this.length = atnParse.getEndIndex() - atnParse.getStartIndex();

      for (Tree<AtnState> stateNode = atnParse.getEndState(); stateNode != null; stateNode = stateNode.getParent()) {
        final AtnState pathState = stateNode.getData();
        if (pathState == null) break;

        ++complexity;

        if (pathState.isSkipped()) {
          final Token skipToken = pathState.getInputToken();
          if (skipTokens == null) skipTokens = new ArrayList<Token>();
          skipTokens.add(skipToken);
          skipCount += skipToken.getWordCount();
        }
      }

      if (weightPath != null) {
        final Tree<String> parseTree = atnParse.getParseTree();

        final Set<String> tagNames = new HashSet<String>();
        for (Tree<String> leafNode : parseTree.gatherLeaves()) {
          tagNames.add(leafNode.getParent().getData());
        }
        this.diversity = tagNames.size();

        final List<Tree<String>> weightedNodes = weightPath.apply(parseTree);
        if (weightedNodes != null) {
          for (Tree<String> weightedNode : weightedNodes) {
            weight += weightedNode.gatherLeaves().size();
          }
        }
      }

      if (preferData != null) {
        final Tree<String> parseTree = atnParse.getParseTree();
        int idx = 0;
        for (NodePath<String> preferPath : preferData) {
          final List<Tree<String>> preferNodes = preferPath.apply(parseTree);
          if (preferNodes != null) {
            preferValue = idx;
            prefPerplexity = computePerplexity(preferNodes);
            break;
          }
          ++idx;
        }
      }
    }

    private final int computePerplexity(List<Tree<String>> preferNodes) {
      // defined as the minimum number of siblings for any (selected) node
      // used as measure of "complexity" in context of preferred nodes
      int result = Integer.MAX_VALUE;

      for (Tree<String> preferNode : preferNodes) {
        final int curResult = preferNode.getNumSiblings();
        if (curResult < result) {
          result = curResult;
        }
      }

      return result;
    }

    public String toString() {
      return atnParse.getParseTree().toString();
    }

    AtnParse getAtnParse() {
      return atnParse;
    }

    public List<Token> getSkipTokens() {
      return skipTokens;
    }

    public int getSkipCount() {
      return skipCount;
    }

    public int getLength() {
      return length;
    }

    public int getComplexity() {
      return complexity;
    }

    public int getDiversity() {
      return diversity;
    }

    public int getWeight() {
      return weight;
    }

    public boolean equals(Object o) {
      boolean result = (this == o);

      if (!result && o instanceof ParseData) {
        final ParseData other = (ParseData)o;
        result = (this.compareTo(other) == 0);
      }

      return result;
    }

    public int hashCode() {
      // rulePattern, simplest, skipCount, length, complexity, diversity, weight, preferValue
      int result = 1;

      final String rulePattern = getRulePattern();
      if (rulePattern != null) {
        result = (result * 17) + rulePattern.hashCode();
      }

      if (simplest) {
        result = (result * 17) + 1;
      }

      result = (result * 17) + skipCount;
      result = (result * 17) + length;
      result = (result * 17) + complexity;
      result = (result * 17) + diversity;
      result = (result * 17) + weight;
      result = (result * 17) + preferValue;

      return result;
    }

    public int compareTo(ParseData other) {
      int result = this == other ? 0 : -1;

      if (result != 0) {
        final boolean rulePatternMatch =
          this.weight > 0 && other.weight > 0 &&
          hasMatchingRulePattern(other) &&
          diversity == other.getDiversity();

        if (length == other.getLength() &&
            (!simplest || complexity == other.getComplexity()) &&
            skipCount == other.getSkipCount() &&
            (!rulePatternMatch || weight == other.getWeight())) {
          result = 0;
        }
        else {
          // the parse that skips fewer tokens comes first
          result = compareSkips(other);

          // when still unresolved, the longest parse comes first
          if (result == 0) {
            result = other.getLength() - length;

            // when still unresolved, check preference order of parses
            if (result == 0 && preferData != null) {
              result = getPrefCompare(other);
            }

            // when still unresolved, the minimum complexity comes first
            if (result == 0 && simplest) {
              result = this.complexity - other.getComplexity();
            }

            // when still unresolved, the highest weight comes first
            // but only in the context of a matching rule pattern (id or ruleName)
            if (result == 0 && rulePatternMatch) {
              result = other.getWeight() - this.weight;
            }
          }
        }
      }

      return result;
    }

    private final int getPrefCompare(ParseData other) {
      int result = 0;

      if (preferData != null) {
        if (this.preferValue >= 0 && other.preferValue >= 0) {
          // as long as !simplest or prefPerplexities match,
          if (!simplest || this.prefPerplexity == other.prefPerplexity) {
            // lower prefer value comes first
            result = this.preferValue - other.preferValue;
          }
        }
      }

      return result;
    }

    private final String getRulePattern() {
      if (!computedRulePattern) {
        final AtnRule rule = atnParse.getStartRule();

        _rulePattern = rule.getRuleId();

        if (_rulePattern == null) {
          // fall back to rule name if no ID
          _rulePattern = rule.getRuleName();
        }
        computedRulePattern = true;
      }

      return _rulePattern;
    }

    private final boolean hasMatchingRulePattern(ParseData other) {

      final String pattern1 = this.getRulePattern();
      final String pattern2 = other.getRulePattern();

      final boolean result = (pattern1 == pattern2) || (pattern1 != null && pattern1.equals(pattern2));

      return result;
    }

    private final int compareSkips(ParseData other) {
      int result = 0;

      if (skipCount > 0 || other.skipCount > 0) {
        if (skipCount > 0 && other.skipCount > 0) {
          final boolean otherReducesThis = reducesSkipCount(other);
          final boolean thisReducesOther = other.reducesSkipCount(this);
          if (otherReducesThis && !thisReducesOther) {
            // prefer other
            result = 1;
          }
          else if (thisReducesOther && !otherReducesThis) {
            // prefer this
            result = -1;
          }
          // else, no preference
        }
        else if (skipCount > 0) {
          // if the other parse reduces this' skip count, other is better (+1)
          if (reducesSkipCount(other)) {
            result = 1;
          }
        }
        else { // other.skipCount > 0
          // if this reduces other's skip count, this is better (-1)
          if (other.reducesSkipCount(this)) {
            result = -1;
          }
        }
      }

      return result;
    }

    private final boolean reducesSkipCount(ParseData other) {
      boolean result = false;

      if (skipCount > 0) {
        for (Token skipToken : skipTokens) {
          final int overlap = other.overlapFlag(skipToken);
          if (overlap != 1) {
            result = true;
            break;
          }
        }
      }

      return result;
    }

    /**
     * Return
     * <ul>
     * <li>0 if token is fully covered by this parse;</li>
     * <li>1 if token is fully outside this parse;</li>
     * <li>-1 if token partially overlaps with this parse</li>
     * </ul>
     */
    private final int overlapFlag(Token token) {
      int result = 0;

      final int myStartIndex = atnParse.getStartIndex();
      final int myEndIndex = atnParse.getEndIndex();
      final int tokenStartIndex = token.getStartIndex();
      final int tokenEndIndex = token.getEndIndex();

      if (Token.encompasses(myStartIndex, myEndIndex, tokenStartIndex, tokenEndIndex)) {
        // count how many of token's words are not skipped by this
        if (skipCount == 0) {
          // nothing is skipped, so all are covered
          result = 0;
        }
        else {
          // if any part of the token is skipped, then not all are covered
          for (Token skipToken : skipTokens) {
            if (skipToken.encompasses(token)) {
              // token is entirely skipped, so not covered
              result = 1;
              break;
            }
            else if (skipToken.overlaps(token)) {
              // token is partially skipped, so not entirely covered
              result = -1;
            }
          }
        }
      }
      else if (Token.overlaps(myStartIndex, myEndIndex, tokenStartIndex, tokenEndIndex)) {
        // token is not entirely covered by this (and not at all if overlap is skipped)
        result = -1;
      }
      else {
        // else no coverage
        result = 1;
      }

      return result;
    }
  }
}
