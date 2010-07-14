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
package org.sd.util;


/**
 * Simple context wrapper around a string.
 * <p>
 * @author Spence Koehler
 */
public class StringInputContext implements InputContext {
  
  private String text;
  private int id;

  public StringInputContext(String text) {
    this(text, 0);
  }

  public StringInputContext(String text, int id) {
    this.text = text;
    this.id = id;
  }


  /**
   * Get this context's text.
   */
  public String getText() {
    return text;
  }

  /**
   * Get an ID that identifies this InputContext within its container.
   */
  public int getId() {
    return id;
  }

  /**
   * Get the character startPosition of the other context's text within
   * this context or return false if the other context is not contained
   * within this context.
   * <p>
   * This implementation searches for the first matching text of the other
   * context within this context's string.
   *
   * @param other  The other input context
   * @param startPosition a single element array holding the return value
   *        of the start position -- only set when returning 'true'.
   *
   * @result true and startPosition[0] holds the value or false.
   */
  public boolean getPosition(InputContext other, int[] startPosition) {
    boolean result = false;

    final int pos = this.text.indexOf(other.getText());
    if (pos >= 0) {
      startPosition[0] = pos;
      result = true;
    }

    return result;
  }

  /**
   * Get this context's ultimate container or itself if it is not contained
   * by anything.
   */
  public InputContext getContextRoot() {
    return this;
  }
}
