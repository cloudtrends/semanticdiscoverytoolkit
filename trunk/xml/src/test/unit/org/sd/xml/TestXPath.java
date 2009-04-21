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

import org.sd.util.tree.Tree;

import java.io.IOException;
import java.util.List;

/**
 * JUnit Tests for the XPath class.
 * <p>
 * @author Spence Koehler
 */
public class TestXPath extends TestCase {

  public TestXPath(String name) {
    super(name);
  }
  
  private final void doTextTest(String xmlString, XPath xPath, String[] expected) throws IOException {
    final Tree<XmlLite.Data> xmlTree = XmlFactory.buildXmlTree(xmlString, true, false);
    final List<String> texts = xPath.getText(xmlTree, true);

    if (expected == null) {
      assertNull(texts);
    }
    else {
      assertNotNull(texts);
      assertEquals(expected.length, texts.size());
      for (int i = 0; i < expected.length; ++i) {
        assertEquals(expected[i], texts.get(i));
      }
    }
  }

  private final void doAttributesTest(String xmlString, XPath xPath, String attribute, String[] expected) throws IOException {
    final Tree<XmlLite.Data> xmlTree = XmlFactory.buildXmlTree(xmlString, true, false);
    final List<String> texts = xPath.getText(xmlTree, attribute, false);

    if (expected == null) {
      assertNull(texts);
    }
    else {
      assertNotNull(texts);
      assertEquals(expected.length, texts.size());
      for (int i = 0; i < expected.length; ++i) {
        assertEquals(expected[i], texts.get(i));
      }
    }
  }

  public void testSimple() throws IOException {
    final XPath xpath = new XPath("a.b.c");

    doTextTest("<a><b><c foo=\"bar\">xxx</c></b></a>", xpath, new String[]{"xxx"});
    doAttributesTest("<a><b><c foo=\"bar\">xxx</c></b></a>", xpath, "foo", new String[]{"bar"});

    doTextTest("<a><b><d foo=\"bar\">xxx</d></b></a>", xpath, null);
    doAttributesTest("<a><b><d foo=\"bar\">xxx</d></b></a>", xpath, "foo", null);
    doAttributesTest("<a><b><c bar=\"baz\">xxx</d></b></a>", xpath, "foo", null);
    doAttributesTest("<a><b><c foo=\"\">xxx</d></b></a>", xpath, "foo", new String[0]);

    doTextTest("<a><b><c foo=\"bar\">xxx</c></b><b><c foo=\"baz\">yyy</c></b></a>", xpath, new String[]{"xxx", "yyy"});
    doAttributesTest("<a><b><c foo=\"bar\">xxx</c></b><b><c foo=\"baz\">yyy</c></b></a>", xpath, "foo", new String[]{"bar", "baz"});

    doTextTest("<a><b><c foo=\"bar\">xxx</c></b><d/><b><c foo=\"baz\">yyy</c></b></a>", xpath, new String[]{"xxx", "yyy"});
    doAttributesTest("<a><b><c foo=\"bar\">xxx</c></b><d/><b><c foo=\"baz\">yyy</c></b></a>", xpath, "foo", new String[]{"bar", "baz"});

    doTextTest("<a><b><c foo=\"bar\">xxx</c></b><d><b><c foo=\"aaa\">zzz</c></b></d><b><c foo=\"baz\">yyy</c></b><e/></a>", xpath, new String[]{"xxx", "yyy"});
    doAttributesTest("<a><b><c foo=\"bar\">xxx</c></b><d><b><c foo=\"aaa\">zzz</c></b></d><b><c foo=\"baz\">yyy</c></b><e/></a>", xpath, "foo", new String[]{"bar", "baz"});

    doTextTest("<c><b><a/></b></c>", xpath, null);
    doAttributesTest("<c><b><a/></b></c>", xpath, "foo", null);
  }

