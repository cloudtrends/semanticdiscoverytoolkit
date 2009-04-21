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


import java.util.ArrayList;
import java.util.LinkedList;

/**
 * Utility for scanning for expected patterns of characters in strings.
 * <p>
 * @author Spence Koehler
 */
public class StringScanner {
  
  private static final int INDEX  = 0;  // string index
  private static final int VALUE  = 1;  // character value
  private static final int CHAR   = 2;  // character
  private static final int DEPTH  = 3;  // parentheses nesting depth
  private static final int CLOSED = 4;  // num closed parentheses


  private CharFunction charFunction;
  private String string;
  private int maxClosedParens;

  private int len;
  private int[] curInfo;
  private StringBuilder capturedString;
  private LinkedList<int[]> states;
  private LinkedList<String> strings;

  public StringScanner(CharFunction charFunction, String string, int startIndex, int maxClosedParens) {
    
    this.charFunction = charFunction;
    this.string = string;
    this.maxClosedParens = maxClosedParens;

    this.len = string.length();
    this.curInfo = new int[]{startIndex, -1, -1, 0, 0};
    this.capturedString = new StringBuilder();
    this.states = new LinkedList<int[]>();
    this.strings = new LinkedList<String>();

    updateCurInfo();
  }

  public String getString() {
    return string;
  }
  
  /**
   * Set the char function to use going forward.
   */
  public void setCharFunction(CharFunction charFunction) {
    this.charFunction = charFunction;
  }

  /**
   * Set the max closed parens to use going forward.
   */
  public void setMaxClosedParens(int maxClosedParens) {
    this.maxClosedParens = maxClosedParens;
    curInfo[CLOSED] = 0;
  }

  /**
   * Get the current index.
   */
  public int getIndex() {
    return curInfo[INDEX];
  }

  /**
   * Get the value for the character at the current index.
   */
  public int getValue() {
    return curInfo[VALUE];
  }

  /**
   * Get the character at the current index.
   */
  public char getChar() {
    return (char)curInfo[CHAR];
  }

  /**
   * Get the current parentheses nesting depth.
   */
  public int getParenNestingDepth() {
    return curInfo[DEPTH];
  }

  /**
   * Get the total number of closed parentheses.
   */
  public int getNumClosedParentheses() {
    return curInfo[CLOSED];
  }

  /**
   * Determine whether the character at the current index is valid.
   */
  public boolean atValidValue() {
    return curInfo[INDEX] < len && charFunction.isValidChar((char)curInfo[CHAR]);
  }

  /**
   * Determine whether the character at the current index is a break.
   */
  public boolean atBreak() {
    return curInfo[INDEX] == len || charFunction.isBreak((char)curInfo[CHAR]);
  }

  public boolean nextIsValidValue() {
    final int nextIndex = curInfo[INDEX] + 1;
    return nextIndex < len && charFunction.isValidChar(string.charAt(nextIndex));
  }

  /**
   * Get the captured string, clearing if indicated.
   */
  public String getCapturedString(boolean clear) {
    final String result = capturedString.toString();
    if (clear) capturedString.setLength(0);
    return result;
  }

  // captures all values; stops when done capturing or doesn't find any.
  // findAll  if true, find all; otherwise, exit when a value is not found.
  public boolean scanValues(int[] valuesInOrder, boolean mustBeConsecutive, boolean findAll) {
    int lastFoundValueIndex = 0;
    int[] nextInfo = null;

    final int resetLen = capturedString.length();

    for (int i = 0; i < valuesInOrder.length; ++i) {
      nextInfo = nextValidIndex(false);
      if (nextInfo == null) {
        // found invalid or no valid chars.
        capturedString.setLength(resetLen);
        return false;
      }
      else if (nextInfo[VALUE] == valuesInOrder[i]) {
        // found what we're looking for.
        curInfo[VALUE] = nextInfo[VALUE];
        curInfo[CHAR] = nextInfo[CHAR];
        capturedString.append((char)nextInfo[CHAR]);

        final int curIndex = nextInfo[INDEX];
        if (mustBeConsecutive && i > 0 && lastFoundValueIndex != curIndex - 1) {
          // violated constraint.
          capturedString.setLength(resetLen);
          return false;
        }
        lastFoundValueIndex = curIndex;
        curInfo[INDEX] = curIndex + 1;
      }
      else {
        // at a valid phone char before having seen all (or any) values.
        if (findAll || i == 0) {
          capturedString.setLength(resetLen);
          return false;
        }
        i = valuesInOrder.length;  // end loop after setting vars.
        curInfo[INDEX] = nextInfo[INDEX];
      }
      curInfo[DEPTH] = nextInfo[DEPTH];
      curInfo[CLOSED] += nextInfo[CLOSED];
    }
    if (curInfo[CLOSED] > maxClosedParens) {
      capturedString.setLength(resetLen);
      return false;  // too many parenthesis.
    }

    return true;
  }

