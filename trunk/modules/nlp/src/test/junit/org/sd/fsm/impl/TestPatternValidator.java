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

import org.sd.fsm.State;
import org.sd.fsm.Token;
import org.sd.util.tree.Tree;

import java.util.List;

/**
 * JUnit Tests for the PatternValidator class.
 * <p>
 * @author Spence Koehler
 */
public class TestPatternValidator extends TestCase {

  public TestPatternValidator(String name) {
    super(name);
  }
  
  private final PatternValidator getValidator(String[] rules) {
    return new PatternValidator(GrammarImpl.loadGrammar(rules, DefaultGrammarTokenFactory.getInstance(), null));
  }

  private final void checkStates(List<State> states, String[] expectedTreeStrings) {
    if (expectedTreeStrings == null) {
      assertNull("expected null, but got: " + states, states);
    }
    else {
      assertNotNull(states);
      if (expectedTreeStrings.length != states.size()) {
        int counter = 0;
        for (State state : states) {
          final Tree<Token> tree = state.buildTree();
          System.out.println("tree[" + (counter++) + "]=" + tree);
        }
      }
      assertEquals("got states: " + states, expectedTreeStrings.length, states.size());

      int index = 0;
      for (State state : states) {
        final Tree<Token> tree = state.buildTree();
        assertEquals("got=" + tree.toString() + ", expected=" + expectedTreeStrings[index], expectedTreeStrings[index], tree.toString());
        ++index;
      }
    }
  }

  public void testAXCrule() {
    final PatternValidator validator = getValidator(new String[]{"x <- a x c"});
    List<State> validStates = null;

    validStates = validator.getValidStates(new String[]{"a", "x", "c"});
    checkStates(validStates, new String[]{"(x a x c)"});

    validStates = validator.getValidStates(new String[]{"a", "a", "x", "c", "c"});
    checkStates(validStates, new String[]{"(x a (x a x c) c)"});

    validStates = validator.getValidStates(new String[]{"a", "a", "a", "x", "c", "c", "c"});
    checkStates(validStates, new String[]{"(x a (x a (x a x c) c) c)"});

    validStates = validator.getValidStates(new String[]{"a", "x"});
    checkStates(validStates, null);
  }

  public void testOptional() {
    final PatternValidator validator = getValidator(new String[] {"x <- a b ? c"});
    List<State> validStates = null;

    validStates = validator.getValidStates(new String[]{"a", "b", "c"});
    checkStates(validStates, new String[]{"(x a b c)"});

    validStates = validator.getValidStates(new String[]{"a", "c"});
    checkStates(validStates, new String[]{"(x a c)"});

    validStates = validator.getValidStates(new String[]{"a", "b", "b", "c"});
    checkStates(validStates, null);
  }

  public void testZeroOrMore() {
    final PatternValidator validator = getValidator(new String[] {"x <- a b * c"});
    List<State> validStates = null;

    validStates = validator.getValidStates(new String[]{"a", "c"});
    checkStates(validStates, new String[]{"(x a c)"});

    validStates = validator.getValidStates(new String[]{"a", "b", "c"});
    checkStates(validStates, new String[]{"(x a b c)"});

    validStates = validator.getValidStates(new String[]{"a", "b", "b", "c"});
    checkStates(validStates, new String[]{"(x a b b c)"});

    validStates = validator.getValidStates(new String[]{"a", "b", "b", "b", "c"});
    checkStates(validStates, new String[]{"(x a b b b c)"});
  }

  public void testOneOrMore() {
    final PatternValidator validator = getValidator(new String[] {"x <- a b + c"});
    List<State> validStates = null;

    validStates = validator.getValidStates(new String[]{"a", "c"});
    checkStates(validStates, null);

    validStates = validator.getValidStates(new String[]{"a", "b", "c"});
    checkStates(validStates, new String[]{"(x a b c)"});

    validStates = validator.getValidStates(new String[]{"a", "b", "b", "c"});
    checkStates(validStates, new String[]{"(x a b b c)"});

    validStates = validator.getValidStates(new String[]{"a", "b", "b", "b", "c"});
    checkStates(validStates, new String[]{"(x a b b b c)"});
  }

