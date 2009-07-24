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
package org.sd.text.radixtree;


import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * JUnit Tests for the RadixTreeImpl class.
 * <p>
 * @author Spence Koehler
 */
public class TestRadixTreeImpl extends TestCase {

  public TestRadixTreeImpl(String name) {
    super(name);
  }
  

  public void testInsert() {
    RadixTreeImpl<String> trie = new RadixTreeImpl<String>();

    trie.insert("apple", "apple");
    trie.insert("bat", "bat");
    trie.insert("ape", "ape");
    trie.insert("bath", "bath");
    trie.insert("banana", "banana");
        
    boolean result = false;
    try {
      trie.insert("apple", "apple2");
    }
    catch (IllegalStateException e) {
      result = true;
    }
        
    assertTrue(result);

    assertEquals(trie.find("apple"), "apple");
    assertEquals(trie.find("bat"), "bat");
    assertEquals(trie.find("ape"), "ape");
    assertEquals(trie.find("bath"), "bath");
    assertEquals(trie.find("banana"), "banana");
  }
    
  public void testInsert2() {
    RadixTreeImpl<String> trie = new RadixTreeImpl<String>();

    trie.insert("xbox 360", "xbox 360");
    trie.insert("xbox", "xbox");
    trie.insert("xbox 360 games", "xbox 360 games");
    trie.insert("xbox games", "xbox games");
    
    trie.insert("xbox xbox 360", "xbox xbox 360");
    trie.insert("xbox xbox", "xbox xbox");
    trie.insert("xbox 360 xbox games", "xbox 360 xbox games");
    trie.insert("xbox games 360", "xbox games 360");
    trie.insert("xbox 360 360", "xbox 360 360");
    trie.insert("xbox 360 xbox 360", "xbox 360 xbox 360");
    trie.insert("360 xbox games 360", "360 xbox games 360");
    trie.insert("xbox xbox 361", "xbox xbox 361");
  }

  public void testDelete() {
    RadixTreeImpl<String> trie = new RadixTreeImpl<String>();

    trie.insert("apple", "apple");
    trie.insert("appleshack", "appleshack");
    trie.insert("appleshackcream", "appleshackcream");
    trie.insert("applepie", "applepie");
    trie.insert("ape", "ape");

    assertTrue(trie.contains("apple"));
    assertTrue(trie.delete("apple"));
    assertFalse(trie.contains("apple"));

    assertTrue(trie.contains("applepie"));
    assertTrue(trie.delete("applepie"));
    assertFalse(trie.contains("applepie"));

    assertTrue(trie.contains("appleshack"));
    assertTrue(trie.delete("appleshack"));
    assertFalse(trie.contains("appleshack"));

    // try to delete "apple" again this should fail
    assertFalse(trie.delete("apple"));

    // try to find "ape" and "appleshackcream"
    assertTrue(trie.contains("appleshackcream"));
    assertTrue(trie.contains("ape"));

    // try to delete "ap" this should fail.
    assertFalse(trie.delete("ap"));
  }

  public void testFind() {
    RadixTreeImpl<String> trie = new RadixTreeImpl<String>();

    trie.insert("apple", "apple");
    trie.insert("appleshack", "appleshack");
    trie.insert("appleshackcream", "appleshackcream");
    trie.insert("applepie", "applepie");
    trie.insert("ape", "ape");

    // we shou7ld be able to find all of these
    assertNotNull(trie.find("apple"));
    assertNotNull(trie.find("appleshack"));
    assertNotNull(trie.find("appleshackcream"));
    assertNotNull(trie.find("applepie"));
    assertNotNull(trie.find("ape"));

    // try to delete "apple" again this should fail
    assertNull(trie.find("ap"));
    assertNull(trie.find("apple2"));
    assertNull(trie.find("appl"));
    assertNull(trie.find("app"));
    assertNull(trie.find("appples"));
  }

