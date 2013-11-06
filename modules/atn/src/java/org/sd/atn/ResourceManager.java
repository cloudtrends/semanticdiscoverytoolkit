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
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
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
  private boolean disableLoad;        // disable EVERYTHING
  private boolean disableResources;   // disable just 'resources' node(s)

  /**
   * Map to store named instances for reference by later instances.
   */
  private Map<String, Object> name2resource;

  private Map<String, Normalizer> id2Normalizer;

  private LinkedHashSet<MetaData> metaData;

  /**
   * Default constructor for empty instance.
   */
  public ResourceManager() {
    init(null);
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
    this.disableLoad = options == null ? false : options.getBoolean("_disableLoad", false);
    this.disableResources = options == null ? false : options.getBoolean("_disableResources", false);

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

  public boolean hasMetaData() {
    return metaData != null && metaData.size() > 0;
  }

  public LinkedHashSet<MetaData> getMetaData() {
    return metaData;
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

      if (result != null) {
        if (resourceName != null) {
          name2resource.put(resourceName, result);
          if (GlobalConfig.verboseLoad()) {
            System.out.println(new Date() + ": ResourceManager built/stored '" + resourceName + "' resource.");
          }

          if (metaData == null) metaData = new LinkedHashSet<MetaData>();
          metaData.add(new XmlMetaData(resourceElement, extraArgs));
        }
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

      if (result != null) {
        if (metaData == null) metaData = new LinkedHashSet<MetaData>();
        metaData.add(new ClassMetaData(classPath, extraArgs));
      }
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

      if (result != null) {
        if (GlobalConfig.verboseLoad()) {
          System.out.println(new Date() + ": ResourceManager retrieved '" + resourceName + "' resource.");
        }
      }
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

      if (result != null) {
        if (metaData == null) metaData = new LinkedHashSet<MetaData>();
        metaData.add(new FileMetaData(filename, result));
      }
    }

    return result;
  }

  private final Object buildInstance(DomElement resourceElement, Object[] extraArgs) {
   Object result = null;

   final Object[] args = getArgs(resourceElement, extraArgs);
   String classname = null;

    try {
      final DomNode classnameNode = resourceElement.selectSingleNode("jclass");
      if (classnameNode == null) {
        if (disableLoad || disableResources) {
          if (GlobalConfig.verboseLoad()) {
            System.out.println("*** WARNING: ResourceManager(disabled) missing required xpath 'jclass' relative to '" +
                               resourceElement.getLocalName() + "' node!");
          }
          return null;
        }
        else {
          throw new IllegalArgumentException("Required xpath 'jclass' not found " +
                                             "(relative to '" +
                                             resourceElement.getLocalName() + "' node)!");
        }
      }
      classname = classnameNode.getTextContent().trim();
      final Class theClass = Class.forName(classname);

      if (GlobalConfig.verboseLoad()) {
        System.out.println(new Date() + ": ResourceManager constructing '" + classname + "' resource.");
      }

      result = ReflectUtil.constructInstance(theClass, args);
    }
    catch (ClassNotFoundException e) {
      if (disableLoad || disableResources) {
        if (GlobalConfig.verboseLoad()) {
          System.out.println("*** WARNING : ResourceManager(disabled) unable to load '" + classname + "'");
        }
      }
      else {
        throw new IllegalArgumentException(e);
      }
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


  public static class MetaData {
    protected Object[] extraArgs;
    protected Properties properties;

    protected MetaData() {
      this(null, null);
    }

    protected MetaData(Object[] extraArgs, Properties properties) {
      this.extraArgs = extraArgs;
      this.properties = properties;
    }

    public boolean hasExtraArgs() {
      return extraArgs != null && extraArgs.length > 0;
    }

    public Object[] getExtraArgs() {
      return extraArgs;
    }

    public void setExtraArgs(Object[] extraArgs) {
      this.extraArgs = extraArgs;
    }

    public boolean hasProperties() {
      return properties != null && properties.size() > 0;
    }

    public Properties getProperties() {
      return properties;
    }

    public void setProperties(Properties properties) {
      this.properties = properties;
    }

    public XmlMetaData asXmlMetaData() {
      return null;
    }

    public ClassMetaData asClassMetaData() {
      return null;
    }

    public FileMetaData asFileMetaData() {
      return null;
    }

    public boolean equals(Object other) {
      boolean result = (this == other);

      if (!result && other instanceof MetaData) {
        final MetaData otherMetaData = (MetaData)other;
        result = (extraArgs == otherMetaData.extraArgs);
        if (!result && extraArgs != null && otherMetaData.extraArgs != null &&
            extraArgs.length == otherMetaData.extraArgs.length) {
          result = true;
          for (int i = 0; i < extraArgs.length; ++i) {
            if (!extraArgs[i].equals(otherMetaData.extraArgs[i])) {
              result = false;
              break;
            }
          }
        }
        if (result) {
          result = (properties == otherMetaData.properties);
          if (!result && properties != null && otherMetaData.properties != null) {
            result = properties.equals(otherMetaData.properties);
          }
        }
      }

      return result;
    }

    public int hashCode() {
      int result = 11;

      if (extraArgs != null) {
        result = result * 11 + extraArgs.length;
        for (Object extraArg : extraArgs) {
          if (extraArg != null) {
            result = result * 11 + extraArg.hashCode();
          }
        }
      }
      if (properties != null) {
        result = result * 11 + properties.hashCode();
      }

      return result;
    }
  }

  public static class XmlMetaData extends MetaData {
    private DomElement resourceElement;
    private String _flatString; // for equals/hashCode

    public XmlMetaData(DomElement resourceElement) {
      this(resourceElement, null);
    }

    public XmlMetaData(DomElement resourceElement, Object[] extraArgs) {
      super(extraArgs, null);
      this.resourceElement = resourceElement;
    }

    public XmlMetaData(DataProperties dataProperties) {
      super(null, dataProperties.getProperties());
      this.resourceElement = dataProperties.getDomElement();
    }

    public DomElement getResourceElement() {
      return resourceElement;
    }

    public final XmlMetaData asXmlMetaData() {
      return this;
    }

    public boolean equals(Object other) {
      boolean result = (this == other);

      if (!result && other instanceof XmlMetaData && super.equals(other)) {
        final XmlMetaData otherMetaData = (XmlMetaData)other;
        result = (resourceElement == otherMetaData.resourceElement);
        if (!result && resourceElement != null && otherMetaData.resourceElement != null) {
          result = getFlatString().equals(otherMetaData.getFlatString());
        }
      }

      return result;
    }

    public int hashCode() {
      int result = super.hashCode();

      if (resourceElement != null) {
        result = result * 11 + getFlatString().hashCode();
      }

      return result;
    }

    private final String getFlatString() {
      if (_flatString == null) {
        _flatString = (resourceElement == null) ? "" : resourceElement.asFlatString(null).toString();
      }
      return _flatString;
    }
  }

  public static class ClassMetaData extends MetaData {
    private String classPath;

    public ClassMetaData(String classPath, Object[] extraArgs) {
      super(extraArgs, null);
      this.classPath = classPath;
    }

    public String getClassPath() {
      return classPath;
    }

    public final ClassMetaData asClassMetaData() {
      return this;
    }

    public boolean equals(Object other) {
      boolean result = (this == other);

      if (!result && other instanceof ClassMetaData && super.equals(other)) {
        final ClassMetaData otherMetaData = (ClassMetaData)other;
        result = (classPath == otherMetaData.classPath);
        if (!result && classPath != null && otherMetaData.classPath != null) {
          result = classPath.equals(otherMetaData.classPath);
        }
      }

      return result;
    }

    public int hashCode() {
      int result = super.hashCode();

      if (classPath != null) {
        result = result * 11 + classPath.hashCode();
      }

      return result;
    }
  }

  public static class FileMetaData extends MetaData {
    private String filename;
    private File file;

    public FileMetaData(String filename, File file) {
      super();
      this.filename = filename;
      this.file = file;
    }

    public String getFileName() {
      return filename;
    }

    public File getFile() {
      return file;
    }

    public final FileMetaData asFileMetaData() {
      return this;
    }

    public boolean equals(Object other) {
      boolean result = (this == other);

      if (!result && other instanceof FileMetaData && super.equals(other)) {
        final FileMetaData otherMetaData = (FileMetaData)other;
        result = (filename == otherMetaData.filename);
        if (!result && filename != null && otherMetaData.filename != null) {
          result = filename.equals(otherMetaData.filename);
        }
      }

      return result;
    }

    public int hashCode() {
      int result = super.hashCode();

      if (filename != null) {
        result = result * 11 + filename.hashCode();
      }

      return result;
    }
  }
}
