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
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
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
  
  /**
   * Environment variable to poll for default properties directory.
   */
  public static final String DEFAULT_PROPERTIES_VAR = "DEFAULT_PROPERTIES_DIR";

  /**
   * Default properties directory (if unspecified).
   */
  public static final String DEFAULT_PROPERTIES_DIR = "/home/" + ExecUtil.getUser() + "/cluster/resources/properties";

  private static boolean lostResourceVerbose = false;
  public static final boolean setLostResourceVerbose(boolean v) {
    final boolean result = lostResourceVerbose;
    lostResourceVerbose = v;
    return result;
  }


  private Properties properties;
  private String[] args;
  private String context;
  private String _defaultPropertiesDir;

  private Map<String, Set<String>> prefix2keys;

  /**
   * Construct with default properties.
   */
  public PropertiesParser() {
    this((Properties)null);
  }

  /**
   * Construct with the given base properties.
   */
  public PropertiesParser(Properties base) {
    this.properties = (base == null) ? new Properties() : new Properties(base);
    this.args = new String[0];
    this.context = null;
  }

  /**
   * Construct with the given args.
   */
  public PropertiesParser(String[] args) throws IOException {
    this(null, args, false);
  }

  public PropertiesParser(String[] args, String workingDir, boolean getenv) throws IOException {
    this(null, args, workingDir, getenv);
  }

  /**
   * Construct with the given args using the given base properties.
   */
  public PropertiesParser(Properties base, String[] args)  throws IOException {
    this(base, args, false);
  }

  /**
   * Construct with the given args, optionally including environment vars.
   */
  public PropertiesParser(String[] args, boolean getenv) throws IOException {
    this(null, args, getenv);
  }

  public PropertiesParser(Properties base, String[] args, boolean getenv) throws IOException {
    this(base, args, null, getenv);
  }

  public PropertiesParser(Properties base, String[] args, String workingDir, boolean getenv) throws IOException {
    this(base);

    parseArgs(args, workingDir);

    if (getenv) getEnvironmentVars();
  }

  /**
   * Taking the arguments in order, where later arguments override earlier
   * arguments, load
   * <ul>
   * <li>the default .properties file of any argument that ends in ".default.properties"</li>
   * <li>the file of any argument that ends in ".properties"</li>
   * <li>any argument of the form "property=value"</li>
   * </ul>
   */
  private final void parseArgs(String[] args, String workingDir) throws IOException {
    final List<String> remainingArgs = new ArrayList<String>();

    for (String arg : args) {
      if (arg.endsWith(".default.properties")) {
        loadDefaultProperties(this.properties, arg, workingDir);
      }
      else if (arg.endsWith(".properties")) {
        loadFileProperties(this.properties, arg, workingDir);
      }
      else if (arg.length() > 2 && arg.charAt(0) == '<' && arg.charAt(arg.length() - 1) == '>') {
        // add xml argument
        remainingArgs.add(arg);
      }
      else if (arg.indexOf('=') > 0) {
        setProperty(this.properties, arg);
      }
      else {
        remainingArgs.add(arg);
      }
    }

    this.args = remainingArgs.toArray(new String[remainingArgs.size()]);
  }

  private final void loadDefaultProperties(Properties defaultProperties, String arg, String workingDir) throws IOException {
    File path = null;

    if (isAbsolute(arg)) {
      // use path specified with arg; still remove ".default" from "x.default.properties"
      final String filename = arg.replaceFirst(".default.properties", ".properties");
      path = workingDir == null ? new File(filename) : new File(workingDir, filename);
    }
    else {
      // use default path: "/home/$USER/cluster/resources/properties/x.properties" given "x.default.properties"
      final String filename = arg.replaceFirst(".default.properties", ".properties");
      if (workingDir != null) {
        path = new File(workingDir, filename);
      }
      else {
        path = FileUtil.getFile(filename);
      }
    }

    if (path == null) {
      throw new IllegalArgumentException("Can't decode default.properties arg '" + arg + "'! (workingDir=" + workingDir + ")");
    }
    else {
      doLoad(defaultProperties, path, arg);
    }
  }

  private final void loadFileProperties(Properties fileProperties, String arg, String workingDir) throws IOException {
    File file = (workingDir == null) ? FileUtil.getFile(arg) : new File(workingDir, arg);

    if (file != null && file.exists()) {
      // use path specified by arg
      doLoad(fileProperties, file, arg);
    }
    else {
      file = new File(new File(System.getProperty("user.dir")), arg);

      if (file != null && file.exists()) {
        // use path under user.dir to arg
        doLoad(fileProperties, file, arg);
      }
      else {
        // search classpath for "x.properties"
        loadProperties(this.properties, arg, false/*verbose*/);
      }
    }
  }

  private final void setProperty(Properties properties, String arg) {
    String[] pieces = null;

    final int eqPos = arg.indexOf('=');

    if (eqPos < 0) {
      pieces = new String[]{arg};
    }
    else {
      pieces = new String[]{arg.substring(0, eqPos), arg.substring(eqPos + 1)};
    }

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

  /**
   * Get the default properties dir.
   * <p>
   * This identifies the location at which X.default.properties should be found
   * as X.properties. It defaults to "$DEFAULT_PROPERTIES_DIR/properties" or
   * "/home/$USER/cluster/resources/properties"
   */
  private final String getDefaultPropertiesDir() {
    if (_defaultPropertiesDir == null) {
      String envDir = System.getenv(DEFAULT_PROPERTIES_VAR);
      if (envDir == null) {
        envDir = DEFAULT_PROPERTIES_DIR;
      }
    }
    return _defaultPropertiesDir;
  }

  /**
   * Determine whether the given path is absolute (from the root).
   * <p>
   * Account for paths like "/...", "~...", and "C:\\...".
   */
  private final boolean isAbsolute(String path) {
    final int cpos = path.indexOf(':');
    final char c = path.charAt(cpos + 1);
    return (c == '/' || c == '\\' || c == '~');
  }

  /**
   * Combine the properties into one, where later properties will "shadow"
   * earlier properties.
   */
  public static final Properties combineProperties(Properties[] properties) {
    final Properties result = new Properties();

    if (properties != null) {
      for (Properties p : properties) {
        result.putAll(p);
      }
    }

    return result;
  }

  /**
   * Load as properties all resources with the given name.
   *
   * @param properties  The properties in which to load the found resources (ok if null).
   * @param name  The name of the properties resource to load.
   * @param verbose  When true, show resources from which properties resources are loaded.
   *
   * @return the loaded properties (== properties if non-null).
   */
  public static final Properties loadProperties(Properties properties, String name, boolean verbose) throws IOException {
    final Properties result = properties == null ? new Properties() : properties;

    boolean gotResource = false;

    final Enumeration<URL> pUrls = ClassLoader.getSystemResources(name);
    if (pUrls != null) {
      while (pUrls.hasMoreElements()) {
        final URL pUrl = pUrls.nextElement();
        try {
          final URI pUri = pUrl.toURI();
          final File pFile = new File(pUri);
          final BufferedReader reader = FileUtil.getReader(pFile);
          result.load(reader);
          reader.close();

          gotResource = true;

          if (verbose) {
            System.out.println(new Date() + ": PropertiesParser loaded '" + pUrl.toString() + "'");
          }
        }
        catch (URISyntaxException e) {
          throw new IOException(e);
        }
      }
    }

    if (!gotResource) {
      System.err.println("***WARNING: Couldn't load resource '" + name + "' (user.dir=" +
                         System.getProperty("user.dir") + ")");
      if (lostResourceVerbose) new Exception("Lost Resource").printStackTrace(System.err);
    }

    return result;
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

    final String keyPrefix = concat(context, propertyName);
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

    final String keyPrefix = concat(context, propertyName);
    final Set<String> keys = getKeys(keyPrefix);
    if (keys != null) {
      result = new ArrayList<Pair>();
      for (String key : keys) {
        result.add(new Pair(key, properties.getProperty(key), propertyName, context));
      }
    }

    return result;
  }

  public boolean hasContextProperty(String propertyName) {
    return hasContextProperty(propertyName, context);
  }

  public boolean hasContextProperty(String propertyName, String context) {
    boolean result = false;

    final String keyPrefix = concat(context, propertyName);    

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
    getEnvironmentVars(null);
  }

  /**
   * Get environment vars, setting properties for those that are not already
   * properties. In other words, properties override environment variables.
   *
   * @param prefix  If non-null, then set properties "prefix.key" for each env
   *                key. If null, property keys will match environment keys.
   */
  private final void getEnvironmentVars(String prefix) {
    final Map<String, String> env = System.getenv();

    if (this.properties == null) this.properties = new Properties();

    for (Map.Entry<String, String> entry : env.entrySet()) {
      String key = entry.getKey();
      final String value = entry.getValue();

      if (prefix != null) key = concat(prefix, key);

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
   *
   * @param properties  The properties from which to retrieve the values.
   * @param propertyPrefix  The property name (before the ordering '_').
   */
  public static final String[] getMultiValues(Properties properties, String propertyPrefix) {
    return getMultiValues(properties, null, propertyPrefix, null);
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
   *
   * @param properties  The properties from which to retrieve the values.
   * @param prefix  The prefix for the property (name) for generating property
   *                names of the form "prefix.property".
   * @param propertyPrefix  The property name (before the ordering '_').
   */
  public static final String[] getMultiValues(Properties properties, String prefix, String propertyPrefix) {
    return getMultiValues(properties, prefix, propertyPrefix, null);
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
   *
   * @param properties  The properties from which to retrieve the values.
   * @param prefix  The prefix for the property (name) for generating property
   *                names of the form "prefix.property".
   * @param propertyPrefix  The property name (before the ordering '_').
   * @param defaultValues  The default values to apply when a value is not found.
   *                       Note that if the defaultValue size is less than the
   *                       found value size, then the last defaultValue will be
   *                       used (even if null).
   */
  public static final String[] getMultiValues(Properties properties, String prefix, String propertyPrefix, String[] defaultValues) {
    Set<String> propertyNames = null;
    
    propertyPrefix = concat(prefix, propertyPrefix);

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
      String curProperty = properties.getProperty(propertyName);
      if (curProperty == null && defaultValues != null && defaultValues.length > 0) {
        curProperty = defaultValues[index >= defaultValues.length ? defaultValues.length - 1 : index];
      }
      result[index++] = curProperty;
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
   * Concatenate the prefix to the property with an intermediate '.', if non-null.
   *
   * @param prefix  The prefix to the result (ok if null)
   * @param property  The property.
   *
   * @return "prefix.property" or "property"
   */
  public static final String concat(String prefix, String property) {
    if (prefix == null) return property;  // shortcut out
    if (property == null) return prefix;  // shortcut out

    final StringBuilder result = new StringBuilder();
    result.append(prefix).append('.').append(property);
    return result.toString();
  }

  /**
   * Get the value for the concatenated property from the properties, applying
   * the prefix as "prefix.property".
   *
   * @param properties  The properties from which to retrieve a value.
   * @param prefix  The prefix for the property (name).
   * @param property  The property (name).
   *
   * @return the value for the property (possibly null).
   */
  public static final String getProperty(Properties properties, String prefix,
                                         String property) {
    return getProperty(properties, prefix, property, null, false);
  }

  /**
   * Get the value for the concatenated property from the properties, applying
   * the prefix as "prefix.property".
   *
   * @param properties  The properties from which to retrieve a value.
   * @param prefix  The prefix for the property (name).
   * @param property  The property (name).
   * @param defaultValue  The default value to retrieve if no value for the
   *                      property (or its fallback if applied) is set.
   *
   * @return the value for the property (possibly null).
   */
  public static final String getProperty(Properties properties, String prefix,
                                         String property, String defaultValue) {
    return getProperty(properties, prefix, property, defaultValue, false);
  }

  /**
   * Get the value for the concatenated property from the properties, applying
   * the prefix as "prefix.property".
   *
   * @param properties  The properties from which to retrieve a value.
   * @param prefix  The prefix for the property (name).
   * @param property  The property (name).
   * @param defaultValue  The default value to retrieve if no value for the
   *                      property (or its fallback if applied) is set.
   * @param fallback  If true, then return the value associated with "property"
   *                  iff the value for "prefix.property" is null.
   *
   * @return the value for the property (possibly null).
   */
  public static final String getProperty(Properties properties, String prefix,
                                         String property, String defaultValue,
                                         boolean fallback) {

    // shortcut out
    if (prefix == null) {
      return properties.getProperty(property, defaultValue);
    }

    // get the concatenated property's value.
    String result = properties.getProperty(concat(prefix, property));

    // fallback if warranted
    if (result == null && fallback && prefix != null) {
      result = properties.getProperty(property);
    }

    // apply the default value if needed
    if (result == null && defaultValue != null) {
      result = defaultValue;
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
