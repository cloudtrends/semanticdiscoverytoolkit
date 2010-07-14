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
 * Context wrapper around input.
 * <p>
 * @author Spence Koehler
 */
public interface InputContext {
  
  /**
   * Get this context's text.
   */
  public String getText();

  /**
   * Get an ID that identifies this InputContext within its container.
   */
  public int getId();

  /**
   * Get the character startPosition of the other context's text within
   * this context or return false if the other context is not contained
   * within this context.
   *
   * @param other  The other input context
   * @param startPosition a single element array holding the return value
   *        of the start position -- only set when returning 'true'.
   *
   * @result true and startPosition[0] holds the value or false.
   */
  public boolean getPosition(InputContext other, int[] startPosition);

  /**
   * Get this context's ultimate container or itself if it is not contained
   * by anything.
   */
  public InputContext getContextRoot();

}
