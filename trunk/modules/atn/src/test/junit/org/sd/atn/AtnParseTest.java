/*
    Copyright 2010 Semantic Discovery, Inc. (www.semanticdiscovery.com)

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
package org.sd.atn;


import java.io.IOException;
import org.sd.token.StandardTokenizer;
import org.sd.token.StandardTokenizerFactory;
import org.sd.token.StandardTokenizerOptions;
import org.sd.token.Tokenizer;
import org.sd.xml.DomDocument;
import org.sd.xml.DomElement;
import org.sd.xml.XmlFactory;

/**
 * Utilities for testing AtnParse classes
 * <p>
 * @author Spence Koehler
 */
public class AtnParseTest {
  
  public static final DomElement stringToXml(String xmlString, boolean htmlFlag) throws IOException {
    final DomDocument domDocument = XmlFactory.loadDocument(xmlString, htmlFlag);
    return (DomElement)domDocument.getDocumentElement();
  }

  public static final AtnParser buildParser(String grammarXml, boolean htmlFlag) throws IOException {
    final DomElement domElement = stringToXml(grammarXml, htmlFlag);
    final AtnGrammar grammar = new AtnGrammar(domElement, new ResourceManager());
    return new AtnParser(grammar);
  }

  public static final StandardTokenizer buildTokenizer(String tokenizerOptionsXml, String inputString) throws IOException {
    final StandardTokenizerOptions tokenizerOptions =
      tokenizerOptionsXml == null ?
      new StandardTokenizerOptions() :
      new StandardTokenizerOptions(stringToXml(tokenizerOptionsXml, false));
    return StandardTokenizerFactory.getTokenizer(inputString, tokenizerOptions);
  }

  public static final AtnParseOptions buildParseOptions(String parseOptionsXml) throws IOException {
    final ResourceManager resourceManager = new ResourceManager();

    final AtnParseOptions parseOptions =
      parseOptionsXml == null ?
      new AtnParseOptions(resourceManager) :
      new AtnParseOptions(AtnParseTest.stringToXml(parseOptionsXml, false), resourceManager);

    if (parseOptionsXml == null) {
      parseOptions.setConsumeAllText(true);
    }

    return parseOptions;
  }

  public static final AtnParseResult parse(AtnParser parser, Tokenizer tokenizer, AtnParseOptions options, boolean seek) {
    return seek ? parser.seekParse(tokenizer, options, null, null, null) : parser.parse(tokenizer, options, null, null, null);
  }
}
