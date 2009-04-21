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

import java.util.List;

/**
 * JUnit Tests for the NodePath class.
 * <p>
 * @author Spence Koehler
 */
public class TestNodePath extends TestCase {

  private TreeBuilder<String> treeBuilder = TreeBuilderFactory.getStringTreeBuilder();

  public TestNodePath(String name) {
    super(name);
  }
  
  private final void doTest(Tree<String> tree, NodePath<String> nodePath, String[] expected) {
    final List<Tree<String>> paths = nodePath.apply(tree);
    if (expected == null) {
      assertNull("expected null, but got paths=" + paths, paths);
    }
    else {
      assertNotNull("got no paths for tree=" + tree, paths);
      assertEquals("expected " + expected.length + " got " + paths.size() + " " + paths,
                   expected.length, paths.size());
      for (int i = 0; i < expected.length; ++i) {
        final String data = paths.get(i).getData();
        assertEquals(expected[i], data);
      }
    }
  }

  public void testSimple() {
    final NodePath<String> nodePath = new NodePath<String>();
    nodePath.add("a", null);
    nodePath.add("b", null);
    nodePath.add("c", null);

    doTest(treeBuilder.buildTree("(a (b c))"), nodePath, new String[]{"c"});
    doTest(treeBuilder.buildTree("(a (b d))"), nodePath, null);
    doTest(treeBuilder.buildTree("(a (b c) (b c))"), nodePath, new String[]{"c", "c"});
    doTest(treeBuilder.buildTree("(a (b c) d (b c))"), nodePath, new String[]{"c", "c"});
    doTest(treeBuilder.buildTree("(a (b c) (d (e f)) (b c) e)"), nodePath, new String[]{"c", "c"});
    doTest(treeBuilder.buildTree("(a (b d) (d e f) (b c))"), nodePath, new String[]{"c"});
    doTest(treeBuilder.buildTree("(c (b a))"), nodePath, null);
  }

  public void testSubscripts() {
    final NodePath<String> nodePath = new NodePath<String>();
    nodePath.add("a", null);
    nodePath.add("b", new int[]{1, 3, 5});
    nodePath.add("c", new int[]{0, 2, 4});

    doTest(treeBuilder.buildTree("(a (b c) (b c) (b c))"), nodePath, new String[]{"c"});
    doTest(treeBuilder.buildTree("(a (b c) (b d c f) (b c))"), nodePath, null);
    doTest(treeBuilder.buildTree("(a (b c) (b c d c) (b c))"), nodePath, new String[]{"c", "c"});
  }

  public void testSkipLevels1() {
    final NodePath<String> nodePath = new NodePath<String>();
    nodePath.add();
    nodePath.add("b", null);
    nodePath.add("c", null);

    doTest(treeBuilder.buildTree("(a (b c))"), nodePath, new String[]{"c"});
    doTest(treeBuilder.buildTree("(b c)"), nodePath, new String[]{"c"});
    doTest(treeBuilder.buildTree("(x (b c))"), nodePath, new String[]{"c"});
    doTest(treeBuilder.buildTree("(x (y (b c)))"), nodePath, new String[]{"c"});
    doTest(treeBuilder.buildTree("(x (y (z (b c))))"), nodePath, new String[]{"c"});
    doTest(treeBuilder.buildTree("(x (y (b c) (z (b c))))"), nodePath, new String[]{"c", "c"});
  }

  public void testSkipLevels2() {
    final NodePath<String> nodePath = new NodePath<String>();
    nodePath.add("a", null);
    nodePath.add();
    nodePath.add("c", null);

    doTest(treeBuilder.buildTree("(a (b c))"), nodePath, new String[]{"c"});
    doTest(treeBuilder.buildTree("(a c)"), nodePath, new String[]{"c"});
    doTest(treeBuilder.buildTree("(a (x c))"), nodePath, new String[]{"c"});
    doTest(treeBuilder.buildTree("(a (x (y c)))"), nodePath, new String[]{"c"});
    doTest(treeBuilder.buildTree("(a (x (y (z c))))"), nodePath, new String[]{"c"});
    doTest(treeBuilder.buildTree("(a c (y (z c))))"), nodePath, new String[]{"c", "c"});
  }

  public void testSkipLevels3() {
    final NodePath<String> nodePath = new NodePath<String>();
    nodePath.add("a", null);
    nodePath.add("b", null);
    nodePath.add();

    doTest(treeBuilder.buildTree("(a (b c))"), nodePath, new String[]{"c"});
    doTest(treeBuilder.buildTree("(a b)"), nodePath, null);
    doTest(treeBuilder.buildTree("(a (b c) (b f g) (h (i j (b k)))))"), nodePath, new String[]{"c", "f", "g"});
  }

