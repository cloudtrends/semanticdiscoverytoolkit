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

import org.sd.io.FileUtil;
import org.sd.util.tree.Tree;

import java.io.IOException;

/**
 * JUnit Tests for the XmlNodeRipper class.
 * <p>
 * @author Spence Koehler
 */
public class TestXmlNodeRipper extends TestCase {

  public TestXmlNodeRipper(String name) {
    super(name);
  }
  
  private static final String TEST1_XML = "resources/xml-node-ripper-test-data-1.xml";
  private static final String TEST2_XML = "resources/xml-node-ripper-test-data-2.xml";
  private static final String TEST3_XML = "resources/xml-node-ripper-test-data-3.xml";

  public void test1() throws IOException {
    final String filename = FileUtil.getFilename(this.getClass(), TEST1_XML);
    final XmlNodeRipper ripper = new XmlNodeRipper(filename, false, null);

    final String[] expected = new String[] {
      "1 2", "3", "4",
    };

    try {
      int index = 0;
      while (ripper.hasNext()) {
        final Tree<XmlLite.Data> node = ripper.next();
        assertEquals(expected[index++], XmlTreeHelper.getAllText(node));
      }
    }
    finally {
      ripper.close();
    }
  }

  public void test2() throws IOException {
    final String filename = FileUtil.getFilename(this.getClass(), TEST2_XML);
    final XmlNodeRipper ripper = new XmlNodeRipper(filename, false, null);

    final String[] expected = new String[] {
      "Foo Bar Baz",
    };

    try {
      int index = 0;
      while (ripper.hasNext()) {
        final Tree<XmlLite.Data> node = ripper.next();
        assertEquals(expected[index++], XmlTreeHelper.getAllText(node));
      }
    }
    finally {
      ripper.close();
    }
  }

  public void test3() throws IOException {
    final String filename = FileUtil.getFilename(this.getClass(), TEST3_XML);
    final XmlNodeRipper ripper = new XmlNodeRipper(filename, false, null);

    final String[] expected = new String[] {
      "Foo", "Bar", "", "Baz",
    };

    try {
      int index = 0;
      while (ripper.hasNext()) {
        final Tree<XmlLite.Data> node = ripper.next();
        assertEquals(expected[index++], XmlTreeHelper.getAllText(node));
      }
    }
    finally {
      ripper.close();
    }
  }

  public static Test suite() {
    TestSuite suite = new TestSuite(TestXmlNodeRipper.class);
    return suite;
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}
