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


import java.util.List;
import org.sd.util.InputContext;
import org.sd.util.MathUtil;
import org.sd.atn.extract.Extraction;
import org.sd.util.tree.Tree;
import org.sd.xml.DomContext;
import org.sd.xml.DomNode;
import org.sd.xml.DomUtil;
import org.sd.xml.XmlLite;

/**
 * Utility class for building xml parse output.
 * <p>
 * @author Spence Koehler
 */
public class XmlParseOutput {

  private StringBuilder output;

  private ParseSourceInfo parseSourceInfo;
  private int initialIndentLevel;
  private int indentSpaces;

  public XmlParseOutput(ParseSourceInfo parseSourceInfo, int initialIndentLevel, int indentSpaces) {
    this.output = new StringBuilder();

    this.parseSourceInfo = parseSourceInfo;
    this.initialIndentLevel = initialIndentLevel;
    this.indentSpaces = indentSpaces;
  }

  public void addParseResult(AtnParseResult parseResult, boolean onlySelected, boolean onlyInterpreted) {
    final int curOutputLength = output.length();

    openParseResult(-1, initialIndentLevel + 2);  //todo: compute globalStartPos if/when necessary

    //NOTE: multiple parses represent structural ambiguity
    boolean addedParse = false;
    final int numParses = parseResult.getNumParses();
    for (int i = 0; i < numParses; ++i) {
      final AtnParse parse = parseResult.getParse(i);
      addedParse |= addParse(parse, initialIndentLevel + 3, onlySelected, onlyInterpreted);
    }

    if (!addedParse) {
      output.setLength(curOutputLength);
    }
    else {
      closeParseResult(initialIndentLevel + 2);
    }
  }

  public void addExtractionGroups(ExtractionGroups extractionGroups) {

    final int groupIndentLevel = initialIndentLevel + 2;
    final int extractionIndentLevel = groupIndentLevel + 1;

    for (ExtractionGroup extractionGroup : extractionGroups.getExtractionGroups()) {
      openExtractionGroup(extractionGroup, groupIndentLevel);

      for (ExtractionContainer extractionContainer : extractionGroup.getExtractions()) {
        addExtractionContainer(extractionContainer, extractionIndentLevel);
      }

      closeExtractionGroup(groupIndentLevel);
    }
  }

  private final void openExtractionGroup(ExtractionGroup extractionGroup, int indentLevel) {
    int indent = indentLevel * indentSpaces;
    addIndent(output, indent);
    output.append("<extractionGroup>\n");

    indent += indentSpaces;
    addIndent(output, indent);
    output.append("<groupContext>\n");

    indent += indentSpaces;

    boolean gotXml = false;

    final DomNode inputNode = extractionGroup.getInputNode();
    if (inputNode != null) {
      final String inputXml = DomUtil.getSubtreeXml(inputNode);

      if (inputXml != null) {
        gotXml = true;

        addIndent(output, indent);
        output.
          append("<xml depth='").
          append(inputNode.getDepth()).
          append("'>\n").
          append(inputXml).
          append('\n');

        addIndent(output, indent);
        output.append("</xml>\n");
      }
    }

    if (!gotXml) {
      final String inputText = extractionGroup.getInputText();
      if (inputText != null) {
        addIndent(output, indent);
        output.append("<text>").append(inputText).append("</text>\n");
      }
    }

    addIndent(output, indent);
    output.append("<key>").append(extractionGroup.getKey()).append("</key>\n");

    indent -= indentSpaces;
    addIndent(output, indent);
    output.append("</groupContext>\n");
  }

  private final void closeExtractionGroup(int indentLevel) { 
    final int indent = indentLevel * indentSpaces;
    addIndent(output, indent);
    output.append("</extractionGroup>\n");
  }

  public void addExtractionContainer(ExtractionContainer extractionContainer, int indentLevel) {
    final int curOutputLength = output.length();

    openParseResult(extractionContainer.getGlobalStartPosition(), indentLevel);
    
    boolean addedOne = false;

    final ParseInterpretation theInterpretation = extractionContainer.getTheInterpretation();
    if (theInterpretation != null) {
      addedOne = addExtractionData(extractionContainer.getTheExtraction(), extractionContainer.getTheInterpretation(), indentLevel + 1);
    }
    else {
      for (ExtractionContainer.ExtractionData extractionData : extractionContainer.getExtractions()) {
        addedOne |= addExtractionData(extractionData, null, indentLevel + 1);
      }
    }

    if (!addedOne) {
      output.setLength(curOutputLength);
    }
    else {
      closeParseResult(indentLevel);
    }
  }

