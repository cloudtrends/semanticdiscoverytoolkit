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
package org.sd.util.cmd;


import org.sd.io.FileUtil;
import org.sd.util.ReflectUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Interpreter for creating a command-line utility.
 * <p>
 * @author Spence Koehler
 */
public class CommandInterpreter {
  
  private Map<String, String> vars;
  private Commands commands;
  private AtomicBoolean interpreting = new AtomicBoolean(true);

  private static final Pattern VAR_EQ_VAL = Pattern.compile("^\\s*(\\w+)\\s*=\\s*(.*)\\s*$");
  private static final Pattern COMMAND_PATTERN = Pattern.compile("^\\s*(\\w+)\\s*(.*)$");
  private static final Pattern WORD_PATTERN = Pattern.compile("^\\s*(\\w+)\\s*$");

  public CommandInterpreter(String commandsClass, String initFile) {
    // init default vars
    this.vars = new LinkedHashMap<String, String>();
    vars.put("prompt", "cmd> ");
    if (initFile != null) vars.put("initFile", initFile);

    // create commands instance
    this.commands = (Commands)ReflectUtil.buildInstance(commandsClass);
  }

  public CommandInterpreter(Commands commands, String initFile) {
    this.vars = new LinkedHashMap<String, String>();
    vars.put("prompt", "cmd> ");
    if (initFile != null) vars.put("initFile", initFile);
    this.commands = commands;
  }

  //for junit testing
  CommandInterpreter() {
    this.vars = new LinkedHashMap<String, String>();
    this.commands = null;
  }

  public void setCommands(Commands commands) {
    this.commands = commands;
  }

  public Commands getCommands() {
    return commands;
  }

  public final void init() throws IOException {
    // read initFile and run each line through interpreter
    final String initFile = getVar("initFile");
    if (initFile != null && new File(initFile).exists()) {
      final BufferedReader reader = FileUtil.getReader(initFile);
      doBatch(reader, true);
      reader.close();
    }
  }

  private final void doBatch(BufferedReader reader, boolean batchMode) throws IOException {
    String line = null;
    int index = 0;
    while ((batchMode || interpreting.get()) && ((line = reader.readLine()) != null)) {
      if (!processLine(line, batchMode)) {
        if (commands.handleUnrecognizedInputLine(this, index, line, batchMode)) {
          break;
        }
      }
      ++index;
      if (!batchMode && interpreting.get()) System.out.print(index + ": " + getVar("prompt"));
    }
  }

//batch -vs- interactive mode
  private final boolean processLine(String line, boolean batchMode) {
    boolean result = true;

    // ignore empty and comment lines
    if (line.length() == 0 || '#' == line.charAt(0)) {
      return result;
    }

    final String[] twoPieces = new String[2];

    if (isVariableAssignment(line, twoPieces)) {  // "var"="value"
      doSetVariable(twoPieces);
      showMessage(twoPieces[0] + "=" + twoPieces[1], batchMode);
    }
    else if (isCommand(line, twoPieces)) {  // "command" "args"
      result = executeCommand(twoPieces, batchMode);
    }
    else if (isSingleWord(line, twoPieces)) {  // "variable" or "no-arg-command"
      if (commands.getCommandExecutor(twoPieces[0]) != null) {
        result = executeCommand(twoPieces, batchMode);
      }
      else if (getVar(line) != null) {
        showMessage(getVar(line), batchMode);
      }
      else {
        result = false;
      }
    }
    else {
      result = false;
    }

    return result;
  }

  private final boolean matchTwoPieces(Pattern p, String line, String[] twoPieces) {
    boolean result = false;

    final Matcher m = p.matcher(line);
    if (m.matches()) {
      result = true;
      twoPieces[0] = m.group(1);
      twoPieces[1] = m.groupCount() == 2 ? m.group(2) : null;
    }

    return result;
  }

  private final boolean isVariableAssignment(String line, String[] twoPieces) {
    return matchTwoPieces(VAR_EQ_VAL, line, twoPieces);
  }

  private final void doSetVariable(String[] twoPieces) {
    setVar(twoPieces[0], twoPieces[1]);
  }

  private final boolean isCommand(String line, String[] twoPieces) {
    return matchTwoPieces(COMMAND_PATTERN, line, twoPieces);
  }

  // note: twoPieces[1] == null ==> no args.
  // @return true for ok; false for problem
  private final boolean executeCommand(String[] twoPieces, boolean batchMode) {
    boolean result = false;
    final CommandExecutor e = commands.getCommandExecutor(twoPieces[0]);
    if (e != null) {
      result = e.execute(this, twoPieces[1], batchMode);
    }
    return result;
  }

  private final boolean isSingleWord(String line, String[] twoPieces) {
    return matchTwoPieces(WORD_PATTERN, line, twoPieces);
  }

  public void showError(String type, String message, boolean batchMode) {
//todo: show error on configured PrintStream and according to batchMode
    System.out.println(type + ": " + message);
  }

  public void showMessage(String message, boolean batchMode) {
//todo: show message on configured PrintStream and according to batchMode
    System.out.println(message);
  }

  public PrintStream getPrintStream() {
    return System.out;
  }

  public void start() throws IOException {
    // enter the command interpreter loop.
    final BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
    interpreting.set(true);

    System.out.print("0: " + getVar("prompt"));
    doBatch(in, false);
    in.close();
  }

  public void quit() {
    interpreting.set(false);
  }

  public void setVar(String name, String value) {
    vars.put(name, value);
  }

  public String getVar(String name) {
    return vars.get(name);
  }

  public Collection<String> getVarNames() {
    return vars.keySet();
  }

  public static void main(String[] args) throws IOException {
    //todo: parse args: override commands class, initFile, prompt, etc.

    final CommandInterpreter interp = new CommandInterpreter("org.sd.util.cmd.Foo", null);
    interp.init();
    interp.start();
  }
}
