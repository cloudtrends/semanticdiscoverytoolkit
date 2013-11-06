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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import org.sd.token.StandardTokenizerOptions;
import org.sd.util.Usage;
import org.sd.xml.DataProperties;
import org.sd.xml.DomElement;
import org.sd.xml.XmlFactory;

/**
 * A container for an ATN Parser with its options and strategies.
 * <p>
 * @author Spence Koehler
 */
@Usage(notes = "A container for an org.sd.atn.AtnParser with its options and strategies.")
public class AtnParserWrapper {
  
  public static AtnParserWrapper buildInstance(DomElement parserElement, ResourceManager resourceManager) {
    DomElement parserSupplement = null;
    final String parserFileName = parserElement.getAttributeValue("file", null);
    if (parserFileName != null) {
      final File parserFile = resourceManager.getOptions().getWorkingFile(parserFileName, "workingDir");
      if (GlobalConfig.verboseLoad()) {
        System.out.println(new Date() + ": AtnParserWrapper loading parserFile '" + parserFile.getAbsolutePath() + "'.");
      }
      if (parserFile.exists()) {
        try {
          final DomElement parserFileElement
            = (DomElement)XmlFactory.loadDocument(parserFile, false, resourceManager.getOptions()).getDocumentElement();
          parserSupplement = parserElement;  // parserElement becomes the supplement
          parserElement = parserFileElement; // to the loaded config file
        }
        catch (IOException e) {
          throw new IllegalStateException(e);
        }
      }
      else {
        throw new IllegalStateException(new Date() +
                                        ": ERROR can't find parser file in CompoundParser: '" +
                                        parserFile.getAbsolutePath() + "'");
      }
    }

    final AtnParserWrapper parserWrapper = new AtnParserWrapper(parserElement, resourceManager);
    if (parserSupplement != null) parserWrapper.supplement(parserSupplement, resourceManager);
    parserWrapper.validateInstance();

    return parserWrapper;
  }


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
  public void setTokenizerOptions(StandardTokenizerOptions tokenizerOptions) {
    this.tokenizerOptions = tokenizerOptions;
  }

  private AtnParseOptions parseOptions;
  public AtnParseOptions getParseOptions() {
    return parseOptions;
  }
  public void setParseOptions(AtnParseOptions parseOptions) {
    this.parseOptions = parseOptions;
  }

  private AtnParsePrequalifier prequalifier;
  public AtnParsePrequalifier getPrequalifier() {
    return prequalifier;
  }
  public void setPrequalifier(AtnParsePrequalifier prequalifier) {
    this.prequalifier = prequalifier;
  }

  private AtnParseSelector parseSelector;
  public AtnParseSelector getParseSelector() {
    return parseSelector;
  }
  public void setParseSelector(AtnParseSelector parseSelector) {
    this.parseSelector = parseSelector;
  }

  private AmbiguityResolver ambiguityResolver;
  public AmbiguityResolver getAmbiguityResolver() {
    return ambiguityResolver;
  }

  private int minNumTokens;
  public int getMinNumTokens() {
    return minNumTokens;
  }

  private int maxWordCount;
  public int getMaxWordCount() {
    return maxWordCount;
  }

  private DomElement parserElement;
  public DomElement getParserElement() {
    return parserElement;
  }

  private DomElement prequalifierElement;
  public DomElement getPrequalifierElement() {
    return prequalifierElement;
  }

  private DomElement parseSelectorElement;
  public DomElement getParseSelectorElement() {
    return parseSelectorElement;
  }

  private DomElement ambiguityResolverElement;
  public DomElement getAmbiguityResolverElement() {
    return ambiguityResolverElement;
  }


  AtnParserWrapper(DomElement parserElement, ResourceManager resourceManager) {
    this.id = "<UNSPECIFIED>";
    this.parser = null;
    this.prequalifierElement = null;
    this.prequalifier = null;
    this.parseSelectorElement = null;
    this.parseSelector = null;
    this.ambiguityResolverElement = null;
    this.ambiguityResolver = null;
    this.tokenizerOverride = null;
    this.tokenizerOptions = new StandardTokenizerOptions();
    this.parseOptions = new AtnParseOptions(resourceManager);
    this.maxWordCount = 0;

    doSupplement(parserElement, resourceManager);
  }

