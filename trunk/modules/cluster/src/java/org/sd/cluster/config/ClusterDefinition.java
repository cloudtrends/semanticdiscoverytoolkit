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
import org.sd.util.ExecUtil;
import org.sd.util.PropertiesParser;
import org.sd.util.tree.Hierarchy;
import org.sd.util.tree.SimpleTreeBuilder;
import org.sd.util.tree.SimpleTreeBuilderStrategy;
import org.sd.util.tree.Tree;
import org.sd.util.tree.TreeBuilder;
//import org.sd.util.tree.TreeBuilderFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Class to encapsulate a cluster definition.
 * <p>
 * @author Spence Koehler
 */
public class ClusterDefinition {
  
  /**
   * The "Config Resources" identifies the location of the cluster config
   * resources directory to override the default directory of Config.class/resources.
   */
  public static final String CLUSTERS_DIR_ENV = "CLUSTERS_DIR";
  public static final String CLUSTERS_DIR_PROPERTY = "clustersDir";

  /**
   * Default location of 'clusters' from Config.class.
   */
  public static final String CLUSTER_DEFINITIONS_PATH = "resources/clusters/";

  public static final String ALL_NODES_GROUP = "_ALL_";

  /**
   * The "Cluster Gateway" identifies the name of the property whose value
   * is the gateway machine (name or IP) through which the cluster is deployed.
   */
  public static final String CLUSTER_GATEWAY_ENV = "CLUSTER_GATEWAY";
  public static final String CLUSTER_GATEWAY_PROPERTY = "gateway";

  /**
   * The "Cluster Definition Name" identifies the name of the property whose
   * value is the name of the persisted cluster definition to use.
   */
  public static final String CLUSTER_DEFINITION_NAME_PROPERTY = "cluterDefName";  // defName

  /**
   * The "Cluster Definition" identifies the name of the property whose
   * value is "user defName machines" identifying the cluster to use.
   */
  public static final String CLUSTER_DEFINITION_PROPERTY = "clusterDef";

  /**
   * The "Cluster Machines" identifies the name of the property whose value
   * is the names (or IP addresses) of machines participating in the cluster
   * in a comma-delimited string.
   */
  public static final String CLUSTER_MACHINES_PROPERTY = "clusterMachines";  // machines

  /**
   * The "Cluster User Name" identifies the name of the property whose value
   * is the user name under which the cluster is running.
   */
  public static final String CLUSTER_USER_PROPERTY = "clusterUser";  // user

  /**
   * The "Group Definition" identifies a dynamic cluster configuration.
   * <p>
   * NOTE: Either generic ('node-') or specific ('machineA') machine names are
   *       acceptable, but generic naming requires the machines to be defined
   *       separately.
   */
  public static final String CLUSTER_GROUP_DEFINITION_PROPERTY = "clusterGroupDef";


  /**
   * Get the (first) cluster definition identified in the given properties.
   * <p>
   * See getClusterDefinitions for a description of the properties.
   */
  public static final ClusterDefinition getClusterDefinition(Properties properties) {
    return getClusterDefinition(null, properties);
  }

  /**
   * Get the (first) cluster definition identified in the given properties.
   * <p>
   * See getClusterDefinitions for a description of the properties.
   */
  public static final ClusterDefinition getClusterDefinition(String prefix, Properties properties) {
    ClusterDefinition result = null;

    final List<ClusterDefinition> clusterDefs = getClusterDefinitions(prefix, properties);

    if (clusterDefs != null && clusterDefs.size() > 0) {
      result = clusterDefs.get(0);
    }

    return result;
  }

