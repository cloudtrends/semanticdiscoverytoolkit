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
package org.sd.extract;


import org.sd.nlp.Parser;

import java.util.List;

/**
 * Extraction data for holding parses.
 * <p>
 * @author Spence Koehler
 */
public final class ExtractionParseData extends AbstractExtractionData {
  
  private List<Parser.Parse> parses;

  public ExtractionParseData(List<Parser.Parse> parses) {
    this.parses = parses;
  }

  public ExtractionParseData asParseData() {
    return this;
  }

  public List<Parser.Parse> getParses() {
    return parses;
  }

  public String toString() {
    return parses.toString();
  }

  public String getExtractedString() {
    String result = null;

    if (parses != null && parses.size() > 0) {
      final Parser.Parse parse = parses.get(0);
      result = parse.getInput();
    }

    return result;
  }
}
