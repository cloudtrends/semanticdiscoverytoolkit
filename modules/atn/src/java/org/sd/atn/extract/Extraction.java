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
package org.sd.atn.extract;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Container class for extracted text that can be recursively fielded.
 * <p>
 * @author Spence Koehler
 */
public class Extraction {
  
  private String type;
  /**
   * This extraction's type.
   */
  public String getType() {
    return type;
  }
  protected final void setType(String type) {
    this.type = type;
  }


  private String text;
  /**
   * The extracted text.
   */
  public String getText() {
    return text;
  }
  protected final void setText(String text) {
    this.text = text;
  }


  private Map<String, List<Extraction>> _fields;
  /**
   * This extraction's direct fields mapped by extraction type.
   */
  public Map<String, List<Extraction>> getFields() {
    if (_fields == null) {
      _fields = new HashMap<String, List<Extraction>>();
    }
    return _fields;
  }


  public boolean hasFields() {
    return _fields != null && _fields.size() > 0;
  }


  /**
   * Construct with the given type and null text.
   */
  protected Extraction(String type) {
    this.type = type;
    this.text = null;
  }

  /**
   * Construct with the given type and text.
   */
  /// <param name="text"></param>
  /// <param name="type"></param>
  public Extraction(String type, String text) {
    this.type = type;
    this.text = text;
  }


  public void addField(Extraction extraction) {
    addField(extraction.getType(), extraction);
  }

  public void addField(String type, Extraction extraction) {

    final Map<String, List<Extraction>> fields = getFields();
    List<Extraction> extractions = fields.get(type);
    
    if (extractions == null) {
      extractions = new ArrayList<Extraction>();
      fields.put(type, extractions);
    }

    extractions.add(extraction);
  }

  public Extraction getFirstField(String type) {
    Extraction result = null;

    final Map<String, List<Extraction>> fields = getFields();
    List<Extraction> extractions = fields.get(type);

    if (extractions != null && extractions.size() > 0)  {
      result = extractions.get(0);
    }

    return result;
  }

  /**
   * Get just this extraction's String without its fields.
   */
  protected String asString() {
    final StringBuilder result = new StringBuilder();

    final String text = (this.text == null) ? "" : this.text;
    final int numChars = Math.min(40, text.length());
    result.append(type).append(": ").append(text.substring(0, numChars));
    if (numChars < text.length()) result.append("...");

    return result.toString();
  }

  /**
   * Get a String form of this extraction.
   */
  public String toString() {
    StringBuilder result = new StringBuilder();

    result.append(asString());

    if (hasFields()) {
      for (Map.Entry<String, List<Extraction>> fieldEntry : getFields().entrySet()) {
        final List<Extraction> extractions = fieldEntry.getValue();
        for (Extraction extraction : extractions) {
          final String[] substrings = extraction.toString().split("\n");
          for (String substring : substrings) {
            result.append("\n  ").append(substring);
          }
        }
      }
    }

    return result.toString();
  }
}
