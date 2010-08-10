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
package org.sd.atn;


import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import org.sd.token.StandardTokenizerOptions;
import org.sd.xml.DataProperties;
import org.sd.xml.DomDocument;
import org.sd.xml.DomElement;
import org.sd.xml.DomUtil;
import org.sd.xml.XmlFactory;

/**
 * A container for an ATN Parser with its options and strategies.
 * <p>
 * @author Spence Koehler
 */
public class AtnParserWrapper {
  
  private String id;
  public String getId() {
    return id;
  }

  private AtnParser parser;
  public AtnParser getParser() {
    return parser;
  }

  private DomElement tokenizerOverride;
  public DomElement getTokenizerOverride() {
    return tokenizerOverride;
  }

  private StandardTokenizerOptions tokenizerOptions;
  public StandardTokenizerOptions getTokenizerOptions() {
    return tokenizerOptions;
  }

  private AtnParseOptions parseOptions;
  public AtnParseOptions getParseOptions() {
    return parseOptions;
  }

  private AtnParseSelector parseSelector;
  public AtnParseSelector getParseSelector() {
    return parseSelector;
  }

  private int minNumTokens;
  public int getMinNumTokens() {
    return minNumTokens;
  }


  AtnParserWrapper(DomElement parserElement) {
    final DomElement idElement = (DomElement)parserElement.selectSingleNode("id");
    if (idElement != null) {
      this.id = idElement.getTextContent();
    }
    else {
      this.id = "<UNSPECIFIED>";
    }

    final DomElement grammarFileElement = (DomElement)parserElement.selectSingleNode("grammar");
    DomElement grammarElement = grammarFileElement;

    if (grammarElement == null) {
      throw new IllegalArgumentException("Parser element must have 'grammar' child!");
    }

    if (DomUtil.getFirstChild(grammarFileElement) == null) {
      final DataProperties dataProperties = grammarElement.getDataProperties();
      final String grammarFilename = grammarFileElement.getTextContent();
      File grammarFile = dataProperties == null ? new File(grammarFilename) : dataProperties.getWorkingFile(grammarFilename, "workingDir");

      try {
        final DomDocument domDocument = XmlFactory.loadDocument(grammarFile, false, dataProperties);
        grammarElement = (DomElement)domDocument.getDocumentElement();
      }
      catch (IOException e) {
        throw new IllegalStateException(e);
      }
    }

    this.parser = new AtnParser(grammarElement);

    final DomElement parseSelectorElement = (DomElement)parserElement.selectSingleNode("parseSelector");
    this.parseSelector = (parseSelectorElement != null) ? (AtnParseSelector)parseSelectorElement.buildInstance("jclass") : null;

    this.tokenizerOverride = (DomElement)parserElement.selectSingleNode("tokenizer");

    final DomElement tokenizerOptionsElement = (DomElement)parserElement.selectSingleNode("tokenizerOptions");
    this.tokenizerOptions = tokenizerOptionsElement == null ? new StandardTokenizerOptions() : new StandardTokenizerOptions(tokenizerOptionsElement);

    final DomElement parseOptionsElement = (DomElement)parserElement.selectSingleNode("parseOptions");
    this.parseOptions = parseOptionsElement == null ? new AtnParseOptions() : new AtnParseOptions(parseOptionsElement);

    this.minNumTokens = this.parseOptions.getAdjustInputForTokens() ? this.parser.getGrammar().computeMinNumTokens(this.parseOptions) : 1;
  }

  public List<AtnParseResult> seekAll(AtnParseBasedTokenizer tokenizer, Set<Integer> stopList) {
    tokenizer.setTokenizerOptions(tokenizerOptions);

    final List<AtnParseResult> parseResults = parser.seekAll(tokenizer, parseOptions, stopList);

    if (parseSelector != null && parseResults != null && parseResults.size() > 0) {
      for (AtnParseResult parseResult : parseResults) {
        parseSelector.selectParses(parseResult);
      }
    }

    // Add the parse results as tokens
    tokenizer.add(parseResults);

    return parseResults;
  }
}
