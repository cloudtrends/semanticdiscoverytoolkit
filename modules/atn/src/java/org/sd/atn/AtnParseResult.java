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
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import org.sd.token.Token;
import org.sd.util.InputContext;
import org.sd.util.tree.Tree;

/**
 * Container for the result of a parse that holds ambiguous parses.
 * <p>
 * @author Spence Koehler
 */
public class AtnParseResult {
  
  private AtnGrammar grammar;
  private Set<Integer> stopList;

  private Token firstToken;
  public Token getFirstToken() {
    return firstToken;
  }

  private AtnParseOptions options;
  public AtnParseOptions getOptions() {
    return options;
  }
  public void setOptions(AtnParseOptions options) {
    this.options = options;
  }

  private Tree<AtnState> parse;
  private LinkedList<AtnState> states;
  private List<AtnRule> startRules;
  private int startRuleIndex;

  private List<AtnParse> _parses;

  /**
   * Determine whether all parses have been generated for this result. 
   */
  public boolean isComplete() {
    return states.size() == 0 && startRuleIndex == startRules.size();
  }

  /**
   * Get the text that precedes this parse result, which will be empty
   * when the FirstToken is the first token from its tokenizer.
   */
  public String getPriorText() {
    return firstToken.getTokenizer().getPriorText(firstToken);
  }

  /**
   * Get the input context associated with this parse result's input or null.
   */
  public InputContext getInputContext() {
    return firstToken.getTokenizer().getInputContext();
  }

  AtnParseResult(AtnGrammar grammar, Token firstToken, AtnParseOptions options, Set<Integer> stopList) {
    this.grammar = grammar;
    this.firstToken = firstToken;
    this.options = options;
    this.stopList = stopList;

    this.parse = new Tree<AtnState>(null);
    this.states = new LinkedList<AtnState>();
    this.startRules = grammar.getStartRules(options);
    this.startRuleIndex = 0;

    this._parses = null;
  }

  /**
   * Get the number of parses currently available in this result.
   * 
   * Note that more parses *may* be available if this result is not yet
   * complete. Complete the parsing by invoking ContinueParsing until
   * IsComplete is true.
   */
  public int getNumParses() {
    final List<AtnParse> parses = getParses();
    return (parses == null) ? 0 : parses.size();
  }

  /**
   * Get the designated parse from 0 to GetNumParses (exclusive).
   * 
   * The number of parses *may* be increased by calling CointinueParsing
   * (for the next 1) or GenerateParses (for the next N), but this may
   * also change the parse number for any parse already retrieved.
   */
  public AtnParse getParse(int parseNum) {
    AtnParse result = null;

    final List<AtnParse> parses = getParses();

    if (parses != null && parseNum < parses.size()) {
      result = parses.get(parseNum);
    }

    return result;
  }

  private List<AtnParse> getParses() {
    if (_parses == null) {
      _parses = new ArrayList<AtnParse>();

      int curParseNum = 0;
      for (Iterator<Tree<AtnState>> iter = parse.iterator(Tree.Traversal.DEPTH_FIRST); iter.hasNext(); ) {
        final Tree<AtnState> curState = iter.next();
        if (isValidEndNode(curState)) {
          final AtnParse curParse = new AtnParse(curParseNum, curState, this);
          _parses.add(curParse);
          
          ++curParseNum;
        }
      }
    }
    return _parses;
  }

  private boolean isValidEndNode(Tree<AtnState> stateNode) {
    return (stateNode.getData() == null) ? false : stateNode.getData().isValidEnd(stopList);
  }

  /**
   * Generate (or ensure that at least) numParses of the parses (have been
   * generated). Return the true current number of parses.
   * 
   * If numParses == 0, then generate *all* possible parses.
   * 
   * Because continuing parsing can change parse tree numbers, pre-generating
   * a specified number of parses can guarantee consistency in those parses'
   * numbers until ContinueParsing is called again.
   * 
   */
  public int generateParses(int numParses) {
    int curNumParses = getNumParses();
    for ( ; curNumParses < numParses || numParses == 0; curNumParses = getNumParses()) {
      if (!continueParsing()) break;
    }

    return curNumParses;
  }

  /**
   * Continue parsing if possible, returning whether another parse is complete.
   */
  public boolean continueParsing() {
    boolean success = false;

    while (startRuleIndex < startRules.size() || states.size() > 0) {
      if (states.size() == 0) {
        final AtnRule startRule = startRules.get(startRuleIndex);
        final Token firstToken = getFirstToken(startRule, this.firstToken);
        if (firstToken == null) return false;

        states.addLast(new AtnState(firstToken, startRule, 0, parse, options, 0, 0, null));
        ++startRuleIndex;
      }

      success = AtnState.matchTokenToRule(grammar, states, stopList);

      if (success && options.getFirstParseOnly()) {
        break;
      }
    }

    if (success) _parses = null;

    return success;
  }

  private Token getFirstToken(AtnRule startRule, Token firstToken) {
    final Token result = grammar.getAcceptedToken(startRule.getTokenFilterId(), firstToken, false, null, true, true);
    return result;
  }
}
