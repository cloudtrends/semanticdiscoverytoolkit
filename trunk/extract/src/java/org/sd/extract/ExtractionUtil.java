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


import java.util.List;

/**
 * Extraction utilities.
 * <p>
 * @author Spence Koehler
 */
public class ExtractionUtil {
  
  /**
   * Fix a path key by removing the special indexes for "table" and "td"
   * components.
   */
  public static final String fixPathKey(String pathKey) {
    String result = pathKey;

    // remove digits after table and td
    if (pathKey.indexOf(".td") >= 0 || pathKey.indexOf(".table") >= 0) {
      StringBuilder builder = new StringBuilder();

      final String[] pieces = pathKey.split("\\.");
      for (int i = 0; i < pieces.length; ++i) {
        if (i > 0) builder.append('.');

        String piece = pieces[i];
        if (piece.startsWith("td") && piece.length() > 2 && Character.isDigit(piece.charAt(2))) {
          builder.append("td");
        }
        else if (piece.startsWith("table") && piece.length() > 5 && Character.isDigit(piece.charAt(5))) {
          builder.append("table");
        }
        else {
          builder.append(piece);
        }
      }

      result = builder.toString();
    }

    return result;
  }

  public static final String getTableString(int tablePos, String pathKey) {
    String result = null;

    if (tablePos >= 0) {
      final int dotPos = pathKey.indexOf('.', tablePos + 1);
      if (dotPos < 0) {
        result = pathKey.substring(tablePos);
      }
      else {
        result = pathKey.substring(tablePos, dotPos + 1);
      }
    }

    return result;
  }

  public static final int getVariableTablePos(List<Extraction> extractions) {
    int result = -1;

    if (extractions.size() >= 2) {
      final Extraction e1 = extractions.get(0);
      final Extraction e2 = extractions.get(1);

      final String pathKey1 = e1.getPathKey();
      result = pathKey1.indexOf(".table");
      if (result >= 0) {
        final String pathKey2 = e2.getPathKey();

        while (result >= 0) {
          final String table1 = ExtractionUtil.getTableString(result, pathKey1);
          final String table2 = ExtractionUtil.getTableString(result, pathKey2);

          if (!table1.equals(table2)) break;
          result = pathKey1.indexOf(".table", result + 1);
        }
      }
    }

    return result;
  }
}
