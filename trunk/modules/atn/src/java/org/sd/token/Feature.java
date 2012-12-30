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
package org.sd.token;


import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.Serializable;
import org.sd.io.DataHelper;
import org.sd.io.Publishable;

/**
 * Container for a token feature.
 * <p>
 * @author Spence Koehler
 */
public class Feature implements Publishable {
  
  private String type;
  /**
   * This feature's type.
   * 
   * Note that this needs to be coordinated across feature extractors (or
   * sources) and token consumers. A consumer can verify the applicability
   * of a feature by also taking into account the source's class type.
   */
  public String getType() {
    return type;
  }
  public void setType(String type) {
    this.type = type;
  }

  private Serializable value;
  /**
   * This feature's value.
   */
  public Serializable getValue() {
    return value;
  }
  public void setValue(Serializable value) {
    this.value = value;
  }

  private double p;
  /**
   * A probability associated with this feature.
   */
  public double getP() {
    return p;
  }
  public void setP(double p) {
    this.p = p;
  }

  private Class<?> sourceType;
  /**
   * The source object that created this feature.
   */
  public Class<?> getSourceType() {
    return sourceType;
  }
  public void setSourceType(Object source) {
    this.sourceType = source == null ? null : source.getClass();
  }
  public void setSourceType(Class<?> sourceType) {
    this.sourceType = sourceType;
  }


  /**
   * Empty constructor for publishable reconstruction.
   */
  public Feature() {
  }

  public Feature(String type, Serializable value, double p, Object source) {
    this.type = type;
    this.value = value;
    this.p = p;
    this.sourceType = source == null ? null : source.getClass();
  }

  public Feature(String type, Serializable value, double p, Class<?> sourceType) {
    this.type = type;
    this.value = value;
    this.p = p;
    this.sourceType = sourceType;
  }


  /**
   * Write this message to the dataOutput stream such that this message
   * can be completely reconstructed through this.read(dataInput).
   *
   * @param dataOutput  the data output to write to.
   */
  public void write(DataOutput dataOutput) throws IOException {
    DataHelper.writeString(dataOutput, type);
    DataHelper.writeSerializable(dataOutput, value);
    dataOutput.writeDouble(p);
    DataHelper.writeString(dataOutput, sourceType.getName());
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
    this.type = DataHelper.readString(dataInput);
    this.value = DataHelper.readSerializable(dataInput);
    this.p = dataInput.readDouble();
    this.sourceType = null;
    final String sourceString = DataHelper.readString(dataInput);

    if (sourceString != null) {
      try {
        this.sourceType = Class.forName(sourceString);
      }
      catch (ClassNotFoundException e) {
        throw new IllegalStateException(e);
      }
    }
  }

  public String toString() {
    final StringBuilder result = new StringBuilder();

    result.
      append("Feature(").
      append(type).
      append(',').
      append(value).
      append(")");

    return result.toString();
  }
}
