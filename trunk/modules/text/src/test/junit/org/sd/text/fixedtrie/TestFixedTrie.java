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
package org.sd.text.fixedtrie;


import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * JUnit Tests for the FixedTrie class.
 * <p>
 * @author Spence Koehler
 */
public class TestFixedTrie extends TestCase {

  public TestFixedTrie(String name) {
    super(name);
  }
  
  public void testSimple() {
    final FixedTrie fixedTrie = new FixedTrie(4, 2);
    fixedTrie.add(new int[]{1, 0, 1, 0});

    for (int a = 0; a < 2; ++a) {
      for (int b = 0; b < 2; ++b) {
        for (int c = 0; c < 2; ++c) {
          for (int d = 0; d < 2; ++d) {
            if (a == 1 && b == 0 && c == 1 && d == 0) {
              assertTrue("expected true: " + a + "" + b + "" + c + "" + d, fixedTrie.contains(new int[]{a, b, c, d}));
            }
            else {
              assertFalse("expected false: " + a + "" + b + "" + c + "" + d, fixedTrie.contains(new int[]{a, b, c, d}));
            }
          }
        }
      }
    }

    fixedTrie.add(new int[]{1, 1, 1, 1});

    for (int a = 0; a < 2; ++a) {
      for (int b = 0; b < 2; ++b) {
        for (int c = 0; c < 2; ++c) {
          for (int d = 0; d < 2; ++d) {
            if (a == 1 && b == 0 && c == 1 && d == 0) {
              assertTrue("expected true: " + a + "" + b + "" + c + "" + d, fixedTrie.contains(new int[]{a, b, c, d}));
            }
            else if (a == 1 && b == 1 && c == 1 && d == 1) {
              assertTrue("expected true: " + a + "" + b + "" + c + "" + d, fixedTrie.contains(new int[]{a, b, c, d}));
            }
            else {
              assertFalse("expected false: " + a + "" + b + "" + c + "" + d, fixedTrie.contains(new int[]{a, b, c, d}));
            }
          }
        }
      }
    }

    fixedTrie.add(new int[]{0, 0, 0, 0});

    for (int a = 0; a < 2; ++a) {
      for (int b = 0; b < 2; ++b) {
        for (int c = 0; c < 2; ++c) {
          for (int d = 0; d < 2; ++d) {
            if (a == 1 && b == 0 && c == 1 && d == 0) {
              assertTrue("expected true: " + a + "" + b + "" + c + "" + d, fixedTrie.contains(new int[]{a, b, c, d}));
            }
            else if (a == 1 && b == 1 && c == 1 && d == 1) {
              assertTrue("expected true: " + a + "" + b + "" + c + "" + d, fixedTrie.contains(new int[]{a, b, c, d}));
            }
            else if (a == 0 && b == 0 && c == 0 && d == 0) {
              assertTrue("expected true: " + a + "" + b + "" + c + "" + d, fixedTrie.contains(new int[]{a, b, c, d}));
            }
            else {
              assertFalse("expected false: " + a + "" + b + "" + c + "" + d, fixedTrie.contains(new int[]{a, b, c, d}));
            }
          }
        }
      }
    }
  }

  public static Test suite() {
    TestSuite suite = new TestSuite(TestFixedTrie.class);
    return suite;
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}
