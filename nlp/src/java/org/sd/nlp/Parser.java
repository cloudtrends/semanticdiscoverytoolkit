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
package org.sd.nlp;


import org.sd.fsm.FSM;
import org.sd.fsm.Grammar;
import org.sd.fsm.impl.GrammarToken;
import org.sd.fsm.Rule;
import org.sd.fsm.State;
import org.sd.fsm.Token;
import org.sd.fsm.impl.FSMImpl;
import org.sd.fsm.impl.RuleImpl;
import org.sd.fsm.impl.StateUtil;
import org.sd.util.tree.TraversalIterator;
import org.sd.util.tree.Tree;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * An NLP Parser built on top of an FSM.
 *
 * @author Spence Koehler
 */
public class Parser {

  private FSM fsm;
  private Lexicon lexicon;
  private boolean ignoreExtraInput;
  private int skipUpTo;

  public Parser(Grammar grammar, Lexicon lexicon) {
    this(grammar, lexicon, false, 0);
  }

  public Parser(Grammar grammar, Lexicon lexicon, boolean ignoreExtraInput, int skipUpTo) {
    this.fsm = new FSMImpl(grammar);
    this.lexicon = lexicon;
    this.ignoreExtraInput = ignoreExtraInput;
    this.skipUpTo = skipUpTo;
  }

  public Lexicon getLexicon() {
    return lexicon;
  }

  public List<Parse> parse(String sentence) {
    return parse(new DelimitedStringLexicalTokenizer(sentence, lexicon, skipUpTo), null, false);
  }

  /**
   * Utility method to get the substrings for each parse leaf.
   */
  public static final List<StringWrapper.SubString> getSubStrings(Parse parse) {
    final List<StringWrapper.SubString> result = new ArrayList<StringWrapper.SubString>();
    final List<Tree<Token>> leaves = parse.getLeaves();
    for (Tree<Token> leaf : leaves) {
      result.add(getSubString(leaf));
    }
    return result;
  }

  /**
   * Utility method to extract substrings from the parse for parse nodes that
   * have any of the given node (category) names.
   * <p>
   * Note that all text under a node will be concatenated together and
   * returned as a single substring.
   * <p>
   * Each substring will have a 'category' attribute that identifies
   * which nodeName was matched while extracting the substring from
   * the parse tree.
   */
  public static final List<StringWrapper.SubString> getSubStrings(Parse parse, Set<String> nodeNames) {
    List<StringWrapper.SubString> result = null;

    final Tree<Token> parseTree = parse.getTree();
    for ( TraversalIterator<Token> iter = parseTree.iterator(Tree.Traversal.BREADTH_FIRST); iter.hasNext(); ) {
      final Tree<Token> parseNode = iter.next();
      final String nodeName = ((GrammarToken)parseNode.getData()).getToken();
      if (nodeNames.contains(nodeName)) {
        final List<Tree<Token>> leaves = parseNode.gatherLeaves();
        final Tree<Token> firstLeaf = leaves.get(0);
        final Tree<Token> lastLeaf = leaves.get(leaves.size() - 1);

        final StringWrapper.SubString firstSubString = Parser.getSubString(firstLeaf);
        final StringWrapper.SubString lastSubString = (firstLeaf == lastLeaf) ? firstSubString : Parser.getSubString(lastLeaf);

        final StringWrapper.SubString subString = firstSubString.stringWrapper.getSubString(firstSubString.startPos, lastSubString.endPos);
        subString.addAttribute("category", nodeName);
        if (result == null) result = new ArrayList<StringWrapper.SubString>();
        result.add(subString);
      }
    }

    return result;
  }

  /**
   * Utility method to get the substring within the parse leaf's token.
   */
  public static final StringWrapper.SubString getSubString(Tree<Token> leaf) {
    final LexicalToken lexicalToken = (LexicalToken)leaf.getData();
    final LexicalEntry lexicalEntry = lexicalToken.getLexicalEntry();
    final TokenPointer pointer = lexicalEntry.getPointer();

    //todo: here we're in danger of a class cast exception. fix this.
    final StringWrapperTokenPointer swtp = (StringWrapperTokenPointer)pointer;
    return swtp.getSubString();
  }


