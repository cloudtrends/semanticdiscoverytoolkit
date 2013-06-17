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
package org.sd.text;


import org.sd.io.FileUtil;
import org.sd.util.logic.LogicalExpression;
import org.sd.util.logic.LogicalResult;
import org.sd.util.logic.TruthFunction;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Term finding utility for finding combinations of terms in text.
 * <p>
 * This works using LogicalExpression instances, TermFinderTruthFunctions,
 * and returns TermFinderLogicalResults.
 * 
 * @author Spence Koehler
 */
public class MultiTermFinder {

  /**
   * Default expression string to use for a single finder.
   */
  public static final String SINGLE_FINDER_EXPRESSION = "0";

  /**
   * Default "and" expression string to use for two finders.
   */
  public static final String DOUBLE_AND_FINDER_EXPRESSION = "(and 0 1)";

  /**
   * Default "or" expression string to use for two finders.
   */
  public static final String DOUBLE_OR_FINDER_EXPRESSION = "(or 0 1)";

  /**
   * Default "a not b" expression string to use for two finders.
   */
  public static final String DOUBLE_NOT_FINDER_EXPRESSION = "(and 0 (not 1))";

  /**
   * Default "neither a nor b" expression string to use for two finders.
   */
  public static final String DOUBLE_NEITHER_FINDER_EXPRESSION = "(not (or 0 1))";

  /**
   * Default "either a or b (but not both)" expression string to use for two finders.
   */
  public static final String DOUBLE_XOR_FINDER_EXPRESSION = "(or (and 0 (not 1)) (and (not 0) 1))";


  private TermFinderTruthFunction[] finders;
  private Map<String, LogicalExpression<String>> str2expr;
  private String[] expressions;

  /**
   * Construct with the term finders to be referenced in expressionStrings
   * by index number.
   */
  public MultiTermFinder(TermFinderTruthFunction[] finders) {
    this(finders, null);
  }

  /**
   * Construct with the term finders to be referenced in expressionStrings
   * by index number.
   */
  public MultiTermFinder(TermFinderTruthFunction[] finders, String[] expressions) {
    this.finders = finders;
    this.str2expr = new HashMap<String, LogicalExpression<String>>();
    this.expressions = expressions;
  }

  /**
   * Get expressions stored with this term finder.
   */
  public String[] getExpressions() {
    return expressions;
  }

  /**
   * Evaluate the expression string against the input using this instance's
   * finders.
   * <p>
   * See LogicalExpression's constructor and tests for info.
   * <p>
   * Note that some convenience constants are present in this class as examples
   * and that the possible expressions are not limited to the constants.
   * <p>
   * Note that the return value can be used to build an "explanation" for
   * the match, but the presence or absence of a match is indicated by a
   * non-null or null response, respectively.
   *
   * @return null if the expression fails to match the input; otherwise, a non-null
   *         list of matching TermFinderLogicalResult instances.
   */
  public List<LogicalResult<String>> evaluateLogicalExpression(String expressionString, String inputString) {
    LogicalExpression<String> expr = str2expr.get(expressionString);

    if (expr == null) {
      expr = new LogicalExpression<String>(expressionString, finders);
      str2expr.put(expressionString, expr);
    }

    return expr.evaluate(inputString);
  }

  /**
   * Find the first matching expression from this instance's expressions to the given
   * input string.
   */
  public List<LogicalResult<String>> findFirstMatch(String inputString) {
    List<LogicalResult<String>> result = null;

    if (expressions != null) {
      for (String expression : expressions) {
        result = evaluateLogicalExpression(expression, inputString);
        if (result != null) break;
      }
    }

    return result;
  }

  /**
   * Find all matching expression from this instance's expressions to the given
   * input string.
   */
  public List<LogicalResult<String>> findAllMatches(String inputString) {
    List<LogicalResult<String>> result = null;

    if (expressions != null) {
      for (String expression : expressions) {
        List<LogicalResult<String>> curResult = evaluateLogicalExpression(expression, inputString);
        if (curResult != null) {
          if (result == null) result = new ArrayList<LogicalResult<String>>();
          result.addAll(curResult);
        }
      }
    }

    return result;
  }

  /**
   * Convenience method to extract match positions from match results.
   */
  public List<int[]> getMatchPositions(List<LogicalResult<String>> matches) {
    List<int[]> result = null;

    if (matches != null) {
      result = new ArrayList<int[]>();
      for (LogicalResult<String> match : matches) {
        if (match instanceof TermFinderLogicalResult) {
          final TermFinderLogicalResult tfResult = (TermFinderLogicalResult)match;
          result.add(tfResult.getPatternPos());
        }
      }
    }

    return result;
  }

  public int getFinderIndex(TruthFunction<String> truthFunction) {
    int result = -1;

    for (int i = 0; i < finders.length; ++i) {
      if (finders[i] == truthFunction) {
        result = i;
        break;
      }
    }

    return result;
  }

