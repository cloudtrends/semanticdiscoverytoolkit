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
package org.sd.xml;


/**
 * A strategy to apply to each visited node while traversing the tree of
 * an xml node that identifies which visited nodes are
 * <p>
 * (1) iterable (to be returned with its DomContext) and
 * (2) queuable (that lead to and/or are iterable).
 *
 * @author Spence Koehler
 */
public interface DomIterationStrategy {
  
  /**
   *  Identifies nodes that will be iterated when visited.
   */
  public boolean isIterableNode(DomNode curNode);


  /**
   *  Identifies nodes that should be queued while traversing the DOM
   *  for ultimately finding the Iterable nodes. Note that these
   *  nodes may be Iterable or may potentially lead to Iterable
   *  nodes.
   */
  public boolean shouldQueueNode(DomNode childNode);
}
