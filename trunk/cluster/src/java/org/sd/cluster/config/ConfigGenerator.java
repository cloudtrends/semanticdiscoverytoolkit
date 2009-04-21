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
package org.sd.cluster.config;


import org.sd.io.FileUtil;
import org.sd.util.tree.TraversalIterator;
import org.sd.util.tree.Tree;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Utility to create a cluster definition configuration file.
 * <p>
 * @author Spence Koehler
 */
public class ConfigGenerator {

  private File destinationDir;
  private List<String> allNodeDefs;
  private List<String> namedNodeDefs;
  private Map<String, String[]> group2nodeDefs;
  
  /**
   * Construct given dir and group definitions, where dirAndGroupDefs[0]
   * is a directory to which the config should be written and dirAndGroupDefs[1+]
   * are group definitions where each group definition is of the form:
   * <p>
   * "groupName:nodeDefs"
   * <p>
   * Where nodeDefs is of the form "nodeDef,nodeDef,..."
   * <p>
   * Where each nodeDef is of the form:
   * <p>
   * nodeN-C, where N is the node's number and C is the node's jvm count.
   * <p>
   * Node numbers are assumed to range from 1 to N, where N is the total
   * number of nodes.
   */
  public ConfigGenerator(String[] dirAndGroupDefs) {
    this.destinationDir = new File(dirAndGroupDefs[0]);
    this.allNodeDefs = new ArrayList<String>();
    this.namedNodeDefs = new ArrayList<String>();
    this.group2nodeDefs = new LinkedHashMap<String, String[]>();

    init(dirAndGroupDefs);
  }

  private final void init(String[] groupDefs) {
    int nodeNum = 1;

    for (int i = 1; i < groupDefs.length; ++i) {
      final String groupDef = groupDefs[i];
      final String[] groupSplit = groupDef.split("\\s*:\\s*");

      String groupName = null;
      String groupNodeDefs = null;
      if (groupSplit.length == 2) {
        groupName = groupSplit[0];
        groupNodeDefs = groupSplit[1];
      }
      else {
        groupNodeDefs = groupSplit[0];
      }

      final String[] pieces = groupNodeDefs.split("\\s*,\\s*");
      final String[] nodes = new String[pieces.length];

      // add all node defs
      for (int j = 0; j < pieces.length; ++j) {
        String piece = pieces[j];

        // convert specific machine name to "nodeN"
        final String[] parts = piece.split("-");
        namedNodeDefs.add(parts[0]);

        if (!piece.startsWith("node")) {
          if (parts.length == 1) {
            piece = "node" + nodeNum;
          }
          else {
            piece = "node" + nodeNum + "-" + parts[1];
          }
        }
        nodes[j] = piece;

        allNodeDefs.add(piece);

        ++nodeNum;
      }

      // add group2nodeDefs
      if (groupName != null) group2nodeDefs.put(groupName, nodes);
    }
  }


  public final Bundle writeClusterDefinition() throws IOException {
    final Tree<String> gatewayTree = buildGatewayTree();
    final String defName = buildDefinitionName(gatewayTree);
    final Tree<String> clusterDef = buildClusterDefinition(gatewayTree);

    final File clusterDefFile = new File(destinationDir, defName);

    if (!clusterDefFile.exists()) {
      final BufferedWriter writer = FileUtil.getWriter(clusterDefFile);
      writer.write(clusterDef.toString());
      writer.newLine();
      writer.close();
    }
    else {
      throw new IllegalStateException(clusterDefFile.getAbsolutePath() + " already exists!");
    }

    final String deployLine = buildDeployLine(defName);

    return new Bundle(clusterDefFile, deployLine);
  }

	public final ClusterDefinition buildClusterDefinition(String gateway, String[] machines) {
    final Tree<String> gatewayTree = buildGatewayTree();
    final String defName = buildDefinitionName(gatewayTree);
    final Tree<String> clusterDef = buildClusterDefinition(gatewayTree);
		
		return new ClusterDefinition(defName, clusterDef, gateway, machines);
	}

  private final String buildDefinitionName(Tree<String> gatewayTree) {

    final int[] numJvmsPerLevel = new int[gatewayTree.maxDepth() - 1];
    int numMachines = 0;
    int numJvms = 0;

    for (TraversalIterator<String> iter = gatewayTree.iterator(Tree.Traversal.BREADTH_FIRST); iter.hasNext(); ) {
      final Tree<String> curNode = iter.next();
      final int curDepth = curNode.depth();
      if (curDepth == 0) continue;  // ignore "gateway"

      final String curNodeDef = curNode.getData();
      final int jvmNumCount = getJvmNumCount(curNodeDef);

      numJvmsPerLevel[curDepth - 1] += jvmNumCount;
      ++numMachines;
      numJvms += jvmNumCount;
    }

    final StringBuilder result = new StringBuilder();

    result.
      append(numMachines).append('m').
      append(numJvms).append("n.");

    for (int i = 0; i < numJvmsPerLevel.length; ++i) {
      final int levelJvms = numJvmsPerLevel[i];
      result.append(levelJvms);

      if (i + 1 < numJvmsPerLevel.length) result.append('-');
    }

    result.append(".def");

    return result.toString();
  }