  public List<Parse> parse(LexicalTokenizer tokenizer, Category[] acceptCategories, boolean allowSkipAfterFirst) {

    LexicalEntry firstEntry = tokenizer.getFirstEntry();

    while (firstEntry != null) {

      final LexicalToken firstToken = new LexicalToken(firstEntry);
      final List<State> parses = executeMachine(firstToken);

      if (parses != null) {
        return buildParses(parses, tokenizer, acceptCategories);
      }
      else {
        // we had a false start. skip ahead and try again if could/should.
        if (skipUpTo - firstEntry.getPosition() > 0) {
          firstEntry = firstEntry.next(allowSkipAfterFirst);
        }
        else break;
      }
    }

    return null;
  }

  /**
   * Build acceptable 'Parse' instances from the non-null FSM parse states.
   */
  private final List<Parse> buildParses(List<State> parses, LexicalTokenizer tokenizer, Category[] acceptCategories) {
    List<Parse> result = null;
    for (State state : parses) {
      boolean addParse = (acceptCategories == null);

      if (!addParse) {
        final State top = StateUtil.getTop(state);
        final ExtendedGrammarToken token = (ExtendedGrammarToken)(((RuleImpl)top.getRule()).getLHS());
        final Category treeDef = token.getCategory();
        for (Category def : acceptCategories) {
          if (def == treeDef) {
            addParse = true;
            break;
          }
        }
      }

      if (addParse) {
        if (result == null) result = new ArrayList<Parse>();
        else {
          // check for subsumption; remove subsumed
          for (Iterator<Parse> iter = result.iterator(); iter.hasNext(); ) {
            final Parse prevParse = iter.next();
            if (StateUtil.subsumes(state, prevParse.getFinalState())) iter.remove();
          }
        }
        result.add(new Parse(tokenizer.getInputString(), state));
      }
    }
    return result;
  }

  private List<State> executeMachine(LexicalToken lexicalToken) {
    if (lexicalToken == null) return null;

    final Transition startTransition = new Transition(lexicalToken);
    final List<Transition> potentialFinalTransitions = new ArrayList<Transition>();
    List<Transition> finalTransitions = new ArrayList<Transition>();
    TransitionContainer transitions = new TransitionHelper(startTransition).getTransitions();


    while (transitions != null) {
      finalTransitions.addAll(transitions.finished);
      final TransitionContainer nextTransitions = getNextTransitions(transitions);

      // find any completed transitions. guaranteeing we get only the most complete parses.
      if (ignoreExtraInput) {
        for (Transition curTransition : transitions.active) {
          if (curTransition.isAtEnd()) {
            addTransition(potentialFinalTransitions, curTransition);
          }
        }
      }

      // add active transitions if ignoreExtraInput when we fail to progress
      if (nextTransitions == null && ignoreExtraInput && potentialFinalTransitions.size() > 0) {
        for (Transition finalTransition : finalTransitions) {
          if (finalTransition.isAtEnd()) {
            addTransition(potentialFinalTransitions, finalTransition);
          }
        }
        finalTransitions = potentialFinalTransitions;
      }

      transitions = nextTransitions;
    }

    return getFinalStates(finalTransitions);
  }

  /**
   * Add the given transition to the list, removing from the list any transitions
   * that precede the given transition. This is to guarantee we preserve only the
   * most complete parses in the list.
   */
  private final void addTransition(List<Transition> transitions, Transition transition) {
    for (Iterator<Transition> iter = transitions.iterator(); iter.hasNext(); ) {
      final Transition prevTransition = iter.next();
//      if (transition.follows(prevTransition)) {
      if (StateUtil.subsumes(transition.nextState, prevTransition.nextState)) {
        iter.remove();  // remove less complete parse
      }
    }
    transitions.add(transition);
  }

