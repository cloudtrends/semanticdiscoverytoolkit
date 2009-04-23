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
import org.sd.util.tree.Hierarchy;
import org.sd.util.tree.SimpleTreeBuilder;
import org.sd.util.tree.SimpleTreeBuilderStrategy;
import org.sd.util.tree.Tree;
import org.sd.util.tree.TreeBuilder;
//import org.sd.util.tree.TreeBuilderFactory;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/**
 * Class to encapsulate a cluster definition.
 * <p>
 * @author Spence Koehler
 */
public class ClusterDefinition {
  
  public static final String CLUSTER_DEFINITIONS_PATH = "resources/clusters/";
  public static final String ALL_NODES_GROUP = "_ALL_";

  public static final String getClusterDefinitionPath(String defName) {
    final String ext = defName.endsWith(".def") ? "" : ".def";
    String result = FileUtil.getFilename(ClusterDefinition.class, CLUSTER_DEFINITIONS_PATH + defName + ext);
		File file = FileUtil.getFile(result);

		if (!file.exists()) {
			// try testing area
			result = result.replace("/classes/", "/unit-test-classes/");
			file = FileUtil.getFile(result);
		}

    System.out.println("clusterDef(" + defName + ")=" + result + "  [exists=" + file.exists() + "]");

    return result;
  }

//  private static TreeBuilder<String> treeBuilder = TreeBuilderFactory.getStringTreeBuilder();
  private static TreeBuilder<NameNum> treeBuilder = new SimpleTreeBuilder<NameNum>(new NameNumTreeBuilderStrategy());

  private String defName;
  private String gateway;
	private File clusterDefFile;
	private String clusterDefString;  // for (re)writing this definition
  private Tree<NameNum> machineTree;
  private Map<String, Tree<NameNum>> group2node;  // map group name to the node defining its participants
  private Collection<String> fixedNodeNames;
  private Map<String, ServerAddressCache> serverAddresses;  // user -> serverAddressCache
  private List<String> nodeNames;  // all node names
  private int numNodes;
  private String[] _machines;

	/**
	 * Package protected constructor for internal and testing access only.
	 */
  ClusterDefinition(String defName, boolean doInit) throws IOException {
    this.defName = defName;
    this.gateway = null;
		this.clusterDefFile = findClusterDefinitionFile(defName);
    this.machineTree = readMachineTree(defName, clusterDefFile);
    if(this.machineTree == null) System.out.println("readMachineTree(" + defName + ") returned null result! file=" + clusterDefFile);
    this.fixedNodeNames = null;
    this.serverAddresses = new HashMap<String, ServerAddressCache>();
    this.nodeNames = null;  // lazily computed
    this.numNodes = 0;      // lazily computed
    this._machines = null;
    this.group2node = null;

    if (doInit) init(machineTree);
  }

	/**
	 * Construct with a generic definition file where we will substitute the
	 * given gateway for the "gateway" and machine names for the "nodes" in
	 * the definition tree.
	 */
  public ClusterDefinition(String defName, String gateway, String[] machines) throws IOException {
    this(defName, false);

    // walk the tree; replace root data with gateway & replacing nodeN-n with machines[N-1]-n
    this.gateway = gateway;
    this._machines = machines;
    fixMachineTree(machineTree, gateway, machines);

    init(machineTree);
  }

	/**
	 * Construct with a named definition file that already has the machine names.
	 */
  public ClusterDefinition(String defName) throws IOException {
    this(defName, true);
  }

	/**
	 * Construct with the given generic clusterDef tree, substituting the given
	 * gateway for the "gateway" and machine names for the "nodes" in the
	 * definition tree.
	 * <p>
	 * This can be used in conjunction with ConfigGenerator in order to create
	 * on-the-fly cluster definitions that need not be persisted.
	 */
	public ClusterDefinition(String defName, Tree<String> clusterDef, String gateway, String[] machines) {
    this.defName = defName;
    this.gateway = gateway;
		this.clusterDefString = clusterDef.toString();
    this.machineTree = treeBuilder.buildTree(clusterDefString);
    this.fixedNodeNames = null;
    this.serverAddresses = new HashMap<String, ServerAddressCache>();
    this.nodeNames = null;  // lazily computed
    this.numNodes = 0;      // lazily computed
    this.group2node = null;
    this._machines = machines;
    fixMachineTree(machineTree, gateway, machines);

    init(machineTree);
	}

