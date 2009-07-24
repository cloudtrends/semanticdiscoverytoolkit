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


import java.util.List;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * JUnit Tests for the Record class.
 * <p>
 * @author Spence Koehler
 */
public class TestRecord extends TestCase {

  public TestRecord(String name) {
    super(name);
  }
  

  public void testSimpleRecord() {
    final Record record =
      Record.buildRecord(
        "simple",
        new String[]{"foo", "bar", "baz"},
        new String[]{"abc", "def", "ghi"});

    assertEquals("simple", record.getName());
    assertEquals("abc", record.getValueString("foo"));
    assertEquals("def", record.getValueString("bar"));
    assertEquals("ghi", record.getValueString("baz"));
  }

  public void testSimpleSplit() {
    final Record record =
      Record.buildRecord(
        "simple",
        new String[]{"foo", "bar", "baz", "foo", "bar", "baz"},
        new String[]{"abc", "def", "ghi", "jkl", "mno", "pqr"});

    final List<Record> records = new SplitStrategy("fbz").divide(record);
    assertEquals(2, records.size());

    Record srec = records.get(0);
    assertEquals("fbz", srec.getName());
    assertEquals("abc", srec.getValueString("foo"));
    assertEquals("def", srec.getValueString("bar"));
    assertEquals("ghi", srec.getValueString("baz"));
    
    srec = records.get(1);
    assertEquals("fbz", srec.getName());
    assertEquals("jkl", srec.getValueString("foo"));
    assertEquals("mno", srec.getValueString("bar"));
    assertEquals("pqr", srec.getValueString("baz"));
  }


  public static Test suite() {
    TestSuite suite = new TestSuite(TestRecord.class);
    return suite;
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}
