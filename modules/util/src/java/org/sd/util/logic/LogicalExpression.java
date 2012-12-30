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
package org.sd.util.logic;


import org.sd.util.StringUtil;
import org.sd.util.tree.SimpleTreeBuilder;
import org.sd.util.tree.SimpleTreeBuilderStrategy;
import org.sd.util.tree.Tree;
import org.sd.util.tree.TreeBuilder;
import org.sd.util.tree.TreeBuilderStrategy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Factory for creating an engine for evaluating logical expressions,
 * which are trees of logical statements.
 * <p>
 * @author Spence Koehler
 */
public class LogicalExpression <T> {

  private String expressionString;
  private TruthFunction<T>[] truthFunctions;
  private Tree<LogicalStatement<T>> expression;

  /**
   * Construct a logical expression (tree) from the expression string and its
   * accompanying truth functions.
   * <p>
   * An expressionString is of the form: ([OP] [EXP]) where
   * <p>
   * OP is a LogicalOperator.Type (case-insensitive) {"and", "or", "not"}.
   * <p>
   * and EXP is
   * <ul>
   * <li>another expressionString, or</li>
   * <li> a space-delimited list of expressionStrings, or</li>
   * <li>a number (represented by ascii digits) indicating the index of the
   * accompanying truthFunctions for the statement to occupy a position
   * in the logical expression tree.</li>
   */
  public LogicalExpression(String expressionString, TruthFunction<T>[] truthFunctions) {
    this.expressionString = expressionString;
    this.truthFunctions = truthFunctions;
    this.expression = buildExpression();
  }

  /**
   * Construct with the given expression tree.
   */
  public LogicalExpression(Tree<LogicalStatement<T>> expression) {
    this.expressionString = null;
    this.truthFunctions = null;
    this.expression = expression;
  }

  /**
   * Evaluate the given input through this expression.
   *
   * @return a list of (the first) LogicalResult(s) that evaluate to true over
   *         the input, or null if the expression evaluates to false.
   */
  public List<LogicalResult<T>> evaluate(T input) {
    final Map<TruthFunction, LogicalResult<T>> evalCache = new HashMap<TruthFunction, LogicalResult<T>>();
    return evaluate(input, expression, true, evalCache);
  }

  private final List<LogicalResult<T>> evaluate(T input, Tree<LogicalStatement<T>> statementNode, boolean findTrue, Map<TruthFunction, LogicalResult<T>> evalCache) {
    List<LogicalResult<T>> result = null;

    final LogicalStatement<T> logicalStatement = statementNode.getData();

    final LogicalOperator op = logicalStatement.asLogicalOperator();
    if (op != null) {
      final List<Tree<LogicalStatement<T>>> children = statementNode.getChildren();

      if (children == null || children.size() == 0) {
        // illegal leaf operator node
        throw new IllegalStateException("Illegal leaf operator node! [" + op.getType().name() + "] in expression '" + expression + "'!");
      }

      switch (op.getType()) {
        case OR :
          if (findTrue) {  // or ==> true if one is true
            result = findChild(children, input, true, evalCache);
          }
          else {  // not or ==> true if all are false
            result = ascertainAllChildrenAs(children, input, false, evalCache);
          }
          break;
        case AND :
          if (findTrue) {  // and ==> true if all are true
            result = ascertainAllChildrenAs(children, input, true, evalCache);
          }
          else {  // not and ==> true if one is false
            result = findChild(children, input, false, evalCache);
          }
          break;
        case NOT :
          // illegal operator if there is more than one child!
          if (children.size() > 1) {
            throw new IllegalStateException("'NOT' operator must have exactly 1 child! [has " + children.size() + "] in expression '" + expression + "'!");
          }
          result = evaluate(input, children.get(0), !findTrue, evalCache);
          break;
        case XOR :
          // illegal operator if there is not exactly two children!
          if (children.size() != 2) {
            throw new IllegalStateException("'XOR' operator must have exactly 2 children! [has " + children.size() + "] in expression '" + expression + "'!");
          }
          result = ascertainXor(children, input, findTrue, evalCache);
          break;
      }
    }
    else {

      final TruthFunction<T> truthFunction = logicalStatement.asTruthFunction();

      if (statementNode.hasChildren()) {
        // truthFunction nodes must be terminal!
        throw new IllegalStateException("TruthFunction node must be terminal statement! " + truthFunction);
      }

      LogicalResult<T> logicalResult = null;
      if (evalCache.containsKey(truthFunction)) {
        logicalResult = evalCache.get(truthFunction);  // ok if null.
      }
      else {
        logicalResult = truthFunction.evaluateInput(input);
        evalCache.put(truthFunction, logicalResult);
      }

      if (logicalResult != null) {
        final boolean isTrue = logicalResult.isTrue();
        if ((findTrue && isTrue) || (!findTrue && !isTrue)) {
          result = new ArrayList<LogicalResult<T>>();
          result.add(logicalResult);
        }
      }
    }

    return result != null && result.size() > 0 ? result : null;
  }