  public String getOutput(boolean includeXmlHeader) {
    final StringBuilder result = new StringBuilder();

    if (includeXmlHeader) {
      result.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
    }

    int indent = initialIndentLevel * indentSpaces;
    addIndent(result, indent);
    result.append("<output>\n");
    indent += indentSpaces;

    addIndent(result, indent);
    result.append("<results>\n");

    addSourceXmlString(result, initialIndentLevel + 2);
    result.append('\n');

    result.append(output);

    addIndent(result, indent);
    result.append("</results>\n");

    indent -= indentSpaces;
    addIndent(result, indent);
    result.append("</output>\n");

    return result.toString();
  }

  private final void addSourceXmlString(StringBuilder result, int indentLevel) {
    if (parseSourceInfo != null && parseSourceInfo.getInputString() != null) {
      int indent = indentLevel * indentSpaces;
      addIndent(result, indent);
      result.append("<source>\n");

      final String sourceType = parseSourceInfo.getSourceType();
      indent += indentSpaces;
      addIndent(result, indent);
      result.
        append('<').append(sourceType).append('>').
        append(parseSourceInfo.getInputString()).
        append("</").append(sourceType).append(">\n");

      final String url = parseSourceInfo.getUrl();
      if (url != null) {
        addIndent(result, indent);
        result.
          append("<url>").
          append(url).
          append("</url>\n");
      }

      final String diffType = parseSourceInfo.getSourceDiffType();
      if (diffType != null) {
        addIndent(result, indent);
        result.
          append('<').append(sourceType).append('>').
          append(parseSourceInfo.getDiffString()).
          append("</").append(diffType).append(">\n");
      }

      final String diffUrl = parseSourceInfo.getDiffUrl();
      if (diffUrl != null) {
        addIndent(result, indent);
        result.
          append("<diffUrl>").
          append(diffUrl).
          append("</diffUrl>\n");
      }

      indent -= indentSpaces;
      addIndent(result, indent);
      result.append("</source>\n");
    }
  }

  private final void openParseResult(int globalStartPos, int indentLevel) {
    int indent = indentLevel * indentSpaces;
    addIndent(output, indent);
    output.append("<parseResult>\n");

    if (globalStartPos >= 0) {
      indent += indentSpaces;
      addIndent(output, indent);
      output.append("<context>\n");

      indent += indentSpaces;
      addIndent(output, indent);
      output.
        append("<globalStartPos>").
        append(globalStartPos).
        append("</globalStartPos>\n");

      indent -= indentSpaces;
      addIndent(output, indent);
      output.append("</context>\n");
    }
  }

  private final void closeParseResult(int indentLevel) {
    final int indent = indentLevel * indentSpaces;
    addIndent(output, indent);
    output.append("</parseResult>\n");
  }

  private final void openParse(int indentLevel, boolean isSelected, int parseNum) {
    final int indent = indentLevel * indentSpaces;
    addIndent(output, indent);
    output.append("<parse selected='").append(isSelected).append("' num='").append(parseNum).append("'>\n");
  }

  private final void closeParse(int indentLevel) {
    final int indent = indentLevel * indentSpaces;
    addIndent(output, indent);
    output.append("</parse>\n");
  }

  private final boolean addParse(AtnParse parse, int indentLevel, boolean onlySelected, boolean onlyInterpreted) {

    final boolean isSelected = parse.getSelected();
    if (onlySelected && !isSelected) return false;

    final List<ParseInterpretation> interpretations = parse.getParseInterpretations();
    if (onlyInterpreted && (interpretations == null || interpretations.size() == 0)) return false;

    openParse(indentLevel++, isSelected, parse.getParseNum());

    addParseContext(indentLevel, parse.getParsedText(),
                    parse.getStartIndex(), parse.getEndIndex(),
                    parse.getFullTextLength(), parse.getInputContext());
    addParseExtraction(parse.getExtraction(), indentLevel);

    //NOTE: multiple interpretations represent semantic ambiguity
    if (interpretations != null) {
      output.append('\n');
      for (ParseInterpretation interpretation : interpretations) {
        addParseInterpretation(interpretation, indentLevel);
      }
    }

    closeParse(--indentLevel);

    return true;
  }

  private final boolean addExtractionData(ExtractionContainer.ExtractionData extractionData, ParseInterpretation theInterpretation, int indentLevel) {
    if (extractionData == null) return false;

    openParse(indentLevel++, true, extractionData.getParseNum());

    addParseContext(indentLevel, extractionData.getParsedText(),
                    extractionData.getStartPos(), extractionData.getEndPos(),
                    extractionData.getLength(), extractionData.getContext().getInputContext());
    addParseExtraction(extractionData.getExtraction(), indentLevel);

    if (theInterpretation != null) {
      output.append('\n');
      addParseInterpretation(theInterpretation, indentLevel);
    }
    else if (extractionData.getInterpretations() != null && extractionData.getInterpretations().size() > 0) {
      output.append('\n');
      for (ParseInterpretation interpretation : extractionData.getInterpretations()) {
        addParseInterpretation(interpretation, indentLevel);
      }
    }

    closeParse(--indentLevel);

    return true;
  }

