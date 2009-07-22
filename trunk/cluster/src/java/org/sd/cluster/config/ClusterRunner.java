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


import org.sd.util.ExecUtil;
import org.sd.util.PropertiesParser;
import org.sd.util.tree.Tree;

import java.io.IOException;
import java.util.Date;
import java.util.Properties;

/**
 * Utility to run classes within the cluster.
 * <p>
 * @author Spence Koehler
 */
public class ClusterRunner {
  
  public static final String CLUSTER_PROPERTIES = "/cluster.properties";

  //NOTE: Environment variables are accessible as properties and serve as fallbacks

  /**
   * The "Cluster Home" identifies the name of the property whose value
   * is the path to the cluster's home directory, which defaults to "~/cluster".
   */
  public static final String CLUSTER_HOME_ENV = "CLUSTER_HOME";
  public static final String CLUSTER_HOME_PROPERTY = "clusterHome";

  /**
   * The "Cluster Port Range" identifies the name of the property whose value
   * is of the form "lowPort:highPort" for the low and high port ranges to be
   * assigned to the nodes in the cluster.
   */
  public static final String CLUSTER_PORT_RANGE_ENV = "CLUSTER_PORT_RANGE";
  public static final String CLUSTER_PORT_RANGE_PROPERTY = "portRange";


  private boolean useActiveCluster;
  private String prefix;
  private Properties _properties;
  private ClusterDefinition _clusterDef;
  private Console _console;
  private String _groupName;

  private static Properties _clusterProperties = null;

  ClusterRunner() {
    this.useActiveCluster = false;
    this.prefix = null;
    this._properties = null;

    this._clusterDef = null;
    this._console = null;
    this._groupName = null;

    ConfigUtil.setPortOverride(System.getenv("CLUSTER_PORT_RANGE"));
  }

  /**
   * Construct using the given properties without using the active cluster and
   * with no prefix.
   */
  public ClusterRunner(Properties properties) {
    this(null, false, properties);
  }

  /**
   * Construct with the given settings and no prefix.
   */
  public ClusterRunner(boolean useActiveCluster, Properties properties) {
    this(null, useActiveCluster, properties);
  }

  /**
   * Construct with the given settings.
   */
  public ClusterRunner(String prefix, boolean useActiveCluster, Properties properties) {
    this();

    this.prefix = prefix;
    this.useActiveCluster = useActiveCluster;

    // get/load cluster properties for fallback
    final Properties clusterProperties = getClusterProperties();
    this._properties =
      properties == null ? clusterProperties :
      PropertiesParser.combineProperties(new Properties[] {
          clusterProperties,
          properties,
        });

    // cluster home (root dir)
    final String defaultHome = PropertiesParser.getProperty(_properties, prefix, ClusterRunner.CLUSTER_HOME_ENV);
    final String clusterHome = PropertiesParser.getProperty(_properties, prefix, ClusterRunner.CLUSTER_HOME_PROPERTY, defaultHome);
    if (clusterHome != null) ConfigUtil.setClusterRootDir(clusterHome);

    // override port range
    final String defaultPortRange = PropertiesParser.getProperty(_properties, prefix, ClusterRunner.CLUSTER_PORT_RANGE_ENV);
    final String portRangeString = PropertiesParser.getProperty(_properties, prefix, ClusterRunner.CLUSTER_PORT_RANGE_PROPERTY, defaultPortRange);
    ConfigUtil.setPortOverride(portRangeString);

    this._clusterDef = null;
    this._console = null;
    this._groupName = _properties.getProperty("groupName");
  }

  /**
   * Set this runner's flag to use the active cluster to the given value.
   */
  public void setUseActiveCluster(boolean useActiveCluster) {
    this.useActiveCluster = useActiveCluster;
  }

  public void setClusterDefinition(ClusterDefinition clusterDef) {
    this._clusterDef = clusterDef;
  }

  public void setConsole(Console console) {
    this._console = console;
  }

  public String getGroupName() {
    if (_groupName == null) {
      _groupName = _properties != null ? _properties.getProperty("groupName") : null;
    }
    return _groupName;
  }

  public ClusterDefinition getClusterDefinition() throws IOException {
    if (_clusterDef == null) {
      if (useActiveCluster) {
        _clusterDef = getActiveClusterDefinition();
      }
      else if (_properties != null) {
        _clusterDef = ClusterDefinition.getClusterDefinition(prefix, _properties);
      }

      if (_clusterDef == null || !_clusterDef.isValid()) {  // fallback to active cluster
        _clusterDef = getActiveClusterDefinition();
      }
    }
    return _clusterDef;
  }

  public final ClusterDefinition getActiveClusterDefinition() throws IOException {
    ClusterDefinition result = null;

    final Tree<String> clusterTree = Admin.getActiveClusterDefTree();
    if (clusterTree != null) {
      final int[] portOverride = Admin.getActivePortOverride();
      if (portOverride != null) {
        ConfigUtil.setPortOverride(portOverride[0], portOverride[1]);
      }

      final String defName = Admin.getActiveClusterName();
      final String[] machines = Admin.getActiveMachines();
      String gateway = Admin.getActiveGateway();
      if (gateway == null) gateway = "localhost";  // default

      result = new ClusterDefinition(ExecUtil.getUser(), defName, clusterTree, gateway, machines);
    }

    return result;
  }

  public Console getConsole() throws IOException {
    if (_console == null) {
      final ClusterDefinition clusterDef = getClusterDefinition();
      _console = new Console(clusterDef, "ClusterRunner.console");
      final String groupName = getGroupName();
      if (groupName != null) _console.setDefaultGroup(groupName);
    }
    return _console;
  }

  public void close() {
    if (_console != null) {
      _console.shutdown();
    }
  }


  /**
   * (Lazy)Load or Retrieve the cluster.properties.
   */
  public static final Properties getClusterProperties() {
    if (_clusterProperties == null) {
      try {
        _clusterProperties = PropertiesParser.loadProperties(null, CLUSTER_PROPERTIES, true/*verbose*/);
      }
      catch (IOException e) {
        System.err.println(new Date() + ": WARNING: ClusterRunner can't getClusterProperties!" + e);
        e.printStackTrace(System.err);
        _clusterProperties = new Properties();
      }
    }
    return _clusterProperties;
  }
}
