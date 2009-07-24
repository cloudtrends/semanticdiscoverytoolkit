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

import org.sd.util.tree.Tree;
import org.sd.xml.XmlLite;
import org.sd.xml.XmlTreeHelper;
import org.sd.util.LineBuilder;

/**
 * Container for extraction information for a DocText.
 * <p>
 * @author Spence Koehler
 */
public class Extraction {

//todo: make this Publishable if necessary.

  private String extractionType;
  private DocText docText;
  private double weight;
  private ExtractionData data;
  private long groupId;

  private Interpretation interpretation;  // disambiguated interpretation
  private HeadingOrganizer.HeadingStack headingStack;
  private boolean extrapolated;

  /**
   * Default constructor.
   */
  public Extraction() {
    this(null, null, 0.0, null);
  }

  /**
   * Construct with the given values.
   */
  public Extraction(String extractionType, DocText docText, double weight) {
    this(extractionType, docText, weight, null);
  }

  /**
   * Construct with the given values.
   */
  public Extraction(String extractionType, DocText docText, double weight, ExtractionData data) {
    this.extractionType = extractionType;
    this.docText = docText;
    this.weight = weight;
    this.data = data;

    this.interpretation = null;  // use setter
    this.headingStack = null;    // use setter
    this.extrapolated = false;   // user setter

    if (docText != null) {
      docText.setExtraction(this);
    }
  }

  /**
   * Get the doc text's xml node's full text string.
   */
  public String getText() {
    String result = null;

    if (docText != null) {
      final Tree<XmlLite.Data> xmlNode = docText.getXmlNode();
      if (xmlNode != null) {
        result = XmlTreeHelper.getAllText(xmlNode);
      }
    }

    return result;
  }

  /**
   * Get the extraction type.
   */
  public String getExtractionType() {
    return extractionType;
  }

  /**
   * Set the extraction type.
   */
  public void setExtractionType(String extractionType) {
    this.extractionType = extractionType;
  }

  /**
   * Get the docText.
   */
  public DocText getDocText() {
    return docText;
  }

  /**
   * Set the docText.
   */
  public void setDocText(DocText docText) {
    if (this.docText != null) {
      //todo: remove this extraction from its existing docText?
    }

    this.docText = docText;

    if (docText != null) {
      // add a reference to this extraction in the docText
      docText.setExtraction(this);
    }
  }

  /**
   * Set the property on the docText's xml node.
   */
  public void setProperty(String property, Object value) {
    if (docText != null) {
      final Tree<XmlLite.Data> xmlNode = docText.getXmlNode();
      if (xmlNode != null) {
        xmlNode.getData().setProperty(property, value);
      }
    }
  }

  /**
   * Get the weight.
   */
  public double getWeight() {
    return weight;
  }

  /**
   * Set the weight.
   */
  public void setWeight(double weight) {
    this.weight = weight;
  }

  /**
   * Get the data.
   */
  public ExtractionData getData() {
    return data;
  }

  /**
   * Set the extraction data.
   */
  public void setData(ExtractionData data) {
    this.data = data;
  }

  /**
   * Get the doc text's path key.
   */
  public String getPathKey() {
    String result =  null;

    if (docText != null) {
      result = docText.getPathKey();
    }

    return result;
  }

  /**
   * Get the parse key for the extraction if it exists, or null.
   */
  public String getStructureKey() {
    String result = null;

    if (interpretation != null) {
      result = interpretation.getStructureKey();
    }

    return result;
  }

  /**
   * Get the path index of this extraction's docText (or -1).
   */
  public int getPathIndex() {
    int result = -1;

    if (docText != null) {
      result = docText.getPathIndex();
    }

    return result;
  }

  /**
   * Set this instance's disambiguated interpretation.
   */
  public void setInterpretation(Interpretation interpretation) {
    this.interpretation = interpretation;
  }

