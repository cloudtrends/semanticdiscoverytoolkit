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
package org.sd.xml;


import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * JUnit Tests for the DataProperties class.
 * <p>
 * @author Spence Koehler
 */
public class TestDataProperties extends TestCase {

  public TestDataProperties(String name) {
    super(name);
  }
  

  public void testReplaceVariables_oneLevel() {
    final DataProperties dp = new DataProperties();

    dp.set("HOME", "/abc");
    dp.set("path", "${HOME}/path");

    final String path = dp.getString("path");
    final String value = dp.replaceVariables(path);

    assertEquals("/abc/path", value);
  }

  public void testReplaceVariables_twoLevels() {
    final DataProperties dp = new DataProperties();

    dp.set("HOME", "/abc/${USER}/def");
    dp.set("USER", "ghi");
    dp.set("path", "${HOME}/path");

    final String path = dp.getString("path");
    final String value = dp.replaceVariables(path);

    assertEquals("/abc/ghi/def/path", value);
  }


  public static Test suite() {
    TestSuite suite = new TestSuite(TestDataProperties.class);
    return suite;
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}
