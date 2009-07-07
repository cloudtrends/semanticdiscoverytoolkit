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
package org.sd.util;


import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.sd.io.FileUtil;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

/**
 * JUnit Tests for the PropertiesParser class.
 * <p>
 * @author Spence Koehler
 */
public class TestPropertiesParser extends TestCase {

  public TestPropertiesParser(String name) {
    super(name);
  }
  
  public void testGetInt() {
    final Properties properties = new Properties();

    properties.setProperty("foo", "3");
    
    assertEquals(new Integer(3), PropertiesParser.getInt(properties, "foo", "2"));
    assertEquals(new Integer(2), PropertiesParser.getInt(properties, "bar", "2"));
  }

  public void testGetBoolean() {
    final Properties properties = new Properties();

    properties.setProperty("foo", "TRUE");
    properties.setProperty("bar", "False");
    
    assertEquals(new Boolean(true), PropertiesParser.getBoolean(properties, "foo", "false"));
    assertEquals(new Boolean(false), PropertiesParser.getBoolean(properties, "bar", "false"));
    assertEquals(new Boolean(false), PropertiesParser.getBoolean(properties, "baz", "false"));
  }

  public void testGetDouble() {
    final Properties properties = new Properties();

    properties.setProperty("foo", "3.14159265");
    
    assertEquals(3.14159265, PropertiesParser.getDouble(properties, "foo", "2.99792458"));
    assertEquals(2.99792458, PropertiesParser.getDouble(properties, "bar", "2.99792458"));
  }

  public void testGetStrings() {
    final Properties properties = new Properties();

    properties.setProperty("foo", "a, b, c");
    final String[] gotValues = PropertiesParser.getStrings(properties, "foo");

    assertEquals("a", gotValues[0]);
    assertEquals("b", gotValues[1]);
    assertEquals("c", gotValues[2]);
  }

  public void testGetMultiValues() {
    final Properties properties = new Properties();

    final String[] values = new String[] {
      "value 0",
      "this is value 1",
      "and Value 2",
    };

    for (int i = 0; i < values.length; ++i) {
      properties.setProperty("property_" + i, values[i]);
    }

    final String[] gotValues = PropertiesParser.getMultiValues(properties, "property_");

    assertEquals(values.length, gotValues.length);

    for (int i = 0; i < values.length; ++i) {
      assertEquals(values[i], gotValues[i]);
    }
  }

  public void testGetProperties() {
    final PropertiesParser pp = new PropertiesParser();

    final String[] values = new String[] {
      "value 0",
      "this is value 1",
      "and Value 2",
    };

    for (int i = 0; i < values.length; ++i) {
      pp.setProperty("property_" + i, values[i]);
    }

    final String[] gotValues = pp.getProperties("property_");

    assertEquals(values.length, gotValues.length);

    for (int i = 0; i < values.length; ++i) {
      assertEquals(values[i], gotValues[i]);
    }
  }

  public void testProperShadowing() throws IOException {
    final File basePath = FileUtil.getFile(this.getClass(), "resources/testPropertiesParser_shadow2.properties").getParentFile();

    final PropertiesParser pp = new PropertiesParser(new String[] {
      "foo=3",
      new File(basePath, "testPropertiesParser_shadow2.properties").getAbsolutePath(),
      new File(basePath, "testPropertiesParser_shadow1.default.properties").getAbsolutePath(),
    });


    assertEquals("1", pp.getProperty("foo"));
  }

  public void testFindProperty() {
    final Properties properties = new Properties();

    properties.setProperty("foo", "a");
    properties.setProperty("bar", "a");
    properties.setProperty("baz", "a");

    assertNotNull(PropertiesParser.findProperty(properties, new String[]{"foo", "bar", "baz"}));
    assertNull(PropertiesParser.findProperty(properties, new String[]{"boo", "far", "faz"}));
  }

  public void testGetMissingProperties1() {
    final Properties properties = new Properties();

    properties.setProperty("foo", "a");
    properties.setProperty("bar", "a");
    properties.setProperty("baz", "a");

    assertEquals("far, faz", PropertiesParser.getMissingProperties(properties, new String[]{"foo", "far", "faz"}));
  }

  public void testGetMissingProperties2() {
    final Properties properties = new Properties();

    properties.setProperty("foo", "a");
    properties.setProperty("bar", "a");
    properties.setProperty("baz", "a");

    assertEquals("far, faz",
                 PropertiesParser.getMissingProperties(properties, new String[][] {
                   {"foo", "far", "faz"},
                   {"aaa", "bbb", "ccc"},
                 }));

    assertEquals("boo, far, faz or aaa, bbb, ccc",
                 PropertiesParser.getMissingProperties(properties, new String[][] {
                   {"boo", "far", "faz"},
                   {"aaa", "bbb", "ccc"},
                 }));
  }

  public static Test suite() {
    TestSuite suite = new TestSuite(TestPropertiesParser.class);
    return suite;
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}