  private final Tree<String> buildClusterDefinition(Tree<String> gatewayTree) {
    final Tree<String> result = new Tree<String>("_root_");

    result.addChild(gatewayTree);

    if (!group2nodeDefs.containsKey("controller")) {
      result.addChild(buildGroupTree("controller", new String[]{allNodeDefs.get(0)}));
    }
    for (Map.Entry<String, String[]> group2nodeDef : group2nodeDefs.entrySet()) {
      result.addChild(buildGroupTree(group2nodeDef.getKey(), group2nodeDef.getValue()));
    }
    result.addChild(buildGroupTree("all", allNodeDefs.toArray(new String[allNodeDefs.size()])));

    return result;
  }

  /**
   * Create a tree with "gateway" root, where each node has at most 3 children.
   * <p>
   * Where each nodeDef is of the form "nodeN-C" where N is the node's number
   * and C is the node's jvm count.
   */
  private final Tree<String> buildGatewayTree() {
    final Tree<String> result = new Tree<String>("gateway");

    final LinkedList<Tree<String>> parentStack = new LinkedList<Tree<String>>();
    Tree<String> curParent = result;

    for (String nodeDef : allNodeDefs) {
      final int numChildren = curParent.numChildren();
      if (numChildren == 3 || (curParent == result && numChildren > 0)) {
        // need to use a new parent.
        curParent = parentStack.removeFirst();
      }

      final Tree<String> anotherParent = curParent.addChild(nodeDef);
      parentStack.addLast(anotherParent);
    }

    return result;
  }

  /**
   * Create a tree of the form (groupName nodeN-a nodeN-b ...) where N is each defined
   * node's node number and a, b, ... are jvmNumbers from 0 to C-1 for each nodeDef.
   * <p>
   * Note that a nodeDef is of the form "nodeN-C" where N is the node's number
   * and C is the node's jvm count.
   */
  private final Tree<String> buildGroupTree(String groupName, String[] nodeDefs) {
    final Tree<String> result = new Tree<String>(groupName);

    for (String nodeDef : nodeDefs) {
      final String[] pieces = nodeDef.split("-");
      int jvmCount = 1;
      if (pieces.length == 2) {
        jvmCount = Integer.parseInt(pieces[1]);
      }
      final String nodeName = pieces[0];

      for (int i = 0; i < jvmCount; ++i) {
        result.addChild(nodeName + "-" + i);
      }
    }

    return result;
  }

  private final int getJvmNumCount(String nodeDef) {
    int result = 1;

    final String[] pieces = nodeDef.split("-");
    if (pieces.length == 2) {
      result = Integer.parseInt(pieces[1]);
    }

    return result;
  }

  private final String buildDeployLine(String defName) {
    final StringBuilder result = new StringBuilder();

    result.append("./deploy USER ").append(defName).append(" \"");

    for (Iterator<String> iter = namedNodeDefs.iterator(); iter.hasNext(); ) {
      final String machineName = iter.next();

      result.append(machineName);
      if (iter.hasNext()) result.append(',');
    }

    result.append("\" JVM-SIZE");

    return result.toString();
  }


  public static final class Bundle {
    public final File file;
    public final String deployLine;

    Bundle(File file, String deployLine) {
      this.file = file;
      this.deployLine = deployLine;
    }

    public String toString() {
      final StringBuilder result = new StringBuilder();

      result.
        append(" ClusterDef: ").append(file).
        append("\n     Deploy: ").append(deployLine);

      return result.toString();
    }
  }


  // each arg:  "<groupName>:<nodeDefs>", where nodeDefs is a comma-delimited list of "nodeN-C".
  // java org.sd.cluster.config.ConfigGenerator ~/co/core/src/java/org.sd.cluster/config/resources/clusters "server:node1" "grabber:node2-2" "extractor:node3-5,node4-5,node5-5,node6-5,node7-5"
  // java org.sd.cluster.config.ConfigGenerator ~/co/core/src/java/org.sd.cluster/config/resources/clusters "server:node1" "extractor:node2-5,node3-5,node4-5,node5-5,node6-5"
  public static final void main(String[] args) throws IOException {
    final ConfigGenerator configurator = new ConfigGenerator(args);
    final Bundle bundle = configurator.writeClusterDefinition();
    System.out.println(bundle);
  }
}