  public void testEnd() {
    final PatternValidator validator = getValidator(new String[] {"x <- a b . c"});
    List<State> validStates = null;

    validStates = validator.getValidStates(new String[]{"a", "b"});
    checkStates(validStates, new String[]{"(x a b)"});

    validStates = validator.getValidStates(new String[]{"a", "b", "c"});
    checkStates(validStates, new String[]{"(x a b c)"});

    validStates = validator.getValidStates(new String[]{"a"});
    checkStates(validStates, null);
  }

  public void testEndWithOptional1() {
    final PatternValidator validator = getValidator(new String[] {"x <- a b ? . c"});
    List<State> validStates = null;

    validStates = validator.getValidStates(new String[]{"a"});
    checkStates(validStates, new String[]{"(x a)"});

    validStates = validator.getValidStates(new String[]{"a", "b"});
    checkStates(validStates, new String[]{"(x a b)"});

    validStates = validator.getValidStates(new String[]{"a", "b", "c"});
    checkStates(validStates, new String[]{"(x a b c)"});

    validStates = validator.getValidStates(new String[]{"a", "c"});
    checkStates(validStates, new String[]{"(x a c)"});
  }

  public void testEndWithOptional2() {
    final PatternValidator validator = getValidator(new String[] {"x <- a b ? . c ?"});
    List<State> validStates = null;

    validStates = validator.getValidStates(new String[]{"a"});
    checkStates(validStates, new String[]{"(x a)"});

    validStates = validator.getValidStates(new String[]{"a", "b"});
    checkStates(validStates, new String[]{"(x a b)"});

    validStates = validator.getValidStates(new String[]{"a", "b", "c"});
    checkStates(validStates, new String[]{"(x a b c)"});

    validStates = validator.getValidStates(new String[]{"a", "c"});
    checkStates(validStates, new String[]{"(x a c)"});
  }

  public void testEndWithOptional3() {
    final PatternValidator validator = getValidator(new String[] {"x <- a b ? c ?"});
    List<State> validStates = null;

    validStates = validator.getValidStates(new String[]{"a"});
    checkStates(validStates, new String[]{"(x a)"});

    validStates = validator.getValidStates(new String[]{"a", "b"});
    checkStates(validStates, new String[]{"(x a b)"});

    validStates = validator.getValidStates(new String[]{"a", "b", "c"});
    checkStates(validStates, new String[]{"(x a b c)"});

    validStates = validator.getValidStates(new String[]{"a", "c"});
    checkStates(validStates, new String[]{"(x a c)"});
  }

  public void testEndWithOptional4() {
    final PatternValidator validator = getValidator(new String[] {"x <- a b . c ?"});
    List<State> validStates = null;

    validStates = validator.getValidStates(new String[]{"a"});
    checkStates(validStates, null);

    validStates = validator.getValidStates(new String[]{"a", "b"});
    checkStates(validStates, new String[]{"(x a b)"});

    validStates = validator.getValidStates(new String[]{"a", "b", "c"});
    checkStates(validStates, new String[]{"(x a b c)"});

    validStates = validator.getValidStates(new String[]{"a", "c"});
    checkStates(validStates, null);
  }

  public void testEndWithZeroOrMore1() {
    final PatternValidator validator = getValidator(new String[] {"x <- a b * . c"});
    List<State> validStates = null;

    validStates = validator.getValidStates(new String[]{"a"});
    checkStates(validStates, new String[]{"(x a)"});

    validStates = validator.getValidStates(new String[]{"a", "b"});
    checkStates(validStates, new String[]{"(x a b)"});

    validStates = validator.getValidStates(new String[]{"a", "b", "b"});
    checkStates(validStates, new String[]{"(x a b b)"});

    validStates = validator.getValidStates(new String[]{"a", "b", "b", "b"});
    checkStates(validStates, new String[]{"(x a b b b)"});

    validStates = validator.getValidStates(new String[]{"a", "b", "c"});
    checkStates(validStates, new String[]{"(x a b c)"});

    validStates = validator.getValidStates(new String[]{"a", "b", "b", "b", "c"});
    checkStates(validStates, new String[]{"(x a b b b c)"});

    validStates = validator.getValidStates(new String[]{"a", "c"});
    checkStates(validStates, new String[]{"(x a c)"});
  }

