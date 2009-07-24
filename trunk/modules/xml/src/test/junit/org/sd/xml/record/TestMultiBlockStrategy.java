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


/**
 * JUnit Tests for the HtmlRecordScraper class.
 * <p>
 * @author Spence Koehler
 */
public class TestMultiBlockStrategy extends TestCase {

  public TestMultiBlockStrategy(String name) {
    super(name);
  }
  

//  public void doScrapeTest(File file, 

  public void testFoo() throws IOException {
    final File file = FileUtil.getFile(this.getClass(), "resources/classifieds-001.html");

    final Record topRecord = Record.buildHtmlRecord(file);
    final List<Record> records = new MultiBlockStrategy().divide(topRecord);

    if (false) {
      int i = 0;
      for (Record record : records) {
        System.out.println("\n" + i + ": " + record);
        ++i;
      }
    }

    assertEquals(15, records.size());
  }


  public static Test suite() {
    TestSuite suite = new TestSuite(TestMultiBlockStrategy.class);
    return suite;
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}
