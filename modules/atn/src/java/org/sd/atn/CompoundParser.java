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


import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.sd.token.StandardTokenizerOptions;
import org.sd.token.Tokenizer;
import org.sd.util.InputContext;
import org.sd.xml.DataProperties;
import org.sd.xml.DomElement;
import org.sd.xml.DomNode;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * A CompoundParser applies ATN Parser instances to tokenized input,
 * updating (compounding) the tokenization with parse results.
 * <p>
 * In effect, if o = p(i), where o is parsed output; i is input, and
 * p is the application of the parser to the input, this class computes
 * pN(...p2(p1(i)) for each parser pI, I=1..N for N parsers.
 * <p>
 * @author Spence Koehler
 */
public class CompoundParser {
  
  private String id;
  public String getId() {
    return id;
  }

  private int minNumTokens;
  public int getMinNumTokens() {
    return minNumTokens;
  }

  private Map<String, AtnParserWrapper> parserWrappers;
  public Map<String, AtnParserWrapper> getParserWrappers() {
    return parserWrappers;
  }

  private boolean verbose;
  public boolean getVerbose() {
    return verbose;
  }
  public void setVerbose(boolean verbose) {
    this.verbose = verbose;
  }


  private DomElement outputNode;

  public CompoundParser(DomElement configElement) {
    final DataProperties config = new DataProperties(configElement);
    init(config);
  }

  public CompoundParser(DataProperties config) {
    init(config);
  }

  private void init(DataProperties config) {
    //
    // expected format:
    //
    //   <compoundParser>
    //
    //     <id>compound-parser-id</id>
    //
    //     <outputs>
    //       <markup>
    //         <style>border: 3px solid; border-color: green; background-color: yellow;</style>
    //       </markup>
    //       <stdout/>
    //       <file type='dom'>path-to-dom-file.dom</file>
    //     </outputs>
    //
    //     <parser>
    //
    //       <id>id-for-parser</id>
    //
    //       <grammar>path-to-grammar-file</grammar>
    //
    //       <parseSelector>
    //         <jclass>java-InputReprocessor-class</jclass>
    //         <class>class-of-optional-parse-selector</class>
    //         <dll>path-to-parse-selector-dll</dll>
    //       </parseSelector>
    //
    //       <!-- optional (AtnParseBasedTokenizer) tokenizer specification for overriding the default AtnParseBasedTokenizer built within ParseInputIterator -->
    //       <tokenizer>
    //         <jclass>java-InputReprocessor-class</jclass>
    //         <class></class>
    //         <dll></dll>
    //         ...
    //       </tokenizer>
    //
    //       <tokenizerOptions>
    //         <revisionStrategy>LSL</revisionStrategy>
    //         <lowerUpperBreak>ZERO_WIDTH_SOFT_BREAK</lowerUpperBreak>
    //         <upperLowerBreak>NO_BREAK</upperLowerBreak>
    //         <upperDigitBreak>NO_BREAK</upperDigitBreak>
    //         <lowerDigitBreak>ZERO_WIDTH_SOFT_BREAK</lowerDigitBreak>
    //         <digitUpperBreak>NO_BREAK</digitUpperBreak>
    //         <digitLowerBreak>NO_BREAK</digitLowerBreak>
    //         <nonEmbeddedDoubleDashBreak>SINGLE_WIDTH_HARD_BREAK</nonEmbeddedDoubleDashBreak>
    //         <embeddedDoubleDashBreak>SINGLE_WIDTH_HARD_BREAK</embeddedDoubleDashBreak>
    //         <leftBorderedDashBreak>NO_BREAK</leftBorderedDashBreak>
    //         <rightBorderedDashBreak>NO_BREAK</rightBorderedDashBreak>
    //         <freeStandingDashBreak>SINGLE_WIDTH_HARD_BREAK</freeStandingDashBreak>
    //         <whitespaceBreak>SINGLE_WIDTH_SOFT_BREAK</whitespaceBreak>
    //       </tokenizerOptions>
    //
    //       <parseOptions>
    //         <consumeAllText>true</consumeAllText>
    //         <skipTokenLimit>0</skipTokenLimit>
    //         <firstParseOnly>false</firstParseOnly>
    //       </parseOptions>
    //     </parser>
    //
    //     <parser>...</parser>
    //
    //   </compoundParser>
    //


    this.id = config.getString("id");

    this.minNumTokens = 1;
    this.parserWrappers = new LinkedHashMap<String, AtnParserWrapper>();
    final NodeList parserNodes = config.getDomElement().selectNodes("parser");
    for (int parserNodeIndex = 0; parserNodeIndex < parserNodes.getLength(); ++parserNodeIndex) {
      final DomElement parserElement = (DomElement)parserNodes.item(parserNodeIndex);
      final AtnParserWrapper parserWrapper = new AtnParserWrapper(parserElement);
      parserWrappers.put(parserWrapper.getId(), parserWrapper);
      if (parserWrapper.getMinNumTokens() > this.minNumTokens) this.minNumTokens = parserWrapper.getMinNumTokens();
    }

    this.outputNode = (DomElement)config.getDomElement().selectSingleNode("outputs");
  }


  public ParseOutputCollector parse(InputContext input, String[] flow) {
    return parse(input, flow, null, null);
  }

  public ParseOutputCollector parse(InputContext input, String[] flow, ParseOutputCollector output, Set<Integer> stopList) {
    final ParseOutputCollector theOutput = output == null ? new ParseOutputCollector(outputNode) : output;

    //NOTE: stopList holds indexes for starts of tokens that have been consumed by other parses

    collectOutput(input, flow, theOutput, stopList);

    return theOutput;
  }

  private void collectOutput(InputContext input, String[] flow, ParseOutputCollector output, Set<Integer> stopList) {
    if (verbose) System.out.print("\nParsing '" + input.getText() + "'...");

    if (flow == null) {
      flow = parserWrappers.keySet().toArray(new String[parserWrappers.size()]);
    }

    boolean gotResults = false;
    AtnParseBasedTokenizer currentTokenizer = null;

    for (String id : flow) {
      final AtnParserWrapper parserWrapper = parserWrappers.get(id);

      if (parserWrapper == null) {
        throw new IllegalArgumentException("Can't find parserWrapper id=" + id + " in compoundParser=" + getId());
      }

      currentTokenizer = getCurrentTokenizer(currentTokenizer, output, input, parserWrapper);

      final List<AtnParseResult> parseResults = parserWrapper.seekAll(currentTokenizer, stopList);

      if (parseResults.size() > 0) {
        if (verbose && !gotResults) {
          System.out.println("\n Context: " + input.toString());
        }
        
        gotResults = true;
        
        if (verbose) System.out.println(" " + parserWrapper.getId() + " Parser got " + parseResults.size() + " parse results.");
      }

      int parseResultNum = 1;
      AtnParse representativeParse = null;

      for (AtnParseResult parseResult : parseResults) {
        parseResult.generateParses(0);
        
        if (verbose) System.out.println("   ParseResult #" + parseResultNum + ": (" + parseResult.getNumParses() + " parses)");
        
        for (int parsit = 0; parsit < parseResult.getNumParses(); ++parsit) {
          final AtnParse parse = parseResult.getParse(parsit);
          final String asterisk = parse.getSelected() ? "*" : " ";
          if (verbose) {
            System.out.println("   " + asterisk + " Parse #" + (parsit + 1) +
                               ": \"" + parse.getParsedText() + "\" == " +
                               parse.getParseTree().toString());
          }
          
          if (parse.getSelected()) {
            representativeParse = parse;
          }
        }
        
        if (representativeParse != null) {
          output.add(parseResult);

          if (stopList != null) {
            final int pos = representativeParse.getStartIndex();
            stopList.add(pos);
          }
        }
        
        ++parseResultNum;
      }
    }

    if (!gotResults && verbose) System.out.print(" No results.");
  }

  private final AtnParseBasedTokenizer getCurrentTokenizer(AtnParseBasedTokenizer currentTokenizer, ParseOutputCollector output, InputContext input, AtnParserWrapper parserWrapper) {

    final DomElement tokenizerConfig = parserWrapper.getTokenizerOverride();

    if (currentTokenizer == null) {
      currentTokenizer = new AtnParseBasedTokenizer(tokenizerConfig, output.getParseResults(), input, parserWrapper.getTokenizerOptions());
    }
    else {
      if (!currentTokenizer.getOptions().equals(parserWrapper.getTokenizerOptions())) {
        currentTokenizer = new AtnParseBasedTokenizer(tokenizerConfig, output.getParseResults(), input, parserWrapper.getTokenizerOptions());
      }
    }
    
    return currentTokenizer;
  }
}
