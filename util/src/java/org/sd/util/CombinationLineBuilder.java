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
package org.sd.util;


import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Utility to create multiple (record) lines from combinations of field values.
 * <p>
 * @author Spence Koehler
 */
public class CombinationLineBuilder {

  private List<Collection<String>> fields;
  private String[] _lines;

  public CombinationLineBuilder() {
    this.fields = new ArrayList<Collection<String>>();
    this._lines = null;
  }

  public void addUrlField(String value) {
    addUrlField(new String[]{value});
  }

  public void addUrlField(String[] values) {
    this._lines = null;
    final List<String> list = new ArrayList<String>();
    if (values != null) {
      for (String value : values) {
        list.add(LineBuilder.fixString(value, "%7c"));
      }
    }
    fields.add(list);
  }

  public void addHtmlField(String value) {
    addHtmlField(new String[]{value});
  }

  public void addHtmlField(String[] values) {
    this._lines = null;
    final List<String> list = new ArrayList<String>();
    if (values != null) {
      for (String value : values) {
        list.add(LineBuilder.fixString(value, "&#124;"));
      }
    }
    fields.add(list);
  }

  public void addBuiltField(String value) {
    addBuiltField(new String[]{value});
  }

  public void addBuiltField(String[] values) {
    this._lines = null;
    final List<String> list = new ArrayList<String>();
    if (values != null) {
      for (String value : values) {
        list.add(value);
      }
    }
    fields.add(list);
  }

  public void addField(String value) {
    addField(new String[]{value});
  }

  public void addField(String[] values) {
    this._lines = null;
    final List<String> list = new ArrayList<String>();
    if (values != null) {
      for (String value : values) {
        list.add(LineBuilder.fixString(value, ","));
      }
    }
    fields.add(list);
  }

  public void addField(int value) {
    addField(new int[]{value});
  }

  public void addField(int[] values) {
    this._lines = null;
    final List<String> list = new ArrayList<String>();
    if (values != null) {
      for (int value : values) {
        list.add(Integer.toString(value));
      }
    }
    fields.add(list);
  }

  public void addField(double value, int places) {
    addField(new double[]{value}, places);
  }

  public void addField(double[] values, int places) {
    this._lines = null;
    final List<String> list = new ArrayList<String>();
    if (values != null) {
      for (double value : values) {
        list.add(MathUtil.doubleString(value, places));
      }
    }
    fields.add(list);
  }

  public String[] getStrings() {
    if (_lines == null) {
      final List<Collection<String>> combos = GeneralUtil.combine(fields);
      final LineBuilder lineBuilder = new LineBuilder();
      _lines = new String[combos.size()];

      int index = 0;
      for (Collection<String> strings : combos) {
        for (String string : strings) {
          lineBuilder.appendBuilt(string);
        }
        _lines[index++] = lineBuilder.toString();
        lineBuilder.reset();
      }
    }
    return _lines;
  }
}