	/**
	 * Get this instance's initialization string.
	 */
	public String getClusterDefString() {
		return clusterDefString;
	}

	public File getClusterDefinitionFile() {
		return clusterDefFile;
	}

	private final File findClusterDefinitionFile(String defName) {
		File result = null;

		if (defName != null) {
			// First try to find a persisted cluster definition within the deployed code
			final String clusterdefFilename = getClusterDefinitionPath(defName);
			result = FileUtil.getFile(clusterdefFilename);
			if (!result.exists()) {
				// Next look at the "active" cluster def
				result = Admin.getActiveClusterDefFile();
				if (!result.exists()) {
					result = null;  // no cluster definition found.
				}
			}
		}

		return result;
	}

	private final Tree<NameNum> readMachineTree(String defName) throws IOException {
		Tree<NameNum> result = null;

System.out.println("ClusterDefinition.readMachineTree(" + defName + ")");

		if (defName != null) {
			if (!defName.endsWith(".def")) defName += ".def";
			final java.io.InputStream in = this.getClass().getResourceAsStream(CLUSTER_DEFINITIONS_PATH + defName);
			this.clusterDefString = FileUtil.readAsString(new java.io.BufferedReader(new java.io.InputStreamReader(in)), FileUtil.LINUX_COMMENT_IGNORER);
			if (clusterDefString != null) {
				result = treeBuilder.buildTree(clusterDefString);
System.out.println("\tclusterDefString(1)=" + clusterDefString);
			}
			in.close();
		}

		return result;
	}

  private final Tree<NameNum> readMachineTree(String defName, File clusterdefFile) throws IOException {
		Tree<NameNum> result = null;

System.out.println("ClusterDefinition.readMachineTree(" + defName + "," + clusterdefFile +")... exists=" + ((clusterdefFile == null) ? false : clusterdefFile.exists()));

		if (clusterdefFile == null || !clusterdefFile.exists()) {
System.out.println("\treadMachineTree 1a");
			result = readMachineTree(defName);
		}

		if (result == null) {
System.out.println("\treadMachineTree 2");
			this.clusterDefString = clusterdefFile != null && clusterdefFile.exists() ? FileUtil.readAsString(clusterdefFile.getPath(), FileUtil.LINUX_COMMENT_IGNORER) : null;
			result = clusterDefString == null ? null : treeBuilder.buildTree(clusterDefString);
System.out.println("\tclusterDefString(2)=" + clusterDefString);
		}

		if (result == null && "".equals(clusterDefString)) {
System.out.println("\treadMachineTree 1b");
			result = readMachineTree(defName);
		}

		return result;
  }

  /**
   * Method for non-lazy initializations.
   */
  private final void init(Tree<NameNum> machineTree) {
    this.group2node = new HashMap<String, Tree<NameNum>>();

		if (machineTree == null) return;

    // separate out the groups from the machineTree if top node is "_root_"
    if ("_root_".equals(machineTree.getData().name)) {
      // first child is the machineTree
      final Tree<NameNum> fullTree = machineTree;
      final List<Tree<NameNum>> children = fullTree.getChildren();
      final Iterator<Tree<NameNum>> childIter = children.iterator();

      this.machineTree = childIter.next();

      while (childIter.hasNext()) {
        final Tree<NameNum> node = childIter.next();
        group2node.put(node.getData().name, node);
      }
    }

    group2node.put(ALL_NODES_GROUP, buildAllNodesNode(this.machineTree));
  }

