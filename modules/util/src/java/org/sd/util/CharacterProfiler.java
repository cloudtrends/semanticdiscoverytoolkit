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
 * Utility to profile characters in strings in terms of unicode blocks.
 * <p>
 * @author Spence Koehler
 */
public class CharacterProfiler {

  private int numAdds;
  private int numEmptyAdds;
  private Histogram<Character.UnicodeBlock> ubHistogram;  // unicode block histogram
  private Histogram<Integer> ncHistogram;  // non-character histogram


  /**
   * Default constructor.
   */
  public CharacterProfiler() {
    this.numAdds = 0;
    this.numEmptyAdds = 0;
    this.ubHistogram = new Histogram<Character.UnicodeBlock>();
    this.ncHistogram = new Histogram<Integer>();
  }

  /**
   * Add the characters from the given string to this instance.
   */
  public void add(String string) {
    ++numAdds;

    if (string != null && !"".equals(string)) {
      final int len = string.length();
      for (int i = 0; i < len; ++i) {
        final int cp = string.codePointAt(i);
        if (isNonChar(cp)) {
          ncHistogram.add(cp);
        }
        else {
          ubHistogram.add(Character.UnicodeBlock.of(cp));
        }
      }
    }
    else {
      ++numEmptyAdds;
    }
  }

  /**
   * Get the number of strings added, including empty adds.
   */
  public int getNumAdds() {
    return numAdds;
  }

  /**
   * Get the number of empty or null strings added.
   */
  public int getNumEmptyAdds() {
    return numEmptyAdds;
  }

  /**
   * Get the unicode profile of all characters, not including "non-characters".
   */
  public Histogram<Character.UnicodeBlock> getUnicodeProfile() {
    return ubHistogram;
  }

  /**
   * Get the profile of all non-characters, where a non-character is either a
   * control character or a non-valid code point. See Character.isValidCodePoint.
   */
  public Histogram<Integer> getNonCharacterProfile() {
    return ncHistogram;
  }

  /**
   * Determine whether a codepoint represents a non-character.
   * <p>
   * Here, codepoints [0, 32), (126, 160), and !Character.isValidCodePoint(cp)
   * are considered to be non-characters.
   */
  protected boolean isNonChar(int cp) {
    // invalid if it is an ascii "control" character or is not a valid unicode code point
    return
      cp < 32 ||
      (cp < 160 && cp > 126) ||
      !Character.isValidCodePoint(cp);
  }
}
