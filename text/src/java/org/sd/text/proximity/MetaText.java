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
package org.sd.text.proximity;


import java.util.HashMap;
import java.util.Map;

/**
 * Container for text with its meta information.
 * <p>
 * @author Spence Koehler
 */
public class MetaText {

  private String text;                            // substring text
  private Integer docPos;                         // full string's position in document
  private Integer subPos;                         // substring position in full string
  private Integer headingWeight;                  // heading weight
  private Map<String, Double> heading2certainty;  // possible types of headings

  public MetaText(String text) {
    this(text, null, null, null);
  }

  public MetaText(String text, Integer docPos, Integer subPos, Integer headingWeight) {
    this.text = text;
    this.docPos = docPos;
    this.subPos = subPos;
    this.headingWeight = headingWeight;

    this.heading2certainty = null;
  }

  public String getText() {
    return text;
  }

  public Integer getDocPos() {
    return docPos;
  }

  public Integer getSubPos() {
    return subPos;
  }

  public Integer getHeadingWeight() {
    return headingWeight;
  }

  public void setText(String text) {
    this.text = text;
  }

  public void setDocPos(Integer docPos) {
    this.docPos = docPos;
  }

  public void setSubPos(Integer subPos) {
    this.subPos = subPos;
  }

  public void setHeadingWeight(Integer headingWeight) {
    this.headingWeight = headingWeight;
  }

  public void addHeadingType(String headingString, Double certainty) {
    if (heading2certainty == null) {
      heading2certainty = new HashMap<String, Double>();
    }
    heading2certainty.put(headingString, certainty);
  }

  public void addHeadingType(UncertainString headingType) {
    addHeadingType(headingType.getText(), headingType.getCertainty());
  }

  public Double hasHeadingType(String headingString) {
    Double result = null;

    if (heading2certainty != null) {
      result = heading2certainty.get(headingString);
    }

    return result;
  }
}
