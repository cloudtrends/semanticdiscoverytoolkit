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


import org.sd.util.IdGenerator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A helper class to analyze a tree and cache the results.
 * <p>
 * Note that an analyzer's results are invalidated by structural changes to the tree.
 *
 * @author Spence Koehler
 */
public class TreeAnalyzer<T> {

  private final Tree<T> tree;
  private final KeyFunction<T> keyFunction;

  private final int numLevels;                             // number of levels in the tree
  private final Map<Integer, NodeInfo> node2info;          // map each node's transient id to its info
  private final List<NodeInfo> leafInfos;                  // cache terminal infos
  private final Map<Integer, List<NodeInfo>> level2infos;  // map level to its infos
  private final NodeInfo rootInfo;                         // cache root info

  /**
   * Construct a tree analyzer for the given tree and using the given match
   * function (okay if null).
   *
   * @param tree         The tree to be analyzed.
   * @param keyFunction  A key function for creating a key from node data for
   *                     matching data across nodes. If null, a default key
   *                     function will be generated based on data.toString().
   */
  public TreeAnalyzer(Tree<T> tree, KeyFunction<T> keyFunction) {
    this.tree = tree;
    this.keyFunction = keyFunction != null ? keyFunction : new KeyFunction<T>() {
      public String getKey(T data) {
        return data == null ? "" : data.toString();
      }
    };

    this.numLevels = tree.maxDepth();
    this.node2info = new LinkedHashMap<Integer, NodeInfo>();  // breadth-first order
    this.leafInfos = new ArrayList<NodeInfo>();
    this.level2infos = new LinkedHashMap<Integer, List<NodeInfo>>();
    this.rootInfo = init();
  }
  
  /**
   * Get the analyzed tree.
   */
  public Tree<T> getTree() {
    return tree;
  }

  /**
   * Get the node information for the root of the tree.
   */
  public NodeInfo getRootInfo() {
    return rootInfo;
  }

  /**
   * Get the number of levels in the tree (tree's max depth).
   */
  public int getNumLevels() {
    return numLevels;
  }

  /**
   * Get node info instances for all nodes at the given level
   * in the tree.
   */
  public List<NodeInfo> getLevelInfos(int level) {
    return level2infos.get(level);
  }

  /**
   * Get node info instances for all leaf nodes.
   */
  public List<NodeInfo> getLeafInfos() {
    return leafInfos;
  }

  /**
   * Determine whether the given node is contained in this analyzer's tree.
   */
  public boolean containsNode(Tree<T> node) {
    return node2info.containsKey(node.getTransientId());
  }

  /**
   * Get the node info corresponding to the given node.
   *
   * @return the node info or null if the node is not in the tree.
   */
  public final NodeInfo getNodeInfo(Tree<T> node) {
    return getNodeInfo(node.getTransientId());
  }

  /**
   * Get the node info corresponding to the given transientId.
   */
  public final NodeInfo getNodeInfo(int transientId) {
    return node2info.get(transientId);
  }

  /**
   * Get the path from the tree's root to the given node.
   */
  public List<Tree<T>> getPathTo(Tree<T> node) {
    final LinkedList<Tree<T>> result = new LinkedList<Tree<T>>();

    while (node != null) {
      result.addFirst(node);
      node = node.getParent();
    }

    return result;
  }

  private final NodeInfo init() {
    int nextId = 1;
    NodeInfo result = new NodeInfo(tree, 0, null, null, null, null, null);
    catalogue(tree, nextId++, result);

    for (int level = 1; level < numLevels; ++level) {
      final List<Tree<T>> nodes = tree.treesAtDepth(level);
      final Integer[] uids = computeUniqueIds(nodes);
      final Map<String, LevelDataCache> key2levelData = new HashMap<String, LevelDataCache>();
      final LevelCache levelCache = new LevelCache();

      NodeInfo lastNodeInfo = null;
      for (Tree<T> node : nodes) {
        final NodeInfo curNodeInfo = new NodeInfo(node, level, nodes, uids, lastNodeInfo, key2levelData, levelCache);
        catalogue(node, nextId++, curNodeInfo);
        lastNodeInfo = curNodeInfo;
      }
    }

    return result;
  }

