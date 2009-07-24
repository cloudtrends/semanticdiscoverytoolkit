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

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;


/**
 * Abstract implementation of a command executor.
 * <p>
 * @author Spence Koehler
 */
public abstract class AbstractExecutor implements CommandExecutor {

  private String commandName;
  private String description;
  private CommandLineParser parser;
  private Options _options;

  protected abstract Options buildOptions();
  protected abstract boolean execute(CommandInterpreter interpreter, CommandLine commandLine, boolean batchMode);

  protected AbstractExecutor(String commandName, String description) {
    this.commandName = commandName;
    this.description = description;
    this.parser = new PosixParser();
    this._options = null;
  }

  protected final Options getOptions() {
    if (_options == null) {
      _options = buildOptions();
    }
    return _options;
  }

  /**
   * Execute this command with the given args, returning the status of the
   * execution (true=ok, false=error).
   * <p>
   * Note that any error or response to be printed by the interpreter needs to
   * be handled here.
   * <p>
   * @param interpreter  the interpreter executing the command.
   * @param argsString   string containing arguments for the command.
   * @param batchMode    true if interpreter is operating in batchMode; false if interactive.
   *
   * @return true for successful execution (from the interpreter's standpoint); otherwise, false.
   */
  public boolean execute(CommandInterpreter interpreter, String argsString, boolean batchMode) {
    boolean result = false;
    final Options options = getOptions();
    final String[] args = parseArgsString(interpreter, argsString);

    try {
      final CommandLine commandLine = parser.parse(options, args);
      result = execute(interpreter, commandLine, batchMode);
    }
    catch (ParseException e) {
      interpreter.showError("BadArgs", FileUtil.getStackTrace(e), batchMode);
    }
    catch (Throwable t) {
      interpreter.showError("ExecutionError", FileUtil.getStackTrace(t), batchMode);
    }

    return result;
  }

  /**
   * Get the name of this command.
   */
  public String getCommandName() {
    return commandName;
  }

  /**
   * Get a short description for this command.
   */
  public String getDescription() {
    return description;
  }

  protected static String[] parseArgsString(CommandInterpreter interpreter, String argsString) {
    // split on spaces unless quoted. make variable substitutions
    final List<String> result = new ArrayList<String>();

    final int len = (argsString == null) ? 0 : argsString.length();

    if (len > 0) {
      argsString = doVariableSubstitutions(interpreter, argsString);

      final char[] chars = argsString.toCharArray();
      final StringBuilder stringBuilder = new StringBuilder();
      int index = 0;

      while (index < len) {
        // spin past any whitespace
        while (index < chars.length && chars[index] == ' ') ++index;

        int endPos = index;
        int dataBegin = index;
        int dataEnd = index + 1;

        if (chars[index] == '\'') {
          // find unescaped end single quote
          endPos = findEndSingleQuote(chars, index + 1);
          if (endPos > index) {
            dataBegin++;
            dataEnd = endPos;
          }
        }
        else if (chars[index] == '"') {
          // find unescaped end double quote
          endPos = findEndDoubleQuote(chars, index + 1);
          if (endPos > index) {
            dataBegin++;
            dataEnd = endPos;
          }
        }
        else if (chars[index] == '(') {
          // find unescaped, balanced end paren
          endPos = findEndParen(chars, index + 1);
          if (endPos > index) {
            dataEnd = endPos + 1;
          }
        }
        else {
          // find end of "word"
          endPos = findEndWord(chars, index + 1);
          if (endPos > index) {
            dataEnd = endPos;
          }
        }

        for (int i = dataBegin; i < dataEnd; ++i) {
          stringBuilder.append(chars[i]);
        }

        if (stringBuilder.length() > 0) {
          result.add(stringBuilder.toString());
          stringBuilder.setLength(0);  // clear out for next go-round
        }

        //make sure index has been inc'd
        index = endPos + 1;
      }
    }

    return result.toArray(new String[result.size()]);
  }

  protected static String doVariableSubstitutions(CommandInterpreter interpreter, String argsString) {
    final StringBuilder result = new StringBuilder();

    final int len = argsString.length();
    int curPos = 0;

    while (curPos <  len) {
      final int dollarPos = argsString.indexOf("$", curPos);
      if (dollarPos >= 0) {
        result.append(argsString.substring(curPos, dollarPos));
        curPos = dollarPos + 1;
        if (isEscaped(argsString, dollarPos, new char[] {'$', '\\'})) {
          result.setLength(result.length() - 1);  // chop off escape char
          result.append('$');
          continue;
        }
        final int endPos = nextSymbol(argsString, dollarPos + 1);
        if (endPos > dollarPos + 1) {
          final String varName = argsString.substring(dollarPos + 1, endPos);
          final String value = interpreter.getVar(varName);
          if (value != null) {
            result.append(value);
          }
          else {
            result.append('$').append(argsString.substring(curPos, endPos));
          }
          curPos = endPos;
        }
        else {
          result.append('$');
          continue;
        }
      }
      else {
        result.append(argsString.substring(curPos, len));
        curPos = len;
      }
    }

    return result.toString();
  }

  protected static boolean isEscaped(String string, int pos, char[] escapeChars) {
    if (pos > 0) {
      final char prevChar = string.charAt(pos - 1);
      for (char escapeChar : escapeChars) {
        if (prevChar == escapeChar) return true;
      }
    }

    return false;
  }

  protected static int nextSymbol(String string, int fromPos) {
    final int len = string.length();
    while (fromPos < len && Character.isLetterOrDigit(string.charAt(fromPos))) ++fromPos;
    return fromPos;
  }

  protected static int findEndSingleQuote(char[] chars, int startPos) {
    while (startPos < chars.length) {
      if (chars[startPos] == '\'' && chars[startPos - 1] != '\\') {
        return startPos;
      }
      ++startPos;
    }
    return -1;
  }

  protected static int findEndDoubleQuote(char[] chars, int startPos) {
    while (startPos < chars.length) {
      if (chars[startPos] == '"' && chars[startPos - 1] != '\\') {
        return startPos;
      }
      ++startPos;
    }
    return -1;
  }

  protected static int findEndParen(char[] chars, int startPos) {
    int numParens = 1;
    while (startPos < chars.length && numParens > 0) {
      if (chars[startPos] == ')') {
        --numParens;
        if (numParens == 0) return startPos;
      }
      else if (chars[startPos] == '(') {
        ++numParens;
      }
      ++startPos;
    }
    return -1;
  }

  protected static int findEndWord(char[] chars, int startPos) {
    while (startPos < chars.length && chars[startPos] != ' ') ++startPos;
    return startPos;
  }
}
