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


import org.sd.util.tree.Tree;

import java.io.File;
import java.io.IOException;
import java.util.Set;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;

/**
 * Utility to rip content from html meta tags.
 * <p>
 * @author Spence Koehler
 */
public class MetaTagRipper {
  
  private Set<String> names;
  private boolean ripTitle;

  /**
   * Construct with the meta names whose content this is to rip.
   * <p>
   * The content text from tags of form <meta name="name" content="...">
   * encountered before the "body" tag will be extracted.
   */
  public MetaTagRipper(String[] names, boolean ripTitle) {
    this.names = new HashSet<String>();
    for (String name : names) this.names.add(name.toLowerCase());
    this.ripTitle = ripTitle;
  }

  /**
   * Add content to the given meta name to content map if found in the file.
   *
   * @return true if content was found and added; otherwise, false.
   */
  public final boolean ripContent(File file, Map<String, Set<String>> results, boolean keepFirstOnly) throws IOException {
    boolean result = false;
    final XmlTagRipper ripper = new XmlTagRipper(file, true, null);

    while (ripper.hasNext()) {
      final XmlLite.Tag tag = ripper.next();

      if ("meta".equals(tag.name)) {
        // extract content for targeted names
        final String name = tag.getAttribute("name");
        if (name != null && names.contains(name.toLowerCase())) {
          final String content = tag.getAttribute("content");
          if (content != null && content.length() > 0) {
            result = addContent(results, name, content, keepFirstOnly);
          }
        }
      }

      else if (ripTitle && "title".equals(tag.name)) {
        // extract title text
        final String titleText = ripper.ripText();
        if (titleText != null && titleText.length() > 0) {
          result = addContent(results, "title", titleText, keepFirstOnly);
        }
      }

      else if ("body".equals(tag.name)) {
        // we've gone beyond 'meta' tags. time to stop looking.
        break;
      }
    }

    // make sure everything is closed.
    ripper.close();

    return result;
  }

  private final synchronized boolean addContent(Map<String, Set<String>> results, String name, String content, boolean keepFirstOnly) {
    boolean result = true;

    Set<String> contents = results.get(name);
    if (contents == null) {
      contents = new LinkedHashSet<String>();
      results.put(name, contents);
    }
    else if (keepFirstOnly) {
      // already found this content; don't collect more.
      result = false;;
    }

    if (result) {
      contents.add(content);
    }

    return result;
  }

  public static void main(String[] args) throws IOException {
    final MetaTagRipper ripper = new MetaTagRipper(new String[]{"description", "keywords"}, true);
    final Map<String, Set<String>> metaContent = new HashMap<String, Set<String>>();

    if (ripper.ripContent(new File(args[0]), metaContent, false)) {
      System.out.println("Meta-data in '" + args[0] + "'=");
      for (Map.Entry<String, Set<String>> entry : metaContent.entrySet()) {
        System.out.println("\t" + entry.getKey() + ": " + entry.getValue());
      }
    }
    else {
      System.out.println("Didn't find any meta-data in '" + args[0] + "'!");
    }
  }
}
