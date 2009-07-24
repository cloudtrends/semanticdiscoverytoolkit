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
package org.sd.text.radixtree;


/**
 * Data to hold in a radix tree node.
 * <p>
 * @author Spence Koehler
 */
public class RadixData<T> {
  
  private String key;
  private boolean real;
  private T value;

  public RadixData() {
    this("", false, null);
  }

  public RadixData(String key, boolean real, T value) {
    this.key = key;
    this.real = real;
    this.value = value;
  }

  public T getValue() {
    return value;
  }

  public void setValue(T data) {
    this.value = data;
  }

  public String getKey() {
    return key;
  }

  public void setKey(String key) {
    this.key = key;
  }

  public boolean isReal() {
    return real;
  }

  public void setReal(boolean datanode) {
    this.real = datanode;
  }

  public String toString() {
    final StringBuilder result = new StringBuilder();

    result.append(key);
    if (real) result.append('*');
    if (value != null) {
      result.append('{').append(value.toString()).append('}');
    }

    return result.toString();
  }
}
