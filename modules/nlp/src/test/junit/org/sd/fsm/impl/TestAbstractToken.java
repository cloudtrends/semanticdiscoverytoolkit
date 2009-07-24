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
package org.sd.fsm.impl;


import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.sd.fsm.Token;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * JUnit Tests for the AbstractToken class.
 * <p>
 * @author Spence Koehler
 */
public class TestAbstractToken extends TestCase {

  public TestAbstractToken(String name) {
    super(name);
  }
  
  public void testEquals() {
    assertEquals(GrammarToken.ZERO_OR_MORE, GrammarToken.ZERO_OR_MORE);
    assertEquals(GrammarToken.ONE_OR_MORE, GrammarToken.ONE_OR_MORE);
    assertEquals(GrammarToken.OPTIONAL, GrammarToken.OPTIONAL);
    assertEquals(GrammarToken.END, GrammarToken.END);

    assertFalse(new StringToken("a").equals(new StringToken("b")));
    assertTrue(new StringToken("a").equals(new StringToken("a")));
    assertEquals(new StringToken("a"), new StringToken("a"));
  }

  public void testSetBehavior() {
    final Set<Token> set = new LinkedHashSet<Token>();
    set.add(new StringToken("a"));
    set.add(new StringToken("b"));
    set.add(new StringToken("c"));

    assertTrue(set.contains(new StringToken("a")));
    assertTrue(set.contains(new StringToken("b")));
    assertTrue(set.contains(new StringToken("c")));
    assertFalse(set.contains(new StringToken("d")));

    set.add(GrammarToken.ZERO_OR_MORE);
    set.add(GrammarToken.ONE_OR_MORE);
    set.add(GrammarToken.OPTIONAL);
    assertTrue(set.contains(GrammarToken.ZERO_OR_MORE));
    assertTrue(set.contains(GrammarToken.ONE_OR_MORE));
    assertTrue(set.contains(GrammarToken.OPTIONAL));
    assertFalse(set.contains(GrammarToken.END));
  }

  public static Test suite() {
    TestSuite suite = new TestSuite(TestAbstractToken.class);
    return suite;
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}
