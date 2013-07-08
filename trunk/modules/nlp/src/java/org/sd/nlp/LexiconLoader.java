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
package org.sd.nlp;


import org.sd.io.FileUtil;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;

/**
 * Utility class to load a lexicon.
 * <p>
 * @author Spence Koehler
 */
public class LexiconLoader {
  
  /**
   * Load the lexicon from a csv stream with lines of the form:
   * <p>
   * term,CATEGORY1,CATEGORY2,...
   * <p>
   * Blank lines and those beginning with '#' are ignored.
   */
  public static Lexicon loadCsvLexicon(CategoryFactory categoryFactory, Class clazz, String resource, AbstractNormalizer normalizer) throws IOException {
    return loadCsvLexicon(categoryFactory, FileUtil.getInputStream(clazz, resource), normalizer);
  }

  /**
   * Load the lexicon from a csv stream with lines of the form:
   * <p>
   * term,CATEGORY1,CATEGORY2,...
   * <p>
   * Blank lines and those beginning with '#' are ignored.
   */
  public static Lexicon loadCsvLexicon(CategoryFactory categoryFactory, InputStream inputStream, AbstractNormalizer normalizer) throws IOException {
    final LexiconImpl result = new LexiconImpl(normalizer);
    final BufferedReader reader = FileUtil.getReader(inputStream);
    String line = null;
    while ((line = reader.readLine()) != null) {
      if ((line = line.trim()).length() > 0) {
        if (!line.startsWith("#")) {
          final String[] pieces = line.split(",");
          if (pieces.length > 1) {
            pieces[0] = pieces[0].trim();
            for (int i = 1; i < pieces.length; ++i) {
              result.addDefinition(pieces[0], categoryFactory.getCategory(pieces[i].trim().toUpperCase()));
            }
          }
        }
      }
    }
    return result;
  }
}
