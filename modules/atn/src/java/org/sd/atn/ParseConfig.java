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
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import org.sd.token.StandardTokenizerOptions;
import org.sd.util.InputContext;
import org.sd.util.InputContextIterator;
import org.sd.util.Usage;
import org.sd.xml.DataProperties;
import org.sd.xml.DomDocument;
import org.sd.xml.DomElement;
import org.sd.xml.DomNode;
import org.sd.xml.XmlFactory;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Class for loading and managing parse configuration information.
 * <p>
 * @author Spence Koehler
 */
@Usage(notes =
       "Class for loading and managing parse configuration information\n" +
       "through XML input of the form:\n" +
       "\n" +
       "  <parse>\n" +
       "    <resources>\n" +
       "      <resource name='resource-name'>\n" +
       "        <jclass>resource-classpath</jclass>\n" +
       "        ...resource configuration...\n" +
       "      </resource>\n" +
       "      ...more resource elements...\n" +
       "    </resources>\n" +
       "\n" +
       "    <compoundParser>\n" +
       "      <id>compoundParser-id</id>\n" +
       "\n" +
       "      <outputs><markup><style>...</style></markup></outputs>\n" +
       "\n" +
       "      <parser>\n" +
       "        <id>parser-id</id>\n" +
       "        <grammar>path-to-grammar-file</grammar>\n" +
       "        <parseSelector>\n" +
       "          <jclass>selector-classpath</jclass>\n" +
       "        </parseSelector>\n" +
       "        <ambiguityResolver>\n" +
       "          <jclass>resolver-classpath</jclass>\n" +
       "        </ambiguityResolver>\n" +
       "        <tokenizer>\n" +
       "          <tokens>\n" +
       "            <token start='' end=''>token-category</token>\n" +
       "            ...more tokens...\n" +
       "          </tokens>\n" +
       "        </tokenizer>\n" +
       "        <tokenizerOptions>...</tokenizerOptions>\n" +
       "        <parseOptions>...</parseOptions>\n" +
       "      </parser>\n" +
       "\n" +
       "      ...more parser elements...\n" +
       "    </compoundParser>\n" +
       "\n" +
       "    ...more compoundParser elements...\n" +
       "  </parse>\n" +
       "\n" +
       "And supplements of the form:\n" +
       "\n" +
       "   <supplement>\n" +
       "     <classifier parser=\"compoundID:parserID\" id=\"classifierID\" mode=\"supplement|override\" ...supplemental attributes..>...supplemental elements...</classifier>\n" +
       "     <tokenizerOptions parser=\"compoundID:parserID\">\n" +
       "        ...overriding tokenizer options...\n" +
       "     </tokenizerOptions>\n" +
       "     <grammar parser=\"compoundID:parserID\">...supplemental file or elements...</grammar>\n" +
       "     <interpreter parser=\"compoundID:parserID\" mode=\"supplement|override\">\n" +
       "        ...\n" +
       "     </interpreter>\n" +
       "     <parseSelector parser=\"compoundID:parserID\" ...override attributes...>...override elements...</parseSelector>\n" +
       "   </supplement>"
  )
public class ParseConfig {
  
