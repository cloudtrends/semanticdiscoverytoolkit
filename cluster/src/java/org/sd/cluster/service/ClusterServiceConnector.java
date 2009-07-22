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
package org.sd.cluster.service;


import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.sd.cluster.config.ClusterDefinition;
import org.sd.cluster.config.Console;
import org.sd.cluster.io.SafeDepositMessage;

/**
 * A service connector for connecting to a cluster as the server.
 * <p>
 * @author Spence Koehler
 */
public class ClusterServiceConnector extends ServiceConnector {

  //
  // This class takes care of creating, storing, and closing consoles for
  // accessing the clusters.
  //
  private List<ConsoleInfo> consoles;


  /**
   * Properties:
   * <ul>
   * <li>ServiceConnector properties.</li>
   * <li>clusterGateway -- (optional, default=localhost) Gateway to use for
   *                       the cluster.</li>
   * <li>clusterDefDir -- (optional) Path to directory containing cluster
   *                      definitions referenced by clusterDef[N] properties.
   * <li>clusterDef[N] -- (required) Specifies which cluster definition(s)
   *                      to use. N can be empty or consecutive integers
   *                      starting from 1 (e.g. clusterDef1, clusterDef2,
   *                      etc.) If N is empty, the single clusterDef is
   *                      used. If clusterDef is empty, then consecutively
   *                      numbered definitions are loaded. A cluster definition
   *                      is of the form "AmBn.x[-y]+" and is searched for
   *                      within the clusterDefDir (if defined) or the
   *                      default "clusters" dir. An example of a clusterDef
   *                      is "clusterUser 2m2n.1-1 machine1,machine2"</li>
   * <li>nodesToContact[N] -- (optional, default=all) Comma-delimited list
   *                          of nodes or groups to contact within each
   *                          corresponding defined cluster.</li>
   * </ul>
   */
  public ClusterServiceConnector(Properties properties) {
    super(properties);

    // build console(s) to access cluster(s)
    this.consoles = buildConsoles(properties);
  }

  /**
   * Get the process controller for this connector.
   */
  protected ProcessController getProcessController() {

    //
    // build a new process controller for each request because it collects
    // and maintains responses for the task. the processController instance
    // will be stored in a ProcessHandle and should be closed when the
    // processHandle is released.
    //
    return new ClusterProcessController(serviceID, consoles);
  }

  /**
   * Build the process handle for this connector.
   */
  protected ProcessHandle buildProcessHandle(String serviceKey, SafeDepositMessage serviceTask,
                                             long requestTimeout, long withdrawalTimeout,
                                             boolean verbose) {
    //
    // build a new process handle for each request.
    //
    return new ClusterProcessHandle(serviceKey, getProcessController(), serviceTask,
                                    requestTimeout, withdrawalTimeout,
                                    verbose);
  }

  /**
   * Close the resources associated with this instance.
   */
  public void close() {
    // shutdown the consoles
    if (consoles != null) {
      for (ConsoleInfo consoleInfo : consoles) {
        consoleInfo.console.shutdown();
      }
    }

    // shutdown super's resources
    super.close();
  }

  /**
   * Properties:
   * <ul>
   * <li>clusterGateway -- (optional, default=localhost) Gateway to use for
   *                       the cluster.</li>
   * <li>clusterDefDir -- (optional) Path to directory containing cluster
   *                      definitions referenced by clusterDef[N] properties.
   * <li>clusterDef[N] -- (required) Specifies which cluster definition(s)
   *                      to use. N can be empty or consecutive integers
   *                      starting from 1 (e.g. clusterDef1, clusterDef2,
   *                      etc.) If N is empty, the single clusterDef is
   *                      used. If clusterDef is empty, then consecutively
   *                      numbered definitions are loaded. A cluster definition
   *                      is of the form "AmBn.x[-y]+" and is searched for
   *                      within the clusterDefDir (if defined) or the
   *                      default "clusters" dir. An example of a clusterDef
   *                      is "clusterUser 2m2n.1-1 machine1,machine2"</li>
   * <li>nodesToContact[N] -- (optional, default=all) Comma-delimited list
   *                          of nodes or groups to contact within each
   *                          corresponding defined cluster.</li>
   * </ul>
   */
  private final List<ConsoleInfo> buildConsoles(Properties properties) {
    List<ConsoleInfo> result = new ArrayList<ConsoleInfo>();
    final String gateway = properties.getProperty("clusterGateway", "localhost");
    final String clusterDefDir = properties.getProperty("clusterDefDir");
    String clusterDef = properties.getProperty("clusterDef");
    if (clusterDef == null) {  // look for clusterDefN for N=1..
      for (int i = 1; true; ++i) {
        clusterDef = properties.getProperty("clusterDef" + i);
        if (clusterDef == null) break;  // all done
        result.add(buildConsoleInfo(gateway, clusterDefDir, clusterDef, properties.getProperty("nodesToContact" + i, "all"), serviceID));
      }
    }
    else {
      result.add(buildConsoleInfo(gateway, clusterDefDir, clusterDef, properties.getProperty("nodesToContact", "all"), serviceID));
    }

    if (result.size() == 0) {
      throw new IllegalArgumentException("Must define 'clusterDef[N]'!");
    }

    return result;
  }

  private final ConsoleInfo buildConsoleInfo(String gateway, String clusterDefDir, String clusterDefString, String nodesToContact, String serviceID) {
    final String[] clusterDefParts = clusterDefString.split("\\s+");
    if (clusterDefParts.length != 3) {
      throw new IllegalStateException("Improper format for clusterDefString '" + clusterDefString +
                                      "'! Must be \"user cluster_def_name machine_1,machine_2,...,machine_n\"");
    }
    final String clusterUserName = clusterDefParts[0];
    final String clusterDefName = clusterDefParts[1];
    final String clusterMachineNames = clusterDefParts[2];

    ClusterDefinition clusterDef = null;
    try {
      clusterDef = new ClusterDefinition(clusterUserName, clusterDefName, gateway, clusterMachineNames.split("\\s*,\\s*"), clusterDefDir);
    }
    catch (IOException e) {
      throw new IllegalStateException(e);
    }
    final Console console = new Console(clusterDef, serviceID + "Console");

    return new ConsoleInfo(console, nodesToContact);
  }


  public static final class ConsoleInfo {
    public final Console console;
    public final String[] nodesToContact;

    /**
     * Construct with the given console and non-null comma-delimited nodes
     * string.
     */
    ConsoleInfo(Console console, String nodesToContactString) {
      this.console = console;
      this.nodesToContact = nodesToContactString.split("\\s*,\\s*");
    }
  }
}
