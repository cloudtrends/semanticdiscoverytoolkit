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
package org.sd.util;


import org.sd.io.FileUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

/**
 * Utility class to parse and load properties.
 * <p>
 * Given command line arguments, extract those that identify properties. Load
 * and provide access to the properties and the remaining args.
 * <p>
 * Arguments that will be extracted are as follows:
 * <ul>
 * <li>Each argument that ends with ".default.properties" will be loaded from either
 *     the default property path (/home/$USER/cluster/properties/X.properties) or
 *     from the specified path (/.../X.properties). These will be the default
 *     or fallback properties if not overridden.</li>
 * <li>Each argument that ends with ".properties" will be loaded from the file
 *     specified in that argument.</li>
 * <li>Each argument of the form "X=Y" will be set as a property that overrides
 *     the file and default properties.</li>
 * </ul>
 * @author Spence Koehler
 */
public class PropertiesParser {
  
  private Properties properties;
  private String[] args;
  private String context;

  private Map<String, Set<String>> prefix2keys;

  /**
   * Construct with default properties.
   */
  public PropertiesParser() {
    this.properties = new Properties();
    this.args = new String[0];
    this.context = null;
  }

  /**
   * Construct with the given args.
   */
  public PropertiesParser(String[] args) throws IOException {
		this(args, false);
  }

  /**
   * Construct with the given args, optionally including environment vars.
   */
  public PropertiesParser(String[] args, boolean getenv) throws IOException {
    parseArgs(args);

		if (getenv) getEnvironmentVars();
  }

  /**
   * Load the default .properties file of any argument that ends in ".default.properties".
   * Load the file of any argument that ends in ".properties" (overrides defaults)
   * Load any argument of the form "property=value" as overriding properties (overrides files).
   */
  private final void parseArgs(String[] args) throws IOException {
    final List<String> remainingArgs = new ArrayList<String>();

    final Properties defaultProperties = new Properties();
    final Properties fileProperties = new Properties(defaultProperties);
    final Properties properties = new Properties(fileProperties);

    boolean gotOne = false;

    for (String arg : args) {
      if (arg.endsWith(".default.properties")) {
        loadDefaultProperties(defaultProperties, arg);
        gotOne = true;
      }
      else if (arg.endsWith(".properties")) {
        loadFileProperties(fileProperties, arg);
        gotOne = true;
      }
      else if (arg.indexOf('=') > 0) {
        setProperty(properties, arg);
        gotOne = true;
      }
      else {
        remainingArgs.add(arg);
      }
    }

    this.args = remainingArgs.toArray(new String[remainingArgs.size()]);
    this.properties = gotOne ? properties : this.properties;
    this.context = null;
  }

  private final void loadDefaultProperties(Properties defaultProperties, String arg) throws IOException {
    String path = null;

		final int cpos = arg.indexOf(':');
    final char c = arg.charAt(cpos + 1);
    if (c == '/' || c == '~') {
      // use path specified with arg; still remove ".default" from "x.default.properties"
      path = arg.replaceFirst(".default.properties", ".properties");
    }
    else {
      // use default path: "/home/$USER/cluster/resources/properties/x.properties" given "x.default.properties"
      path = getDefaultPropertyPath() + arg.replaceFirst(".default.properties", ".properties");
    }

    final File file = FileUtil.getFile(path);
    doLoad(defaultProperties, file, arg);
  }

  private final void doLoad(Properties properties, File file, String arg) throws IOException {
    if (file.exists()) {
      final BufferedReader reader = FileUtil.getReader(file);
      properties.load(reader);
      reader.close();
    }
    else {
      throw new IllegalArgumentException("Can't find default properties at '" +
                                         file.getAbsolutePath() + "' using arg='" +
                                         arg + "'");
    }
  }

  private final void loadFileProperties(Properties fileProperties, String arg) throws IOException {
    final File file = FileUtil.getFile(arg);
    doLoad(fileProperties, file, arg);
  }

  private final void setProperty(Properties properties, String arg) {
    final String[] pieces = arg.split("=");

    if (pieces.length == 1) {
      properties.setProperty(pieces[0], "");
    }
    else {
      properties.setProperty(pieces[0], pieces[1]);
    }
  }

  public void setProperty(String propertyName, String value) {
    if (properties == null) properties = new Properties();
    properties.setProperty(propertyName, value);
  }

  /**
   * Get the path to the default properties directory for this user.
   * <p>
   * The default path is: "/home/$USER/cluster/resources/properties/".
   */
  public static final String getDefaultPropertyPath() {
    final StringBuilder result = new StringBuilder();

    result.
      append("/home/").
      append(ExecUtil.getUser()).
      append("/cluster/resources/properties/");

    return result.toString();
  }

  /**
   * Get the properties loaded according to the original arguments.
   */
  public Properties getProperties() {
    if (properties == null) this.properties = new Properties();
    return properties;
  }

