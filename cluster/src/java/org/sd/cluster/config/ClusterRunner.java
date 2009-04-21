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
import java.util.Properties;

/**
 * Utility to run classes within the cluster.
 * <p>
 * @author Spence Koehler
 */
public class ClusterRunner {
  
  private String _defName;
  private String _gateway;
  private String[] _machines;
  private String _user;
  private ClusterDefinition _clusterDef;
  private Console _console;
  private String _groupName;

  public ClusterRunner() {
    this._defName = null;
    this._machines = null;
    this._user = null;
    this._clusterDef = null;
    this._console = null;
    this._groupName = null;
  }

  public ClusterRunner(String defName, String[] machines, String user) {
    this(defName, machines, user, null);
  }

  public ClusterRunner(String defName, String[] machines, String user, String gateway) {
    this._defName = defName;
    this._gateway = gateway == null ? "localhost" : gateway;
    this._machines = machines;
    this._user = user;
    this._clusterDef = null;
    this._console = null;
    this._groupName = null;
  }

  public ClusterRunner(Properties properties) {
    this();

    if (properties != null) {
			// cluster home (root dir)
			final String defaultHome = properties.getProperty("CLUSTER_HOME");
			final String clusterHome = properties.getProperty("clusterHome", defaultHome);
			if (clusterHome != null) ConfigUtil.setClusterRootDir(clusterHome);

			// override port range
			final String defaultPortRange = properties.getProperty("CLUSTER_PORT_RANGE");
			final String portRangeString = properties.getProperty("portRange", defaultPortRange);
			if (portRangeString != null) {
				final String[] portRangePieces = portRangeString.split(":");
				final int lowPort = Integer.parseInt(portRangePieces[0]);
				final int highPort = portRangePieces.length > 1 ? Integer.parseInt(portRangePieces[1]) : lowPort;
				ConfigUtil.setPortOverride(lowPort, highPort);
			}

			// default gateway
			final String defaultGateway = properties.getProperty("CLUSTER_GATEWAY");

      this._defName = properties.getProperty("defName");
      this._gateway = properties.getProperty("gateway", defaultGateway);
      this._machines = PropertiesParser.getStrings(properties, "machines");
      this._user = properties.getProperty("user");
      this._clusterDef = null;
      this._console = null;
      this._groupName = properties.getProperty("groupName");
    }
  }

  public void setDefName(String defName) {
    this._defName = defName;
  }

  public void setMachines(String[] machines) {
    this._machines = machines;
  }

  public void setUser(String user) {
    this._user = user;
  }

  public void setClusterDefinition(ClusterDefinition clusterDef) {
    this._clusterDef = clusterDef;
  }

  public void setConsole(Console console) {
    this._console = console;
  }

  public String getDefName() {
    if (_defName == null) {
      _defName = ConfigUtil.getActiveClusterName();
    }
    return _defName;
  }

  public String[] getMachines() {
    if (_machines == null) {
      _machines = ConfigUtil.getActiveClusterMachines();
    }
    return _machines;
  }

  public String getUser() {
    if (_user == null) {
      _user = ExecUtil.getUser();
    }
    return _user;
  }

  public ClusterDefinition getClusterDefinition() throws IOException {
    if (_clusterDef == null) {
			final int[] portOverride = Admin.getActivePortOverride();
			if (portOverride != null) {
				ConfigUtil.setPortOverride(portOverride[0], portOverride[1]);
			}

      final String defName = getDefName();
      final String[] machines = getMachines();
			final Tree<String> clusterTree = Admin.getActiveClusterDefTree();
      _clusterDef = new ClusterDefinition(defName, clusterTree, _gateway, machines);
    }
    return _clusterDef;
  }

  public Console getConsole() throws IOException {
    if (_console == null) {
      final String user = getUser();
      final ClusterDefinition clusterDef = getClusterDefinition();
      _console = new Console(user, clusterDef, "ClusterRunner.console");
      if (_groupName != null) _console.setDefaultGroup(_groupName);
    }
    return _console;
  }

  public void close() {
    if (_console != null) {
      _console.shutdown();
    }
  }
}
