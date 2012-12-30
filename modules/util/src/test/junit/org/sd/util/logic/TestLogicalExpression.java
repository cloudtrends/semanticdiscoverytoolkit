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
package org.sd.util.logic;


import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.util.List;

/**
 * JUnit Tests for the LogicalExpression class.
 * <p>
 * @author Spence Koehler
 */
public class TestLogicalExpression extends TestCase {

  public TestLogicalExpression(String name) {
    super(name);
  }
  
  public void testSingle1() {
    final StringMatchTruthFunction truthFunction = new StringMatchTruthFunction();
    final LogicalExpression<String> expression = new LogicalExpression<String>("0", new StringMatchTruthFunction[]{truthFunction});

    List<LogicalResult<String>> result = null;

    result = expression.evaluate("true");
    assertEquals(1, result.size());
    assertTrue(result.get(0).isTrue());

    result = expression.evaluate("false");
    assertNull(result);
  }

  public void testSingle2() {
    final StringMatchTruthFunction truthFunction = new StringMatchTruthFunction();
    final LogicalExpression<String> expression = new LogicalExpression<String>("(0)", new StringMatchTruthFunction[]{truthFunction});

    List<LogicalResult<String>> result = null;

    result = expression.evaluate("true");
    assertEquals(1, result.size());
    assertTrue(result.get(0).isTrue());

    result = expression.evaluate("false");
    assertNull(result);
  }

  public void testSingle3() {
    final StringMatchTruthFunction truthFunction = new StringMatchTruthFunction();
    final LogicalExpression<String> expression = new LogicalExpression<String>("(and 0)", new StringMatchTruthFunction[]{truthFunction});

    List<LogicalResult<String>> result = null;

    result = expression.evaluate("true");
    assertEquals(1, result.size());
    assertTrue(result.get(0).isTrue());

    result = expression.evaluate("false");
    assertNull(result);
  }

  public void testSingle4() {
    final StringMatchTruthFunction truthFunction = new StringMatchTruthFunction();
    final LogicalExpression<String> expression = new LogicalExpression<String>("(or 0)", new StringMatchTruthFunction[]{truthFunction});

    List<LogicalResult<String>> result = null;

    result = expression.evaluate("true");
    assertEquals(1, result.size());
    assertTrue(result.get(0).isTrue());

    result = expression.evaluate("false");
    assertNull(result);
  }

  public void testSingle5() {
    final StringMatchTruthFunction truthFunction = new StringMatchTruthFunction();
    final LogicalExpression<String> expression = new LogicalExpression<String>("(not 0)", new StringMatchTruthFunction[]{truthFunction});

    List<LogicalResult<String>> result = null;

    result = expression.evaluate("false");
    assertEquals(1, result.size());
    assertFalse(result.get(0).isTrue());

    result = expression.evaluate("true");
    assertNull(result);
  }

  public void testOr() {
    final StringMatchTruthFunction foo = new StringMatchTruthFunction("foo");
    final StringMatchTruthFunction bar = new StringMatchTruthFunction("bar");
    final StringMatchTruthFunction baz = new StringMatchTruthFunction("baz");
    final LogicalExpression<String> expression = new LogicalExpression<String>("(or 0 1 2)", new StringMatchTruthFunction[]{foo, bar, baz});

    List<LogicalResult<String>> result = null;

    result = expression.evaluate("foo");
    assertEquals(1, result.size());
    assertTrue(result.get(0).isTrue());

    result = expression.evaluate("bar");
    assertEquals(1, result.size());
    assertTrue(result.get(0).isTrue());

    result = expression.evaluate("baz");
    assertEquals(1, result.size());
    assertTrue(result.get(0).isTrue());

    result = expression.evaluate("other");
    assertNull(result);
  }

  public void testAnd() {
    final StringContainsTruthFunction foo = new StringContainsTruthFunction("foo");
    final StringContainsTruthFunction bar = new StringContainsTruthFunction("bar");
    final StringContainsTruthFunction baz = new StringContainsTruthFunction("baz");
    final LogicalExpression<String> expression = new LogicalExpression<String>("(and 0 1 2)", new StringContainsTruthFunction[]{foo, bar, baz});

    List<LogicalResult<String>> result = null;

    result = expression.evaluate("foo bar baz");
    assertEquals(3, result.size());
    assertTrue(result.get(0).isTrue());
    assertTrue(result.get(1).isTrue());
    assertTrue(result.get(2).isTrue());

    result = expression.evaluate("bar foo baz");
    assertEquals(3, result.size());
    assertTrue(result.get(0).isTrue());
    assertTrue(result.get(1).isTrue());
    assertTrue(result.get(2).isTrue());

    result = expression.evaluate("foo baz bar");
    assertEquals(3, result.size());
    assertTrue(result.get(0).isTrue());
    assertTrue(result.get(1).isTrue());
    assertTrue(result.get(2).isTrue());

    result = expression.evaluate("foo");
    assertNull(result);

    result = expression.evaluate("bar");
    assertNull(result);

    result = expression.evaluate("baz");
    assertNull(result);

    result = expression.evaluate("other");
    assertNull(result);
  }