  private final void doSupplement(DomElement parserElement, ResourceManager resourceManager) {
    this.parserElement = parserElement;

    // Set ID
    final DomElement idElement = (DomElement)parserElement.selectSingleNode("id");
    if (idElement != null) {
      this.id = idElement.getTextContent();
    }

    // Load grammar (parser)
    final DomElement grammarFileElement = (DomElement)parserElement.selectSingleNode("grammar");
    final DomElement grammarElement = AtnGrammar.getGrammarElement(grammarFileElement);
    if (grammarElement != null) {
      final boolean override = grammarElement.getAttributeBoolean("override", false);
      if (parser != null && !override) {
        parser.getGrammar().supplement(grammarElement);
      }
      else {
        this.parser = new AtnParser(grammarElement, resourceManager);
      }
    }

    // Load prequalifier
    final DomElement pqElt = (DomElement)parserElement.selectSingleNode("prequalifier");
    if (pqElt != null) {
      this.prequalifierElement = pqElt;
      this.prequalifier = (AtnParsePrequalifier)resourceManager.getResource(prequalifierElement);
    }

    // Load parse selector
    final DomElement psElt = (DomElement)parserElement.selectSingleNode("parseSelector");
    if (psElt != null) {
      this.parseSelectorElement = psElt;
      this.parseSelector = (AtnParseSelector)resourceManager.getResource(parseSelectorElement);
    }

    // Load ambiguity resolver
    final DomElement arElt = (DomElement)parserElement.selectSingleNode("ambiguityResolver");
    if (arElt != null) {
      this.ambiguityResolverElement = arElt;
      this.ambiguityResolver = (AmbiguityResolver)resourceManager.getResource(ambiguityResolverElement);
    }

    // Load tokenizer override
    final DomElement tovElt = (DomElement)parserElement.selectSingleNode("tokenizer");
    if (tovElt != null) {
      this.tokenizerOverride = tovElt;
    }

    // Load tokenizer options
    final DomElement topElt = (DomElement)parserElement.selectSingleNode("tokenizerOptions");
    if (topElt != null) {
      this.tokenizerOptions = new StandardTokenizerOptions(topElt);
    }

    // Load parse options
    final DomElement poElt = (DomElement)parserElement.selectSingleNode("parseOptions");
    if (poElt != null) {
      this.parseOptions = new AtnParseOptions(poElt, resourceManager);
    }

    // Update min/max counts
    if (parser != null) {
      this.maxWordCount = this.parser.getGrammar().computeMaxWordCount();
      if (parseOptions != null) {
        this.minNumTokens = this.parseOptions.getAdjustInputForTokens() ? this.parser.getGrammar().computeMinNumTokens(this.parseOptions) : 1;
      }
    }

    // Update token break limit
    if (tokenizerOptions != null) {
      // reconcile maxWordCount with tokenBreakLimit, which is considered to set a minimum for this when non-zero
      //todo: currently using maxWordCount as an estimate for tokenBreakLimit. Fix if/when this becomes a problem.
      final int tokenBreakLimit = tokenizerOptions.getTokenBreakLimit();
      if ((tokenBreakLimit == 0 && maxWordCount > 0) || (this.maxWordCount > tokenBreakLimit)) {
        tokenizerOptions.setTokenBreakLimit(maxWordCount);
      }
      if (GlobalConfig.verboseLoad()) {
        System.out.println(new Date() + ": AtnParserWrapper (" + id + ") tokenBreakLimit=" + tokenizerOptions.getTokenBreakLimit());
      }
    }
  }

  public void supplement(DomElement supplementElement, ResourceManager resourceManager) {
    doSupplement(supplementElement, resourceManager);
  }

  /**
   * Make sure this instance has all necessary components defined.
   */
  public void validateInstance() {
    if (parser == null) {
      throw new IllegalArgumentException("No 'grammar' found in parser config!");
    }
  }

  /**
   * Convenience method to get the parser's grammar.
   */
  public AtnGrammar getGrammar() {
    AtnGrammar result = null;

    if (parser != null) {
      result = parser.getGrammar();
    }

    return result;
  }

  public List<AtnParseResult> seekAll(AtnParseBasedTokenizer tokenizer, Set<Integer> stopList,
                                      AtnParseOptions parseOptions, DataProperties overrides,
                                      AtomicBoolean die) {

    List<AtnParseResult> parseResults = null;
    tokenizer.setTokenizerOptions(tokenizerOptions);
    final boolean qualified = (prequalifier == null) ? true : prequalifier.prequalify(tokenizer);

    if (qualified) {
      parseResults = parser.seekAll(tokenizer, parseOptions, stopList, overrides, die);

      if (parseSelector != null && parseResults != null && parseResults.size() > 0) {
        for (AtnParseResult parseResult : parseResults) {
          parseSelector.selectParses(parseResult);
        }
      }

      // Add the parse results as tokens
      tokenizer.add(parseResults);
    }
    else {
      // return empty list when input not (pre)qualified for this parser
      parseResults = new ArrayList<AtnParseResult>();
    }

    return parseResults;
  }

  /**
   * Resolve ambiguities (if possible) within the parse results generated
   * through this wrapper.
   */
  public void resolveAmbiguities(List<AtnParseResult> parseResults) {
    if (this.ambiguityResolver == null) return;
    ambiguityResolver.resolve(parseResults);
  }
}