  public static ParseConfig buildInstance(DataProperties options) throws IOException {
    final File parseConfigFile = options.getFile("parseConfig", "workingDir");

    if (parseConfigFile != null) {
      if (GlobalConfig.verboseLoad()) {
        System.out.println(new Date() + ": ParseConfig loading '" +
                           parseConfigFile.getAbsolutePath() +
                           "' (exists=" + parseConfigFile.exists() + ")");
      }
    }

    if (parseConfigFile == null || !parseConfigFile.exists()) {
      throw new IllegalStateException("Must define 'parseConfig'! (" + parseConfigFile + ")");
    }

    final DomDocument domDocument = XmlFactory.loadDocument(parseConfigFile, false, options);
    final DomElement parseElement = (DomElement)domDocument.getDocumentElement();
    final StringBuilder description = new StringBuilder();

    if (GlobalConfig.verboseLoad()) {
      System.out.println(new Date() + ": ParseConfig init(" + parseConfigFile + ")");
    }
    final ParseConfig result = new ParseConfig(parseElement);

    description.append(parseConfigFile.getName());

    // add-in optional supplements
    final String supplementsString = options.getString("supplementalConfig", null);
    if (supplementsString != null && !"".equals(supplementsString)) {
      final String[] supplements = supplementsString.split("\\s*;\\s*");
      for (String supplement : supplements) {
        final File supplementFile = options.getWorkingFile(supplement, "workingDir");
        if (GlobalConfig.verboseLoad()) {
          System.out.println(new Date() + ": ParseConfig supplement(" + supplementFile + ")");
        }
        final DomDocument supDocument = XmlFactory.loadDocument(supplementFile, false, options);
        final DomElement supElement = (DomElement)supDocument.getDocumentElement();
        result.supplement(supElement);

        description.append('+').append(supplementFile.getName());
      }
    }

    result.setDescription(description.toString());
    result.setTraceFlow(options.getBoolean("traceflow", false));

    return result;
  }


  private String[] compoundParserIds;
  private String description;
  private boolean traceflow;
  private List<DomElement> supplementElements;

  private DataProperties parseConfigProperties;
  public DataProperties getParseConfigProperties() {
    return parseConfigProperties;
  }

  private Map<String, CompoundParser> id2CompoundParser;
  public Collection<CompoundParser> getCompoundParsers() {
    return id2CompoundParser.values();
  }
  public void setCompoundParsers(List<CompoundParser> parsers) {
    compoundParserIds = null;
    id2CompoundParser.clear();
    for (CompoundParser parser : parsers) {
      id2CompoundParser.put(parser.getId(), parser);
    }
  }

  public Map<String, CompoundParser> getId2CompoundParser() {
    return id2CompoundParser;
  }

  private boolean verbose;
  public boolean getVerbose() {
    return verbose;
  }
  public void setVerbose(boolean verbose) {
    this.verbose = verbose;
  }

  private ResourceManager resourceManager;
  public ResourceManager getResourceManager() {
    return resourceManager;
  }


  public ParseConfig(String filename) throws IOException {
    if (GlobalConfig.verboseLoad()) {
      System.out.println(new Date() + ": ParseConfig init(" + filename + ")");
    }
    init(new DataProperties(new File(filename)));
  }

  public ParseConfig(File configFile) throws IOException {
    if (GlobalConfig.verboseLoad()) {
      System.out.println(new Date() + ": ParseConfig init(" + configFile + ")");
    }
    init(new DataProperties(configFile));
  }

  public ParseConfig(DomElement configElement) {
    init(new DataProperties(configElement));
  }

  public ParseConfig(DataProperties properties) {
    init(properties);
  }


  private void init(DataProperties properties) {
    this.parseConfigProperties = properties;

    final boolean disableResources = properties.getBoolean("_disableResources", false);
    final DomElement resourcesElement = disableResources ? null : (DomElement)properties.getDomElement().selectSingleNode("resources");
    this.resourceManager = (resourcesElement == null) ? new ResourceManager(properties) : new ResourceManager(resourcesElement);

    this.id2CompoundParser = new LinkedHashMap<String, CompoundParser>();
    final NodeList cparserNodes = properties.getDomElement().selectNodes("compoundParser");
    if (cparserNodes != null && cparserNodes.getLength() > 0) {
      for (int i = 0; i < cparserNodes.getLength(); ++i) {
        final Node curNode = cparserNodes.item(i);
        if (curNode.getNodeType() != DomElement.ELEMENT_NODE) continue;
        final DomElement cparserNode = (DomElement)curNode;
        final CompoundParser cparser = new CompoundParser(cparserNode, resourceManager);
        this.id2CompoundParser.put(cparser.getId(), cparser);
      }
    }
    else {
      final DomElement parseNode = properties.getDomElement();
      if ("parser".equals(parseNode.getLocalName())) {
        final AtnParserWrapper parserWrapper = AtnParserWrapper.buildInstance(parseNode, resourceManager);
        final CompoundParser cparser = new CompoundParser(parserWrapper, resourceManager);
        this.id2CompoundParser.put(cparser.getId(), cparser);
      }
    }
  }