  /**
   * Scan through ignorable characters, including closing parentheses, but stop
   * at (before consuming) an open parenthesis or valid character.
   *
   * @return true if parens are balanced and under the limit; otherwise, false.
   *         (Note that we may or may not have skipped any chars regardless of
   *         the return value.)
   */
  public boolean scanIgnorables(boolean ensureBalanced) {
    boolean result = false;

    int[] nextInfo = nextValidIndex(true);

    // succeed only if parens are balanced and we've remained under the limit
    result = updateCurInfo(nextInfo, ensureBalanced); 

    return result;
  }

  /**
   * Scan through ignorable characters, including closing parentheses, but stop
   * at (before consuming) an open parenthesis or valid character.
   *
   * @return true if parens are balanced and under the limit; otherwise, false.
   *         (Note that we may or may not have skipped any chars regardless of
   *         the return value.)
   */
  public boolean scanIgnorables() {
    return scanIgnorables(true);
  }

  public boolean isBalanced() {
    return (curInfo[DEPTH] == 0);
  }
  
  /**
   * Scan up to and consume the next valid value, without ensuring balanced parentheses.
   */
  public Integer nextValidValue() {
    Integer result = null;

    if (curInfo[INDEX] >= len) return null;

    int[] nextInfo = nextValidIndex(false);
    if (updateCurInfo(nextInfo, false)) {
      result = curInfo[VALUE];

      // consume
      incrementCurInfo();
    }

    return result;
  }

  /**
   * Get the current value if it is valid according to the current char function
   * and increment to the next position.
   *
   * @return the valid value or null.
   */
  public Integer getValidValue() {
    Integer result = null;

    if (atValidValue()) {
      result = nextValidValue();
    }

    return result;
  }

  public int[] nextValidValues(int numValues, boolean ensureBalanced) {
    int[] result = new int[numValues];

    for (int i = 0; i < numValues; ++i) {
      final Integer nextValue = nextValidValue();
      if (nextValue == null) return null;
      result[i] = nextValue;
    }

    if (curInfo[CLOSED] > maxClosedParens || ensureBalanced && curInfo[DEPTH] != 0) {
      return null;
    }

    return result;
  }

  public int[] nextValidValues() {
    ArrayList<Integer> result = new ArrayList<Integer>();
    
    boolean currentValid = false;
    Integer nextValue = null;
    
    while (nextIsValidValue()) {
      nextValue = nextValidValue();
      if (nextValue == null) break;
      result.add(nextValue);
      currentValid = true;
    }

    if (currentValid && nextValue != null) {
      nextValue = nextValidValue();
      if (nextValue != null) result.add(nextValue);
    }
    
    if (result.size() == 0) return null;

    int[] retVal = new int[result.size()];
    for (int i=0; i < retVal.length; i++) {
      retVal[i] = result.get(i);
    }
    
    return retVal;
  }

  /**
   * Grab 'numValues' consecutive valid values, or null.
   */
  public int[] nextConsecutiveValidValues(int numValues) {
    return nextConsecutiveValidValues(numValues, numValues);
  }

  /**
   * Grab from 'minNumValues' to 'maxNumValues' consecutive valid values, or null.
   */
  public int[] nextConsecutiveValidValues(int minNumValues, int maxNumValues) {
    if (curInfo[INDEX] + minNumValues > len) return null;  // won't fit
    if (maxNumValues < minNumValues || curInfo[INDEX] + maxNumValues > len) maxNumValues = len - curInfo[INDEX];

    int[] result = new int[maxNumValues];

    mark();  // in case we need to revert
    for (int i = 0; i < maxNumValues; ++i) {
      if (!charFunction.isValidChar((char)curInfo[CHAR])) {
        if (i >= minNumValues) {  // found enough to return the result.
          final int[] trimmedResult = new int[i];
          for (int j = 0; j < i; ++j) trimmedResult[j] = result[j];
          result = trimmedResult;
          break;
        }

        // didn't find enough!
        revert();  // revert
        return null;
      }
      result[i] = curInfo[VALUE];
      incrementCurInfo();
    }
    unmark();  // didn't revert -- remove now obsolete mark.
      
    return result;
  }

  /**
   * Mark this instance's current state.
   * <p>
   * A marked state will be restored through the 'revert' method. Multiple
   * marks can be set and the last mark set will be restored by 'revert'.
   */
  public void mark() {
    final int[] storedState = new int[curInfo.length];
    for (int i = 0; i < curInfo.length; ++i) storedState[i] = curInfo[i];
    states.addLast(storedState);
    strings.addLast(capturedString.toString());
  }

