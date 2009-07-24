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


import org.sd.util.tree.Tree;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * An iterator over arbitrary-depth xml elements.
 * <p>
 * This provides an incremental xml parser for processing xml data that
 * are too large to fit in memory all at once.
 *
 * @author Spence Koehler
 */
public class XmlNodeIterator implements Iterator<Tree<XmlLite.Data>> {

  private XmlNodeRipper ripper;
  private Tree<XmlLite.Data> curNode;
  private Tree<XmlLite.Data> next;

  /**
   * Default constructor for a plain xml file.
   */
  public XmlNodeIterator(File xmlFile) throws IOException {
    this(xmlFile, false, null);
  }

  /**
   * Construct with the given parameters.
   */
  public XmlNodeIterator(File xmlFile, boolean isHtml, Set<String> ignoreTags) throws IOException {
    this.ripper = new XmlNodeRipper(xmlFile, isHtml, ignoreTags);
    this.curNode = null;
    this.next = null;
  }

  public boolean hasNext() {
    return ripper.hasNext() || next != null;
  }

  public Tree<XmlLite.Data> next() {
    if (this.next != null) {
      this.curNode = next;
    }
    else {
      this.curNode = ripper.next();
    }
    this.next = null;
    return curNode;
  }

  public void remove() {
    //do nothing.
  }

  public void close() {
    ripper.close();
  }

  public boolean hasPath(String[] path) {
    boolean result = true;

    final TagStack tagStack = ripper.getTagStack();
    final List<XmlLite.Tag> tags = tagStack.getTags();

    int index = 0;
    for (; index < path.length && index < tags.size(); ++index) {
      final XmlLite.Tag tag = tags.get(index);
      if (!tag.name.equals(path[index])) {
        result = false;
        break;
      }
    }

    if (result && index < path.length) {
      Tree<XmlLite.Data> tree = curNode;

      for (; tree != null && index < path.length; ++index) {
        tree = findChild(tree, path[index]);
      }

      if (tree == null) {
        result = false;
      }
    }

    return result;
  }

  /**
   * Read in the xml until we pass beyond the nodes that constitue path.
   */
  public Tree<XmlLite.Data> getNode(String[] path) {
    if (!hasPath(path)) return null;

    Tree<XmlLite.Data> result = null;

    // get tag a path's current end tag instance from the ripper.
    final TagStack tagStack = ripper.getTagStack();
    final int depth = path.length - 1;

    if (tagStack.depth() > depth) {
      final List<XmlLite.Tag> tags = tagStack.getTags();
      final XmlLite.Tag theTag = tags.get(depth);

      // combine paths into a tree until the end tag instance changes
      result = new Tree<XmlLite.Data>(theTag);
      
      final Tree<XmlLite.Data> deepNode = addRemainingTags(result, tags, depth + 1);
      deepNode.addChild(curNode);

      while (hasNext()) {
        final Tree<XmlLite.Data> nextNode = next();
        final TagStack nextTagStack = ripper.getTagStack();

        if (nextTagStack.depth() > depth) {
          final List<XmlLite.Tag> nextTags = nextTagStack.getTags();
          final XmlLite.Tag nextTag = nextTags.get(depth);

          if (theTag != nextTag) {
            // have gone past the record. can stop now.
            this.next = nextNode;  // put "back"
            break;
          }
          else {
            // graft in this child.
            final Tree<XmlLite.Data> nextDeepNode = addRemainingTags(result, nextTags, depth + 1);
            nextDeepNode.addChild(nextNode);
          }
        }
      }
    }
    else {
      result = curNode;
      int curDepth = tagStack.depth();

      while (result != null && curDepth < depth) {
        result = findChild(result, path[curDepth++]);
      }
    }

    return result;
  }

  private final Tree<XmlLite.Data> addRemainingTags(Tree<XmlLite.Data> root, List<XmlLite.Tag> tags, int startDepth) {
    Tree<XmlLite.Data> result = root;

    for (int i = startDepth; i < tags.size(); ++i) {
      final XmlLite.Tag tag = tags.get(i);

      Tree<XmlLite.Data> child = findChild(result, tag);
      if (child == null) {
        child = new Tree<XmlLite.Data>(tag);
        result.addChild(child);
      }
      result = child;
    }

    return result;
  }

  private final Tree<XmlLite.Data> findChild(Tree<XmlLite.Data> tree, XmlLite.Tag tag) {
    Tree<XmlLite.Data> result = null;

    final List<Tree<XmlLite.Data>> children = tree.getChildren();
    if (children != null) {
      for (Tree<XmlLite.Data> child : children) {
        final XmlLite.Tag childTag = child.getData().asTag();
        if (childTag == tag) {
          result = child;
          break;
        }
      }
    }

    return result;
  }

  private final Tree<XmlLite.Data> findChild(Tree<XmlLite.Data> tree, String tagName) {
    Tree<XmlLite.Data> result = null;

    final List<Tree<XmlLite.Data>> children = tree.getChildren();
    if (children != null) {
      for (Tree<XmlLite.Data> child : children) {
        final XmlLite.Tag childTag = child.getData().asTag();
        if (childTag != null && childTag.name.equals(tagName)) {
          result = child;
          break;
        }
      }
    }

    return result;
  }
}
