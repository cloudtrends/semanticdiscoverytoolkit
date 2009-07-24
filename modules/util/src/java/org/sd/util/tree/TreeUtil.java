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

import org.sd.util.GeneralUtil;

import java.util.List;
import java.util.LinkedList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

/**
 * Utilities over the Tree class.
 * <p>
 * @author Spence Koehler
 */
public class TreeUtil {

  /**
   * Compute (1+) the maximum depth of the given tree (0-based).
   * <p>
   * The depth of a null tree is 0; the depth of a tree with only a root and
   * no children is 1; etc.
   */
  public static <T> int maxDepth(Tree<T> tree) {
    int depth = 0;

    if (tree != null) {
      ++depth;

      List<Tree<T>> children = tree.getChildren();
      if (children != null) {
        int maxChildDepth = 0;
        for (Tree<T> child : children) {
          int childDepth = maxDepth(child);
          if (childDepth > maxChildDepth) {
            maxChildDepth = childDepth;
          }
        }
        depth += maxChildDepth;
      }
    }

    return depth;
  }

  /**
   * Return all trees at the given depth (0-based) from the given node, where
   * the root is depth 0, the next level down is depth 1, and so on.
   * <p>
   * @return the nodes at the specified depth (possibly empty, but not null).
   */
  public static <T> List<Tree<T>> treesAtDepth(Tree<T> node, int depth) {
    List<Tree<T>> result = new ArrayList<Tree<T>>();

    if (node == null) {
      return result;
    }

    if (depth == 0) {
      result.add(node);
    }
    else if (depth > 0) {
      List<Tree<T>> children = node.getChildren();
      if (children != null) {
        for (Tree<T> child : children) {
          result.addAll(treesAtDepth(child, depth - 1));
        }
      }
    }

    return result;
  }

//   /**
//    * Graft the nodes into the tree.
//    * <p>
//    * If the nodes are already children in another tree, they will be pruned
//    * from that tree.
//    */
//    public static <T> void graft(XFormTree<T> tree, List<XFormTree<T>> nodes) {
//      for (XFormTree<T> node : nodes) {
//        tree.addChild(node);
//      }
//    }

//   /**
//    * Recursively distribute the different (by data) nodes in the tree.
//    * <p>
//    * For example, the tree (a (b 1) (b 2) (c 1)) distributed is
//    * (a (b 1) (c 1) (b 2) (c 1)).
//    * <p>
//    * or, bundled, is (a (bundle (b 1) (c 1)) (bundle (b 2) (c 1))).
//    * <p>
//    * @param tree  The tree to distribute.
//    * @param bundleData  The data for a node to bundle results (of size > 1).
//    *                    If null, no bundling will occur.
//    * <p>
//    * @return a distributed XFormTree.
//    */
//   public static <T> XFormTree<T> distribute(Tree<T> tree, T bundleData) {
//     XFormTree<T> result = null;

//     if (tree != null) {
//       result = new XFormTree<T>(tree.getData(), null);
//       distributeAux(result, tree, bundleData, null);
//     }

//     return result;
//   }

//   private static <T> void distributeAux(XFormTree<T> result, Tree<T> tree, T bundleData, XFormTree<T> parent) {
//     List<Collection<Tree<T>>> combinations = getChildCombinations(tree);
//     addComboChildren(result, combinations, bundleData, true);
//   }

//   private static <T> void addComboChildren(XFormTree<T> parent, List<Collection<Tree<T>>> combinations, T bundleData, boolean recurse) {
//     if (combinations != null) {
//       XFormTree<T> bundleNode = parent;
//       boolean didFirst = false;
//       for (Collection<Tree<T>> combos : combinations) {

//         //
//         // figure out the bundleNode
//         //
//         if (combos.size() > 1 && combinations.size() > 1) {
//           bundleNode = computeBundleNode(parent, bundleData, didFirst);
//           didFirst = true;
//         }

//         //
//         // add (distributed) nodes to the bundle node
//         //
//         for (Tree<T> curTree : combos) {
//           if (recurse) {
//             XFormTree<T> nextResult = new XFormTree<T>(curTree.getData(), bundleNode);
//             distributeAux(nextResult, curTree, bundleData, bundleNode);
//           }
//           else {
//             bundleNode.addChild(new XFormTree<T>(curTree));
//           }
//         }

//         if (recurse) {
//           // check the children under bundle node for the conditions of distribution
//           List<Collection<Tree<T>>> newCombos = getChildCombinations(bundleNode);
//           if (newCombos.size() > 1) {
//             // try again with non-recursive distribution
//             bundleNode.clearChildren();
//             addComboChildren(bundleNode, newCombos, bundleData, false);
//           }
//         }
//       }
//     }
//   }

//   private static <T> XFormTree<T> computeBundleNode(XFormTree<T> parent, T bundleData, boolean didFirst) {
//     XFormTree<T> bundleNode = parent;
//     XFormTree<T> grandParent = parent.getParent();

//     if (grandParent == null) {
//       // we need to insert a bundling node
//       if (bundleData != null) {
//         bundleNode = (XFormTree<T>)parent.addChild(bundleData);
//       }
//     }
//     else {
//       // we can use existing nodes to bundle
//       if (didFirst) {
//         // we need a copy of the first node as our bundle node
//         bundleNode = new XFormTree<T>(parent.getData(), grandParent);
//       }
//     }
    
//     return bundleNode;
//   }