  private final Tree<NameNum> buildAllNodesNode(Tree<NameNum> machineTree) {
    final Tree<NameNum> result = new Tree<NameNum>(new NameNum(ALL_NODES_GROUP, 0));

    boolean didFirst = false;
    for (Iterator<Tree<NameNum>> it = machineTree.iterator(Tree.Traversal.BREADTH_FIRST); it.hasNext(); ) {
      final Tree<NameNum> node = it.next();
      if (didFirst) {
        final NameNum nameNum = node.getData();
        final int numJvms = nameNum.getNumAsCount();
        final String machineName = nameNum.name;
        for (int jvmNum = 0; jvmNum < numJvms; ++jvmNum) {
          final Tree<NameNum> machineNode = new Tree<NameNum>(new NameNum(machineName, jvmNum));
          result.addChild(machineNode);
        }
      }
      else didFirst = true;
    }

    return result;
  }

  private final void fixMachineTree(Tree<NameNum> machineTree, String gateway, String[] machines) {
    final Iterator<Tree<NameNum>> iter = machineTree.iterator(Tree.Traversal.BREADTH_FIRST);

    while (iter.hasNext()) {
      final Tree<NameNum> machineNode = iter.next();
      final NameNum nameNum = machineNode.getData();
      if ("gateway".equals(nameNum.name)) {
        machineNode.setData(new NameNum(gateway, 0));
      }
      if (nameNum.name.startsWith("node")) {
        final int index = Integer.parseInt(nameNum.name.substring(4)) - 1;
        machineNode.setData(new NameNum(machines[index], nameNum.getNumAsId()));
      }
    }
  }

  /**
   * Get the cluster name.
   */
  public String getDefinitionName() {
    return defName;
  }

  /**
   * Get the gateway name.
   */
  public String getGateway() {
    if (gateway == null) {
      gateway = findGateway();
    }
    return gateway;
  }

  private final String findGateway() {
    String result = null;

    final Iterator<Tree<NameNum>> iter = machineTree.iterator(Tree.Traversal.BREADTH_FIRST);
    if (iter.hasNext()) {
      final Tree<NameNum> node = iter.next();  // the first is the gateway
      result = node.getData().name;
    }

    return (result == null) ? "gateway" : result;
  }

  /**
   * Get the path to this definition's cluster definition file.
   */
  public String getClusterDefinitionPath() {
    return getClusterDefinitionPath(defName);
  }

  /**
   * Determine whether this definition is valid.
   */
  public boolean isValid() {
    return machineTree != null;
  }

  /**
   * Get the hierarchy of nodes.
   */
  public Collection<String> getHierarchyStrings(int numIndentSpaces) {
    final Hierarchy<NameNum> hierarchy = new Hierarchy<NameNum>(machineTree);
    return hierarchy.getHierarchyStrings(numIndentSpaces);
  }

  /**
   * Get the unique machine names at the given level.
   *
   * @param level  If 0, get all machines; else get machines at indicated level.
   * @param fix    If true, normalize the machine names.
   *
   * @return the machine names.
   */
  public Collection<String> getMachineNames(int level, boolean fix) {
    final Collection<String> result = new LinkedHashSet<String>();

    if (level == 0) {
      if (_machines != null) {
        // do it this way to preserve the proper ordering of machines identified for a general config.
        // otherwise, the configs must be restricted to number nodes in a breadth first fashion only.
        for (String machine : _machines) {
          result.add(machine);
        }
      }
      else {
        boolean didFirst = false;
        for (Iterator<Tree<NameNum>> it = machineTree.iterator(Tree.Traversal.BREADTH_FIRST); it.hasNext(); ) {
          final Tree<NameNum> node = it.next();
          if (didFirst) {
            final NameNum nameNum = node.getData();
            final String fixedName = fix ? nameNum.name.toLowerCase() : nameNum.name;
            result.add(fixedName);
          }
          else didFirst = true;
        }
      }
    }
    else {
      final List<Tree<NameNum>> nodes = machineTree.treesAtDepth(level);

      for (Tree<NameNum> node : nodes) {
        final NameNum nameNum = node.getData();
        final String fixedName = fix ? nameNum.name.toLowerCase() : nameNum.name;
        result.add(fixedName);
      }
    }

    return result;
  }

  /**
   * Get the name of the cluster node at the top of the hierarchy.
   */
  public String getTopNodeName(boolean fix) {
    String result = machineTree.getData().name;
    return (fix) ? result.toLowerCase() : result;
  }

