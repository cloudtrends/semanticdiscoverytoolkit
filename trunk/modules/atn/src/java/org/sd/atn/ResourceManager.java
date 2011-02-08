/*
    Copyright 2010 Semantic Discovery, Inc. (www.semanticdiscovery.com)

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


import java.io.File;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import org.sd.token.Normalizer;
import org.sd.util.ReflectUtil;
import org.sd.xml.DataProperties;
import org.sd.xml.DomElement;
import org.sd.xml.DomNode;
import org.w3c.dom.NodeList;

/**
 * Class to hold and manage reflective resources.
 * <p>
 * Reflective constructors are assumed to take 2 arguments:
 * <ol>
 * <li>DomNode -- the DOM Element node containing a 'jclass' and any other,
 *                elements defining the class where the text under 'jclass'
 *                is the classpath of the instance being built.</li>
 * <li>ResourceManager -- the resource manager being used to create and store
 *                        instances.</li>
 * </ol>
 *
 * @author Spence Koehler
 */
public class ResourceManager {

  private DataProperties options;
  private boolean disableLoad;

  /**
   * Map to store named instances for reference by later instances.
   */
  private Map<String, Object> name2resource;

  private Map<String, Normalizer> id2Normalizer;

  /**
   * Default constructor for empty instance.
   */
  public ResourceManager() {
  }

  /**
   * assumed: resourcesElement has "resource" children, each of which
   * has an optional name attribute; a mandatory "jclass" child whose
   * text content is a classpath for the resource; and other attributes
   * and children as required by the specific resource.
   */
  public ResourceManager(DomElement resourcesElement) {
    this.options = resourcesElement.getDataProperties();
    final DataProperties initOptions = new DataProperties(resourcesElement);
    init(initOptions);
  }

  /**
   * assumed: options' domElement has "resource" nodes.
   */
  public ResourceManager(DataProperties options) {
    init(options);
  }
  
  private final void init(DataProperties options) {
    if (this.options == null) this.options = options;
    this.name2resource = new HashMap<String, Object>();
    this.disableLoad = options.getBoolean("_disableLoad", false);

    final DomElement resourceElement = (options == null) ? null : options.getDomElement();
    loadResources(resourceElement);
    this.id2Normalizer = null;
  }

  public void setDisableLoad(boolean disableLoad) {
    this.disableLoad = disableLoad;
  }

  public void setId2Normalizer(Map<String, Normalizer> id2Normalizer) {
    this.id2Normalizer = id2Normalizer;
  }

  public Map<String, Normalizer> getId2Normalizer() {
    return id2Normalizer;
  }

  public final void loadResources(DomElement resourcesElement) {
    if (resourcesElement != null && !disableLoad) {
      final NodeList resourceNodes = resourcesElement.selectNodes("resource");
      if (resourceNodes != null) {
        for (int i = 0; i < resourceNodes.getLength(); ++i) {
          final DomElement resourceElement = (DomElement)resourceNodes.item(i);
          getResource(resourceElement);
        }
      }
    }
  }

  public void close() {
    for (Object resource : name2resource.values()) {
      ReflectUtil.invokeMethod(resource, "close", null, null);
    }
  }

  public DataProperties getOptions() {
    return options;
  }

  /**
   * Retrieve or load a resource.
   * <p>
   * The resource classname is the text content under the resourceElement's
   * "jclass" child. The (optional) name of the resource is in the
   * resourceElement's "name" attribute.
   * <p>
   * If the resource is named and there is already a mapping for the name,
   * the previously constructed instance will be retrieved; otherwise, the
   * resource will be constructed and stored with its name for later retrieval.
   */
  public final Object getResource(DomElement resourceElement) {
    return getResource(resourceElement, null);
  }

  public synchronized final Object getResource(DomElement resourceElement, Object[] extraArgs) {
    Object result = null;

    if (resourceElement == null) return null;

    // attribute 'resource' gives resource name for lookups
    String resourceName = resourceElement.getAttributeValue("resource", null);
    if (resourceName != null) {
      result = getResource(resourceName);
    }

    if (result == null && !disableLoad) {
      result = buildInstance(resourceElement, extraArgs);

      // attribute 'name' (or 'resource') gives resource name for storage
      if (resourceName == null) {
        resourceName = resourceElement.getAttributeValue("name", null);
      }

      if (result != null && resourceName != null) {
        name2resource.put(resourceName, result);

        System.out.println(new Date() + ": ResourceManager built/stored '" + resourceName + "' resource.");
      }
    }

    return result;
  }

  public final Object getResourceByClass(String classPath) {
    return getResourceByClass(classPath, null);
  }

  public final Object getResourceByClass(String classPath, Object[] extraArgs) {
    Object result = null;

    try {
      final Class theClass = Class.forName(classPath);
      final Object[] args = getArgs(null, extraArgs);
      result = ReflectUtil.constructInstance(theClass, args);
    }
    catch (ClassNotFoundException e) {
      throw new IllegalArgumentException(e);
    }

    return result;
  }

  public final void addResource(String resourceName, Object resource) {
    name2resource.put(resourceName, resource);
  }

  /**
   * Get the named resource or null.
   */
  public final Object getResource(String resourceName) {
    Object result = null;

    if (resourceName != null) {
      result = name2resource.get(resourceName);

      if (result != null) System.out.println(new Date() + ": ResourceManager retrieved '" + resourceName + "' resource.");
    }

    return result;
  }

  /**
   * assumed: fileElement's text content identifies the path to the file with
   * optional variable substitution (of form "${varname}") and under optional
   * "workingDir" (property's) directory (when non-null) using this instance's
   * options (DataProperties).
   */
  public final File getWorkingFile(DomElement fileElement) {
    File result = null;

    if (fileElement != null) {
      final String filename = fileElement.getTextContent().trim();

      if (options != null) {
        result = options.getWorkingFile(filename, "workingDir");
      }
      else if (fileElement.getDataProperties() != null) {
        result = fileElement.getDataProperties().getWorkingFile(filename, "workingDir");
      }
      else {
        result = new File(filename);
      }
    }

    return result;
  }

  private final Object buildInstance(DomElement resourceElement, Object[] extraArgs) {
   Object result = null;

   final Object[] args = getArgs(resourceElement, extraArgs);

    try {
      final DomNode classnameNode = resourceElement.selectSingleNode("jclass");
      if (classnameNode == null) {
        throw new IllegalArgumentException("Required xpath 'jclass' not found " +
                                           "(relative to '" +
                                           resourceElement.getLocalName() + "' node)!");
      }
      final String classname = classnameNode.getTextContent().trim();
      final Class theClass = Class.forName(classname);
      result = ReflectUtil.constructInstance(theClass, args);
    }
    catch (ClassNotFoundException e) {
      throw new IllegalArgumentException(e);
    }

    return result;
  }

  private final Object[] getArgs(DomElement resourceElement, Object[] extraArgs) {
    Object[] args = null;
    if (extraArgs == null) {
      args = new Object[] { resourceElement, this };
    }
    else {
      args = new Object[extraArgs.length + 2];

      args[0] = resourceElement;
      args[1] = this;

      for (int argIdx = 0; argIdx < extraArgs.length; ++argIdx) {
        args[argIdx + 2] = extraArgs[argIdx];
      }
    }
    return args;
  }
}
