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
 * JUnit Tests for the XmlTagRipper class.
 * <p>
 * @author Spence Koehler
 */
public class TestXmlTagRipper extends TestCase {

  public TestXmlTagRipper(String name) {
    super(name);
  }
  
  private static final String TEST1_XML = "resources/xml-text-ripper-test-data-1.xml";
  private static final String TEST2_XML = "resources/xml-tag-ripper-test-data-1.xml";
  private static final String TEST3_XML = "resources/xml-tag-ripper-test-data-2.xml";

  public void testNormalIteration() throws IOException {
    final String filename = FileUtil.getFilename(this.getClass(), TEST1_XML);
    final XmlTagRipper ripper = new XmlTagRipper(filename);

    final String[] expected = new String[] {
      "root", "a", "b", "c", "d", "e",
    };

    int index = 0;
    while (ripper.hasNext()) {
      final XmlLite.Tag tag = ripper.next();
      assertEquals(expected[index++], tag.name);
    }
    assertEquals(expected.length, index);
  }

  public void testNodeRipping() throws IOException {
    final String filename = FileUtil.getFilename(this.getClass(), TEST2_XML);
    final XmlTagRipper ripper = new XmlTagRipper(filename);

    final String expectedC = "<c><d><e>4</e></d></c>";

    // iterate up to the "C" node.
    while (ripper.hasNext()) {
      final XmlLite.Tag tag = ripper.next();
      if ("c".equals(tag.name)) break;
    }

    final Tree<XmlLite.Data> cNode = ripper.ripNode(XmlFactory.XML_LITE_IGNORE_COMMENTS);
    final String cString = XmlLite.asXml(cNode, false).replaceAll("\\s+", "");

    assertEquals(expectedC, cString);
    assertTrue(ripper.hasNext());

    final XmlLite.Tag fTag = ripper.next();
    assertEquals("f", fTag.name);
    assertFalse(ripper.hasNext());
  }

  public void testScriptIsTag() throws IOException {
    final String filename = FileUtil.getFilename(this.getClass(), TEST3_XML);
    final XmlTagRipper ripper = new XmlTagRipper(filename, true, null);

    final String expectedC = "<c><d><e>4</e></d></c>";

    boolean foundScriptAsTag = false;

    // iterate up to the "C" node.
    while (ripper.hasNext()) {
      final XmlLite.Tag tag = ripper.next();
      if ("script".equals(tag.name)) {
        foundScriptAsTag = true;
        break;
      }
    }

    assertTrue(foundScriptAsTag);
  }

  public static Test suite() {
    TestSuite suite = new TestSuite(TestXmlTagRipper.class);
    return suite;
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}
