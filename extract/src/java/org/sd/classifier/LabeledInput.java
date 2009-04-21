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
package org.sd.classifier;


import org.sd.extract.TextContainer;
import org.sd.util.LineBuilder;

/**
 * Immutable container for a label and an input.
 * <p>
 * @author Spence Koehler
 */
public class LabeledInput {

  public final String label;
  public final String source;
  public final TextContainer input;

  /**
   * Construct an instance with the given label, source, and input.
   *
   * @param label  The label (value) for the classification feature.
   * @param source  The source of the label.
   * @param input  The input having the classification (label).
   */
  public LabeledInput(String label, String source, TextContainer input) {
    this.label = label;
    this.source = source;
    this.input = input;
  }

  /**
   * Get a line representing the source of this input.
   * <p>
   * Currently, the line is of the form: number|source|label|containerName
   */
  public String getSourceLine(int number) {
    final LineBuilder result = new LineBuilder();

    result.
      append(number).
      append(source).
      append(label).
      append(input.getName());

    return result.toString();
  }
}
