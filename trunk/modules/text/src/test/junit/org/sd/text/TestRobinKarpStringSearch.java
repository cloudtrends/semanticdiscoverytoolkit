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
 * JUnit Tests for the RobinKarpStringSearch class.
 * <p>
 * @author Spence Koehler
 */
public class TestRobinKarpStringSearch extends TestCase {

  public TestRobinKarpStringSearch(String name) {
    super(name);
  }
  
  public void test1() {
    final RobinKarpStringSearch rkSearch = new RobinKarpStringSearch(7, new String[]{"abcd"});
    assertEquals(null, rkSearch.search("", PatternFinder.ACCEPT_PARTIAL));
    assertEquals(null, rkSearch.search("a", PatternFinder.ACCEPT_PARTIAL));
    assertEquals(null, rkSearch.search("ab", PatternFinder.ACCEPT_PARTIAL));
    assertEquals(null, rkSearch.search("abc", PatternFinder.ACCEPT_PARTIAL));
    assertEquals(0, rkSearch.search("abcd", PatternFinder.ACCEPT_PARTIAL)[0]);
    assertEquals(1, rkSearch.search("xabcd", PatternFinder.ACCEPT_PARTIAL)[0]);
    assertEquals(2, rkSearch.search("xxabcd", PatternFinder.ACCEPT_PARTIAL)[0]);
    assertEquals(3, rkSearch.search("xxxabcd", PatternFinder.ACCEPT_PARTIAL)[0]);
    assertEquals(4, rkSearch.search("xxxxabcd", PatternFinder.ACCEPT_PARTIAL)[0]);
    assertEquals(null, rkSearch.search("xxxxabbcd", PatternFinder.ACCEPT_PARTIAL));
  }

  public void test2() {
    final RobinKarpStringSearch rkSearch = new RobinKarpStringSearch(7, new String[]{"abcd"});
    assertEquals(0, rkSearch.search("abcdyyyy", PatternFinder.ACCEPT_PARTIAL)[0]);
    assertEquals(1, rkSearch.search("xabcdyyyy", PatternFinder.ACCEPT_PARTIAL)[0]);
    assertEquals(2, rkSearch.search("xxabcdyyyy", PatternFinder.ACCEPT_PARTIAL)[0]);
    assertEquals(3, rkSearch.search("xxxabcdyyyy", PatternFinder.ACCEPT_PARTIAL)[0]);
    assertEquals(4, rkSearch.search("xxxxabcdyyyy", PatternFinder.ACCEPT_PARTIAL)[0]);
    assertEquals(null, rkSearch.search("xxxxabbcdyyyy", PatternFinder.ACCEPT_PARTIAL));
  }

  public void test3() {
    final RobinKarpStringSearch rkSearch = new RobinKarpStringSearch(7, new String[]{"abcd"});
    assertEquals(1, rkSearch.search("aabcd", PatternFinder.ACCEPT_PARTIAL)[0]);
    assertEquals(2, rkSearch.search("ababcd", PatternFinder.ACCEPT_PARTIAL)[0]);
    assertEquals(3, rkSearch.search("abcabcd", PatternFinder.ACCEPT_PARTIAL)[0]);
  }

  public void test4() {
    final RobinKarpStringSearch rkSearch = new RobinKarpStringSearch(7, new String[]{"abcd", "xxxx"});
    assertEquals(0, rkSearch.search("abcdyyyy", PatternFinder.ACCEPT_PARTIAL)[0]);
    assertEquals(1, rkSearch.search("xabcdyyyy", PatternFinder.ACCEPT_PARTIAL)[0]);
    assertEquals(2, rkSearch.search("xxabcdyyyy", PatternFinder.ACCEPT_PARTIAL)[0]);
    assertEquals(3, rkSearch.search("xxxabcdyyyy", PatternFinder.ACCEPT_PARTIAL)[0]);
    assertEquals(0, rkSearch.search("xxxxabcdyyyy", PatternFinder.ACCEPT_PARTIAL)[0]);
    assertEquals(0, rkSearch.search("xxxxabbcdyyyy", PatternFinder.ACCEPT_PARTIAL)[0]);
  }

