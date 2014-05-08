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


import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

/**
 * Utility class to help format data into tables.
 * <p>
 * @author Spence Koehler
 */
public class Tableizer {

  public static final String LEFT_ALIGNED_BORDERLESS_TABLE =
  "<table style=\"text-align: left;\" border=\"0\" cellpadding=\"2\" cellspacing=\"2\">";

  public static final String CENTER_ALIGNED_BORDERLESS_TABLE =
  "<table style=\"text-align: center;\" border=\"0\" cellpadding=\"2\" cellspacing=\"2\">";

  public static final String LEFT_ALIGNED_TABLE =
  "<table style=\"text-align: left;\" border=\"1\" cellpadding=\"2\" cellspacing=\"2\">";

  public static final String CENTER_ALIGNED_TABLE =
  "<table style=\"text-align: center;\" border=\"1\" cellpadding=\"2\" cellspacing=\"2\">";

  private String defaultTable;

  public Tableizer() {
    this.defaultTable = LEFT_ALIGNED_BORDERLESS_TABLE;
  }

  public Tableizer(String defaultTableString) {
    this.defaultTable = defaultTableString;
  }

  public String setDefaultTable(String tableString) {
    String result = defaultTable;
    defaultTable = tableString;
    return result;
  }

  public String generateTable(boolean html, String[][] data) {
    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    PrintStream out = new PrintStream(bytes);
    generateTable(out, html, data);
    return bytes.toString();
  }

  public void generateTable(PrintStream out, boolean html, String[][] data) {
    if (html) {
      generateHtmlTable(out, data, defaultTable);
    }
    else {
      generateTextTable(out, data);
    }
  }

  public void startTable(PrintStream out, boolean html) {
    if (html) {
      out.println(defaultTable);
    }
    else {
      out.println();
    }
  }

  public void endTable(PrintStream out, boolean html) {
    if (html) {
      out.println("</table>");
    }
    else {
      out.println();
    }
  }

  public void generateRow(PrintStream out, boolean html, String[] data) {
    if (html) {
      out.println("<tr>");
      for (int i = 0; i < data.length; ++i) {
        out.print("<td>" + data[i] + "</td>");
      }
      out.println("</tr>");
    }
    else {
      for (int i = 0; i < data.length; ++i) {
        out.print(data[i]);
        if (i + 1 < data.length) {
          out.print(", ");
        }
      }
      out.println();
    }
  }

  public void generateHtmlTable(PrintStream out, String[][] data, String tableTag) {
    out.println(tableTag);

    for (int col = 0; col < data.length; ++col) {
      out.println("<tr>");
      for (int row = 0; row < data[col].length; ++row) {
        if (row % 2 == 0) {
          out.print("<td style=\"text-align: right;\">");
        }
        else {
          out.print("<td>");
        }
        if (data[col][row] != null) {
          String string = data[col][row].replaceAll("\n", "<br/>");
          string = string.replaceAll(" ", "&nbsp;");
          out.print(string);
        }
        out.println("</td>");
      }
      out.println("</tr>");
    }

    out.println("</table>");
  }

  public void generateTextTable(PrintStream out, String[][] data) {
    final int numCols = computeNumCols(data);
    final int[] colWidths = computeColWidths(numCols, data);
    final String[] formats = buildFormats(colWidths);
    for (int row = 0; row < data.length ; ++row) {
      final String[] rowData = data[row];
      for (int col = 0; col < rowData.length; ++col) {
        final String cellData = rowData[col];
        out.printf(formats[col], cellData == null ? "" : cellData);
      }
    }
  }

  private final int computeNumCols(String[][] data) {
    int result = 0;
    for (int row = 0; row < data.length ; ++row) {
      final String[] rowData = data[row];
      if (rowData.length > result) {
        result = rowData.length;
      }
    }
    return result;
  }

  private final int[] computeColWidths(int numCols, String[][] data) {
    final int[] result = new int[numCols];
    for (int i = 0; i < result.length; ++i) result[i] = 0;
    for (int row = 0; row < data.length ; ++row) {
      final String[] rowData = data[row];
      for (int col = 0; col < rowData.length; ++col) {
        final String cellData = rowData[col];
        if (cellData != null) {
          if (cellData.length() > result[col]) {
            result[col] = cellData.length();
          }
        }
      }
    }
    return result;
  }

  private final String[] buildFormats(int[] colWidths) {
    final String[] result = new String[colWidths.length];
    final StringBuilder builder = new StringBuilder();
    for (int i = 0; i < colWidths.length; ++i) {
      builder.setLength(0);
      if (i > 0) builder.append("  ");  // add spacer (todo: parameterize spacer size)
      buildFormat(builder, colWidths[i]);
      if (i == colWidths.length - 1) builder.append("\n");
      result[i] = builder.toString();
    }
    return result;
  }

  private final String buildFormat(StringBuilder builder, int colWidth) {
    builder.
      append('%').
      append(Math.max(1, colWidth)).
      append('s');
    return builder.toString();
  }

/*
  public void generateTextTable(PrintStream out, String[][] data) {
    for (int col = 0; col < data.length; ++col) {
      for (int row = 0; row < data[col].length; ++row) {
        if (data[col][row] != null) {
          out.print(data[col][row]);
          if (row % 2 == 0) {
            out.print(": ");
          }
          else {
            if (row + 1 < data[col].length) {
              out.print(", ");
            }
          }
        }
      }
      out.println();
    }
  }
*/
}
