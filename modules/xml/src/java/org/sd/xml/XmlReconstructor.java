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


import java.util.HashMap;
import java.util.Map;
import org.sd.util.tree.Tree;

/**
 * Utility to reconstruct xml from tag paths.
 * <p>
 * @author Spence Koehler
 */
public class XmlReconstructor {

  private TagMapping root;
  private Map<String, TagMapping> key2mapping;

  public XmlReconstructor() {
    this.root = null;
    this.key2mapping = new HashMap<String, TagMapping>();
  }

  /**
   * Get the reconstructed tree.
   */
  public Tree<XmlLite.Data> getXmlTree() {
    return root == null ? null : root.getTreeNode();
  }

  /**
   * Get the reconstructed dom node.
   */
  public DomNode getDomNode() {
    return root == null ? null : root.getTreeNode().getData().asDomNode();
  }

  /**
   * Add a string of the form "tag1[index]/tag2[index]/.../tagN[index]"
   * representing a tag path ending with the given text. If the text is
   * non-null, add to the end of the tree a tag named "text" over a text
   * node with the given text value.
   *
   * @return null if the tagPath does not merge into this instance; otherwise,
   *         the newly added leaf (text) xml node.
   */
  public Tree<XmlLite.Data> addTagPath(String tagPath, String text) {
    return addTagPath(tagPath.split("/"), text);
  }

  /**
   * Add strings of the form "tagI[index]" representing a tag path ending with
   * the given text. If the text is non-null, add to the end of the tree a tag
   * named "text" over a text node with the given text value.
   *
   * @return null if the tagPath does not merge into this instance; otherwise,
   *         the newly added leaf (text) xml node.
   */
  public Tree<XmlLite.Data> addTagPath(String[] orderedTags, String text) {
    Tree<XmlLite.Data> result = null;
    boolean success = true;

    TagMapping parentMapping = null;

    for (int depth = 0; depth < orderedTags.length; ++depth) {
      final String tagString = orderedTags[depth];
      final String key = buildKey(orderedTags, depth);

      TagMapping tagMapping = key2mapping.get(key);
      if (tagMapping == null) {
        if (depth == 0 && root != null) {
          // doesn't align with existing root!
          success = false;
          break;
        }

        tagMapping = new TagMapping(key, depth, tagString);
        key2mapping.put(key, tagMapping);

        if (depth == 0) {
          if (root == null) {
            this.root = tagMapping;
          }
          else {
            if (root != tagMapping) {
              success = false;
              break;
            }
          }
        }
      }

      if (parentMapping != null) {
        // add tagMapping's treeNode as a child if necessary
        final Tree<XmlLite.Data> tagNode = tagMapping.getTreeNode();
        final Tree<XmlLite.Data> parentNode = parentMapping.getTreeNode();

        final Tree<XmlLite.Data> tagParent = tagNode.getParent();
        if (tagParent == null) {
          // NOTE: current implementation assumes 'add's are in order, so index is ignored.
          parentNode.addChild(tagNode);
        }
        else if (tagParent != parentNode) {
          success = false;
          break;
        }
      }

      parentMapping = tagMapping;
    }

    // add text onto the end of the path
    if (success) {
      if (text == null) {
        result = parentMapping.getTreeNode();
      }
      else {
        // add one extra layer for cases where there are multiple text nodes under a tag
        final Tree<XmlLite.Data> textParent = new Tree<XmlLite.Data>(new XmlLite.Tag("text", false));
        textParent.getAttributes().put("TagMapping", parentMapping);

        result = textParent.addChild(new XmlLite.Text(text));
        result.getAttributes().put("TagMapping", parentMapping);

        parentMapping.getTreeNode().addChild(textParent);
      }
    }

    return result;
  }

  /**
   * Remove the tag path ending with the given node.
   */
  public void removeTagPath(Tree<XmlLite.Data> node) {
    // walk up the tree, pruning nodes until there is a sibling.
    while (node != null) {
      final Tree<XmlLite.Data> nextNode = (node.getNumSiblings() == 1) ? node.getParent() : null;
      node.prune(true, true);
      node = nextNode;
    }
  }


  private static final String buildKey(String[] orderedTags, int depth) {
    final StringBuilder result = new StringBuilder();

    for (int i = 0; i <= depth; ++i) {
      if (result.length() > 0) result.append('/');
      result.append(orderedTags[i]);
    }

    return result.toString();
  }

  public static final class TagMapping {
    private String key;
    private int depth;
    private String tagString;
    private String tag;
    private int index;
    private Tree<XmlLite.Data> treeNode;

    /**
     * Construct with a string of the form "tag[index]" and its depth.
     */
    private TagMapping(String key, int depth, String tagString) {
      this.key = key;
      this.depth = depth;
      this.tagString = tagString;

      final int[] tagIndex = new int[]{0};
      this.tag = DomUtil.parseIndexedPathPiece(tagString, tagIndex);
      this.index = tagIndex[0];

      this.treeNode = new Tree<XmlLite.Data>(new XmlLite.Tag(tag, false));
      this.treeNode.getAttributes().put("TagMapping", this);  // store backpointer
    }

    // /**
    //  * Reconstruct the original tag path to this mapping's treeNode.
    //  * <p>
    //  * Typical usage will use this instance's treeNode to get back to
    //  * this instance using treeNode.getAttributes().get("TagMapping")
    //  * and then calling this method.
    //  */
    // public String reconstructTagPath() {
    //   final StringBuilder result = new StringBuilder();

    //   result.insert(0, tagString);
    //   for (Tree<XmlLite.Data> curTreeNode = this.treeNode.getParent(); curTreeNode != null; curTreeNode = curTreeNode.getParent()) {
    //     final TagMapping curTagMapping = (TagMapping)curTreeNode.getAttributes().get("TagMapping");
    //     result.insert(0, '/').insert(0, curTagMapping.getTagString());
    //   }

    //   return result.toString();
    // }

    public String getKey() {
      return key;
    }

    public int getDepth() {
      return depth;
    }

    public String getTagString() {
      return tagString;
    }

    public Tree<XmlLite.Data> getTreeNode() {
      return treeNode;
    }

    public String getTag() {
      return tag;
    }

    public int getIndex() {
      return index;
    }
  }
}
