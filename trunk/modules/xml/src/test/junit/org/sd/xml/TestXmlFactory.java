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

import java.io.File;
import java.io.IOException;

/**
 * JUnit Tests for the XmlFactory class.
 * <p>
 * @author Spence Koehler
 */
public class TestXmlFactory extends TestCase {

  public TestXmlFactory(String name) {
    super(name);
  }
                                                       
  private static final String TEST1_XML = "resources/testXmlFactory-big-file.xml";
  private static final String TEST2_XML = "resources/wierd.xml";

  public void testTimeLimit_do_timeout() {
    // read a huge file in a very short time limit to ensure we timeout.
    final File file = FileUtil.getFile(this.getClass(), TEST1_XML);
    Tree<XmlLite.Data> xmlTree = XmlFactory.readXmlTree(file, true, false, 1, true);
    assertNull(xmlTree);
  }

  public void testTimeLimit_dont_timeout() {
    // read a tiny file with a very high time limit to ensure we don't timeout.
    final File file = FileUtil.getFile(this.getClass(), TEST2_XML);
    Tree<XmlLite.Data> xmlTree = XmlFactory.readXmlTree(file, true, false, 10000, true);
    assertNotNull(xmlTree);
  }

  /**
   * Test parsing a string with html that is missing a top tag to hold it all together.
   */
  public void testBuildXmlTreeWithoutTopTag() throws IOException {
    final String xmlString = "<a href=\"foo.com\">foo</a> <a href=\"bar.com\">bar</a>";
    final Tree<XmlLite.Data> xmlTree = XmlFactory.buildXmlTree(xmlString, true, true);
    final String data = XmlTreeHelper.getAllText(xmlTree);
    assertEquals("foo bar", data);
  }

  /**
   * Test stripping html formatting from concatenated html strings.
   */
  public void testStripHtmlFormatting() {
    assertEquals("foo bar", XmlFactory.stripHtmlFormatting("<a href=\"foo.com\">foo</a> <a href=\"bar.com\">bar</a>"));
    assertEquals("foo bar", XmlFactory.stripHtmlFormatting("foo bar"));
  }

  public static Test suite() {
    TestSuite suite = new TestSuite(TestXmlFactory.class);
    return suite;
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}
