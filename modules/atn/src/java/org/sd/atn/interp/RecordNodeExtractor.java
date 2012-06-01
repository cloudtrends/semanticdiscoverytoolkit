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


import java.util.List;
import org.sd.atn.Parse;
import org.sd.util.tree.Tree;
import org.sd.xml.DataProperties;
import org.sd.xml.XmlLite;

/**
 * NodeExtractor for Records.
 * <p>
 * @author Spence Koehler
 */
public class RecordNodeExtractor extends AbstractNodeExtractor {
  
  private String recordId;

  RecordNodeExtractor(FieldTemplate fieldTemplate, InnerResources resources, String recordId) {
    super(fieldTemplate, resources);
    this.recordId = recordId;
  }

  public List<Tree<XmlLite.Data>> extract(Parse parse, Tree<String> parseNode, DataProperties overrides, InterpretationController controller) {
    List<Tree<XmlLite.Data>> result = null;

    final RecordTemplate recordTemplate = resources.id2recordTemplate.get(recordId);
    if (recordTemplate != null) {
      final Tree<XmlLite.Data> interpNode = recordTemplate.interpret(parse, parseNode, null, fieldTemplate.getName(), overrides, controller);
      result = super.cleanup(interpNode, parse, parseNode, false);
    }

    return result;
  }

  public String extractString(Parse parse, Tree<String> parseNode) {
    return null;
  }
}
