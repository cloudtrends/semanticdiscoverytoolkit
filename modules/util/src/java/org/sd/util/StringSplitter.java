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


import org.sd.io.FileUtil;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Utilities for splitting strings various ways.
 * <p>
 * These methods are meant as helpers for segmenting strings in an I18N-safe way.
 * 
 * @author Spence Koehler
 */
public class StringSplitter {
  
  /**
   * Trim whitespace off the ends and remove extra non-quoted internal whitespace.
   */
  public static String hypertrim(String data) {
    final StringBuilder result = new StringBuilder();

    boolean sawWhite = false;
    Character quote = (char)0;
    Character lastQuote = (char)0;

    for (int charIndex = 0; charIndex < data.length(); ++charIndex) {
      final char c = data.charAt(charIndex);

      lastQuote = quote;
      boolean isQuoted = (quote != 0 || lastQuote == '\\');
      if (!isQuoted) {
        if (c == '\\' || c == '"' || c == '\'') {
          quote = c;
          isQuoted = true;
        }
      }
      else {  // isQuoted
        if (c == quote) {
          // end quote when we see its match
          isQuoted = false;
          quote = 0;
        }
        else if (lastQuote == '\\') {
          // still quoted this round, but not next
          quote = 0;
        }
      }

      final boolean isWhite = !isQuoted && Character.isWhitespace(c);
      
      if (isWhite) {
        sawWhite = true;
      }
      else {
        if (sawWhite && result.length() > 0) result.append(' ');
        result.append(c);
        sawWhite = false;
      }
    }

    return result.toString();
  }

  /**
   * Split the data on whitespace.
   */
  public static String[] splitOnWhitespace(String data) {
    return splitOnDelim(data,
                        new DelimIdentifier() {
                          public boolean isDelim(char c) {
                            //todo: merge with/use StringUtil.isWhite
                            return Character.isWhitespace(c) || Character.isSpaceChar(c);
                          }
                        });
  }

  /**
   * Split the data on whitespace and non-alpha-numeric symbols.
   */
  public static String[] splitAlphaNumerics(String data) {
    return splitOnDelim(data,
                        new DelimIdentifier() {
                          public boolean isDelim(char c) {
                            return !Character.isLetterOrDigit(c);
                          }
                        });
  }

  /**
   * Split the data on delims according to the given delimiter identifier.
   */
  public static String[] splitOnDelim(String data, DelimIdentifier delimId) {
    if (data == null) {
      return null;
    }

    List<String> result = new ArrayList<String>();

    char[] chars = data.toCharArray();
    StringBuilder curBuffer = new StringBuilder();

    for (char c : chars) {
      if (delimId.isDelim(c)) {
        if (curBuffer.length() > 0) {
          result.add(curBuffer.toString());
          curBuffer = new StringBuilder();
        }
      }
      else {
        curBuffer.append(c);
      }
    }

    if (curBuffer.length() > 0) {
      result.add(curBuffer.toString());
    }

    return result.toArray(new String[result.size()]);
  }

  /**
   * Remove delims (chars) from the data according to the given delimiter identifier.
   */
  public static String removeDelims(String data, DelimIdentifier delimId) {
    if (data == null) {
      return null;
    }

    char[] chars = data.toCharArray();
    StringBuilder result = new StringBuilder();

    for (char c : chars) {
      if (!delimId.isDelim(c)) {
        result.append(c);
      }
    }

    return result.toString();
  }

  /**
   * Map from char[i][0] to char[i][1]
   */
  public static Map<Character, Character> buildMap(char[][] chars) {
    final Map<Character, Character> result = new HashMap<Character, Character>();
    for (char[] cr : chars) {
      result.put(cr[0], cr[1]);
    }
    return result;
  }

  /**
   * Map from String[i][0].charAt(0) to String[i][1]
   */
  public static Map<Character, String> buildMap(String[][] char2String) {
    final Map<Character, String> result = new HashMap<Character, String>();
    for (String[] c2s : char2String) {
      result.put(c2s[0].charAt(0), c2s[1]);
    }
    return result;
  }

  /**
   * Remove delims (chars) from the data according to the given delimiter identifier.
   *
   * @param data  the string to replace chars in.
   * @param charmap  when see charmap.key, replace with charmap.value
   */
  public static <T> String replaceChars(String data, Map<Character, T> charmap) {
    if (data == null) {
      return null;
    }

    char[] chars = data.toCharArray();
    StringBuilder result = new StringBuilder();

    for (char c : chars) {
      final T replacement = charmap.get(c);
      if (replacement != null) {
        result.append(replacement);
      }
      else {
        result.append(c);
      }
    }

    return result.toString();
  }

