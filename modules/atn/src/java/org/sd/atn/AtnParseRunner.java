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


import java.io.BufferedWriter;
import java.io.File;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.io.IOException;
import org.sd.io.FileUtil;
import org.sd.util.ExecUtil;
import org.sd.util.FileContext;
import org.sd.util.InputContext;
import org.sd.util.InputContextIterator;
import org.sd.util.WhitespacePolicy;
import org.sd.util.tree.Tree;
import org.sd.xml.DataProperties;
import org.sd.xml.DomContextIterator;
import org.sd.xml.DomContextIteratorFactory;
import org.sd.xml.DomDocument;
import org.sd.xml.DomElement;
import org.sd.xml.DomIterationStrategy;
import org.sd.xml.DomNode;
import org.sd.xml.DomTextBlockIterationStrategy;
import org.sd.xml.DomTextIterationStrategy;
import org.sd.xml.XmlFactory;
import org.sd.xml.XmlLite;

/**
 * 
 * <p>
 * @author Spence Koehler
 */
public class AtnParseRunner {
  
  protected DataProperties options;
  protected ParseConfig parseConfig;
  protected boolean verbose;
  protected String compoundParserId;
  protected String[] flow;
  protected String[] override;

  public AtnParseRunner(DataProperties dataProperties) throws IOException {
    this.options = dataProperties;
    this.parseConfig = loadParseConfig(options);

    updateOptions();
  }

  /**
   * Update internal variables according to the current state of the
   * options.
   */
  public final void updateOptions() {
    this.verbose = options.getBoolean("verbose", false);
    this.compoundParserId = options.getString("compoundParserId", null);
    this.flow = loadFlow(options);
    this.override = loadOverride(options);

    this.parseConfig.setVerbose(verbose);
  }

  private final ParseConfig loadParseConfig(DataProperties options) throws IOException {
    final File parseConfigFile = options.getFile("parseConfig", "workingDir");

    if (parseConfigFile == null) {
      throw new IllegalStateException("Must define 'parseConfig'!");
    }

    final DomDocument domDocument = XmlFactory.loadDocument(parseConfigFile, false, options);
    final DomElement parseElement = (DomElement)domDocument.getDocumentElement();
    return new ParseConfig(parseElement);
  }

  private final String[] loadFlow(DataProperties options) {
    String[] result = null;

    final String flowString = options.getString("flow", null);
    if (flowString != null) {
      result = flowString.split("\\s*,\\s*");
    }

    return result;
  }

  private final String[] loadOverride(DataProperties options) {
    String[] result = null;

    final String overrideString = options.getString("override", null);
    if (overrideString != null) {
      result = overrideString.split("\\s*,\\s*");
    }

    return result;
  }

  public void run() throws IOException {
    final ParseOutputCollector output = buildOutput();
    final ExtractionGroups extractionGroups = output != null ? new ExtractionGroups(output) : null;
    handleOutput(output, extractionGroups);
  }

  public DataProperties getOptions() {
    return options;
  }

  public ParseOutputCollector buildOutput() throws IOException {
    ParseOutputCollector output = null;

    final String inputLines = options.getString("inputLines", null);
    if (inputLines != null) {
      output = parseLines(new File(inputLines));
    }
    else {
      final String inputHtml = options.getString("inputHtml", null);
      if (inputHtml != null) {
        final String diffHtml = options.getString("diffHtml", null);
        output = parseHtml(new File(inputHtml), diffHtml == null ? null : new File(diffHtml));
      }
    }

    if (output != null) {
      final String stage2cpid = options.getString("stage2cpid", null);
      if (stage2cpid != null) {
        // recycle extraction groups as input through the indicated compound parser
        options.set("compoundParserId", stage2cpid);
        options.set("flow", null);
        options.set("override", null);
        updateOptions();
      
        final ExtractionGroups extractionGroups = new ExtractionGroups(output); 
        for (ExtractionGroup extractionGroup : extractionGroups.getExtractionGroups()) {
          final DomNode groupNode = extractionGroup.getInputNode();
          if (groupNode != null) {
            final boolean isHtml = options.getString("inputHtml", null) != null;
            output = parseDomNode(groupNode, isHtml, output);
          }
        }
      }
    }

    return output;
  }

  public void handleOutput(ParseOutputCollector output, ExtractionGroups extractionGroups) throws IOException {

    final String outputXml = options.getString("outputXml", null);
    if (outputXml != null) {

      final String outputXmlName = FileUtil.buildOutputFilename(outputXml, ".xml");
      final File outputXmlFile = new File(outputXmlName);

      final boolean dumpGroups = options.getBoolean("dumpGroups", true);
      if (dumpGroups) {
        // dump extraction groups as xml
        writeExtractionGroupsXml(outputXmlFile, output, extractionGroups);
      }
      else {
        // dump parse results as xml
        writeParseResultsXml(outputXmlFile, output);
      }
    }

    final boolean showResults = options.getBoolean("showResults", true);
    final boolean writeMarkup = options.getBoolean("writeMarkup", true);
    final boolean showMarkup = options.getBoolean("showMarkup", false);
    final boolean briefResults = options.getBoolean("briefResults", true);
    if ((showResults || briefResults || writeMarkup || showMarkup) && output != null) {
      showOutput(output, extractionGroups);
    }
  }

