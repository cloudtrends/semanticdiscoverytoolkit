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


import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.sd.cio.MessageHelper;
import org.sd.io.Publishable;

/**
 * Container class for extracted text that can be recursively fielded.
 * <p>
 * @author Spence Koehler
 */
public class Extraction implements Publishable {
  
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
  public boolean hasText() {
    return text != null && !"".equals(text);
  }


  private Map<String, List<Extraction>> _fields;
  /**
   * This extraction's direct fields mapped by extraction type.
   */
  public Map<String, List<Extraction>> getFields() {
    if (_fields == null) {
      _fields = new LinkedHashMap<String, List<Extraction>>();
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

  /**
   * Write this message to the dataOutput stream such that this message
   * can be completely reconstructed through this.read(dataInput).
   *
   * @param dataOutput  the data output to write to.
   */
  public void write(DataOutput dataOutput) throws IOException {
    MessageHelper.writeString(dataOutput, type);
    MessageHelper.writeString(dataOutput, text);

    dataOutput.writeInt(_fields == null ? 0 : _fields.size());

    if (_fields != null) {
      for (Map.Entry<String, List<Extraction>> entry : _fields.entrySet()) {
        MessageHelper.writeString(dataOutput, entry.getKey());

        dataOutput.writeInt(entry.getValue().size());
        for (Extraction e : entry.getValue()) {
          MessageHelper.writePublishable(dataOutput, e);
        }
      }
    }
  }

  /**
   * Read this message's contents from the dataInput stream that was written by
   * this.write(dataOutput).
   * <p>
   * NOTE: this requires all implementing classes to have a default constructor
   *       with no args.
   *
   * @param dataInput  the data output to write to.
   */
  public void read(DataInput dataInput) throws IOException {
    this.type = MessageHelper.readString(dataInput);
    this.text = MessageHelper.readString(dataInput);

    final int numFields = dataInput.readInt();

    if (numFields > 0) {
      this._fields = new LinkedHashMap<String, List<Extraction>>();

      for (int fieldNum = 0; fieldNum < numFields; ++fieldNum) {
        final String fieldKey = MessageHelper.readString(dataInput);
        final int numExtractions = dataInput.readInt();

        final List<Extraction> extractions = new ArrayList<Extraction>();
        _fields.put(fieldKey, extractions);

        for (int extractionNum = 0; extractionNum < numExtractions; ++extractionNum) {
          extractions.add((Extraction)MessageHelper.readPublishable(dataInput));
        }
      }
    }
  }
}
