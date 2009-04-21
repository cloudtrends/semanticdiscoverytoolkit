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
package org.sd.util.property;


/**
 * Property definition for a named property.
 * <p>
 * @author Spence Koehler
 */
public class NamedPropertyDefinition extends AbstractPropertyDefinition {

  private boolean repeats;
  private boolean optional;
  private String label;

  /**
   * The label for the property.
   * <p>
   * Interpret the last char as follows if present:
   * <p> '+' identifies a required property that repeats (1 or more)
   * <p> '*' identifies an optional property that repeats (0 or more)
   * <p> '?' identifies an optional property that doesn't repeat (0 or 1)
   *
   * @param label  The name of the property being defined with an optional
   *               special char postpended on repeating labels.
   */
  public NamedPropertyDefinition(String label) {
    super(label);

    final int len = label.length();
    final char c = label.charAt(len - 1);
    if (c == '+' || c == '*' || c == '?') {
      this.label = label.substring(0, len - 1);
      this.repeats = (c != '?');  // '+' and '*' repeat
      this.optional = (c != '+');  // '?' and '*' are optional
    }
    else {
      this.repeats = false;
      this.optional = false;
      this.label = label;
    }
  }

  /**
   * Get this definition's label (without its 'repeat' cue).
   * <p>
   * The label will be the name (without the 'repeat' cue).
   *
   * @return this definition's label.
   */
  public String getLabel() {
    return label;
  }

  /**
   * Test whether this definition is an 'named'.
   */
  public boolean isNamed() {
    return true;
  }

  /**
   * Get this defintion as a 'named' if it is one.
   *
   * @return this definition as a 'named' or null.
   */
  public NamedPropertyDefinition asNamed() {
    return this;
  }

  /**
   * Test whether this named property definition repeats.
   *
   * @return true if this repeats; otherwise, false.
   */
  public boolean repeats() {
    return repeats;
  }

  /**
   * Test whether this named property definition is optional.
   *
   * @return true if this is optional; otherwise, false.
   */
  public boolean isOptional() {
    return optional;
  }
}
