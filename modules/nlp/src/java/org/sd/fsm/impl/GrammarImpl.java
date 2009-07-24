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
package org.sd.fsm.impl;


import org.sd.fsm.Grammar;
import org.sd.fsm.State;
import org.sd.fsm.StateDecoder;
import org.sd.io.FileUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

/**
 * A grammar expressed as a pattern of tokens.
 * <p>
 * A grammar is defined by rules where the left hand side (lhs) part defines
 * a token that can be substituted for the right hand side (rhs) sequence of
 * tokens. Rules appear as:<br>
 * lhs &lt;- rhs<br>
 * <p>
 * Special rhs tokens are:<br>
 *    x + == the token, x, can be repeated one or more times to match the rule.<br>
 *    x * == the token, x, can be repeated zero or more times to match the rule,<br>
 *    x ? == the token, x, can be optional to match the rule.<br>
 *      . == the token at this position can be considered a valid end.<br>
 * <p>
 *
 * @author Spence Koehler
 */
public class GrammarImpl implements Grammar {
  
  private Map<String, List<RulePos>> token2rules;
  private List<RulePos> guessableRules;
  private StateDecoder stateDecoder;

  public GrammarImpl(List<RuleImpl> rules) {
    this(rules, null);
  }

  public GrammarImpl(List<RuleImpl> rules, StateDecoder stateDecoder) {
    initToken2Rules(rules);
    this.stateDecoder = (stateDecoder == null) ? DefaultStateDecoder.getInstance() : stateDecoder;
  }

  public StateDecoder getStateDecoder() {
    return stateDecoder;
  }

  private final void initToken2Rules(List<RuleImpl> rules) {
    this.token2rules = new LinkedHashMap<String, List<RulePos>>();
    this.guessableRules = null;

    for (RuleImpl rule : rules) {
      int position = 0;
      while (position >= 0) {
        final AbstractToken token = rule.getRHS(position);
        mapKeys(token, rule, position);
        if (rule.isOptional(position)) {
          position = rule.getNextTokenPosition(position);
        }
        else break;
      }
    }
  }

  private final void mapKeys(AbstractToken token, RuleImpl rule, int position) {
    final String[] tokenKeys = token.getCommonKeys();
    for (String key : tokenKeys) {
      List<RulePos> curRules = token2rules.get(key);
      if (curRules == null) {
        curRules = new ArrayList<RulePos>();
        token2rules.put(key, curRules);
      }
      curRules.add(new RulePos(position, key, rule));
    }

    if (token.isGuessable()) {
      if (guessableRules == null) guessableRules = new ArrayList<RulePos>();
      guessableRules.add(new RulePos(position, null, rule));
    }
  }

  /**
   * Create an instance of an initial state for this grammar.
   */
  public State getFirstState() {
    return new StateImpl(this);
  }

  /**
   * Find all rules whose first RHS token matches the given token.
   *
   * @return a list of applicable rules or null if no rules apply.
   */
  List<RulePos> findRules(AbstractToken token) {
    List<RulePos> result = null;
    final String[] keys = token.getCommonKeys();
    if (keys != null) {
      for (String key : keys) {
        final List<RulePos> curRules = token2rules.get(key);
        if (curRules != null) {
          if (result == null) result = new ArrayList<RulePos>();
          result.addAll(curRules);
        }
      }
    }
    else {
      result = guessableRules;
    }
    return result;
  }

  /**
   * Find rules that lead from fromToken (lhs) to toToken, according to the
   * first rhs token.
   * <p>
   * For example, rules of form a <- b, b <- c, c <- d are rules from 'a' to 'd'.
   *
   * @param fromToken  the rule to start from (unlimited if null).
   * @param toToken    the rule to stop at (required to be non-null).
   * 
   * @return null or a list of rule paths, where a rule path is a list (path)
   *         of rules from fromToken to toToken.
   */
  List<RulePath> findRules(GrammarToken fromToken, AbstractToken toToken) {
    List<RulePath> result = null;

    final List<RulePos> applicableRules = findRules(toToken);
    if (applicableRules != null) {
      for (RulePos rulePos : applicableRules) {
        final List<RulePath> paths = followPaths(fromToken, new RulePath(rulePos));
        if (paths != null) {
          if (result == null) result = new ArrayList<RulePath>();
          result.addAll(paths);
        }
      }
    }

    return result;
  }