  public void testEndWithZeroOrMore2() {
    final PatternValidator validator = getValidator(new String[] {"x <- a b * . c *"});
    List<State> validStates = null;

    validStates = validator.getValidStates(new String[]{"a"});
    checkStates(validStates, new String[]{"(x a)"});

    validStates = validator.getValidStates(new String[]{"a", "b"});
    checkStates(validStates, new String[]{"(x a b)"});

    validStates = validator.getValidStates(new String[]{"a", "b", "b"});
    checkStates(validStates, new String[]{"(x a b b)"});

    validStates = validator.getValidStates(new String[]{"a", "b", "b", "b"});
    checkStates(validStates, new String[]{"(x a b b b)"});

    validStates = validator.getValidStates(new String[]{"a", "b", "c"});
    checkStates(validStates, new String[]{"(x a b c)"});

    validStates = validator.getValidStates(new String[]{"a", "b", "b", "b", "c"});
    checkStates(validStates, new String[]{"(x a b b b c)"});

    validStates = validator.getValidStates(new String[]{"a", "b", "b", "b", "c", "c"});
    checkStates(validStates, new String[]{"(x a b b b c c)"});

    validStates = validator.getValidStates(new String[]{"a", "c"});
    checkStates(validStates, new String[]{"(x a c)"});
  }

  public void testEndWithOneOrMore() {
    final PatternValidator validator = getValidator(new String[] {"x <- a b + . c"});
    List<State> validStates = null;

    validStates = validator.getValidStates(new String[]{"a"});
    checkStates(validStates, null);

    validStates = validator.getValidStates(new String[]{"a", "b"});
    checkStates(validStates, new String[]{"(x a b)"});

    validStates = validator.getValidStates(new String[]{"a", "b", "b"});
    checkStates(validStates, new String[]{"(x a b b)"});

    validStates = validator.getValidStates(new String[]{"a", "b", "b", "b"});
    checkStates(validStates, new String[]{"(x a b b b)"});

    validStates = validator.getValidStates(new String[]{"a", "b", "c"});
    checkStates(validStates, new String[]{"(x a b c)"});

    validStates = validator.getValidStates(new String[]{"a", "b", "b", "b", "c"});
    checkStates(validStates, new String[]{"(x a b b b c)"});
  }

  public void testExplicitRepeats() {
    final PatternValidator validator = getValidator(new String[] {"x <- a b b c"});
    List<State> validStates = null;

    validStates = validator.getValidStates(new String[]{"a"});
    checkStates(validStates, null);

    validStates = validator.getValidStates(new String[]{"a", "b"});
    checkStates(validStates, null);

    validStates = validator.getValidStates(new String[]{"a", "b", "b"});
    checkStates(validStates, null);

    validStates = validator.getValidStates(new String[]{"a", "b", "b", "b"});
    checkStates(validStates, null);

    validStates = validator.getValidStates(new String[]{"a", "b", "c"});
    checkStates(validStates, null);

    validStates = validator.getValidStates(new String[]{"a", "b", "b", "c"});
    checkStates(validStates, new String[]{"(x a b b c)"});

    validStates = validator.getValidStates(new String[]{"a", "b", "b", "b", "c"});
    checkStates(validStates, null);
  }

  public void testEndWithOneOrMoreAmbiguous() {
    final PatternValidator validator = getValidator(new String[] {"x <- a b + . b c"});
    List<State> validStates = null;

    validStates = validator.getValidStates(new String[]{"a"});
    checkStates(validStates, null);

    validStates = validator.getValidStates(new String[]{"a", "b"});
    checkStates(validStates, new String[]{"(x a b)"});

    validStates = validator.getValidStates(new String[]{"a", "b", "b"});
    checkStates(validStates, new String[]{"(x a b b)"});

    validStates = validator.getValidStates(new String[]{"a", "b", "b", "b"});
    checkStates(validStates, new String[]{"(x a b b b)"});

    validStates = validator.getValidStates(new String[]{"a", "b", "c"});
    checkStates(validStates, null);

    validStates = validator.getValidStates(new String[]{"a", "b", "b", "c"});
    checkStates(validStates, new String[]{"(x a b b c)"});

    validStates = validator.getValidStates(new String[]{"a", "b", "b", "b", "c"});
    checkStates(validStates, new String[]{"(x a b b b c)"});
  }

