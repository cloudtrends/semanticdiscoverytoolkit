/*
    Copyright 2011 Semantic Discovery, Inc. (www.semanticdiscovery.com)

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


import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

/**
 * Utility to recognize enclosed "blocks" of characters (e.g., text within
 * parentheses or quotes).
 * <p>
 * @author Spence Koehler
 */
public class BlockRecognizer {
  
  /** Default Start/End block chars */
  public static final char[][] DEFAULT_BLOCKS = new char[][] {
    {'"', '"'},
    {'\'', '\''},
    {'(', ')'},
  };


  private Map<Character, Character> blockChars;
  private Stack<Character> startBlockStack;

  public BlockRecognizer() {
    this(DEFAULT_BLOCKS);
  }

  public BlockRecognizer(char[][] blockChars) {
    this.blockChars = new HashMap<Character, Character>();
    for (char[] blockCharPair : blockChars) {
      this.blockChars.put(blockCharPair[0], blockCharPair[1]);
    }
    this.startBlockStack = new Stack<Character>();
  }
    
  /**
   * Get the block chars definition map.
   */
  public Map<Character, Character> getBlockChars() {
    return blockChars;
  }

  /**
   * Get the current start block stack.
   */
  public Stack<Character> getStartBlockStack() {
    return startBlockStack;
  }

  /**
   * Determine whether the startBlockStack is empty.
   */
  public boolean stackIsEmpty() {
    return startBlockStack.size() == 0;
  }

  /**
   * Add the start/end pair identifying a block.
   */
  public void addBlockChars(char blockStart, char blockEnd) {
    this.blockChars.put(blockStart, blockEnd);
  }

  /**
   * Get the block end matching the possible start.
   * <p>
   * @return the block end, or null if the given char is not a block start
   */
  public Character getBlockEnd(char possibleStart) {
    return blockChars.get(possibleStart);
  }

  /**
   * Update the startBlockStack according to the given character, returning
   * true if the character modified the stack. If the stackIsEmpty after
   * a modification, then the character completed all "blocks".
   */
  public boolean updateStack(char curChar) {
    return updateStack(curChar, true, true);
  }

  public boolean updateStack(char curChar, boolean checkStart, boolean checkEnd) {
    boolean result = false;

    // check for hitting an end block character
    if (checkEnd && !startBlockStack.empty()) {
      final char latestStart = startBlockStack.peek();
      final Character latestEnd = blockChars.get(latestStart);
      if (latestEnd != null && latestEnd.equals(curChar)) {
        startBlockStack.pop();
        result = true;
      }
    }

    // check for hitting a start block character
    if (!result && checkStart) {
      if (isPushCandidate(curChar)) {
        startBlockStack.push(curChar);
        result = true;
      }
    }

    return result;
  }

  public void push(char c) {
    startBlockStack.push(c);
  }

  public boolean isPushCandidate(char curChar) {
    boolean result = false;

    // check for hitting a start block character
    final Character blockEnd = blockChars.get(curChar);
    if (blockEnd != null) {
      result = true;
    }

    return result;
  }

  public boolean hasPopCandidate(String text, int pushCandidatePos, char pushCandidate) {
    boolean result = false;

    final Character endChar = blockChars.get(pushCandidate);
    if (endChar != null) {
      final int idx = text.indexOf(endChar, pushCandidatePos + 1);
      if (idx >= 0) {
        result = true;
        if (idx < text.length() - 1) {
          // only valid if a non char/digit follows
          final char nextChar = text.charAt(idx + 1);
//          result = Character.isLetterOrDigit(nextChar);
          // NOTE: this uses isWhitespace instead of isLetterOrDigit because
          //       the expected input comes from word finder output, which is
          //       purely whitespace delimited.
          result = Character.isWhitespace(nextChar);
        }
      }
    }

    return result;
  }
}
