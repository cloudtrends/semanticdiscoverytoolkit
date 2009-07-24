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
package org.sd.util;


import java.util.Collection;

/**
 * Interface for a graph walker strategy.
 * <p>
 * @author Spence Koehler
 */
public interface GraphWalkerStrategy <T> {
  
  /**
   * Visit the given graph node (while walking).
   *
   * @return true to continue walking, false to halt.
   */
  public boolean visit(T graphNode);

  /**
   * Expand the graph node to its successors.
   *
   * @return the successors to the graph node, possibly null or empty.
   */
  public Collection<T> expand(T graphNode);

}