  public int getNumMachines() {
    int result = 0;

    boolean didFirst = false;
    for (Iterator<Tree<NameNum>> it = machineTree.iterator(Tree.Traversal.BREADTH_FIRST); it.hasNext(); ) {
      final Tree<NameNum> node = it.next();
      if (didFirst) ++result;  // the "gateway" node doesn't count here.
      else didFirst = true;
    }

    return result;
  }

  /**
   * Get the name of each node in the cluster. Note that names with more than
   * one jvm will be duplicated.
   */
  public List<String> getNodeNames() {
    if (nodeNames == null) {
      nodeNames = new ArrayList<String>();
      boolean didFirst = false;
      for (Iterator<Tree<NameNum>> it = machineTree.iterator(Tree.Traversal.BREADTH_FIRST); it.hasNext(); ) {
        final Tree<NameNum> node = it.next();
        if (didFirst) {  // don't count the gateway node
          final NameNum nameNum = node.getData();
          final String name = nameNum.name.toLowerCase();
          for (int i = 0; i < nameNum.getNumAsCount(); ++i) {
            nodeNames.add(name);
          }
        }
        else didFirst = true;
      }
    }
    return nodeNames;
  }

  public int getNumNodes() {
    if (numNodes == 0) {
      boolean didFirst = false;
      for (Iterator<Tree<NameNum>> it = machineTree.iterator(Tree.Traversal.BREADTH_FIRST); it.hasNext(); ) {
        final Tree<NameNum> node = it.next();
        if (didFirst) {  // don't count the gateway node
          final NameNum nameNum = node.getData();
          numNodes += nameNum.getNumAsCount();
        }
        else didFirst = true;
      }
    }
    return numNodes;
  }

  public int getNumNodes(int level) {
    if (level == 0) return getNumNodes();
    else if (level < 0) return 1;

    int result = 0;
    final List<Tree<NameNum>> nodes = machineTree.treesAtDepth(level);

    for (Tree<NameNum> node : nodes) {
      final NameNum nameNum = node.getData();
      result += nameNum.getNumAsCount();
    }

    return result;
  }

  /**
   * Query whether this definition has the given group.
   */
  public final boolean hasGroup(String groupName) {
    return group2node.containsKey(groupName);
  }

  /**
   * Get the number of nodes in the given group.
   */
  public final int getNumGroupNodes(String groupName) {
    final List<Tree<NameNum>> groupNodes = getGroupNodes(groupName);
    return (groupNodes != null) ? groupNodes.size() : 1;
  }

  /**
   * Get the position of the given node within the group.
   */
  public final int getGroupNodePosition(String groupName, String machineName, int jvmNum) {
    int result = 0;

    List<String> groupNodeNames = getGroupNodeNames(groupName, true);

    if (groupNodeNames != null) {
      result = groupNodeNames.indexOf(machineName + "-" + jvmNum);
    }

    return result;
/*
    int result = 0;
    final int limit = getNumGroupNodes(groupName);

    if (machineName != null) {
      final List<Tree<NameNum>> groupNodes = getGroupNodes(groupName);
      machineName = machineName.toLowerCase();
      for (Tree<NameNum> groupNode : groupNodes) {
        final NameNum groupData = groupNode.getData();
        if (groupData.getNumAsId() == jvmNum && machineName.equals(groupData.name.toLowerCase())) {
          break;
        }
        ++result;
      }
    }

    return (result == limit) ? -1 : result;
*/
  }

  /**
   * Get the names of nodes in the given group. If the result is null, then the
   * current node is the only node in the group.
   * <p>
   * Note these are of the form machineName-jvmNum.
   *
   * @param groupName  Group for which to get nodes. If null, then the current
   *                   node is the only node in the group and null is returned.
   *                   Special groupName ALL_NODES_GROUP returns all (but the
   *                   root, or gateway, node).
   * @param fix        If true, normalize the machine names.
   *
   * @return the node names, or null.
   */
  public List<String> getGroupNodeNames(String groupName, boolean fix) {
    final List<String> result = new ArrayList<String>();

    final List<Tree<NameNum>> groupNodes = getGroupNodes(groupName);

    if (groupNodes != null) {
      for (Tree<NameNum> groupNode : groupNodes) {
        result.add(groupNode.getData().asString(fix));
      }
    }
    else {
      result.add(groupName);
    }

    return result;
  }

