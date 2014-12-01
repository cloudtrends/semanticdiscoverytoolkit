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
package org.sd.util;


import org.sd.io.FileUtil;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Date;

/**
 * Utilities for executing shell processes.
 * <p>
 * @author Spence Koehler
 */
public class ExecUtil {
  
  private static final Object mutex = new Object();
//  private static final String OS_NAME = System.getProperty("os.name").toLowerCase();

  /**
   * Get the name of the user running this jvm.
   */
  public static String getUser() {
    return System.getProperty("user.name");
  }
  
  public static String getUserHome() {
    return System.getProperty("user.home");
  }

  /**
   * Get the name of the machine running this jvm.
   */
  public static String getMachineName() {
    String result = null;

    synchronized (mutex) {
      
      result = System.getProperty("machine.name");
      if (result == null) {
        final ExecResult execResult = ExecUtil.executeProcess("uname -n");
        if (execResult != null && !execResult.failed()) {
          result = execResult.output;
          if (result != null) result = result.toLowerCase();  // normalize the name to all lower.
        }
        if (result == null) {
          result = "UNKNOWN";
        }
        System.setProperty("machine.name", result);
      }
    }

    return result;
  }

  /**
   * Determine whether the host name (or ip address) identifies the current
   * machine.
   */
  public static boolean isMyAddress(String host) {
    if ("localhost".equals(host)) return true;

    boolean result = false;

    try {
      final InetAddress addr = InetAddress.getByName(host);
      result = addr.isAnyLocalAddress() || addr.isLoopbackAddress();

      if (!result) {
        result = NetworkInterface.getByInetAddress(addr) != null;
      }
    }
    catch (UnknownHostException uhe) {
      // result is false
    }
    catch (SocketException se) {
      // result is false
    }

    return result;
  }

  /**
   * Get this jvm's system process id.
   */
  public static int getProcessId() {
    int processId = 0;

    final ExecResult execResult = ExecUtil.executeProcess("echo $PPID", "bash");

    if (execResult != null && !execResult.failed()) {
      processId = Integer.parseInt(execResult.output);
    }

    return processId;
  }

  public static boolean isUp(int processId) {
    final String pidString = Integer.toString(processId);
    
    String psCmd = "ps -p " + pidString;
    final ExecResult execResult = executeProcess(psCmd);
    boolean result = execResult != null && !execResult.failed();
    if (result) result = (execResult.output.indexOf(pidString) >= 0);

    return result;
  }

  public static ExecResult executeProcess(String pipedInput, String command) {
    ExecResult result = null;
    try {
      final Process process = Runtime.getRuntime().exec(command);
      pipeIntoProcess(pipedInput, process);
      result = getProcessOutput(process);
    }
    catch (IOException e) {
      //eating this is ok as null result signals a problem.
      System.err.println(new Date() + ": ExecUtil.executeProcess died! -- eating exception:");
      e.printStackTrace(System.err);
    }
    return result;
  }

  public static int executeProcess(String pipedInput, String command, BufferedWriter redirectedOutput) {
    int result = 0;
    try {
      final Process process = Runtime.getRuntime().exec(command);
      pipeIntoProcess(pipedInput, process);
      result = redirectProcessOutput(process, redirectedOutput);
    }
    catch (IOException e) {
      //eating this is ok as null result signals a problem.
      System.err.println(new Date() + ": ExecUtil.executeProcess died! -- eating exception:");
      e.printStackTrace(System.err);
      result = -1;
    }
    return result;
  }

  /**
   * Executes a series of commands (serially).
   * 
   * @param commands commands to execute in order 
   * @return returns the {@link ExecResult} for the final command
   */
  public static ExecResult[] executeProcesses(String[] commands) {
    ExecResult[] results = new ExecResult[commands.length];
    
    for (int i=0; i < commands.length; i++) {
      results[i] = executeProcess(commands[i]);
    }
    
    return results;
  }

  /**
   * Executes the command ([0]) and args ([1+]).
   */
  public static ExecResult executeProcess(String[] commandPlusArgs) {
    return executeProcess(commandPlusArgs, null);
  }