  public void testEndWithOneOrMoreExtraAmbiguous1() {
    final PatternValidator validator = getValidator(new String[] {"x <- a b + . b * c"});
    List<State> validStates = null;

    validStates = validator.getValidStates(new String[]{"a"});
    checkStates(validStates, null);

    validStates = validator.getValidStates(new String[]{"a", "b"});
    checkStates(validStates, new String[]{"(x a b)"});

    validStates = validator.getValidStates(new String[]{"a", "b", "b"});
    checkStates(validStates, new String[]{"(x a b b)"});

    validStates = validator.getValidStates(new String[]{"a", "b", "b", "b"});
    checkStates(validStates, new String[]{"(x a b b b)"});

    validStates = validator.getValidStates(new String[]{"a", "b", "b", "c"});
    checkStates(validStates, new String[]{"(x a b b c)", "(x a b b c)"});

    validStates = validator.getValidStates(new String[]{"a", "b", "b", "b", "c"});
    checkStates(validStates, new String[]{"(x a b b b c)", "(x a b b b c)", "(x a b b b c)"});

    validStates = validator.getValidStates(new String[]{"a", "b", "c"});
    checkStates(validStates, new String[]{"(x a b c)"});
  }

  public void testEndWithOneOrMoreExtraAmbiguous2() {
    final PatternValidator validator = getValidator(new String[] {"x <- a b + . b * c ?"});
    List<State> validStates = null;

    validStates = validator.getValidStates(new String[]{"a"});
    checkStates(validStates, null);

    validStates = validator.getValidStates(new String[]{"a", "b"});
    checkStates(validStates, new String[]{"(x a b)"});

    validStates = validator.getValidStates(new String[]{"a", "b", "b"});
    checkStates(validStates, new String[]{"(x a b b)", "(x a b b)"});

    validStates = validator.getValidStates(new String[]{"a", "b", "b", "b"});
    checkStates(validStates, new String[]{"(x a b b b)", "(x a b b b)", "(x a b b b)"});

    validStates = validator.getValidStates(new String[]{"a", "b", "b", "c"});
    checkStates(validStates, new String[]{"(x a b b c)", "(x a b b c)"});

    validStates = validator.getValidStates(new String[]{"a", "b", "b", "b", "c"});
    checkStates(validStates, new String[]{"(x a b b b c)", "(x a b b b c)", "(x a b b b c)"});

    validStates = validator.getValidStates(new String[]{"a", "b", "c"});
    checkStates(validStates, new String[]{"(x a b c)"});
  }

  public void testSpinPastOptionals() {
    final PatternValidator validator = getValidator(new String[] {"x <- a b ? b * c"});
    List<State> validStates = null;

    validStates = validator.getValidStates(new String[]{"a"});
    checkStates(validStates, null);

    validStates = validator.getValidStates(new String[]{"a", "b", "c"});
    checkStates(validStates, new String[]{"(x a b c)", "(x a b c)"});

    validStates = validator.getValidStates(new String[]{"a", "b", "b", "c"});
    checkStates(validStates, new String[]{"(x a b b c)", "(x a b b c)"});

    validStates = validator.getValidStates(new String[]{"a", "b", "b", "b", "c"});
    checkStates(validStates, new String[]{"(x a b b b c)", "(x a b b b c)"});

    validStates = validator.getValidStates(new String[]{"a", "b", "c"});
    checkStates(validStates, new String[]{"(x a b c)", "(x a b c)"});
  }

  public void testOnlyValidPushes1() {
    final PatternValidator validator = getValidator(new String[]{"y <- a x c"});
    List<State> validStates = null;

    validStates = validator.getValidStates(new String[]{"a", "x", "c"});
    checkStates(validStates, new String[]{"(y a x c)"});

    validStates = validator.getValidStates(new String[]{"a", "a", "x", "c", "c"});
    checkStates(validStates, null);

    validStates = validator.getValidStates(new String[]{"a", "a", "a", "x", "c", "c", "c"});
    checkStates(validStates, null);

    validStates = validator.getValidStates(new String[]{"a", "x"});
    checkStates(validStates, null);

    validStates = validator.getValidStates(new String[]{"a", "c"});
    checkStates(validStates, null);
  }

