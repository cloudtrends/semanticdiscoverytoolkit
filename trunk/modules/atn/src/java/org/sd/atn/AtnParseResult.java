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
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import org.sd.token.Token;
import org.sd.util.InputContext;
import org.sd.util.tree.Tree;
import org.sd.xml.DataProperties;

/**
 * Container for the result of a parse that holds ambiguous parses.
 * <p>
 * @author Spence Koehler
 */
public class AtnParseResult {
  
  private AtnGrammar grammar;
  private Set<Integer> stopList;
  private AtomicBoolean die;

  private Token firstToken;
  public Token getFirstToken() {
    return firstToken;
  }

  private int seekStartIndex;
  public int getSeekStartIndex() {
    return seekStartIndex;
  }

  private AtnParseOptions options;
  public AtnParseOptions getOptions() {
    return options;
  }
  public void setOptions(AtnParseOptions options) {
    this.options = options;
  }

  // the compoundParserId that generated this result
  private String compoundParserId;
  public String getCompoundParserId() {
    return compoundParserId;
  }

  // the parserId that generated this result
  private String parserId;
  public String getParserId() {
    return parserId;
  }

  /** Set the IDs of the parser that generated this result. */
  public void setId(String compoundParserId, String parserId) {
    this.compoundParserId = compoundParserId;
    this.parserId = parserId;
  }


  private Tree<AtnState> parse;
  private LinkedList<AtnState> states;
  private LinkedList<AtnState> skipStates;
  private List<AtnRule> startRules;
  private int startRuleIndex;
  private DataProperties overrides;

  private List<AtnParse> _parses;
  private int[] _parsedRange;
  private List<ParseInterpretation> _selectedInterps;