  /**
   * Get the named property's value (possibly null).
   * <p>
   * Note that for this method, the context is not applied. The exact property
   * name is retrieved.
   */
  public String getProperty(String propertyName) {
    return (properties == null) ? null : properties.getProperty(propertyName);
  }

  /**
   * Get the remaining arguments after extracting all properties.
   */
  public String[] getArgs() {
    return args;
  }

  /**
   * Set the current context under which properties should be retrieved.
   * <p>
   * This affects subsequent getProperty calls such that getProperty(x) will result
   * in retrieving the "context.x" property.
   */
  public void setContext(String context) {
    this.context = context;
  }

  /**
   * Get the current context.
   */
  public String getContext() {
    return context;
  }

  /**
   * Clear the current context.
   */
  public void clearContext() {
    this.context = null;
  }

  /**
   * Get the property values (under the currently set context) of the form
   * property_X, (alphabetically) sorted by X.
   * <p>
   * Note that if numbers are used for X they should be prepended with zero's
   * as needed to ensure a proper alphabetical sort.
   */
  public String[] getProperties(String propertyName) {
    return getProperties(propertyName, context);
  }

  /**
   * Get the property under the given context.
   * <p>
   * Note that this retrieves the "context.property" property if context is
   * non-null.
   * <p>
   * Note that if numbers are used for X they should be prepended with zero's
   * as needed to ensure a proper alphabetical sort.
   *
   * @return the non-empty found properties or null if none were found.
   */
  public String[] getProperties(String propertyName, String context) {
    String[] result = null;

    final String keyPrefix = getKeyPrefix(propertyName, context);
    final Set<String> keys = getKeys(keyPrefix);
    if (keys != null) {
      result = new String[keys.size()];
      int index = 0;
      for (String key : keys) {
        result[index++] = properties.getProperty(key);
      }
    }

    return result;
  }

  /**
   * Get the property key/value pairs under the given context.
   * <p>
   * Note that this retrieves the "context.property" property if context is
   * non-null.
   * <p>
   * Note that if numbers are used for X they should be prepended with zero's
   * as needed to ensure a proper alphabetical sort.
   *
   * @return the non-empty found properties or null if none were found.
   */
  public List<Pair> findProperties(String propertyName, String context) {
    List<Pair> result = null;

    final String keyPrefix = getKeyPrefix(propertyName, context);
    final Set<String> keys = getKeys(keyPrefix);
    if (keys != null) {
      result = new ArrayList<Pair>();
      for (String key : keys) {
        result.add(new Pair(key, properties.getProperty(key), propertyName, context));
      }
    }

    return result;
  }

  public static final String getKeyPrefix(String propertyName, String context) {
    return (context == null) ? propertyName : context + "." + propertyName;
  }

  public boolean hasContextProperty(String propertyName) {
    return hasContextProperty(propertyName, context);
  }

  public boolean hasContextProperty(String propertyName, String context) {
    boolean result = false;

    final String keyPrefix = getKeyPrefix(propertyName, context);    

    if (prefix2keys != null && prefix2keys.get(keyPrefix) != null) {
      result = true;
    }
    else {
      for (String pName : properties.stringPropertyNames()) {
        if (pName.startsWith(keyPrefix)) {
          result = true;
          break;
        }
      }
    }

    return result;
  }

  /**
   * Find property names that start with "keyPrefix" in alphabetical order.
   */
  private Set<String> getKeys(String keyPrefix) {
    Set<String> result = (prefix2keys == null) ? null : prefix2keys.get(keyPrefix);

    if (result == null) {
      if (prefix2keys == null) prefix2keys = new HashMap<String, Set<String>>();

      // find property names starting with keyPrefix
      for (String propertyName : properties.stringPropertyNames()) {
        if (propertyName.startsWith(keyPrefix)) {
          if (result == null) result = new TreeSet<String>();
          result.add(propertyName);
        }
      }

      prefix2keys.put(keyPrefix, result);
    }

    return result;
  }

	/**
	 * Get environment vars, setting properties for those that are not already
	 * properties. In other words, properties override environment variables.
	 */
	private final void getEnvironmentVars() {
		final Map<String, String> env = System.getenv();

		if (this.properties == null) this.properties = new Properties();

		for (Map.Entry<String, String> entry : env.entrySet()) {
			final String key = entry.getKey();
			final String value = entry.getValue();

			if (properties.getProperty(key) == null) {
				properties.setProperty(key, value);
			}
		}
	}

  /**
   * Utility method to get a property as an integer.
   */
  public static final Integer getInt(Properties properties, String property, String defaultValue) {
    Integer result = null;

    final String value = properties.getProperty(property, defaultValue);
    if (value != null) {
      result = new Integer(value);
    }
    
    return result;
  }

  /**
   * Utility method to get a property as a boolean.
   * <p>
   * The result is true if the property's value is "true"; otherwise, false.
   */
  public static final Boolean getBoolean(Properties properties, String property, String defaultValue) {
    Boolean result = null;

    final String value = properties.getProperty(property, defaultValue);
    if (value != null) {
      result = new Boolean("true".equals(value.toLowerCase()));
    }

    return result;
  }

