/*
    Copyright 2011 Semantic Discovery, Inc. (www.semanticdiscovery.com)

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
package org.sd.atn.interp;


import java.util.HashMap;
import java.util.Map;
import org.sd.atn.ResourceManager;

/**
 * Container for resources within the TemplateParseInterpreter framework.
 * <p>
 * @author Spence Koehler
 */
public class InnerResources {
  
  public final ResourceManager resourceManager;
  public final Map<String, RecordTemplate> id2recordTemplate;

  InnerResources(ResourceManager resourceManager) {
    this.resourceManager = resourceManager;
    this.id2recordTemplate = new HashMap<String, RecordTemplate>();
  }

  /**
   * Add the record template, returning any template being displaced by the add.
   */
  public RecordTemplate add(RecordTemplate recordTemplate) {
    final String id = recordTemplate.getId();
    final RecordTemplate result = id2recordTemplate.get(id);
    id2recordTemplate.put(id, recordTemplate);
    return result;
  }

  public RecordTemplate get(String id) {
    return id2recordTemplate.get(id);
  }
}
