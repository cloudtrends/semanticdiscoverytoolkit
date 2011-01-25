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

import java.io.IOException;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * General-purpose string utilities.
 * <p>
 * @author Spence Koehler, Ryan McGuire, Dave Barney
 */
public class StringUtil {

  static MessageDigest md;
  
  static {
    try {
      md = MessageDigest.getInstance("MD5");
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public static final String getMD5Sum(String s) {
    if (s == null || s.length() == 0) return null;
    md.reset();
    return toHexString(md.digest(s.getBytes()));
  }

  public static final String buildString(int[] codePoints) {
    final StringBuilder result = new StringBuilder();

    for (int cp : codePoints) {
      result.appendCodePoint(cp);
    }

    return result.toString();
  }

  public static final String toHexString(byte[] array) {
    StringBuffer sb = new StringBuffer();
    for (int i = 0; i < array.length; ++i) {
      sb.append(Integer.toHexString((array[i] & 0xFF) | 0x100).toUpperCase().substring(1,3));
    }
    return sb.toString().toLowerCase();
  }
  
  /**
   * Count the number of spaces in a string.
   * <p>
   * Note that for a string normalized to one space between words,
   * this is one less than the number of words in the string.
   */
  public static final int countSpaces(String string) {
    int result = 0;
    for (int spos = string.indexOf(' '); spos >= 0; spos = string.indexOf(' ', spos + 1)) {
      ++result;
    }
    return result;
  }

  /**
   * Count the number of occurrences of the character in the string.
   */
  public static final int countChars(String string, char c) {
    int result = 0;

    int ppos = string.indexOf(c);
    while (ppos >= 0) {
      ++result;
      ppos = string.indexOf(c, ppos + 1);
    }

    return result;
  }

  /**
   * Count the number of occurrences of char types in a string: 0=LETTERS, 1=DIGITS, 2=WHITE, 3=OTHER, 4=NUM_UPPER
   *
   * @return the counts of types of chars.
   */
  public static final int[] countCharTypes(String string) {
    final int[] result = new int[5];

    for (StringIterator iter = new StringIterator(string); iter.hasNext(); ) {
      StringPointer pointer = iter.next();
      if (isWhite(pointer.codePoint)) ++result[2];
      else if (Character.isLetter(pointer.codePoint)) {
        ++result[0];
        if (Character.isUpperCase(pointer.codePoint)) ++result[4];
      }
      else if (Character.isDigit(pointer.codePoint)) ++result[1];
      else ++result[3];
    }    

    return result;
  }

  /**
   * Flatten a string by removing vowels and all but last in consecutive
   * consonant sequences.
   */
  public static final String flatten(String string) {
    final int len = string.length();
    return flatten(string, 0, len);
  }

  /**
   * Flatten the string from the startPos (inclusive) to the endPos (exclusive).
   */
  public static final String flatten(String string, int startPos, int endPos) {

    final StringBuilder result = new StringBuilder();

    char lastConsonant = 0;
    for (int i = startPos; i < endPos; ++i) {
      final char c = string.charAt(i);

      if (i == startPos) {
        result.append(c);
      }
      else {
        boolean isVowel = (c == 'a' || c == 'e' || c == 'i' || c == 'o' || c == 'u' || c == 'y');

        if (isVowel || c == ' ') {
          if (lastConsonant != 0) {
            result.append(lastConsonant);
            lastConsonant = 0;
          }
        }
        else {  // it is a consonant
          lastConsonant = c;
        }
      }
    }
    if (lastConsonant != 0) result.append(lastConsonant);

    return result.toString();
  }

  /**
   * Find the first letter or digit in the string at or after startPos.
   */
  public static final int firstLetterOrDigitPos(String string, int startPos) {
    final int len = string.length();
    for (; startPos < len; ++startPos) {
      final char c = string.charAt(startPos);
      if (Character.isLetterOrDigit(c)) break;
    }

    return startPos;
  }

  /**
   * Build a string concatenating the pieces from startIndex (inclusive) to endIndex
   * (exclusive), adding the delim between each.
   */
  public static final String concat(String[] pieces, String delim, int startIndex, int endIndex) {
    final StringBuilder result = new StringBuilder();

    for (int i = startIndex; i < endIndex; ++i) {
      if (result.length() > 0) result.append(delim);
      result.append(pieces[i]);
    }

    return result.toString();
  }

  /**
   * Build a string concatenating the pieces, adding the delim between each.
   */
  public static final String concat(Collection<String> pieces, String delim) {
    return concat(pieces, delim, 0, pieces.size());
  }

  /**
   * Build a string concatenating the pieces from startIndex (inclusive) to endIndex
   * (exclusive), adding the delim between each.
   */
  public static final String concat(Collection<String> pieces, String delim, int startIndex, int endIndex) {
    final StringBuilder result = new StringBuilder();

    int index = 0;
    for (Iterator<String> iter = pieces.iterator(); iter.hasNext(); ) {
      final String piece = iter.next();
      if (index >= endIndex) break;
      if (index < startIndex) continue;

      if (result.length() > 0) result.append(delim);
      result.append(piece);
    }

    return result.toString();
  }

  /**
   * Find the first position in superString where the beginning of subString
   * and the beginning of a word in superString match at least minMatchLength
   * characters.
   * <p>
   * For example findPrefixMatch("corpinfo", "read our corporate information", 4) would be 9.
   *
   * @return the match position or -1
   */
  public static final int findPrefixMatch(String subString, String superString, int minMatchLength) {
    if (subString.length() < minMatchLength) return -1;
    if (superString.length() < minMatchLength) return -1;
    final String ssubString = subString.substring(0, minMatchLength).toLowerCase();

    superString = superString.toLowerCase();
    int pos = superString.indexOf(ssubString);
    for (; pos >= 0; pos = superString.indexOf(ssubString, pos + 1)) {
      if (pos == 0) break;  // found at beginning of string. done.
      final char c = superString.charAt(pos - 1);
      if (!Character.isLetterOrDigit(c)) break;
    }

    return pos;
  }

  /**
   * Utility to split a string on symbols that aren't between 2 chars,
   * removing extra whitespace in the process both on the ends and in
   * the middle.
   */
  public static final List<String> splitOnSymbols(String string) {
    final List<String> result = new ArrayList<String>();

    final StringBuilder curString = new StringBuilder();

    boolean sawWhite = false;
    boolean sawLetter = false;
    int lastCharPos = -1;

    for (StringContext context = new StringContext(1, string); context.hasNext(); ) {
      final int codePoint = context.next();
      final int curPos = context.getPosition();
      boolean addChar = false;
      
      if (!Character.isLetterOrDigit(codePoint)) {
        if (isWhite(codePoint)) {
          sawWhite = true;
        }
        else {  // this is a break unless there is a letter before and after
          if (lastCharPos == curPos - 1 && context.hasNext() && Character.isLetterOrDigit(context.getNextCodePoint(1))) {
            // not a break. we're between 2 letters.
            addChar = true;
          }
          else {  // is a break. add the current piece.
            if (curString.length() > 0) {
              result.add(curString.toString());
              curString.setLength(0);
            }
          }
        }
      }
      else {
        addChar = true;
      }

      if (addChar) {
        if (sawWhite && curString.length() > 0) {
          curString.append(' ');
        }
        sawWhite = false;
        curString.appendCodePoint(codePoint);
        lastCharPos = curPos;
      }
    }

    // don't forget to add the last piece.
    if (curString.length() > 0) {
      result.add(curString.toString());
    }

    return result;
  }

  /**
   * Utility to split a string on vertical bars into at least minNum pieces.
   * <p>
   * Each fields contents will be trimmed and the result will contain at
   * least minNum pieces (with extras if any the empty string, not null).
   */
  public static final String[] splitFields(String fieldsString, int minNum) {
    final String[] pieces = fieldsString.split("\\s*\\|\\s*");
    return ensureNumFields(pieces, minNum);
  }

  public static final String[] ensureNumFields(String[] pieces, int minNum) {
    if (pieces != null && pieces.length >= minNum) return pieces;

    final int len = (pieces == null) ? 0 : pieces.length;
    final String[] result = new String[minNum];

    for (int i = 0; i < len; ++i) result[i] = pieces[i];
    for (int i = len; i < result.length; ++i) result[i] = "";

    return result;
  }

  public static final boolean isWhite(int codePoint) {
    return Character.isWhitespace(codePoint) || Character.isSpaceChar(codePoint);
  }

  /**
   * Convert non alpha's to %hex and spaces to '+'.
   */
  public static final String urlQueryEscape(String string) {
    final StringBuilder result = new StringBuilder();

    boolean sawWhite = false;
    final StringBuilder addChar = new StringBuilder();

    for (StringIterator iter = new StringIterator(string); iter.hasNext(); ) {
      final StringPointer pointer = iter.next();
      final int cp = pointer.codePoint;

      if (isWhite(cp)) {
        sawWhite = true;
      }
      else if (Character.isLetterOrDigit(cp) || cp == '.') {
        addChar.appendCodePoint(cp);
      }
      else {
        addChar.append('%').append(Integer.toHexString(cp));
      }

      if (addChar.length() > 0) {
        if (sawWhite & result.length() > 0) result.append('+');
        sawWhite = false;
        result.append(addChar);
        addChar.setLength(0);
      }
    }

    return result.toString();
  }

  /**
   * Utility to split a string on a specified substring into an ArrayList
   * <p>
   * This wraps String.split to return an ArrayList instead of a String[] array.
   * @return an ArrayList containing the string parts
   */
  public static final ArrayList<String> split(String string, String seperator){
    return new ArrayList<String>(Arrays.asList(string.split(seperator)));
  }

  public static final boolean hasAsianChar(String string) {
    if (string == null) return false;
    for (StringIterator iter = new StringIterator(string); iter.hasNext(); ) {
      StringPointer pointer = iter.next();
      if (isAsianCodePoint(pointer.codePoint)) return true;
    }    
    return false;
  }

  public static final boolean isAsianCodePoint(int cp) {

    // from http://www.unicode.org/Public/UNIDATA/Blocks.txt, asian/ideographic unicode ranges are:
    // 2E80..2EFF; CJK Radicals Supplement
    // 2F00..2FDF; Kangxi Radicals
    // 2FF0..2FFF; Ideographic Description Characters
    // 3000..303F; CJK Symbols and Punctuation
    // 3040..309F; Hiragana
    // 30A0..30FF; Katakana
    // 3100..312F; Bopomofo
    // 3130..318F; Hangul Compatibility Jamo
    // 3190..319F; Kanbun
    // 31A0..31BF; Bopomofo Extended
    // 31C0..31EF; CJK Strokes
    // 31F0..31FF; Katakana Phonetic Extensions
    // 3200..32FF; Enclosed CJK Letters and Months
    // 3300..33FF; CJK Compatibility
    // 3400..4DBF; CJK Unified Ideographs Extension A
    // 4DC0..4DFF; Yijing Hexagram Symbols
    // 4E00..9FFF; CJK Unified Ideographs
    // A000..A48F; Yi Syllables
    // A490..A4CF; Yi Radicals
    // A4D0..A4FF; Lisu
    // F900..FAFF; CJK Compatibility Ideographs
    // FE30..FE4F; CJK Compatibility Forms
    // 1F200..1F2FF; Enclosed Ideographic Supplement
    // 20000..2A6DF; CJK Unified Ideographs Extension B
    // 2A700..2B73F; CJK Unified Ideographs Extension C
    // 2F800..2FA1F; CJK Compatibility Ideographs Supplement
    // 
    // or, sorted, the ranges are:
    // 2E80..A4FF
    // F900..FAFF
    // FE30..FE4F
    // 1F200..1F2FF
    // 20000-2A6DF
    // 2A700-2B73F
    // 2F800..2FA1F

    if (cp < 0x2E80 || cp > 0x2FA1F) return false;
    if (cp <= 0xA4FF) return true;                      // 2E80-A4FF
    if (cp < 0x20000) {
      if (cp >= 0xF900 && cp <= 0xFAFF) return true;    // F900-FAFF
      if (cp >= 0xFE30 && cp <= 0xFE4F) return true;    // FE30-FE4F
      if (cp >= 0x1F200 && cp <= 0x1F2FF) return true;  // 1F200-1F2FF
    }
    else {
      if (cp < 0x2A6DF) return true;                    // 20000-2A6DF
      if (cp >= 0x2A700 && cp <= 0x2B73F) return true;  // 2A700-2B73F
      else if (cp >= 0x2F800) return true;              // 2F800-2FA1F
    }

    return false;
  }

  public static final boolean hasChineseChar(String string) {
    if (string == null) return false;
    for (StringIterator iter = new StringIterator(string); iter.hasNext(); ) {
      StringPointer pointer = iter.next();
      if (isChineseCodePoint(pointer.codePoint)) return true;
    }    
    return false;
  }

  public static final boolean isChineseCodePoint(int cp) {

    // from "http://en.wikipedia.org/wiki/CJK_Unified_Ideographs" (2009-11-16), unicode ranges are:
    // 4E00-9FFF (original)
    // 3400-4DBF (extension A: 1999)
    // 20000-2A6DF (extension B: 2001)
    // 2A700-2B73f (extension C: 2009)
    // F900-FAFF
    // 
    // or, sorted, the ranges are:
    // 3400-4DBF (extension A: 1999)
    // 4E00-9FFF (original)
    // F900-FAFF
    // 20000-2A6DF (extension B: 2001)
    // 2A700-2B73F (extension C: 2009)

    if (cp < 0x3400 || cp > 0x2B73f) return false;
    if (cp <= 0x4DBF) return true;                    // 3400-4DBF
    if (cp < 0x20000) {
      if (cp >= 0x4E00 && cp <= 0x9FFF) return true;  // 4E00-9FFF
      if (cp >= 0xF900 && cp <= 0xFAFF) return true;  // F900-FAFF
    }
    else {
      if (cp < 0x2A6DF) return true;        // 20000-2A6DF
      else if (cp >= 0x2A700) return true;  // 2F700-2B73F
    }

    return false;
  }

  /**
   * Collapses all strings of whitespace to a single space and trims the ends.
   * If the string is null or all white, an empty string will be returned.
   */
  public static final String trim(String string) {
    final StringBuilder result = new StringBuilder();

    if (string != null) {
      boolean sawNonWhite = false;
      boolean sawWhite = false;

      final int len = string.length();
      for (int i = 0; i < len; ++i) {
        final int codePoint = string.codePointAt(i);
        if (isWhite(codePoint)) {
          if (sawNonWhite) {
            sawWhite = true;
          }
        }
        else {
          sawNonWhite = true;
          if (sawWhite) {
            result.append(' ');
            sawWhite = false;
          }
          result.appendCodePoint(codePoint);
        }
      }
    }
        
    return result.toString();
  }

  /**
   * Remove leading and trailing space and store what was trimmed in a String array
   * containing the trimmed prefix, the trimmed string, and the trimmed suffix
   * If the string is null, an array containing three empty strings will be returned.
   */
  public static final String[] trimAndStore(String string) {
    String[] result = new String[3];

    StringBuilder prefix = new StringBuilder();
    StringBuilder trimmed = new StringBuilder();
    StringBuilder suffix = new StringBuilder();

    if (string != null) {
      boolean sawNonWhite = false;

      final int len = string.length();
      for (int i = 0; i < len; ++i) {
        final int codePoint = string.codePointAt(i);
        if (isWhite(codePoint)) {
          if (sawNonWhite) {
            suffix.appendCodePoint(codePoint);
          }
					else {
            prefix.appendCodePoint(codePoint);
					}
        }
        else {
          sawNonWhite = true;

          trimmed.append(suffix);
          trimmed.appendCodePoint(codePoint);
          suffix = new StringBuilder();
        }
      }
    }
        
    result[0] = prefix.toString();
    result[1] = trimmed.toString();
    result[2] = suffix.toString();
    return result;
  }

  public static final String squashNonLetterOrDigit(String string) {
    final StringBuilder result = new StringBuilder();

    for (StringIterator iter = new StringIterator(string); iter.hasNext(); ) {
      StringPointer pointer = iter.next();
      if (Character.isLetterOrDigit(pointer.codePoint)) result.appendCodePoint(pointer.codePoint);
    }    

    return result.toString();
  }

  public static final String wsNonLetterOrDigit(String string) {
    final StringBuilder result = new StringBuilder();

    boolean ws = false;
    for (StringIterator iter = new StringIterator(string); iter.hasNext(); ) {
      StringPointer pointer = iter.next();
      if (Character.isLetterOrDigit(pointer.codePoint)) {
        if (ws) result.append(' ');
        result.appendCodePoint(pointer.codePoint);
        ws = false;
      }
      else {
        ws = true;
      }
    }    

    return result.toString();
  }

  private static final CodePointBag DEFAULT_CHOMP_BAG = new CodePointBag() {
      public Response getResponse(int codePoint) {
        return Character.isLetterOrDigit(codePoint) ? Response.YES : Response.NO;
      }
    };

  public static final CodePointBag CHOMP_WHITE_BAG = new CodePointBag() {
      public Response getResponse(int codePoint) {
        return isWhite(codePoint) ? Response.NO : Response.YES;
      }
    };

  /**
   * Remove the first word (ws+letters+digits+ws) from the front of the string.
   *
   * @return an array of length 2 or null where result[0]=the first word (not null);
   *         result[1]=remainder of string (possibly null)
   */
  public static final String[] chomp(String string) {
    return chomp(string, DEFAULT_CHOMP_BAG);
  }

  /**
   * Remove the first word (letters+digits+stringDelims) from the front of the string.
   */
  public static final String[] chomp(String string, final char[] stringDelims) {
    final CodePointBag myBag = new CodePointBag() {
        public Response getResponse(int codePoint) {
          StringUtil.Response result = StringUtil.Response.NO;

          if (Character.isLetterOrDigit(codePoint)) {
            result = StringUtil.Response.YES;
          }
          else if (stringDelims != null) {
            for (char c : stringDelims) {
              if (codePoint == c) {
                result = StringUtil.Response.MAYBE;
                break;
              }
            }
          }

          return result;
        }
      };

    return chomp(string, myBag);
  }

  public static final String[] chomp(String string, CodePointBag bag) {
    StringPointer pointer = null;

    boolean skippedPreDelims = false;
    boolean skippedWord = false;
    boolean gotit = false;
    StringIterator iter = new StringIterator(string);
    int wordStart = -1;
    int wordEnd = string.length();

    while (iter.hasNext()) {
      pointer = iter.next();
      int codePoint = pointer.codePoint;
      Response response = bag.getResponse(codePoint);

      if (!skippedPreDelims) {
        if (response == Response.YES) {
          skippedPreDelims = true;
          wordStart = pointer.curPos;
        }
      }
      else if (!skippedWord) {
        if (response != Response.YES) {
          wordEnd = pointer.curPos;
          skippedWord = true;
        }
      }
      else {  // skip post delims
        if (response == Response.YES) {
          gotit = true;
          break;
        }
      }
    }

    String[] result = (wordStart < 0) ? null :
      new String[]{string.substring(wordStart, wordEnd),
                   (gotit) ? string.substring(pointer.curPos) : null};

    return result;
  }

  public static enum Response {YES, NO, MAYBE};

  public static interface CodePointBag {
    public Response getResponse(int codePoint);
  }

  /**
   * Trim no's and maybe's from the left (until hitting a yes).
   * Trim no's from the middle.
   * Keep maybe's in the middle.
   * Trim no's and maybe's from the right (after the last yes).
   */
  public static final String trim(String string, CodePointBag bag) {
    final StringBuilder result = new StringBuilder();
    final StringBuilder buffer = new StringBuilder();

    boolean didLeft = false;

    for (StringIterator iter = new StringIterator(string); iter.hasNext(); ) {
      StringPointer pointer = iter.next();
      int codePoint = pointer.codePoint;
      Response response = bag.getResponse(codePoint);

      if (!didLeft) {
        while (response != Response.YES && iter.hasNext()) {
          pointer = iter.next();
          codePoint = pointer.codePoint;
          response = bag.getResponse(codePoint);
        }
        if (response == Response.YES) result.appendCodePoint(codePoint);
        didLeft = true;
      }
      else {
        if (response == Response.YES) {
          result.append(buffer);
          buffer.setLength(0);
          result.appendCodePoint(codePoint);
        }
        else {
          if (response == Response.MAYBE) {
            buffer.appendCodePoint(codePoint);
          }
        }
      }
    }

    return result.toString();
  }

  /**
   * Determine whether the string is a sequence of digits.
   *
   * @param s         The string.
   *
   * @return true if the string is exclusively composed of digits.
   */
  public static final boolean isDigits(String s) {
    return isDigits(s, 0, s.length());
  }

  /**
   * Determine whether the string is a sequence of digits within the range.
   *
   * @param s         The string.
   * @param startPos  The index at which to start checking (inclusive).
   * @param endPos    The index at which to end checking (exclusive).
   *
   * @return true if the string is exclusively composed of digits in the range.
   */
  public static final boolean isDigits(String s, int startPos, int endPos) {
    boolean allDigits = true;
    final int len = s.length();
    for (int i = 0; i < len; ++i) {
      if (!Character.isDigit(s.charAt(i))) {
        allDigits = false;
        break;
      }
    }
    return allDigits;
  }

  /**
   * Determine whether the string is a single word with a digit.
   *
   * @param s         The string.
   *
   * @return true if the string is letters and digits with at least one digit.
   */
  public static final boolean wordWithDigit(String s) {
    return wordWithDigit(s, 0, s.length());
  }
  
  /**
   * Determine whether the string is a single word with a digit within the range.
   *
   * @param s         The string.
   * @param startPos  The index at which to start checking (inclusive).
   * @param endPos    The index at which to end checking (exclusive).
   *
   * @return true if the string is exclusively composed of letters and digits in the range.
   */
  public static final boolean wordWithDigit(String s, int startPos, int endPos) {
    boolean hasDigit = false;
    final int len = s.length();

    for (int i = 0; i < len; ++i) {
      final int c = s.codePointAt(i);
      if (c > 65535) ++i;
      if (Character.isDigit(c)) {
        hasDigit = true;
      }
      else if (!Character.isLetter(c)) {  // not a single word
        hasDigit = false;
        break;
      }
    }
    return hasDigit;
  }

  /**
   * Determine whether the string has a digit. (Can have non-white symbols.)
   *
   * @param s         The string.
   *
   * @return true if the string has digits.
   */
  public static final boolean stringWithDigit(String s) {
    return stringWithDigit(s, 0, s.length());
  }

  /**
   * Determine whether the string has a digit in the range. (Can have non-white symbols.)
   *
   * @param s         The string.
   * @param startPos  The index at which to start checking (inclusive).
   * @param endPos    The index at which to end checking (exclusive).
   *
   * @return true if the string has digits in the range.
   */
  public static final boolean stringWithDigit(String s, int startPos, int endPos) {
    boolean hasDigit = false;
    final int len = s.length();

    for (int i = 0; i < len; ++i) {
      final int c = s.codePointAt(i);
      if (c > 65535) ++i;
      if (Character.isDigit(c)) {
        hasDigit = true;
      }
      else if (Character.isWhitespace(c)) {  // not a single word
        hasDigit = false;
        break;
      }
    }
    return hasDigit;
  }

  public static final boolean capitalizedWord(String s) {
    return capitalizedWord(s, 0, s.length());
  }

  public static final boolean capitalizedWord(String s, int startPos, int endPos) {
    boolean capitalized = false;
    int c = s.codePointAt(startPos++);
    if (c > 65535) ++startPos;
    if (isCapital(c)) {
      capitalized = allLowerCase(s, startPos, endPos) && isWord(s, startPos, endPos);
    }
    return capitalized;
  }

  public static final boolean capitalizedWords(String s) {
    boolean capitalized = true;

    final int len = s.length();
    int wordPos = 0;

    while (wordPos < len) {
      final int cp = s.codePointAt(wordPos);
      if (Character.isUpperCase(cp) || Character.isDigit(cp)) {
        // have a capital where we expect it. need to find a lowercase before the next position.
        final int nextWordPos = nextWordPos(s, wordPos);
        boolean foundLower = false;
        for (int i = wordPos + 1; i < nextWordPos; ++i) {
          if (Character.isLowerCase(s.codePointAt(i))) {
            foundLower = true;
          }
        }
        if (!foundLower) {
          capitalized = false;
          break;
        }
        wordPos = nextWordPos;
      }
      else {
        capitalized = false;
        break;
      }
    }

    return capitalized;
  }

  public static final int nextWordPos(String s, int curWordPos) {
    final int len = s.length();
    int result = len;

    // look for a space, then spin beyond all non-chars.
    int endOfWord = s.indexOf(' ', curWordPos);
    if (endOfWord >= curWordPos) {
      // found a space, now spin to first char
      for (result = endOfWord + 1; result < len; ++result) {
        if (Character.isLetterOrDigit(s.codePointAt(result))) {
          break;
        }
      }
    }

    return result;
  }

  /**
   * Backup to the end of the prior word from the position (based on
   * space and non-letters/digits).
   *
   * @return the position after the word prior to (or equal to) curWordPos or -1
   *         if no letters or digits are found prior to curWordPos.
   */
  public static final int endOfPriorWordPos(String s, int curWordPos) {
    int result = curWordPos;

    for (; result - 1 >= 0; --result) {
      if (Character.isLetterOrDigit(s.codePointAt(result - 1))) {
        break;
      }
    }

    return result;
  }

  /**
   * Backup to the beginning of the prior word from the position (based on
   * space and non-letters/digits).
   */
  public static final int priorWordPos(String s, int curWordPos) {
    int result = endOfPriorWordPos(s, curWordPos);

    if (result > 0) {
      result = s.lastIndexOf(' ', result - 1);

      if (result >= 0) {
        result = nextWordPos(s, result);
      }
    }

    return result;
  }

  /**
   * Get the last word in the string without punctuation or whitespace.
   */
  public static final String getLastWord(String s) {
    String result = s;

    final int endOfLastWord = endOfPriorWordPos(s, s.length());
    if (endOfLastWord > 0) {
      final int beginningOfLastWord = priorWordPos(s, endOfLastWord);
      if (beginningOfLastWord >= 0) {
        result = s.substring(beginningOfLastWord, endOfLastWord);
      }
    }

    return result;
  }

  /**
   * Count the characters that are in capitalized words (or symbols or digits) as
   * opposed to those characters in non-capitalized words. Exclude spaces from counts.
   *
   * @return {numCapitailzedChars, numNonCapitalizedChars}
   */
  public static final int[] capitalizationRatio(String s) {
    int capitalCount = 0;
    int nonCapitalCount = 0;

    final String[] pieces = s.split("\\s+");
    for (String piece : pieces) {
      final int pieceLen = piece.length();
      if (pieceLen > 0) {
        final int cp = piece.codePointAt(0);
        final boolean isCapitalizedPiece = !Character.isLowerCase(cp);

        if (isCapitalizedPiece) {
          capitalCount += pieceLen;
        }
        else {
          nonCapitalCount += pieceLen;
        }
      }
    }

    return new int[]{capitalCount, nonCapitalCount};
  }

  public static final boolean allCaps(String s) {
    return allCaps(s, 0, s.length());
  }

  /**
   * @return true if all letters in the string are upper case and there is at least one letter.
   */
  public static final boolean allCaps(String s, int startPos, int endPos) {
    boolean allCaps = true;
    boolean foundOne = false;
    for (int i = startPos; i < endPos; ++i) {
      final int c = s.codePointAt(i);
      if (c > 65535) ++i;
      if (Character.isLetter(c)) {
        foundOne = true;
        if (!Character.isUpperCase(c)) {
          allCaps = false;
          break;
        }
      }
    }
    return allCaps && foundOne;
  }

  public static final boolean allLowerCase(String s) {
    return allLowerCase(s, 0, s.length());
  }

  /**
   * @return true if all letters in the string are lower case and there is at least one letter.
   */
  public static final boolean allLowerCase(String s, int startPos, int endPos) {
    boolean allLower = true;
    boolean foundOne = false;
    for (int i = startPos; i < endPos; ++i) {
      final int c = s.codePointAt(i);
      if (c > 65535) ++i;
      if (Character.isLetter(c)) {
        foundOne = true;
        if (!Character.isLowerCase(c)) {
          allLower = false;
          break;
        }
      }
    }
    return allLower && foundOne;
  }

  public static final boolean isWord(String s) {
    return isWord(s, 0, s.length());
  }

  public static final boolean isWord(String s, int startPos, int endPos) {
    boolean isWord = true;
    boolean foundOne = false;
    for (int i = startPos; i < endPos; ++i) {
      final int c = s.codePointAt(i);
      if (c > 65535) ++i;
      if (!Character.isLetter(c)) {
        isWord = false;
        break;
      }
      else {
        if (!foundOne) foundOne = true;
      }
    }
    return isWord && foundOne;
  }

  /**
   * Combine the string elements in the array to form a new string, composed of all the elements
   * where each element is separated by the delimiter string.  This method is the opposite of the 
   * String.split(String delimiter) command.  Null elements are not added to the string, nor do they appear delimited.  Empty strings, however
   * do appear between  two delimiters.
   * @param array          The array to join
   * @param delimiter      The delimiter between array components
   *
   * @return null if the array is empty, the string itself it the array contains only one element, or the combined string with delimiters otherwise
   */
  public static final String join(String[] array, String delimiter) {
    String result = null;
    
    if (delimiter == null){
      delimiter = "";
    }
    
    if (array != null){
      if(array.length == 0){
        return result;
      }
      result = array[0];
      
      for (int i = 1; i < array.length; i++){
        if (array[i] != null){
          result = result + delimiter + array[i];
        }
      }
    }
      
    return result;
  }
  /**
   * Alternate format for the join method that accepts any collection<String> in place of a String[] array.
   *
   * @param delimiter   The delimeter between collection components
   * @param collection  The collection to join
   */ 
  public static final String join(Collection<String> collection, String delimiter) {
    return join(collection.toArray(new String[collection.size()]), delimiter);
  }

  /**
   * Determine whether the string is a single hyphenated word. A hyphenated word
   * is of the form l+[-l+]+, where l is a letter.
   *
   * @param s  The string.
   *
   * @return true if the string has only letters and hyphens.
   */
  public static final boolean hyphenatedWord(String s) {
    boolean result = false;
    final int len = s.length();
    int lasthpos = 0;
    int hpos = s.indexOf('-');
    while ((hpos > 0) && (hpos < (len - 1))) {
      if (isWord(s, lasthpos, hpos)) {
        lasthpos = hpos + 1;
        hpos = s.indexOf('-', lasthpos);
        if (hpos >= 0) {
          result = (hpos < (len - 1));
        }
        else {
          result = isWord(s, lasthpos, len);
        }
      }
      else {
        result = false;
        break;
      }
    }
    return result;
  }

  /**
   * Determine whether the string is a single hyphenated word, where each word
   * is capitalized.
   *
   * A hyphenated word is of the form Ll*[-Ll*]+, where l is a letter.
   *
   * @param s  The string.
   *
   * @return true if the string has only letters and hyphens.
   */
  public static final boolean hyphenatedCapitalizedWord(String s) {
    boolean result = false;
    final int len = s.length();
    int lasthpos = 0;
    int hpos = s.indexOf('-');
    while ((hpos > 0) && (hpos < (len - 1))) {
      if (capitalizedWord(s, lasthpos, hpos)) {
        lasthpos = hpos + 1;
        hpos = s.indexOf('-', lasthpos);
        if (hpos >= 0) {
          result = (hpos < (len - 1));
        }
        else result = capitalizedWord(s, lasthpos, len);
      }
      else {
        result = false;
        break;
      }
    }
    return result;
  }

  // sequence of letters can have whitespace and symbols (no digits), but must have a connecting hyphen between.
  public static final boolean hyphenatedSequence(String s) {
    boolean result = false;
    final String[] pieces = s.split("-");
    if (pieces.length > 1) {
      result = true;
      for (String piece : pieces) {
        if (hasLetter(piece)) {
          result = false;
          break;
        }
      }
    }
    return result;
  }

  public static final boolean hasLetter(String s) {
    boolean result = false;
    for (StringIterator iter = new StringIterator(s); iter.hasNext(); ) {
      final StringPointer pointer = iter.next();
      if (Character.isLetter(pointer.codePoint)) {
        result = true;
        break;
      }
    }
    return result;
  }

  public static final boolean hasDigit(String s) {
    boolean result = false;
    for (StringIterator iter = new StringIterator(s); iter.hasNext(); ) {
      final StringPointer pointer = iter.next();
      if (Character.isDigit(pointer.codePoint)) {
        result = true;
        break;
      }
    }
    return result;
  }

  public static final boolean hasConsecutiveRepeats(String s, int numRepeats) {
    boolean result = false;
    --numRepeats;
    int curNumRepeats = 0;
    int prevCodePoint = 0;
    for (StringIterator iter = new StringIterator(s); iter.hasNext(); ) {
      final StringPointer pointer = iter.next();
      final int curCodePoint = pointer.codePoint;
      if (curCodePoint == prevCodePoint) {
        ++curNumRepeats;
        if (curNumRepeats >= numRepeats) {
          result = true;
          break;
        }
      }
      else curNumRepeats = 0;
      prevCodePoint = curCodePoint;
    }
    return result;
  }

  public static final boolean isCapital(String s, int pos) {
    return Character.isUpperCase(s.codePointAt(pos));
  }

  public static final boolean isCapital(int codePoint) {
 //Character.isLetter(c) && (Character.isUpperCase(c) || Character.isTitleCase(c)))
    return Character.isUpperCase(codePoint);
  }

  public static final boolean camelCasedWord(String s) {
    return camelCasedWord(s, 0, s.length());
  }

  public static final boolean camelCasedWord(String s, int startPos, int endPos) {
    boolean result = false;
    int capPos = capitalPos(s, startPos, endPos);
    boolean foundLowers = false;
    if (capPos >= 0) {
      result = true;
      while (result && capPos >= 0) {
        final int nextCapPos = capitalPos(s, capPos + 1, endPos);
        final int stopAt = (nextCapPos >= 0) ? nextCapPos : endPos;
        if (stopAt > capPos + 1) {
          result = allLowerCase(s, capPos + 1, stopAt) && isWord(s, capPos + 1, stopAt);
          if (!foundLowers) foundLowers = result;
        }
        capPos = nextCapPos;
      }
    }
    return result && foundLowers;
  }

  private static final int capitalPos(String s, int startPos, int endPos) {
    for (int i = startPos; i < endPos; ++i) {
      int c = s.codePointAt(i);
      if (isCapital(c)) return i;
      if (c > 65535) ++i;
    }
    return -1;
  }
  
  /** Strip a string of a substring pattern on both ends just like Python's strip
   * 
   * @param string     The string.
   * @param strip_char The string to strip, if null it will strip whitespace
   * <p>
   * this is taken directly from Jython's PyString methods
   */
  public static final String strip(String string, String strip_char){
    return rstrip(lstrip(string,strip_char),strip_char);
  }

  /** Strip a string of a substring pattern at the left side just like Python's lstrip
   * 
   * @param string      The string.
   * @param strip_char  The string to strip, if null it will strip whitespace
   * <p>
   * this is taken directly from Jython's PyString methods
   */
  public static final String lstrip(String string, String strip_char){
    char[] chars = string.toCharArray();
    int n=chars.length;
    int start=0;
    if (strip_char == null)
      while (start < n && Character.isWhitespace(chars[start]))
  start++;
    else
      while (start < n && strip_char.indexOf(chars[start]) >= 0)
  start++;
    
    return (start > 0) ? string.substring(start, n) : string;
  }

  /** Strip a string of a substring pattern at the right side just like Python's rstrip
   * 
   * @param string      The string.
   * @param strip_char  The string to strip, if null it will strip whitespace
   * <p>
   * this is taken directly from Jython's PyString methods
   */
  public static final String rstrip(String string, String strip_char){
    char[] chars = string.toCharArray();
    int n=chars.length;
    int end=n-1;
    if (strip_char == null)
      while (end >= 0 && Character.isWhitespace(chars[end]))
  end--;
    else
      while (end >= 0 && strip_char.indexOf(chars[end]) >= 0)
  end--;
    
    return (end < n-1) ? string.substring(0, end+1) : string;
  }


  public static int[] toCodePoints(String string) {
    final List<Integer> integers = new ArrayList<Integer>();

    for (StringIterator iter = new StringIterator(string); iter.hasNext(); ) {
      final StringPointer pointer = iter.next();
      integers.add(pointer.codePoint);
    }

    final int[] result = new int[integers.size()];

    int index = 0;
    for (Integer integer : integers) {
      result[index++] = integer;
    }

    return result;
  }

  public static final int longestSubstr(String string1, String string2) {
    return longestSubstr(string1, string2, null);
  }

  /**
   * Find the longest overlap between the two strings.
   *
   * @param string1  The first string.
   * @param string2  The second string.
   * @param result  The characters that overlap.
   *
   * @return Null if there is no overlap or the start positions and length of
   *         the overlap in the strings: {maxLen, string1.start}
   *         or null for no overlap
   */
  public static final int[] findOverlap(String string1, String string2, StringBuilder result) {
    if (string1 == null || string2 == null || "".equals(string1) || "".equals(string2)) {
      return null;
    }

    final int len1 = string1.length();
    final int len2 = string2.length();

    int[][] compareTable = new int[len1][len2];
    int maxLen = 0;
    int lastSubsBegin = 0;
    int thisSubsBegin = 0;
 
    if (result != null) result.setLength(0);

    for (int m = 0; m < len1; m++) {
      for (int n = 0; n < len2; n++) {
        compareTable[m][n] = (string1.charAt(m) != string2.charAt(n)) ? 0
          : (((m == 0) || (n == 0)) ? 1
             : compareTable[m - 1][n - 1] + 1);

        if (compareTable[m][n] > maxLen) {
          maxLen = compareTable[m][n];

          if (result != null) {
            thisSubsBegin = m - compareTable[m][n] + 1;
            if (lastSubsBegin == thisSubsBegin) {
              // the current LCS is the same as the last time this block ran
              result.append(string1.charAt(m));
            }
            else {
              // different LCS resets the string builder 
              lastSubsBegin = thisSubsBegin;
              result.setLength(0);  // clear out the result
              result.append(string1.substring(lastSubsBegin, m + 1));
            }
          }
        }
      }
    }

    return new int[]{maxLen, thisSubsBegin};
  }

  public static final int longestSubstr(String string1, String string2, StringBuilder result) {
    final int[] overlap = findOverlap(string1, string2, result);
    return (overlap == null) ? 0 : overlap[0];
  }

  public static final int longestSubstr(String[] strings) {
    return longestSubstr(strings, null);
  }

  public static final int longestSubstr(String[] strings, StringBuilder result) {
    int len = 0;
    String resultString = null;

    if (strings == null || strings.length == 0) return 0;
    if (strings.length == 1) {
      len = strings[0].length();
      resultString = strings[0];
    }
    else {
      final StringBuilder curResult = (result == null) ? new StringBuilder() : result;
      len = longestSubstr(strings[0], strings[1], curResult);

      for (int i = 2; i < strings.length && len > 0; ++i) {
        len = longestSubstr(curResult.toString(), strings[i], curResult);
      }
    }

    if (result != null && resultString != null) {
      result.setLength(0);
      result.append(resultString);
    }

    return len;
  }

  /**
   * Find the longest substrings between every 2 of the strings.
   */
  public static final Set<String> longestSubstr2(String[] strings) {
    Set<String> result = new HashSet<String>();

    if (strings == null || strings.length == 0) return result;

    if (strings.length == 1) {
      result.add(strings[0]);
      return result;
    }

    final StringBuilder builder = new StringBuilder();

    for (int i = 0; i < strings.length - 1; ++i) {
      final String string1 = strings[i];
      for (int j = i + 1; j < strings.length; ++j) {
        final int curlen = longestSubstr(string1, strings[j], builder);
        if (curlen > 0) {
          final String curResult = builder.toString().trim();
          if (!"".equals(curResult)) {
            final int curLen = curResult.length();

            // only keep the smaller of those that are substrings of others.
            boolean doAdd = true;
            for (Iterator<String> iter = result.iterator(); iter.hasNext(); ) {
              final String aResult = iter.next();
              final int aLen = aResult.length();

              if (aLen <= curLen) {
                if (curResult.indexOf(aResult) >= 0) { // don't add current, a smaller already exists
                  doAdd = false;
                }
              }
              else {
                if (aResult.indexOf(curResult) >= 0) { // need to remove existing and add current
                  iter.remove();
                }
              }
            }
            if (doAdd) result.add(curResult);
          }
        }
      }
    }

    return result;
  }

  /**
   * Empty out all but the smallest unique strings from the array.
   * (Where emptying out sets the reference to null.)
   * <p>
   * That is, if one string is a substring of another, discard the other.
   * <p>
   * NOTE: this DESTRUCTIVELY modifies the input array's references!
   */
  public static final void collapseStrings(String[] strings) {
    if (strings == null || strings.length < 2) return;

    for (int i = 0; i < strings.length - 1; ++i) {
      final String string1 = strings[i];
      if (string1 == null) continue;
      final int len1 = string1.length();

      for (int j = i + 1; j < strings.length; ++j) {
        final String string2 = strings[j];
        if (string2 == null) continue;
        final int len2 = string2.length();

        if ((len1 < len2) && (string2.indexOf(string1) >= 0)) {
          strings[j] = null;
        }
        else if ((len2 < len1) && (string1.indexOf(string2) >= 0)) {
          strings[i] = null;
          break;
        }
      }
    }
  }

  /**
   * Create a map from an array of string arrays as follows:
   * <p>
   * For {{key1, value1}, {key2, value2}, ...}, create a map of the keys
   * to values. Ignore array elements with fewer than 2 entries, map
   * pairs of even elements when there are 2 or more elements and
   * ignore extra (odd) array elements.
   */
  public static final Map<String, String> toMap(String[][] strings) {
    final Map<String, String> result = new HashMap<String, String>();

    for (String[] array : strings) {
      for (int i = 0; i < array.length - 1; i += 2) {
        result.put(array[i], array[i + 1]);
      }
    }

    return result;
  }

  /**
   * Create a map from an array of strings of the form:
   * <p>
   * {key1, value1, key2, value2, ...}
   * <p>
   * NOTE: extra (odd) entries will be ignored.
   */
  public static final Map<String, String> toMap(String[] strings) {
    final Map<String, String> result = new HashMap<String, String>();

    for (int i = 0; i < strings.length - 1; i += 2) {
      result.put(strings[i], strings[i + 1]);
    }

    return result;    
  }

  public static final Set<Character> VOWELS = new HashSet<Character>();
  static {
    VOWELS.add('a');
    VOWELS.add('e');
    VOWELS.add('i');
    VOWELS.add('o');
    VOWELS.add('u');
    VOWELS.add('y');

    VOWELS.add('A');
    VOWELS.add('E');
    VOWELS.add('I');
    VOWELS.add('O');
    VOWELS.add('U');
    VOWELS.add('Y');

    VOWELS.add('ä');
    VOWELS.add('ë');
    VOWELS.add('ï');
    VOWELS.add('ö');
    VOWELS.add('ü');
    VOWELS.add('ÿ');

    VOWELS.add('Ä');
    VOWELS.add('Ë');
    VOWELS.add('Ï');
    VOWELS.add('Ö');
    VOWELS.add('Ü');
    VOWELS.add('ÿ');

    VOWELS.add('á');
    VOWELS.add('é');
    VOWELS.add('í');
    VOWELS.add('ó');
    VOWELS.add('ú');
    VOWELS.add('ý');

    VOWELS.add('Á');
    VOWELS.add('É');
    VOWELS.add('Í');
    VOWELS.add('Ó');
    VOWELS.add('Ú');
    VOWELS.add('Ý');

    VOWELS.add('À');
    VOWELS.add('È');
    VOWELS.add('Ì');
    VOWELS.add('Ò');
    VOWELS.add('Ù');

    VOWELS.add('ã');
    VOWELS.add('õ');

    VOWELS.add('Ã');
    VOWELS.add('Õ');
  }

  public static final Set<Character> SILENTS = new HashSet<Character>();
  static {
    SILENTS.add('h');
  }

  public static final Set<Character> GROUPING_CONSONANTS = new HashSet<Character>();
  static {
    GROUPING_CONSONANTS.add('s');
    GROUPING_CONSONANTS.add('c');
    GROUPING_CONSONANTS.add('l');
    GROUPING_CONSONANTS.add('r');
    GROUPING_CONSONANTS.add('m');
    GROUPING_CONSONANTS.add('n');
  }

  /**
   * Non-abbreviations have at least one vowel overall and at least one vowel
   * between at most 2 consonants, where some adjacent consonants are counted
   * as one.
   */
  public static final boolean isLikelyAbbreviation(String word) {
    return isLikelyAbbreviation(word, VOWELS, SILENTS, GROUPING_CONSONANTS);
  }

  /**
   * Non-abbreviations have at least one vowel overall and at least one vowel
   * between at most 2 consonants, where silent consonants, repeated consonants
   * and those consonants immediately following a grouping consonant are not
   * counted.
   */
  public static final boolean isLikelyAbbreviation(String word, Set<Character> vowels, Set<Character> silents,
                                                   Set<Character> groupingConsonants) {
    boolean result = false;

    boolean sawVowel = false;
    int consecutiveConsonantCount = 0;
    char lastConsonant = 0;

    final int len = word.length();
    for (int idx = 0; idx < len; ++idx) {
      final char curC = word.charAt(idx);

      if (curC == '.') {
        result = true;
        break;
      }
      else if (Character.isLetterOrDigit(curC)) {
        if (vowels.contains(curC)) {
          sawVowel = true;
          consecutiveConsonantCount = 0;
        }
        else {
          if (!silents.contains(curC)) {
            if (curC == lastConsonant) {
              lastConsonant = 0;
            }
            else if (consecutiveConsonantCount == 0 || lastConsonant == 0 || !groupingConsonants.contains(lastConsonant)) {
              lastConsonant = curC;
              ++consecutiveConsonantCount;

              if (consecutiveConsonantCount >= 3) {
                result = true;
                break;
              }
            }
          }
        }
      }
      else {
        lastConsonant = 0;
        consecutiveConsonantCount = 0;
      }
    }

    return !sawVowel || result;
  }

  public static final class StringIterator implements Iterator<StringPointer> {
    private StringPointer nextPointer;

    public StringIterator(String string) {
      this(string, 0, string.length());
    }

    public StringIterator(String string, int startPos, int endPos) {
      this.nextPointer = (endPos - startPos > 0) ? new StringPointer(string, startPos, endPos) : null;
    }

    public final boolean hasNext() {
      return (nextPointer != null);
    }

    public final StringPointer next() {
      final StringPointer result = nextPointer;

      if (nextPointer != null) {
        nextPointer = nextPointer.next();
      }

      return result;
    }

    public final void remove() {
      throw new UnsupportedOperationException("not supported.");
    }

    /**
     * Increment until a space or the end of the string is found. Note that
     * if the end of the string is reached, null will be returned and this
     * iterator will be finished.
     */
    public final StringPointer spinToEndOfWord() {
      StringPointer result = null;
      while (hasNext() && result == null) {
        final StringPointer curResult = next();
        if (curResult.codePoint == ' ') {
          result = curResult;
        }
      }
      return result;
    }

    /**
     * Increment until a letter or digit is returned from next after a word
     * boundary (beginning of text or a space). If no word start (letters or
     * digits) is found, return null. Note that this iterator will be finished
     * if this happens.
     */
    public final StringPointer spinToStartOfWord() {
      // if we're not at the start of the string, skip to the end of the current word.
      if (nextPointer.curPos > 0) {
        final StringPointer endOfWord = spinToEndOfWord();
        if (endOfWord == null) return null;
      }

      StringPointer result = null;
      while (hasNext() && result == null) {
        final StringPointer curResult = next();
        if (Character.isLetterOrDigit(curResult.codePoint)) {
          result = curResult;
        }
      }
      return result;
    }
  }

  public static final class StringPointer {
    public final String string;
    public final int curPos;
    public final int endPos;
    public final int codePoint;
    private final int nextPos;

    public StringPointer(String string, int curPos, int endPos) {
      this.string = string;
      this.curPos = curPos;
      this.endPos = endPos;
      this.codePoint = string.codePointAt(curPos);

      final int inc = (codePoint > 65535) ? 2 : 1;
      this.nextPos = curPos + inc;
    }

    public final StringPointer next() {
      return (nextPos < endPos) ? new StringPointer(string, nextPos, endPos) : null;
    }
  }

  public static final class StringContext implements Iterator<Integer> {
    private int[] prevWindow;
    private int[] postWindow;
    private StringPointer nextPointer;
    private int nextCodePoint;
    private int position;

    private String string;
    private int startPos;
    private int endPos;

    public StringContext(int windowSize, String string) {
      this(windowSize, string, 0, string.length());
    }

    public StringContext(int windowSize, String string, int startPos, int endPos) {
      if (windowSize < 1) windowSize = 1;

      final int range = endPos - startPos;
      if (windowSize > range) windowSize = range;

      this.prevWindow = new int[windowSize];
      this.postWindow = new int[windowSize];
      this.nextPointer = null;
      this.nextCodePoint = 0;
      this.position = startPos;

      this.string = string;
      this.startPos = startPos;
      this.endPos = endPos;

      if (range > 0) {
        this.nextPointer = new StringPointer(string, startPos, endPos);
        for (int index = 0; index < windowSize && nextPointer != null; ++index) {
          postWindow[index] = nextPointer.codePoint;
          nextPointer = nextPointer.next();
        }
      }
    }


    public final Integer next() {
      for (int i = prevWindow.length - 1; i >= 1; --i) prevWindow[i] = prevWindow[i - 1];
      prevWindow[0] = nextCodePoint;
      nextCodePoint = postWindow[0];
      for (int i = 0; i < postWindow.length - 1; ++i) postWindow[i] = postWindow[i + 1];
      if (nextPointer != null) {
        postWindow[postWindow.length - 1] = nextPointer.codePoint;
        nextPointer = nextPointer.next();
      }
      else postWindow[postWindow.length - 1] = 0;
      ++position;

      return nextCodePoint;
    }

    public final boolean hasNext() {
      return (position < endPos);
    }

    public final void remove() {
      throw new UnsupportedOperationException("not supported.");
    }

    public final int getPosition() {
      return position - 1;
    }

    public final int getCodePoint(int offset) {
      final int absOffset = (offset < 0) ? -offset : offset;
      return (absOffset > prevWindow.length) ? getDirectCodePoint(offset) : (offset == 0) ? nextCodePoint : (offset < 0) ? prevWindow[absOffset - 1] : postWindow[offset - 1];
    }

    public final int getPrevCodePoint(int offset) {
      return (offset > prevWindow.length) ? getDirectCodePoint(offset) : (offset == 0) ? nextCodePoint : prevWindow[offset - 1];
    }

    public final int getNextCodePoint(int offset) {
      return (offset > prevWindow.length) ? getDirectCodePoint(offset) : (offset == 0) ? nextCodePoint : postWindow[offset - 1];
    }

    private final int getDirectCodePoint(int offset) {
      int result = 0;
      offset = offset + position - 1;
      if (offset >= startPos && offset < endPos) {
        result = string.codePointAt(offset);
      }
      return result;
    }
  }
  
  public static void main(String[] args) throws IOException {
    if (args != null) {
      for (int i=0; i < args.length; i++) {
        System.out.println(args[i] + "\t-->\t" + getMD5Sum(args[i]));
      }
    }
  }
}
