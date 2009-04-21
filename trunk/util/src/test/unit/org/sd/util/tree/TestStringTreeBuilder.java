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
package org.sd.util.tree;


import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * JUnit Tests for the StringTreeBuilder class.
 * <p>
 * @author Spence Koehler
 */
public class TestStringTreeBuilder extends TestCase {

  private TreeBuilder<String> treeBuilder = TreeBuilderFactory.getStringTreeBuilder();

  public TestStringTreeBuilder(String name) {
    super(name);
  }
  
  public void testNullBuild1() {
    Tree<String> tree = treeBuilder.buildTree(null);
    assertNull(tree);
  }

  public void testNullBuild2() {
    String string = treeBuilder.buildString(null);
    assertNull(string);
  }

  public void testBadBuild1() {
    Tree<String> tree = treeBuilder.buildTree("(a (b c)");
    assertNull(tree);
  }

  public void testShallowBuild1() {
    Tree<String> tree = treeBuilder.buildTree("(a)");
    assertNotNull(tree);
    assertEquals("a", tree.getData());
    assertNull(tree.getChildren());
  }

  public void testReversibility1() {
    Tree<String> tree = treeBuilder.buildTree("(a (b c) (d (e f)))");
    Tree<String> tree1 = treeBuilder.buildTree(treeBuilder.buildString(tree));

    assertEquals(tree, tree1);
  }

  public void testReversibility2() {
    Tree<String> tree = treeBuilder.buildTree("(a (b \"c\") (d (e \"f\")))");
    Tree<String> tree1 = treeBuilder.buildTree(treeBuilder.buildString(tree));

    assertEquals(tree, tree1);
  }

  public void testReversibility3() {
    Tree<String> tree = treeBuilder.buildTree("(a (b \"&lp;g&sbl;h&sbr; i&lp;\") (d (e \"f\")))");
    Tree<String> tree1 = treeBuilder.buildTree(treeBuilder.buildString(tree));

    assertEquals(tree, tree1);
  }

  public void testReversibility4() {
    Tree<String> tree = treeBuilder.buildTree("(a[1=2] (b[3=4;5,6=7] \"c\") (d (e \"f\")))");
    Tree<String> tree1 = treeBuilder.buildTree(treeBuilder.buildString(tree));

    assertEquals(tree, tree1);
  }

  public static Test suite() {
    TestSuite suite = new TestSuite(TestStringTreeBuilder.class);
    return suite;
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}