  /**
   * Get the cluster definitions identified in the given properties.
   *
   * @param prefix  Prefix to use (as prefix.key) when retrieving definition
   *                properties. (Okay if null.)
   * @param properties  Properties identifying definition parameters.
   *
   * Properties:
   * <ul>
   * <li>'gateway'</li>
   * <li>ClusterDefinition Properties (choose 1 of 3 different ways to define)
   *   <ul>
   *     <li>Using a persisted configuration...
   *       <ul>
   *         <li>'clusterDefDir' (optional)</li>
   *         <ul>...with a single property for combined pieces of information
   *           <li>'clusterDef' == "user defname machines"; where
   *             <ul>
   *               <li>'user' is the cluster user</li>
   *               <li>'defname' is the persisted definition name e.g. 1m1n.1 to be found at "clusterDefDir/1m1n.1.def"</li>
   *               <li>'machines' is the comma-delimited list of machines to use with defname.</li>
   *             </ul>
   *           </li>
   *         </ul>
   *         <ul>...with a property for each piece of information:
   *           <li>'clusterUser' is the cluster user</li>
   *           <li>'clusterDefName' (same format as 'defname' above)</li>
   *           <li>'clusterMachines' (same format as 'machines' above)</li>
   *         </ul>
   *       </ul>
   *     </li>
   *     <li>Using an on-the-fly generated configuration:
   *       <ul>
   *         <li>'clusterUser' is the cluster user</li>
   *         <li>'clusterMachines' (same format as 'machines' above)</li>
   *         <li>'clusterGroupDef' == "groupNameA:nodeDefs1 groupNameB:nodeDefs2 ..."; where
   *           <ul>
   *             <li>'groupName' is a group name for the nodeDefs</li>
   *             <li>'nodeDefs' == "nodeDef1,nodeDef2,..."</li>
   *             <li>'nodeDef' == "nodeN-C" or "machine-C"</li>
   *             <li>'node' is literally 'node'</li>
   *             <li>'N' is the number (1-based) for the node's machine</li>
   *             <li>'C' is the number of jvms (nodes) to run on the machine</li>
   *             <li>'machine' is the hostname of a machine</li>
   *           </ul>
   *         </li>
   *       </ul>
   *     </li>
   *      e.g. for a 3 node cluster using machines "machA" with 1 node,
   *      "machB" with 2 nodes, and "machC" with 2 nodes and having
   *      machA as a "controller" group while machB and machC nodes
   *      are all in the "processor" group, either of the following
   *      forms can be used:
   *     <ul>
   *       <li>clusterDef="user 3m5n.1-4 machA,machB,machC"</li>
   *       <li>groupDefs="controller:machA-1 processor:machB-2,machC-2" clusterUser="user"</li>
   *       <li>groupDefs="controller:node1-1 processor:node2-2,node3-2" clusterUser="user" clusterMachines="machA,machB,machC"</li>
   *     </ul>
   *   </ul>
   * </li>
   * </ul>
   */
  public static final List<ClusterDefinition> getClusterDefinitions(String prefix, Properties properties) {
    List<ClusterDefinition> result = new ArrayList<ClusterDefinition>();

    // config resources dir
    final String defaultClustersDir = PropertiesParser.getProperty(properties, prefix, ClusterDefinition.CLUSTERS_DIR_ENV);
    final String clusterDefDir = PropertiesParser.getProperty(properties, prefix, ClusterDefinition.CLUSTERS_DIR_PROPERTY, defaultClustersDir);

    // gateway
    final String[] defaultGateways = PropertiesParser.getMultiValues(properties, prefix, ClusterDefinition.CLUSTER_GATEWAY_ENV);
    String[] gateways = PropertiesParser.getMultiValues(properties, prefix, ClusterDefinition.CLUSTER_GATEWAY_PROPERTY, defaultGateways);
    if (gateways == null || gateways.length == 0) gateways = new String[]{"localhost"};  // final fallback is to localhost.

    // Check for persisited definition(s)
    final String[] defNames = PropertiesParser.getMultiValues(properties, prefix, ClusterDefinition.CLUSTER_DEFINITION_NAME_PROPERTY);

    // Check for user/machine definition(s)/override(s)
    final String[] users = PropertiesParser.getMultiValues(properties, prefix, ClusterDefinition.CLUSTER_USER_PROPERTY);
    final String[] machines = PropertiesParser.getMultiValues(properties, prefix, ClusterDefinition.CLUSTER_MACHINES_PROPERTY);

    // Check for combined persisted definition(s)
    final String[] clusterDefs = PropertiesParser.getMultiValues(properties, prefix, ClusterDefinition.CLUSTER_DEFINITION_PROPERTY);

    // Check for dynamic ("on-the-fly") config(s)
    final String[] groupDefs = PropertiesParser.getMultiValues(properties, prefix, ClusterDefinition.CLUSTER_GROUP_DEFINITION_PROPERTY);


    if (groupDefs != null) {  // use ConfigGenerator
      for (int index = 0; index < groupDefs.length; ++index) {
        final String groupDef = groupDefs[index];
        final String theGateway = getMatchOrLast(index, gateways, "localhost");
        final String[] theMachines = getMachines(index, machines);
        final String theUser = getMatchOrLast(index, users, ExecUtil.getUser());

        final String[] splitDefs = groupDef.split("\\s+");
        final ConfigGenerator configGenerator = new ConfigGenerator(splitDefs, 0);
        final ClusterDefinition clusterDef = configGenerator.buildClusterDefinition(theGateway, theUser, theMachines);

        if (clusterDef != null) {
          result.add(clusterDef);
        }
      }
    }
    else if (clusterDefs != null) {
      for (int index = 0; index < clusterDefs.length; ++index) {
        final String theClusterDef = clusterDefs[index];
        final String[] defPieces = theClusterDef.split("\\s+");  // user defname [machines]
        final String defName = getDefName(1, defPieces);
        final String theUser = defPieces[0];
        final String theGateway = getMatchOrLast(index, gateways, "localhost");
        final String[] overrideMachines = getMachines(index, machines);
        final String[] theMachines = (overrideMachines != null) ? overrideMachines : defPieces[2].split("\\s*,\\s*");

        if (defName != null && !"".equals(defName)) {
          try {
            final ClusterDefinition clusterDef = new ClusterDefinition(theUser, defName, theGateway, theMachines, clusterDefDir);
            result.add(clusterDef);
          }
          catch (IOException e) {
            //todo: log error!
          }
        }
      }
    }
    else if (defNames != null) {
      for (int index = 0; index < defNames.length; ++index) {
        final String defName = getDefName(index, defNames);
        final String theGateway = getMatchOrLast(index, gateways, "localhost");
        final String[] theMachines = getMachines(index, machines);
        final String theUser = getMatchOrLast(index, users, ExecUtil.getUser());

        if (defName != null && !"".equals(defName)) {
          try {
            final ClusterDefinition clusterDef = new ClusterDefinition(theUser, defName, theGateway, theMachines, clusterDefDir);
            result.add(clusterDef);
          }
          catch (IOException e) {
            //todo: log error!
          }
        }
      }
    }

    return result;
  }

