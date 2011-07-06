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


import org.sd.cluster.io.Message;
import org.sd.cluster.job.GetJobsMessage;
import org.sd.cluster.job.JobCommand;
import org.sd.cluster.job.JobCommandMessage;
import org.sd.cluster.job.LocalJobId;
import org.sd.io.FileUtil;
import org.sd.io.Publishable;
import org.sd.util.ExecUtil;
import org.sd.util.PropertiesParser;
import org.sd.util.ReflectUtil;
import org.sd.util.tree.Tree;
import org.sd.util.tree.TreeBuilderFactory;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;

import java.io.BufferedWriter;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.Date;
import java.util.Properties;

/**
 * Administrative class for performing cluster functions like deploy, start, stop.
 * <p>
 * @author Spence Koehler
 */
public class Admin {
  
  /**
   * Universal auxiliary for sending a message to a running cluster.
   * <p>
   * Properties:
   * <ul>
   * <li>message: (required) className[:buildMethod] for message to send. Must have a 'properties' arg or no-arg constructor. Properties will be passed in.</li>
   * <li>group: (required) comma-delimited group or node names to which message is to be sent.</li>
   * <li>timeout: (optional, default=5000) millis to wait for a resposne from nodes.</li>
   * <li>requireAll: (optional, default=false) flag for throwing an exception if not all responses are received.</li>
   * </ul>
   */
  public static final void sendMessage(String[] args) {
    PropertiesParser pp = null;
    Properties properties = null;

    try {
      pp = new PropertiesParser(args);
      properties = pp.getProperties();
    }
    catch (IOException e) {
      throw new IllegalStateException(e);  // rethrow as a RuntimeException
    }

    sendMessage(properties);
  }

  /**
   * Universal auxiliary for sending a message to a running cluster.
   * <p>
   * Properties:
   * <ul>
   * <li>message: (required) className[:buildMethod] for message to send. Must have a 'properties' arg or no-arg constructor. Properties will be passed in.</li>
   * <li>group: (required) comma-delimited group or node names to which message is to be sent.</li>
   * <li>timeout: (optional, default=5000) millis to wait for a resposne from nodes.</li>
   * <li>requireAll: (optional, default=false) flag for throwing an exception if not all responses are received.</li>
   * </ul>
   */
  public static final void sendMessage(Properties properties) {

    final String messageClassName = properties.getProperty("message");
    if (messageClassName == null) {
      throw new IllegalArgumentException("Must define a 'message' (class name) to send!");
    }
    final Message message = (Message)ReflectUtil.buildInstance(messageClassName, properties);

    final String groupName = properties.getProperty("group");
    if (groupName == null) {
      throw new IllegalArgumentException("Must define a 'group' (comma-delimited group or node names) to send!");
    }

    doSendMessage(message, groupName, properties);
  }

  /**
   * Universal auxiliary for sending a job command message to a running cluster.
   * <p>
   * Properties:
   * <ul>
   * <li>jobCommand: (required) JobCommand enumeration name</li>
   * <li>jobId: (required) id.nodeName-jvmNum or jobDescription.groupName identifying localJobId of command recipients.</li>
   * <li>payload: (optional) className[:buildMethod] for job command message payload to send. Must have a 'properties' arg or no-arg constructor. Properties will be passed in.</li>
   * <li>timeout: (optional, default=5000) millis to wait for a resposne from nodes.</li>
   * <li>requireAll: (optional, default=false) flag for throwing an exception if not all responses are received.</li>
   * </ul>
   */
  public static final void sendJobCommand(Properties properties) {
    // jobCommand
    // jobId (id.nodeName-jvmNum or jobDescription.groupName)
    // payload (optional)
    // timeout
    // requireAll

    final String jobCommandName = properties.getProperty("jobCommand");
    if (jobCommandName == null) {
      throw new IllegalArgumentException("Must define a 'jobCommand' (enum name) to send!");
    }
    final JobCommand jobCommand = Enum.valueOf(JobCommand.class, jobCommandName.toUpperCase());

    final String jobIdString = properties.getProperty("jobId");
    if (jobIdString == null) {
      throw new IllegalArgumentException("Must define a 'jobId' (id.nodeName-jvmNum or jobDescription.groupName) to send to!");
    }
    final LocalJobId jobId = new LocalJobId(jobIdString);

    final String payloadString = properties.getProperty("payload");
    final Publishable payload = (payloadString == null) ? null : (Publishable)ReflectUtil.buildInstance(payloadString, properties);

    final JobCommandMessage message = new JobCommandMessage(jobCommand, jobId, payload);

    doSendMessage(message, jobId.getNodeName(), properties);
  }

