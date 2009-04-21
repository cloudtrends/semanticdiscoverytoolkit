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
package org.sd.extract;


import org.sd.nlp.Parser;
import org.sd.util.LineBuilder;
import org.sd.util.tree.Tree;
import org.sd.xml.XmlLite;
import org.sd.xml.XmlTreeHelper;

import java.io.IOException;
import java.util.regex.Matcher;

/**
 * A default implementation of the interpretation interface.
 * <p>
 * This version simply dumps the extraction data as fields based on
 * type. For more intelligent manipulation of the extraction data
 * content, create your own interpretation!
 *
 * @author Spence Koehler
 */
public class DefaultInterpretation implements Interpretation {
  
  private Extraction extraction;
  private LineBuilder lineBuilder;
  private String structureKey;
  private String string;

  public DefaultInterpretation() {
    this.extraction = null;
    this.lineBuilder = new LineBuilder();
    this.structureKey = null;
    this.string = null;
  }

  public DefaultInterpretation(Extraction extraction) {
    this();

    setExtraction(extraction);
  }

  /**
   * Get the extraction that contains this interpretation.
   */
  public Extraction getExtraction() {  //backpointer.
    return extraction;
  }

  /**
   * Set or update the extraction backpointer.
   */
  public final void setExtraction(Extraction extraction) {
    this.extraction = extraction;

    final ExtractionData data = extraction.getData();
    if (data != null) {
      this.string = data.getExtractedString();
      if (data.getString() != null) {  // string extraction
        final ExtractionStringData sdata = data.asStringData();
        // field: extractedString

        if (sdata.wasBuiltByLineBuilder()) {
          lineBuilder.appendBuilt(data.getString());
        }
        else {
          lineBuilder.append(data.getString());
        }
        structureKey = "string";
      }
      else if (data.getParses() != null) {
        // fields: extractedParses (as parse trees)
        for (Parser.Parse parse : data.getParses()) {  // parse extraction
          if (structureKey == null) structureKey = parse.getParseKey();
          lineBuilder.append(parse.toString());
        }
      }
      else if (data.getMatcher() != null) {  // regex extraction
        // fields: match groups 0 through N
        final Matcher matcher = data.getMatcher();
        for (int i = 0; i <= matcher.groupCount(); ++i) {
          lineBuilder.append(matcher.group(i));
        }
        structureKey = matcher.pattern().toString();
      }
      else if (data.getStrings() != null) {  // strings extraction
        final ExtractionStringsData sdata = data.asStringsData();

        // fields: extractedStrings
        for (String string : data.getStrings()) {
          if (sdata.wasBuiltByLineBuilder()) {
            lineBuilder.appendBuilt(string);
          }
          else {
            lineBuilder.append(string);
          }
        }
        structureKey = "strings";
      }
      else if (data.getXmlTree() != null) {  // xml extraction
        // fields: xmlNode | allNodeText
        final Tree<XmlLite.Data> node = data.getXmlTree();
        try {
          lineBuilder.append(XmlLite.asXml(node, false).replaceAll("\\s+", " "));
        }
        catch (IOException e) {
          lineBuilder.append(e.toString());
        }
        lineBuilder.append(XmlTreeHelper.getAllText(node));
        structureKey = "xml";  //todo: build a string representation of the the xml tags, excluding the text.
      }
    }
  }

  /**
   * Get this interpretation's data as a pipe-delimited string of fields
   * specific to the type and implementation.
   * <p>
   * unnormalizedTelephoneNumber|telHeadingPos|faxHeadingPos|headingContext
   */
  public String getFieldsString() {
    return lineBuilder.toString();
  }

  /**
   * Get a key identifying the structure (not content) of this interpretation.
   */
  public String getStructureKey() {
    return structureKey;
  }

  /**
   * Get an unambiguous string representing this interpretation's content.
   */
  public String asString() {
    return string;
  }

  public String toString() {
    return getFieldsString();
  }
}