  /**
   * Utility to get the matching index element or the last available.
   */
  private static final String getMatchOrLast(int index, String[] elements, String defaultValue) {
    String result = null;

    if (elements != null) {
      final int elementIndex = index < elements.length ? index : elements.length - 1;  // last defined element is ok
      result = elements[elementIndex];
    }

    return (result == null || "".equals(result)) ? defaultValue : result;
  }

  private static final String[] getMachines(int index, String[] machines) {
    String[] result = null;

    final int machinesIndex = machines == null ? -1 : index < machines.length ? index : -1;  // machines index must match
    final String machinesString = (machinesIndex < 0) ? null : machines[machinesIndex];
    if (machinesString != null) {
      result = machinesString.split("\\s*,\\s*");
    }
    else {
      result = ConfigUtil.getActiveClusterMachines();
    }

    return result;
  }

  private static final String getDefName(int index, String[] defNames) {
    String result = null;

    if (defNames != null && defNames.length > index) {
      result = defNames[index];
    }

    return (result == null || "".equals(result)) ? ConfigUtil.getActiveClusterName() : result;
  }

  private static final InputStream findClusterDefinition(String defName) throws IOException {
    return findClusterDefinition(defName, null);
  }

  private static final InputStream findClusterDefinition(String defName, String clusterDefDir) throws IOException {
    InputStream result = null;

    final String ext = defName.endsWith(".def") ? "" : ".def";

    // look in clusterDefDir for the definition
    if (clusterDefDir != null) {
      final File clusterDefDirFile = new File(clusterDefDir);
      if (clusterDefDirFile.exists()) {
        final File file = new File(clusterDefDirFile, defName + ext);
        if (file.exists()) {
          result = FileUtil.getInputStream(file);
        }
      }
    }

    // look in the default area
    if (result == null) {
      result = FileUtil.getInputStream(ClusterDefinition.class, CLUSTER_DEFINITIONS_PATH + defName + ext);
    }

    if (result == null) {
      // try searching classpath
      final Enumeration<URL> urls = ClassLoader.getSystemResources(defName + ext);
      if (urls != null) {
        while (urls.hasMoreElements() && result == null) {
          final URL url = urls.nextElement();
          try {
            final URI uri = url.toURI();
            final File file = new File(uri);
            result = FileUtil.getInputStream(file);
          }
          catch (URISyntaxException e) {
            // ignore and skip
          }
        }
      }
    }

    return result;
  }