  public void testSkipLevels4() {
    final NodePath<String> nodePath = new NodePath<String>();
    nodePath.add("a", null);
    nodePath.add();
    nodePath.add("b", null);
    nodePath.add();

    doTest(treeBuilder.buildTree("(a (b c))"), nodePath, new String[]{"c"});
    doTest(treeBuilder.buildTree("(a b)"), nodePath, null);
    doTest(treeBuilder.buildTree("(a (b c) (b f g) (h (i j (b k)))))"), nodePath, new String[]{"c", "f", "g", "k"});
  }

  public void testPatternSimple() {
    final NodePath<String> nodePath = new NodePath<String>("a.b.c");

    doTest(treeBuilder.buildTree("(a (b c))"), nodePath, new String[]{"c"});
    doTest(treeBuilder.buildTree("(a (b d))"), nodePath, null);
    doTest(treeBuilder.buildTree("(a (b c) (b c))"), nodePath, new String[]{"c", "c"});
    doTest(treeBuilder.buildTree("(a (b c) d (b c))"), nodePath, new String[]{"c", "c"});
    doTest(treeBuilder.buildTree("(a (b c) (d (e f)) (b c) e)"), nodePath, new String[]{"c", "c"});
    doTest(treeBuilder.buildTree("(a (b d) (d e f) (b c))"), nodePath, new String[]{"c"});
    doTest(treeBuilder.buildTree("(c (b a))"), nodePath, null);
  }

  public void testPatternSubscripts() {
    final NodePath<String> nodePath = new NodePath<String>("a.b[1,3,5].c[0,2,4]");

    doTest(treeBuilder.buildTree("(a (b c) (b c) (b c))"), nodePath, new String[]{"c"});
    doTest(treeBuilder.buildTree("(a (b c) (b d c f) (b c))"), nodePath, null);
    doTest(treeBuilder.buildTree("(a (b c) (b c d c) (b c))"), nodePath, new String[]{"c", "c"});
  }

  public void testPatternSkipLevels1() {
    final NodePath<String> nodePath = new NodePath<String>("**.b.c");

    doTest(treeBuilder.buildTree("(a (b c))"), nodePath, new String[]{"c"});
    doTest(treeBuilder.buildTree("(b c)"), nodePath, new String[]{"c"});
    doTest(treeBuilder.buildTree("(x (b c))"), nodePath, new String[]{"c"});
    doTest(treeBuilder.buildTree("(x (y (b c)))"), nodePath, new String[]{"c"});
    doTest(treeBuilder.buildTree("(x (y (z (b c))))"), nodePath, new String[]{"c"});
    doTest(treeBuilder.buildTree("(x (y (b c) (z (b c))))"), nodePath, new String[]{"c", "c"});
  }

  public void testPatternSkipLevels2() {
    final NodePath<String> nodePath = new NodePath<String>("a.**.c");

    doTest(treeBuilder.buildTree("(a (b c))"), nodePath, new String[]{"c"});
    doTest(treeBuilder.buildTree("(a c)"), nodePath, new String[]{"c"});
    doTest(treeBuilder.buildTree("(a (x c))"), nodePath, new String[]{"c"});
    doTest(treeBuilder.buildTree("(a (x (y c)))"), nodePath, new String[]{"c"});
    doTest(treeBuilder.buildTree("(a (x (y (z c))))"), nodePath, new String[]{"c"});
    doTest(treeBuilder.buildTree("(a c (y (z c))))"), nodePath, new String[]{"c", "c"});
  }

  public void testPatternSkipLevels3() {
    final NodePath<String> nodePath = new NodePath<String>("a.b.**");

    doTest(treeBuilder.buildTree("(a (b c))"), nodePath, new String[]{"c"});
    doTest(treeBuilder.buildTree("(a b)"), nodePath, null);
    doTest(treeBuilder.buildTree("(a (b c) (b f g) (h (i j (b k)))))"), nodePath, new String[]{"c", "f", "g"});
  }

  public void testPatternSkipLevels4() {
    final NodePath<String> nodePath = new NodePath<String>("a.**.b.**");

    doTest(treeBuilder.buildTree("(a (b c))"), nodePath, new String[]{"c"});
    doTest(treeBuilder.buildTree("(a b)"), nodePath, null);
    doTest(treeBuilder.buildTree("(a (b c) (b f g) (h (i j (b k)))))"), nodePath, new String[]{"c", "f", "g", "k"});
  }

  public static Test suite() {
    TestSuite suite = new TestSuite(TestNodePath.class);
    return suite;
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}
