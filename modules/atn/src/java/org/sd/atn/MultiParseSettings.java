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


import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.sd.util.InputContextIterator;
import org.sd.xml.DomElement;

/**
 * Container for settings for multi-pass parsing.
 * <p>
 * @author Spence Koehler
 */
public class MultiParseSettings {

  public final List<ParseSettings> parseSettings;

  public MultiParseSettings() {
    this.parseSettings = new ArrayList<ParseSettings>();
  }

  public void add(ParseSettings parseSettings) {
    this.parseSettings.add(parseSettings);
  }
  
  protected List<ParseSettings> getParseSettings() {
    return parseSettings;
  }

  public ParseOutputCollector parse(ParseConfig parseConfig, InputContextIterator inputContextIterator,
                                    InputOptions overrides, AtomicBoolean die) {
    return parse(parseConfig, inputContextIterator, null, overrides, die);
  }

  public ParseOutputCollector parse(ParseConfig parseConfig, InputContextIterator inputContextIterator,
                                    ParseOutputCollector output, InputOptions overrides,
                                    AtomicBoolean die) {
    for (ParseSettings parseSettings : this.parseSettings) {
      output = parseSettings.parse(parseConfig, inputContextIterator, output, overrides, die);
    }
    return output == null ? new ParseOutputCollector((DomElement)null) : output;
  }
}
