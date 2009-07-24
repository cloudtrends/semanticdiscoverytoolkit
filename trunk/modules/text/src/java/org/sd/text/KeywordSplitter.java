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
package org.sd.text;


import java.util.ArrayList;
import java.util.List;

/**
 * Class to split a string at keywords.
 * <p>
 * @author Spence Koehler
 */
public class KeywordSplitter {

  public enum SplitType {PRE_FIRST_WORD, IS_FIRST_WORD, HAS_LAST_WORD, POST_LAST_WORD};


  // constants for word boundaries
  private static final int END_OF_PRIOR_WORD  = 0;
  private static final int START_OF_WORD      = 1;
  private static final int END_OF_WORD        = 2;
  private static final int START_OF_NEXT_WORD = 3;


  private RobinKarpStringSearch firstWordSearch;
  private RobinKarpStringSearch lastWordSearch;

  /**
   * Construct with the keyword delimiters for splits in the form of
   * words that begin a split and words that end a split.
   * 
   * @param firstWords  words that begin a split, ok if null.
   * @param lastWords   words that end a split, ok if null.
   */
  public KeywordSplitter(String[] firstWords, String[] lastWords) {
    this.firstWordSearch = (firstWords == null) ? null : new RobinKarpStringSearch(17, normalize(firstWords));
    this.lastWordSearch = (lastWords == null) ? null : new RobinKarpStringSearch(17, normalize(lastWords));
  }

  private final String[] normalize(String[] words) {
    for (int i = 0; i < words.length; ++i) {
      words[i] = words[i].toLowerCase();
    }
    return words;
  }

  /**
   * Split the string by finding a the last 'last' word in an adjacent sequence,
   * then backing up to a first word from the first 'last' word in the sequence
   * to find string boundaries.
   */
  public Split[] splitOnLast(String string) {
    return doSplitOnLast(string, false);
  }

  /**
   * Split the string on all 'last' words in an adjacent sequence.
   */
  public Split[] splitOnlyLast(String string) {
    return doSplitOnLast(string, true);
  }

  private final Split[] doSplitOnLast(String string, boolean onlyLast) {
    final List<Split> result = new ArrayList<Split>();

    String nstring = string.toLowerCase();
    final int startPos = skipDelims(string, 0);

    if (startPos < string.length()) {
      string = string.substring(startPos);
      nstring = nstring.substring(startPos);
      int[] lastWordBoundaries = findLastLastWordBoundaries(nstring);

      while (lastWordBoundaries != null) {
        // found last word, find closest first word before it
        lookBackForFirstWord(string, nstring, result, lastWordBoundaries, SplitType.HAS_LAST_WORD, onlyLast);

        string = string.substring(lastWordBoundaries[START_OF_NEXT_WORD]);
        nstring = nstring.substring(lastWordBoundaries[START_OF_NEXT_WORD]);
        lastWordBoundaries = (nstring.length() > 0) ? findLastLastWordBoundaries(nstring) : null;
      }

      // add remainder
      if (string.length() > 0) {
        // find closest first word to end
        final int len = string.length();
        lookBackForFirstWord(string, nstring, result, new int[]{len, len, len, len}, SplitType.POST_LAST_WORD, onlyLast);
      }
    }

    return result.toArray(new Split[result.size()]);
  }

  private final void lookBackForFirstWord(String string, String nstring, List<Split> result, int[] lastWordBoundaries, SplitType splitType, boolean onlyLast) {
    if (onlyLast) {
      if (lastWordBoundaries[END_OF_PRIOR_WORD] > 0) {
        result.add(new Split(SplitType.PRE_FIRST_WORD, string.substring(0, lastWordBoundaries[END_OF_PRIOR_WORD])));
      }
      
      if (lastWordBoundaries[END_OF_WORD] > lastWordBoundaries[START_OF_WORD]) {
        result.add(new Split(splitType, string.substring(lastWordBoundaries[START_OF_WORD], lastWordBoundaries[END_OF_WORD])));
      }
    }
    else {
      final int[] firstWordBoundaries = findLastFirstWordBoundaries(nstring, lastWordBoundaries[END_OF_PRIOR_WORD]);

      if (firstWordBoundaries != null) {
        if (firstWordBoundaries[END_OF_PRIOR_WORD] > 0) {
          final String priorString = string.substring(0, firstWordBoundaries[END_OF_PRIOR_WORD]);
          result.add(new Split(SplitType.PRE_FIRST_WORD, priorString));
        }

        final String firstWord = string.substring(firstWordBoundaries[START_OF_WORD], firstWordBoundaries[END_OF_WORD]);
        result.add(new Split(SplitType.IS_FIRST_WORD, firstWord));

        if (firstWordBoundaries[START_OF_NEXT_WORD] < string.length()) {
          final String curResult = string.substring(firstWordBoundaries[START_OF_NEXT_WORD], lastWordBoundaries[END_OF_WORD]);
          result.add(new Split(splitType, curResult));
        }
      }
      else {
        result.add(new Split(splitType, string.substring(0, lastWordBoundaries[END_OF_WORD])));
      }
    }
  }

  public int[] findFirstWord(String string) {
    return (firstWordSearch == null) ? null : firstWordSearch.search(string, PatternFinder.FULL_WORD);
  }

