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
package org.sd.xml;


import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.Properties;
import org.sd.util.PropertiesParser;

/**
 * DOM-backed properties container.
 * <p>
 * @author Spence Koehler
 */
public class DataProperties extends BaseDataProperties {


  private LinkedList<DomDataProperties> domDataProperties;
  private Properties properties;
  private String[] remainingArgs;

  /**
   * Construct containing only environment variables.
   */
  public DataProperties() {
    init(new String[]{}, null);
  }

  /**
   * Args of form:
   *  - X.default.properties -- load env's DEFAULT_PROPERTIES_DIR's X.properties file
   *  - X.properties -- load X.properties file
   *  - property=value -- set property
   *  - X.xml -- load X.xml dom config file
   *
   * NOTEs:
   *  - Properties trump config (xml) mappings.
   *  - Later specified properties trump formerly specified properties.
   */
  public DataProperties(String[] args) {
    init(args, null);
  }

  public DataProperties(String[] args, String workingDir) {
    init(args, workingDir);
  }

  public DataProperties(Properties properties) {
    this.properties = properties;
    this.domDataProperties = new LinkedList<DomDataProperties>();
  }

  public DataProperties(DomElement domElement) {
    init(new String[]{}, null);

    final DomDataProperties ddp = new DomDataProperties(domElement);
    doAddDataProperties(ddp);

    final DataProperties elementProperties = domElement.getDataProperties();
    if (elementProperties != null) {
      this.properties = elementProperties.properties;
    }
  }

  public DataProperties(File xmlFile) throws IOException {
    init(new String[]{}, null);

    final DomDataProperties ddp = new DomDataProperties(xmlFile);
    doAddDataProperties(ddp);
  }

  /** Copy constructor. */
  public DataProperties(DataProperties other) {
    if (other == null) {
      init(new String[]{}, null);
    }
    else {
      this.domDataProperties = copy(other.domDataProperties);
      this.properties = copy(other.properties);
      this.remainingArgs = copy(other.remainingArgs);
    }
  }

  private final LinkedList<DomDataProperties> copy(LinkedList<DomDataProperties> otherDomDataProperties) {
    //NOTE: currently no deep copy on DomDataProperties
    return otherDomDataProperties == null ? null : new LinkedList<DomDataProperties>(otherDomDataProperties);
  }

  private final Properties copy(Properties otherProperties) {
    if (otherProperties == null) return null;

    final Properties result = new Properties();
    for (String name : otherProperties.stringPropertyNames()) {
      result.setProperty(name, otherProperties.getProperty(name));
    }

    return result;
  }

  private final String[] copy(String[] otherArgs) {
    String[] result = null;

    if (otherArgs != null) {
      result = new String[otherArgs.length];
      for (int i = 0; i < otherArgs.length; ++i) {
        result[i] = otherArgs[i];
      }
    }

    return result;
  }