  /**
   * Load a MultiTermFinder from a definition file with the following format:
   * <ul>
   * <li>Blank lines and lines beginning with '#' are ignored as formatting and comments.</li>
   * <li>Lines beginning with '%' contain a logical expression string (after the %).</li>
   * <li>Lines beginning with '$' contain [caseSensitive] and [matchFlag] (space delimited)
   *     to begin definition of a term finder as referenced by logical expression(s)
   *     and are followed by lines with comma-delimited terms (ignoring blank lines and comments)
   *     and/or lines beginning with '@' which reference a file with terms to load.</li>
   *     All terms are considered to be included in a term finder until another '$'
   *     or EOF is encountered.
   * </ul>
   * Note that logical expressions denote term finders by 0-based index in the order
   * they appear in the file. That is, the first '$' section defines term finder 0,
   * the next '$' section defines term finder 1, and so on.
   */
  public static final MultiTermFinder loadFromFile(File multiDefFile) throws IOException {
    List<String> expressions = null;
    List<TermFinderTruthFunction> truthFunctions = new ArrayList<TermFinderTruthFunction>();
    final File contextDir = multiDefFile.getParentFile();

    final BufferedReader reader = FileUtil.getReader(multiDefFile);
    String line = null;
    while ((line = reader.readLine()) != null) {
      if (line.length() == 0) continue;  // ignore blank lines
      final char firstC = line.charAt(0);
      switch (firstC) {
        case '#' :  // ignore comment lines
          continue;
          
        case '%' :  // record expression
          if (expressions == null) expressions = new ArrayList<String>();
          expressions.add(line.substring(1).trim());
          break;
          
        case '$' :  // load term finder truth function
          loadTruthFunctions(truthFunctions, line, reader, contextDir);
          break;
      }
    }
    reader.close();

    return new MultiTermFinder(
      truthFunctions.toArray(new TermFinderTruthFunction[truthFunctions.size()]),
      expressions == null ? null : expressions.toArray(new String[expressions.size()]));
  }

  private static final void loadTruthFunctions(List<TermFinderTruthFunction> result, String defLine, BufferedReader reader, File contextDir) throws IOException {
    while (defLine != null) {
      final String[] defPieces = defLine.substring(1).trim().toLowerCase().split("\\s+");
      final boolean caseSensitive = "casesensitive".equals(defPieces[0]);

      if (!caseSensitive && !"caseinsensitive".equals(defPieces[0])) {
        throw new IllegalStateException("Bad defLine! Must have 'caseSensitive' or 'caseInsensitive' as first token! defLine=" + defLine);
      }

      final int acceptPartial = TermFinder.getMatchFlag(defPieces[1]);

      if (acceptPartial < 0) {
        throw new IllegalStateException("Bad defLine! Must have 'sub', 'prefix', 'suffix' or 'full' as second token! defLine=" + defLine);
      }

      final List<String> terms = new ArrayList<String>();
      defLine = loadTerms(terms, reader, contextDir, caseSensitive);

      if (terms.size() > 0) {
        result.add(new TermFinderTruthFunction(caseSensitive, terms.toArray(new String[terms.size()]), acceptPartial));
      }
    }
  }

  private static final String loadTerms(List<String> result, BufferedReader reader, File contextDir, boolean caseSensitive) throws IOException {
    boolean keepGoing = true;
    String line = null;
    while (keepGoing && (line = reader.readLine()) != null) {
      if (line.length() == 0) continue;  // ignore blank lines
      final char firstC = line.charAt(0);
      switch (firstC) {
        case '#' :  // ignore comment lines
          continue;

        case '$' :  // end of this term finder/start of another term finder
          keepGoing = false;
          break;

        case '@' :  // load referenced file
          loadTerms(result, line.substring(1).trim(), contextDir, caseSensitive);
          break;

        default :  // load comma-delimited terms
          addWords(result, line, caseSensitive);
      }
    }
    return line;
  }

  private static final void loadTerms(List<String> result, String filename, File contextDir, boolean caseSensitive) throws IOException {
    if (filename.length() == 0) return;
    final File file = (filename.charAt(0) == '/') ? new File(filename) : new File(contextDir, filename);

    final BufferedReader reader = FileUtil.getReader(file);
    String line = null;
    while ((line = reader.readLine()) != null) {
      if (line.length() == 0 || line.charAt(0) == '#') continue;
      addWords(result, line, caseSensitive);
    }
    reader.close();
  }

  private static final void addWords(List<String> result, String line, boolean caseSensitive) {
    if (line.indexOf(',') >= 0) {
      final String[] words = line.split("\\s*,\\s*");
      for (String word : words) {
        if (word.length() > 0) {
          if (!caseSensitive) word = word.toLowerCase();  // normalize
          result.add(word);
        }
      }
    }
    else {
      line = line.trim();
      if (line.length() > 0) {
        if (!caseSensitive) line = line.toLowerCase();  // normalize
        result.add(line);
      }
    }
  }


  public static final void main(String[] args) throws IOException {
    //arg0: mtf def file
    //args1+: strings or files to run through
    //stdout:  match/non-match | string

    final File mtfFile = new File(args[0]);
    final MultiTermFinder mtf = MultiTermFinder.loadFromFile(mtfFile);

    for (int i = 1; i < args.length; ++i) {
      final String string = args[i];
      final File stringFile = new File(string);
      if (stringFile.exists()) {
        // apply to the strings in the file.
        final BufferedReader reader = FileUtil.getReader(stringFile);
        String line = null;
        while ((line = reader.readLine()) != null) {
          if (line.length() == 0 || line.charAt(0) == '#') continue;

          String matchNonMatch = "nonMatch";
          if (mtf.findFirstMatch(line) != null) {
            matchNonMatch = "match";
          }

          System.out.println(matchNonMatch + "|" + line);
        }
        reader.close();
      }
      else {
        // apply to the string.
        String matchNonMatch = "nonMatch";
        if (mtf.findFirstMatch(string) != null) {
          matchNonMatch = "match";
        }

        System.out.println(matchNonMatch + "|" + string);
      }
    }
  }
}
