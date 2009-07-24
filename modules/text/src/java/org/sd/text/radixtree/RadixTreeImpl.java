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
package org.sd.text.radixtree;


import org.sd.util.tree.Tree;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;


/**
 * Implementation ported to use the org.sd.util.tree.Tree datastructure.
 * <p>
 * Using the Tree enables us to apply reverse operations to the radix tree,
 * which allow us to do things like longest substring matches.
 * <p>
 * Ported from RadixTreeImpl by:
 * <p>
 * author Tahseen Ur Rehman 
 * email: tahseen.ur.rehman {at.spam.me.not} gmail.com
 *
 * @author Spence Koehler
 */
public class RadixTreeImpl<T> implements RadixTree<T> {
  
  private Tree<RadixData<T>> root;

  private long size;

  /**
   * Create a Radix Tree with only the default node root.
   */
  public RadixTreeImpl() {
    root = new Tree<RadixData<T>>(new RadixData<T>("", false, null));
    size = 0;
  }
    
  /**
   * Get this tree's root.
   */
  public Tree<RadixData<T>> getRoot() {
    return root;
  }

  /**
   * Insert a new string key and its value to the tree. Throw an IllegalStateException
   * if there is a conflict.
   * 
   * @param key
   *            The string key of the object
   * @param value
   *            The value that need to be stored corresponding to the given
   *            key.
   */
  public void insert(String key, T value) {
    insert(key, value, null, null);
  }

  /**
   * Insert a new string key and its value to the tree. If there is already
   * a conflicting value at the insertion point, resolve by calling the
   * value merger function.
   * 
   * @param key
   *            The string key of the object
   * @param value
   *            The value that need to be stored corresponding to the given
   *            key.
   * @param valueMerger
   *            The value merger to use to resolve conflicts. If null, the
   *            conflict will generate an IllegalStateException.
   * @param valueReplicator
   *            The value replicator to use to make a duplicate of a value
   *            to place into nodes inserted for splits. If null, the
   *            same instance of value will be used. For values like Strings,
   *            using the same value instance does no harm as the string's
   *            contents are immutable. A value whose object carries state
   *            that does change (i.e. when merged,) however, must be
   *            duplicated for proper function.
   */
  public void insert(String key, T value, ValueMerger<T> valueMerger, ValueReplicator<T> valueReplicator) {
    try {
      insert(key, root, value, valueMerger, valueReplicator);
    }
    catch (IllegalStateException e) {
      // re-throw the exception with 'key' in the message
      throw new IllegalStateException("Duplicate key: '" + key + "'");
    }
    size++;
  }

  /**
   * Delete a key and its associated value from the tree.
   * @param key The key of the node that need to be deleted
   * @return true if deleted
   */
  public boolean delete(String key) {
    final boolean[] delete = new boolean[]{false};

    final Visitor<T> visitor = new Visitor<T>() {

      public void visit(String key, Tree<RadixData<T>> node) {
        final Tree<RadixData<T>> parent = node.getParent();
        if (parent == null) return;

        final RadixData<T> data = node.getData();

        delete[0] = data.isReal();

        // if it is a real node
        if (delete[0]) {

          // If there no children of the node we need to
          // delete it from the its parent children list
          if (node.numChildren() == 0) {
            node.prune(true, true);

            // if parent is not real node and has only one child
            // then they need to be merged.
            if (parent.numChildren() == 1 && !parent.getData().isReal()) {
              mergeNodes(parent, parent.getChildren().get(0));
            }
          }
          else if (node.numChildren() == 1) {
            // we need to merge the only child of this node with
            // itself
            mergeNodes(node, node.getChildren().get(0));
          }
          else { // we jus need to mark the node as non real.
            data.setReal(false);
          }
        }
      }

      /**
       * Merge a child into its parent node. Operation only valid if it is
       * only child of the parent node and parent node is not a real node.
       * 
       * @param parent
       *            The parent Node
       * @param child
       *            The child Node
       */
      private void mergeNodes(Tree<RadixData<T>> parent, Tree<RadixData<T>> child) {
        final RadixData<T> parentData = parent.getData();
        final RadixData<T> childData = child.getData();

        parentData.setKey(parentData.getKey() + childData.getKey());
        parentData.setReal(childData.isReal());
        parentData.setValue(childData.getValue());

        // delete the child, moving its children to the parent
        final List<Tree<RadixData<T>>> children = child.getChildren();
        child.prune(true, true);
        if (children != null) {
          for (Tree<RadixData<T>> gchild : children) {
            parent.addChild(gchild);
          }
        }
      }
    };

    visit(key, visitor);

    if(delete[0]) {
      size--;   
    }
        
    return delete[0];
  }