  private final Integer[] computeUniqueIds(List<Tree<T>> nodes) {
    final Integer[] result = new Integer[nodes.size()];

    final IdGenerator<String> idGenerator = new IdGenerator<String>();
    int index = 0;
    for (Tree<T> node : nodes) {
      final Integer id = idGenerator.getId(keyFunction.getKey(node.getData()));
      result[index++] = id;
    }

    return result;
  }

  /**
   * Use this analyzer's match function to determine whether the two nodes
   * match.
   */
  public final boolean matches(Tree<T> node1, Tree<T> node2) {
    final String key1 = keyFunction.getKey(node1.getData());
    final String key2 = keyFunction.getKey(node2.getData());
    return key1 != null && key2 != null && key1.equals(key2);
  }

  private final void catalogue(Tree<T> node, int transientId, NodeInfo info) {
    node.setTransientId(transientId);
    node2info.put(transientId, info);
    if (info.numChildren == 0) leafInfos.add(info);

    List<NodeInfo> infos = level2infos.get(info.level);
    if (infos == null) {
      infos = new ArrayList<NodeInfo>();
      level2infos.put(info.level, infos);
    }
    infos.add(info);
  }

  private final class LevelDataCache {
    public int localRepeatIndex;
    public int levelRepeatIndex;
    public int localOverrunIndex;
    public int numLocalRepeatSiblings;
    public int localRepeatId;
    public final int numLevelRepeatSiblings;
    public final int levelRepeatId;

    public LevelDataCache(Integer[] uids, int localIndex, int levelIndex, int numSiblings, LevelCache levelCache) {
      this.localRepeatIndex = 0;
      this.levelRepeatIndex = 0;
      this.localOverrunIndex = levelIndex + numSiblings - localIndex;

      setLocals(uids, levelIndex, localOverrunIndex, levelCache);

      this.numLevelRepeatSiblings = countOccurrences(uids, levelIndex, uids.length, false);
      this.levelRepeatId = levelCache.levelRepeatId++;
    }

    private final void setLocals(Integer[] uids, int levelIndex, int localOverrunIndex, LevelCache levelCache) {
      this.numLocalRepeatSiblings = countOccurrences(uids, levelIndex, localOverrunIndex, false);
      this.localRepeatId = levelCache.localRepeatId++;
    }

    public void increment(Integer[] uids, int localIndex, int levelIndex, int numSiblings, LevelCache levelCache) {
      ++levelRepeatIndex;
      if (levelIndex >= localOverrunIndex) {
        localOverrunIndex = levelIndex + numSiblings - localIndex;
        localRepeatIndex = 0;
        setLocals(uids, levelIndex, localOverrunIndex, levelCache);
      }
      else {
        ++localRepeatIndex;
      }
    }
  }

  private final class LevelCache {
    int localRepeatId = 0;
    int levelRepeatId = 0;
    int consecutiveLocalRepeatId = 0;
    int consecutiveLevelRepeatId = 0;
  }

  /**
   * Data structure to hold computed information about each node.
   */
  public final class NodeInfo {

    private final Set<String> flags;

    public final Tree<T> node;
    public final String key;
    public final int level;
    public final int numChildren;
    public final int localIndex;
    public final int levelIndex;
    public final int localRepeatIndex;
    public final int levelRepeatIndex;
    public final int localRepeatId;
    public final int levelRepeatId;
    public final int consecutiveLocalRepeatIndex;
    public final int consecutiveLevelRepeatIndex;
    public final int consecutiveLocalRepeatId;
    public final int consecutiveLevelRepeatId;
    public final int numSiblings;
    public final int numLevelNodes;
    public final int numLocalRepeatSiblings;
    public final int numLevelRepeatSiblings;
    public final int numConsecutiveLocalSiblings;
    public final int numConsecutiveLevelSiblings;

