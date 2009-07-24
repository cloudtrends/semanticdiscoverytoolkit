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


import org.sd.cluster.io.Response;
import org.sd.cluster.job.GetJobsMessage;
import org.sd.cluster.job.JobCommand;
import org.sd.cluster.job.JobCommandMessage;
import org.sd.cluster.job.LocalJobId;
import org.sd.util.PropertiesParser;
import org.sd.util.cmd.AbstractExecutor;
import org.sd.util.cmd.BaseCommands;
import org.sd.util.cmd.CommandInterpreter;

import java.io.IOException;
import java.util.Properties;
import java.util.regex.Pattern;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;

/**
 * Utility to send job commands to the cluster.
 * <p>
 * @author Spence Koehler
 */
public class JobAdmin extends BaseCommands {
  
  private int timeout;
  private Console console;

  public JobAdmin(ClusterDefinition clusterDef, int timeout) throws IOException {
    this.timeout = timeout;
    this.console = new Console(clusterDef, "JobAdmin");
  }

  public void shutdown() {
    console.shutdown();
  }

  /**
   * Send the given job command to the identified job.
   */
  public Response[] sendJobCommand(JobCommand jobCommand, String oneNodeName, int localJobId, boolean singleOnly) throws ClusterException {
    Response[] result = null;

    final JobCommandMessage message = new JobCommandMessage(jobCommand, new LocalJobId(oneNodeName, localJobId));

    if (singleOnly) {
      Response response = console.sendJobCommandToNode(message, timeout);
      if (response != null) {
        result = new Response[]{response};
      }
    }
    else {
      result = console.sendJobCommand(message, timeout);
    }
    
    return result;
  }

  public void init() {
    addCommandExecutor(new JobCommandExecutor(console, JobCommand.PAUSE));
    addCommandExecutor(new JobCommandExecutor(console, JobCommand.RESUME));
    addCommandExecutor(new JobCommandExecutor(console, JobCommand.BOUNCE));
    addCommandExecutor(new JobCommandExecutor(console, JobCommand.INTERRUPT));
    addCommandExecutor(new JobCommandExecutor(console, JobCommand.DETAIL));

    // add executor to get a list of jobs from nodes, sorting by node -vs- job
    addCommandExecutor(new JobListExecutor(console));

    // push data to job
    addCommandExecutor(new PushDataExecutor(console));


//todo: add commands to
//clear job data
//clear batch data
  }

  private final class JobCommandExecutor extends AbstractExecutor {
    private Console console;
    private JobCommand jobCommand;

    public JobCommandExecutor(Console console, JobCommand jobCommand) {
      super(jobCommand.name().toLowerCase(), "[-1] -n nodeName-jvmNum -j localJobId");

      this.console = console;
      this.jobCommand = jobCommand;
    }

    public Options buildOptions() {
      final Options options = new Options();
      options.addOption(OptionBuilder.withArgName("single").withLongOpt("single-node-only").isRequired(false).create('1'));
      options.addOption(OptionBuilder.withArgName("node").withLongOpt("node-name").hasArg().isRequired(true).create('n'));
      options.addOption(OptionBuilder.withArgName("jobId").withLongOpt("jvm-id").hasArg().isRequired(true).create('j'));
      return options;
    }

    public boolean execute(CommandInterpreter interpreter, CommandLine commandLine, boolean batchMode) {
      final String machineName = commandLine.getOptionValue('n');
      final int localJobId = Integer.parseInt(commandLine.getOptionValue('j'));
      final boolean singleOnly = commandLine.hasOption('1');

      String message = "sending " + jobCommand + " to " + machineName + ":" + localJobId;
      if (singleOnly) message = message + " (single)";
      interpreter.showMessage(message, batchMode);

      Response[] responses = null;
      try {
        responses = sendJobCommand(jobCommand, machineName, localJobId, singleOnly);
      }
      catch (ClusterException e) {
        throw new IllegalStateException(e);
      }

      interpreter.showMessage("Received " + responses.length + " responses:", batchMode);
      for (Response response : responses) {
        String responseMessage = null;
        if (response instanceof BooleanResponse) {
          final BooleanResponse bresponse = (BooleanResponse)response;
          responseMessage = bresponse.getNodeName() + ": " + bresponse.getValue();
        }
        else {
          responseMessage = response.toString();
        }
        interpreter.showMessage(responseMessage, batchMode);
      }

      return true;
    }
  }

  private final class JobListExecutor extends AbstractExecutor {
    private Console console;

    public JobListExecutor(Console console) {
      super("jobs", "[-g groupName]");

      this.console = console;
    }

    public Options buildOptions() {
      final Options options = new Options();
      options.addOption(OptionBuilder.withArgName("group").withLongOpt("group-name").hasArg().isRequired(false).create('g'));
//todo: add options for sorting responses by job -vs- by node.
      return options;
    }

    public boolean execute(CommandInterpreter interpreter, CommandLine commandLine, boolean batchMode) {
      final String groupName = commandLine.getOptionValue('g', ClusterDefinition.ALL_NODES_GROUP);

      final String message = "Requesting jobs from group '" + groupName + "'...";
      interpreter.showMessage(message, batchMode);

      Response[] responses = null;
      try {
        responses = console.sendMessageToNodes(new GetJobsMessage(), groupName, 5000, false);
      }
      catch (ClusterException e) {
        throw new IllegalStateException(e);
      }

      interpreter.showMessage("Received " + responses.length + " responses:", batchMode);
      for (Response response : responses) {
        interpreter.showMessage(response.toString(), batchMode);
      }

      return true;
    }
  }

  private final class PushDataExecutor extends AbstractExecutor {
    private Console console;

