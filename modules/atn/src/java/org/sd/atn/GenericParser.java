/*
    Copyright 2013 Semantic Discovery, Inc. (www.semanticdiscovery.com)

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
package org.sd.atn;


import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import org.sd.xml.DataProperties;

/**
 * Wrapper for a generic config-based AtnParseRunner.
 * <p>
 * @author Spence Koehler
 */
public class GenericParser {

  private DataProperties originalOptions;
  private DataProperties localOptions;
  private AtnParseRunner parseRunner;
  private GenericParseHelper genericParseHelper;
  private String startRules;

  //
  // Properties
  //
  //   parseConfig -- (required) path to data properties (config) file (xml)
  //   resourcesDir -- (required) path to resources (e.g. "${HOME}/co/ancestry/resources")
  //   startRules -- (optional) comma delimited names of start rule(s) of form
  //                            compoundParserId:parserId:startRulName
  //
  //  Debugging options
  //   verbose -- (optional, default=false) true to turn on verbosity
  //   trace -- (optional, default=false) true to trace/debug AtnStates
  //
  public GenericParser(DataProperties options) throws IOException {
    this.originalOptions = options;
    this.localOptions = new DataProperties(options);
    this.parseRunner = new AtnParseRunner(options);
    this.genericParseHelper = new GenericParseHelper();
    this.startRules = options.getString("startRules", null);

    if (startRules != null && !"".equals(startRules)) {
      setStartRules(startRules);
    }
  }

  /**
   * Clean-up/release resources used by this parser.
   */
  public void close() {
    parseRunner.close();
  }

  public DataProperties getOriginalOptions() {
    return originalOptions;
  }

  public DataProperties getLocalOptions() {
    return localOptions;
  }

  public AtnParseRunner getParseRunner() {
    return parseRunner;
  }

  public String getStartRules() {
    return startRules;
  }

  /**
   * Set the start rules.
   *
   * @return true if start rules were set; false if there was a problem (bad value).
   */
  public final boolean setStartRules(String startRules) {
    boolean result = false;

    if (startRules != null && !"".equals(startRules)) {
      final String[] pieces = startRules.split("\\s*:\\s*");
      if (pieces.length == 3) {
        localOptions.set(InputOptions.buildStartRulesProperty(pieces[0], pieces[1]), pieces[2]);

        this.startRules = startRules;
        result = true;
      }
      //else: bad arg -- result=false
    }

    return result;
  }

  /**
   * Set start rules back to config default.
   */
  public final void unsetStartRules() {
    this.localOptions = new DataProperties(originalOptions);
  }

  public GenericParseResults parse(String inputText, DataProperties options, AtomicBoolean die) {
    ParseOutputCollector parseOutput = null;

    if (inputText != null && !"".equals(inputText)) {
      try {
        parseOutput = parseRunner.parseInputString(inputText, options, die);
      }
      catch (IOException e) {
        throw new IllegalStateException(e);
      }
    }

    return genericParseHelper.buildGenericParseResults(parseOutput);
  }
}
