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


import java.util.HashMap;
import java.util.Map;

/**
 * Map of (namespaced) keys to values of type T.
 * <p>
 * @author Spence Koehler
 */
public class DomNamespaceMap<T> {
  
  private String defaultPrefix;
  private Map<String, T> defaultMap;

  private Map<String, Map<String, T>> prefixMap;

  public DomNamespaceMap() {
    this(null);
  }

  public DomNamespaceMap(String defaultPrefix) {
    this.defaultPrefix = defaultPrefix;
    this.defaultMap = null;
    this.prefixMap = null;
  }

  public void setDefaultPrefix(String defaultPrefix) {
    this.defaultPrefix = defaultPrefix;
  }

  public boolean containsKey(String key) {
    boolean result = false;

    final int cPos = key.indexOf(':');
    if (cPos >= 0) {
      final String prefix = key.substring(0, cPos);
      final String name = key.substring(cPos + 1);
      result = containsKey(prefix, name);
    }
    else if (defaultPrefix != null) {
      result = containsKey(defaultPrefix, key);
    }
    else {
      if (defaultMap != null) {
        result = defaultMap.containsKey(key);
      }
    }

    return result;
  }

  public void put(String key, T value) {
    final int cPos = key.indexOf(':');

    if (cPos >= 0) {
      final String prefix = key.substring(0, cPos);
      final String name = key.substring(cPos + 1);
      put(prefix, name, value);
    }
    else if (defaultPrefix != null) {
      put(defaultPrefix, key, value);
    }
    else {
      defaultPut(key, value);
    }
  }

  public T get(String key) {
    T value = null;

    final int cPos = key.indexOf(':');
    if (cPos >= 0) {
      final String prefix = key.substring(0, cPos);
      final String name = key.substring(cPos + 1);
      value = get(prefix, name);
    }
    else if (defaultPrefix != null) {
      value = get(defaultPrefix, key);
    }
    else {
      value = defaultGet(key);
    }

    return value;
  }

  public T remove(String key) {
    T value = null;

    final int cPos = key.indexOf(':');
    if (cPos >= 0) {
      final String prefix = key.substring(0, cPos);
      final String name = key.substring(cPos + 1);
      value = remove(prefix, name);
    }
    else if (defaultPrefix != null) {
      value = remove(defaultPrefix, key);
    }
    else {
      if (defaultMap != null) {
        value = defaultMap.remove(key);
      }
    }

    return value;
  }

  public boolean containsKey(String namespacePrefix, String key) {
    boolean result = false;

    if (namespacePrefix == null) namespacePrefix = defaultPrefix;

    if (namespacePrefix == null) {
      if (defaultMap != null) {
        result = defaultMap.containsKey(key);
      }
    }
    else {
      if (prefixMap != null) {
        final Map<String, T> map = prefixMap.get(namespacePrefix);
        if (map != null) {
          result = map.containsKey(key);
        }
      }
    }

    return result;
  }

  public void put(String namespacePrefix, String key, T value) {
    if (namespacePrefix == null) namespacePrefix = defaultPrefix;

    if (namespacePrefix == null) {
      defaultPut(key, value);
    }
    else {
      if (prefixMap == null) prefixMap = new HashMap<String, Map<String, T>>();

      Map<String, T> map = prefixMap.get(namespacePrefix);
      if (map == null) {
        map = new HashMap<String, T>();
        prefixMap.put(namespacePrefix, map);
      }
      map.put(key, value);
    }
  }

  public T get(String namespacePrefix, String key) {
    T value = null;

    if (namespacePrefix == null) namespacePrefix = defaultPrefix;

    if (namespacePrefix == null) {
      value = defaultGet(key);
    }
    else {
      if (prefixMap != null) {
        final Map<String, T> map = prefixMap.get(namespacePrefix);
        if (map != null) {
          value = map.get(key);
        }
      }
    }

    return value;
  }

  public T remove(String namespacePrefix, String key) {
    T value = null;

    if (namespacePrefix == null) namespacePrefix = defaultPrefix;

    if (namespacePrefix == null) {
      if (defaultMap != null) {
        value = defaultMap.remove(key);
      }
    }
    else {
      if (prefixMap != null) {
        final Map<String, T> map = prefixMap.get(namespacePrefix);
        if (map != null) {
          value = map.remove(key);
        }
      }
    }

    return value;
  }

  private void defaultPut(String key, T value) {
    if (defaultMap == null) defaultMap = new HashMap<String, T>();
    defaultMap.put(key, value);
  }

  private T defaultGet(String key) {
    T result = null;

    if (defaultMap != null) {
      result = defaultMap.get(key);
    }

    return result;
  }
}
