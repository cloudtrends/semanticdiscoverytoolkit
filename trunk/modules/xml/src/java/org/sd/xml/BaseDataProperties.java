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
package org.sd.xml;


import org.sd.util.ReflectUtil;

/**
 * 
 * <p>
 * @author Spence Koehler
 */
public abstract class BaseDataProperties {
  

  /**
   * Get the value associated with the key or null.
   */
  protected abstract String getValueString(String key);


  /**
   * Build an instance of the class that takes this DataProperties object as its
   * sole construction parameter.
   */
  public Object buildInstance(String classname) {
    Object result = null;

    try {
      final Class theClass = Class.forName(classname);
      result = ReflectUtil.constructInstance(theClass, this);
    }
    catch (ClassNotFoundException e) {
      throw new IllegalArgumentException(e);
    }

    return result;
  }

  /**
   * Determine whether the key has a non-null associated value.
   */
  public boolean hasProperty(String key) {
    return getValueString(key) != null;
  }

  public boolean getBoolean(String key) {
    boolean result = false;

    String value = getValueString(key);

    if (value == null) {
      throw new IllegalStateException("Required property '" + key + "' not present!");
    }
    else {
      value = value.toLowerCase().trim();
      result = "true".equals(value);
    }

    return result;
  }

  public boolean getBoolean(String key, boolean defaultValue) {
    boolean result = defaultValue;

    String value = getValueString(key);

    if (value != null) {
      value = value.toLowerCase().trim();
      result = "true".equals(value);
    }

    return result;
  }

  public int getInt(String key) {
    int result = 0;

    String value = getValueString(key);

    if (value == null) {
      throw new IllegalStateException("Required property '" + key + "' not present!");
    }
    else {
      value = value.toLowerCase().trim();
      result = Integer.parseInt(value);
    }

    return result;
  }

  public int getInt(String key, int defaultValue) {
    int result = defaultValue;

    String value = getValueString(key);

    if (value != null) {
      value = value.toLowerCase().trim();
      result = Integer.parseInt(value);
    }

    return result;
  }

  public String getString(String key) {
    String result = null;

    final String value = getValueString(key);

    if (value == null) {
      throw new IllegalStateException("Required property '" + key + "' not present!");
    }
    else {
      result = value;
    }

    return result;
  }

  public String getString(String key, String defaultValue) {
    String result = defaultValue;

    final String value = getValueString(key);

    if (value != null) {
      result = value;
    }

    return result;
  }
}