  private List<RulePath> followPaths(GrammarToken stopAt, RulePath rulePath) {
    List<RulePath> result = null;
    final GrammarToken lhs = rulePath.getLHS();
    if (lhs.equals(stopAt)) {
      result = new ArrayList<RulePath>();
      result.add(rulePath);
    }
    else {
      final List<RulePos> applicableRules = findRules(lhs);
      if (applicableRules != null) {
        final List<RulePath> rulePaths = rulePath.getRulePaths(applicableRules);
        for (RulePath curRulePath : rulePaths) {
          final List<RulePath> curRulePaths = followPaths(stopAt, curRulePath);
          if (curRulePaths != null) {
            if (result == null) result = new ArrayList<RulePath>();
            result.addAll(curRulePaths);
          }
        }
      }
      else {
        if (stopAt == null) {
          result = new ArrayList<RulePath>();
          result.add(rulePath);
          return result;
        }
        // else, didn't hit target, return null.
      }
    }
    return result;
  }

  static class RulePos {
    final int pos;
    final String key;
    final RuleImpl rule;

    RulePos(int pos, String key, RuleImpl rule) {
      this.pos = pos;
      this.key = key;
      this.rule = rule;
    }

    public String toString() {
      return "RulePos[pos=" + pos + ",key=" + key + ",rule=" + rule + "]";
    }
  }

  static class RulePath {
    private LinkedList<RulePos> path;

    RulePath(RulePos finalRule) {
      this.path = new LinkedList<RulePos>();
      this.path.addFirst(finalRule);
    }

    GrammarToken getLHS() {
      return path.getFirst().rule.getLHS();
    }

    Iterator<RulePos> iterator() {
      return path.iterator();
    }

    int size() {
      return path.size();
    }

    ListIterator<RulePos> iterator(int start) {
      return path.listIterator(start);
    }

    private RulePath(RulePath other, RulePos headRule) {
      this.path = new LinkedList<RulePos>(other.path);
      this.path.addFirst(headRule);
    }

    List<RulePath> getRulePaths(List<RulePos> headRules) {
      List<RulePath> result = null;
      if (headRules == null) return result;
      if (headRules.size() == 1) {
        path.addAll(0, headRules);
        result = new ArrayList<RulePath>();
        result.add(this);
        return result;
      }
      result = new ArrayList<RulePath>();
      for (RulePos rule : headRules) {
        result.add(new RulePath(this, rule));
      }
      return result;
    }

    public String toString() {
      return path.toString();
    }
  }

  public static GrammarImpl loadGrammar(String filename, GrammarTokenFactory gtFactory, StateDecoder stateDecoder) throws IOException {
    return loadGrammar(FileUtil.getInputStream(filename), gtFactory, stateDecoder);
  }

  public static GrammarImpl loadGrammar(InputStream inStream, GrammarTokenFactory gtFactory, StateDecoder stateDecoder) throws IOException {
    final List<RuleImpl> rules = new ArrayList<RuleImpl>();
    final BufferedReader reader = FileUtil.getReader(inStream);
    String line = null;
    while ((line = reader.readLine()) != null) {
      final RuleImpl rule = parseRule(line.trim(), gtFactory);
      if (rule != null) {
        rules.add(rule);
      }
    }
    return new GrammarImpl(rules, stateDecoder);
  }

  public static GrammarImpl loadGrammar(String[] ruleLines, GrammarTokenFactory gtFactory, StateDecoder stateDecoder) {
    final List<RuleImpl> rules = new ArrayList<RuleImpl>();
    for (String line : ruleLines) {
      final RuleImpl rule = parseRule(line.trim(), gtFactory);
      if (rule != null) {
        rules.add(rule);
      }
    }
    return new GrammarImpl(rules, stateDecoder);
  }

  static RuleImpl parseRule(String line, GrammarTokenFactory gtFactory) {
    if (line.startsWith("#") || line.trim().length() == 0) return null;

    final String[] sides = line.split("<-");
    if (sides.length == 2) {
    }
    else {
      throw new IllegalArgumentException("non-comment line: '" + line + "' doesn't have form 'LHS <- RHS'!");
    }

    final String lhs = sides[0].trim();
    if (lhs.length() == 0) throw new IllegalArgumentException("empty LHS in line: '" + line + "'!");

    final String[] rhsTokens = sides[1].split(" +");

    final List<GrammarToken> rhs = new ArrayList<GrammarToken>();
    for (String rhsToken : rhsTokens) {
      if (rhsToken.length() > 0) {
        rhs.add(gtFactory.getGrammarToken(rhsToken));
      }
    }

    if (rhs.size() == 0) throw new IllegalArgumentException("empty RHS in line: '" + line + "'!");

    return new RuleImpl(gtFactory.getGrammarToken(lhs), rhs.toArray(new GrammarToken[rhs.size()]));
  }
}