  public String getJvmBasePath(int jvmNum) {
    return ConfigUtil.CLUSTER_DIR_NAME + "/jvm-" + jvmNum + "/";
  }

  /**
   * Get the named group's nodes or null.
   */
  private final List<Tree<NameNum>> getGroupNodes(String groupName) {
    List<Tree<NameNum>> result = null;

    final String[] pieces = groupName.split("\\s*,\\s*");
    if (pieces.length == 1) {
      result = getSingleGroupNodes(groupName);
    }
    else {
      result = new ArrayList<Tree<NameNum>>();
      for (String piece : pieces) {
        final List<Tree<NameNum>> curResult = getSingleGroupNodes(piece);
        if (curResult != null) {
          result.addAll(curResult);
        }
      }
    }

    return result;
  }

  /**
   * Get the named group's nodes or null.
   */
  private final List<Tree<NameNum>> getSingleGroupNodes(String singleGroupName) {
    List<Tree<NameNum>> result = null;
    final Tree<NameNum> groupRoot = group2node.get(singleGroupName);
    if (groupRoot != null) {
      result = groupRoot.getChildren();
    }
    else {
      // interpret the groupName as a single node name.
      final NameNum singleNode = new NameNum(singleGroupName);
      result = new ArrayList<Tree<NameNum>>();
      result.add(new Tree<NameNum>(singleNode));
    }
    return result;
  }

  public int getNumLevels() {
    return machineTree.maxDepth() - 1;  // subtract out the gateway
  }

  /**
   * Get the names of nodes at the given level.
   * <p>
   * Note these are of the form machineName-jvmNum.
   *
   * @param level  If 0, get all nodes; else get nodes at indicated level.
   * @param fix    If true, normalize the machine names.
   *
   * @return the node names.
   */
  public Collection<String> getNodeNames(int level, boolean fix) {
    final Collection<String> result = new LinkedHashSet<String>();

    if (level == 0) {
      boolean didFirst = false;
      for (Iterator<Tree<NameNum>> it = machineTree.iterator(Tree.Traversal.BREADTH_FIRST); it.hasNext(); ) {
        final Tree<NameNum> node = it.next();
        if (didFirst) {
          final NameNum nameNum = node.getData();
          final String fixedName = fix ? nameNum.name.toLowerCase() : nameNum.name;
          for (int i = 0; i < nameNum.getNumAsCount(); ++i) {
            result.add(fixedName + "-" + i);
          }
        }
        else didFirst = true;
      }
    }
    else {
      final List<Tree<NameNum>> nodes = machineTree.treesAtDepth(level);

      for (Tree<NameNum> node : nodes) {
        final NameNum nameNum = node.getData();
        final String fixedName = fix ? nameNum.name.toLowerCase() : nameNum.name;
        for (int i = 0; i < nameNum.getNumAsCount(); ++i) {
          result.add(fixedName + "-" + i);
        }
      }
    }

    return result;
  }

  /**
   * Get the global (breadth-first) position of the indicated node.
   */
  public int getGlobalPosition(String machineName, int jvmNum) {
    int result = 0;

    boolean didFirst = false;  // skip the "gateway" node
    for (Iterator<Tree<NameNum>> it = machineTree.iterator(Tree.Traversal.BREADTH_FIRST); it.hasNext(); ) {
      final Tree<NameNum> node = it.next();
      if (didFirst) {
        final NameNum nameNum = node.getData();
        if (machineName.equalsIgnoreCase(nameNum.name)) {
          result += jvmNum;
          break;
        }
        else {
          result += nameNum.getNumAsCount();
        }
      }
      else didFirst = true;
    }

    return result;
  }