  private final void writeParseResultsXml(File outputXmlFile, ParseOutputCollector output) throws IOException {
    final BufferedWriter writer = FileUtil.getWriter(outputXmlFile);

    System.out.println("\nWriting xml parse results output to '" + outputXmlFile.getAbsolutePath() + "'");

    final boolean showOnlySelected = options.getBoolean("showOnlySelected", true);
    final boolean showOnlyInterpreted = options.getBoolean("showOnlyInterpreted", true);
    writer.write(output.asXml(true, showOnlySelected, showOnlyInterpreted, 0, 2));

    writer.close();
  }

  private final void writeExtractionGroupsXml(File outputXmlFile, ParseOutputCollector output, ExtractionGroups extractionGroups) throws IOException {
    System.out.println("\nWriting xml extraction groups output to '" + outputXmlFile.getAbsolutePath() + "'");

    final XmlParseOutput xmlOutput = new XmlParseOutput(output.getParseSourceInfo(), 0, 2);
    xmlOutput.addExtractionGroups(extractionGroups);
    final String xml = xmlOutput.getOutput(true);

    final BufferedWriter writer = FileUtil.getWriter(outputXmlFile);
    writer.write(xml);
    writer.close();
  }

  public void showOutput(ParseOutputCollector output, ExtractionGroups extractionGroups) throws IOException {
    if (output == null) return;

    final String source = getSourceString(output.getParseSourceInfo());

    final boolean showResults = options.getBoolean("showResults", true);
    final boolean briefResults = options.getBoolean("briefResults", true);

    final boolean showOnlyInterpreted = options.getBoolean("showOnlyInterpreted", false);
    // force executing show markup
    final List<AtnParse> parses = output.getTopInterpretedParses(showOnlyInterpreted);

    final LinkedHashSet<DomDocument> domDocuments = output.getDomDocuments();
    if (domDocuments != null && domDocuments.size() > 0) {

      final boolean writeMarkup = options.getBoolean("writeMarkup", true);
      final boolean showMarkup = options.getBoolean("showMarkup", false);

      for (DomDocument domDocument : domDocuments) {
        if (showMarkup || writeMarkup) {
          final String outputFile = writeModifiedDocument(domDocument);

          if (showMarkup) {
            ExecUtil.executeProcess(new String[]{"/usr/bin/firefox", "file://" + outputFile});
          }
        }
      }
    }

    if (showResults || briefResults) {
      if (!briefResults) {
        final boolean showInterpretations = options.getBoolean("showInterpretations", true);
        if (showInterpretations) {
          System.out.println("\nOutput: ");
          output.showParseResults(showOnlyInterpreted);
        }
      }
      else {
        // show brief results
        final boolean numberKeys = options.getBoolean("numberKeys", true);
        extractionGroups.showExtractionGroups(source, numberKeys);
      }
    }
  }

  private final String getSourceString(ParseSourceInfo sourceInfo) {
    String result = sourceInfo.getUrl();

    if (result == null || "".equals(result)) {
      result = sourceInfo.getInputString();
    }

    if (result == null || "".equals(result)) {
      result = "[unknown-source]";
    }

    return result;
  }

  public ParseOutputCollector parseLines(File inputLines) throws IOException {
    final FileContext fileContext = new FileContext(inputLines, WhitespacePolicy.HYPERTRIM);

    //todo: parameterize whether to broaden the scope of the iterator
    //      - if line records, then don't broaden.
    //      - if paragraphs, then broaden (but at which compound parser boundaries?)
    final boolean broaden = false;

    final ParseOutputCollector output = parseInput(fileContext.getLineIterator(), broaden, null);

    final ParseSourceInfo sourceInfo = new ParseSourceInfo(inputLines, false, false);
    output.setParseSourceInfo(sourceInfo);

    return output;
  }

  public ParseOutputCollector parseHtml(File inputHtml, File diffHtml) throws IOException {
    //todo: parameterize textBlock flag
    final boolean textBlock = false;

    final DomIterationStrategy strategy = textBlock ? DomTextBlockIterationStrategy.INSTANCE : DomTextIterationStrategy.INSTANCE;

    //todo: parameterize (and implement) whether to broaden the scope of the iterator
    final boolean broaden = false;

    final DomContextIterator inputContextIterator = DomContextIteratorFactory.getDomContextIterator(inputHtml, diffHtml, true, strategy);
    final ParseOutputCollector result = parseInput(inputContextIterator, broaden, null);

    final ParseSourceInfo sourceInfo = new ParseSourceInfo(inputHtml, true, true);
    if (diffHtml != null) sourceInfo.setDiffString(diffHtml.getAbsolutePath());
    result.setParseSourceInfo(sourceInfo);

    return result;
  }