  private TransitionContainer getNextTransitions(TransitionContainer transitions) {
    TransitionContainer result = null;
    for (Transition transition : transitions.active) {
      final TransitionContainer nextTransitions = new TransitionHelper(transition).getTransitions();
      if (nextTransitions != null) {
        if (result == null) result = new TransitionContainer();
        result.incorporate(nextTransitions);
      }
    }
    return result;
  }

  private List<State> getFinalStates(List<Transition> transitions) {
    List<State> result = null;

    for (Transition transition : transitions) {
      final State nextState = transition.getNextState();
      if (nextState != null && nextState.isTerminal()) {
        if (result == null) result = new ArrayList<State>();
        result.add(nextState);
      }
    }

    return result;
  }

  public final class Parse {

    private String input;
    private State finalState;
    private Tree<Token> tree;
    private LexicalToken finalToken;

    private List<Tree<Token>> _leaves;
    private StringWrapper.SubString _parsedInput;
    private String _parseKey;

    Parse(String input, State state) {
      this.input = input;
      this.finalState = state;
      this.tree = state.buildTree(ParseStateDecoder.getInstance());
      this.finalToken = (LexicalToken)state.getInputToken();
      this._leaves = null;
      this._parsedInput = null;
      this._parseKey = null;
    }

    public String getInput() {
      return input;
    }

    public State getFinalState() {
      return finalState;
    }

    public Tree<Token> getTree() {
      return tree;
    }
    
    public List<Tree<Token>> getLeaves() {
      if (_leaves == null) {
        _leaves = tree.gatherLeaves();
      }
      return _leaves;
    }

    /**
     * Get the portion of the input that was parsed.
     */
    public StringWrapper.SubString getParsedInput() {
      if (_parsedInput == null) {
        final List<Tree<Token>> leaves = getLeaves();
        final Tree<Token> firstLeaf = leaves.get(0);
        final Tree<Token> lastLeaf = leaves.get(leaves.size() - 1);

        final StringWrapper.SubString firstSubString = getSubString(firstLeaf);
        final StringWrapper.SubString lastSubString = (firstLeaf == lastLeaf) ? firstSubString : getSubString(lastLeaf);

        _parsedInput = firstSubString.stringWrapper.getSubString(firstSubString.startPos, lastSubString.endPos);
      }
      return _parsedInput;
    }

    /**
     * Get a StringWrapper.SubString from the original input's StringWrapper
     * encompassing the unparsed input prior to this parse.
     *
     * @return the subString or null if there is no prior input.
     */
    public StringWrapper.SubString getUnparsedPriorInput() {
      final StringWrapper.SubString inputSubString = getParsedInput();
      final StringWrapper stringWrapper = inputSubString.stringWrapper;
      return stringWrapper.getSubString(0, stringWrapper.getPrevEndIndex(inputSubString.startPos));
    }

    /**
     * Get a StringWrapper.SubString from the original input's StringWrapper
     * encompassing the unparsed input after this parse.
     *
     * @return the subString or null if there is no following input.
     */
    public StringWrapper.SubString getUnparsedPostInput() {
      final StringWrapper.SubString inputSubString = getParsedInput();
      final StringWrapper stringWrapper = inputSubString.stringWrapper;
      return stringWrapper.getSubString(stringWrapper.getNextStartIndex(inputSubString.endPos));
    }

    /**
     * Get the parse key for this parse.
     * <p>
     * The parse key represents the structure of a parse without regard to the input.
     * <p>
     * Parses with different content (input) will have the same parse key iff they
     * have the same parse structure.
     */
    public String getParseKey() {
      if (_parseKey == null) {
        _parseKey = computeParseKey();
      }
      return _parseKey;
    }

    private final String computeParseKey() {
      final StringBuilder result = new StringBuilder();

      for (Iterator<Tree<Token>> iter = tree.iterator(Tree.Traversal.DEPTH_FIRST); iter.hasNext(); ) {
        final Tree<Token> parseNode = iter.next();
        if (parseNode.getChildren() != null) {  // as long as not a leaf
          final String tokenString = parseNode.getData().toString();
          if (result.length() > 0) result.append(' ');
          result.append(tokenString);
        }
      }

      return result.toString();
    }

