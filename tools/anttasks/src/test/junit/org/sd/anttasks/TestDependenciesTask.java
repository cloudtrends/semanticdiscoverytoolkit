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
package org.sd.anttasks;


import java.util.LinkedHashSet;
import java.util.Set;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * JUnit Tests for the DependenciesTask class.
 * <p>
 * @author Spence Koehler
 */
public class TestDependenciesTask extends TestCase {

  public TestDependenciesTask(String name) {
    super(name);
  }
  

  public void testAsString() {
    final Set<String> set = new LinkedHashSet<String>();

    final String[] strings = new String[] {
      "a", "c", "b", "z", "e", "a", "c", "f",
    };
    final String expected = "a c b z e f";

    for (String string : strings) set.add(string);

    assertEquals(expected, DependenciesTask.asString(set));
  }

  public void testNullModulePattern() {
    final DependenciesTask.ModulePattern mp = new DependenciesTask.ModulePattern(null);
    assertEquals("foo", mp.apply("foo"));
  }

  public void testModulePattern() {
    final DependenciesTask.ModulePattern mp = new DependenciesTask.ModulePattern("%mxy%mz%mbam%");

    assertEquals("fooxyfoozfoobam%", mp.apply("foo"));
    assertEquals("%mxy%mz%mbam%", mp.apply("%m"));
  }


  public static Test suite() {
    TestSuite suite = new TestSuite(TestDependenciesTask.class);
    return suite;
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}