  private static final void doSendMessage(Message message, String groupName, Properties properties) {

    final int timeout = Integer.parseInt(properties.getProperty("timeout", "5000"));
    final boolean requireAllResponses = "true".equals(properties.getProperty("requireAll", "false"));

    Console console = null;

    try {
      console = new ClusterRunner(true/*useActiveCluster*/, properties).getConsole();
      console.showResponses(System.out, console.sendMessageToNodes(message, groupName, timeout, requireAllResponses));
    }
    catch (Exception e) {
      if (console != null) {
        console.shutdown();
        console = null;
      }

      throw new IllegalStateException(e);  // rethrow as a RuntimeException.
    }

    if (console != null) console.shutdown();
  }

  private ClusterDefinition clusterDef;
  private PrintStream out;
  private Console console;

  public Admin(ClusterDefinition clusterDef, PrintStream out) {
    this.clusterDef = clusterDef;
    this.out = out;

    this.console = new Console(clusterDef, "Admin");
    console.setDefaultGroup(ClusterDefinition.ALL_NODES_GROUP);
  }

  public void shutdown() {
    console.shutdown();
  }

  public void deploy(String heapSize) throws IOException {
    String command = null;
    ExecUtil.ExecResult execResult = null;

    // create conf/active-*.txt files
    configure(heapSize);

    // rsync build to top clusterDef node (=gateway)
    final String binDir = ConfigUtil.getClusterDevBinDir();

    final String[] topInfo = clusterDef.getTopInfo(true);

    command = "./ddeploy " + topInfo[0] + " " + topInfo[1];
    if (out != null) out.println(command);
    execResult = ExecUtil.executeProcess(command, new File(binDir));
    if (out != null && execResult != null /*&& !execResult.failed()*/ && execResult.output != null) out.println(execResult.output);

    // start top clusterDef node a-deployin'
    final String userName = clusterDef.getUser();
    command = "cd ~/cluster/bin;./hdeploy " + userName;
    if (out != null) out.println("ssh " + topInfo[0] + "@" + topInfo[1] + " " + command);
    execResult = ExecUtil.executeRemoteProcess(topInfo[0], topInfo[1], command);
    if (out != null && execResult != null /*&& !execResult.failed()*/ && execResult.output != null) out.println(execResult.output);
  }

  public void start(String heapSize) throws IOException {
    String command = null;
    ExecUtil.ExecResult execResult = null;

    // create conf/active-*.txt files
    configure(heapSize);

    // tell top to start children. top isn't started because it is the "gateway" into the cluster, not a worker.
    final String userName = clusterDef.getUser();
    final String topNodeName = clusterDef.getTopNodeName(true);
    command = " cd ~/cluster/bin;./hstart";
    if (out != null) out.println("ssh " + userName + "@" + topNodeName + " " + command);
    execResult = ExecUtil.executeRemoteProcess(userName, topNodeName, command);
    if (out != null && execResult != null && !execResult.failed() && execResult.output != null) out.println(execResult.output);

    //todo: wait until all jvms are up and running
  }

  public void stop(String[] theMachines) throws IOException, ClusterException {
    sendMessage(new ShutdownMessage(), theMachines, null);
  }

