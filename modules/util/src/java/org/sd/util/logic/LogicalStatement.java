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


/**
 * Container for a logical operator or a truth function.
 * <p>
 * A LogicalStatement is intended to be the data portion of a node in a tree
 * representing a LogicalConstruct.
 * <p>
 * A LogicalOperator statement defines the operator to apply to its child nodes
 * (and are, therefore, the data for non-leaf nodes) while a TruthFunction
 * statement defines an operation over the input to evaluate the "truth" of an
 * expression (and are the data for leaf nodes).
 *
 * @author Spence Koehler
 */
public abstract class LogicalStatement <T> {

  /**
   * Safely downcast this LogicalStatement as a LogicalOperator if it is one.
   */
  public LogicalOperator<T> asLogicalOperator() {
    return null;
  }
  
  /**
   * Safely downcast this LogicalStatement as a TruthFunction if it is one.
   */
  public TruthFunction<T> asTruthFunction() {
    return null;
  }
}