  private final void init(String[] args, String workingDir) {
    try {
      this.domDataProperties = new LinkedList<DomDataProperties>();

      final PropertiesParser pp = new PropertiesParser(args, workingDir, true);
      this.properties = pp.getProperties();
      this.remainingArgs = pp.getArgs();

      final File base = (workingDir == null) ? new File(".") : new File(workingDir);

      for (String arg : pp.getArgs()) {
        if (arg.endsWith(".xml")) {
          final DomDataProperties ddp = new DomDataProperties(new File(base, arg));
          doAddDataProperties(ddp);
        }
      }
    }
    catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  private final void doAddDataProperties(DomDataProperties ddp) {
    domDataProperties.addFirst(ddp);
    ddp.getDomElement().setDataProperties(this);
  }

  public String[] getRemainingArgs() {
    return remainingArgs;
  }

  public Properties getProperties() {
    return properties;
  }

  public DomElement getDomElement() {
    return domDataProperties.size() > 0 ? domDataProperties.getFirst().getDomElement() : null;
  }

  public DomDataProperties getDomDataProperties() {
    return domDataProperties.size() > 0 ? domDataProperties.getFirst() : null;
  }

  /**
   * Add a dom element as a property.
   */
  public void addDomElement(DomElement domElement) {
    if (domElement != null) {
      final DomDataProperties ddp = new DomDataProperties(domElement);
      doAddDataProperties(ddp);
    }
  }

  /**
   * Get the first (most recently added) DomElement with the given name
   * (or null).
   */
  public DomElement getDomElement(String name) {
    DomElement result = null;

    for (DomDataProperties ddp : domDataProperties) {
      final DomElement curElt = ddp.getDomElement();
      if (name.equals(curElt.getLocalName())) {
        result = curElt;
        break;
      }
    }

    return result;
  }

  /**
   * Build an instance of the domNode's class specified by its relative
   * classXPath that takes the domNode object as its sole construction
   * parameter.
   */
  public Object buildInstance(DomNode domNode, String classXPath) {
    return domDataProperties.size() > 0 ? domDataProperties.getFirst().buildInstance(domNode, classXPath) : null;
  }

  /**
   * Add a new property or override an existing.
   */
  public void set(String key, String value) {
    if (value == null) {
      properties.remove(key);
    }
    else {
      properties.setProperty(key, value);
    }
  }

  /**
   * Add a new property or override an existing.
   */
  public void set(String key, boolean value) {
    properties.setProperty(key, Boolean.toString(value));
  }

  /**
   * Add a new property or override an existing.
   */
  public void set(String key, int value) {
    properties.setProperty(key, Integer.toString(value));
  }


  /**
   * Set the property to have the value iff its value is currently null.
   */
  public void setIfNull(String key, String value) {
    if (!hasProperty(key)) properties.setProperty(key, value);
  }

  /**
   * Set the property to have the value iff its value is currently null.
   */
  public void setIfNull(String key, boolean value) {
    if (!hasProperty(key)) properties.setProperty(key, Boolean.toString(value));
  }

  /**
   * Set the property to have the value iff its value is currently null.
   */
  public void setIfNull(String key, int value) {
    if (!hasProperty(key)) properties.setProperty(key, Integer.toString(value));
  }


  /**
   * Replace segments of the form "${x}" with getString(x) iff getString(x) != null.
   */
  public String replaceVariables(String text) {
    if (text == null) return null;

    final StringBuilder result = new StringBuilder();

    while (doReplaceVariables(text, result)) {
      text = result.toString();
      result.setLength(0);
    }

    return text;
  }

  public String getFilename(String key) {
    String filename = getString(key);
    return replaceVariables(filename);
  }

  public String getFilename(String key, String defaultValue) {
    String filename = getString(key, defaultValue);
    return replaceVariables(filename);
  }

  /**
   * Get the file referenced by the 'key', replacing variables and taking
   * the value of the 'workingDirKey' into account if present.
   * <p>
   * @return the file or null.
   */
  public File getFile(String key, String workingDirKey) {
    return getWorkingFile(getString(key, null), workingDirKey);
  }

  /**
   * Translate the filename into a File under the workingDir indicated
   * by the key if present -- note that the key itself may be null or
   * the value associated with the key may be null, in which case the
   * filename itself is used as the full file path.
   */
  public File getWorkingFile(String filename, String workingDirKey) {
    File result = null;

    if (filename != null) {
      filename = replaceVariables(filename);
      final String workingDir = filename.startsWith("/") ? null : replaceVariables(getString(workingDirKey, null));
      if (workingDir != null) {
        result = new File(new File(workingDir), filename);
      }
      else {
        result = new File(filename);
      }
    }

    return result;
  }

  private final boolean doReplaceVariables(String text, StringBuilder result) {
    boolean replaced = false;

    int ptr = 0;
    int varEndPos = 0;
    for (int varStartPos = text.indexOf("${"); varStartPos >= 0 && ptr < text.length(); varStartPos = text.indexOf("${", varEndPos + 1)) {
      if (varStartPos > ptr) {
        result.append(text.substring(ptr, varStartPos));
        ptr = varStartPos;
      }

      varEndPos = text.indexOf('}', varStartPos + 2);
      if (varEndPos < 0) {
        break;
      }

      final String var = text.substring(varStartPos + 2, varEndPos);
      final String val = getValueString(var);
      if (val != null) {
        // replace variable with value
        result.append(val);
        replaced = true;
      }
      else {
        // keep variable text unchanged
        result.append(text.substring(varStartPos, varEndPos + 1));
      }

      ptr = varEndPos + 1;
    }

    if (ptr < text.length()) result.append(text.substring(ptr));

    return replaced;
  }


  protected String getValueString(String key) {
    if (properties == null || key == null) return null;

    String result = properties.getProperty(key);

    if (result == null) {
      for (DomDataProperties ddp : domDataProperties) {
        result = ddp.getValueString(key);
        if (result != null) {
          // cache the result for faster access next time
          //set(key, result);
          break;
        }
      }
    }

    return result;
  }
}