    NodeInfo(Tree<T> node, int level, List<Tree<T>> nodes, Integer[] uids, NodeInfo lastNodeInfo, Map<String, LevelDataCache> key2levelData, LevelCache levelCache) {
      this.flags = new LinkedHashSet<String>();
      this.node = node;
      this.level = level;  // could be node.depth() but this way is faster.
      final T data = node.getData();
      this.key = keyFunction.getKey(data);

      final List<Tree<T>> children = node.getChildren();
      this.numChildren = (children == null) ? 0 : children.size();

      if (nodes == null) {
        this.localIndex = 0;
        this.levelIndex = 0;
        this.localRepeatIndex = 0;
        this.levelRepeatIndex = 0;
        this.consecutiveLocalRepeatIndex = 0;
        this.consecutiveLevelRepeatIndex = 0;
        this.numSiblings = 1;
        this.numLevelNodes = 1;

        this.numLocalRepeatSiblings = 1;
        this.numLevelRepeatSiblings = 1;
        this.numConsecutiveLocalSiblings = 1;
        this.numConsecutiveLevelSiblings = 1;

        this.localRepeatId = 0;
        this.levelRepeatId = 0;
        this.consecutiveLocalRepeatId = 0;
        this.consecutiveLevelRepeatId = 0;
      }
      else {
        final List<Tree<T>> localSiblings = node.getSiblings();
        final List<Tree<T>> levelSiblings = nodes; //node.getGlobalSiblings();
        this.numSiblings = localSiblings.size();
        this.numLevelNodes = levelSiblings.size();

        if (lastNodeInfo == null) {
          this.localIndex = 0;
          this.levelIndex = 0;
          this.localRepeatIndex = 0;
          this.levelRepeatIndex = 0;
          this.consecutiveLocalRepeatIndex = 0;
          this.consecutiveLevelRepeatIndex = 0;

          this.localRepeatId = 0;
          this.levelRepeatId = 0;
          this.consecutiveLocalRepeatId = 0;
          this.consecutiveLevelRepeatId = 0;

          final LevelDataCache levelData = new LevelDataCache(uids, localIndex, levelIndex, numSiblings, levelCache);
          key2levelData.put(key, levelData);

          this.numLocalRepeatSiblings = levelData.numLocalRepeatSiblings;
          this.numLevelRepeatSiblings = levelData.numLevelRepeatSiblings;
          this.numConsecutiveLocalSiblings = countOccurrences(uids, levelIndex, levelIndex + numSiblings - localIndex, true);
          this.numConsecutiveLevelSiblings = countOccurrences(uids, levelIndex, uids.length, true);
        }
        else {
          final boolean sameParentAsLast = (node.getParent() == lastNodeInfo.node.getParent());
          final boolean sameKeyAsLast = (key != null && key.equals(lastNodeInfo.key));

          this.localIndex = sameParentAsLast ? lastNodeInfo.localIndex + 1 : 0;
          this.levelIndex = lastNodeInfo.levelIndex + 1;

          LevelDataCache levelData = key2levelData.get(key);
          if (levelData == null) {
            levelData = new LevelDataCache(uids, localIndex, levelIndex, numSiblings, levelCache);
            key2levelData.put(key, levelData);
          }
          else {
            levelData.increment(uids, localIndex, levelIndex, numSiblings, levelCache);
          }

          this.localRepeatIndex = levelData.localRepeatIndex;
          this.levelRepeatIndex = levelData.levelRepeatIndex;
          this.consecutiveLocalRepeatIndex = sameKeyAsLast ? lastNodeInfo.consecutiveLocalRepeatIndex + 1 : 0;
          this.consecutiveLevelRepeatIndex = sameKeyAsLast ? lastNodeInfo.consecutiveLevelRepeatIndex + 1 : 0;

          this.numLocalRepeatSiblings = levelData.numLocalRepeatSiblings;
          this.numLevelRepeatSiblings = levelData.numLevelRepeatSiblings;
          this.numConsecutiveLocalSiblings = sameKeyAsLast ? lastNodeInfo.numConsecutiveLocalSiblings : countOccurrences(uids, levelIndex, levelIndex + numSiblings - localIndex, true);
          this.numConsecutiveLevelSiblings = sameKeyAsLast ? lastNodeInfo.numConsecutiveLevelSiblings : countOccurrences(uids, levelIndex, uids.length, true);

          this.localRepeatId = levelData.localRepeatId;
          this.levelRepeatId = levelData.levelRepeatId;
          this.consecutiveLocalRepeatId = sameKeyAsLast ? levelCache.consecutiveLocalRepeatId : levelCache.consecutiveLocalRepeatId++;
          this.consecutiveLevelRepeatId = sameKeyAsLast ? levelCache.consecutiveLevelRepeatId : levelCache.consecutiveLevelRepeatId++;
        }
      }
    }

    public void setFlag(String flag) {
      flags.add(flag);
    }

    public void clearFlag(String flag) {
      flags.remove(flag);
    }

    public boolean hasFlag(String flag) {
      return flags.contains(flag);
    }
  }

