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
package org.sd.util.cmd;


import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * JUnit Tests for the AbstractExecutor class.
 * <p>
 * @author Spence Koehler
 */
public class TestAbstractExecutor extends TestCase {

  public TestAbstractExecutor(String name) {
    super(name);
  }
  
  public void testDoVariableSubstitution() {
    final CommandInterpreter interpreter = new CommandInterpreter();
    interpreter.setVar("a", "foo");
    interpreter.setVar("b", "bar");

    assertEquals("foo", AbstractExecutor.doVariableSubstitutions(interpreter, "$a"));
    assertEquals("afoo", AbstractExecutor.doVariableSubstitutions(interpreter, "a$a"));
    assertEquals("afoo(a)", AbstractExecutor.doVariableSubstitutions(interpreter, "a$a(a)"));
    assertEquals("foobar", AbstractExecutor.doVariableSubstitutions(interpreter, "$a$b"));

    assertEquals("$a", AbstractExecutor.doVariableSubstitutions(interpreter, "$$a"));
    assertEquals("$a", AbstractExecutor.doVariableSubstitutions(interpreter, "\\$a"));
  }

  private final void verify(String[] expected, String[] got) {
    assertEquals(expected.length, got.length);
    for (int i = 0 ; i < expected.length; ++i) {
      assertEquals(i + ": expected='" + expected[i] + "' got='" + got[i] + "'",
                   expected[i], got[i]);
    }
  }

  public void testParseArgsString() {
    final CommandInterpreter interpreter = new CommandInterpreter();
    interpreter.setVar("a", "foo");
    interpreter.setVar("b", "bar");

    verify(new String[]{}, AbstractExecutor.parseArgsString(interpreter, ""));
    verify(new String[]{"foo", "bar"}, AbstractExecutor.parseArgsString(interpreter, "$a $b"));
    verify(new String[]{"-x", "foo \\' bar"}, AbstractExecutor.parseArgsString(interpreter, "-x '$a \\\' $b'"));
    verify(new String[]{"-x", "foo \\\" bar"}, AbstractExecutor.parseArgsString(interpreter, "-x \"$a \\\" $b\""));
    verify(new String[]{"-x", "(foo () bar)"}, AbstractExecutor.parseArgsString(interpreter, "-x ($a () $b)"));
  }

  public static Test suite() {
    TestSuite suite = new TestSuite(TestAbstractExecutor.class);
    return suite;
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}
