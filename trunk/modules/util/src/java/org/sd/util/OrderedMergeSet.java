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


import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Container for an ordered list of elements into which other ordered lists
 * can be merged such that cumulative ordering is preserved.
 * <p>
 * @author Spence Koehler
 */
public class OrderedMergeSet<T> {
  
  private LinkedList<T> startElements;

  // map elements to those that immediately follow: elt -> {nextElt1, nextElt2, ...}
  private Map<T, Set<T>> forwardLinks;
  private Map<T, Set<T>> backLinks;

  private ElementComparator<T> eltComparator = new ElementComparator<T>(this);

  private Set<T> _elements;

  /**
   * Construct an empty instance.
   */
  public OrderedMergeSet() {
    this.startElements = new LinkedList<T>();
    this.forwardLinks = new HashMap<T, Set<T>>();
    this.backLinks = new HashMap<T, Set<T>>();
    this._elements = null;
  }

  /**
   * Construct with the given initial elements.
   */
  public OrderedMergeSet(Collection<T> elements) {
    this.startElements = new LinkedList<T>();
    this.forwardLinks = new HashMap<T, Set<T>>();
    this.backLinks = new HashMap<T, Set<T>>();
    this._elements = null;

    if (elements.size() > 0) startElements.add(elements.iterator().next());

    addLinks(elements);
  }

  public LinkedList<T> getStartElements() {
    return startElements;
  }

  public Set<T> getElements() {
    if (_elements == null) {
      _elements = new TreeSet<T>(eltComparator);

      for (T startElement : startElements) {
        _elements.addAll(getElements(startElement));
      }
    }

    return _elements;
  }

  public Set<T> getElements(T startElement) {
    final Set<T> result = new TreeSet<T>(eltComparator);
    walkLinks(forwardLinks, startElement, null, result, null);
    return result;
  }

  /**
   * Merge the other ordered list's elements into this instance.
   */
  public void merge(OrderedMergeSet<T> other) {
    final LinkedList<T> otherStartElements = other.getStartElements();
    for (T otherStartElement : otherStartElements) {
      merge(other.getElements(otherStartElement));
    }
  }

  /**
   * Merge the elements into this instance.
   */
  public void merge(Collection<T> elementsToMerge) {

    if (elementsToMerge.size() == 0) return;

    _elements = null;

    // Add a new start element if necessary
    final T firstElement = elementsToMerge.iterator().next();
    if (!forwardLinks.containsKey(firstElement)) {
      startElements.add(firstElement);
    }

    // maintain forward and backward links
    addLinks(elementsToMerge);

    // Remove start elements that are not starts anymore
    for (Iterator<T> iter = startElements.iterator(); iter.hasNext(); ) {
      final T startElement = iter.next();
      if (backLinks.containsKey(startElement)) {
        iter.remove();
      }
    }
  }


  private final boolean leadsTo(T element, T nextElement) {
    return walkLinks(forwardLinks, element, nextElement, null, null);
  }

  private final boolean leadsTo(Map<T, Set<T>> links, T element, T otherElement) {
    return walkLinks(links, element, otherElement, null, null);
  }

  private final void addLinks(Collection<T> elements) {
    T lastElement = null;
    for (T element : elements) {
      if (lastElement != null) {
        if (!leadsTo(backLinks, lastElement, element)) {
          addLink(forwardLinks, lastElement, element, backLinks);
        }
        if (!leadsTo(forwardLinks, element, lastElement)) {
          addLink(backLinks, element, lastElement, forwardLinks);
        }
      }
      lastElement = element;
    }
  }

  private final void addLink(Map<T, Set<T>> links, T element, T nextElement, Map<T, Set<T>> reverseLinks) {
    // add mapping from element to nextElement in links
    Set<T> nextElements = links.get(element);
    if (nextElements == null) {
      nextElements = new LinkedHashSet<T>();
      links.put(element, nextElements);
    }
    nextElements.add(nextElement);

    // clean up any direct links that are now indirect
    // by looking back from 'element' to find for removal elements that lead directly to 'nextElement'
    final Set<T> candidates = new HashSet<T>();
    walkLinks(reverseLinks, element, nextElement, null, candidates);
    for (T candidate : candidates) {
      removeDirectLink(links, candidate, nextElement);
    }
  }

  private final void removeDirectLink(Map<T, Set<T>> links, T element, T nextElement) {
    final Set<T> nextElements = links.get(element);
    if (nextElements != null) {
      nextElements.remove(nextElement);
    }
  }

  /**
   * Walk the given links from startElement to endElement (until done if null),
   * storing the path followed in pathResult (if non-null) and all of the
   * elements encountered in linkResult (if non-null).
   *
   * @return true if successfully navigated from startElement to non-null endElement.
   */
  private final boolean walkLinks(Map<T, Set<T>> links, T startElement, T endElement, Set<T> pathResult, Set<T> linkResult) {
    boolean result = false;

    final Set<T> seen = new HashSet<T>();
    final LinkedList<T> queue = new LinkedList<T>();
    queue.add(startElement);

    while (queue.size() > 0) {
      final T curElement = queue.removeFirst();

      // avoid circular paths
      if (seen.contains(curElement)) continue;
      seen.add(curElement);
      
      if (pathResult != null) pathResult.add(curElement);
      if (linkResult != null && !startElement.equals(curElement)) linkResult.add(curElement);

      if (endElement != null && endElement.equals(curElement)) {
        result = true;
        break;
      }

      final Set<T> nextElements = links.get(curElement);

      if (nextElements != null) {
        queue.addAll(0, nextElements);
      }
    }

    return result;
  }


  private static final class ElementComparator<T> implements Comparator<T> {

    private OrderedMergeSet<T> mergeSet;

    public ElementComparator(OrderedMergeSet<T> mergeSet) {
      this.mergeSet = mergeSet;
    }

    public int compare(T o1, T o2) {
      int result = 1;

      if (o1.equals(o2)) {
        result = 0;
      }
      else if (mergeSet.leadsTo(mergeSet.forwardLinks, o1, o2) || mergeSet.leadsTo(mergeSet.backLinks, o2, o1)) {
        result = -1;
      }

      return result;
    }
  }
}
