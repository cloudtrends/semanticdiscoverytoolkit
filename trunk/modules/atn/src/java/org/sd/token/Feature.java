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


/**
 * Container for a token feature.
 * <p>
 * @author Spence Koehler
 */
public class Feature {
  
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

  private Object value;
  /**
   * This feature's value.
   */
  public Object getValue() {
    return value;
  }

  private double p;
  /**
   * A probability associated with this feature.
   */
  public double getP() {
    return p;
  }

  private Object source;
  /**
   * The source object that created this feature.
   */
  public Object getSource() {
    return source;
  }


  public Feature(String type, Object value, double p, Object source) {
    this.type = type;
    this.value = value;
    this.p = p;
    this.source = source;
  }
}