  /**
   * Get the level of the indicated node, where the node(s) under the gateway
   * node are at level 1, their children are at level 2, etc.
   */
  public int getLevel(String machineName, int jvmNum) {
    //note: in this impl, all jvms of a machine are at the same level.
    final Tree<NameNum> node = findNode(machineName, jvmNum);
    return node.depth();
  }

  private final Tree<NameNum> findNode(String machineName, int jvmNum) {
    Tree<NameNum> result = null;

    boolean didFirst = false;  // skip the "gateway" node
    for (Iterator<Tree<NameNum>> it = machineTree.iterator(Tree.Traversal.BREADTH_FIRST); it.hasNext(); ) {
      final Tree<NameNum> node = it.next();
      if (didFirst) {
        final NameNum nameNum = node.getData();
        if (machineName.equalsIgnoreCase(nameNum.name)) {
          result = node;
          break;
        }
      }
      else didFirst = true;
    }

    return result;
  }

  /**
   * Among all nodes at the given node's level, find its position.
   */
  public int getLocalPosition(String machineName, int jvmNum) {
    int result = 0;

    final Collection<String> nodeNames = getNodeNames(getLevel(machineName, jvmNum), true);
    final String nodeName = machineName.toLowerCase() + "-" + jvmNum;
    for (String curName : nodeNames) {
      if (nodeName.equalsIgnoreCase(curName)) break;
      else ++result;
    }

    return result;
  }


  /**
   * For JUnit testing.
   */
  Tree<NameNum> getMachineTree() {
    return machineTree;
  }

  /**
   * To be overridden for JUnit testing.
   */
  InetSocketAddress getInetSocketAddress(String hostname, String user, int jvmNum) {
    final int serverPort = ConfigUtil.getServerPort(jvmNum, user);
    return new InetSocketAddress(hostname, serverPort);
  }

  private final ServerAddressCache initServerAddresses(String user) {
    final ServerAddressCache result = new ServerAddressCache();

    final List<Tree<NameNum>> allNodes = getGroupNodes(ALL_NODES_GROUP);
    for (Tree<NameNum> node : allNodes) {
      final NameNum nameNum = node.getData();
      final InetSocketAddress serverAddress = getInetSocketAddress(nameNum.name, user, nameNum.getNumAsId());
      result.nameNum2address.put(nameNum, serverAddress);
    }

    return result;
  }

  private final ServerAddressCache getServerAddressCache(String user) {
    ServerAddressCache result = serverAddresses.get(user);
    if (result == null) {
      result = initServerAddresses(user);
      serverAddresses.put(user, result);
    }
    return result;
  }

  /**
   * Get the server address for the given user's identified node.
   *
   * @param user         The user.
   * @param nodeWithNum  The node in the form machineName-jvmNum.
   *
   * @return the server address or null if the user or node are invalid.
   */
  public InetSocketAddress getServerAddress(String user, String nodeWithNum) {
    final ServerAddressCache cache = getServerAddressCache(user);
    return cache.nameNum2address.get(new NameNum(nodeWithNum));
  }

  /**
   * Get the server address for the given user's identified node.
   *
   * @param user         The user.
   * @param machineName  The machine name.
   * @param jvmNum       The jvm number.
   *
   * @return the server address or null if the user or node are invalid.
   */
  public InetSocketAddress getServerAddress(String user, String machineName, int jvmNum) {
    final ServerAddressCache cache = getServerAddressCache(user);
    return cache.nameNum2address.get(new NameNum(machineName, jvmNum));
  }

