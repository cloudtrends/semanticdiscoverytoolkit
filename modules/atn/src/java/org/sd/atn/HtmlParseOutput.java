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


import java.io.Writer;
import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.sd.io.FileUtil;
import org.sd.util.InputContext;
import org.sd.util.MathUtil;
import org.sd.atn.extract.Extraction;
import org.sd.util.MathUtil;
import org.sd.util.tree.Tree;
import org.sd.util.tree.Tree2Dot;
import org.sd.xml.DomContext;

/**
 * Utility class for building html parse output.
 * <p>
 * @author Spence Koehler
 */
public class HtmlParseOutput {
  
  private File tempDir;
  private Set<String> extractionTypes;
  private boolean filterInterps;
  private StringBuilder output;

  public HtmlParseOutput() {
    this(null, null, false);
  }

  public HtmlParseOutput(File tempDir, String extractionTypes, boolean filterInterps) {
    this.tempDir = tempDir;

    this.extractionTypes = null;

    if (extractionTypes != null && !"".equals(extractionTypes.trim())) {
      this.extractionTypes = new HashSet<String>();

      final String[] types = extractionTypes.split(",");
      for (String type : types) this.extractionTypes.add(type);
    }

    this.filterInterps = filterInterps;
    this.output = new StringBuilder();
  }

  public void addExtractionGroups(ExtractionGroups extractionGroups, boolean briefResults) {
    if (extractionGroups == null) return;


    if (briefResults) {
      output.append("<table border=\"1\" cellpadding=\"1\" cellspacing=\"1\" style=\"font-size: 80%;\">\n");
      output.append("  <tr><th>group</th><th>text</th><th>interp</th><th>label</th><th>conf</th><th>path</th></tr>\n");
      extractionGroups.visitExtractions(
        null, true,
        new ExtractionGroups.ExtractionVisitor() {
          ExtractionContainer.ExtractionData priorExtraction = null;
          public boolean visitExtractionGroup(String source, int groupNum, String groupKey, ExtractionGroup group) {
            return true;
          }
          public void visitInterpretation(String source, int groupNum, String groupKey, ExtractionContainer.ExtractionData theExtraction,
                                          int interpNum, ParseInterpretation theInterpretation, String extractionKey) {
            if (addBriefExtraction(groupNum, groupKey, theExtraction, interpNum, theInterpretation, extractionKey, priorExtraction)) {
              priorExtraction = theExtraction;
            }
          }
        });
      output.append("</table>\n");
    }
    else {
      for (ExtractionGroup extractionGroup : extractionGroups.getExtractionGroups()) {
        openExtractionGroup(extractionGroup);
        for (ExtractionContainer extractionContainer : extractionGroup.getExtractions()) {
          addExtractionContainer(extractionContainer);
        }
        closeExtractionGroup();
      }
    }

  }

  public String getOutput() {
    final StringBuilder result = new StringBuilder();

    result.
      append("<div>\n").
      append(output).
      append("</div>\n");

    return result.toString();
  }


  private final void openExtractionGroup(ExtractionGroup extractionGroup) {
    output.
      append("<div>\n").
      append("  <div>").append(extractionGroup.getKey()).append("  </div>\n").
      append("  <table border=\"1\">\n");

  }

  private final void closeExtractionGroup() {
    output.
      append("  </table>\n").
      append("</div>\n");
  }

  private final boolean addBriefExtraction(int groupNum, String groupKey, ExtractionContainer.ExtractionData theExtraction,
                                           int interpNum, ParseInterpretation theInterpretation, String extractionKey,
                                           ExtractionContainer.ExtractionData priorExtraction) {

    if (filterInterps && interpNum > 0) return false;
    if (extractionTypes != null) {
      final String extractionType = theExtraction.getExtraction().getType();
      if (!extractionTypes.contains(extractionType)) return false;
    }

    // ignore consecutive duplicates
    if (priorExtraction != null) {
      final Tree<String> priorParseTree = priorExtraction.getParseTree();
      final Tree<String> curParseTree = theExtraction.getParseTree();
      if (curParseTree.equals(priorParseTree)) return false;
    }


    final String parsedText = theExtraction.getParsedText();

    output.
      append("<tr>\n").
      append("<td>").append(groupKey).append("</td>\n").
      append("<td>").append(getParsedTextHtml(theExtraction)).append("</td>\n").
      append("<td>").append(interpNum).append("</td>\n").
      append("<td>").append(theInterpretation.getClassification()).append("</td>\n").
      append("<td>").append(MathUtil.doubleString(theInterpretation.getConfidence(), 6)).append("</td>\n").
      append("<td>").append(extractionKey).append("</td>\n").
      append("</tr>\n");

    return true;
  }