  public int[] findFirstWord(String string, int startPos, int endPos) {
    int[] result = null;

    if (firstWordSearch != null && startPos < endPos) {
      result = firstWordSearch.search(string, startPos, endPos, PatternFinder.FULL_WORD);
    }

    return result;
  }

  public int[] findLastWord(String string) {
    return (lastWordSearch == null) ? null : lastWordSearch.search(string, PatternFinder.FULL_WORD);
  }

  public int[] findLastWord(String string, int startPos) {
    int[] result = null;

    if (lastWordSearch != null && startPos < string.length()) {
      result = lastWordSearch.search(string, startPos, string.length(), PatternFinder.FULL_WORD);
    }

    return result;
  }

  public static final String[] getStrings(Split[] splits, SplitType splitType) {
    final List<String> result = new ArrayList<String>();

    for (Split split : splits) {
      if (splitType == null || (splitType == split.type)) {
        result.add(split.string);
      }
    }

    return result.toArray(new String[result.size()]);
  }

  /**
   * Find the boundaries of the first 'last word' in the string.
   *
   * @return null or the 3 positions: last word start, last word end, next word start.
   */
  public int[] findLastWordBoundaries(String string) {
    int[] result = null;

    final int[] lastWordIndex = findLastWord(string);
    if (lastWordIndex != null) {
      result = findWordBoundaries(string, lastWordIndex[0]);
    }

    return result;
  }

  // {endOfPriorWord, startOfWord, endOfWord, startOfNextWord}
  private final int[] findWordBoundaries(String string, int wordStartPos) {
    final int[] result = new int[4];

    int endOfPriorWord = wordStartPos;
    int endOfWord = string.indexOf(' ', wordStartPos);
    int startOfNextWord = string.length();

    result[START_OF_WORD] = wordStartPos;

    if (endOfWord >= 0) {
      result[END_OF_WORD] = skipDelimsBackward(string, endOfWord);
      result[START_OF_NEXT_WORD] = skipDelims(string, endOfWord);
    }
    else {
      result[END_OF_WORD] = skipDelimsBackward(string, string.length());
      result[START_OF_NEXT_WORD] = string.length();
    }

    if (wordStartPos > 0) {
      result[END_OF_PRIOR_WORD] = skipDelimsBackward(string, wordStartPos);
    }

    return result;
  }

  public static final int skipDelims(String string, int startPos) {
    int result = startPos;
    final int len = string.length();

    for (; result < len; ++result) {
      final char c = string.charAt(result);
      if (Character.isLetterOrDigit(c)) {
        break;
      }
    }

    return result;
  }

  public static final int skipDelimsBackward(String string, int startPos) {
    int result = startPos;

    for (; result > 0; --result) {
      final char c = string.charAt(result - 1);
      if (Character.isLetterOrDigit(c)) {
        break;
      }
    }

    return result;
  }

  // similar to finLastWordBoundaries, but stretches last word when
  // consecutive matches are found.
  private final int[] findLastLastWordBoundaries(String string) {
    int[] result = null;

    int[] lastWordIndex = findLastWord(string, 0);

    while (lastWordIndex != null) {
      if (result == null) {
        // this is the first last word.
        result = findWordBoundaries(string, lastWordIndex[0]);
      }
      else {
        if (lastWordIndex[0] == result[START_OF_NEXT_WORD]) {  // next last word is adjacent to last last word
          // stretch result
          final int[] boundaries = findWordBoundaries(string, lastWordIndex[0]);
          result[END_OF_WORD] = boundaries[END_OF_WORD];
          result[START_OF_NEXT_WORD] = boundaries[START_OF_NEXT_WORD];
        }
        else {
          // not adjacent. time to quit.
          break;
        }
      }

      lastWordIndex = findLastWord(string, result[START_OF_NEXT_WORD]);
    }

    return result;
  }


  // find the last first word start position before the endPos
  // return {endOfWordBeforeFirstWord+1, beginingOfFirstWord}
  private final int[] findLastFirstWordBoundaries(String string, int endPos) {
    int[] result = null;

    int[] firstWordIndex = findFirstWord(string, 0, endPos);

    while (firstWordIndex != null) {
      result = findWordBoundaries(string, firstWordIndex[0]);
      firstWordIndex = findFirstWord(string, result[START_OF_NEXT_WORD], endPos);
    }

    return result;
  }

  /**
   * Container for split text, identifying its origin.
   */
  public static final class Split {
    public final SplitType type;
    public final String string;

    Split(SplitType type, String string) {
      this.type = type;
      this.string = string;
    }

    public String toString() {
      final StringBuilder result = new StringBuilder();

      result.append(type.name()).append('(').append(string).append(')');

      return result.toString();
    }
  }


  public static final void main(String[] args) {
    //arg0: firstWords (comma separated)
    //arg1: lastWords  (comma separated)
    //args2+: strings to split on last

    final String firstWords = args[0];
    final String lastWords = args[1];

    final KeywordSplitter splitter = new KeywordSplitter(firstWords.split("\\s*,\\s*"),
                                                         lastWords.split("\\s*,\\s*"));

    for (int i = 2; i < args.length; ++i) {
      final String string = args[i];
      System.out.println("'" + string + "'");

      final Split[] splits = splitter.splitOnLast(string);
      for (int j = 0; j < splits.length; ++j) {
        System.out.println("  --> '" + splits[j] + "'");
      }
    }
  }
}
