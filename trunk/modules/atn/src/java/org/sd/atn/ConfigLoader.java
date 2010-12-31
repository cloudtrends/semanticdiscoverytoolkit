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
import java.io.IOException;
import org.sd.xml.DataProperties;
import org.sd.xml.DomDocument;
import org.sd.xml.DomElement;
import org.sd.xml.XmlFactory;

/**
 * Utility for loading configuration files while maintaining a single
 * ResourceManager.
 * <p>
 * @author Spence Koehler
 */
public class ConfigLoader {
  
  private DataProperties options;
  private ResourceManager _resources;

  public ConfigLoader() {
    this((DataProperties)null);
  }

  public ConfigLoader(DataProperties options) {
    this.options = options;
    this._resources = null;
  }

  public ConfigLoader(ResourceManager resourceManager) {
    this.options = resourceManager.getOptions();
    this._resources = resourceManager;
  }

  public ResourceManager getResourceManager() {
    if (_resources == null) {
      _resources = new ResourceManager(options);
    }
    return _resources;
  }

  public Object getResource(String configFilename, String resourceNodeName) throws IOException {
    Object result = null;

    final ResourceManager resources = getResourceManager();

    // load xml
    final File configFile = resources.getOptions().getWorkingFile(configFilename, "workingDir");
    final DomDocument domDocument = XmlFactory.loadDocument(configFile, false, options);
    final DomElement configElement = (DomElement)domDocument.getDocumentElement();
    final DomElement resourcesElement = (DomElement)configElement.selectSingleNode("resources");

    // get "resources"
    resources.loadResources(resourcesElement);

    // get specified resource
    if (resourceNodeName != null) {
      final DomElement resourceElement = (DomElement)configElement.selectSingleNode(resourceNodeName);
      if (resourceElement != null) {
        result = _resources.getResource(resourceElement);
      }
    }

    return result;
  }
}
