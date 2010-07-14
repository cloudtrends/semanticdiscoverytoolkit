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


import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.sd.util.InputContext;
import org.sd.util.InputContextIterator;

/**
 * Basic iterator over specific DomContext instances.
 * <p>
 * @author Spence Koehler
 */
public class DomContextIterator implements InputContextIterator {
  
  private List<DomContext> domContexts;
  private Iterator<DomContext> iter;

  public DomContextIterator() {
    this.domContexts = new ArrayList<DomContext>();
  }

  public DomContextIterator(List<DomContext> domContexts) {
    this.domContexts = domContexts;
  }

  /**
   * Add a domContext to those being iterated over. Note that this will
   * reset the iterator if it has been started.
   */
  public void add(DomContext domContext) {
    this.domContexts.add(domContext);
    reset();
  }

  /**
   * Add a domContext to those being iterated over. Note that this will
   * reset the iterator if it has been started.
   */
  public void add(DomNode domNode) {
    add(domNode.getDomContext());
  }

  public InputContext next() {
    if (iter == null) iter = domContexts.iterator();
    return iter.next();
  }

  public boolean hasNext() {
    if (iter == null) iter = domContexts.iterator();
    return iter.hasNext();
  }

  public void remove() {
    if (iter != null) iter.remove();
  }

  /**
   * Reset this iterator for re-iterating over its elements.
   */
  public void reset() {
    this.iter = null;
  }

  /**
   * Return a new iterator over the same content, but with broader elements
   * that are supersets of elements from this iterator. If broadening is not
   * possible, then return null.
   */
  public InputContextIterator broaden() {
    DomContextIterator result = null;

    for (DomContext domContext : domContexts) {
      final DomNode curDomNode = domContext.getDomNode();
      final String curText = curDomNode.getTextContent();

      DomNode parentNode = curDomNode.getParent();
      while (parentNode != null) {
        final String parentText = parentNode.getTextContent();
        if (!parentText.equals(curText)) {
          break;
        }
        parentNode = parentNode.getParent();
      }

      if (parentNode != null) {
        boolean alreadyThere = false;
        if (result != null) {
          for (DomContext newDomContext : result.domContexts) {
            if (newDomContext.getDomNode().isAncestor(parentNode, true)) {
              alreadyThere = true;
              break;
            }
          }
        }

        if (!alreadyThere) {
          if (result == null) result = new DomContextIterator();
          result.add(parentNode);
        }
      }
    }

    return result;
  }
}
