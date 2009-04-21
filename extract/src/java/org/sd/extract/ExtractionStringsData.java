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


import java.util.List;

/**
 * Extraction data for holding a list of strings.
 * <p>
 * @author Spence Koehler
 */
public final class ExtractionStringsData extends AbstractExtractionData {

  private List<String> strings;
  private boolean builtByLineBuilder;
  private String string;

  public ExtractionStringsData(List<String> strings) {
    this(strings, false);
  }

  public ExtractionStringsData(List<String> strings, boolean builtByLineBuilder) {
    this.strings = strings;
    this.builtByLineBuilder = builtByLineBuilder;
    this.string = null;
  }

  public ExtractionStringsData asStringsData() {
    return this;
  }

  public boolean wasBuiltByLineBuilder() {
    return builtByLineBuilder;
  }

  public List<String> getStrings() {
    return strings;
  }

  public String toString() {
    return strings.toString();
  }

  /**
   * Get the strings concatenated together delimited by a '\n'.
   */
  public String getExtractedString() {
    if (string == null) {
      final StringBuilder result = new StringBuilder();

      for (String string : strings) {
        if (result.length() > 0) result.append('\n');
        result.append(string);
      }

      string = result.toString();
    }
    return string;
  }
}