  /**
   * Revert to the last stored state.
   * <p>
   * NOTE: an exception will be thrown if there are no states to revert to.
   */
  public void revert() {
    this.curInfo = states.removeLast();
    capturedString.setLength(0);
    capturedString.append(strings.removeLast());
  }

  /**
   * Remove the last mark as if it were never set.
   */
  public void unmark() {
    states.removeLast();
    strings.removeLast();
  }

  /**
   * Increment the pointer to the next character.
   * <p>
   * The current character is 'captured' before incrementing.
   */
  private final void incrementCurInfo() {
    capturedString.append((char)curInfo[CHAR]);
    ++curInfo[INDEX];
    updateCurInfo();
  }

  // set VALUE and CHAR according to INDEX
  private final void updateCurInfo() {
    final int index = curInfo[INDEX];
    if (index < len) {
      final char c = string.charAt(index);
      curInfo[VALUE] = charFunction.getValue(c);
      curInfo[CHAR] = c;
    }
  }

  // update according to nextInfo
  private final boolean updateCurInfo(int[] nextInfo, boolean ensureBalanced) {
    boolean result = false;

    if (nextInfo != null) {
      curInfo[INDEX] = nextInfo[INDEX];
      curInfo[VALUE] = nextInfo[VALUE];
      curInfo[CHAR] = nextInfo[CHAR];
      curInfo[DEPTH] = nextInfo[DEPTH];
      curInfo[CLOSED] += nextInfo[CLOSED];

      if (((ensureBalanced && curInfo[DEPTH] == 0) || !ensureBalanced) &&   // ensure balanced
          (curInfo[CLOSED] <= maxClosedParens)) {                           // enforce max parens
        result = true;
      }
    }

    return result;
  }

  /**
   * Scan for the next digit, starting at index. Fail if encounter a symbol other than
   *  a valid character, an ignorable character, [lparen], or [rparen].
   * <p>
   * If non-null, add each character skipped to 'capture'. The returned digit will NOT be added to capture.
   *
   * @return [nextIndex, charValue, actualChar, parenNestingDepth, numClosedParens]; or null if failed.
   */
  private final int[] nextValidIndex(boolean stopAtOpenParen) {
    int index = curInfo[INDEX];
    int parenNestingDepth = curInfo[DEPTH];

    int charValue = Integer.MIN_VALUE;
    int numClosedParens = 0;
    char c = 0;

    for (; index < len; ++index) {
      c = string.charAt(index);
      if (!charFunction.isValidChar(c)) {
        if (c == '(' || c == '[') {
          if (stopAtOpenParen) {
            charValue = charFunction.getValue(c);
            break;
          }
          ++parenNestingDepth;
        }
        else if (c == ')' || c == ']') {
          --parenNestingDepth;
          ++numClosedParens;
        }
        else if (!charFunction.isIgnorableChar(c)) {
          return null;
        }
      }
      else {
        charValue = charFunction.getValue(c);
        break;
      }

      capturedString.append(c);
    }

    return new int[]{index, charValue, c, parenNestingDepth, numClosedParens};
  }

  public static interface CharFunction {
    public boolean isValidChar(int c);       // determine whether 'c' is a valid char (excluding ignorable chars)
    public boolean isIgnorableChar(int c);   // determine whether 'c' can be ignored
    public boolean isBreak(int c);           // determine whether 'c' is a break char
    public int getValue(int c);              // translate character into an int value (for all valid chars and open paren)
  }

  /**
   * A default char function for recognizing numbers.
   * <p>
   * This implementation is purely 'roman' based and also accepts the common 'Oh' /
   * 'Zero' replacement.
   */
  public static class NumberCharFunction implements StringScanner.CharFunction {

    public NumberCharFunction() {
    }

    /**
     * Accept digits, 'Oh', and 'plus'. Parens will be taken care of in the
     * StringScanner and whitespace and hyphens are handled as ignorable
     * chars.
     */
    public boolean isValidChar(int c) {
      return ((c <= '9' && c >= '0') || c == 'O');
    }

    /**
     * Treat spaces, hyphens, and underscores as ignorable chars.
     */
    public boolean isIgnorableChar(int c) {
      return (c == ' ' || c == '-' || c == '_');
    }

    /**
     * Break on invalid chars that aren't letters.
     */
    public boolean isBreak(int c) {
      return !isValidChar(c) && !Character.isLetter(c);
    }

    /**
     * Map '0' through '9' as 0 through 9, including 'Oh' as zero.
     */
    public int getValue(int c) {
      return (c == 'O') ? 0 : c - '0';
    }
  }
}
