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


/**
 * Interface for executing a specific command.
 * <p>
 * @author Spence Koehler
 */
public interface CommandExecutor {

  /**
   * Get the name of this command.
   */
  public String getCommandName();

  /**
   * Get a short description for this command.
   */
  public String getDescription();

  /**
   * Execute this command with the given args, returning the status of the
   * execution (true=ok, false=error).
   * <p>
   * Note that any error or response to be printed by the interpreter needs to
   * be handled here.
   * <p>
   * @param interpreter  the interpreter executing the command
   * @param args         of the form "(...)"
   * @param batchMode    true if interpreter is operating in batchMode; false if interactive.
   *
   * @return true for successful execution (from the interpreter's standpoint); otherwise, false.
   */
  public boolean execute(CommandInterpreter interpreter, String args, boolean batchMode);

}
