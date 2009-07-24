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
 * Utility to keep a window of characters.
 * <p>
 * @author Spence Koehler
 */
public class WindowString {

  private StringBuilder builder;
  private int windowSize;
  private int windowCenter;
  private int centerPos;
  private boolean rolled;
  private boolean truncated;

  /**
   * Construct an new instance.
   *
   * @param windowSize  The maximum number of characters to keep.
   */
  public WindowString(int windowSize) {
    this.builder = new StringBuilder();
    this.windowSize = windowSize;
    this.windowCenter = windowSize >> 1;  // half the window size.
    this.centerPos = -1;
    this.rolled = false;
    this.truncated = false;
  }

  /**
   * Retrieve whether the window has rolled (lopped off front chars).
   */
  public boolean rolled() {
    return rolled;
  }

  /**
   * Retrieve whether the window has truncated (lopped off end chars).
   */
  public boolean truncated() {
    return truncated;
  }

  /**
   * Get the current window's text.
   */
  public String toString() {
    return builder.toString();
  }

  /**
   * Get the current window's text with an ellipsis before and/or after if
   * the window has rolled or truncated.
   */
  public String asString() {
    final StringBuilder result = new StringBuilder();

    if (rolled) result.append("...");
    result.append(builder);
    if (truncated) result.append("...");

    return result.toString();
  }

  /**
   * Get the current length of the string in this window.
   */
  public int length() {
    return builder.length();
  }

  /**
   * Append the given string to this instance.
   * <p>
   * If a centerPos has not been set, then keep adding content,
   * rolling the window to drop earlier chars in favor of keeping
   * later chars.
   * <p>
   * If a centerPos has been set and no more chars will fit in the
   * window after adding what could be added from string, then
   * this string is full and no more content will be added.
   *
   * @return false, if no more content will be added to this window.
   */
  public boolean append(String string) {

    if (windowSize <= 0) {
      builder.append(string);
      return true;
    }

    boolean hitHardEnd = false;

    if (centerPos < 0) {
      // add all of the chars and clip from the front any extras (roll)
      builder.append(string);
      if (builder.length() > windowSize) {
        builder.delete(0, builder.length() - windowSize);
        rolled = true;
      }
    }
    else {
      int numCharsToAdd = string.length();

      if (centerPos <= windowCenter) {
        // add enough chars to fill the window, then stop
        final int newLen = builder.length() + numCharsToAdd;
        hitHardEnd = newLen >= windowSize;
        if (newLen > windowSize) {
          numCharsToAdd = windowSize - builder.length();
          hitHardEnd = true;
          string = string.substring(0, numCharsToAdd);
          truncated = true;
        }

        builder.append(string);
      }
      else {
        // add enough chars to roll centerPos back to windowCenter, then stop
        final int rollNeeded = centerPos - windowCenter;
        final int rollThisTime = builder.length() + numCharsToAdd - windowSize;

        if (rollThisTime > 0) {
          hitHardEnd = (rollNeeded <= rollThisTime);
          if (rollNeeded < rollThisTime) {
            numCharsToAdd -= (rollThisTime - rollNeeded);
            string = string.substring(0, numCharsToAdd);
            truncated = true;
          }
        }

        if (numCharsToAdd > 0) {
          int shift = 0;
          builder.append(string);
          if (builder.length() > windowSize) {
            shift = builder.length() - windowSize;
            builder.delete(0, shift);
            rolled = true;
          }
          centerPos -= shift;  // moved back with roll
        }
      }
    }

    return !hitHardEnd;
  }

  /**
   * Append the given string to this instance, setting the centerPos
   * to be at the identified position within the given string (not
   * this window's contents).
   *
   * @param string     The string to append to this window.
   * @param centerPos  The position in 'string' that is to be centered
   *                   in this window.
   *
   * @return false, if no more content will be added to this window.
   */
  public boolean append(String string, int centerPos) {
    if (centerPos < string.length()) {
      this.centerPos = builder.length() + centerPos;
    }
    return append(string);
  }

  /**
   * Append the given string to this instance, setting the centerPos
   * to be at the identified position within the given string (not
   * this window's contents).
   *
   * @param string     The string to append to this window.
   * @param centerPos  The position in 'string' that is to be centered
   *                   in this window. In this case, centerPos[0] will
   *                   be interpreted as a character position in string
   *                   and centerPos[1] will be interpreted as the length
   *                   of a substring in string. The actual center will
   *                   be at the center of said substring.
   *
   * @return false, if no more content will be added to this window.
   */
  public boolean append(String string, int[] centerPos) {
    return append(string, centerPos[0] + centerPos.length / 2);
  }


  /**
   * Clear out this instance to re-use or start over.
   */
  public void reset() {
    builder.setLength(0);
    this.centerPos = -1;
  }
}
