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
import org.sd.atn.ParseInterpretation;
import org.sd.util.tree.Tree;
import org.sd.xml.DataProperties;
import org.sd.xml.XmlLite;

/**
 * Interface for controlling an interpretation.
 * <p>
 * @author Spence Koehler
 */
public interface InterpretationController {
  
  /**
   * Hook on each record interpNode just after creation and before insertion
   * as a child into its tree.
   * <p>
   * NOTE: This hook can be called twice for each recordTemplate -- once before
   *       its fields are processed (start==true) and again after the fields
   *       have been *successfully* processed (start=false).  If the hook is
   *       called twice in a row with start=true, then field processing of the
   *       prior invocation yielded no true results.
   */
  public Tree<XmlLite.Data> interpRecordNodeHook(
    Tree<XmlLite.Data> recordNode, Parse parse,
    Tree<String> parseNode, Tree<XmlLite.Data> parentNode,
    String fieldName, RecordTemplate recordTemplate,
    boolean start, DataProperties overrides);

  /**
   * Hook on each field interpNode just after creation and before insertion
   * as a child into its tree.
   * <p>
   * Note that each non-root record node will come back through as a field
   * (after all fields have been visited) but not all fields come through as a
   * record.
   */
  public Tree<XmlLite.Data> interpFieldNodeHook(
    Tree<XmlLite.Data> fieldNode, Parse parse,
    Tree<String> selectedNode, Tree<XmlLite.Data> parentNode,
    FieldTemplate fieldTemplate, DataProperties overrides);

  /**
   * @return true to execute recordTemplate.interpret(parse); otherwise, false.
   */
  public boolean foundMatchingTemplateHook(RecordTemplate recordTemplate, Parse parse, DataProperties overrides);

  /**
   * Hook on a final interpretation.
   */
  public ParseInterpretation interpretationHook(ParseInterpretation interp, Parse parse, DataProperties overrides);

}