  public void ping(String[] theMachines) throws IOException, ClusterException {
    sendMessage(new Ping(), theMachines, "Ping");
  }

  public void jobs(String[] theMachines) throws IOException, ClusterException {
    sendMessage(new GetJobsMessage(), theMachines, "Jobs");
  }

  private final void sendMessage(Message message, String[] theMachines, String description) throws IOException, ClusterException {
    if (description != null) {
      intro(description);
    }

    if (theMachines != null) {
      console.showResponses(out, console.sendMessageToNodes(message, theMachines, 5000, false));
    }
    else {
      console.showResponses(out, console.sendMessageToNodes(message, ClusterDefinition.ALL_NODES_GROUP, 5000, false));
    }
  }

  private void intro(String description) {
    if (out != null) {
      final String userName = clusterDef.getUser();
      out.println("userName=" + userName + ", cluster=" + clusterDef.getDefinitionName() +
                  ", numMachines=" + clusterDef.getNumMachines() + " numNodes=" + clusterDef.getNumNodes());
      out.println("command: " + description);
      final InetSocketAddress[] serverAddresses = clusterDef.getServerAddresses(ClusterDefinition.ALL_NODES_GROUP);

      out.println("\nclusterDef.serverAddresses[" + serverAddresses.length + "]=");
      for (InetSocketAddress address : serverAddresses) {
        out.println("\t" + address);
      }
      out.println();
    }
  }

  public void purge(String[] theMachines) throws IOException {
//todo: implement this?
  }


  /**
   * Create configuration files and put them in cluster/conf.
   * <p>
   * <p>active-cluster-name.txt
   * <p>active-gateway.txt
   * <p>active-cluster-def.txt
   * <p>active-cluster-hierarchy.txt
   * <p>active-machines.txt
   * <p>...
   */
  private final void configure(String heapSize) throws IOException {
    final String confDir = ConfigUtil.getClusterDevConfDir();
    configure(confDir, heapSize, clusterDef);
  }

  /**
   * Create configuration files and put them in the conf dir.
   * <p>
   * <p>active-cluster-name.txt
   * <p>active-gateway.txt
   * <p>active-cluster-def.txt
   * <p>active-cluster-hierarchy.txt
   * <p>active-machines.txt
   * <p>...
   */
  public static final void configure(String confDir, String heapSize, ClusterDefinition clusterDef) throws IOException {
    configureActiveClusterName(confDir, clusterDef);
    configureActiveGateway(confDir, clusterDef);
    configureActiveClusterDef(confDir, clusterDef);
    configureActiveClusterHierarchy(confDir, clusterDef);
    configureActiveMachines(confDir, clusterDef);
    configureHeapSize(confDir, heapSize);
    configureLogSettings(confDir);
    configureActivePortOverride(confDir, ConfigUtil.getPortRange(clusterDef.getUser()));
  }

  private static  final void configureActiveClusterName(String confDir, ClusterDefinition clusterDef) throws IOException {
    final BufferedWriter writer = FileUtil.getWriter(confDir + "active-cluster-name.txt");
    writer.write(clusterDef.getDefinitionName());
    writer.close();
  }

  public static final String getActiveClusterName() throws IOException {
    String result = null;

    final File activeClusterNameFile = FileUtil.getFile(ConfigUtil.getClusterPath("conf/active-cluster-name.txt"));
    if (activeClusterNameFile.exists()) {
      final BufferedReader reader = FileUtil.getReader(activeClusterNameFile);
      result = FileUtil.readAsString(reader, FileUtil.LINUX_COMMENT_IGNORER);
    }

    return result;
  }

  private static final void configureActiveGateway(String confDir, ClusterDefinition clusterDef) throws IOException {
    final BufferedWriter writer = FileUtil.getWriter(confDir + "active-gateway.txt");
    writer.write(clusterDef.getGateway());
    writer.close();
  }