  public void testOnlyValidPushes2() {
    final PatternValidator validator = getValidator(new String[]{"y <- a x c", "x <- b +"});
    List<State> validStates = null;

    validStates = validator.getValidStates(new String[]{"a", "x", "c"});
    checkStates(validStates, new String[]{"(y a x c)"});

    validStates = validator.getValidStates(new String[]{"a", "b", "c"});
    checkStates(validStates, new String[]{"(y a (x b) c)"});

    validStates = validator.getValidStates(new String[]{"a", "b", "b", "c"});
    checkStates(validStates, new String[]{"(y a (x b b) c)"});

    validStates = validator.getValidStates(new String[]{"a", "b", "b", "b", "c"});
    checkStates(validStates, new String[]{"(y a (x b b b) c)"});

    validStates = validator.getValidStates(new String[]{"a", "x"});
    checkStates(validStates, null);

    validStates = validator.getValidStates(new String[]{"a", "b"});
    checkStates(validStates, null);

    validStates = validator.getValidStates(new String[]{"a", "c"});
    checkStates(validStates, null);
  }

  public void testRepeatingPushes1() {
    final PatternValidator validator = getValidator(new String[]{"y <- a x ? c", "x <- b +"});
    List<State> validStates = null;

    validStates = validator.getValidStates(new String[]{"a", "x", "c"});
    checkStates(validStates, new String[]{"(y a x c)"});

    validStates = validator.getValidStates(new String[]{"a", "b", "c"});
    checkStates(validStates, new String[]{"(y a (x b) c)"});

    validStates = validator.getValidStates(new String[]{"a", "b", "b", "c"});
    checkStates(validStates, new String[]{"(y a (x b b) c)"});

    validStates = validator.getValidStates(new String[]{"a", "b", "b", "b", "c"});
    checkStates(validStates, new String[]{"(y a (x b b b) c)"});

    validStates = validator.getValidStates(new String[]{"a", "x"});
    checkStates(validStates, null);

    validStates = validator.getValidStates(new String[]{"a", "b"});
    checkStates(validStates, null);

    validStates = validator.getValidStates(new String[]{"a", "c"});
    checkStates(validStates, new String[]{"(y a c)"});
  }

  public void testRepeatingPushes2() {
    final PatternValidator validator = getValidator(new String[]{"y <- a x + c", "x <- b +"});
    List<State> validStates = null;

    validStates = validator.getValidStates(new String[]{"a", "x", "c"});
    checkStates(validStates, new String[]{"(y a x c)"});

    validStates = validator.getValidStates(new String[]{"a", "b", "c"});
    checkStates(validStates, new String[]{"(y a (x b) c)"});

    validStates = validator.getValidStates(new String[]{"a", "b", "b", "c"});
    checkStates(validStates, new String[]{"(y a (x b b) c)", "(y a (x b) (x b) c)"});

    validStates = validator.getValidStates(new String[]{"a", "b", "b", "b", "c"});
    checkStates(validStates, new String[]{"(y a (x b b b) c)", "(y a (x b b) (x b) c)", "(y a (x b) (x b b) c)", "(y a (x b) (x b) (x b) c)"});

    validStates = validator.getValidStates(new String[]{"a", "x"});
    checkStates(validStates, null);

    validStates = validator.getValidStates(new String[]{"a", "b"});
    checkStates(validStates, null);

    validStates = validator.getValidStates(new String[]{"a", "c"});
    checkStates(validStates, null);
  }

  public void testRepeatingPushes3() {
    final PatternValidator validator = getValidator(new String[]{"y <- a x * c", "x <- b +"});
    List<State> validStates = null;

    validStates = validator.getValidStates(new String[]{"a", "x", "c"});
    checkStates(validStates, new String[]{"(y a x c)"});

    validStates = validator.getValidStates(new String[]{"a", "b", "c"});
    checkStates(validStates, new String[]{"(y a (x b) c)"});

    validStates = validator.getValidStates(new String[]{"a", "b", "b", "c"});
    checkStates(validStates, new String[]{"(y a (x b b) c)", "(y a (x b) (x b) c)"});

    validStates = validator.getValidStates(new String[]{"a", "b", "b", "b", "c"});
    checkStates(validStates, new String[]{"(y a (x b b b) c)", "(y a (x b b) (x b) c)", "(y a (x b) (x b b) c)", "(y a (x b) (x b) (x b) c)"});

    validStates = validator.getValidStates(new String[]{"a", "x"});
    checkStates(validStates, null);

    validStates = validator.getValidStates(new String[]{"a", "b"});
    checkStates(validStates, null);

    validStates = validator.getValidStates(new String[]{"a", "c"});
    checkStates(validStates, new String[]{"(y a c)"});
  }