  public void setDescription(String description) {
    this.description = description;
  }

  public String getDescription() {
    return description;
  }

  public void setTraceFlow(boolean traceflow) {
    this.traceflow = traceflow;
  }

  public boolean getTraceFlow() {
    return traceflow;
  }


  public void close() {
    resourceManager.close();
  }


  public List<DomElement> getSupplementElements() {
    return supplementElements;
  }

  /**
   * Supplement resources according to the given supplement element.
   */
  public void supplement(DomElement supplementElement) {
    //
    // <supplement>
    //   <classifier parser="compoundID:parserID" id="classifierID" mode="supplement|override" ...supplemental attributes..>...supplemental elements...</classifier>
    //   <tokenizerOptions parser="compoundID:parserID">
    //      ...overriding tokenizer options...
    //   </tokenizerOptions>
    //   <grammar parser="compoundID:parserID">...supplemental file or elements...</grammar>
    //   <interpreter parser="compoundID:parserID" mode="supplement|override">
    //      ...
    //   </interpreter>
    //   <parseSelector parser="compoundID:parserID" ...override attributes...>...override elements...</parseSelector>
    // </supplement>
    //

    if (supplementElements == null) supplementElements = new ArrayList<DomElement>();
    supplementElements.add(supplementElement);

    final NodeList supplementNodes = supplementElement.getChildNodes();
    for (int nodeNum = 0; nodeNum < supplementNodes.getLength(); ++nodeNum) {
      final DomNode supplementNode = (DomNode)supplementNodes.item(nodeNum);
      if (supplementNode.getNodeType() != DomNode.ELEMENT_NODE) continue;

      final String directive = supplementNode.getNodeName().toLowerCase();
      final String parserId = supplementNode.getAttributeValue("parser");

      boolean supplemented = false;
      String failureReason = null;

      final AtnParserWrapper parserWrapper = getParserWrapper(parserId);
      if (parserWrapper != null) {
        final AtnGrammar grammar = parserWrapper.getGrammar();

        // 'classifier' supplement
        if ("classifier".equals(directive)) {
          if (grammar != null) {
            final String classifierId = supplementNode.getAttributeValue("id");
            final List<AtnStateTokenClassifier> classifiers = grammar.getClassifiers(classifierId);
            if (classifiers != null) {
              final String mode = supplementNode.getAttributeValue("mode", "supplement");

              if ("supplement".equals(mode)) {
                for (AtnStateTokenClassifier classifier : classifiers) {
                  classifier.supplement(supplementNode);
                  supplemented = true;
                }
              }
              else if ("override".equals(mode)) {
                final DomElement classifierNode = (DomElement)supplementNode.selectSingleNode(classifierId);
                if (classifierNode != null) {
                  final AtnStateTokenClassifier classifier =
                    (AtnStateTokenClassifier)resourceManager.getResource(classifierNode,
                                                                         new Object[] { grammar.getId2Normalizer() });
                  if (classifier != null) {
                    supplemented = true;
                    classifiers.clear();
                    classifiers.add(classifier);
                  }
                }
                else {
                  System.err.println(new Date() + " : ***WARNING : ParseConfig missing required '" +
                                     classifierId + "' node for supplement for classifier override, parserId '" +
                                     parserId + "'");
                }
              }
              else {
                System.err.println(new Date() + " : ***WARNING : ParseConfig unknown mode '" +
                                   mode + "' for classifier '" + classifierId + "' supplement " +
                                   " for parserId '" + parserId + "'");
              }
            }
            else {
              failureReason = "No classifiers for id=" + classifierId;
            }
          }
          else {
            failureReason = "No grammar";
          }
        }

        // 'tokenizerOptions' supplement
        else if ("tokenizeroptions".equals(directive)) {
          parserWrapper.setTokenizerOptions(new StandardTokenizerOptions((DomElement)supplementNode));
          supplemented = true;
        }

        // 'grammar' supplement
        else if ("grammar".equals(directive)) {
          if (grammar != null) {
            grammar.supplement(supplementNode);
            supplemented = true;
          }
        }

        // 'interpreter' supplement
        else if ("interpreter".equals(directive)) {
          final String mode = supplementNode.getAttributeValue("mode", "supplement");
          final AtnParseOptions parseOptions = parserWrapper.getParseOptions();

          if ("supplement".equals(mode)) {
            parseOptions.supplementParseInterpreter(supplementNode.asDomElement());
          }
          else if ("override".equals(mode)) {
            parseOptions.setParseInterpreter(supplementNode.asDomElement());
          }

          supplemented = true;
        }

        // parseSelector override
        else if ("parseselector".equals(directive)) {
          final AtnParseSelector parseSelectorOverride = (AtnParseSelector)resourceManager.getResource((DomElement)supplementNode);
          parserWrapper.setParseSelector(parseSelectorOverride);
          supplemented = true;
        }
      }
      else {
        failureReason = "No parserWrapper";
      }

      if (!supplemented) {
        System.err.println(new Date() + " : ***WARNING : ParseConfig unable to supplement " +
                           directive + " for parserId '" + parserId + "' (" + failureReason + ")");
      }
    }
  }


