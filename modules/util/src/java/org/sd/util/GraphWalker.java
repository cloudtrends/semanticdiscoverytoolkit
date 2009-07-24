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
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

/**
 * Utility to walk graphs of objects.
 * <p>
 * @author Spence Koehler
 */
public class GraphWalker <T> {
  
  public enum Order {BREADTH_FIRST, DEPTH_FIRST};

  private GraphWalkerStrategy<T> strategy;
  private Order order;
  private Set<T> expanded;

  public GraphWalker(GraphWalkerStrategy<T> strategy, Order order) {
    this.strategy = strategy;
    this.order = order;
    this.expanded = new HashSet<T>();
  }

  public GraphWalkerStrategy<T> getStrategy() {
    return strategy;
  }

  public void walk(T startNode) {
    final LinkedList<T> queue = new LinkedList<T>();
    queue.addFirst(startNode);

    while (queue.size() > 0) {
      final T graphNode = queue.removeFirst();

      if (!expanded.contains(graphNode)) {
        expanded.add(graphNode);

        if (strategy.visit(graphNode)) {
          final Collection<T> nextNodes = strategy.expand(graphNode);
          if (nextNodes != null) {
            for (T nextNode : nextNodes) {
              if (expanded.contains(nextNode)) continue;

              //todo: if queue.contains(nextNode) should we change its position in the queue?
              if (queue.contains(nextNode)) continue;

              switch (order) {
                case BREADTH_FIRST :
                  queue.addLast(nextNode);
                  break;
                case DEPTH_FIRST :
                  queue.addFirst(nextNode);
                  break;
              }
            }
          }
        }
        else {
          break;
        }
      }
    }
  }
}