  /**
   * Find a value based on its corresponding key.
   * 
   * @param key The key for which to search the tree.
   * @return The value corresponding to the key. null if the key cannot be found.
   */
  public T find(String key) {
    final List<T> result = new ArrayList<T>();

    final Visitor<T> visitor = new Visitor<T>() {

      public void visit(String key, Tree<RadixData<T>> node) {
        final RadixData<T> data = node.getData();

        if (data.isReal()) {
          result.add(data.getValue());
        }
      }
    };

    visit(key, visitor);

    return result.size() == 1 ? result.get(0) : null;
  }

  /**
   * Check if the tree contains any entry corresponding to the given key.
   * 
   * @param key The key that needto be searched in the tree.
   * @return retun true if the key is present in the tree otherwise false
   */
  public boolean contains(String key) {
    final boolean[] result = new boolean[]{false};

    final Visitor<T> visitor = new Visitor<T>() {

      public void visit(String key, Tree<RadixData<T>> node) {
        result[0] = node.getData().isReal();
      }
    };

    visit(key, visitor);

    return result[0];
  }
    
  /**
   * Search for all the keys that start with given prefix. limiting the results based on the supplied limit.
   * 
   * @param key The prefix for which keys need to be search
   * @param recordLimit The limit for the results
   * @return The list of values whose key start with the given prefix
   */
  public List<T> searchPrefix(String key, int recordLimit) {
    List<T> keys = new ArrayList<T>();

    Tree<RadixData<T>> node = searchPefix(key, root);

    if (node != null) {
      final RadixData<T> nodeData = node.getData();
      if (nodeData.isReal()) {
        keys.add(nodeData.getValue());
      }
      getNodes(node, keys, recordLimit);
    }

    return keys;
  }

  /**
   * Return the size of the Radix tree
   * @return the size of the tree
   */
  public long getSize() {
    return size;
  }


  /**
   * Recursively insert the key in the radix tree.
   * 
   * @param key The key to be inserted
   * @param node The current node
   * @param value The value associated with the key 
   * @param valueMerger The function to use for conflicts. If null, conflicts
   *        cause an IllegalStateException to be thrown.
   * @pram valueReplicator
   *            The value replicator to use to make a duplicate of a value
   *            to place into nodes inserted for splits. If null, the
   *            same instance of value will be used. For values like Strings,
   *            using the same value instance does no harm as the string's
   *            contents are immutable. A value whose object carries state
   *            that does change (i.e. when merged,) however, must be
   *            duplicated for proper function.
   */
  private void insert(String key, Tree<RadixData<T>> node, T value, ValueMerger<T> valueMerger, ValueReplicator<T> valueReplicator) {
    int i = 0;
    final int keylen = key.length();
    final RadixData<T> nodeData = node.getData();
    final int nodelen = nodeData.getKey().length();
    final String nodeKey = nodeData.getKey();

    while (i < keylen && i < nodelen) {
      if (key.charAt(i) != nodeKey.charAt(i)) {
        break;
      }
      i++;
    }

    // we are either at the root node
    // or we need to go down the tree
    if ("".equals(nodeKey) || i == 0 || (i < keylen && i >= nodelen)) {
      boolean flag = false;
      final String newText = key.substring(i, keylen);
      if (node.hasChildren()) {
        for (Tree<RadixData<T>> child : node.getChildren()) {
          if (child.getData().getKey().startsWith(newText.charAt(0) + "")) {
            flag = true;
            insert(newText, child, value, valueMerger, valueReplicator);
            break;
          }
        }
      }

      // just add the node as the child of the current node
      if (flag == false) {
        node.addChild(new RadixData<T>(newText, true, value));
      }
    }

    // there is a exact match just make the current node as data node
    else if (i == keylen && i == nodelen) {
      if (nodeData.isReal()) {
        if (valueMerger == null) {
          throw new IllegalStateException("Duplicate key");
        }
        else {
          valueMerger.merge(nodeData, value);
        }
      }
      else {
        nodeData.setReal(true);
        nodeData.setValue(value);
      }
    }
    // This node need to be split as the key to be inserted
    // is a prefix of the current node key
    else if (i > 0 && i < nodelen) {
      Tree<RadixData<T>> n1 =
        new Tree<RadixData<T>>(
          new RadixData<T>(
            nodeKey.substring(i, nodelen),
            nodeData.isReal(),
            nodeData.getValue()));

      node.moveChildrenTo(n1);
      nodeData.setKey(key.substring(0, i));
      nodeData.setReal(false);
      node.addChild(n1);  // note: node's value is now also in its child.

      if (i < keylen) {
        node.addChild(new RadixData<T>(key.substring(i, keylen), true, value));

        if (valueReplicator != null) {
          nodeData.setValue(valueReplicator.replicate(nodeData.getValue()));
        }
        //else node's value is referenced in both self and child.
      }
      else {
        nodeData.setValue(value);
        nodeData.setReal(true);
      }
    }        
    // this key need to be added as the child of the current node
    else {
      node.addChild(new RadixData<T>(nodeKey.substring(i, nodelen), nodeData.isReal(), nodeData.getValue()));

      nodeData.setKey(key);
      nodeData.setReal(true);
      nodeData.setValue(value);
    }
  }

