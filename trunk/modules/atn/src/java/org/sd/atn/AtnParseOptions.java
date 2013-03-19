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


import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import org.sd.xml.DataProperties;
import org.sd.xml.DomElement;
import org.w3c.dom.NodeList;

/**
 * Container for options for AtnParser instances.
 * <p>
 * @author Spence Koehler
 */
public class AtnParseOptions {
  
  private DataProperties options;
  public DataProperties getOptions() {
    return options;
  }

  private boolean consumeAllText;
  /**
   * If true, a successful parse must consume all available text.
   */
  public boolean getConsumeAllText() {
    return consumeAllText;
  }
  public void setConsumeAllText(boolean consumeAllText) {
    this.consumeAllText = consumeAllText;
  }

  private int skipTokenLimit;
  /**
   * A limit for the number of tokens that may be skipped while parsing.
   * 
   * This gives a way to account for random unknown and/or unpredicted tokens
   * while still allowing a parse to succeed.
   */
  public int getSkipTokenLimit() {
    return skipTokenLimit;
  }
  public void setSkipTokenLimit(int skipTokenLimit) {
    this.skipTokenLimit = skipTokenLimit;
  }

  private boolean firstParseOnly;
  /**
   * A flag to limit (initial) parsing to the first full parse encountered.
   * 
   * This option does not prevent additional parses from being computed,
   * but allows the first parse to be inspected before deciding to continue
   * searching for valid parses. While true, the parser will return control
   * to its caller after each successful parse is found as opposed to
   * retaining control of its thread until all possible parses have been
   * exhausted.
   */
  public boolean getFirstParseOnly() {
    return firstParseOnly;
  }
  public void setFirstParseOnly(boolean firstParseOnly) {
    this.firstParseOnly = firstParseOnly;
  }

  private boolean adjustInputForTokens;
  /**
   * A flag to specify whether to adjust input granularity based on the
   * minimum number of tokens required by the grammar.
   * 
   * When false, each text node becomes input to the parsing; otherwise,
   * text under higher-level dom nodes may be the adjusted input if
   * warranted and possible.
   */
  public boolean getAdjustInputForTokens() {
    return adjustInputForTokens;
  }
  public void setAdjustInputForTokens(boolean adjustInputForTokens) {
    this.adjustInputForTokens = adjustInputForTokens;
  }

  private ParseInterpreter parseInterpreter;
  /**
   * An interpreter to use with parses.
   */
  public ParseInterpreter getParseInterpreter() {
    return parseInterpreter;
  }
  public void setParseInterpreter(ParseInterpreter parseInterpreter) {
    this.parseInterpreter = parseInterpreter;
  }

  private List<String> startRules;
  /**
   * Overrides the grammar's start rules for parsing.
   */
  public List<String> getStartRules() {
    return startRules;
  }

  public boolean hasStartRules() {
    return startRules != null && startRules.size() > 0;
  }

  /** Set the names of start rules to use. */
  public void setStartRules(String[] startRules) {
    this.startRules = Arrays.asList(startRules);
  }

  private ResourceManager resourceManager;
  public ResourceManager getResourceManager() {
    return resourceManager;
  }

  /**
   * Default constructor.
   * 
   * ConsumeAllText = false;         (a parse is only successful if all text is consumed)
   * SkipTokenLimit = 0;             (doesn't allow for any skipped tokens)
   * FirstParseOnly = false;         (exhaust all parses)
   * AdjustInputForTokens = false;   (don't adjust input granularity)
   * ParseInterpreter = null;        (no parse interpreter)
   * StartRules = null;              (use grammar's start rules)
   * 
   */
  public AtnParseOptions(ResourceManager resourceManager) {
    this.resourceManager = resourceManager;
    this.consumeAllText = false;
    this.skipTokenLimit = 0;
    this.firstParseOnly = false;
    this.adjustInputForTokens = false;
    this.parseInterpreter = null;
    this.startRules = null;
  }

