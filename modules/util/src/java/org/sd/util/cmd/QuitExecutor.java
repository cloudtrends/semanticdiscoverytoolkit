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


import org.apache.commons.cli.CommandLine;
//import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;

/**
 * A command executor for giving command quit.
 * <p>
 * @author Spence Koehler
 */
public class QuitExecutor extends AbstractExecutor {
  
  public QuitExecutor() {
    super("quit", "Exit the interactive loop.");
  }

  protected final Options buildOptions() {
    return new Options();   // currently no options.
  }

  protected final boolean execute(CommandInterpreter interpreter, CommandLine commandLine, boolean batchMode) {
    interpreter.quit();
    interpreter.showMessage("goodbye.", batchMode);
    return true;
  }
}
