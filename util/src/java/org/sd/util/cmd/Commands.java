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

/**
 * Interface for commands to be executed by the command interpreter.
 * <p>
 * @author Spence Koehler
 */
public interface Commands {
  
  /**
   * Get the command executor for the given command, or null if the command
   * is unrecognized.
   */
  public CommandExecutor getCommandExecutor(String command);

  /**
   * Get all of the commands names.
   */
  public Collection<String> getCommandNames();

  /**
   * Initialize i.e. by adding all command executors.
   */
  public void init();

  /**
   * Handle an unrecognized input line.
   *
   * @return true if properly reported; false if not.
   */
  public boolean handleUnrecognizedInputLine(CommandInterpreter interpreter, int lineNum, String line, boolean batchMode);

}
