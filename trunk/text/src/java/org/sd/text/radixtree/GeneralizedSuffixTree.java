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
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Implementation of a generalized suffix tree using a radix tree.
 * <p>
 * A generalized suffix tree builds a trie of a set of strings and
 * all of their substrings. It can be used to find the longest common
 * substrings in O(N) where N is the sum of the lengths of the strings.
 *
 * @author Spence Koehler
 */
public class GeneralizedSuffixTree {

  public static final String EOS = "$";


  private String[] strings;
  private RadixTreeImpl<SuffixData> suffixTree;
  private String eosMarker;
  private int eosLen;

  private List<Set<Tree<RadixData<SuffixData>>>> _leavesByString;

  private static final ValueMerger<SuffixData> VALUE_MERGER = new ValueMerger<SuffixData>() {
    public void merge(RadixData<SuffixData> existingData, SuffixData newValue) {
      final SuffixData existingSuffixData = existingData.getValue();
      existingSuffixData.incorporate(newValue);
    }
  };

  private static final ValueReplicator<SuffixData> VALUE_REPLICATOR = new ValueReplicator<SuffixData>() {
    public SuffixData replicate(SuffixData value) {
      return value.replicate();
    }
  };

  /**
   * Construct with the given set of strings.
   */
  public GeneralizedSuffixTree(String[] strings) {
    this(strings, EOS);
  }

  public GeneralizedSuffixTree(String[] strings, String eosMarker) {
    this.strings = strings;
    this.eosMarker = eosMarker;
    this.eosLen = (eosMarker == null) ? 0 : eosMarker.length();
    this.suffixTree = new RadixTreeImpl<SuffixData>();

    for (int i = 0; i < strings.length; ++i) {
      final String string = strings[i];
      final int len = string.length();

      for (int j = 0; j < len; ++j) {
        final String suffix = (j == 0) ? string + eosMarker + i: string.substring(j) + eosMarker;
        suffixTree.insert(suffix, new SuffixData(i, j), VALUE_MERGER, VALUE_REPLICATOR);
      }
    }
  }
  
  public Set<String> longestSubstrs() {
    return longestSubstrs(1);
  }

  public Set<String> longestSubstrs(int minSize) {

    if (minSize <= 1) minSize = 1;

    final PathContainer pathContainer = new PathContainer(suffixTree);
    final Set<String> result = pathContainer.collectLongest(minSize);

    return result;
  }

  /**
   * Clip the eos marker from the end of the string.
   */
  private final String clip(String string, int startPos) {
    int eosPos = string.lastIndexOf(eosMarker);
    if (eosPos < 0) eosPos = string.length();
    return string.substring(startPos, eosPos);
  }


  public final class SuffixData {
    public final int stringIndex;
    public final int substrStartIndex;
    private List<SuffixData> shared;  // other data's sharing space with this node.

    public SuffixData(int stringIndex, int substrStartIndex) {
      this.stringIndex = stringIndex;
      this.substrStartIndex = substrStartIndex;
      this.shared = null;
    }

    /** Copy self */
    public SuffixData replicate() {
      final SuffixData result = new SuffixData(stringIndex, substrStartIndex);
      result.incorporateShared(shared);
      return result;
    }

    public String getSourceString() {
      return strings[stringIndex];
    }

    public String getString(int depth) {
      return strings[stringIndex].substring(substrStartIndex, substrStartIndex + depth);
    }

    public String getSuffix() {
      return substrStartIndex == 0 ? strings[stringIndex] : strings[stringIndex].substring(substrStartIndex);
    }

    public void incorporate(SuffixData other) {
      if (shared == null) shared = new ArrayList<SuffixData>();
      shared.add(other);
      incorporateShared(other.shared);
    }

    private final void incorporateShared(List<SuffixData> otherShared) {
      if (otherShared != null) {
        if (shared == null) shared = new ArrayList<SuffixData>();
        for (SuffixData sharedData : otherShared) {
          shared.add(sharedData);
          incorporateShared(sharedData.shared);
        }
      }
    }

    public List<SuffixData> getShared() {
      return shared;
    }

    public String toString() {
      final StringBuilder result = new StringBuilder();

      result.append(stringIndex).append(':').append(substrStartIndex);
      if (shared != null) {
        for (SuffixData sharedData : shared) {
          result.append(',').append(sharedData.toString());
        }
      }

      return result.toString();
    }
  }

  private final class PathContainer {

    public final Map<Tree<RadixData<SuffixData>>, PathElement> node2pathElt;
    public final List<Tree<RadixData<SuffixData>>> leaves;
    public final List<Path> paths;