  /**
   * Utility method to get a property as an integer.
   */
  public static final Double getDouble(Properties properties, String property, String defaultValue) {
    Double result = null;

    final String value = properties.getProperty(property, defaultValue);
    if (value != null) {
      result = new Double(value);
    }
    
    return result;
  }

  /**
   * Split property's value on comma.
   */
  public static final String[] getStrings(Properties properties, String property) {
    String[] result = null;

    final String value = properties.getProperty(property);
    if (value != null) {
      result = value.split("\\s*,\\s*");
    }

    return result;
  }

  /**
   * Get all values for properties with the given prefix in sorted order.
   * <p>
   * This is intended for getting values for multiple ordered properties of the form:
   * <p>
   * propertyName_1=value1 <p>
   * propertyName_2=value2 <p>
   * and so on.
   * <p>
   * Note that _1, _2, can be _A, _B, or full word strings and their values will be
   * collected in alphabetical order.
   */
  public static final String[] getMultiValues(Properties properties, String propertyPrefix) {
    Set<String> propertyNames = null;
    
    for (String propertyName : properties.stringPropertyNames()) {
      if (propertyName.startsWith(propertyPrefix)) {
        if (propertyNames == null) propertyNames = new TreeSet<String>();
        propertyNames.add(propertyName);
      }
    }

    if (propertyNames == null) return null;

    final String[] result = new String[propertyNames.size()];
    int index = 0;
    for (String propertyName : propertyNames) {
      result[index++] = properties.getProperty(propertyName);
    }

    return result;
  }

  /**
   * Find the name of the first existing property from names.
   *
   * @return the found property name or null.
   */
  public static final String findProperty(Properties properties, String[] names) {
    String result = null;

    for (String name : names) {
      if (properties.containsKey(name)) {
        result = name;
        break;
      }
    }

    return result;
  }

  /**
   * Get the names of properties that are missing.
   *
   * @return null if non are missing; otherwise, return the names of the missing properties
   *         in a comma delimited string.
   */
  public static final String getMissingProperties(Properties properties, String[] names) {
    StringBuilder result = null;

    for (String name : names) {
      if (!properties.containsKey(name)) {
        if (result == null) result = new StringBuilder();
        if (result.length() > 0) result.append(", ");
        result.append(name);
      }
    }

    return result == null ? null : result.toString();
  }

  /**
   * Get the name of properties that are missing from one of the sets.
   * <p>
   * It is assumed that the sets are mutually exclusive and that when
   * one property is present for a set, all properties must be present
   * for that set and no properties are present for any other set.
   * <p>
   * If no property is found, create a string representing all of the sets
   * separated by " or " as a delimiter.
   *
   * @return null if one set of properties is fully present; otherwise a
   *         string representing the missing properties.
   */
  public static final String getMissingProperties(Properties properties, String[][] alternateNameSets) {
    StringBuilder result = null;

    for (String[] names : alternateNameSets) {
      final String foundName = findProperty(properties, names);
      final String missing = getMissingProperties(properties, names);

      if (foundName == null) {
        // none in this set are present. collect the names for if no sets are present.
        if (result == null) result = new StringBuilder();
        if (result.length() > 0) result.append(" or ");
        result.append(missing);
      }
      else {
        // one in this set is present.
        result = new StringBuilder();
        result.append(missing);

        break;  // assume only one set applies.
      }
    }

    return result == null ? null : result.toString();
  }

  /**
   * Container class for a key/value pair.
   */
  public static final class Pair {
    public final String key;           // full key with context and ordinal info
    public final String value;         // property value retrieved for key
    public final String propertyName;  // propertyName (without ordinal info)
    public final String context;       // just the context

    private String _trimmedKey;
    private String _ordinal;

    public Pair(String key, String value, String propertyName, String context) {
      this.key = key;
      this.value = value;
      this.propertyName = propertyName;
      this.context = context;
      this._trimmedKey = null;
      this._ordinal = null;
    }

    /**
     * Get the trimmed key. This will be the propertyName with any ordinal information included.
     */
    public String getTrimmedKey() {
      if (_trimmedKey == null) {
        if (context == null) {
          _trimmedKey = key;
        }
        else {
          // trim context off the front of the key.
          _trimmedKey = key.substring(context.length() + 1);
        }
      }
      return _trimmedKey;
    }

    /**
     * Get the ordinal (including any preceding delimiters).
     */
    public String getOrdinal() {
      if (_ordinal == null) {
        final String trimmedKey = getTrimmedKey();
        if (propertyName == null) {
          _ordinal = trimmedKey;
        }
        else {
          _ordinal = trimmedKey.substring(propertyName.length());
        }
      }
      return _ordinal;
    }
  }


  public static final void main(String[] args) throws IOException {
    final PropertiesParser pp = new PropertiesParser(args);
    final Properties properties = pp.getProperties();

    final Set<String> keys = properties.stringPropertyNames();
    for (String property : keys) {
      System.out.println(property + "=" + properties.getProperty(property));
    }
  }
}