  /**
   * Given an ID of the form "compoundID:parserID", get the identified parser wrapper.
   */
  public AtnParserWrapper getParserWrapper(String complexID) {
    AtnParserWrapper result = null;

    if (complexID != null) {
      final String[] parserIds = complexID.split("\\s*:\\s*");
      if (parserIds.length == 2) {
        final String cpID = parserIds[0];
        final String pID = parserIds[1];
        final CompoundParser compoundParser = getCompoundParser(cpID);
        if (compoundParser != null) {
          result = compoundParser.getParserWrapper(pID);
        }
      }
    }

    return result;
  }

  public String[] getCompoundParserIds() {
    if (compoundParserIds == null) {
      compoundParserIds = id2CompoundParser.keySet().toArray(new String[id2CompoundParser.size()]);
    }
    return compoundParserIds;
  }

  public CompoundParser getCompoundParser(String compoundParserId) {
    CompoundParser result = null;

    if (id2CompoundParser != null) {
      result = id2CompoundParser.get(compoundParserId);
    }

    return result;
  }


  /**
   * Build settings that encompasses all of the atn parsers for the identified
   * compound parser. If compoundParserId does not identify an existing compound
   * parser (e.g. if null), then null settings will be returned.
   */
  public MultiParseSettings buildSettings(String compoundParserId) {
    return buildSettings(compoundParserId, null);
  }

  /**
   * Build settings that encompasses all of the identified compound parser's
   * atn parsers. Note that if antParserFlow is null, then all of the compound
   * parser's atn parsers will be executed in order when parsing input.
   */
  public MultiParseSettings buildSettings(String compoundParserId, String[] atnParserFlow) {
    MultiParseSettings result = null;

    final CompoundParser cp = id2CompoundParser.get(compoundParserId);
    if (cp != null) {
      result = ParseSettingsFactory.getInitialSettings(compoundParserId, atnParserFlow);
    }

    return result;
  }

  /**
   * Build settings that encompass all of this config's compound parsers
   * with all of their atn parsers, broadening input on reconfiguration
   * if indicated (and resetting if not.)
   */
  public MultiParseSettings buildSettings(ParseSettingsFactory.ReconfigureStrategy strategy) {
    return buildSettings(getCompoundParserIds(), strategy);
  }