  private String getParsedTextHtml(ExtractionContainer.ExtractionData theExtraction) {
    if (theExtraction == null) return "???";

    final String parsedText = theExtraction.getParsedText();

    final StringBuilder result = new StringBuilder();

    if (tempDir != null) {

      final Tree2Dot<String> tree2dot = new Tree2Dot<String>(theExtraction.getParseTree());
      tree2dot.setNodeAttribute("fontsize", "8");

      File dotFile = null;
      File dotPng = null;

      try {
        dotFile = File.createTempFile("parse.", ".dot", tempDir);
        final Writer writer = FileUtil.getWriter(dotFile);
        tree2dot.writeDot(writer);
        writer.close();
      }
      catch (IOException e) {
        System.err.println(new Date() + ": WARNING: Unable to convert parseTree to dotFile '" + dotFile + "'!");
        e.printStackTrace(System.err);

        dotFile = null;
      }
      
      if (dotFile != null) {
        // <a href='webex/genParseGraph.jsp?dotFile=tmp/....dot' target='parseTree'>parsedText</a>
        result.
          append("<a href='genParseGraph.jsp?dotFile=").
          append(dotFile.getName()).
          append("' target='parseTree'>").
          append(parsedText).
          append("</a>");

        dotFile.deleteOnExit();
      }
    }

    if (result.length() == 0) {
      result.append(parsedText);
    }

    return result.toString();
  }

  private final void addExtractionContainer(ExtractionContainer extractionContainer) {
    final int curOutputLength = output.length();

    openParseResult(extractionContainer.getGlobalStartPosition());
    
    boolean addedOne = false;

    final ParseInterpretation theInterpretation = extractionContainer.getTheInterpretation();
    if (theInterpretation != null) {
      addedOne = addExtractionData(extractionContainer.getTheExtraction(), extractionContainer.getTheInterpretation());
    }
    else {
      for (ExtractionContainer.ExtractionData extractionData : extractionContainer.getExtractions()) {
        addedOne |= addExtractionData(extractionData, null);
      }
    }

    if (!addedOne) {
      output.setLength(curOutputLength);
    }
    else {
      closeParseResult();
    }
  }

  private final void openParseResult(int globalStartPos) {
    output.append("<tr>\n");
  }

  private final void closeParseResult() {
    output.append("</tr>\n");
  }

  private final boolean addExtractionData(ExtractionContainer.ExtractionData extractionData, ParseInterpretation theInterpretation) {
    if (extractionData == null) return false;

    openParse(true, extractionData.getParseNum());

    addParseContext(extractionData.getParsedText(),
                    extractionData.getStartPos(), extractionData.getEndPos(),
                    extractionData.getLength(), extractionData.getContext().getInputContext());
    addParseExtraction(extractionData.getExtraction());

    output.append("<td><table border=\"1\">\n");  // open parseInterpretations

    if (theInterpretation != null) {
      output.append('\n');
      addParseInterpretation(theInterpretation, 1);
    }
    else if (extractionData.getInterpretations() != null && extractionData.getInterpretations().size() > 0) {
      output.append('\n');
      int interpNum = 1;
      for (ParseInterpretation interpretation : extractionData.getInterpretations()) {
        addParseInterpretation(interpretation, interpNum++);
      }
    }

    output.append("</table></td>\n");  // close parseInterpretations

    closeParse();

    return true;
  }

  private final void addParseContext(String parsedText, int startIndex, int endIndex, int length, InputContext inputContext) {

    output.append("<td>").append(parsedText).append("</td>\n");
    output.append("<td>").append(startIndex).append("</td>\n");
    output.append("<td>").append(endIndex).append("</td>\n");
    output.append("<td>").append(length).append("</td>\n");

    String key = "";
    if (inputContext != null && inputContext instanceof DomContext) {
      final DomContext domContext = (DomContext)inputContext;
      key = domContext.getIndexedPathString();
    }

    output.append("<td>").append(key).append("</td>\n");
  }

  private final void addParseExtraction(Extraction extraction) {
    if (extraction == null)  return;

    output.append("<td><table>\n");

    doAddParseExtraction(extraction);

    output.append("</table></td>\n");
  }

  private final void doAddParseExtraction(Extraction extraction) {
    if (extraction == null) return;

    output.
      append("<tr><td align=\"top\">").append(extraction.getType()).append(':').append("</td>\n").
      append("<td>\n");

    if (extraction.hasFields()) {
      output.append("<table>\n");

      for (List<Extraction> fieldExtractions : extraction.getFields().values()) {
        if (fieldExtractions != null) {
          for (Extraction fieldExtraction : fieldExtractions) {
            doAddParseExtraction(fieldExtraction);
          }
        }
      }

      output.append("</table>\n");
    }
    else {
      output.append(extraction.getText());
    }

    output.append("</td>\n");
  }

  private final void addParseInterpretation(ParseInterpretation interpretation, int interpNum) {

    output.append("<tr><td>").append(interpretation.getClassification()).append("</td>\n");

    final double confidence = interpretation.getConfidence();
    final String cString = MathUtil.doubleString(confidence, 6);
    output.append("<td>").append(cString).append("</td>\n");

    output.append("<td>").append(interpretation.getToStringOverride()).append("</td>\n");

    output.append("</tr>\n");
  }

  private final void openParse(boolean isSelected, int parseNum) {
    output.append("<td>");
    if (isSelected) output.append("*");
    output.append("</td><td>").append(parseNum).append("</td>\n");
  }

  private final void closeParse() {
  }
}
