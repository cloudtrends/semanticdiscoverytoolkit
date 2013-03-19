/*
    Copyright 2009 Semantic Discovery, Inc. (www.semanticdiscovery.com)

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


import java.util.concurrent.atomic.AtomicBoolean;
import org.sd.util.InputContext;
import org.sd.util.InputContextIterator;

/**
 * Container for settings for parsing.
 * <p>
 * @author Spence Koehler
 */
public abstract class ParseSettings {
  
  private String compoundParserId;
  private String[] atnParserFlow;


  protected ParseSettings(String compoundParserId, String[] atnParserFlow) {
    this.compoundParserId = compoundParserId;
    this.atnParserFlow = atnParserFlow;
  }


  protected String getCompoundParserId() {
    return compoundParserId;
  }

  protected String[] getAtnParserFlow() {
    return atnParserFlow;
  }

  public ParseOutputCollector parse(ParseConfig parseConfig, InputContextIterator inputContextIterator,
                                    InputOptions overrides, AtomicBoolean die) {
    return parse(parseConfig, inputContextIterator, null, overrides, die);
  }

  public ParseOutputCollector parse(ParseConfig parseConfig, InputContextIterator inputContextIterator,
                                    ParseOutputCollector output, InputOptions overrides,
                                    AtomicBoolean die) {

    final InputContextIterator reconfiguredInput =  reconfigureInput(inputContextIterator, output);
    if (reconfiguredInput != null) {
      output = parseConfig.parse(reconfiguredInput, this.compoundParserId, this.atnParserFlow, output, overrides, die);
    }

    return output;
  }

  /**
   * Reconfigure the input for parsing according to this settings instance.
   *
   * @return the reconfigured input or null to curtail parsing.
   */
  protected abstract InputContextIterator reconfigureInput(InputContextIterator inputContextIterator, ParseOutputCollector output);

}