  private static <T> List<Collection<Tree<T>>> getChildCombinations(Tree<T> node) {
    List<Collection<Tree<T>>> result = null;

    List<Tree<T>> children = node.getChildren();
    if (children != null) {

      List<Collection<Tree<T>>> outerList = new ArrayList<Collection<Tree<T>>>();
      List<Tree<T>> innerList = null;
      T lastData = null;

      for (Tree<T> child : children) {
        T curData = child.getData();

        if (lastData == null || !lastData.equals(curData)) {
          innerList = new ArrayList<Tree<T>>();          
          outerList.add(innerList);
        }

        if (curData != null) {
          innerList.add(child);
          lastData = curData;
        }
      }

      result = GeneralUtil.combine(outerList);
    }    

    return result;
  }
  
  /**
   * Graphically display the tree hierarchy using depth first traversal , as the pstree commands does
   * <p>
   * author Abe Sanderson
   * 
   * @param tree  The tree to print
   * @return the graphical string representing the tree
   */
  public static <T> String prettyPrint(Tree<T> tree){
    return prettyPrint(tree, "", false);
  }

  /**
   * Graphically display the tree hierarchy using depth first traversal , as the pstree commands does
   * <p>
   * author Abe Sanderson
   * 
   * @param tree  The tree to print
   * @param preTree  The string prefix
   * @param hasSiblings  The
   * @return the graphical string representing the tree
   */
  public static <T> String prettyPrint(Tree<T> tree, String preTree, boolean hasSiblings){
    String result = null;
    String tabs = preTree;
    
    // Gather children
    List<Tree<T>> children = tree.getChildren();
    
    // Build the preceding tree graphics
    StringBuffer buffer = new StringBuffer();
    if(hasSiblings){
      buffer.append(preTree + "├─");
      tabs = tabs + "│ ";
    } else {
      buffer.append(preTree + "└─");
      tabs = tabs + "  ";
    }
    String data = tree.getData().toString();
    // Escape the new lines
    data = data.replace("\n", "");
    buffer.append(data + "\n");
    
    // If there are children, call this method recursively
    if(children != null && children.size() != 0){
      for(Iterator<Tree<T>> i = children.iterator(); i.hasNext();){
        final Tree<T> child = i.next();
        buffer.append(prettyPrint(child, tabs, i.hasNext()));
      }
    }

    // Convert the buffer and return the string
    result = buffer.toString();
    return result;
  }
  
  /**
   * Find the trunk to branch structure that both trees share
   * <p>
   * author Abe Sanderson
   * 
   * @return the portion of the first tree which the elements within the second tree share, or null if they do not share any
   */
  public static <T> Tree<T> findCommonTrunkPath(Tree<T> tree1, Tree<T> tree2){
    // todo: the desired behavior of this method is still unclear
    // For now, I am considering it for use primarily with single branch trees, to find the common path
    // For more complex tree, it still matches the common trunk path, but because branches which do not match are omitted,
    // it is not as useful to have the structure for analysis
    // e.g.: 
    // tree1 = (root (branch1(subbranch1) branch2 branch3(subbranch1 subbranch2(leaf 1))))
    // tree2 = (root (branch1 branch4 branch3(subbranch1(leaf1) subbranch2)))
    // the expected result is (root (branch1 branch3(subbranch1 subbranch2))
    // however, the original structure of the branches is now lost.  We are sure that both tree1 and tree2 share a branch like branch3 as children under root,
    // but as the original structure is lost, we can't say which child it was.  This becomes problematic if the method is being used to compare many trees
    // of varying structure, such as to create a method which finds the common trunk path for all trees in a list.  If the resultant tree from two compares is used
    // in turn to compare against a third tree, the method can't find the similar branch
    //
    // For now, the solution is to ensure that all children of a node match before they are added to the common pathway.  That is, in order for a common branch
    // to be included, its siblings must also match.
    Tree<T> commonPath;
    
    if (tree1 == null || tree2 == null){
      return null;
    }
    else if(tree1.equals(tree2)){
      return tree1;
    }
    else if (tree1.getData().equals(tree2.getData())){
      commonPath = new Tree<T>(tree1.getData());
      
      // Compare the children of each tree
      List<Tree<T>> tree1children = tree1.getChildren();
      List<Tree<T>> tree2children = tree2.getChildren();
      if (tree1children == null || tree2children == null){
        return commonPath;
      }

      Iterator<Tree<T>> t1iterator = tree1children.iterator();
      Iterator<Tree<T>> t2iterator = tree2children.iterator();

      // Stop if either tree runs out of children
      // The order of the children is important, as branch which doesn't match is not included
      List<Tree<T>> childTrees = new LinkedList<Tree<T>>();
      boolean allChildrenMatch = true;
      while (t1iterator.hasNext() && t2iterator.hasNext()){
        Tree<T> child1 = t1iterator.next();
        Tree<T> child2 = t2iterator.next();
        Tree<T> commonChild = findCommonTrunkPath(child1, child2);
        if (commonChild == null){
          allChildrenMatch = false;
        }
        else {
          childTrees.add(commonChild);
        }
      }
      
      if(allChildrenMatch){
        for (Tree<T> child : childTrees){
          commonPath.addChild(child);
        }
      }

      return commonPath;
    }
    else {
      return null;
    }
  }
}