  public void testContains() {
    RadixTreeImpl<String> trie = new RadixTreeImpl<String>();

    trie.insert("apple", "apple");
    trie.insert("appleshack", "appleshack");
    trie.insert("appleshackcream", "appleshackcream");
    trie.insert("applepie", "applepie");
    trie.insert("ape", "ape");

    // we shou7ld be able to find all of these
    assertTrue(trie.contains("apple"));
    assertTrue(trie.contains("appleshack"));
    assertTrue(trie.contains("appleshackcream"));
    assertTrue(trie.contains("applepie"));
    assertTrue(trie.contains("ape"));

    // try to delete "apple" again this should fail
    assertFalse(trie.contains("ap"));
    assertFalse(trie.contains("apple2"));
    assertFalse(trie.contains("appl"));
    assertFalse(trie.contains("app"));
    assertFalse(trie.contains("appples"));
  }

  public void testContains2() {
    RadixTreeImpl<String> trie = null;

    trie = new RadixTreeImpl<String>();
    trie.insert("abba", "abba");
    trie.insert("abab", "abab");
    trie.insert("baba", "baba");
    assertTrue(trie.contains("abba"));
    assertTrue(trie.contains("abab"));
    assertTrue(trie.contains("baba"));


    trie = new RadixTreeImpl<String>();
    trie.insert("abba", "abba");
    trie.insert("baba", "baba");
    trie.insert("abab", "abab");
    assertTrue(trie.contains("abba"));
    assertTrue(trie.contains("abab"));
    assertTrue(trie.contains("baba"));


    trie = new RadixTreeImpl<String>();
    trie.insert("baba", "baba");
    trie.insert("abab", "abab");
    trie.insert("abba", "abba");
    assertTrue(trie.contains("abba"));
    assertTrue(trie.contains("abab"));
    assertTrue(trie.contains("baba"));


    trie = new RadixTreeImpl<String>();
    trie.insert("baba", "baba");
    trie.insert("abba", "abba");
    trie.insert("abab", "abab");
    assertTrue(trie.contains("abba"));
    assertTrue(trie.contains("abab"));
    assertTrue(trie.contains("baba"));


    trie = new RadixTreeImpl<String>();
    trie.insert("abab", "abab");
    trie.insert("baba", "baba");
    trie.insert("abba", "abba");
    assertTrue(trie.contains("abba"));
    assertTrue(trie.contains("abab"));
    assertTrue(trie.contains("baba"));


    trie = new RadixTreeImpl<String>();
    trie.insert("abab", "abab");
    trie.insert("abba", "abba");
    trie.insert("baba", "baba");
    assertTrue(trie.contains("abba"));
    assertTrue(trie.contains("abab"));
    assertTrue(trie.contains("baba"));
  }

  public void testSearchPrefix() {
    RadixTreeImpl<String> trie = new RadixTreeImpl<String>();

    trie.insert("apple", "apple");
    trie.insert("appleshack", "appleshack");
    trie.insert("appleshackcream", "appleshackcream");
    trie.insert("applepie", "applepie");
    trie.insert("ape", "ape");

    List<String> expected = new ArrayList<String>();
    expected.add("appleshack");
    expected.add("applepie");
    expected.add("apple");

    List<String> result = trie.searchPrefix("appl", 3);
    assertTrue(expected.size() == result.size());
    Set<String> resultSet = new HashSet<String>(result);
    for (String key : expected) {
      assertTrue(resultSet.contains(key));
    }

    expected.add("appleshackcream");

    result = trie.searchPrefix("app", 10);
    assertTrue(expected.size() == result.size());
    resultSet = new HashSet<String>(result);
    for (String key : expected) {
      assertTrue(resultSet.contains(key));
    }
  }
    
  public void testGetSize() {
    RadixTreeImpl<String> trie = new RadixTreeImpl<String>();

    trie.insert("apple", "apple");
    trie.insert("appleshack", "appleshack");
    trie.insert("appleshackcream", "appleshackcream");
    trie.insert("applepie", "applepie");
    trie.insert("ape", "ape");
        
    assertTrue(trie.getSize() == 5);
        
    trie.delete("appleshack");
        
    assertTrue(trie.getSize() == 4);
  }


  public static Test suite() {
    TestSuite suite = new TestSuite(TestRadixTreeImpl.class);
    return suite;
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}
