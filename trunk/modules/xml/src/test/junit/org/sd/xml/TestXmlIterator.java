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
 * JUnit Tests for the XmlIterator class.
 * <p>
 * @author Spence Koehler
 */
public class TestXmlIterator extends TestCase {

  public TestXmlIterator(String name) {
    super(name);
  }
  
  private static final String TEST1_XML = "resources/xml-iterator-test-data-1.xml";

  public void test1() throws IOException {
    final String filename = FileUtil.getFilename(this.getClass(), TEST1_XML);
    final XmlIterator xmlIter = new XmlIterator(filename);

    final Tree<XmlLite.Data> top = xmlIter.getTop();
    final XmlLite.Tag topTag = top.getData().asTag();

    assertEquals("RDF", topTag.name);

    final String[] childTags = new String[]{"d:Title", "d:Creator", "d:Subject",
                                            "d:Description", "d:Publisher", "d:Date",
                                            "d:Identifier", "d:Language", "d:Rights", 
                                            "d:Rights", "Restaurant", "DinerReview",
                                            "DinerReview", "Restaurant", "DinerReview",
                                            "Restaurant", "Restaurant", "DinerReview",
                                            "DinerReview", "DinerReview", "Restaurant",
                                            "DinerReview", "DinerReview"};

    int index = 0;
    while (xmlIter.hasNext()) {
      final Tree<XmlLite.Data> childNode = xmlIter.next();
      final String expectedChildTag = childTags[index];

      assertEquals(expectedChildTag, childNode.getData().asTag().name);

      ++index;
    }
  }

  public static Test suite() {
    TestSuite suite = new TestSuite(TestXmlIterator.class);
    return suite;
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}
