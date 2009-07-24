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
package org.sd.xml.record;


import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.sd.io.FileUtil;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Properties;
import java.util.Set;

/**
 * JUnit Tests for the Settings class.
 * <p>
 * @author Spence Koehler
 */
public class TestSettings extends TestCase {

  public TestSettings(String name) {
    super(name);
  }
  
  public void testBasics() throws IOException {
    final File settingsFile = FileUtil.getFile(this.getClass(), "resources/basic-settings-test.xml");
    final Properties properties = new Properties();
    properties.setProperty("baz", "Bar");
    properties.setProperty("abc", "ABC");
    properties.setProperty("xyz3", "XYZ-3");
    properties.setProperty("attr2-1", "ATTR2-1");
    properties.setProperty("attr2-2", "ATTR2-2");

    final Settings settings = new Settings(properties, settingsFile);

    final Record settingsRecord = settings.getSettingsRecord("SettingsA");
    assertEquals("Foo", settings.getSettingsValue(settingsRecord, "foo"));
    assertEquals("Bar", settings.getSettingsValue(settingsRecord, "bar"));
    assertEquals("ABC", settings.getSettingsValue(settingsRecord, "abc"));

    final Set<String> values = settings.getSettingsValues(settingsRecord, "xyz");
    assertEquals(4, values.size());
    for (int i = 1; i <= 4; ++i) {
      assertTrue(values.contains("XYZ-" + i));
    }

    final List<Record> subSettings = settings.getAllSettingRecords("sub", settingsRecord);
    assertEquals(2, subSettings.size());

    final Record sub1 = subSettings.get(0);
    assertEquals("ATTR1-1", settings.getSettingsValue(sub1, "attr1"));
    assertEquals("ATTR2-1", settings.getSettingsValue(sub1, "attr2"));

    final Record sub2 = subSettings.get(1);
    assertEquals("ATTR1-2", settings.getSettingsValue(sub2, "attr1"));
    assertEquals("ATTR2-2", settings.getSettingsValue(sub2, "attr2"));
  }

  public static Test suite() {
    TestSuite suite = new TestSuite(TestSettings.class);
    return suite;
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}