  public static Map<Character, Character> readC2C(String resourceName) {
    final Map<Character, Character> result = new HashMap<Character, Character>();

    try {
      final BufferedReader reader = FileUtil.getReader(StringSplitter.class, "resources/" + resourceName);
      String line = null;
      while ((line = reader.readLine()) != null) {
        final String[] pieces = line.split(",");
        if (pieces.length == 2) {
          result.put((char)Integer.parseInt(pieces[0]), (char)Integer.parseInt(pieces[1]));
        }
      }
    }
    catch (IOException e) {
      throw new IllegalStateException("couldn't load resource '" + resourceName + "'!", e);
    }

    return result;
  }

  public static Map<Character, String> readC2S(String resourceName) {
    return readC2S(StringSplitter.class, resourceName);
  }

  public static Map<Character, String> readC2S(Class clazz, String resourceName) {
    final Map<Character, String> result = new HashMap<Character, String>();

    try {
      final BufferedReader reader = FileUtil.getReader(clazz, "resources/" + resourceName);
      String line = null;
      while ((line = reader.readLine()) != null) {
        final String[] pieces = line.split(",");
        if (pieces.length == 2) {
          final int c = parseInt(pieces[0]);
          result.put((char)c, pieces[1]);
        }
      }
    }
    catch (IOException e) {
      throw new IllegalStateException("couldn't load resource '" + resourceName + "'!", e);
    }

    return result;
  }

  /**
   * Utility to parse an integer in either hexadecimal ("0x...") or decimal format.
   */
  public static final int parseInt(String string) {
    int result = 0;

    final int strlen = string.length();
    if (strlen > 0) {
      if (strlen > 1 && string.charAt(0) == '0' && string.charAt(1) == 'x') {
        result = Integer.parseInt(string.substring(2), 16);
      }
      else {
        result = Integer.parseInt(string);
      }
    }

    return result;
  }

  public static final Map<Character, Character> DIACRITICS_SQUASH_MAP = readC2C("diacritics-squash-map.csv");

  public static String squashDiacritics(String data) {
    return replaceChars(data, DIACRITICS_SQUASH_MAP);
  }

  //todo: verify/fix multi character mappings.
  public static final Map<Character, String> DIACRITICS_REPLACE_MAP = readC2S("diacritics-replace-map.csv");

  public static String replaceDiacritics(String data) {
    return replaceChars(data, DIACRITICS_REPLACE_MAP);
  }

  public static final String getDiacriticReplacement(int c) {
    return DIACRITICS_REPLACE_MAP.get(new Character((char)c));
  }

  public static boolean isDiacritic(int c) {
    return isDiacritic(new Character((char)c));
  }

  public static boolean isDiacritic(Character c) {
    return DIACRITICS_REPLACE_MAP.containsKey(c);
  }

  public static int appendReplacementDiacritic(int codePoint, StringBuilder result) {
    return appendReplacementDiacritic(codePoint, result, DIACRITICS_REPLACE_MAP);
  }

  public static int appendReplacementDiacritic(int codePoint, StringBuilder result, Map<Character, String> diacriticsReplaceMap) {
    int rv = 0;

    final char[] chars = Character.toChars(codePoint);
    for (char c : chars) {
      final String replacement = diacriticsReplaceMap.get(c);
      if (replacement == null) {
        result.append(c);
        rv = 1;
      }
      else {
        result.append(replacement);
        rv = replacement.length();
      }
    }

    return rv;
  }

  /**
   * Expand (i.e. add delims between) the data according to the following rules.
   *
   * Where isLetter(x) == true and isDigit(d) == true,
   *       isUpper(u) == true, isLower(l) == true,
   *
   *   add a space after periods (unless at end): x.x -> x. x
   *   set numbers off with spaces: xd -> x d; dx -> d x
   *   expand camel case: U+Ul -> U+ Ul; lU -> l U
   */
  public static String expand(String data, String delim) {
    if (data == null) {
      return null;
    }

    char[] chars = data.toCharArray();
    StringBuilder result = new StringBuilder();
    int index = 0;
    final int len1 = chars.length - 1;

    boolean isUpper = false;
    boolean isLower = false;
    boolean isLetter = false;
    boolean isDigit = false;

    boolean wasUpper = false;
    boolean wasLower = false;
    boolean wasLetter = false;
    boolean wasDigit = false;

    boolean wasWasUpper = false;

    for (char c : chars) {
      isLetter = Character.isLetter(c);
      isUpper = Character.isUpperCase(c);
      isLower = Character.isLowerCase(c);
      isDigit = Character.isDigit(c);

      if ((wasUpper && isLower && wasWasUpper)) {
        result.insert(index - 1, delim);
      }
      else if ((isDigit && wasLetter) ||
               (wasDigit && isLetter) ||
               (isUpper && wasLower)) {
        result.append(delim);
      }

      result.append(c);

      if (c == '.' && index < len1 && wasLetter) {
        result.append(delim);
      }

      ++index;

      wasWasUpper = wasUpper;
      wasUpper = isUpper;
      wasLower = isLower;
      wasLetter = isLetter;
      wasDigit = isDigit;
    }

    return result.toString();
  }