  /**
   * Executes the command ([0]) and args ([1+]) from the given workingDir.
   */
  public static ExecResult executeProcess(String[] commandPlusArgs, File workingDir) {
    ExecResult result = null;
    try {
      final Process process =
        workingDir == null ? Runtime.getRuntime().exec(commandPlusArgs) :
        Runtime.getRuntime().exec(commandPlusArgs, null, workingDir);
      result = getProcessOutput(process);
    } catch (IOException e) {
      //eating this is ok as null result signals a problem.
      e.printStackTrace(System.err);
    }
    return result;
  }

  public static ExecResult executeProcess(String command) {
    ExecResult result = null;
    try {
      final Process process = Runtime.getRuntime().exec(command);
      result = getProcessOutput(process);
    } catch (IOException e) {
      //eating this is ok as null result signals a problem.
      e.printStackTrace(System.err);
    }
    return result;
  }

  public static int executeProcess(String command, BufferedWriter redirectedOutput) {
    int result = 0;

    try {
      final Process process = Runtime.getRuntime().exec(command);
      result = redirectProcessOutput(process, redirectedOutput);
    }
    catch (IOException e) {
      e.printStackTrace(System.err);
      result = -1;
    }

    return result;
  }

  public static ExecResult executeProcess(String command, File workingDir) {
    ExecResult result = null;
    try {
      final Process process = Runtime.getRuntime().exec(command, null, workingDir);
      result = getProcessOutput(process);
    }
    catch (IOException e) {
      //eating this is ok as null result signals a problem.
      e.printStackTrace(System.err);
    }
    return result;
  }

  public static ExecResult executeRemoteProcess(String user, String address, String command) {
    ExecResult result = null;
    try {
      if (user == null || user.length() == 0) user = getUser();
      final Process process = Runtime.getRuntime().exec(new String[]{"ssh", user + "@" + address, command});
      result = getProcessOutput(process);
    }
    catch (IOException e) {
      //eating this is ok as null result signals a problem.
      e.printStackTrace(System.err);
    }
    return result;
  }

  /**
   * Get the process output and exit value.
   */
  private static ExecResult getProcessOutput(Process process) throws IOException {
    String result = null;
    final InputStream inputStream = process.getInputStream();
    if (inputStream != null) {
      try {
        final BufferedReader reader = FileUtil.getReader(inputStream);
        result = FileUtil.readAsString(reader);
        reader.close();
      }
      finally {
        inputStream.close();
      }
    }
    boolean finished = false;
    int exitValue = -1;
    try {
      exitValue = process.waitFor();
      finished = true;
    }
    catch (InterruptedException e) {
      //eating this is ok as finished=false signals problem
      e.printStackTrace(System.err);
    }

    if (!finished && exitValue == 0) exitValue = -1;
    return new ExecResult(exitValue, result);
  }

  private static int redirectProcessOutput(Process process, BufferedWriter writer) throws IOException {
    final InputStream inputStream = process.getInputStream();

    if (inputStream != null) {
      try {
        final BufferedReader reader = FileUtil.getReader(inputStream);

        String line = null;
        while ((line = reader.readLine()) != null) {
          writer.write(line);
          writer.newLine();
        }

        reader.close();
      }
      finally {
        inputStream.close();
      }
    }
    boolean finished = false;
    int exitValue = -1;
    try {
      exitValue = process.waitFor();
      finished = true;
    }
    catch (InterruptedException e) {
      //eating this is ok as finished=false signals problem
      e.printStackTrace(System.err);
    }

    return finished ? exitValue : -1;
  }

  private static void pipeIntoProcess(String data, Process process) throws IOException {
    final BufferedWriter writer = FileUtil.getWriter(process.getOutputStream());
    writer.write(data);
    writer.close();
  }

  /**
   * Container to hold the result of executing a process.
   */
  public static final class ExecResult {
    public final int exitValue;
    public final String output;

    public ExecResult(int exitValue, String output) {
      this.exitValue = exitValue;
      this.output = output;
    }

    public boolean failed() {
      return exitValue != 0;
    }

    // (exitValue)output
    public String toString() {
      final StringBuilder result = new StringBuilder();
      result.append('(').append(exitValue).append(')').append(output);
      return result.toString();
    }
  }
}
