/*
    Copyright 2010 Semantic Discovery, Inc. (www.semanticdiscovery.com)

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
package org.sd.util;


import java.io.Serializable;
import java.util.Set;
import java.util.TreeSet;

/**
 * Container for an integer that may be ambiguous.
 * <p>
 * @author Spence Koehler
 */
public class AmbiguousInteger implements Serializable {

  private TreeSet<Integer> values;
  private transient int[] _values;

  private static final long serialVersionUID = 42L;

  public AmbiguousInteger(Integer value) {
    this.values = new TreeSet<Integer>();
    this.values.add(value);
  }

  /**
   * Construct with semicolon-separated values.
   */
  public AmbiguousInteger(String values) {
    this.values = new TreeSet<Integer>();
    final String[] pieces = values.split("\\s*;\\s*");
    for (String piece : pieces) {
      this.values.add(new Integer(piece));
    }
  }

  public void setValue(Integer value) {
    this.values.add(value);
    this._values = null;
  }

  public boolean isAmbiguous() {
    return values.size() > 1;
  }
  
  public int[] getValues() {
    if (_values == null) {
      this._values = new int[values.size()];
      int i = 0;
      for (Integer value : values) {
        _values[i++] = value;
      }
    }
    return _values;
  }

  public Set<Integer> getValueSet() {
    return values;
  }

  public int getLowValue() {
    final int[] values = getValues();
    return values[0];
  }

  public int getHighValue() {
    final int[] values = getValues();
    return values[values.length - 1];
  }

  public boolean isStrictlyLessThan(AmbiguousInteger other) {
    return getHighValue() < other.getLowValue();
  }

  public boolean isStrictlyGreaterThan(AmbiguousInteger other) {
    return getLowValue() > other.getHighValue();
  }

  public boolean isStrictlyLessThan(Integer other) {
    return getHighValue() < other;
  }

  public boolean isStrictlyGreaterThan(Integer other) {
    return getLowValue() > other;
  }

  public boolean isLooselyLessThan(AmbiguousInteger other) {
    return getLowValue() < other.getHighValue();
  }

  public boolean isLooselyGreaterThan(AmbiguousInteger other) {
    return getHighValue() > other.getLowValue();
  }

  public boolean isLooselyLessThan(Integer other) {
    return getLowValue() < other;
  }

  public boolean isLooselyGreaterThan(Integer other) {
    return getHighValue() > other;
  }

  public boolean equalsStrictly(AmbiguousInteger other) {
    boolean result = false;

    final int[] myValues = getValues();
    final int[] otherValues = other.getValues();

    if (myValues.length == otherValues.length) {
      result = true;
      for (int i = 0; i < myValues.length; ++i) {
        if (myValues[i] != otherValues[i]) {
          result = false;
          break;
        }
      }
    }

    return result;    
  }

  public boolean equalsLoosely(AmbiguousInteger other) {
    boolean result = false;

    final int[] myValues = getValues();
    final int[] otherValues = other.getValues();

    int i = 0;
    int j = 0;
    int myValue = myValues[i];
    int otherValue = otherValues[j];

    while (true) {
      if (myValue == otherValue) {
        result = true;
        break;
      }
      else if (myValue < otherValue) {
        ++i;
        if (i < myValues.length) {
          myValue = myValues[i];
        }
        else {
          break;
        }
      }
      else {  // otherValue < myValue
        ++j;
        if (j < otherValues.length) {
          otherValue = otherValues[i];
        }
        else {
          break;
        }
      }
    }

    return result;
  }

  public boolean equalsLoosely(Integer other) {
    boolean result = false;
    for (Integer value : values) {
      if (value == other) {
        result = true;
        break;
      }
    }
    return result;
  }

  public String toString() {
    final StringBuilder result = new StringBuilder();

    for (Integer value : values) {
      if (result.length() > 0) result.append(';');
      result.append(value);
    }

    return result.toString();
  }
}
