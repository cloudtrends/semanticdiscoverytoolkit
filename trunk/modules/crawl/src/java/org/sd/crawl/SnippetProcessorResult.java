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
import org.sd.crawl.SearchEngineSettings.HitSnippet;
import org.sd.xml.EntityConverter;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 
 * <p>
 * @author Spence Koehler
 */
public class SnippetProcessorResult extends ProcessorResult {

  private List<HitSnippet> snippets;

  /**
   * Default constructor for publishable reconstruction only.
   */
  public SnippetProcessorResult() {
  }

  /**
   * Construct with the given hit snippets.
   */
  public SnippetProcessorResult(String name, List<HitSnippet> snippets) {
    super(name);
    this.snippets = snippets;
  }

  /**
   * Get this result's information with html markup.
   */
  public String toHtml(int maxRows) {
    final StringBuilder result = new StringBuilder();

    result.
      append("<table border=\"1\"><tr><th>").append(getSimpleName()).
      append("</th></tr>\n<tr><td><table>");

    int rowNum = 0;
    for (HitSnippet snippet : snippets) {
      if (maxRows > 0 && rowNum >= maxRows) break;

      result.
        append("<tr><td>\n").
        append("<a href=\"").
        append(snippet.getLinkUrl()).
        append("\">").
        append(snippet.getLinkText()).      //todo: html escape?
        append("</a>\n<br>\n").
        append(snippet.getContent(true)).   //todo: html escape?
        append("\n<br>\n").
        append("</td></tr>");

      ++rowNum;
    }

    result.append("</table></tr></table>\n");

    return result.toString();
  }

  /**
   * Get this result's value as a string.
   */
  protected String createValueAsString() {
    final StringBuilder result = new StringBuilder();

    for (HitSnippet snippet : snippets) {
      if (result.length() > 0) result.append('\n');
      result.append(snippet.toString());
    }

    return result.toString();
  }

  /**
   * Do the work of combining other instance's value into this instance.
   */
  protected void doCombineWithOthers(ProcessorResult[] others) {
    for (ProcessorResult other : others) {
      if (other instanceof SnippetProcessorResult) {
        SnippetProcessorResult o = (SnippetProcessorResult)other;
        if (o.snippets != null) {
          snippets.addAll(o.snippets);
        }
      }
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

    if (snippets == null) {
      dataOutput.writeInt(-1);
    }
    else {
      dataOutput.writeInt(snippets.size());
      for (HitSnippet snippet : snippets) {
        MessageHelper.writePublishable(dataOutput, snippet);
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

    final int numSnippets = dataInput.readInt();
    if (numSnippets >= 0) {
      this.snippets = new ArrayList<HitSnippet>();
      for (int i = 0; i < numSnippets; ++i) {
        snippets.add((HitSnippet)MessageHelper.readPublishable(dataInput));
      }
    }
  }
}
