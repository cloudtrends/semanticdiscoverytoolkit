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


import java.util.Collection;
import java.util.Map;
import java.util.LinkedHashMap;

/**
 * Base commands implementation that sets up default generic commands.
 * <p>
 * Extend this class to add application-specific commands.
 *
 * @author Spence Koehler
 */
public abstract class BaseCommands implements Commands {
  
  private Map<String, CommandExecutor> executors;

  protected BaseCommands() {
    this.executors = new LinkedHashMap<String, CommandExecutor>();

    addCommandExecutor(new HelpExecutor());
    addCommandExecutor(new QuitExecutor());
    addCommandExecutor(new VarsExecutor());
//PrintVarExecutor
//SetVarExecutor
//UnsetVarExecutor
  }

  protected final void addCommandExecutor(CommandExecutor executor) {
    executors.put(executor.getCommandName(), executor);
  }

  /**
   * Get the command executor for the given command, or null if the command
   * is unrecognized.
   */
  public CommandExecutor getCommandExecutor(String command) {
    return executors.get(command);
  }

  /**
   * Get all of the commands names.
   */
  public Collection<String> getCommandNames() {
    return executors.keySet();
  }

  /**
   * Handle an unrecognized input line.
   * <p>
   * Default behavior is to show an error and continue.
   * 
   * @return true to halt batch processing, false to continue.
   */
  public boolean handleUnrecognizedInputLine(CommandInterpreter interpreter, int lineNum, String line, boolean batchMode) {
    interpreter.showError("BadLine", "(" + lineNum + ") " + line, batchMode);
    return false;
  }
  
}
