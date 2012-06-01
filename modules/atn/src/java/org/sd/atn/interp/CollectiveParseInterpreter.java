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


import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import org.sd.atn.Parse;
import org.sd.atn.ResourceManager;
import org.sd.util.Usage;
import org.sd.util.tree.Tree;
import org.sd.xml.DataProperties;
import org.sd.xml.DomNode;
import org.sd.xml.XmlLite;

/**
 * ParseInterpreter implementation that collects interpretation information
 * in InterpContainer instances.
 * <p>
 * This implementation (May, 2012) builds upon TemplateParseInterpreter by
 * collecting the selected parse tree elements instead of spawning functional
 * treatment as found. Thus, analysis can be performed on collected elements
 * in full context as opposed to making decisions purely incrementally and in
 * isolation of other elements.
 *
 * @author Spence Koehler
 */
@Usage(notes =
       "An org.sd.org.atn.ParseInterpreter implementation\n" +
       "that allows for parse node targeting using org.sd.util.tree.NodePaths\n" +
       "(similar to XPaths, but for parse trees) and manipulating the contents\n" +
       "of selected nodes for generating an org.sd.atn.ParseInterpretation,\n" +
       "where generated interpretations are viewed as records (non-terminal\n" +
       "XML elements) containing fields (nested or terminal elements).\n\n" +
       "In this implementation, the full parse tree is transformed to records\n" +
       "with fields that are collected for optional further analysis."
  )
public class CollectiveParseInterpreter extends BaseTemplateParseInterpreter {

  public CollectiveParseInterpreter(DomNode domNode, ResourceManager resourceManager) {
    super(domNode, resourceManager);
  }

  protected InterpretationController buildInterpretationController() {
    return new CollectiveInterpretationController(super.trace, super.disabled);
  }



  protected class CollectiveInterpretationController extends BaseInterpretationController<InterpContainer> {

    protected List<InterpContainer> interpContainers;
    private RecordInterpContainer curRecordInterp;

    public CollectiveInterpretationController(boolean trace, boolean disabled) {
      super(trace, disabled);
      this.interpContainers = new ArrayList<InterpContainer>();
      this.curRecordInterp = null;
    }

    public List<InterpContainer> getFinalModels() {
      return interpContainers;
    }

    public Serializable getInterpObject() {
      InterpContainer[] result = null;

      // interpObject is result of getFinalModels (as a native array)

      final List<InterpContainer> models = getFinalModels();

      if (models != null) {
        result = models.toArray(new InterpContainer[models.size()]);
      }

      return result;
    }

    public String getInterpString() {

      // at this level, there is no interp string

      return null;
    }

    protected void handleRecordCommand(Tree<XmlLite.Data> recordNode, String cmd, String fieldName,
                                       boolean start, DataProperties overrides, RecordTemplate recordTemplate,
                                       Parse parse, Tree<String> parseNode, Tree<XmlLite.Data> parentNode) {
      if (start) {
        // final RecordInterpContainer recordInterp =
        //   new RecordInterpContainer(recordNode, cmd, fieldName, overrides, recordTemplate,
        //                             parse, parseNode, parentNode);
        final RecordInterpContainer recordInterp =
          new RecordInterpContainer(recordNode, cmd, fieldName, overrides, null,
                                    null, null, null);

        // if (recordTemplate.isTop()) {
        //   interpContainers.add(recordInterp);
        // }
        // else {
        //   curRecordInterp.addField(fieldName, recordInterp);
        // }

        curRecordInterp = recordInterp;
      }
      else {
        curRecordInterp = curRecordInterp.getParentContainer().asRecord();
      }
    }

    protected void handleFieldCommand(Tree<XmlLite.Data> fieldNode, String cmd,
                                      String fieldName, String fieldText,
                                      Tree<String> selectedNode, DataProperties overrides,
                                      FieldTemplate fieldTemplate, Parse parse,
                                      Tree<XmlLite.Data> parentNode) {
      // final FieldInterpContainer fieldInterp =
      //   new FieldInterpContainer(fieldNode, cmd, fieldName, fieldText, selectedNode, overrides, fieldTemplate,
      //                            parse, parentNode);
      final FieldInterpContainer fieldInterp =
        new FieldInterpContainer(fieldNode, cmd, fieldName, fieldText, selectedNode, overrides, null,
                                 null, null);

      curRecordInterp.addField(fieldName, fieldInterp);
    }
  }
}