  private static TreeBuilder<NameNum> treeBuilder = new SimpleTreeBuilder<NameNum>(new NameNumTreeBuilderStrategy());

  private String user;
  private String defName;
  private String gateway;
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
  ClusterDefinition(String user, String defName, boolean doInit) throws IOException {
    this(user, defName, doInit, null);
  }

  /**
   * Package protected constructor for internal and testing access only.
   */
  ClusterDefinition(String user, String defName, boolean doInit, String clusterDefDir) throws IOException {
    this.user = user;
    this.defName = defName;
    this.gateway = null;

    final InputStream cdInput = getClusterDefinitionInputStream(defName, clusterDefDir);
    this.machineTree = readMachineTree(defName, cdInput);
    if (cdInput != null) cdInput.close();
    if(this.machineTree == null) System.out.println("readMachineTree(" + defName + ") returned null result! defName=" + defName + " clusterDefDir=" + clusterDefDir);
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
  public ClusterDefinition(String user, String defName, String gateway, String[] machines) throws IOException {
    this(user, defName, gateway, machines, null);
  }

  /**
   * Construct with a generic definition file where we will substitute the
   * given gateway for the "gateway" and machine names for the "nodes" in
   * the definition tree.
   */
  public ClusterDefinition(String user, String defName, String gateway, String[] machines, String clusterDefDir) throws IOException {
    this(user, defName, false, clusterDefDir);

    // walk the tree; replace root data with gateway & replacing nodeN-n with machines[N-1]-n
    this.gateway = gateway;
    this._machines = machines;
    fixMachineTree(machineTree, gateway, machines);

    init(machineTree);
  }

  /**
   * Construct with a named definition file that already has the machine names.
   */
  public ClusterDefinition(String user, String defName) throws IOException {
    this(user, defName, true, null);
  }

  /**
   * Construct with the given generic clusterDef tree, substituting the given
   * gateway for the "gateway" and machine names for the "nodes" in the
   * definition tree.
   * <p>
   * This can be used in conjunction with ConfigGenerator in order to create
   * on-the-fly cluster definitions that need not be persisted.
   */
  public ClusterDefinition(String user, String defName, Tree<String> clusterDef, String gateway, String[] machines) {
    this.user = user;
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
   * Get the user associated with this instance.
   */
  public String getUser() {
    return user;
  }

  /**
   * Get this instance's initialization string.
   */
  public String getClusterDefString() {
    return clusterDefString;
  }

  private final InputStream getClusterDefinitionInputStream(String defName, String clusterDefDir) throws IOException {
    InputStream result = null;

    if (defName != null) {
      // First try to find a persisted cluster definition within the deployed code
      result = findClusterDefinition(defName, clusterDefDir);
      if (result == null) {
        // Next look at the "active" cluster
        final File file = Admin.getActiveClusterDefFile();
        if (file.exists()) {
          result = FileUtil.getInputStream(file);
        }
      }
    }

    return result;
  }

  /**
   * Read the machineTree from the default cluster definition's path for the defName.
   */
  private final Tree<NameNum> readDefaultMachineTree(String defName) throws IOException {
    Tree<NameNum> result = null;

    if (defName != null) {
      if (!defName.endsWith(".def")) defName += ".def";
      final InputStream in = FileUtil.getInputStream(this.getClass(), CLUSTER_DEFINITIONS_PATH + defName);
      if (in != null) {
        result = doRead(in);
        in.close();
      }
    }

    System.out.println("ClusterDefinition.readDefaultMachineTree(" + defName + ")... success=" + (result != null));

    return result;
  }

  /**
   * Read the machine tree from the given cluster definition's path for the defName.
   */
  private final Tree<NameNum> readMachineTree(String defName, InputStream cdInputStream) throws IOException {
    Tree<NameNum> result = null;

    System.out.println("ClusterDefinition.readMachineTree(" + defName + ",cdInputStream)... exists=" + (cdInputStream != null));

    if (cdInputStream == null) {
      // use default
      result = readDefaultMachineTree(defName);
    }
    else {
      // read the given stream
      doRead(cdInputStream);

      if (result == null) {
        // fallback to default
        result = readDefaultMachineTree(defName);
      }
    }

    return result;
  }

  /**
   * Do the work of reading a cluster definition from an input stream as a NameNum tree.
   */
  private final Tree<NameNum> doRead(InputStream in) throws IOException {
    Tree<NameNum> result = null;

    if (in != null) {
      this.clusterDefString = FileUtil.readAsString(FileUtil.getReader(in), FileUtil.LINUX_COMMENT_IGNORER);
      if (clusterDefString != null) {
        result = treeBuilder.buildTree(clusterDefString);
      }
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
      if (machines != null && nameNum.name.startsWith("node")) {
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

  public String[] getMachines() {
    return _machines;
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
   * Get the initial user and machine through which to deploy and/or start a
   * cluster.
   * <p>
   * Typically, the initial machine will be the gateway. If the gateway is
   * "localhost" <b>and</b> there is only one node under the gateway node
   * <b>and</b> the gateway user is the same as the cluter user, then the
   * initial machine will be the first node under the gateway.
   * <p>
   * @return {topUser, topMachine}
   */
  public String[] getTopInfo(boolean fix) {
    // [user@]topnode is always top node in machine tree
    String topnode = getTopNodeName(fix);
    final String clusterUser = getUser();
    String user = clusterUser;  // topnode user defaults to the cluster user

    // NOTE: topnode user can be overridden by including it with the gateway
    //       name.
    final int atpos = topnode.indexOf('@');
    if (atpos >= 0) {
      user = topnode.substring(0, atpos);
      topnode = topnode.substring(atpos + 1);
    }

    // check for nullified gateway
    if ("localhost".equals(topnode) && machineTree.numChildren() == 1 && clusterUser.equals(user)) {
      topnode = machineTree.getChildren().get(0).getData().name;
      if (fix) topnode = topnode.toLowerCase();
      System.out.println("bypassing gateway. topnode override=" + topnode);
    }

    return (topnode == null) ? null : new String[]{user, topnode};
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
  InetSocketAddress getInetSocketAddress(String hostname, int jvmNum) {
    final int serverPort = ConfigUtil.getServerPort(jvmNum, user);
    return new InetSocketAddress(hostname, serverPort);
  }

  private final ServerAddressCache initServerAddresses() {
    final ServerAddressCache result = new ServerAddressCache();

    final List<Tree<NameNum>> allNodes = getGroupNodes(ALL_NODES_GROUP);
    for (Tree<NameNum> node : allNodes) {
      final NameNum nameNum = node.getData();
      final InetSocketAddress serverAddress = getInetSocketAddress(nameNum.name, nameNum.getNumAsId());
      result.nameNum2address.put(nameNum, serverAddress);

      System.out.println(new Date() + ": ClusterDefinition(" + defName + ")." + nameNum + "=" + serverAddress);
    }

    return result;
  }

  private final ServerAddressCache getServerAddressCache() {
    ServerAddressCache result = serverAddresses.get(user);
    if (result == null) {
      result = initServerAddresses();
      serverAddresses.put(user, result);
    }
    return result;
  }

  /**
   * Get the server address for this definition's user's identified node.
   *
   * @param nodeWithNum  The node in the form machineName-jvmNum.
   *
   * @return the server address or null if the user or node are invalid.
   */
  public InetSocketAddress getServerAddress(String nodeWithNum) {
    final ServerAddressCache cache = getServerAddressCache();
    return cache.nameNum2address.get(new NameNum(nodeWithNum));
  }

  /**
   * Get the server address for this definition's user's identified node.
   *
   * @param machineName  The machine name.
   * @param jvmNum       The jvm number.
   *
   * @return the server address or null if the user or node are invalid.
   */
  public InetSocketAddress getServerAddress(String machineName, int jvmNum) {
    final ServerAddressCache cache = getServerAddressCache();
    return cache.nameNum2address.get(new NameNum(machineName, jvmNum));
  }

  /**
   * Get the server address for this definition's user's identified node.
   *
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
  public InetSocketAddress[] getServerAddresses(String groupName) {
    if (groupName == null) return null;

    final ServerAddressCache cache = getServerAddressCache();
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