  /**
   * Find the first child that is true or false.
   */
  private final List<LogicalResult<T>> findChild(List<Tree<LogicalStatement<T>>> statementNodes, T input, boolean trueOrFalse, Map<TruthFunction, LogicalResult<T>> evalCache) {
    List<LogicalResult<T>> result = null;

    for (Tree<LogicalStatement<T>> statementNode : statementNodes) {
      result = evaluate(input, statementNode, trueOrFalse, evalCache);
      if (result != null) break;
    }

    return result;
  }

  /**
   * Ascertain that all children are true or false.
   */
  private final List<LogicalResult<T>> ascertainAllChildrenAs(List<Tree<LogicalStatement<T>>> statementNodes, T input, boolean trueOrFalse, Map<TruthFunction, LogicalResult<T>> evalCache) {
    List<LogicalResult<T>> result = null;

    for (Tree<LogicalStatement<T>> statementNode : statementNodes) {
      final List<LogicalResult<T>> curResult = evaluate(input, statementNode, trueOrFalse, evalCache);
      if (curResult == null) {
        result = null;
        break;
      }
      else {
        if (result == null) result = new ArrayList<LogicalResult<T>>();
        result.addAll(curResult);
      }
    }

    return result;
  }

  private final List<LogicalResult<T>> ascertainXor(List<Tree<LogicalStatement<T>>> statementNodes, T input, boolean trueOrFalse, Map<TruthFunction, LogicalResult<T>> evalCache) {
    List<LogicalResult<T>> result = null;

    final Tree<LogicalStatement<T>> firstStatement = statementNodes.get(0);
    final Tree<LogicalStatement<T>> secondStatement = statementNodes.get(1);

    final List<LogicalResult<T>> firstAsTrue = evaluate(input, firstStatement, true, evalCache);
    final List<LogicalResult<T>> secondAsTrue = evaluate(input, secondStatement, true, evalCache);
    final List<LogicalResult<T>> firstAsFalse = evaluate(input, firstStatement, false, evalCache);
    final List<LogicalResult<T>> secondAsFalse = evaluate(input, secondStatement, false, evalCache);

    if (trueOrFalse) {
      if (firstAsTrue != null && secondAsFalse != null) {
        result = firstAsTrue;
      }
      else if (firstAsFalse != null && secondAsTrue != null) {
        result = secondAsTrue;
      }
      //else result is null
    }
    else {
      if ((firstAsTrue != null && secondAsTrue != null)) {
        result = new ArrayList<LogicalResult<T>>();
        result.addAll(firstAsTrue);
        result.addAll(secondAsTrue);
      }
      else if ((firstAsFalse !=  null && secondAsFalse != null)) {
        result = new ArrayList<LogicalResult<T>>();
        result.addAll(firstAsFalse);
        result.addAll(secondAsFalse);
      }
      //else result is null
    }

    return result;
  }


  private final Tree<LogicalStatement<T>> buildExpression() {
    final TreeBuilder<LogicalStatement<T>> treeBuilder = new SimpleTreeBuilder<LogicalStatement<T>>(new StatementTreeBuilderStrategy());
    return treeBuilder.buildTree(expressionString);
  }


  private final class StatementTreeBuilderStrategy extends SimpleTreeBuilderStrategy <LogicalStatement<T>> {

    StatementTreeBuilderStrategy() {
    }

    public LogicalStatement<T> constructCoreNodeData(String coreNodeString) {
      LogicalStatement<T> result = null;

      if (StringUtil.isDigits(coreNodeString)) {
        final int index = Integer.parseInt(coreNodeString);
        result = truthFunctions[index];
      }
      else {
        final LogicalOperator.Type type = Enum.valueOf(LogicalOperator.Type.class, coreNodeString.toUpperCase());
        result = new LogicalOperator<T>(type);
      }

      return result;
    }
  }
}