  /**
   * Get the server address for the given user's identified node.
   *
   * @param user       The user.
   * @param groupName  The name of the group identifying the cluster nodes
   *                   for which to get addresses.  NOTE: null would indicate
   *                   to get the current node's server address, but we require
   *                   the caller to know it's own node's name and number, so
   *                   null will be returned. ALL_NODES_GROUP indicates to get
   *                   all but the gateway's addresses.  Otherwise, the group
   *                   as defined in the cluster definition is used.
   *
   * @return the server addresses or null if the user or level are invalid.
   */
  public InetSocketAddress[] getServerAddresses(String user, String groupName) {
    if (groupName == null) return null;

    final ServerAddressCache cache = getServerAddressCache(user);
    InetSocketAddress[] result = cache.getGroupAddresses(groupName);
    if (result == null) {  // lazy load the cache
      final List<Tree<NameNum>> groupNodes = getGroupNodes(groupName);
      if (groupNodes != null) {
        result = new InetSocketAddress[groupNodes.size()];
        int index = 0;
        for (Tree<NameNum> groupNode : groupNodes) {
          final NameNum nameNum = groupNode.getData();
          final InetSocketAddress address = getSocketAddress(cache, nameNum, groupName);
          result[index++] = address;
        }
        cache.group2addresses.put(groupName, result);
      }
      else {
        // assume the group name is a single node id of the form nodeName-jvmNum
        try {
          result = new InetSocketAddress[]{getSocketAddress(cache, new NameNum(groupName), groupName)};
        }
        catch (NumberFormatException e) {
          result = null;  // bad groupName as nameNum!
        }
      }
    }
    return result;
  }

  private final InetSocketAddress getSocketAddress(ServerAddressCache cache, NameNum nameNum, String groupName) {
    final InetSocketAddress result = cache.nameNum2address.get(nameNum);
    if (result == null) {
      throw new IllegalStateException("Bad group node '" + nameNum + "' in group '" + groupName + "'");
    }
    return result;
  }

  private static final class ServerAddressCache {
    public final Map<NameNum, InetSocketAddress> nameNum2address;
    public final Map<String, InetSocketAddress[]> group2addresses;

    public ServerAddressCache() {
      this.nameNum2address = new HashMap<NameNum, InetSocketAddress>();
      this.group2addresses = new HashMap<String, InetSocketAddress[]>();
    }

    public InetSocketAddress[] getGroupAddresses(String groupName) {
      List<InetSocketAddress> result = null;

      // allow a comma-delimited list of group (and/or nodeName-jvmNum) names
      final String[] pieces = groupName.split("\\s*,\\s*");
      if (pieces.length == 1) {
        // no need to collect, just return the result.
        return group2addresses.get(groupName);
      }
      for (String piece : pieces) {
        final InetSocketAddress[] addresses = group2addresses.get(piece);
        if (addresses != null) {
          if (result == null) result = new ArrayList<InetSocketAddress>();
          for (InetSocketAddress address : addresses) {
            result.add(address);
          }
        }
      }
      
      return (result == null) ? null : result.toArray(new InetSocketAddress[result.size()]);
    }
  }

  private static final class NameNum {
    public final String name;
    private final int num;

    NameNum(String name, int num) {
      this.name = name;
      this.num = num;
    }

    NameNum(String nodeName) {
      final String[] pieces = nodeName.split("-");

      String name = pieces[0];
      int num = 0;

      if (pieces.length > 1) {
        try {
          num = Integer.parseInt(pieces[1]);
        }
        catch (NumberFormatException e) {
          // assume it was a hyphenated name instead of a node-number.
          name = nodeName;
        }
      }
      if (pieces.length > 2) {
        throw new IllegalStateException("Bad node name '" + nodeName + "'! expected for name-num.");
      }

      this.name = name;
      this.num = num;
    }

    public final int getNumAsId() {
      return num;
    }

    public final int getNumAsCount() {
      return (num == 0 ? 1 : num);
    }

    public boolean equals(Object o) {
      boolean result = this == o;

      if (!result && o instanceof NameNum) {
        final NameNum other = (NameNum)o;
        result = (num == other.num) && name.equalsIgnoreCase(other.name);
      }

      return result;
    }

    public int hashCode() {
      return num * 17 + name.toLowerCase().hashCode();
    }

    /**
     * Get in the form of name-num. If fix, then normalize the name.
     */
    public String asString(boolean fix) {
      final String theName = fix ? name.toLowerCase() : name;
      return theName + "-" + num;
    }

    public String toString() {
      return (getNumAsCount() != 1) ? name + "-" + num : name;
    }
  }

  private static final class NameNumTreeBuilderStrategy extends SimpleTreeBuilderStrategy<NameNum> {
    public NameNum constructCoreNodeData(String data) {
      return new NameNum(data);
    }
  }
}