  public void testRepeatingPushes4() {
    final PatternValidator validator = getValidator(new String[]{"y <- a x ? c", "x <- b"});
    List<State> validStates = null;

    validStates = validator.getValidStates(new String[]{"a", "x", "c"});
    checkStates(validStates, new String[]{"(y a x c)"});

    validStates = validator.getValidStates(new String[]{"a", "b", "c"});
    checkStates(validStates, new String[]{"(y a (x b) c)"});

    validStates = validator.getValidStates(new String[]{"a", "b", "b", "c"});
    checkStates(validStates, null);

    validStates = validator.getValidStates(new String[]{"a", "b", "b", "b", "c"});
    checkStates(validStates, null);

    validStates = validator.getValidStates(new String[]{"a", "x"});
    checkStates(validStates, null);

    validStates = validator.getValidStates(new String[]{"a", "b"});
    checkStates(validStates, null);

    validStates = validator.getValidStates(new String[]{"a", "c"});
    checkStates(validStates, new String[]{"(y a c)"});
  }

  public void testRepeatingPushes5() {
    final PatternValidator validator = getValidator(new String[]{"y <- a x + c", "x <- b"});
    List<State> validStates = null;

    validStates = validator.getValidStates(new String[]{"a", "x", "c"});
    checkStates(validStates, new String[]{"(y a x c)"});

    validStates = validator.getValidStates(new String[]{"a", "b", "c"});
    checkStates(validStates, new String[]{"(y a (x b) c)"});

    validStates = validator.getValidStates(new String[]{"a", "b", "b", "c"});
    checkStates(validStates, new String[]{"(y a (x b) (x b) c)"});

    validStates = validator.getValidStates(new String[]{"a", "b", "b", "b", "c"});
    checkStates(validStates, new String[]{"(y a (x b) (x b) (x b) c)"});

    validStates = validator.getValidStates(new String[]{"a", "x"});
    checkStates(validStates, null);

    validStates = validator.getValidStates(new String[]{"a", "b"});
    checkStates(validStates, null);

    validStates = validator.getValidStates(new String[]{"a", "c"});
    checkStates(validStates, null);
  }

  public void testRepeatingPushes6() {
    final PatternValidator validator = getValidator(new String[]{"y <- a x * c", "x <- b"});
    List<State> validStates = null;

    validStates = validator.getValidStates(new String[]{"a", "x", "c"});
    checkStates(validStates, new String[]{"(y a x c)"});

    validStates = validator.getValidStates(new String[]{"a", "b", "c"});
    checkStates(validStates, new String[]{"(y a (x b) c)"});

    validStates = validator.getValidStates(new String[]{"a", "b", "b", "c"});
    checkStates(validStates, new String[]{"(y a (x b) (x b) c)"});

    validStates = validator.getValidStates(new String[]{"a", "b", "b", "b", "c"});
    checkStates(validStates, new String[]{"(y a (x b) (x b) (x b) c)"});

    validStates = validator.getValidStates(new String[]{"a", "x"});
    checkStates(validStates, null);

    validStates = validator.getValidStates(new String[]{"a", "b"});
    checkStates(validStates, null);

    validStates = validator.getValidStates(new String[]{"a", "c"});
    checkStates(validStates, new String[]{"(y a c)"});

    validStates = validator.getValidStates(new String[]{"a", "x", "b", "x", "b", "c"});
    checkStates(validStates, new String[]{"(y a x (x b) x (x b) c)"});
  }

  public void testDeepPushesAndPops1() {
    final PatternValidator validator = getValidator(new String[]{"x <- x0 x1", "x0 <- x2", "x2 <- x3", "x1 <- x4", "x4 <- x5 x2"});
    List<State> validStates = null;

    validStates = validator.getValidStates(new String[]{"x3", "x5", "x3"});
    checkStates(validStates, new String[]{"(x (x0 (x2 x3)) (x1 (x4 x5 (x2 x3))))"});
  }

//todo: test that we can repeat "pushes", where these are real and "hardwired"
//todo: test that we can "pop" at an "end" in a "push"

  public static Test suite() {
    TestSuite suite = new TestSuite(TestPatternValidator.class);
    return suite;
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}
