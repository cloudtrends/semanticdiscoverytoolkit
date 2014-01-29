/*
    Copyright 2013 Semantic Discovery, Inc. (www.semanticdiscovery.com)

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
import java.util.List;

/**
 * A simple doubly linked list implementation.
 * <p>
 * @author Spence Koehler
 */
public class DoublyLinkedList <T> {
  
  private LinkNode<T> firstLinkNode;
  private LinkNode<T> lastLinkNode;
  private int size;

  /** Construct an empty list. */
  public DoublyLinkedList() {
    this.firstLinkNode = null;
    this.lastLinkNode = null;
    this.size = 0;
  }

  /** Construct with the given elements. */
  public DoublyLinkedList(Collection<? extends T> c) {
    this();
    if (c != null) {
      for (T elt : c) {
        this.add(elt);
      }
    }
  }

  /** Get the size of this list. */
  public int size() {
    return size;
  }

  /** Add the element to the end. */
  public final LinkNode<T> add(T elt) {
    return buildLinkNode(elt, lastLinkNode, null);
  }

  /** Insert the element at the given index. */
  public final LinkNode<T> add(int idx, T elt) {
    LinkNode<T> result = null;

    if (idx == size) {
      result = add(elt);
    }
    else {
      final LinkNode<T> linkNode = getLinkNode(idx);
      if (linkNode != null) {
        result = linkNode.push(elt);
      }
    }

    return result;
  }

  /** Remove the node at the given index, returning its elt. */
  public T remove(int idx) {
    final LinkNode<T> linkNode = getLinkNode(idx);
    return linkNode == null ? null : linkNode.remove();
  }

  /** Get the element at the given index. */
  public T get(int idx) {
    final LinkNode<T> result = getLinkNode(idx);
    return (result == null) ? null : result.elt;
  }

  /** Get the link node at the given index. */
  public LinkNode<T> getLinkNode(int idx) {
    LinkNode<T> result = null;

    if (idx >= 0 && idx < size) {
      if (idx <= (size >> 1)) {
        result = firstLinkNode;
        for (int i = 0; i < idx; ++i) {
          result = result.next;
        }
      }
      else {
        result = lastLinkNode;
        for (int i = idx + 1; i < size; ++i) {
          result = result.prev;
        }
      }
    }

    return result;
  }

  /** Get the first linkNode containing the given element, or null. */
  public LinkNode<T> find(T elt) {
    LinkNode<T> result = null;

    if (firstLinkNode != null) {
      result = firstLinkNode.find(elt);
    }

    return result;
  }

  /** Get all elements in a List. */
  public List<T> asList() {
    final List<T> result = new ArrayList<T>();

    for (LinkNode<T> linkNode = firstLinkNode; linkNode != null; linkNode = linkNode.next) {
      result.add(linkNode.elt);
    }
    return result;
  }

  /** Get the first link node. */
  public LinkNode<T> getFirst() {
    return firstLinkNode;
  }

  /** Get the last link node. */
  public LinkNode<T> getLast() {
    return lastLinkNode;
  }

  private final LinkNode<T> buildLinkNode(T elt, LinkNode<T> prev, LinkNode<T> next) {
    final LinkNode<T> linkNode = new LinkNode<T>(this, elt, prev, next);
    ++size;
    doUpdates(linkNode);
    return linkNode;
  }

  private final void doUpdates(LinkNode<T> linkNode) {
    int curIdx = -1;

    if (linkNode.prev == null) {
      firstLinkNode = linkNode;
    }
    else {
      curIdx = linkNode.prev.index;
    }

    if (linkNode.next == null) lastLinkNode = linkNode;

    for (LinkNode<T> node = linkNode; node != null; node = node.next) {
      node.setIndex(++curIdx);
    }
  }

  /** do updates after removing the link between prev and next. */
  private final void doUpdates(LinkNode<T> thePrev, LinkNode<T> theNext) {
    --size;

    if (theNext != null) {
      doUpdates(theNext);
    }
    else if (thePrev != null) {
      doUpdates(thePrev);
    }
    else {
      // we're empty
      firstLinkNode = lastLinkNode = null;
    }
  }


  public static final class LinkNode <T> {
    private DoublyLinkedList<T> owner;
    private T elt;
    private LinkNode<T> prev;
    private LinkNode<T> next;
    private int index;

    private LinkNode(DoublyLinkedList<T> owner, T elt, LinkNode<T> prev, LinkNode<T> next) {
      this.owner = owner;
      this.elt = elt;
      this.prev = prev;
      this.next = next;
      this.index = -1;

      if (prev != null) {
        prev.next = this;
      }
      if (next != null) {
        next.prev = this;
      }
    }

    public T getElt() {
      return elt;
    }

    public boolean hasPrev() {
      return prev != null;
    }

    public LinkNode<T> getPrev() {
      return prev;
    }

    public boolean hasNext() {
      return next != null;
    }

    public LinkNode<T> getNext() {
      return next;
    }

    public int getIndex() {
      return index;
    }

    /** Remove this link, returning its elt. */
    public T remove() {
      final LinkNode<T> theNext = this.next;
      final LinkNode<T> thePrev = this.prev;

      if (theNext != null) {
        theNext.prev = thePrev;
        this.next = null;
      }

      if (thePrev != null) {
        thePrev.next = theNext;
        this.prev = null;
      }

      owner.doUpdates(thePrev, theNext);

      return elt;
    }

    /** Insert the elt into the list immediately after this elt. */
    public LinkNode<T> add(T elt) {
      return owner.buildLinkNode(elt, this, this.next);
    }

    /** Insert the elt into the list immediately before this elt. */
    public LinkNode<T> push(T elt) {
      return owner.buildLinkNode(elt, this.prev, this);
    }

    /** From (including) this node, find the first node containing the elt. */
    public LinkNode<T> find(T elt) {
      LinkNode<T> result = null;

      for (LinkNode<T> linkNode = this; linkNode != null; linkNode = linkNode.next) {
        if ((elt == linkNode.elt) || (elt != null && elt.equals(linkNode.elt))) {
          result = linkNode;
          break;
        }
      }

      return result;
    }

    /** Find the next link node whose element matches this node's element. */
    public LinkNode<T> findNext() {
      return (next == null) ? null : next.find(this.elt);
    }

    private void setIndex(int index) {
      this.index = index;
    }
  }
}