    PathContainer(RadixTreeImpl<SuffixData> suffixTree) {
      this.node2pathElt = new HashMap<Tree<RadixData<SuffixData>>, PathElement>();
      this.leaves = suffixTree.getRoot().gatherLeaves();
      this.paths = new ArrayList<Path>();

      // create all paths
      for (Tree<RadixData<SuffixData>> leaf : leaves) {
        paths.add(new Path(leaf, node2pathElt));
      }

      // finalize all paths
      for (Path path : paths) {
        path.finalizePath();
      }
    }

    public Set<String> collectLongest(int minSize) {

      final Set<String> result = new HashSet<String>();

      final Map<BitSet, PathElementSet> participants2pathElts = new HashMap<BitSet, PathElementSet>();
      for (Path path : paths) path.collectLongest(participants2pathElts);

      // now we want to have each participant's best contribution from the largest group
      // map each participant to its longest strings with the most contributors, next most, ... least.
      // define "best" as that with the most contributors and size >= minSize

      final Map<Integer, ContributionLevels> participant2clevels = createContributionLevels(participants2pathElts);

      for (Map.Entry<Integer, ContributionLevels> entry : participant2clevels.entrySet()) {
        final Integer participant = entry.getKey();
        final ContributionLevels clevels = entry.getValue();
        
        clevels.getBestKeys(minSize, result);
      }

      return result;
    }

    private final Map<Integer, ContributionLevels> createContributionLevels(Map<BitSet, PathElementSet> participants2pathElts) {
      final Map<Integer, ContributionLevels> result = new HashMap<Integer, ContributionLevels>();

      for (Map.Entry<BitSet, PathElementSet> entry : participants2pathElts.entrySet()) {
        final BitSet participants = entry.getKey();
        final PathElementSet pathEltSet = entry.getValue();
        final int numParticipants = participants.cardinality();

        for (int participant = participants.nextSetBit(0); participant >= 0; participant = participants.nextSetBit(participant + 1)) {
          updateContributionLevels(participant, numParticipants, pathEltSet, result);
        }
      }      

      return result;
    }

    private final void updateContributionLevels(int participant, int numParticipants, PathElementSet pathEltSet, Map<Integer, ContributionLevels> p2cl) {
      ContributionLevels clevels = p2cl.get(participant);
      if (clevels == null) {
        clevels = new ContributionLevels(participant, numParticipants, pathEltSet);
        p2cl.put(participant, clevels);
      }
      else {
        clevels.update(numParticipants, pathEltSet);
      }
    }
  }

  private final class ContributionLevels {

    private int participant;
    private Map<Integer, ContributionLevel> num2level;

    ContributionLevels(int participant, int numParticipants, PathElementSet pathEltSet) {
      this.participant = participant;
      this.num2level = new HashMap<Integer, ContributionLevel>();

      update(numParticipants, pathEltSet);
    }

    final void update(int numParticipants, PathElementSet pathEltSet) {
      ContributionLevel clevel = num2level.get(numParticipants);
      if (clevel == null) {
        clevel = new ContributionLevel(numParticipants, pathEltSet);
        num2level.put(numParticipants, clevel);
      }
      else {
        clevel.update(pathEltSet);
      }
    }

    public void getBestKeys(int minSize, Set<String> results) {

      //
      // get the first level's keys at or above minSize with the most participants
      //

      final List<Integer> ordered = new ArrayList<Integer>(num2level.keySet());
      Collections.sort(ordered, new Comparator<Integer>() {
          public int compare(Integer i1, Integer i2) {
            return i2 - i1;  // sort from highest to lowest.
          }
        });

      ContributionLevel clevel = null;

      for (Integer num : ordered) {
        final ContributionLevel curClevel = num2level.get(num);
        if (curClevel.getKeyLen() >= minSize) {
          clevel = curClevel;
          break;
        }
      }

      if (clevel != null) {
        clevel.getKeys(results);
      }
    }

    public String toString() {
      return num2level.toString();
    }
  }

  private final class ContributionLevel {

    private int numParticipants;
    private PathElementSet pathEltSet;

    ContributionLevel(int numParticipants, PathElementSet pathEltSet) {
      this.numParticipants = numParticipants;

      update(pathEltSet);
    }

    final void update(PathElementSet pathEltSet) {
      // keep the pathEltSet with the longest key
      if (this.pathEltSet == null || this.pathEltSet.getKeyLen() < pathEltSet.getKeyLen()) {
        this.pathEltSet = pathEltSet;
      }
    }

    public void getKeys(Set<String> results) {
      pathEltSet.getKeys(results);
    }

    public int getKeyLen() {
      return pathEltSet.getKeyLen();
    }

    public String toString() {
      return pathEltSet.toString();
    }
  }

  private final class Path {

    public final LinkedList<PathElement> pathElements; //from root to leaf