    public PushDataExecutor(Console console) {
      super("push", "-l localPathToDataDir -g groupName -d destDataDirId -j jobIdString -p partitionPatternString -n partitionGroupNum");
      // example: push -l /home/sbk/tmp/impressum/batches/ -g processor -d german.071706 -j CompanyFinderJob.main -p "^workbatch-(\d+).dat$" -n 1

      this.console = console;
    }

    public Options buildOptions() {
      final Options options = new Options();
      options.addOption(OptionBuilder.withArgName("local").withLongOpt("local-datadir").hasArg().isRequired(true).create('l'));
      options.addOption(OptionBuilder.withArgName("group").withLongOpt("group-name").hasArg().isRequired(true).create('g'));
      options.addOption(OptionBuilder.withArgName("dest").withLongOpt("dest-datadir").hasArg().isRequired(true).create('d'));
      options.addOption(OptionBuilder.withArgName("jobid").withLongOpt("jobid-string").hasArg().isRequired(true).create('j'));
      options.addOption(OptionBuilder.withArgName("pattern").withLongOpt("partition-pattern").hasArg().isRequired(true).create('p'));
      options.addOption(OptionBuilder.withArgName("num").withLongOpt("partition-group-num").hasArg().isRequired(true).create('n'));
      return options;
    }

    public boolean execute(CommandInterpreter interpreter, CommandLine commandLine, boolean batchMode) {
      final String localPathToDataDir = commandLine.getOptionValue('l');
      final String groupName = commandLine.getOptionValue('g');
      final String destDataDirId = commandLine.getOptionValue('d');
      final String jobIdString = commandLine.getOptionValue('j');
      final String partitionPatternString = commandLine.getOptionValue('p');
      final int partitionGroupNum = Integer.parseInt(commandLine.getOptionValue('n'));

      final String message = "Sending data to nodes (backgrounded):\n" +
        "\tlocalPathToDataDir=" + localPathToDataDir + "\n" +
        "\tgroupName=" + groupName + "\n" +
        "\tdestDataDirId=" + destDataDirId + "\n" +
        "\tjobIdString=" + jobIdString + "\n" +
        "\tpartitionPatternString=" + partitionPatternString + "\n" +
        "\tpartitionGroupNum=" + partitionGroupNum + "\n";
      interpreter.showMessage(message, batchMode);

      final String jobDirPostfix = DataPusher.getJobDirPostfix(jobIdString, destDataDirId);
      DataPusher.sendDataToNodes(console.getClusterDefinition(), groupName, jobDirPostfix, localPathToDataDir, Pattern.compile(partitionPatternString), partitionGroupNum, 3, 1);

      return true;
    }
  }

  private static final String concat(String[] strings, String delim) {
    final StringBuilder result = new StringBuilder();

    for (int i = 0; i < strings.length; ++i) {
      result.append(strings[i]);
      if (i + 1 < strings.length) {
        result.append(delim);
      }
    }

    return result.toString();
  }

  /**
   * Starts an interactive command loop for sending job commands to the cluster.
   *
   * arg1: user (i.e. bperry)
   * arg2: defName (i.e. dev-3a)
   * arg3: gateway (i.e. vorta)
   * arg4+ machine names (i.e. suliban andorian tholian)
   */
  public static void oldMain(String[] args) throws IOException {
//java -Xmx640m org.sd.cluster.config.JobAdmin bperry 3m3n.1-2 vorta suliban andorian tholian
//java -Xmx640m org.sd.cluster.config.JobAdmin bperry 3m10n.2-8 vorta suliban founder shran

//todo: manage args through cli

    final String user = args[0];
    final String defName = args[1];
    final String gateway = args[2];
    final String[] machines = new String[args.length - 3];
    for (int i = 3; i < args.length; ++i) {
      machines[i - 3] = args[i];
    }
    final ClusterDefinition clusterDef = new ClusterDefinition(user, defName, gateway, machines);
    final JobAdmin jobAdmin = new JobAdmin(clusterDef, 10000);
    final CommandInterpreter interp = new CommandInterpreter(jobAdmin, null);

    interp.setVar("prompt", "jobcmd> ");
    interp.setVar("user", user);
    interp.setVar("defName", defName);
    interp.setVar("gateway", gateway);
    interp.setVar("machines", concat(machines, ", "));

    jobAdmin.init();
    interp.start();
    jobAdmin.shutdown();
  }

  /**
   * Starts an interactive command loop for sending job commands to the cluster
   * using current active cluster definition.
   */
  public static void main(String[] args) throws IOException {
//./run org.sd.cluster.config.JobAdmin

    final PropertiesParser pp = new PropertiesParser(args, true);
    final Properties properties = pp.getProperties();
    final ClusterRunner cr = new ClusterRunner(true/*useActiveCluster*/, properties);
    //final String user = cr.getUser();
    final ClusterDefinition clusterDef = cr.getClusterDefinition();
    final JobAdmin jobAdmin = new JobAdmin(clusterDef, 10000);
    final String cmdFile = properties != null ? properties.getProperty("cmdFile") : null;
    final CommandInterpreter interp = new CommandInterpreter(jobAdmin, cmdFile);

    interp.setVar("prompt", "jobcmd> ");
    interp.setVar("user", clusterDef.getUser());
    interp.setVar("defName", clusterDef.getDefinitionName());
    interp.setVar("machines", concat(clusterDef.getMachines(), ", "));

    jobAdmin.init();
    if (cmdFile != null) {
      interp.init();  // just run commands in command file
    }
    else {
      interp.init();
      interp.start();  // start interactive interpreter
    }
    jobAdmin.shutdown();
  }
}
