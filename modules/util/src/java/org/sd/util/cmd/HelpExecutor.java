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

import org.apache.commons.cli.CommandLine;
//import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;

/**
 * A command executor for giving command help.
 * <p>
 * @author Spence Koehler
 */
public class HelpExecutor extends AbstractExecutor {
  
  public HelpExecutor() {
    super("help", "Display help for commands");
  }

  protected final Options buildOptions() {
    return new Options();   // currently no options.
  }

  protected final boolean execute(CommandInterpreter interpreter, CommandLine commandLine, boolean batchMode) {
//todo: use org.apache.commons.cli.HelpFormatter to show options help for commands.
    final Commands commands = interpreter.getCommands();
    final Collection<String> commandNames = commands.getCommandNames();
    for (String commandName : commandNames) {
      final CommandExecutor executor = commands.getCommandExecutor(commandName);
      interpreter.showMessage(commandName + ": " + executor.getDescription(), batchMode);
    }
    return true;
  }
}