  /**
   * Split the data string into multiple strings based on the given
   * delimiter.
   */
  public static String[] splitOnChar(String data, char delim) {
    if (data == null) {
      return null;
    }

    List<String> result = new ArrayList<String>();

    char[] chars = data.toCharArray();
    StringBuffer curBuffer = new StringBuffer();

    for (char c : chars) {
      if (delim == c) {
        result.add(curBuffer.toString());
        curBuffer = new StringBuffer();
      }
      else {
        curBuffer.append(c);
      }
    }

    result.add(curBuffer.toString());
    
    return result.toArray(new String[result.size()]);
  }

  /**
   * Split on the first equals found in the data.
   * <p>
   * If there is no equals to split on, return null.
   * <p>
   * For example:  a=b will be split as "a", and "b".
   * "a=b=c" will be split as "a", "b=c".
   * <p>
   * @param data  The data to split.
   *
   * @return null if the pattern is not found or an array of
   *         length 2 containing the data to the left of the equals
   *         in result[0] and the data to the right of the equals
   *         in result[1].
   */
  public static String[] splitOnFirstEquals(String data) {
    return splitOnFirstDelim(data, '=');
  }

  /**
   * Split the string on the first space, returning the data before the space
   * in result[0] and the data after the space in result[1]. If there is no
   * space in data (and the data is non-null and non-empty), return data in
   * result[0].
   */
  public static String[] splitOnFirstSpace(String data) {
    String[] result = splitOnFirstDelim(data, ' ');
    if (result == null && data != null && data.length() > 0) {
      result = new String[] {data};
    }
    return result;
  }

  /**
   * Split on the first equals found in the data.
   * <p>
   * If there is no equals to split on, return null.
   * <p>
   * For example:  a=b will be split as "a", and "b".
   * "a=b=c" will be split as "a", "b=c".
   * <p>
   * @param data  The data to split.
   * @param delim The delimiter to split on (i.e. '=').
   *
   * @return null if the pattern is not found or an array of
   *         length 2 containing the data to the left of the equals
   *         in result[0] and the data to the right of the equals
   *         in result[1].
   */
  public static String[] splitOnFirstDelim(String data, char delim) {
    String[] result = null;

    if (data != null) {
      int equalPos = data.indexOf(delim);
      if (equalPos >= 0) {
        result = new String[] {data.substring(0, equalPos), data.substring(equalPos+1, data.length())};
      }
    }

    return result;
  }

  /**
   * Split on unescaped semicolons, where an escaped semicolon is one that does
   * not appear in a construct "&...;".
   * <p>
   * For example, splitting "a;b;c" will yield {"a", "b", "c"}.
   * Splitting "a&tag;b;c" will yield {"a&tag;b", "c"}.
   * <p>
   * @param data  The string to split.
   *
   * @return a List of Strings that includes the data between unescaped semicolons.
   */
  public static List<String> splitOnSemis(String data) {
    if (data == null) {
      return null;
    }

    List<String> result = new ArrayList<String>();

    char[] chars = data.toCharArray();
    StringBuffer curBuffer = new StringBuffer();
    int balance = 0;
    for (char c : chars) {
      if ('&' == c) {
        ++balance;
      }
      else if (';' == c) {
        --balance;

        if (balance < 0) {
          balance = 0;
          result.add(curBuffer.toString());
          curBuffer = new StringBuffer();
          continue;
        }
      }
      curBuffer.append(c);
    }

    result.add(curBuffer.toString());
    return result;
  }