  /**
   * Construct based on options specified in the given xml element.
   * 
   * <parseOptions>
   *   <consumeAllText>false</consumeAllText>
   *   <skipTokenLimit>0</skipTokenLimit>
   *   <firstParseOnly>false</firstParseOnly>
   *   <adjustInputForTokens>false</adjustInputForTokens>
   *   <parseInterpreter><class>IParseInterpreter-class</class><dll>parse-interpreter-dll</dll></parseInterpreter>
   *   <start>acceptable-start-category-1</start>
   *   <start>acceptable-start-category-2</start>
   *   ...
   * </parseOptions>
   */
  public AtnParseOptions(DomElement optionsElement, ResourceManager resourceManager) {
    this.resourceManager = resourceManager;
    DataProperties options = new DataProperties(optionsElement);
    init(options);
  }

  /**
   * Construct based on the options specified in DataProperties.
   * 
   * consumeAllText (default=false)
   * skipTokenLimit (default=0)
   * firstParseOnly (default=false)
   * adjustInputForTokens (default=false)
   * parseInterpreter (default=null)
   * 
   * multiple "start" options sought from options' xml.
   */
  public AtnParseOptions(DataProperties options, ResourceManager resourceManager) {
    this.resourceManager = resourceManager;
    init(options);
  }

  /**
   * Copy constructor.
   */
  public AtnParseOptions(AtnParseOptions options) {
    this.options = options.options;
    this.resourceManager = options.resourceManager;
    this.consumeAllText = options.consumeAllText;
    this.skipTokenLimit = options.skipTokenLimit;
    this.firstParseOnly = options.firstParseOnly;
    this.adjustInputForTokens = options.adjustInputForTokens;
    this.parseInterpreter = options.parseInterpreter;
    this.startRules = options.startRules;
  }

  private final void init(DataProperties options) {
    //
    // <parseOptions>
    //   <consumeAllText>false</consumeAllText>
    //   <skipTokenLimit>0</skipTokenLimit>
    //   <firstParseOnly>false</firstParseOnly>
    //   <adjustInputForTokens>false</adjustInputForTokens>
    //   <parseInterpreter><class>IParseInterpreter-class</class><dll>parse-interpreter-dll</dll></parseInterpreter>
    //   <start>acceptable-start-category-1</start>
    //   <start>acceptable-start-category-2</start>
    //   ...
    // </parseOptions>
    //
    // Where 'start' node(s) are optional and specify which rule (categories)
    // are acceptable starting rules when parsing. When specified, these will
    // override default grammar start rules; otherwise, those grammar rules
    // that are designated as starts will be used for the parse.
    //

    this.options = options;
    this.consumeAllText = options.getBoolean("consumeAllText", false);
    this.skipTokenLimit = options.getInt("skipTokenLimit", 0);
    this.firstParseOnly = options.getBoolean("firstParseOnly", false);
    this.adjustInputForTokens = options.getBoolean("adjustInputForTokens", false);

    final DomElement parseInterpreterNode = (DomElement)options.getDomElement().selectSingleNode("parseInterpreter");
    setParseInterpreter(parseInterpreterNode);

    final NodeList startNodes = options.getDomElement().selectNodes("start");
    if (startNodes != null) {
      this.startRules = new ArrayList<String>();
      for (int i = 0; i < startNodes.getLength(); ++i) {
        DomElement startElement = (DomElement)startNodes.item(i);
        this.startRules.add(startElement.getTextContent().trim());
      }
    }
  }

  public final void setParseInterpreter(DomElement parseInterpreterNode) {
    this.parseInterpreter = (parseInterpreterNode != null) ? (ParseInterpreter)resourceManager.getResource(parseInterpreterNode) : null;
  }

  public void supplementParseInterpreter(DomElement parseInterpreterNode) {
    if (parseInterpreter != null) {
      parseInterpreter.supplement(parseInterpreterNode);
    }
  }
}