  /**
   * Get this instance's disambiguated interpretation.
   */
  public Interpretation getInterpretation() {
    return interpretation;
  }

  /**
   * Set this extraction's heading stack.
   */
  public void setHeadingStack(HeadingOrganizer.HeadingStack headingStack) {
    this.headingStack = headingStack;
  }

  /**
   * Get this extraction's heading stack (if it has been set).
   */
  public HeadingOrganizer.HeadingStack getHeadingStack() {
    return headingStack;
  }

  /**
   * Set this extraction's 'extrapolated' flag.
   */
  public void setExtrapolated(boolean extrapolated) {
    this.extrapolated = extrapolated;
  }

  /**
   * Get this extractor's 'extrapolated' flag.
   */
  public boolean isExtrapolated() {
    return extrapolated;
  }

  /**
   * Get this interpretation's data as a pipe-delimited string of fields
   * specific to the type and implementation.
   */
  public String getFieldsString() {
    return getFieldsString(false, false);
  }

  /**
   * Get this interpretation's data as a pipe-delimited string of fields
   * specific to the type and implementation.
   */
  public String getFieldsString(boolean includeExtractionType, boolean includeData) {
    final LineBuilder result = new LineBuilder();

    //NOTE: If the fields change, update org.sd.postal.CciExtraction to match.
    //      and update BASE in ContextCciAddress,
    //      and fix test cases in, i.e. TestUkAddressFieldPostProcessor, etc.

    if (includeExtractionType) result.append(extractionType);

    result.
      append(getPathKey()).
      append(getPathIndex()).
      append(extrapolated ? "secondary" : "primary").
      append(weight+"").
      append(limit(getText(), 2048));

    if (headingStack != null) {
      result.append(headingStack.getHeadingsString());
    }
    else {
      result.append("");
    }

    if (interpretation != null) {
      result.appendBuilt(interpretation.getFieldsString());
    }

    if (includeData && data != null) {
      result.append(data.toString());
    }

    return result.toString();
  }

  public void setGroupId(long groupId) {
    this.groupId = groupId;
  }

  public long getGroupId() {
    return groupId;
  }

  /**
   * extractionType
   * pathIndex
   * groupId
   * pathKey
   * fixedPathKey
   * 0="primary"/1="extrapolated"
   * weight
   * extraction text (up to 2048 chars)
   * data
   * interpretation's fieldsString
   */
  public String getFieldsString2() {
    final LineBuilder result = new LineBuilder();

    result.append(extractionType);

    final String pathKey = getPathKey();

    result.
      append(getPathIndex()).
      append(getGroupId()).
      append(pathKey).
      append(ExtractionUtil.fixPathKey(pathKey)).
      append(extrapolated ? "1" : "0").
      append(weight+"").
      append(limit(getText(), 2048));

    if (data != null) {
      result.append(data.toString());
    }
    else {
      result.append("");
    }

    if (interpretation != null) {
      result.appendBuilt(interpretation.getFieldsString());
    }
    else {
      result.append("");
    }

    return result.toString();
  }

  public String toString() {
    final StringBuilder result = new StringBuilder();

    result.append(extractionType);

    if (docText != null) {
      result.append('[').append(docText).append(']');
    }

    if (weight > 0.0) {
      result.append('(').append(weight).append(')');
    }

    if (data != null) {
      result.append('=').append(data.toString());
    }

    return result.toString();
  }

  /**
   * Get an unambiguous string representing the extracted content.
   */
  public String asString() {
    String result = null;

    if (interpretation != null) {
      result = interpretation.asString();
    }

    if (result == null && data != null) {
      result = data.getExtractedString();
    }

    if (result == null && docText != null) {
      result = docText.getString();
    }

    return result;
  }

  private final String limit(String text, int maxLen) {
    final int len = text.length();
    if (len > maxLen) {
      text = text.substring(0, maxLen - 3) + "...";
    }
    return text;
  }
}