  public static final String getActiveGateway() throws IOException {
    String result = null;
    final File gwFile = new File(ConfigUtil.getClusterPath("conf/active-gateway.txt"));
    if (gwFile.exists()) {
      final BufferedReader reader = FileUtil.getReader(gwFile);
      result = FileUtil.readAsString(reader, FileUtil.LINUX_COMMENT_IGNORER);
    }
    return result;
  }

  public static final File getActiveClusterDefFile() {
    return FileUtil.getFile(ConfigUtil.getClusterDevConfDir() + "active-cluster-def.txt");
  }

  public static final Tree<String> getActiveClusterDefTree() {
    Tree<String> result = null;

    final File clusterDefFile = new File(ConfigUtil.getClusterPath("conf/active-cluster-def.txt"));
    if (clusterDefFile.exists()) {
      final String clusterDefString = FileUtil.readAsStringIfCan(clusterDefFile);
      if (clusterDefString != null) {
        result = TreeBuilderFactory.getStringTreeBuilder().buildTree(clusterDefString);
      }
    }
    else {
      System.err.println("Can't find active clusterDefFile '" + clusterDefFile + "'!");
    }

    return result;
  }

  public static final void configureActiveClusterDef(String confDir, ClusterDefinition clusterDef) throws IOException {
    final String dest = confDir + "active-cluster-def.txt";

    // write out the cluster definition
    final BufferedWriter writer = FileUtil.getWriter(dest);
    writer.write(clusterDef.getClusterDefString());
    writer.newLine();
    writer.close();
  }

  public static final void configureLogSettings(String confDir) {
    //todo: parameterize the source.
    final String source = confDir + "../resources/log/default-log-visitor.txt";
    final String dest = confDir + ClusterLogVisitor.LOG_SETTINGS_FILENAME;

    if (new File(source).exists()) {
      ExecUtil.executeProcess("cp -f " + source + " " + dest);
    }
    else {
      System.out.println(new Date() + ": WARNING : Unable to locate log settings file! '" +
                         source + "'!");
    }
  }

  private static final void configureActiveClusterHierarchy(String confDir, ClusterDefinition clusterDef) throws IOException {
    final Collection<String> hierarchyStrings = clusterDef.getHierarchyStrings(1);
    final BufferedWriter writer = FileUtil.getWriter(confDir + "active-cluster-hierarchy.txt");
    for (String hierarchyString : hierarchyStrings) {
      writer.write(hierarchyString.toLowerCase());  // not fixed here, but in hdeploy and hstart when needed.
      writer.newLine();
    }
    writer.close();
  }

  private static final void configureActiveMachines(String confDir, ClusterDefinition clusterDef) throws IOException {
    final Collection<String> machineNames = clusterDef.getMachineNames(0, true);
    final BufferedWriter writer = FileUtil.getWriter(confDir + "active-machines.txt");
    for (String machineName : machineNames) {
      writer.write(machineName);
      writer.newLine();
    }
    writer.close();
  }

  private static final void configureHeapSize(String confDir, String heapSize) throws IOException {
    final File file = FileUtil.getFile(confDir + "active-heap-size.txt");

    if (heapSize == null) {
      if (file.exists()) {
        file.delete();
      }
    }
    else {
      final BufferedWriter writer = FileUtil.getWriter(file);
      writer.write(heapSize);
      writer.newLine();
      writer.close();
    }
  }

  /**
   * Remove the active-port-override.txt file if it exists.
   * <p>
   * NOTE: This should be called in contexts where the file would be configured
   *       (or unconfigured) from environment settings (like when we deploy.)
   */
  public static final void clearPortOverrideFile() {
    File portOverrideFile = FileUtil.getFile(ConfigUtil.getClusterDevConfDir() + "active-port-override.txt");
    if (portOverrideFile.exists()) portOverrideFile.delete();
    portOverrideFile = FileUtil.getFile(ConfigUtil.getClusterPath("conf/active-port-override.txt"));
    if (portOverrideFile.exists()) portOverrideFile.delete();
  }