  public ParseOutputCollector parseDomNode(DomNode domNode, boolean isHtml, ParseOutputCollector priorOutput) {
    final DomContextIterator inputContextIterator = DomContextIteratorFactory.getDomContextIterator(domNode);
    final ParseOutputCollector result = parseInput(inputContextIterator, false, priorOutput);
    if (priorOutput == null) {
      final ParseSourceInfo sourceInfo = new ParseSourceInfo(domNode.getTextContent(), true, isHtml, false, null, null, null);
      result.setParseSourceInfo(sourceInfo);
    }
    return result;
  }

  protected ParseOutputCollector parseInput(InputContextIterator inputContextIterator, boolean broaden, ParseOutputCollector result) {

    if (override != null) {
      for (String cpId : override) {
        final String[] ids = cpId.split(":");
        // compoundParserId:atnParserId[:broaden]+
        this.flow = new String[]{ids[1]};

        if (verbose) System.out.println("override " + cpId);

        result = parseConfig.parse(inputContextIterator, ids[0], this.flow, result);

        boolean needsReset = true;
        for (int i = 2; i < ids.length; ++i) {
          if ("broaden".equalsIgnoreCase(ids[i])) {
            inputContextIterator = inputContextIterator.broaden();
            needsReset = false;
            if (inputContextIterator == null) {
              // all done (versus [todo: parameterize] continue with unbroadened context?)
              break;
            }
          }
        }

        if (inputContextIterator == null) {
          break;
        }
        else if (needsReset) {
          inputContextIterator.reset();
        }
      }
    }
    else {
      if (this.compoundParserId != null) {
        result = parseConfig.parse(inputContextIterator, this.compoundParserId, this.flow, result);
      }
      else {
        final String[] compoundParserIds = parseConfig.getCompoundParserIds();
        for (String compoundParserId : compoundParserIds) {
          result = parseConfig.parse(inputContextIterator, compoundParserId, this.flow, result);

          boolean needsReset = true;
          if (broaden) {
            final InputContextIterator broadenedContextIterator = inputContextIterator.broaden();
            if (broadenedContextIterator != null) {
              inputContextIterator = broadenedContextIterator;
              needsReset = false;
            }
            // else, continue with the original inputContextIterator (versus [todo: parameterize] break out of loop?)
          }
          if (needsReset) {
            inputContextIterator.reset();
          }
        }
      }
    }

    return result;
  }

  private String writeModifiedDocument(DomDocument domDocument) throws IOException {
    final String outputFilename = buildOutputFilename(domDocument);

    System.out.println("\nWriting output to '" + outputFilename + "'...");

    ParseOutputCollector.pruneNodes(domDocument);

    final BufferedWriter writer = FileUtil.getWriter(outputFilename);
    XmlLite.writeXml(domDocument.getDocumentDomElement().asTree(), writer);
    writer.close();

    return outputFilename;
  }

  private String buildOutputFilename(DomDocument domDocument) {

    final Tree<XmlLite.Data> domTree = domDocument.getDocumentDomElement().asTree();
    final File inputFile = (File)domTree.getAttributes().get(XmlFactory.XML_FILENAME_ATTRIBUTE);

    final String inputFilename =  (inputFile != null) ? inputFile.getAbsolutePath() : "output.html";

    return FileUtil.buildOutputFilename(inputFilename, ".html");
  }


  public static void main(String[] args) throws IOException {

    //
    // Properties
    //
    //   parseConfig -- (required) path to data properties (config) file (xml)
    //   resourcesDir -- (required) path to resources (e.g. "${HOME}/co/ancestry/resources")
    //
    //   inputLines -- path to input file whose lines are to be parsed
    //   inputHtml -- path to input html file to parse
    //   diffHtml -- path to html file to use as mask for inputHtml
    //   
    //   verbose -- (optional, default=true)
    //   compoundParserId -- (optional) id of single compound parser from parse config to execute
    //   flow -- (optional) "id1,id2,..."
    //   showMarkup -- (optional, default=false)
    //   writeMarkup -- (optional, default=true)
    //   showInterpretations -- (optional, default=true)
    //   showOnlyInterpreted -- (optional, default=false)
    // 
    //   override -- (optional)compoundParserId1:atnParserId1[:broaden]+,compoundParserId2:atnParserId2[:broaden]+,...
    //
    //   showOnlySelected -- (optional, default=true)
    //   outputXml -- (optional) path to which xml output is to be written
    //   dumpGroups -- (optional, default=true) true to dump extraction groups instead of (raw/ungrouped) parse results
    //
    //   showResults -- (optional, default=true) true to show results on console
    //   briefResults -- (optional, default=true) true to show brief (instead of full) result output on console
    //   numberKeys -- (optional, default=true) true to show numbered group and extraction keys instead of xpaths
    //
    //   stage2cpid -- (optional) stage 2 compound parser id: causes extraction groups to be recycled as input through the compound parser's parsers.
    //

    final DataProperties dataProperties = new DataProperties(args);
    final AtnParseRunner runner = new AtnParseRunner(dataProperties);
    runner.run();
  }
}