    /**
     * Construct this path.
     */
    Path(Tree<RadixData<SuffixData>> leaf, Map<Tree<RadixData<SuffixData>>, PathElement> node2pathElt) {
      this.pathElements = new LinkedList<PathElement>();

      // create the path from leaf to root
      PathElement childElt = null; // the path element of the child of the current node.
      for (Tree<RadixData<SuffixData>> node = leaf; node != null; node = node.getParent()) {
        PathElement pathElt = node2pathElt.get(node);
        if (pathElt == null) {
          pathElt = new PathElement(node, childElt);
          node2pathElt.put(node, pathElt);
        }
        else {
          pathElt.incorporate(childElt);
        }
        pathElements.addFirst(pathElt);
        childElt = pathElt;
      }
    }

    /**
     * Finalize this path.
     */
    void finalizePath() {
      // finalize the path from root to leaf
      PathElement parentElt = null;
      for (PathElement pathElt : pathElements) {
        pathElt.finalizeElt(parentElt);
        parentElt = pathElt;
      }
    }

    public void collectLongest(Map<BitSet, PathElementSet> participants2pathElts) {

      // keep the longest string for each participant
      for (int i = pathElements.size() - 1; i >= 0; --i) {
        final PathElement pathElt = pathElements.get(i);
        final BitSet curParticipants = pathElt.getParticipants();
        
        PathElementSet curMapping = participants2pathElts.get(curParticipants);
        if (curMapping == null) {
          participants2pathElts.put(curParticipants, new PathElementSet(pathElt));
        }
        else {
          // do a special add that discards shorter, keeps equal, resets with longer.
          curMapping.add(pathElt);
        }
      }
    }

    public String toString() {
      final StringBuilder result = new StringBuilder();

      for (PathElement pathElt : pathElements) {
        if (result.length() > 0) result.append('-');
        result.append(pathElt.node.getData().getKey());
      }

      return result.toString();
    }
  }

  private final class PathElement {

    private Tree<RadixData<SuffixData>> node;
    private int depth;
    private BitSet participants;
    private boolean finalized;
    private String key;
    private int contribLen;

    PathElement(Tree<RadixData<SuffixData>> node, PathElement childElt) {
      this.node = node;
      this.participants = new BitSet();
      incorporate(childElt);
      this.depth = (childElt == null) ? node.depth() : childElt.depth - 1;

      // set this node's participant.
      final SuffixData suffixData = node.getData().getValue();
      if (suffixData != null) {
        participants.set(suffixData.stringIndex);
        final List<SuffixData> shared = suffixData.getShared();
        if (shared != null) {
          for (SuffixData sharedData : shared) {
            participants.set(sharedData.stringIndex);
          }
        }
      }

      this.finalized = false;
      this.key = null;
    }

    final void incorporate(PathElement childElt) {
      if (childElt != null) {
        this.participants.or(childElt.participants);
      }
    }

    /**
     * Finalize this path elt (if not already) after all paths have been
     * constructed.
     */
    void finalizeElt(PathElement parent) {
      if (!finalized) {
        if (parent == null) {
          key = "";
        }
        else {
          final String curKey = clip(node.getData().getKey(), 0);
          contribLen += curKey.length();
          key = parent.key + curKey;
        }
      }
    }

    /**
     * Determine whether this instance is finalized.
     */
    public boolean isFinalized() {
      return finalized;
    }

    /**
     * Get this element's key (available after finalized).
     */
    public String getKey() {
      return key;
    }

    /**
     * Get the length of this element's contribution to the key (available after finalized).
     */
    public int getContribLen() {
      return contribLen;
    }

    public BitSet getParticipants() {
      return participants;
    }

    public int getNumParticipating() {
      return participants.cardinality();
    }

    public String toString() {
      return key;
    }
  }

  private final class PathElementSet {

    private Set<PathElement> pathElts;
    private int keyLen;

    PathElementSet(PathElement pathElt) {
      this.pathElts = new HashSet<PathElement>();
      this.keyLen = pathElt.getKey().length();
      pathElts.add(pathElt);
    }

    /**
     * Add the path element by discarding if shorter, adding if same,
     * resetting if longer.
     */
    void add(PathElement pathElt) {
      final int curLen = pathElt.getKey().length();
      if (curLen == keyLen) {
        pathElts.add(pathElt);
      }
      else if (curLen > keyLen) {
        keyLen = curLen;
        pathElts.clear();
        pathElts.add(pathElt);
      }
    }

    public int getKeyLen() {
      return keyLen;
    }

    public void getKeys(Set<String> results) {
      for (PathElement pathElt : pathElts) {
        results.add(pathElt.getKey());
      }
    }

    public String toString() {
      final StringBuilder result = new StringBuilder();
      result.append(keyLen).append(pathElts);
      return result.toString();
    }
  }


  public static final void main(String[] args) {
    final GeneralizedSuffixTree gst = new GeneralizedSuffixTree(args);
    final Set<String> result = gst.longestSubstrs(1);

    System.out.println(result);
  }
}