  private final void addParseContext(int indentLevel, String parsedText, int startIndex, int endIndex, int length, InputContext inputContext) {
    int indent = indentLevel * indentSpaces;
    addIndent(output, indent);
    output.append("<context>\n");
    indent += indentSpaces;

    addIndent(output, indent);
    output.append("<string>").append(parsedText).append("</string>\n");

    addIndent(output, indent);
    output.append("<startPos>").append(startIndex).append("</startPos>\n");

    addIndent(output, indent);
    output.append("<endPos>").append(endIndex).append("</endPos>\n");
    
    addIndent(output, indent);
    output.append("<length>").append(length).append("</length>\n");

    if (inputContext != null && inputContext instanceof DomContext) {
      final DomContext domContext = (DomContext)inputContext;
      addIndent(output, indent);
      output.append("<xpath>").append(domContext.getIndexedPathString()).append("</xpath>\n");
    }

    indent -= indentSpaces;
    addIndent(output, indent);
    output.append("</context>\n");
  }

  private final void addParseExtraction(Extraction extraction, int indentLevel) {
    if (extraction == null)  return;

    final int indent = indentLevel * indentSpaces;
    addIndent(output, indent);
    output.append("<extraction>\n");

    doAddParseExtraction(extraction, indentLevel + 1);

    addIndent(output, indent);
    output.append("</extraction>\n");
  }

  private final void doAddParseExtraction(Extraction extraction, int indentLevel) {
    if (extraction == null) return;

    final int indent = indentLevel * indentSpaces;
    addIndent(output, indent);
    output.append('<').append(extraction.getType()).append('>');
    boolean doAddIndent = true;

    if (extraction.hasFields()) {
      output.append('\n');

      ++indentLevel;
      for (List<Extraction> fieldExtractions : extraction.getFields().values()) {
        if (fieldExtractions != null) {
          for (Extraction fieldExtraction : fieldExtractions) {
            doAddParseExtraction(fieldExtraction, indentLevel);
          }
        }
      }
    }
    else {
      output.append(extraction.getText());
      doAddIndent = false;
    }

    if (doAddIndent) addIndent(output, indent);
    output.append("</").append(extraction.getType()).append(">\n");
  }

  private final void addParseInterpretation(ParseInterpretation interpretation, int indentLevel) {
    if (interpretation.getInterpTree() == null) {
      addParseInterpretationNoTree(interpretation, indentLevel);
    }
    else {
      addParseInterpretationTree(interpretation, indentLevel);
    }
  }

  private final void addParseInterpretationNoTree(ParseInterpretation interpretation, int indentLevel) {
    int indent = indentLevel * indentSpaces;
    addIndent(output, indent);
    output.append("<interpretation>\n");
    indent += indentSpaces;

    addIndent(output, indent);
    output.append("<classification>").append(interpretation.getClassification()).append("</classification>\n");

    final double confidence = interpretation.getConfidence();
    final String cString = MathUtil.doubleString(confidence, 6);
    addIndent(output, indent);
    output.append("<confidence>").append(cString).append("</confidence>\n");

    addIndent(output, indent);
    output.append("<value>").append(interpretation.getToStringOverride()).append("</value>\n");

    indent -= indentSpaces;
    addIndent(output, indent);
    output.append("</interpretation>\n");
  }

  private final void addParseInterpretationTree(ParseInterpretation interpretation, int indentLevel) {
    int indent = indentLevel * indentSpaces;
    addIndent(output, indent);
    output.append("<interpretation>\n");

    final Tree<XmlLite.Data> interpTree = interpretation.getInterpTree();
    addXmlTree(interpTree, indentLevel + 1);

    addIndent(output, indent);
    output.append("</interpretation>\n");
  }

  private final void addXmlTree(Tree<XmlLite.Data> xmlTree, int indentLevel) {

    final XmlLite.Tag tag = xmlTree.getData().asTag();
    if (tag != null) {
      final int indent = indentLevel * indentSpaces;
      if (output.charAt(output.length() - 1) != '\n') output.append('\n');
      addIndent(output, indent);
      output.append(tag.toString());

      if (tag.isSelfTerminating()) output.append("\n");

      // recurse on children
      if (xmlTree.hasChildren()) {
        for (Tree<XmlLite.Data> child : xmlTree.getChildren()) {
          addXmlTree(child, indentLevel + 1);
        }
      }


      // close tag
      if (!tag.isSelfTerminating()) {
        if (output.charAt(output.length() - 1) == '\n') addIndent(output, indent);
        output.append("</").append(tag.name).append(">\n");
      }
    }
    else {
      final XmlLite.Text text = xmlTree.getData().asText();
      if (text != null) {
        output.append(text.text);
      }
    }

  }

  private final void addIndent(StringBuilder result, int indent) {
    for (int i = 0; i < indent; ++i) result.append(' ');
  }
}
