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
package org.sd.token;


/**
 * Class whose instances represent different kinds of breaks.
 *
 * Definitions:
 *
 * - Hard -vs- Soft Breaks:
 * - A Hard Break represents a break byeond which no more characters should
 *   be added to a token upon a lengthening revision.
 * - A Soft Break represents a potential break for a token that could be
 *   revised over the soft break for a longer token.
 *
 * - Single -vs- Zero Width Breaks:
 * - A Single-Width break indicates that the character at the break is not
 *   a part of the token.
 * - A Zero-Width break indicates that the break is "invisible" between
 *   characters such that a character at the break can be considered as a
 *   non-breaking character in a token.
 * <p>
 * @author Spence Koehler
 */
public class Break {
  
  /**
   * Represents no break at the current character.
   */
  public static final Break NO_BREAK = new Break(BreakType.NONE, 1, "NONE", "NO_BREAK");

  /**
   * Represents a hard break at the current character.
   * 
   * Examples would include a punctuation character at the end of a sentence.
   */
  public static final Break SINGLE_WIDTH_HARD_BREAK = new Break(BreakType.HARD, 1, "SW_HARD", "SINGLE_WIDTH_HARD_BREAK");

  /**
   * Represents a soft break at the current character.
   * 
   * Examples would include a hyphen between hyphenated words.
   */
  public static final Break SINGLE_WIDTH_SOFT_BREAK = new Break(BreakType.SOFT, 1, "SW_SOFT", "SINGLE_WIDTH_SOFT_BREAK");

  /**
   * Represents a soft break between the prior and current characters.
   * 
   * Examples would include uppercased characters in camelcased words.
   */
  public static final Break ZERO_WIDTH_SOFT_BREAK = new Break(BreakType.SOFT, 0, "ZW_SOFT", "ZERO_WIDTH_SOFT_BREAK");

  /**
   * Represents a hard break between the prior and current characters.
   * 
   * Included here for completeness and might apply if, for example, camel-
   * cased words are not to be considered as a full token but only always
   * as multiple tokens.
   */
  public static final Break ZERO_WIDTH_HARD_BREAK = new Break(BreakType.HARD, 0, "ZW_HARD", "ZERO_WIDTH_HARD_BREAK");


  /**
   * The type of this break.
   */
  private BreakType bType;
  BreakType getBType() {
    return bType;
  }

  /**
   * The width of this break (either 1 or 0).
   */
  private int bWidth;
  public int getBWidth() {
    return bWidth;
  }

  /**
   * The name of this break.
   */
  private String bName;
  public String getBName() {
    return bName;
  }

  /**
   * The long name of this break
   */
  private String bLongName;
  public String getBLongName() {
    return bLongName;
  }

  /**
   * Determine whether this instance represents any kind of break.
   */
  public boolean breaks() {
    return bType != BreakType.NONE;
  }

  /**
   * Determine whether this instance represents a hard break.
   */
  public boolean isHard() {
    return bType == BreakType.HARD;
  }

  /**
   * Determine whether this instance represents a soft break.
   */
  public boolean isSoft() {
    return bType == BreakType.SOFT;
  }


  /**
   * Private constructor access to enforce flyweight pattern.
   */
  private Break(BreakType breakType, int breakWidth, String breakName, String breakLongName) {
    this.bType = breakType;
    this.bWidth = breakWidth;
    this.bName = breakName;
    this.bLongName = breakLongName;
  }

  /**
   * Determine whether this break agrees with the other break type.
   * 
   * A hard break agrees with another hard break.
   * A soft break agrees with a hard break or a soft break.
   * A non-break agrees with another non-break.
   */
  boolean agreesWith(BreakType otherBreakType) {
    boolean result = (this.bType == otherBreakType);

    if (!result && this.bType == BreakType.HARD && otherBreakType == BreakType.SOFT) {
      result = true;
    }

    return result;
  }


  public String toString() {
    return bName;
  }
}
