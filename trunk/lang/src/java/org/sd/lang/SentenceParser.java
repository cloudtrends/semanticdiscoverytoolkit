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
package org.sd.lang;


import org.sd.nlp.ParserWrapper;
import org.sd.nlp.Parser;

import java.io.IOException;
import java.util.List;
import java.util.Properties;

/**
 * Base class for parsing sentences.
 * <p>
 * @author Spence Koehler
 */
public abstract class SentenceParser {

  private String language;
  private Properties properties;
  private ParserWrapper parserWrapper;

  protected SentenceParser(String language, Properties properties) {
    this.language = language;
    this.properties = properties;
    try {
      this.parserWrapper = ParserWrapper.buildParserWrapper(properties);
    }
    catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  public List<Parser.Parse> parse(String text) {
//todo: break text up into sentences
    return parserWrapper.parse(text);
  }

//todo: add abstract methods for getting subject, verb, direct object, indirect object
}
