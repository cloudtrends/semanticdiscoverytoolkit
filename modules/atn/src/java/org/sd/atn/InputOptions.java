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


import java.util.HashMap;
import java.util.Map;
import org.sd.xml.DataProperties;

/**
 * Container for passing input options down through the parsing stack.
 * <p>
 * @author Spence Koehler
 */
public class InputOptions {

  private DataProperties baseOptions;
  private Map<String, Map<String, AtnParseOptions>> parseOptionsCache;

  public InputOptions(DataProperties baseOptions) {
    this.baseOptions = baseOptions;
    this.parseOptionsCache = new HashMap<String, Map<String, AtnParseOptions>>();
  }

  public DataProperties getBaseOptions() {
    return baseOptions;
  }

  public void setBaseOptions(DataProperties baseOptions) {
    this.baseOptions = baseOptions;
  }

  /**
   * If a property in base options of the form:
   *   "compoundId:parserId:startRules"
   * is found, then use its value (split on commas) as the names
   * of the start rules to use in a copy of the parse options.
   * Otherwise, use the given options.
   */
  public AtnParseOptions getParseOptions(String compoundId, String parserId, AtnParseOptions parseOptions) {
    AtnParseOptions result = retrieveParseOptions(compoundId, parserId);
    if (result == null) {
      result = buildParseOptions(compoundId, parserId, parseOptions);
    }
    return result;
  }

  /**
   * Build and cache the parse options for the identified parser.
   */
  private final AtnParseOptions buildParseOptions(String compoundId, String parserId, AtnParseOptions parseOptions) {
    AtnParseOptions result = parseOptions;

    if (baseOptions != null) {
      final String startRulesProperty = buildStartRulesProperty(compoundId, parserId);
      final String startRules = baseOptions.getString(startRulesProperty, null);
      if (startRules != null && !"".equals(startRules)) {
        result = new AtnParseOptions(parseOptions);  // make a copy
        result.setStartRules(startRules.split("\\s*,\\s*"));
      }
    }

    // cache the result
    Map<String, AtnParseOptions> parserOptions = parseOptionsCache.get(compoundId);
    if (parserOptions == null) {
      parserOptions = new HashMap<String, AtnParseOptions>();
      parseOptionsCache.put(compoundId, parserOptions);
    }
    parserOptions.put(parserId, result);

    return result;
  }

  private final AtnParseOptions retrieveParseOptions(String compoundId, String parserId) {
    AtnParseOptions result = null;

    final Map<String, AtnParseOptions> parserOptions = parseOptionsCache.get(compoundId);
    if (parserOptions != null) {
      result = parserOptions.get(parserId);
    }

    return result;
  }

  public static final String buildStartRulesProperty(String compoundId, String parserId) {
    return buildProperty(compoundId, parserId, "startRules");
  }

  public static final String buildProperty(String compoundId, String parserId, String propertyName) {
    final StringBuilder result = new StringBuilder();
    result.append(compoundId).append(':').append(parserId).append(':').append(propertyName);
    return result.toString();
  }
}
