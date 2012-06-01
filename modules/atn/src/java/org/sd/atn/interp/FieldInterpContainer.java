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


import org.sd.atn.Parse;
import org.sd.util.tree.Tree;
import org.sd.xml.DataProperties;
import org.sd.xml.XmlLite;

/**
 * A field interp container contains field data.
 * <p>
 * @author Spence Koehler
 */
public class FieldInterpContainer extends InterpContainer {

  public final String fieldText;
  public final FieldTemplate fieldTemplate;

  public FieldInterpContainer(Tree<XmlLite.Data> fieldNode, String cmd,
                              String fieldName, String fieldText,
                              Tree<String> selectedNode, DataProperties overrides,
                              FieldTemplate fieldTemplate, Parse parse,
                              Tree<XmlLite.Data> parentNode) {
    super(fieldNode, cmd, fieldName, overrides, parse, selectedNode, parentNode);

    this.fieldText = fieldText;
    this.fieldTemplate = fieldTemplate;
  }
}
