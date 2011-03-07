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
import org.sd.util.InputContext;
import org.sd.util.InputContextIterator;
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
public class ParseConfig {
  
  public static ParseConfig buildInstance(DataProperties options) throws IOException {
    final File parseConfigFile = options.getFile("parseConfig", "workingDir");

    if (parseConfigFile == null) {
      throw new IllegalStateException("Must define 'parseConfig'!");
    }

    final DomDocument domDocument = XmlFactory.loadDocument(parseConfigFile, false, options);
    final DomElement parseElement = (DomElement)domDocument.getDocumentElement();

    final ParseConfig result = new ParseConfig(parseElement);

    // add-in optional supplements
    final String supplementsString = options.getString("supplementalConfig", null);
    if (supplementsString != null) {
      final String[] supplements = supplementsString.split("\\s*;\\s*");
      for (String supplement : supplements) {
        final File supplementFile = options.getWorkingFile(supplement, "workingDir");
        final DomDocument supDocument = XmlFactory.loadDocument(supplementFile, false, options);
        final DomElement supElement = (DomElement)supDocument.getDocumentElement();
        result.supplement(supElement);
      }
    }

    return result;
  }


  private String[] compoundParserIds;

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
    init(new DataProperties(new File(filename)));
  }

  public ParseConfig(File configFile) throws IOException {
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
    if (cparserNodes != null) {
      for (int i = 0; i < cparserNodes.getLength(); ++i) {
        final Node curNode = cparserNodes.item(i);
        if (curNode.getNodeType() != DomElement.ELEMENT_NODE) continue;
        final DomElement cparserNode = (DomElement)curNode;
        final CompoundParser cparser = new CompoundParser(cparserNode, resourceManager);
        this.id2CompoundParser.put(cparser.getId(), cparser);
      }
    }
  }


  public void close() {
    resourceManager.close();
  }


  /**
   * Supplement resources according to the given supplement element.
   */
  public void supplement(DomElement supplementElement) {
    //
    // <supplement>
    //   <classifier parser="compoundID:parserID" id="classifierID" ...supplemental attributes..>...supplemental elements...</classifier>
    //   <grammar parser="compoundID:parserID">...supplemental file or elements...</grammar>
    // </supplement>
    //

    final NodeList supplementNodes = supplementElement.getChildNodes();
    for (int nodeNum = 0; nodeNum < supplementNodes.getLength(); ++nodeNum) {
      final DomNode supplementNode = (DomNode)supplementNodes.item(nodeNum);
      if (supplementNode.getNodeType() != DomNode.ELEMENT_NODE) continue;

      final String directive = supplementNode.getNodeName().toLowerCase();
      final String parserId = supplementNode.getAttributeValue("parser");

      boolean supplemented = false;

      final AtnGrammar grammar = getGrammar(parserId);
      if (grammar != null) {

        // 'classifier' supplement
        if ("classifier".equals(directive)) {
          final String classifierId = supplementNode.getAttributeValue("id");
          final List<AtnStateTokenClassifier> classifiers = grammar.getClassifiers(classifierId);
          if (classifiers != null) {
            for (AtnStateTokenClassifier classifier : classifiers) {
              classifier.supplement(supplementNode);
              supplemented = true;
            }
          }
        }

        // 'grammar' supplement
        else if ("grammar".equals(directive)) {
          grammar.supplement(supplementNode);
          supplemented = true;
        }
      }

      if (!supplemented) {
        System.err.println(new Date() + " : ***WARNING : ParseConfig unable to supplement " +
                           directive + " for parserId '" + parserId + "'");
      }
    }
  }


  /**
   * Given an ID of the form "compoundID:parserID", get the identified parser.
   */
  public AtnParser getParser(String complexID) {
    AtnParser result = null;

    if (complexID != null) {
      final String[] parserIds = complexID.split("\\s*:\\s*");
      if (parserIds.length == 2) {
        final String cpID = parserIds[0];
        final String pID = parserIds[1];
        final CompoundParser compoundParser = getCompoundParser(cpID);
        if (compoundParser != null) {
          final AtnParserWrapper parserWrapper = compoundParser.getParserWrapper(pID);
          if (parserWrapper != null) {
            result = parserWrapper.getParser();
          }
        }
      }
    }

    return result;
  }

  public AtnGrammar getGrammar(String complexID) {
    AtnGrammar result = null;

    final AtnParser parser = getParser(complexID);
    if (parser != null) {
      result = parser.getGrammar();
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


  public ParseOutputCollector parse(InputContextIterator inputContextIterator, MultiParseSettings settings, DataProperties overrides) {
    return settings.parse(this, inputContextIterator, overrides);
  }

  public ParseOutputCollector parse(InputContextIterator inputContextIterator, MultiParseSettings settings, ParseOutputCollector output, DataProperties overrides) {
    return settings.parse(this, inputContextIterator, output, overrides);
  }

  public ParseOutputCollector parse(InputContextIterator inputContextIterator, String compoundParserId, String[] flow, DataProperties overrides) {
    // NOTE: flow holds the ordered parser IDs within the identified compound
    //       parser to apply. When null, all parsers are applied in the order
    //       they were defined.
    return parse(inputContextIterator, compoundParserId, flow, null, overrides);
  }

  public ParseOutputCollector parse(InputContextIterator inputContextIterator, String compoundParserId, String[] flow, ParseOutputCollector output, DataProperties overrides) {
    
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

      output = parse(inputContext, compoundParserId, flow, output, stopList, newResults, overrides);
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
  public ParseOutputCollector parse(InputContext inputContext, String compoundParserId, String[] flow, ParseOutputCollector output, Set<Integer> stopList, List<AtnParseResult> collector, DataProperties overrides) {
    ParseOutputCollector result = output;

    final CompoundParser compoundParser = id2CompoundParser.get(compoundParserId);

    if (compoundParser != null) {
      compoundParser.setVerbose(this.getVerbose());
      result = compoundParser.parse(inputContext, flow, result, stopList, collector, overrides);
    }
    else {
      if (verbose) {
        System.out.println("***WARNING: parseConfig.parse called with unknown compoundParserId=" + compoundParserId + "!");
      }
    }

    return result;
  }
}
