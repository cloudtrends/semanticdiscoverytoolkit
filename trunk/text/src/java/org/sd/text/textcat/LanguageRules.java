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
package org.sd.text.textcat;


import org.sd.io.FileUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * Wrapper class around a rules.txt file for specifying languages to include
 * and/or exclude.
 * <p>
 * @author Spence Koehler
 */
public class LanguageRules {

  private Set<String> include;
  private Set<String> exclude;

  public LanguageRules(File ruleFile) throws IOException {
    this.include = new HashSet<String>();
    this.exclude = new HashSet<String>();

    final BufferedReader reader = FileUtil.getReader(ruleFile);

    String line = null;
    while ((line = reader.readLine()) != null) {
      if (line.length() > 0) {
        final char firstChar = line.charAt(0);
        if (firstChar == '+') {
          include.add(line.substring(1));
        }
        else if (firstChar == '-') {
          exclude.add(line.substring(1));
        }
      }
    }

    reader.close();
  }

  public boolean include(String languageModule) {
    return include.contains(languageModule);
  }

  public boolean exclude(String languageModule) {
    return exclude.contains(languageModule);
  }
}