  /**
   * Given a string of the form: "a(b)c", where b and/or c may include
   * delimiters, but a does not include a left delimiter, return a
   * native array of Strings with the three strings a, b, and c.
   * <p>
   * If there is not a set of balanced parentheses, null will be returned;
   * otherwise, a, b, and c will be in the array (even if a, b, or c are
   * empty) at indeces 0, 1, and 2, respectively.
   * <p>
   * @param data       The data to split.
   *
   * @return null if the pattern doesn not exist, or a String array of length
   *         exactly 3, where result[0] has data before the widest balanced
   *         delims, result[1] has data within the widest balanced delims
   *         (not including the delims themselves), and result[2] has data
   *         after the widest balances delims.
   */
  public static String[] splitOnParens(String data) {
    return splitOnDelims(data, '(', ')');
  }
  
  /**
   * Given a string of the form: "a[b]c", where b and/or c may include
   * delimiters, but a does not include a left delimiter, return a
   * native array of Strings with the three strings a, b, and c.
   * <p>
   * If there is not a set of balanced parentheses, null will be returned;
   * otherwise, a, b, and c will be in the array (even if a, b, or c are
   * empty) at indeces 0, 1, and 2, respectively.
   * <p>
   * @param data       The data to split.
   *
   * @return null if the pattern doesn not exist, or a String array of length
   *         exactly 3, where result[0] has data before the widest balanced
   *         delims, result[1] has data within the widest balanced delims
   *         (not including the delims themselves), and result[2] has data
   *         after the widest balances delims.
   */
  public static String[] splitOnSquareBrackets(String data) {
    return splitOnDelims(data, '[', ']');
  }

  /**
   * Given a string of the form: "a(b)c", where b and/or c may include
   * delimiters, but a does not include a left delimiter, return a
   * native array of Strings with the three strings a, b, and c.
   * <p>
   * The delim char for '(' can be specified in the "leftDelim" parameter.
   * The delim char for ')' can be specified in the "rightDelim" parameter.
   * <p>
   * If there is not a set of balanced parentheses, null will be returned;
   * otherwise, a, b, and c will be in the array (even if a, b, or c are
   * empty) at indeces 0, 1, and 2, respectively.
   * <p>
   * @param data       The data to split.
   * @param leftDelim  The delimiter character to use for the left.
   * @param rightDelim The delimiter character to use for the right.
   *
   * @return null if the pattern doesn not exist, or a String array of length
   *         exactly 3, where result[0] has data before the widest balanced
   *         delims, result[1] has data within the widest balanced delims
   *         (not including the delims themselves), and result[2] has data
   *         after the widest balances delims.
   */
  public static String[] splitOnDelims(String data, char leftDelim, char rightDelim) {
    if (data == null) {
      return null;
    }

    String[] result = new String[3];
    int index = 0;

    char[] chars = data.toCharArray();
    StringBuffer curBuffer = new StringBuffer();
    int numDelims = 0;
    boolean hitDelim = false;
    boolean hitEnd = false;
    for (int i = 0; i < chars.length; ++i) {
      char c = chars[i];
      if (leftDelim == c && !hitEnd) {
        ++numDelims;

        if (!hitDelim) {
          // this is the first time we saw the left delim. Pack away "a".
          result[index++] = curBuffer.toString();
          curBuffer = new StringBuffer();
          hitDelim = true;
        }
        else {
          curBuffer.append(c);
        }
      }
      else if (rightDelim == c && hitDelim && !hitEnd) {
        --numDelims;

        if (numDelims == 0) {
          // we found the matching end delim. Pack away "b".
          result[index++] = curBuffer.toString();
          curBuffer = new StringBuffer();
          hitEnd = true;
        }
        else {
          curBuffer.append(c);
        }
      }
      else {
        curBuffer.append(c);
      }
    }

    // Pack away "c".
    if (index == 2) {
      result[index++] = curBuffer.toString();
    }

    return index == 3 ? result : null;
  }

  public static interface DelimIdentifier {
    public boolean isDelim(char c);
  }
  
  public static void main(String[] args) throws IOException {
    // args[0] = input text file
    // args[1] = output text file (with diacritics expanded/squashed)
    BufferedReader reader = FileUtil.getReader(args[0]);
    BufferedWriter writer = FileUtil.getWriter(args[1]);
    
    String line;
    while ((line = reader.readLine()) != null) {
      writer.write(line);
      writer.newLine();
      
      String squashed = squashDiacritics(line);
      if (!line.equals(squashed)) {
        writer.write(squashed);
        writer.newLine();
      }
      
      String expanded = replaceDiacritics(line);
      if (!line.equals(expanded)) {
        writer.write(expanded);
        writer.newLine();
      }
    }
    
    reader.close();
    writer.close();
  }
}