  /**
   * Get the active port override if there is one.
   */
  public static final int[] getActivePortOverride() {
    int[] result = null;

    final File portOverrideFile = FileUtil.getFile(ConfigUtil.getClusterPath("conf/active-port-override.txt"));
    if (portOverrideFile.exists()) {
      try {
        final BufferedReader reader = FileUtil.getReader(portOverrideFile);
        final String contents = FileUtil.readAsString(reader, FileUtil.LINUX_COMMENT_IGNORER);
        reader.close();
        final String[] pieces = contents.split(":");
        final int lowPort = Integer.parseInt(pieces[0]);
        final int highPort = pieces.length > 1 ? Integer.parseInt(pieces[1]) : lowPort;
        result = new int[]{lowPort, highPort};
      }
      catch (Exception e) {
        System.err.println(new Date() + ": Admin.getActivePortOverride() error!");
        e.printStackTrace(System.err);
      }
    }

    return result;
  }

  private static final void configureActivePortOverride(String confDir, int[] portOverride) throws IOException {
    if (portOverride == null) return;  // nothing to configure

    final BufferedWriter writer = FileUtil.getWriter(confDir + "active-port-override.txt");
    writer.write(portOverride[0] + ":" + portOverride[1]);
    writer.newLine();
    writer.close();
  }


  public static final String[] getActiveMachines() throws IOException {
    String result = null;
    final File mFile = new File(ConfigUtil.getClusterPath("conf/active-machines.txt"));
    if (mFile.exists()) {
      final BufferedReader reader = FileUtil.getReader(mFile);
      result = FileUtil.readAsString(reader, FileUtil.LINUX_COMMENT_IGNORER);
    }
    return result == null ? null : result.split("\\s+");
  }

  public static void usage() {
    System.err.println();
    System.err.println("Admin - perform administrative cluster functions.");
    System.err.println();
    System.err.println("USAGE:");
    System.err.println("\tAdmin [OPTION]...\n");
    System.err.println();
    System.err.println("DESCRIPTION:");
    System.err.println();
    System.err.println("\t-c, --cluster=CLUSTER_NAME (required)");
    System.err.println("\t\tthe name of the cluster as found at");
    System.err.println("\t\torg.sd.cluster.config.resources.clusters.CLUSTER_NAME.def");
    System.err.println();
    System.err.println("\t-m, --machines=MACHINES (optional)");
    System.err.println("\t\tthe names of the machines participating in the cluster.");
    System.err.println("\t\tas a comma-delimited list.");
    System.err.println();
    System.err.println("\t-g, --gateway=GATEWAY (optional)");
    System.err.println("\t\tthe name of the gateway machine for accessing the cluster.");
    System.err.println();
    System.err.println("\t-u, --user=USER_NAME (optional)");
    System.err.println("\t\tthe name of the user to operate the cluster under. Defaults to");
    System.err.println("\t\tthe current user (currently " + ExecUtil.getUser() + ")");
    System.err.println();
    System.err.println("\t-d, --deploy (optional)");
    System.err.println("\t\tdeploys the code to the cluster.");
    System.err.println();
    System.err.println("\t-s, --start (optional)");
    System.err.println("\t\tstarts the cluster jvms.");
    System.err.println();
    System.err.println("\t-k, --stop (optional)");
    System.err.println("\t\tcleanly shuts the cluster jvms down.");
    System.err.println();
    System.err.println("\t-p, --purge (optional)");
    System.err.println("\t\tpurges the clusters temporary data stores.");
    System.err.println();
    System.err.println("\t-a, --ping (optional)");
    System.err.println("\t\tpings nodes to see which are up.");
    System.err.println();
    System.err.println("\t-j, --jobs (optional)");
    System.err.println("\t\tview jobs across the cluster.");
    System.err.println();
    System.err.println("\t-e, --execute (optional)");
    System.err.println("\t\texecute an arbitrary message.");
    System.err.println();
    System.err.println("COMMON USAGE EXAMPLES:");
    System.err.println();
    System.err.println("\tDeploy and start a cluster:");
    System.err.println("\t\tAdmin -c CLUSTER_NAME -d -s");
    System.err.println();
    System.err.println("\tStop a cluster:");
    System.err.println("\t\tAdmin -c CLUSTER_NAME -k");
    System.err.println();
    System.err.println("\tStop and purge a cluster:");
    System.err.println("\t\tAdmin -c CLUSTER_NAME -k -p");
    System.err.println();
  }

