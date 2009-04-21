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


/**
 * Extraction data for holding a string.
 * <p>
 * @author Spence Koehler
 */
public final class ExtractionStringData extends AbstractExtractionData {
  
  private String string;
  private boolean builtByLineBuilder;

  public ExtractionStringData(String string) {
    this(string, false);
  }

  public ExtractionStringData(String string, boolean builtByLineBuilder) {
    this.string = string;
    this.builtByLineBuilder = builtByLineBuilder;
  }

  public ExtractionStringData asStringData() {
    return this;
  }

  public boolean wasBuiltByLineBuilder() {
    return builtByLineBuilder;
  }

  public String getString() {
    return string;
  }

  public String toString() {
    return string;
  }

  public String getExtractedString() {
    return string;
  }
}
