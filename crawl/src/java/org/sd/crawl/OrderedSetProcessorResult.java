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
package org.sd.crawl;


import org.sd.cio.MessageHelper;
import org.sd.text.DetailedUrl;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * A processor result backed by an ordered set.
 * <p>
 * @author Spence Koehler
 */
public class OrderedSetProcessorResult extends ProcessorResult {

  private Set<String> elements;

  /**
   * Default constructor for publishable reconstruction only.
   */
  public OrderedSetProcessorResult() {
  }

  /**
   * Construct with the given name and elements.
   */
  public OrderedSetProcessorResult(String name, Collection<String> elements) {
    super(name);
    this.elements = new LinkedHashSet<String>(elements);
  }

  /**
   * Get this result's information with html markup.
   */
  public String toHtml(int maxRows) {
    final StringBuilder result = new StringBuilder();

    result.
      append("<table border=\"1\"><tr><th>").append(getSimpleName()).
      append("</th></tr>\n<tr><td><table>");


    if (maxRows > 0) {
      // fit the data into enough columns to meet the max rows
      final int numCols = (int)Math.ceil(((double)elements.size()) / ((double)maxRows));
      final String[][] cells = new String[numCols][];
      cells[0] = new String[maxRows];

      int colNum = 0;
      int rowNum = 0;

      for (String element : elements) {
        cells[colNum][rowNum++] = getElementAsHtml(element);

        if (rowNum >= maxRows) {
          ++colNum;
          rowNum = 0;
          if (colNum < cells.length) cells[colNum] = new String[maxRows];
          else break;
        }
      }

      for (int rowNumI = 0; rowNumI < maxRows; ++rowNumI) {
        result.append("<tr>");
        for (int colNumI = 0; colNumI < numCols; ++colNumI) {
          result.append("<td \"nowrap\">");
          if (cells[colNumI][rowNumI] != null) {
            result.append(cells[colNumI][rowNumI]);
          }
          result.append("</td>");
        }
        result.append("</tr>");
      }
    }
    else {
      // just one column
      for (String element : elements) {
        result.append("<tr><td>").append(getElementAsHtml(element)).append("</td></tr>\n");
      }
    }

    result.append("</table></tr></table>\n");

    return result.toString();
  }

  protected String getElementAsHtml(String element) {
    final StringBuilder result = new StringBuilder();

    // if element appears to be a url, wrap an 'a' tag around the element
    final DetailedUrl dUrl = new DetailedUrl(element);
    if (dUrl.getProtocol(false) != null) {
      result.
        append("<a href=\"").append(dUrl.getNormalizedUrl()).append("\">").
        append(element).append("</a>");
    }
    else {
      result.append(element);
    }

    return result.toString();
  }

  /**
   * Get this result's value as a string.
   * <p>
   * This concatenates the "ngram (freq)" toString forms with comma delimiters.
   */
  protected String createValueAsString() {
    final StringBuilder result = new StringBuilder();

    for (String element : elements) {
      if (result.length() > 0) result.append(',');
      result.append(element);
    }

    return result.toString();
  }

  /**
   * Do the work of combining other instance's value into this instance.
   */
  protected void doCombineWithOthers(ProcessorResult[] others) {
    for (ProcessorResult other : others) {
      if (other instanceof OrderedSetProcessorResult) {
        OrderedSetProcessorResult o = (OrderedSetProcessorResult)other;
        elements.addAll(o.elements);
      }
    }
  }

  /**
   * Do the work of combining this instance's value with the given string
   * forms of its value.
   */
  protected void doCombineWithOthers(String[] values) {
    for (String value : values) {
      final String[] pieces = value.split(",");
      for (String piece : pieces) elements.add(piece);
    }
  }
  
  /**
   * Write this message to the dataOutput stream such that this message
   * can be completely reconstructed through this.read(dataInput).
   *
   * @param dataOutput  the data output to write to.
   */
  public void write(DataOutput dataOutput) throws IOException {
    super.write(dataOutput);

    if (elements == null) {
      dataOutput.writeInt(-1);
    }
    else {
      dataOutput.writeInt(elements.size());
      for (String element : elements) {
        MessageHelper.writeString(dataOutput, element);
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
    super.read(dataInput);

    final int numElements = dataInput.readInt();
    if (numElements >= 0) {
      this.elements = new LinkedHashSet<String>();
      for (int i = 0; i < numElements; ++i) {
        this.elements.add(MessageHelper.readString(dataInput));
      }
    }
  }
}