  /**
   * Build settings that encompass all of the given compound parsers
   * with all of their atn parsers, broadening input on reconfiguration
   * if indicated (and resetting if not.)
   */
  public MultiParseSettings buildSettings(String[] compoundParserIds, ParseSettingsFactory.ReconfigureStrategy strategy) {
    MultiParseSettings result = null;

    if (compoundParserIds != null && compoundParserIds.length > 0) {
      result = buildSettings(compoundParserIds[0], null);

      for (int i = 1; i < compoundParserIds.length; ++i) {
        final String compoundParserId = compoundParserIds[i];
        final ParseSettings nextSettings = ParseSettingsFactory.getParseSettings(strategy, compoundParserId, null);
        result.add(nextSettings);
      }
    }

    return result;
  }


  public ParseOutputCollector parse(InputContextIterator inputContextIterator,
                                    MultiParseSettings settings, InputOptions overrides,
                                    AtomicBoolean die) {
    return settings.parse(this, inputContextIterator, overrides, die);
  }

  public ParseOutputCollector parse(InputContextIterator inputContextIterator,
                                    MultiParseSettings settings, ParseOutputCollector output,
                                    InputOptions overrides, AtomicBoolean die) {
    return settings.parse(this, inputContextIterator, output, overrides, die);
  }

  public ParseOutputCollector parse(InputContextIterator inputContextIterator,
                                    String compoundParserId, String[] flow,
                                    InputOptions overrides, AtomicBoolean die) {
    // NOTE: flow holds the ordered parser IDs within the identified compound
    //       parser to apply. When null, all parsers are applied in the order
    //       they were defined.
    return parse(inputContextIterator, compoundParserId, flow, null, overrides, die);
  }

  public ParseOutputCollector parse(InputContextIterator inputContextIterator,
                                    String compoundParserId, String[] flow,
                                    ParseOutputCollector output, InputOptions overrides,
                                    AtomicBoolean die) {
    
    // the stop list holds start positions of tokens that start parses and
    // therefore signal the end of tokens consumable for new parses. If we
    // don't care whether parses overlap, then set the stopList to null.
    //
    // note that this does not prevent later parsers from finding parses
    // starting at or within prior parses, it only stops new parses from
    // overrunning into prior parses.
    //
    // note also that when an InputContext is broadened, this stopList should
    // be correspondingly updated if its function is still valid.
    final Set<Integer> stopList = new HashSet<Integer>();
    final List<AtnParseResult> newResults = new ArrayList<AtnParseResult>();

    while (inputContextIterator.hasNext()) {
      final InputContext inputContext = inputContextIterator.next();

      if (stopList != null) stopList.clear(); // reset for new input

      output = parse(inputContext, compoundParserId, flow, output, stopList, newResults, overrides, die);
    }

    // resolve ambiguities, if possible
    final CompoundParser compoundParser = id2CompoundParser.get(compoundParserId);
    for (AtnParserWrapper wrapper : compoundParser.getParserWrappers(flow)) {
      wrapper.resolveAmbiguities(newResults);
    }

    return output;
  }

  /**
   * Get the output of running each compound parser (in the order indicated
   * by flow (ids) if non-null) over the given input (if non-null) or the
   * already configured input.
   */
  public ParseOutputCollector parse(InputContext inputContext, String compoundParserId,
                                    String[] flow, ParseOutputCollector output,
                                    Set<Integer> stopList, List<AtnParseResult> collector,
                                    InputOptions overrides, AtomicBoolean die) {
    ParseOutputCollector result = output;

    final CompoundParser compoundParser = id2CompoundParser.get(compoundParserId);

    if (compoundParser != null) {
      compoundParser.setVerbose(this.getVerbose());
      compoundParser.setTraceFlow(this.getTraceFlow());
      result = compoundParser.parse(inputContext, flow, result, stopList, collector, overrides, die);
    }
    else {
      if (verbose) {
        System.out.println("***WARNING: parseConfig.parse called with unknown compoundParserId=" + compoundParserId + "!");
      }
    }

    return result;
  }
}