  public void testSubscripts() throws IOException {
    final XPath xpath = new XPath("a.b[1,3,5].c[0,2,4]");

    doTextTest("<a><b><c i=\"0\">000</c></b><b><c i=\"1\">111</c></b><b><c i=\"2\">222</c></b></a>", xpath, new String[]{"111"});
    doTextTest("<a><b><c i=\"0\">000</c></b><b><d/><c i=\"1\">111</c><f/></b><b><c i=\"2\">222</c></b></a>", xpath, null);
    doTextTest("<a><b><c i=\"0\">000</c></b><b><c i=\"1\">111</c></d><c i=\"1a\">aaa</c></b><b><c i=\"2\">222</c></b></a>", xpath, new String[]{"111", "aaa"});

    doAttributesTest("<a><b><c i=\"0\">000</c></b><b><c i=\"1\">111</c></b><b><c i=\"2\">222</c></b></a>", xpath, "i", new String[]{"1"});
    doAttributesTest("<a><b><c i=\"0\">000</c></b><b><d/><c i=\"1\">111</c><f/></b><b><c i=\"2\">222</c></b></a>", xpath, "i", null);
    doAttributesTest("<a><b><c i=\"0\">000</c></b><b><c i=\"1\">111</c></d><c i=\"1a\">aaa</c></b><b><c i=\"2\">222</c></b></a>", xpath, "i", new String[]{"1", "1a"});
  }

  public void testSkipLevels1() throws IOException {
    final XPath xpath = new XPath("**.b.c");

    doTextTest("<a><b><c foo=\"bar\">xxx</c></b></a>", xpath, new String[]{"xxx"});
    doTextTest("<b><c foo=\"bar\">xxx</c></b>", xpath, new String[]{"xxx"});
    doTextTest("<x><b><c foo=\"bar\">xxx</c></b></x>", xpath, new String[]{"xxx"});
    doTextTest("<x><y><b><c foo=\"bar\">xxx</c></b></y></x>", xpath, new String[]{"xxx"});
    doTextTest("<x><y><z><b><c foo=\"bar\">xxx</c></b></y></z></x>", xpath, new String[]{"xxx"});
    doTextTest("<x><y><b><c foo=\"bar\">xxx</c></b><z><b><c foo=\"baz\">yyy</c></b></z></y></x>", xpath, new String[]{"xxx", "yyy"});

    doAttributesTest("<a><b><c foo=\"bar\">xxx</c></b></a>", xpath, "foo", new String[]{"bar"});
    doAttributesTest("<b><c foo=\"bar\">xxx</c></b>", xpath, "foo", new String[]{"bar"});
    doAttributesTest("<x><b><c foo=\"bar\">xxx</c></b></x>", xpath, "foo", new String[]{"bar"});
    doAttributesTest("<x><y><b><c foo=\"bar\">xxx</c></b></y></x>", xpath, "foo", new String[]{"bar"});
    doAttributesTest("<x><y><z><b><c foo=\"bar\">xxx</c></b></y></z></x>", xpath, "foo", new String[]{"bar"});
    doAttributesTest("<x><y><b><c foo=\"bar\">xxx</c></b><z><b><c foo=\"baz\">yyy</c></b></z></y></x>", xpath, "foo", new String[]{"bar", "baz"});
  }

  public void testSkipLevels2() throws IOException {
    final XPath xpath = new XPath("a.**.c");

    doTextTest("<a><b><c foo=\"bar\">xxx</c></b></a>", xpath, new String[]{"xxx"});
    doTextTest("<a><c foo=\"bar\">xxx</c></a>", xpath, new String[]{"xxx"});
    doTextTest("<a><x><c foo=\"bar\">xxx</c></x></a>", xpath, new String[]{"xxx"});
    doTextTest("<a><x><y><c foo=\"bar\">xxx</c></y></x></a>", xpath, new String[]{"xxx"});
    doTextTest("<a><x><y><z><c foo=\"bar\">xxx</c></z></y></x></a>", xpath, new String[]{"xxx"});
    doTextTest("<a><c foo=\"bar\">aaa</c><y><z><c foo=\"baz\">xxx</c></z></y></a>", xpath, new String[]{"aaa", "xxx"});

    doAttributesTest("<a><b><c foo=\"bar\">xxx</c></b></a>", xpath, "foo", new String[]{"bar"});
    doAttributesTest("<a><c foo=\"bar\">xxx</c></a>", xpath, "foo", new String[]{"bar"});
    doAttributesTest("<a><x><c foo=\"bar\">xxx</c></x></a>", xpath, "foo", new String[]{"bar"});
    doAttributesTest("<a><x><y><c foo=\"bar\">xxx</c></y></x></a>", xpath, "foo", new String[]{"bar"});
    doAttributesTest("<a><x><y><z><c foo=\"bar\">xxx</c></z></y></x></a>", xpath, "foo", new String[]{"bar"});
    doAttributesTest("<a><c foo=\"bar\">aaa</c><y><z><c foo=\"baz\">xxx</c></z></y></a>", xpath, "foo", new String[]{"bar", "baz"});
  }

