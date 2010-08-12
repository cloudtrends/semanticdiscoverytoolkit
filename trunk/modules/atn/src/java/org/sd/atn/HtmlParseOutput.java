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


import java.util.List;
import org.sd.util.InputContext;
import org.sd.util.MathUtil;
import org.sd.atn.extract.Extraction;
import org.sd.xml.DomContext;

/**
 * Utility class for building html parse output.
 * <p>
 * @author Spence Koehler
 */
public class HtmlParseOutput {
  
  private StringBuilder output;

  public HtmlParseOutput() {
    this.output = new StringBuilder();
  }

  public void addExtractionGroups(ExtractionGroups extractionGroups, boolean briefResults) {
    if (extractionGroups == null) return;


    if (briefResults) {
      final List<String> briefExtractions = extractionGroups.collectBriefExtractions(null, true);
      output.append("<table border=\"1\" cellpadding=\"1\" cellspacing=\"1\" style=\"font-size: 80%;\">\n");
      output.append("  <tr><th>group</th><th>text</th><th>interp</th><th>label</th><th>conf</th><th>path</th></tr>\n");
      for (String briefExtraction : briefExtractions) {
        addBriefExtraction(briefExtraction);
      }
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

  private final void addBriefExtraction(String briefExtraction) {
    final String[] fields = briefExtraction.split("\t");

    output.append("<tr>\n");

    for (String field : fields) {
      output.append("<td>").append(field).append("</td>\n");
    }

    output.append("</tr>\n");
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
