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

import java.io.File;
import java.io.FilenameFilter;
import java.util.List;

/**
 * General static utilties for config.
 * <p>
 * NOTE: these utilities need to be used in the proper context. The class is
 *       package protected to prevent improper use. For a class to access
 *       what this class provides, it should use Config and ClusterDefinition
 *       accessors.
 *
 * @author Spence Koehler
 */
public class ConfigUtil {

  /**
   * Dispatcher listens on port user-port-base + 10
   */
  public static int DISPATCHER_PORT_OFFSET = 10;

  /**
   * Listeners listen on port user-port-base + LISTENER_BASE_PORT_OFFSET + listenerNum [0..MAX_NUM_LISTENERS)
   */
  public static int LISTENER_PORT_OFFSET = DISPATCHER_PORT_OFFSET + 1;

  /**
   * Name of cluster directory relative to user's home.
   */
  public static final String CLUSTER_DIR_NAME = "cluster";

	private static String clusterRootDir = ExecUtil.getUserHome() + "/" + CLUSTER_DIR_NAME + "/";
	private static int[] portOverride = null;

  /**
   * Get the active cluster name if possible.
   *
   * @return the active cluster name or null.
   */
  public static String getActiveClusterName() {
    String result = null;

    String activeClusterNameFile = getClusterConfDir() + "active-cluster-name.txt";
    result = FileUtil.readAsStringIfCan(activeClusterNameFile);
    if (result == null) {
      // can't get it from cluster environment, see if we can from development environment.
      activeClusterNameFile = getClusterDevConfDir() + "active-cluster-name.txt";
      result = FileUtil.readAsStringIfCan(activeClusterNameFile);
    }

    return result;
  }

  public static String[] getActiveClusterMachines() {
    List<String> result = null;

    String activeClusterMachinesFile = getClusterConfDir() + "active-machines.txt";
    result = FileUtil.readLinesIfCan(activeClusterMachinesFile);
    if (result == null) {
      // can't get it from cluster environment, see if we can from development environment.
      activeClusterMachinesFile = getClusterDevConfDir() + "active-machines.txt";
      result = FileUtil.readLinesIfCan(activeClusterMachinesFile);
    }

    return (result == null) ? null : result.toArray(new String[result.size()]);
  }

  /**
   * Get the cluster root directory path for the current user.
   */
  public static String getClusterRootDir() {
		return clusterRootDir;
  }

	/**
	 * Set (override) the cluster root directory.
	 * <p>
	 * If never called, then the clusterRootDir defaults to: "~/cluster/"
	 */
	public static void setClusterRootDir(String newClusterRootDir) {
		clusterRootDir = newClusterRootDir;
		if (clusterRootDir.charAt(clusterRootDir.length() - 1) != '/') {
			clusterRootDir = clusterRootDir + "/";
		}
	}

  /**
   * Get the cluster directory path for the current user.
   */
  public static String getClusterPath(String pathFromClusterRoot) {
    return getClusterRootDir() + pathFromClusterRoot;
  }

  /**
   * Get the cluster configuration directory for the current user.
   */
  public static String getClusterConfDir() {
    return getClusterRootDir() + "conf/";
  }

  /**
   * Get the cluster bin directory for the current user.
   */
  public static String getClusterBinDir() {
    return getClusterRootDir() + "bin/";
  }

  public static String getClusterDevDir() {
		// try to get from environment variable
		String result = System.getenv("CLUSTER_DEV_DIR");

		if (result == null) {
			// try to get relative to this class (NOTE: won't work when we're using the jar!)
			result = FileUtil.getFilename(ConfigUtil.class, "ConfigUtil.class");
			final int pos = (result != null) ? result.indexOf("/cluster/") : -1;
			result = (pos >= 0) ? result.substring(0, pos + 9) : null;
		}

		return result;
  }

  /**
   * Get the cluster configuration directory for the current user.
   */
  public static String getClusterDevConfDir() {
    return getClusterDevDir() + "conf/";
  }

  /**
   * Get the cluster bin directory for the current user.
   */
  public static String getClusterDevBinDir() {
    return getClusterDevDir() + "bin/";
  }

	/**
	 * Set a low and high port overrides to use instead of the UsersCsv lookup
	 * table.
	 */
	public static void setPortOverride(int lowPortOverride, int highPortOverride) {
		portOverride = new int[]{lowPortOverride, highPortOverride};
	}

	/**
	 * Get the port override.
	 * <p>
	 * Note that this will be null if no override has been set.
	 */
	public static int[] getPortOverride() {
		return portOverride;
	}

  /**
   * Get the server port for the given jvmNum and the current user.
   *
   * @return the server port or 0.
   */
  public static final int getServerPort(int jvmNum) {
    return getServerPort(jvmNum, ExecUtil.getUser());
  }

  /**
   * Get this jvm's server port for the given jvmNum based on the given user.
   *
   * @return the server port or 0.
   */
  public static final int getServerPort(int jvmNum, String user) {
    if (user == null) user = ExecUtil.getUser();

		int serverPort = 0;
		int highPort = 0;

		if (portOverride != null) {
			serverPort = portOverride[0] + jvmNum;  // lowPortOverride + jvmNum
			highPort = portOverride[1];             // highPortOverride
		}
		else {
			final UsersCsv usersCsv = UsersCsv.getInstance();
			serverPort = usersCsv.getLowPort(user) + jvmNum;
			highPort = usersCsv.getHighPort(user);
		}

    if (serverPort > highPort) {
      throw new IllegalStateException("Too many jvms -- can't use another server port for jvmNum=" + jvmNum + " (maxPort=" + Integer.toString(highPort) + ")!");
    }

    return serverPort;
  }

  /**
   * Get the existing jvm dirs on this machine.
   */
  public static final File[] findJvmDirs() {
    return new File(getClusterRootDir()).listFiles(new FilenameFilter() {
        public boolean accept(File dir, String name) {
          return name.startsWith("jvm-");
        }
      });
  }


  public static void main(String[] args) {
    final int processId = ExecUtil.getProcessId();

    System.out.println("user=" + ExecUtil.getUser());
    System.out.println("machineName=" + ExecUtil.getMachineName());
    System.out.println("clusterRootDir=" + getClusterRootDir());
    System.out.println("clusterPath(data/)=" + getClusterPath("data/"));
    System.out.println("processId=" + processId);
    System.out.println("isUp(" + processId + ")=" + ExecUtil.isUp(processId));
    System.out.println("isUp(" + Integer.toString(processId - 1) + ")=" + ExecUtil.isUp(processId - 1));


    // this'll loop, keeping the jvm alive so we can check whether the processId is correct.
    if (true) {
      System.out.println("processId=" + processId);
      while (ExecUtil.isUp(processId)) {
        final int curProcessId = ExecUtil.getProcessId();
        if (processId != curProcessId) {
          System.err.println("oops -- processId changed! (from " + processId + " to " + curProcessId + ")");
        }
      }
      System.err.println("process " + processId + " ended!");
    }
  }

  protected static final String escape(String command) {
    return command.replaceAll("&", "\\&");
  }
}
