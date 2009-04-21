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


import org.sd.io.FileUtil;

import java.io.IOException;

/**
 * JUnit Tests for the MultiTermFinder class.
 * <p>
 * @author Spence Koehler
 */
public class TestMultiTermFinder extends TestCase {

  public TestMultiTermFinder(String name) {
    super(name);
  }
  
  public void testLoadFromFile() throws IOException {
    final MultiTermFinder mtf = MultiTermFinder.loadFromFile(FileUtil.getFile(this.getClass(), "resources/test-mtf-load-1.txt"));

    final String[] expressions = mtf.getExpressions();
    assertEquals(2, expressions.length);

    assertNotNull(mtf.evaluateLogicalExpression(expressions[0], "foo bar baz"));
    assertNull(mtf.evaluateLogicalExpression(expressions[0], "foo oof"));
    assertNull(mtf.evaluateLogicalExpression(expressions[0], "Foo Bar Baz"));

    assertNotNull(mtf.evaluateLogicalExpression(expressions[1], "bar rab"));
    assertNotNull(mtf.evaluateLogicalExpression(expressions[1], "foo RAB"));
    assertNull(mtf.evaluateLogicalExpression(expressions[1], "foo bar"));
  }

  public static Test suite() {
    TestSuite suite = new TestSuite(TestMultiTermFinder.class);
    return suite;
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}
