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


import org.sd.atn.ResourceManager;
import org.sd.util.Usage;
import org.sd.xml.DomNode;

/**
 * Base extension of the TemplateParseInterpreter to build a model of type M
 * from a parse.
 * <p>
 * @author Spence Koehler
 */
@Usage(notes =
       "Base extension of the org.sd.atn.interp.TemplateParseInterpreter to\n" +
       "build a model of type M from a parse.\n" +
       "\n" +
       "NOTE: extenders must implement:\n" +
       "  protected org.sd.atn.interp.InterpretationController buildInterpretationController()\n" +
       "  usually by constructing an extended org.sd.atn.interp.BaseInterpretationController<M>."
  )
public abstract class BaseTemplateParseInterpreter extends TemplateParseInterpreter {

  //NOTE: extenders must implement: protected InterpretationController buildInterpretationController(),
  //      usually by constructing an extended BaseInterpretationController.


  protected boolean trace;
  protected boolean disabled;

  protected BaseTemplateParseInterpreter(DomNode domNode, ResourceManager resourceManager) {
    super(domNode, resourceManager);

    this.trace = domNode.getAttributeBoolean("trace", false);
    if (!this.trace && resourceManager.getOptions().getBoolean("trace", false)) {
      this.trace = true;
    }

    this.disabled = domNode.getAttributeBoolean("disable", false);
    if (!this.disabled && resourceManager.getOptions().getBoolean("FullDateParseInterpreter.disable", false)) {
      this.disabled = true;
    }
  }
}