  /**
   * Determine whether all parses have been generated for this result. 
   */
  public boolean isComplete() {
    return states.size() + skipStates.size() == 0 && startRuleIndex == startRules.size();
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

  /** Primary construtor */
  AtnParseResult(AtnGrammar grammar, Token firstToken, int seekStartIndex,
                 AtnParseOptions options, Set<Integer> stopList,
                 DataProperties overrides, AtomicBoolean die) {
    this.grammar = grammar;
    this.firstToken = firstToken;
    this.seekStartIndex = seekStartIndex;
    this.options = options;
    this.stopList = stopList;
    this.die = die;
    this.overrides = overrides;

    this.parse = new Tree<AtnState>(null);
    this.states = new LinkedList<AtnState>();
    this.skipStates = new LinkedList<AtnState>();
    this.startRules = grammar.getStartRules(options);
    this.startRuleIndex = 0;

    this._parses = null;
  }

  /** Manual build constructor */
  AtnParseResult() {
  }

  /** Manually add a parse */
  void addParse(AtnParse atnParse) {
    if (this._parses == null) this._parses = new ArrayList<AtnParse>();
    this._parses.add(atnParse);
  }

  public DataProperties getOverrides() {
    return overrides;
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
   * Get the (maximum) range {start(inc), end(excl)} of parsed text within this
   * instances parses regardless of parse selection.
   *
   * @return the range or null when nothing has been successfuly parsed.
   */
  public int[] getParsedRange() {
    if (_parsedRange == null) {
      int startPos = -1;
      int endPos = -1;

      final List<AtnParse> parses = getParses();
      if (parses != null) {
        for (AtnParse parse : parses) {
          if (parse != null) {
            final int startIndex = parse.getStartIndex();
            final int endIndex = parse.getEndIndex();
            if (startPos < 0 || startIndex < startPos) startPos = startIndex;
            if (endPos < 0 || endIndex > endPos) endPos = endIndex;
          }
        }
      }

      _parsedRange = (startPos >= 0 && endPos >= 0) ? new int[]{startPos, endPos} : null;
    }
    return _parsedRange;
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

  Tree<AtnState> getStateTree() {
    return parse;
  }

  List<AtnParse> getParses() {
    if (_parses == null) {
      _parses = new ArrayList<AtnParse>();

      final List<AtnState> finalStates = getFinalStates();

      int curParseNum = 0;
      for (AtnState finalState : finalStates) {
        final Tree<AtnState> finalStateNode = finalState.getStateNode();
        if (finalStateNode != null) {
          final AtnParse curParse = new AtnParse(curParseNum, finalStateNode, this);
          _parses.add(curParse);
          
          ++curParseNum;
        }
      }
    }
    return _parses;
  }

  public List<ParseInterpretation> getSelectedInterps() {
    if (_selectedInterps == null) {
      _selectedInterps = new ArrayList<ParseInterpretation>();
      for (AtnParse parse : getParses()) {
        if (parse.getSelected()) {
          final List<ParseInterpretation> parseInterps = parse.getParseInterpretations();
          if (parseInterps != null) {
            _selectedInterps.addAll(parseInterps);
          }
        }
      }
    }
    return _selectedInterps;
  }

  /**
   * Find the final states for complete, valid parses.
   */
  private List<AtnState> getFinalStates() {
    final List<AtnState> result = new ArrayList<AtnState>();

    for (Iterator<Tree<AtnState>> iter = parse.iterator(Tree.Traversal.DEPTH_FIRST); iter.hasNext(); ) {
      final Tree<AtnState> curStateNode = iter.next();
      AtnState curState = curStateNode.getData();
      if (curState == null) continue;

      if (curState.isValidEnd(stopList)) {
        final AtnState truncatedEndState = shiftTruncatedEndState(curStateNode);
        if (truncatedEndState != null) {
          curState = truncatedEndState;
        }
        result.add(curState);  // NOTE: we'll remove this later if its pop failed
      }
      else if (curState.popFailed() && result.size() > 0) {
        //final AtnState constituentStartState = AtnStateUtil.getConstituentStartState(curState);
        final int tokenStart = curState.getInputToken().getStartIndex();

        // failed pops are deeper, below potentially valid states. When we find one, we need to discount its associated parent match.
        for (AtnState parentState = curState.getParentState(); parentState != null; parentState = parentState.getParentState()) {
          final int parentTokenStart = parentState.getInputToken().getStartIndex();
          if (parentTokenStart < tokenStart) break;  // only states within constituent are invalidated
          if (result.remove(parentState)) break;     // only nearest parent is invalidated
        }
      }
    }

    return result;
  }

  private final AtnState shiftTruncatedEndState(Tree<AtnState> stateNode) {
    AtnState result = null;

    // if node's token is shorter than a matched parent, shift to the parent
    // this happens when a matched revised token matches, but doesn't succeed
    // to match enough states to encompass the original 

    final int endIdx = stateNode.getData().getInputToken().getEndIndex();
    for (Tree<AtnState> parentNode = stateNode.getParent();
         parentNode != null;
         parentNode = parentNode.getParent()) {
      final AtnState parentState = parentNode.getData();
      if (parentState == null) break;
      if (parentState.getMatched()) {
        if (parentState.getInputToken().getEndIndex() > endIdx) {
          result = parentState;
          break;
        }
      }
    }

    return result;
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

    while ((startRuleIndex < startRules.size() || (states.size() + skipStates.size() > 0)) && (die == null || !die.get())) {
      if (states.size() + skipStates.size() == 0) {
        final AtnRule startRule = startRules.get(startRuleIndex);
        final Token firstToken = getFirstToken(startRule, this.firstToken);
        if (firstToken == null) return false;

        if (startRule.fromFirstTokenOnly() && firstToken.getSequenceNumber() > 0) return false;

        final int numSteps = startRule.getNumSteps();
        if (startRule.isPermuted()) {
          // add all step states, not just first
          for (int stepNum = 0; stepNum < numSteps; ++stepNum) {
            final AtnState firstState = new AtnState(firstToken, startRule, stepNum, parse, options, 0, 0, null);
            firstState.setSeekStartIndex(this.firstToken.getStartIndex());
            states.addLast(firstState);
          }
        }
        else {
          for (AtnState firstState = new AtnState(firstToken, startRule, 0, parse, options, 0, 0, null);
               firstState != null;
               firstState = firstState.getSkipOptionalState()) {
            firstState.setSeekStartIndex(this.seekStartIndex);
            states.addLast(firstState);
          }
        }
        ++startRuleIndex;
      }

      final AtnState state = states.size() > 0 ? states.getFirst() : skipStates.getFirst();
      success = AtnState.matchTokenToRule(grammar, states, skipStates, stopList, die);
      // if (!success) System.out.println(AtnStateUtil.showStateTree(state.parentStateNode))

      if (success && options.getFirstParseOnly()) {
        break;
      }
    }

    if (success) _parses = null;

    return success;
  }

  private Token getFirstToken(AtnRule startRule, Token firstToken) {
    final Token result = grammar.getAcceptedToken(startRule.getTokenFilterId(), firstToken, false, null, true, true, null);
    return result;
  }
}