  /**
   * common usages:
   * <br>
   * <p>  # deploy and start cluster
   * <p>  Admin -c &lt;cluster-name&gt; -d -s
   * <br>
   * <p>  # stop cluster
   * <p>  Admin -c &lt;cluster-name&gt; -k
   * <br>
   * <p>  # stop and purge cluster
   * <p>  Admin -c &lt;cluster-name&gt; -k -p
   *
   */
  public static void main(String[] args) throws IOException {

    final PropertiesParser pp = new PropertiesParser(args, true);
    final Properties properties = pp.getProperties();
    args = pp.getArgs();

    final Options options = new Options();

    // define options
    options.addOption(OptionBuilder.withArgName("clusters").withLongOpt("set clusters root").hasArg().isRequired(false).create('R'));
    options.addOption(OptionBuilder.withArgName("cluster").withLongOpt("set cluster").hasArg().isRequired(false).create('c'));
    options.addOption(OptionBuilder.withArgName("machines").withLongOpt("set cluster machines").hasArg().isRequired(false).create('m'));
    options.addOption(OptionBuilder.withArgName("nodes").withLongOpt("set targeted nodes").hasArg().isRequired(false).create('n'));
    options.addOption(OptionBuilder.withArgName("gateway").withLongOpt("set cluster gateway").hasArg().isRequired(false).create('g'));
    options.addOption(OptionBuilder.withArgName("user").withLongOpt("set user").hasArg().isRequired(false).create('u'));
    options.addOption(OptionBuilder.withArgName("heap").withLongOpt("set cluster heap memory").hasArg().isRequired(false).create('h'));
    options.addOption(OptionBuilder.withArgName("root").withLongOpt("set cluster root (home) dir").hasArg().isRequired(false).create('r'));
    options.addOption(OptionBuilder.withArgName("deploy").withLongOpt("deploy code to cluster").isRequired(false).create('d'));
    options.addOption(OptionBuilder.withArgName("start").withLongOpt("start cluster").isRequired(false).create('s'));
    options.addOption(OptionBuilder.withArgName("stop").withLongOpt("stop cluster").isRequired(false).create('k'));
    options.addOption(OptionBuilder.withArgName("ping").withLongOpt("ping cluster").isRequired(false).create('a'));
    options.addOption(OptionBuilder.withArgName("jobs").withLongOpt("view jobs").isRequired(false).create('j'));
    options.addOption(OptionBuilder.withArgName("purge").withLongOpt("purge cluster").isRequired(false).create('p'));
    options.addOption(OptionBuilder.withArgName("execute").withLongOpt("execute cluster message").isRequired(false).create('e'));
    options.addOption(OptionBuilder.withArgName("configure").withLongOpt("configure cluster").isRequired(false).create('C'));

    // initialize parameters
    final CommandLineParser parser = new PosixParser();
    Admin admin = null;

    try {
      final CommandLine commandLine = parser.parse(options, args);

      // get args
      final String clustersDir = commandLine.getOptionValue('R');  // clusters directory (default Config.class/resources/clusters directory)
      final String clusterName = commandLine.getOptionValue('c');
      final String machines = commandLine.getOptionValue('m');
      final String nodes = commandLine.getOptionValue('n');
      final String gateway = commandLine.getOptionValue('g');
      final String userName = commandLine.getOptionValue('u');
      final String heapSize = commandLine.getOptionValue('h');
      final String rootDir = commandLine.getOptionValue('r');    // root directory for deployed cluster "~/cluster"

      final boolean deployOption = commandLine.hasOption('d');
      final boolean startOption = commandLine.hasOption('s');
      final boolean stopOption = commandLine.hasOption('k');
      final boolean pingOption = commandLine.hasOption('a');
      final boolean jobsOption = commandLine.hasOption('j');
      final boolean purgeOption = commandLine.hasOption('p');
      final boolean executeOption = commandLine.hasOption('e');
      final boolean configureOption = commandLine.hasOption('C');  // creat configuration files in ClusterDevDir
//todo: implement execute option w/something like sendMessage only not static

      String[] theMachines = null;
      if (machines != null && machines.length() > 0) {
        theMachines = machines.split("\\s*,\\s*");
      }

      String[] theNodes = null;
      if (nodes != null && nodes.length() > 0) {
        theNodes = nodes.split("\\s*,\\s*");
      }

      if (rootDir != null) {
        ConfigUtil.setClusterRootDir(rootDir);
      }

      if (deployOption) {
        // when we deploy, we need to rely on environment settings, not
        // on the override file. If we have overridden and now are not
        // overriding, then this file needs to disappear for proper function.
        clearPortOverrideFile();
      }

      if (gateway != null && gateway.length() > 0) {
        properties.setProperty(ClusterDefinition.CLUSTER_GATEWAY_PROPERTY, gateway);
      }
      if (clustersDir != null && !"".equals(clustersDir.trim())) {
        properties.setProperty(ClusterDefinition.CLUSTERS_DIR_PROPERTY, clustersDir);
      }
      if (clusterName != null && clusterName.length() > 0) {
        properties.setProperty(ClusterDefinition.CLUSTER_DEFINITION_NAME_PROPERTY, clusterName);
      }
      if (machines != null && machines.length() > 0) {
        properties.setProperty(ClusterDefinition.CLUSTER_MACHINES_PROPERTY, machines);
      }
      if (userName != null && userName.length() > 0) {
        properties.setProperty(ClusterDefinition.CLUSTER_USER_PROPERTY, userName);
      }

      final ClusterRunner cr = new ClusterRunner(!deployOption, properties); // useActiveCluster unless deploying

      // check for valid clusterDef
      final ClusterDefinition clusterDef = cr.getClusterDefinition();
      if (clusterDef == null || !clusterDef.isValid()) {
        throw new IllegalArgumentException("Invalid or missing cluster named '" + clusterName + "'!");
      }

//todo: configure dif't print stream than System.out
      admin = new Admin(clusterDef, System.out);

      // perform requested operations
      if (configureOption) {
        System.out.println("configuring cluster (only).");
        admin.configure(heapSize);
      }
      else {
        if (deployOption) admin.deploy(heapSize);
        if (startOption) admin.start(heapSize);
        if (stopOption) admin.stop(theNodes);
        if (pingOption) admin.ping(theNodes);
        if (jobsOption) admin.jobs(theNodes);
        if (purgeOption) admin.purge(theNodes);
      }
    }
    catch (ParseException e) {
      System.err.println("Command line error: " + e.getLocalizedMessage());
      e.printStackTrace(System.err);
      usage();
    }
    catch (IllegalArgumentException e) {
      System.err.println("argument error: " + e.getLocalizedMessage());
      e.printStackTrace(System.err);
      usage();
    }
    catch (Throwable t) {
      System.err.println("Unexpected error!:" + t.getLocalizedMessage());
      t.printStackTrace(System.err);
//      usage();
    }
    finally {
      if (admin != null) admin.shutdown();
    }
  }
}
