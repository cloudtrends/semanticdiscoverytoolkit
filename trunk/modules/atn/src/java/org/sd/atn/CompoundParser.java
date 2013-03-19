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


import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import org.sd.token.StandardTokenizerOptions;
import org.sd.token.Tokenizer;
import org.sd.util.InputContext;
import org.sd.util.Usage;
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
@Usage(notes =
       "A CompoundParser applies org.sd.atn.AtnParser instances to tokenized\n" +
       "input, updating (compounding) the tokenization with parse results.\n" +
       "\n" +
       "In effect, if o = p(i), where o is parsed output; i is input, and\n" +
       "p is the application of the parser to the input, this class computes\n" +
       "pN(...p2(p1(i)) for each parser pI, I=1..N for N parsers."
  )
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

  private String[] parserWrapperIds;
  public String[] getParserIds() {
    if (parserWrapperIds == null && parserWrappers != null) {
      parserWrapperIds = parserWrappers.keySet().toArray(new String[parserWrappers.size()]);
    }
    return parserWrapperIds;
  }
  public AtnParserWrapper getParserWrapper(String parserId) {
    AtnParserWrapper result = null;

    if (parserWrappers != null) {
      
      result = parserWrappers.get(parserId);
    }

    return result;
  }

  private boolean verbose;
  public boolean getVerbose() {
    return verbose;
  }
  public void setVerbose(boolean verbose) {
    this.verbose = verbose;
  }

  private boolean traceflow;
  public boolean getTraceFlow() {
    return traceflow;
  }
  public void setTraceFlow(boolean traceflow) {
    this.traceflow = traceflow;
  }


  private DataProperties config;
  private DomElement outputNode;
  private ResourceManager resourceManager;

  public CompoundParser(DomElement configElement, ResourceManager resourceManager) {
    final DataProperties config = new DataProperties(configElement);
    init(config, resourceManager);
  }

  public CompoundParser(DataProperties config, ResourceManager resourceManager) {
    init(config, resourceManager);
  }

  public CompoundParser(AtnParserWrapper parserWrapper, ResourceManager resourceManager) {
    this.config = null;
    this.id = parserWrapper.getId();
    this.resourceManager = resourceManager;
    this.minNumTokens = Math.max(1, parserWrapper.getMinNumTokens());
    this.parserWrappers = new LinkedHashMap<String, AtnParserWrapper>();
    parserWrappers.put(parserWrapper.getId(), parserWrapper);
    this.outputNode = null;
  }

  private void init(DataProperties config, ResourceManager resourceManager) {
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

//TODO: I'm here... need to construct a CompoundParser instance w/a single AtnParserWrapper

    this.config = config;
    this.id = config.getString("id");
    this.resourceManager = resourceManager;

    this.minNumTokens = 1;
    this.parserWrappers = new LinkedHashMap<String, AtnParserWrapper>();
    final NodeList parserNodes = config.getDomElement().selectNodes("parser");
    for (int parserNodeIndex = 0; parserNodeIndex < parserNodes.getLength(); ++parserNodeIndex) {
      DomElement parserElement = (DomElement)parserNodes.item(parserNodeIndex);
      final AtnParserWrapper parserWrapper = AtnParserWrapper.buildInstance(parserElement, resourceManager);
      parserWrappers.put(parserWrapper.getId(), parserWrapper);
      if (parserWrapper.getMinNumTokens() > this.minNumTokens) this.minNumTokens = parserWrapper.getMinNumTokens();
    }

    this.outputNode = (DomElement)config.getDomElement().selectSingleNode("outputs");
  }

  public DataProperties getConfig() {
    return config;
  }


  public ParseOutputCollector parse(InputContext input, String[] flow, AtomicBoolean die) {
    return parse(input, flow, null, null, null, null, die);
  }

  public ParseOutputCollector parse(InputContext input, String[] flow, ParseOutputCollector output,
                                    Set<Integer> stopList, List<AtnParseResult> collector,
                                    InputOptions overrides, AtomicBoolean die) {
    final ParseOutputCollector theOutput = output == null ? new ParseOutputCollector(outputNode) : output;

    //NOTE: stopList holds indexes for starts of tokens that have been consumed by other parses

    collectOutput(input, flow, theOutput, stopList, collector, overrides, die);

    return theOutput;
  }

  public List<AtnParserWrapper> getParserWrappers(String[] flow) {
    final List<AtnParserWrapper> result = new ArrayList<AtnParserWrapper>();

    if (flow == null) {
      result.addAll(parserWrappers.values());
    }
    else {
      for (String id : flow) {
        result.add(parserWrappers.get(id));
      }
    }

    return result;
  }

  private void collectOutput(InputContext input, String[] flow, ParseOutputCollector output,
                             Set<Integer> stopList, List<AtnParseResult> collector,
                             InputOptions overrides, AtomicBoolean die) {
    if (flow == null) {
      flow = parserWrappers.keySet().toArray(new String[parserWrappers.size()]);
    }

    boolean gotResults = false;
    AtnParseBasedTokenizer currentTokenizer = null;
    AtnParserWrapper parserWrapper = null;

    if (!output.hasInputContext()) {
      output.setInputContext(input);
    }

    for (String id : flow) {
      parserWrapper = parserWrappers.get(id);

      if (parserWrapper == null) {
        throw new IllegalArgumentException("Can't find parserWrapper id=" + id + " in compoundParser=" + getId());
      }

      currentTokenizer = getCurrentTokenizer(currentTokenizer, output, input, parserWrapper);

      if (verbose) System.out.println("\nParser '" + this.id + ":" + id + "' parsing '" + input.getText() + "'...");

      if (traceflow) {
        System.out.println("traceflow--CompoundParser[" + this.id + ":" + id + "].collectOutput(" +
                           getId() + ":" + id + ") seeking parses for \"" +
                           currentTokenizer.getText() + "\"");
      }

      // get appropriate parseOptions and options
      AtnParseOptions parseOptions = parserWrapper.getParseOptions();
      DataProperties options = null;
      if (overrides != null) {
        options = overrides.getBaseOptions();
        parseOptions = overrides.getParseOptions(this.id, parserWrapper.getId(), parseOptions);
      }

      // generate initial parse results
      final List<AtnParseResult> parseResults = parserWrapper.seekAll(currentTokenizer, stopList, parseOptions, options, die);

      if (parseResults.size() > 0) {
        if (verbose && !gotResults) {
          System.out.println("\n Context: " + input.toString());
        }
        
        gotResults = true;
        
        if (verbose) System.out.println(" " + parserWrapper.getId() + " Parser got " + parseResults.size() + " parse results.");
      }

      // generate deep parse results and add parse results to output
      int parseResultNum = 1;
      AtnParse representativeParse = null;

      for (AtnParseResult parseResult : parseResults) {
        parseResult.generateParses(0);
        parseResult.setId(this.getId(), id);
        
        if (verbose) System.out.println("   ParseResult #" + parseResultNum + ": (" + parseResult.getNumParses() + " parses)");
        
        for (int parsit = 0; parsit < parseResult.getNumParses(); ++parsit) {
          final AtnParse parse = parseResult.getParse(parsit);
          if (verbose) {
            final String asterisk = parse.getSelected() ? "*" : " ";
            final String ruleId = parse.getStartRule().getRuleId();
            final String ruleText = (ruleId == null) ? "" : "  [" + ruleId + "]";
            System.out.println("   " + asterisk + " Parse #" + (parsit + 1) +
                               ": \"" + parse.getParsedText() + "\" == " +
                               parse.getParseTree().toString() + ruleText);
          }
          
          if (parse.getSelected()) {
            representativeParse = parse;
          }
        }
        
        if (representativeParse != null) {
          output.add(parseResult);
          if (collector != null) collector.add(parseResult);

          if (stopList != null) {
            final int pos = representativeParse.getStartIndex();
            stopList.add(pos);
          }
        }
        
        ++parseResultNum;
      }
    }

    if (parserWrapper != null && currentTokenizer != null && gotResults) {
      currentTokenizer = getCurrentTokenizer(currentTokenizer, output, input, parserWrapper);
      output.setOutputTokenizer(currentTokenizer);
    }

    if (!gotResults && verbose) System.out.print(" No results.");
  }

  private final AtnParseBasedTokenizer getCurrentTokenizer(AtnParseBasedTokenizer currentTokenizer, ParseOutputCollector output, InputContext input, AtnParserWrapper parserWrapper) {

    if (currentTokenizer == null) {
      currentTokenizer = buildTokenizer(output, input, parserWrapper);
    }
    else {
      if (!currentTokenizer.getOptions().equals(parserWrapper.getTokenizerOptions())) {
        currentTokenizer = buildTokenizer(output, input, parserWrapper);
      }
    }
    
    return currentTokenizer;
  }

  private final AtnParseBasedTokenizer buildTokenizer(ParseOutputCollector output, InputContext input, AtnParserWrapper parserWrapper) {

    final DomElement tokenizerConfig = parserWrapper.getTokenizerOverride();

    final AtnParseBasedTokenizer result =
      new AtnParseBasedTokenizer(resourceManager, tokenizerConfig, output.getParseResults(), input, parserWrapper.getTokenizerOptions());

    return result;
  }
}
