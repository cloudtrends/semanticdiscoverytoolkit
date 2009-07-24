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
package org.sd.lang.english;


import org.sd.io.FileUtil;
import org.sd.lang.SentenceParser;
import org.sd.nlp.Parser;

import java.util.List;
import java.util.Properties;

/**
 * Utility class for parsing English sentences.
 * <p>
 * @author Spence Koehler
 */
public class EnglishSentenceParser extends SentenceParser {
  
  public static final String LANGUAGE = "English";
  private static final String DEFAULT_PROPERTIES = "resources/EnglishSentence.default.properties";  // parser wrapper properties


  /**
   * Construct with default english sentence parsing properties.
   */
  public EnglishSentenceParser() {
    super(LANGUAGE, FileUtil.getProperties(EnglishSentenceParser.class, DEFAULT_PROPERTIES));
  }

  /**
   * Construct with the given properties.
   */
  public EnglishSentenceParser(Properties properties) {
    super(LANGUAGE, properties);
  }


  public static final void main(String[] args) {
    final EnglishSentenceParser parser = new EnglishSentenceParser();

    for (String sentence : args) {
      System.out.println(sentence);

      final List<Parser.Parse> parses = parser.parse(sentence);

      if (parses == null) {
        System.out.println("--> NO PARSES.");
      }
      else {
        System.out.println("--> " + parses.size() + " PARSES:");

        int index = 0;
        for (Parser.Parse parse : parses) {
          System.out.println("    " + (++index) + ": " + parse);
        }
      }
    }
  }
}