  static final int countOccurrences(Integer[] uids, int ofIndex, int toIndex, boolean consecutive) {
    int result = 1;
    final Integer uid = uids[ofIndex];
    
    if (uid == null) return result;

    for (int i = ofIndex + 1; i < toIndex; ++i) {
      final Integer other = uids[i];

      if (other != null && uid.equals(other)) {
        ++result;
      }
      else {
        if (consecutive) break;
      }
    }

    return result;
  }

  public final PivotLevels getPivotLevels(Tree<T> leaf) {
    return new PivotLevels(leaf);
  }

  public final class PivotLevels {

    public final Tree<T> leaf;
    public final List<NodeInfo> pathInfos;
    private int[] pivotLevels;

    public PivotLevels(Tree<T> leaf) {
      this.leaf = leaf;

      final List<Tree<T>> pathToLeaf = getPathTo(leaf);
      final int pathLength = pathToLeaf.size();
      this.pathInfos = new LinkedList<NodeInfo>();

      final List<Integer> pivots = new ArrayList<Integer>();

      int index = 0;
      for (Tree<T> node : pathToLeaf) {
        final NodeInfo nodeInfo = getNodeInfo(node);
        if (nodeInfo == null) continue;
        if (nodeInfo.key == null) break;  // keys are no longer valid in path. stop here.
        pathInfos.add(nodeInfo);
        if (nodeInfo.numConsecutiveLocalSiblings > 1) {
          pivots.add(index);
        }
        ++index;
      }

      this.pivotLevels = new int[pivots.size()];
      index = 0;
      for (Integer level : pivots) pivotLevels[index++] = level;
    }

    public int[] getPivotLevels() {
      return pivotLevels;
    }

    /**
     * Compute the (most shallow) level at which this path changes from the
     * previous.
     *
     * null indicates that there is no level change
     * result[0] indicates the (explicit) section level to end (0 for none)
     * result[1..n] indicates the division levels to start (implied divisions
     *              and sections to end).
     */
    public int[] computeLevelChanges(PivotLevels previous) {
      int[] result = null;

      if (previous == null) {
        // there is nothing to end; all to start.
        if (pivotLevels.length == 0) return null;  // nothing to start

        result = new int[pivotLevels.length + 1];
        result[0] = 0;
        for (int i = 0; i < pivotLevels.length; ++i) result[i + 1] = pivotLevels[i];
        return result;
      }

      final int len1 = previous.pathInfos.size();
      final int len2 = this.pathInfos.size();
      final int len = len1 < len2 ? len1 : len2;

      int diverged = -1;
      NodeInfo info1 = null;
      NodeInfo info2 = null;
      for (int index = 0; index < len; ++index) {
        info1 = previous.pathInfos.get(index);
        info2 = this.pathInfos.get(index);
        if (info1 == info2) continue;  // no change here. try next
        if (!info1.key.equals(info2.key)) {  // paths diverged. done
          diverged = index;
          break;
        }

        // keys are same, but infos aren't ==> index changed
        // the corresponding pivot level and all deeper need to be (re) started.
        return pivotsAtAndDeeper(index);
      }

      if (diverged < 0) {  // didn't diverge
        // need to check beyond the common length for a (the first) pivot level
        if (len2 > len) {
          return pivotsAtAndDeeper(len);
        }

        return null;  // didn't differ == no change
      }

      // check for divergence due to starting a section at the level
      // as opposed to divergence due to having ended a section. or both.
//      final boolean isStarting = (this.pathInfos.get(diverged).numConsecutiveLocalSiblings > 1);
      final boolean isEnding = (previous.pathInfos.get(diverged).numConsecutiveLocalSiblings > 1);

      result = pivotsAtAndDeeper(diverged);
      if (isEnding) {
        if (result != null) result[0] = diverged;
        else result = new int[]{diverged};
      }

      return result;
    }

    private int[] pivotsAtAndDeeper(int level) {
      int levelIndex = 0;
      for (; levelIndex < pivotLevels.length; ++levelIndex) {
        if (pivotLevels[levelIndex] >= level) break;
      }

      if (levelIndex == pivotLevels.length) return null;  // nothing to start

      final int[] result = new int[pivotLevels.length - levelIndex + 1];
      result[0] = 0;  // leave 0 for level to end
      for (int i = levelIndex; i < pivotLevels.length; ++i) {
        result[i - levelIndex + 1] = pivotLevels[i];
      }
      return result;
    }
  }
}
