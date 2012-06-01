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


import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.sd.atn.Parse;
import org.sd.util.tree.Tree;
import org.sd.xml.DataProperties;
import org.sd.xml.XmlLite;

/**
 * Base class containing an interpretation component (field or sub-record).
 * <p>
 * @author Spence Koehler
 */
public class InterpContainer {
  
  private InterpContainer parentContainer;
  private Map<String, List<InterpContainer>> fields;

  public final Tree<XmlLite.Data> interpNode;
  public final String cmd;
  public final String fieldName;
  public final DataProperties overrides;
  public final Parse parse;
  public final Tree<String> parseNode;
  public final Tree<XmlLite.Data> parentInterpNode;

  protected InterpContainer(Tree<XmlLite.Data> interpNode, String cmd, String fieldName,
                            DataProperties overrides, Parse parse, Tree<String> parseNode,
                            Tree<XmlLite.Data> parentInterpNode) {
    this.parentContainer = null;
    this.fields = null;

    this.interpNode = interpNode;
    this.cmd = cmd;
    this.fieldName = fieldName;
    this.overrides = overrides;
    this.parse = parse;
    this.parseNode = parseNode;
    this.parentInterpNode = parentInterpNode;
  }

  /**
   * Safely downcast this instance to a RecordInterpContainer if it is one.
   */
  public RecordInterpContainer asRecord() {
    return null;
  }

  /**
   * Safely downcast this instance to a FieldInterpContainer if it is one.
   */
  public FieldInterpContainer asField() {
    return null;
  }

  public boolean isRecord() {
    return asRecord() != null;
  }

  public boolean isField() {
    return asField() != null;
  }

  public boolean hasParentContainer() {
    return parentContainer != null;
  }

  public InterpContainer getParentContainer() {
    return parentContainer;
  }

  public void setParentContainer(InterpContainer parentContainer) {
    this.parentContainer = parentContainer;
  }

  
  public boolean hasFields() {
    return this.fields != null && this.fields.size() > 0;
  }

  public Map<String, List<InterpContainer>> getFields() {
    return this.fields;
  }

  public List<InterpContainer> getField(String name) {
    return this.fields == null ? null : this.fields.get(name);
  }

  public boolean hasField(String name) {
    return this.fields == null ? false : this.fields.containsKey(name);
  }

  public void addField(String name, InterpContainer childContainer) {
    addField(name, childContainer, true);
  }

  public void addField(String name, InterpContainer childContainer, boolean setChildParent) {
    if (this.fields == null) this.fields = new LinkedHashMap<String, List<InterpContainer>>();
    List<InterpContainer> values = fields.get(name);
    if (values == null) {
      values = new ArrayList<InterpContainer>();
      fields.put(name, values);
    }
    values.add(childContainer);
    if (setChildParent) {
      childContainer.setParentContainer(this);
    }
  }
}