    public String getParsedInput(String unnormalizedInput) {
      return finalToken.getLexicalEntry().getPointer().getInputThrough(unnormalizedInput);
    }

    public String getUnparsedInput(String unnormalizedInput) {
      return finalToken.getLexicalEntry().getPointer().getInputBeyond(unnormalizedInput);
    }

    public String toString() {
      return tree.toString();
    }
  }

  final class TransitionHelper {
    private Transition transition;
    private State startState;
    private LexicalToken inputToken;

    TransitionHelper(Transition transition) {
      this.transition = transition;
      this.startState = transition.getNextState();
      this.inputToken = transition.getNextToken();
    }

    TransitionContainer getTransitions() {
      if (inputToken == null) return null;
      List<Transition> result = null;

      final List<State> nextStates = getNextStates();
      if (nextStates != null) {
        result = new ArrayList<Transition>();
        for (State nextState : nextStates) {
          result.add(new Transition(nextState, inputToken, transition));
        }
      }
      return result == null ? null : new TransitionContainer(result);
    }

    private List<State> getNextStates() {
      List<State> result = (startState == null) ? fsm.accept(inputToken) : fsm.accept(inputToken, startState);

      if (result == null && startState != null) {
        final LexicalToken nextToken = inputToken.revise();
        if (nextToken != null) {
          // try shorter token
          inputToken = nextToken;
          result = getNextStates();
        }
      }

      return result;
    }
  }

  final class Transition {
    private State nextState;
    private LexicalToken nextToken;
    private Transition prevTransition;

    Transition(LexicalToken inputToken) {
      this.nextState = null;
      this.nextToken = inputToken;
      this.prevTransition = null;
    }

    Transition(State nextState, LexicalToken inputToken, Transition prevTransition) {
      this.nextState = nextState;
      this.nextToken = computeNextToken(inputToken);
      this.prevTransition = prevTransition;
    }

    State getNextState() {
      return nextState;
    }

    LexicalToken getNextToken() {
      return nextToken;
    }

    boolean isActive() {
      return nextToken != null;
    }

    boolean isAtEnd() {
      return nextState.isTerminal();
    }

    boolean follows(Transition priorTransition) {
      final Rule rule = nextState.getRule();
      for (Transition curTransition = this.prevTransition; curTransition != null; curTransition = curTransition.prevTransition) {
        if (curTransition == priorTransition) return true;
        if (rule == priorTransition.nextState.getRule()) return true;
      }
      return false;
    }

    private final LexicalToken computeNextToken(LexicalToken inputToken) {
      LexicalToken result = inputToken;

      // increment token (unless grammar rule is peek in nextState)
      final ExtendedGrammarToken grammarToken = (ExtendedGrammarToken)nextState.getMatchedGrammarToken();
      if (!grammarToken.isPeek()) {
        result = inputToken.next();
      }

      return result;
    }

    public String toString() {
      final StringBuilder result = new StringBuilder();

      final String nspt = nextState.buildTree(ParseStateDecoder.getInstance()).toString();
      result.append("Transition[nextState=").append(nspt).append(',').
        append("nextToken=").append(nextToken).append(']');

      return result.toString();
    }
  }

  final class TransitionContainer {
    public final List<Transition> finished;
    public final List<Transition> active;

    TransitionContainer() {
      this.finished = new ArrayList<Transition>();
      this.active = new ArrayList<Transition>();
    }

    TransitionContainer(List<Transition> transitions) {
      this();

      for (Transition transition : transitions) {
        if (transition.isActive()) {
          active.add(transition);
        }
        else {
          finished.add(transition);
        }
      }
    }

    void incorporate(TransitionContainer other) {
      this.finished.addAll(other.finished);
      this.active.addAll(other.active);
    }
  }
}