  public void testSkipLevels3() throws IOException {
    final XPath xpath = new XPath("a.b.**");

    doTextTest("<a><b><c foo=\"bar\">xxx</c></b></a>", xpath, new String[]{"xxx"});
    doTextTest("<a><b foo=\"bar\">xxx</b></a>", xpath, new String[]{"xxx"});
    doTextTest("<a><b><c foo=\"bar\">ccc</c></b><b><f foo=\"baz\">fff</f><g foo=\"bam\">ggg</g></b><h><i><j><b><k foo=\"baa\">kkk</k></b></j></i></h></a>", xpath, new String[]{"ccc", "fff", "ggg"});
    doTextTest("<a><b><c>big</c>dog<d>cat</d>mouse</b></a>", xpath, new String[]{"big", "dog", "cat", "mouse"});

    doAttributesTest("<a><b><c foo=\"bar\">xxx</c></b></a>", xpath, "foo", new String[]{"bar"});
    doAttributesTest("<a><b foo=\"bar\">xxx</b></a>", xpath, "foo", null);
    doAttributesTest("<a><b><c foo=\"bar\">ccc</c></b><b><f foo=\"baz\">fff</f><g foo=\"bam\">ggg</g></b><h><i><j><b><k foo=\"baa\">kkk</k></b></j></i></h></a>", xpath, "foo", new String[]{"bar", "baz", "bam"});
  }

  public void testSkipLevels4() throws IOException {
    final XPath xpath = new XPath("a.**.b.**");

    doTextTest("<a><b><c foo=\"bar\">xxx</c></b></a>", xpath, new String[]{"xxx"});
    doTextTest("<a><b foo=\"bar\">xxx</b></a>", xpath, new String[]{"xxx"});
    doTextTest("<a><b><c foo=\"bar\">ccc</c></b><b><f foo=\"baz\">fff</f><g foo=\"bam\">ggg</g></b><h><i><j><b><k foo=\"baa\">kkk</k></b></j></i></h></a>", xpath, new String[]{"ccc", "fff", "ggg", "kkk"});

    doAttributesTest("<a><b><c foo=\"bar\">xxx</c></b></a>", xpath, "foo", new String[]{"bar"});
    doAttributesTest("<a><b foo=\"bar\">xxx</b></a>", xpath, "foo", null);
    doAttributesTest("<a><b><c foo=\"bar\">ccc</c></b><b><f foo=\"baz\">fff</f><g foo=\"bam\">ggg</g></b><h><i><j><b><k foo=\"baa\">kkk</k></b></j></i></h></a>", xpath, "foo", new String[]{"bar", "baz", "bam", "baa"});
  }

  public void testConcatenateTexts() throws IOException {
    final XPath xpath = new XPath("a.b");
    doTextTest("<a><b><c>big</c>dog<d>cat</d>mouse</b></a>", xpath, new String[]{"big dog cat mouse"});
  }

  public void testFindOrCreateNodes() throws IOException {
    final XPath xpath = new XPath("a.b.c");
    final Tree<XmlLite.Data> xmlTree = XmlFactory.buildXmlTree("<a/>", true, false);

    final List<Tree<XmlLite.Data>> nodes = xpath.findOrCreateNodes(xmlTree);
    assertEquals(1, nodes.size());

    final Tree<XmlLite.Data> c = nodes.get(0);
    assertEquals("c", c.getData().asTag().name);

    final Tree<XmlLite.Data> b = c.getParent();
    assertEquals("b", b.getData().asTag().name);

    final Tree<XmlLite.Data> a = b.getParent();
    assertEquals(xmlTree, a);
  }

  public static Test suite() {
    TestSuite suite = new TestSuite(TestXPath.class);
    return suite;
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}
