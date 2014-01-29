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


import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * JUnit Tests for the DoublyLinkedList class.
 * <p>
 * @author Spence Koehler
 */
public class TestDoublyLinkedList extends TestCase {

  public TestDoublyLinkedList(String name) {
    super(name);
  }
  

  public void testBasicBuildAndGet() {
    for (int size = 0; size < 15; ++size) {
      final DoublyLinkedList<Integer> list = new DoublyLinkedList<Integer>();
      DoublyLinkedList.LinkNode<Integer> prevLinkNode = null;      
      for (int i = 0; i < size; ++i) {
        final DoublyLinkedList.LinkNode<Integer> linkNode = list.add(i);
        assertEquals(i, linkNode.getIndex());
        assertEquals("i=" + i, (Integer)i, linkNode.getElt());
        assertEquals(prevLinkNode, linkNode.getPrev());
        assertNull(linkNode.getNext());
        assertEquals(i + 1, list.size());
        prevLinkNode = linkNode;
      }
      assertEquals("size=" + size, size, list.size());
      for (int i = 0; i < size; ++i) {
        assertEquals("i=" + i, (Integer)i, list.get(i));
      }
    }
  }

  public void testInsertAndRemove() {
    final DoublyLinkedList<Integer> list = new DoublyLinkedList<Integer>();

    // 2 5 8
    // 1* b2 5 8
    // 1 2a *3 5 8
    // 1 2 3 4* b5 8
    // 1 2 3 4 5 7* b8
    // 1 2 3 4 5 6* b7 8
    // 1 2 3 4 5 6 7 8a *9
    // 1 2 3 4 5 6 7 8 9a *10
    // 0* b1 2 3 4 5 6 7 8 9 10

    list.add(2).add(5).add(8);

    final DoublyLinkedList.LinkNode<Integer> node2 = list.find(2);
    node2.push(1);
    final DoublyLinkedList.LinkNode<Integer> node3 = node2.add(3);
    node3.getNext().push(4).find(8).add(9);
    list.add(10);
    list.getFirst().push(0);
    list.find(8).push(7).push(6);

    list.getLast().add(12);
    list.add(11, 11);
    list.add(13, 13);

    assertEquals(14, list.size());
    for (int i = 0; i <= 13; ++i) {
      assertEquals("i=" + i, (Integer)i, list.get(i));

      final DoublyLinkedList.LinkNode<Integer> node = list.getLinkNode(i);
      assertEquals(i, node.getIndex());
      assertEquals((Integer)i, node.getElt());
    }

    // test findNext
    final DoublyLinkedList.LinkNode<Integer> secondNode2 = list.add(2);
    final DoublyLinkedList.LinkNode<Integer> foundSecondNode2 = node2.findNext();
    assertEquals(secondNode2, foundSecondNode2);
    assertNull(secondNode2.findNext());
    secondNode2.add(3);  // 0 1 2 3 4 5 6 7 8 9 10 11 12 13 2 3
    assertNull(secondNode2.findNext());

    // test remove
    assertEquals(16, list.size());
    assertEquals((Integer)2, list.remove(14));
    assertEquals(15, list.size());
    assertEquals((Integer)3, list.remove(14));

    assertEquals(14, list.size());
    for (int i = 0; i <= 13; ++i) {
      assertEquals("i=" + i, (Integer)i, list.get(i));

      final DoublyLinkedList.LinkNode<Integer> node = list.getLinkNode(i);
      assertEquals(i, node.getIndex());
      assertEquals((Integer)i, node.getElt());
    }

    // completely drain the list
    for (int i = 0; i < 14; ++i) {
      assertEquals("i=" + i, (Integer)i, list.remove(0));
      assertEquals(13 - i, list.size());
    }
  }


  public static Test suite() {
    TestSuite suite = new TestSuite(TestDoublyLinkedList.class);
    return suite;
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}
