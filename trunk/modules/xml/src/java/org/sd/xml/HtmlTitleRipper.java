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


import org.sd.io.FileUtil;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Utility for grabbing titles from html.
 * <p>
 * @author Spence Koehler
 */
public class HtmlTitleRipper {

  public static final String ripTitle(String filename) throws IOException {
    return ripTitle(FileUtil.getFile(filename));
  }

  public static final String ripTitle(File file) throws IOException {
    String result = null;
    final XmlTextRipper ripper = new XmlTextRipper(FileUtil.getInputStream(file), true, new HtmlTagStack(), XmlFactory.HTML_TAG_PARSER_IGNORE_COMMENTS, HtmlHelper.DEFAULT_IGNORE_TAGS, null, false);

    while (ripper.hasNext()) {
      final String text = ripper.next();
      final List<XmlLite.Tag> tags = ripper.getTags();
      if (XmlTreeHelper.hasTag(tags, "title")) {
        result = text;
        break;
      }
      if (XmlTreeHelper.hasTag(tags, "body")) {
        // we've gone too far to hope to find the title.
        break;
      }
    }

    // make sure everything is closed.
    ripper.close();

    return result;
  }

  // rip the title from each arg file.
  public static final void main(String[] args) throws IOException {
    for (String arg : args) {
      System.out.println(arg + " title='" + ripTitle(arg) + "'");
    }
  }
}