  public void test5() {
    final RobinKarpStringSearch rkSearch = new RobinKarpStringSearch(7, new String[]{"mail", "email"});

    assertTrue(rkSearch.search("e-mail", PatternFinder.FULL_WORD) != null);
    assertTrue(rkSearch.search("email", PatternFinder.FULL_WORD) != null);
  }

  public void test7() {
    final RobinKarpStringSearch rkSearch = new RobinKarpStringSearch(7, new String[]{"good", "great"});

    assertTrue(rkSearch.search("He is good", PatternFinder.FULL_WORD) != null);
    assertTrue(rkSearch.search("She is great.", PatternFinder.FULL_WORD) != null);
  }

  public void testMatchAtBeginning() {
    final RobinKarpStringSearch rkSearch = new RobinKarpStringSearch(7, new String[]{"pre", "post"});

    assertTrue(rkSearch.search("pre", PatternFinder.BEGIN_WORD) != null);
    assertTrue(rkSearch.search("prepare", PatternFinder.BEGIN_WORD) != null);
    assertFalse(rkSearch.search("unprepared", PatternFinder.BEGIN_WORD) != null);
    assertFalse(rkSearch.search("xpre", PatternFinder.BEGIN_WORD) != null);
  }

  public void testMatchAtEnd() {
    final RobinKarpStringSearch rkSearch = new RobinKarpStringSearch(7, new String[]{"pre", "post"});

    assertTrue(rkSearch.search("post", PatternFinder.END_WORD) != null);
    assertTrue(rkSearch.search("compost", PatternFinder.END_WORD) != null);
    assertFalse(rkSearch.search("postfacto", PatternFinder.END_WORD) != null);
    assertFalse(rkSearch.search("posts", PatternFinder.END_WORD) != null);
  }

  private final void validate(RobinKarpStringSearch rkSearch, String input, int expectedStartPos, int expectedLength) {
    final int[] searchResult = rkSearch.search(input, 0, input.length(), PatternFinder.FULL_WORD);

    if (expectedStartPos < 0) {
      assertNull(searchResult);
    }
    else {
      assertEquals(expectedStartPos, searchResult[0]);
      assertEquals(expectedLength, searchResult[1]);
    }
  }

  public void testMatchLongerSubstring() {
    final RobinKarpStringSearch rkSearch = new RobinKarpStringSearch(7, new String[]{"foo bar baz", "foo bar", "foo"});

    validate(rkSearch, "123 foo bar baz 456", 4, 11);
    validate(rkSearch, "123 foo bar 456", 4, 7);
    validate(rkSearch, "123 foo 456", 4, 3);
  }

  public void testPersisting() throws IOException {
    final RobinKarpStringSearch rkSearch = new RobinKarpStringSearch(7, new String[]{"foo bar baz", "foo bar", "foo"});

    validate(rkSearch, "123 foo bar baz 456", 4, 11);
    validate(rkSearch, "123 foo bar 456", 4, 7);
    validate(rkSearch, "123 foo 456", 4, 3);

    final ByteArrayOutputStream bytesOut = new ByteArrayOutputStream();
    final DataOutputStream dataOut = new DataOutputStream(bytesOut);

    // serialize
    rkSearch.write(dataOut);
    dataOut.close();
    final byte[] bytes = bytesOut.toByteArray();
    bytesOut.close();

    // deserialize
    final ByteArrayInputStream bytesIn = new ByteArrayInputStream(bytes);
    final DataInputStream dataIn = new DataInputStream(bytesIn);
    final RobinKarpStringSearch rkSearch2 = new RobinKarpStringSearch();
    rkSearch2.read(dataIn);
    dataIn.close();

    validate(rkSearch2, "123 foo bar baz 456", 4, 11);
    validate(rkSearch2, "123 foo bar 456", 4, 7);
    validate(rkSearch2, "123 foo 456", 4, 3);
  }

  public static Test suite() {
    TestSuite suite = new TestSuite(TestRobinKarpStringSearch.class);
    return suite;
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}