  public void testNotOr() {
    final StringContainsTruthFunction foo = new StringContainsTruthFunction("foo");
    final StringContainsTruthFunction bar = new StringContainsTruthFunction("bar");
    final StringContainsTruthFunction baz = new StringContainsTruthFunction("baz");
    final LogicalExpression<String> expression = new LogicalExpression<String>("(not (or 0 1 2))", new StringContainsTruthFunction[]{foo, bar, baz});

    List<LogicalResult<String>> result = null;

    result = expression.evaluate("boo far faz");
    assertEquals(3, result.size());
    assertFalse(result.get(0).isTrue());
    assertFalse(result.get(1).isTrue());
    assertFalse(result.get(2).isTrue());

    result = expression.evaluate("far boo faz");
    assertEquals(3, result.size());
    assertFalse(result.get(0).isTrue());
    assertFalse(result.get(1).isTrue());
    assertFalse(result.get(2).isTrue());

    result = expression.evaluate("boo faz far");
    assertEquals(3, result.size());
    assertFalse(result.get(0).isTrue());
    assertFalse(result.get(1).isTrue());
    assertFalse(result.get(2).isTrue());

    result = expression.evaluate("boo");
    assertFalse(result.get(0).isTrue());
    assertFalse(result.get(1).isTrue());
    assertFalse(result.get(2).isTrue());

    result = expression.evaluate("far");
    assertFalse(result.get(0).isTrue());
    assertFalse(result.get(1).isTrue());
    assertFalse(result.get(2).isTrue());

    result = expression.evaluate("baz");
    assertNull(result);
  }

  public void testNotAnd() {
    final StringMatchTruthFunction foo = new StringMatchTruthFunction("foo");
    final StringMatchTruthFunction bar = new StringMatchTruthFunction("bar");
    final StringMatchTruthFunction baz = new StringMatchTruthFunction("baz");
    final LogicalExpression<String> expression = new LogicalExpression<String>("(or 0 1 2)", new StringMatchTruthFunction[]{foo, bar, baz});

    List<LogicalResult<String>> result = null;

    result = expression.evaluate("foo");
    assertEquals(1, result.size());
    assertTrue(result.get(0).isTrue());
    assertEquals("foo", ((StringMatchTruthFunction)result.get(0).getTruthFunction()).getStringToMatch());

    result = expression.evaluate("bar");
    assertEquals(1, result.size());
    assertTrue(result.get(0).isTrue());
    assertEquals("bar", ((StringMatchTruthFunction)result.get(0).getTruthFunction()).getStringToMatch());

    result = expression.evaluate("baz");
    assertEquals(1, result.size());
    assertTrue(result.get(0).isTrue());
    assertEquals("baz", ((StringMatchTruthFunction)result.get(0).getTruthFunction()).getStringToMatch());

    result = expression.evaluate("foo bar baz");
    assertNull(result);
  }

  public void testXor() {
    final StringContainsTruthFunction foo = new StringContainsTruthFunction("foo");
    final StringContainsTruthFunction bar = new StringContainsTruthFunction("bar");
    final LogicalExpression<String> expression = new LogicalExpression<String>("(xor 0 1)", new StringContainsTruthFunction[]{foo, bar});

    List<LogicalResult<String>> result = null;

    result = expression.evaluate("foo");
    assertEquals(1, result.size());
    assertTrue(result.get(0).isTrue());

    result = expression.evaluate("bar");
    assertEquals(1, result.size());
    assertTrue(result.get(0).isTrue());

    result = expression.evaluate("foo other");
    assertEquals(1, result.size());
    assertTrue(result.get(0).isTrue());

    result = expression.evaluate("other foo");
    assertEquals(1, result.size());
    assertTrue(result.get(0).isTrue());

    result = expression.evaluate("foo bar");
    assertNull(result);

    result = expression.evaluate("bar foo");
    assertNull(result);
  }

  public void testNotXor() {
    final StringContainsTruthFunction foo = new StringContainsTruthFunction("foo");
    final StringContainsTruthFunction bar = new StringContainsTruthFunction("bar");
    final LogicalExpression<String> expression = new LogicalExpression<String>("(not (xor 0 1))", new StringContainsTruthFunction[]{foo, bar});

    List<LogicalResult<String>> result = null;

    result = expression.evaluate("foo");
    assertNull(result);

    result = expression.evaluate("bar");
    assertNull(result);

    result = expression.evaluate("foo other");
    assertNull(result);

    result = expression.evaluate("other foo");
    assertNull(result);

    result = expression.evaluate("foo bar");
    assertEquals(2, result.size());
    assertTrue(result.get(0).isTrue());
    assertTrue(result.get(1).isTrue());

    result = expression.evaluate("other1 other2");
    assertEquals(2, result.size());
    assertFalse(result.get(0).isTrue());
    assertFalse(result.get(1).isTrue());
  }


  /**
   * Truth function that evaluates to true equal strings.
   */
  private static final class StringMatchTruthFunction extends TruthFunction<String> {
    private String stringToMatch;

    StringMatchTruthFunction() {
      this("true");
    }

    StringMatchTruthFunction(String stringToMatch) {
      this.stringToMatch = stringToMatch;
    }

    public String getStringToMatch() {
      return stringToMatch;
    }

    public LogicalResult<String> evaluateInput(String input) {
      return new StringLogicalResult(input, stringToMatch.equals(input.toLowerCase()), this);
    }
  }

  /**
   * Truth function that evaluates to true with strings containing substrings.
   */
  private static final class StringContainsTruthFunction extends TruthFunction<String> {
    private String substringToMatch;

    StringContainsTruthFunction(String substringToMatch) {
      this.substringToMatch = substringToMatch;
    }

    public LogicalResult<String> evaluateInput(String input) {
      return new StringLogicalResult(input, input.toLowerCase().indexOf(substringToMatch) >= 0, this);
    }
  }


  public static Test suite() {
    TestSuite suite = new TestSuite(TestLogicalExpression.class);
    return suite;
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}
