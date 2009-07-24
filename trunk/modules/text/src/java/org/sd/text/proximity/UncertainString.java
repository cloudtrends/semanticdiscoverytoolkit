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


/**
 * Container for a string that has an associated probability.
 * <p>
 * @author Spence Koehler
 */
public class UncertainString {

  private String text;
  private Double certainty;

  /**
   * Construct with the given text and unspecified certainty.
   */
  public UncertainString(String text) {
    this(text, null);
  }

  /**
   * Construct with the given text and certainty.
   */
  public UncertainString(String text, Double certainty) {
    this.text = text;
    this.certainty = certainty;
  }

  /**
   * Get the text.
   */
  public String getText() {
    return text;
  }

  /**
   * Get the certainty (null if unspecified).
   */
  public Double getCertainty() {
    return certainty;
  }

  /**
   * Set the text.
   */
  public void setText(String text) {
    this.text = text;
  }

  /**
   * Set the certainty.
   */
  public void setCertainty(Double certainty) {
    this.certainty = certainty;
  }
}