  private Tree<RadixData<T>> searchPefix(String key, Tree<RadixData<T>> node) {
    Tree<RadixData<T>> result = null;
    int i = 0;
    final int keylen = key.length();
    final RadixData nodeData = node.getData();
    final String nodeKey = nodeData.getKey();
    final int nodelen = nodeKey.length();

    while (i < keylen && i < nodelen) {
      if (key.charAt(i) != nodeKey.charAt(i)) {
        break;
      }
      i++;
    }

    if (i == keylen && i <= nodelen) {
      result = node;
    }
    else if (nodeData.getKey().equals("") || (i < keylen && i >= nodelen)) {
      if (node.hasChildren()) {
        String newText = key.substring(i, keylen);
        for (Tree<RadixData<T>> child : node.getChildren()) {
          if (child.getData().getKey().startsWith(newText.charAt(0) + "")) {
            result = searchPefix(newText, child);
            break;
          }
        }
      }
    }

    return result;
  }

  private void getNodes(Tree<RadixData<T>> parent, List<T> keys, int limit) {
    if (!parent.hasChildren()) return;

    final Queue<Tree<RadixData<T>>> queue = new LinkedList<Tree<RadixData<T>>>();

    queue.addAll(parent.getChildren());

    while (!queue.isEmpty()) {
      Tree<RadixData<T>> node = queue.remove();
      final RadixData<T> nodeData = node.getData();
      if (nodeData.isReal()) {
        keys.add(nodeData.getValue());
      }

      if (keys.size() == limit) {
        break;
      }

      if (node.hasChildren()) {
        queue.addAll(node.getChildren());
      }
    }
  }

  /**
   * visit the node whose key matches the given key
   * @param key The key that need to be visited
   * @param visitor The visitor object
   */
  public void visit(String key, Visitor<T> visitor) {
    if (root != null)
      visit(key, visitor, root);
  }

  /**
   * recursively visit the tree based on the supplied "key". calls the Visitor
   * for the node whose key matches the given prefix
   * 
   * @param prefix
   *            The key o prefix to search in the tree
   * @param visitor
   *            The Visitor that will be called if a node with "key" as its
   *            key is found
   * @param node
   *            The Node from where onward to search
   */
  private void visit(String prefix, Visitor<T> visitor, Tree<RadixData<T>> node) {
    int i = 0;
    final int keylen = prefix.length();
    final RadixData nodeData = node.getData();
    final String nodeKey = nodeData.getKey();
    final int nodelen = nodeKey.length();

    // match the prefix with node key
    while (i < keylen && i < nodelen) {
      if (prefix.charAt(i) != nodeKey.charAt(i)) {
        break;
      }
      i++;
    }

    // if the node key and prefix match, we found a match!
    if (i == keylen && i == nodelen) {
      visitor.visit(prefix, node);
    }
    else if ("".equals(nodeKey) || // either we are at the root
             (i < keylen && i >= nodelen)) { // OR we need to traverse the childern
      if (node.hasChildren()) {
        String newText = prefix.substring(i, keylen);
        for (Tree<RadixData<T>> child : node.getChildren()) {
          // recursively search the child nodes
          if (child.getData().getKey().startsWith(newText.charAt(0) + "")) {
            visit(newText, visitor, child);
            break;
          }
        }
      }
    }
  }
    

  /**
   * Display the Trie on console. WARNING! Do not use for large Trie. For
   * testing purpose only.
   */
  @Deprecated
  public void display() {
    display(0, root);
  }

  @Deprecated
  private void display(int level, Tree<RadixData<T>> node) {
    for (int i = 0; i < level; i++) {
      System.out.print(" ");
    }
    System.out.print("|");
    for (int i = 0; i < level; i++) {
      System.out.print("-");
    }

    final RadixData nodeData = node.getData();
    if (nodeData.isReal() == true)
      System.out.println(nodeData.getKey() + "[" + nodeData.getValue() + "]*");
    else
      System.out.println(nodeData.getKey());

    if (node.hasChildren()) {
      for (Tree<RadixData<T>> child : node.getChildren()) {
        display(level + 1, child);
      }
    }
  }
}
