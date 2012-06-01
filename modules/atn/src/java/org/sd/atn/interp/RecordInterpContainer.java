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
 * A record interp container contains fields and/or other records.
 * <p>
 * @author Spence Koehler
 */
public class RecordInterpContainer extends InterpContainer {
  
  public final RecordTemplate recordTemplate;

  public RecordInterpContainer(Tree<XmlLite.Data> recordNode, String cmd, String fieldName,
                               DataProperties overrides, RecordTemplate recordTemplate,
                               Parse parse, Tree<String> parseNode, Tree<XmlLite.Data> parentNode) {
    super(recordNode, cmd, fieldName, overrides, parse, parseNode, parentNode);

    this.recordTemplate = recordTemplate;
  }

  /**
   * Safely downcast this instance to a RecordInterpContainer.
   */
  public RecordInterpContainer asRecord() {
    return this;
  }
}
