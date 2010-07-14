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
 * JUnit Tests for the XmlNodeIterator class.
 * <p>
 * @author Spence Koehler
 */
public class TestXmlNodeIterator extends TestCase {

  public TestXmlNodeIterator(String name) {
    super(name);
  }
  
  public void test1() throws IOException {
    final File file = FileUtil.getFile(this.getClass(), "resources/xml-node-iterator-test-data-1.xml");
    final XmlNodeIterator iter = new XmlNodeIterator(file, false, null);
    final String[] path = new String[]{"a", "b", "c", "Foo"};
    Tree<XmlLite.Data> node = null;
    Tree<XmlLite.Data> recordNode = null;

    assertTrue(iter.hasNext());
    node = iter.next();
    assertEquals("x", XmlTreeHelper.getAllText(node));
    assertFalse(iter.hasPath(path));
    assertNull(iter.getNode(path));

    assertTrue(iter.hasNext());
    node = iter.next();
    assertEquals("y", XmlTreeHelper.getAllText(node));
    assertFalse(iter.hasPath(path));
    assertNull(iter.getNode(path));

    assertTrue(iter.hasNext());
    node = iter.next();
    assertEquals("z", XmlTreeHelper.getAllText(node));
    assertFalse(iter.hasPath(path));
    assertNull(iter.getNode(path));

    assertTrue(iter.hasNext());
    node = iter.next();
    assertEquals("Foo-1", XmlTreeHelper.getAllText(node));
    assertTrue(iter.hasPath(path));
    recordNode = iter.getNode(path);
    assertEquals("Foo", recordNode.getData().asTag().name);
    assertEquals(3, recordNode.getChildren().size());
    assertEquals("Foo-1 Bar-1 Baz-1", XmlTreeHelper.getAllText(recordNode));

    assertTrue(iter.hasNext());
    node = iter.next();
    assertEquals("Foo-2", XmlTreeHelper.getAllText(node));
    assertTrue(iter.hasPath(path));
    recordNode = iter.getNode(path);
    assertEquals("Foo", recordNode.getData().asTag().name);
    assertEquals(3, recordNode.getChildren().size());
    assertEquals("Foo-2 Bar-2 Baz-2", XmlTreeHelper.getAllText(recordNode));

    assertFalse(iter.hasNext());

    iter.close();
  }

  public static Test suite() {
    TestSuite suite = new TestSuite(TestXmlNodeIterator.class);
    return suite;
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}
