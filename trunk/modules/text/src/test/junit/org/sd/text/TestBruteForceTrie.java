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
package org.sd.text;


import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * JUnit Tests for the BruteForceTrie class.
 * <p>
 * @author Spence Koehler
 */
public class TestBruteForceTrie extends TestCase {

  public TestBruteForceTrie(String name) {
    super(name);
  }
  
  private final void checkFooBarBaz(BruteForceTrie trie) {
    assertTrue(trie.contains("foo"));
    assertTrue(trie.contains("bar"));
    assertTrue(trie.contains("baz"));

    assertFalse(trie.contains("boo"));
    assertFalse(trie.contains("far"));
    assertFalse(trie.contains("car"));
    assertFalse(trie.contains("faz"));

    assertFalse(trie.contains("fo"));
    assertFalse(trie.contains("ba"));
    assertFalse(trie.contains("ba"));

    assertFalse(trie.contains("foof"));
    assertFalse(trie.contains("barb"));
    assertFalse(trie.contains("bazy"));
  }

  public void testBasics() {
    final BruteForceTrie trie = new BruteForceTrie(new String[]{"foo", "bar", "baz"});
    checkFooBarBaz(trie);
  }

  public void testDumpAndRead() throws IOException {
    final BruteForceTrie trie = new BruteForceTrie(new String[]{"foo", "bar", "baz"});
    checkFooBarBaz(trie);

    final ByteArrayOutputStream bytesOut = new ByteArrayOutputStream();
    final DataOutputStream dataOut = new DataOutputStream(bytesOut);
    trie.dump(dataOut);
    dataOut.close();

    final byte[] bytes = bytesOut.toByteArray();
    final ByteArrayInputStream bytesIn = new  ByteArrayInputStream(bytes);
    final DataInputStream dataIn = new DataInputStream(bytesIn);

    final BruteForceTrie reTrie = new BruteForceTrie();
    reTrie.read(dataIn);
    dataIn.close();

    checkFooBarBaz(reTrie);
  }

  public static Test suite() {
    TestSuite suite = new TestSuite(TestBruteForceTrie.class);
    return suite;
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}
