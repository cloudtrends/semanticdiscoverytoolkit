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
package org.sd.nlp;


import org.sd.util.StringUtil;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class to break a string into sentences.
 * <p>
 * @author Spence Koehler
 */
public class SentenceSplitter {

  public SentenceSplitter() {
  }

  /**
   * Split the paragraph into sentence strings.
   */
  public final String[] split(String paragraph) {
    final List<String> result = new ArrayList<String>();

    // build an input wrapper
    final InputWrapper inputWrapper = buildInputWrapper(paragraph);

    // scan for punctuation chars. determine which represent a sentence boundary.
    final int len = inputWrapper.length();
    int startPos = 0;

    for (int i = 0; i < len; ++i) {
      if (inputWrapper.isEndOfSentenceMarker(i)) {
        if (inputWrapper.isSentence(startPos, i)) {
          doAddSentence(result, inputWrapper.getInput(startPos, i + 1));
          startPos = i + 1;
        }
      }
    }

    if (startPos < len) {
      doAddSentence(result, inputWrapper.getInput(startPos, len));
    }

    return result.toArray(new String[result.size()]);
  }

  /**
   * Split the paragraph into sentence info instances.
   */
  public final SplitInfo[] splitInfo(String paragraph) {
    final List<SplitInfo> result = new ArrayList<SplitInfo>();

    // build an input wrapper
    final InputWrapper inputWrapper = buildInputWrapper(paragraph);

    // scan for punctuation chars. determine which represent a sentence boundary.
    final int len = inputWrapper.length();
    int startPos = 0;

    for (int i = 0; i < len; ++i) {
      if (inputWrapper.isEndOfSentenceMarker(i)) {
        if (inputWrapper.isSentence(startPos, i)) {
          doAddSplitInfo(result, inputWrapper, startPos, i + 1);
          startPos = i + 1;
        }
      }
    }

    if (startPos < len) {
      doAddSplitInfo(result, inputWrapper, startPos, len);
    }

    return result.toArray(new SplitInfo[result.size()]);
  }

  private final void doAddSentence(List<String> result, String sentence) {
    final String trimmed = trimSentence(sentence);
    if (trimmed != null && !"".equals(trimmed)) {
      result.add(trimmed);
    }
  }

  private final void doAddSplitInfo(List<SplitInfo> result, InputWrapper inputWrapper, int startIndex, int endIndex) {
    final int trimStart = inputWrapper.getTrimStart(startIndex);
    final int trimEnd = inputWrapper.getTrimEnd(endIndex);
    if (trimEnd > trimStart + 1) {
      final String trimmed = inputWrapper.getInput(trimStart, trimEnd);
      result.add(new SplitInfo(trimStart, trimEnd, trimmed));
    }
  }

  protected InputWrapper buildInputWrapper(String paragraph) {
    return new SdInputWrapper(paragraph);
  }

  protected String trimSentence(String sentence) {
    final InputWrapper sentenceWrapper = buildInputWrapper(sentence);
    int start = sentenceWrapper.getTrimStart(0);
    int end = sentenceWrapper.getTrimEnd(sentence.length());

    return sentence.substring(start, end);
  }


  protected static interface InputWrapper {

    /** Get the wrapped input string. */
    public String getInput();

    /** Get the length of the input. */
    public int length();

    /** Get a substring of the input from startPos (inclusive) to endPos (exclusive). */
    public String getInput(int startPos, int endPos);

    /**
     * Determine whether the character at the given index is a potential end of
     * sentence marker.
     */
    public boolean isEndOfSentenceMarker(int index);

    /**
     * Given that input[endPos] is a potential end of sentence marker,
     * determine whether startPos (inclusive) to endPos (inclusive)
     * represents a sentence.
     */
    public boolean isSentence(int startPos, int endPos);

    /**
     * Get the index of the first non-white character at or after startPos.
     */
    public int getTrimStart(int startPos);

    /**
     * Get the index of the last non-white character before endPos.
     */
    public int getTrimEnd(int endPos);
  }

  /**
   * Container for a sentence and its original text boundaries.
   */
  public static final class SplitInfo {
    /** Start index (inclusive.) */
    public final int startIndex;

    /** End index (exclusive.) */
    public final int endIndex;

    /** Sentence text. */
    public final String sentence;

    public SplitInfo(int startIndex, int endIndex, String sentence) {
      this.startIndex = startIndex;
      this.endIndex = endIndex;
      this.sentence = sentence;
    }
  }

  protected static abstract class BaseInputWrapper implements InputWrapper {

    private String input;

    protected BaseInputWrapper(String input) {
      this.input = input;
    }

    public String getInput() {
      return input;
    }

    public int length() {
      return input.length();
    }

    public String getInput(int startPos, int endPos) {
      return input.substring(startPos, endPos);
    }
  }

  protected static class SdInputWrapper extends BaseInputWrapper {

    private int[] codePoints;

    private int lastSpacePos;
    private int lastLetterPos;
    private int lastCapPos;
    private int lastDigitPos;

    private int lastPrimaryEndPos;
    private int lastSecondaryEndPos;

    protected SdInputWrapper(String input) {
      super(input);

      this.codePoints = StringUtil.toCodePoints(input);

      this.lastSpacePos = -1;
      this.lastLetterPos = -1;
      this.lastCapPos = -1;
      this.lastDigitPos = -1;

      this.lastPrimaryEndPos = -1;
      this.lastSecondaryEndPos = -1;
    }

    public int length() {
      return codePoints.length;
    }

    public String getInput(int startPos, int endPos) {
      final StringBuilder result = new StringBuilder();

      for (int i = startPos; i < endPos; ++i) {
        result.appendCodePoint(codePoints[i]);
      }

      return result.toString();
    }

    public int[] getCodePoints() {
      return codePoints;
    }

    public int getCodePoint(int index) {
      return index < codePoints.length ? codePoints[index] : 0;
    }

    /**
     * Determine whether the character at the given index is a potential end of
     * sentence marker.
     */
    public boolean isEndOfSentenceMarker(int index) {
      boolean result = false;
      final int cp = codePoints[index];

      if (cp == '.' || cp == '!' || cp == '?') {
        result = true;
        lastPrimaryEndPos = index;
      }
      else if (cp == '|' || cp == '"' || cp == '\'' || cp == ')') {
        if (lastPrimaryEndPos == index - 1) {
          result = true;
        }
        else if (lastSecondaryEndPos == index - 1) {
          lastPrimaryEndPos = index - 1;
          result = true;
        }
        lastSecondaryEndPos = index;
      }
      else {
        if (isWhitespace(cp)) {
          lastSpacePos = index;
        }
        else if (Character.isDigit(cp)) {
          lastDigitPos = index;
        }
        else if (Character.isLetter(cp)) {
          this.lastLetterPos = index;

          // set lastCapPos, but don't reset unless we've seen a space since the last time!
          // a capital may also begin a paragraph
          if (Character.isUpperCase(cp) && 
              (lastCapPos < lastSpacePos || index == 0)) {
            this.lastCapPos = index;
          }
        }
      }

      return result;
    }

    /**
     * Given that input[endPos] is a potential end of sentence marker,
     * determine whether startPos (inclusive) to endPos (inclusive)
     * represents a sentence.
     */
    public boolean isSentence(int startPos, int endPos) {
      boolean result = false;

      // check for valid end boundary.
      if (isEndBoundary(endPos + 1)) {
        result = true;

        final int cp = codePoints[endPos];

        final boolean capFollows = findCapitalAhead(endPos);

        // check for
        //    - latin acronym or abbrev like "i.e." or "e.g."
        //    - company name like "Yahoo!"
        //  - that isn't followed by a cap
        if (!capFollows) {
          result = false;
        }

        // here a cap does follow, but we want to not break (after Xx.) when we have "Dr. Foo" or "Ms. Foo, D.D.S."
        else {
          if (lastCapPos > lastSpacePos) {
            // then either the sentence ends with a capitalized word or we have
            // smthg like an abbreviated title preceding a name
            if (endPos - lastCapPos <= 3) {
              // assume 3-letters or less are an abbreviation (for now)
              // note that this fails for certain cases (see testVariousAbbreviations)
              result = false;
            }
          }
        }
      }
      // else, check for other valid end conditions

      return result;
    }

    /**
     * Determine whether the next encountered letter after marker is a capital.
     */
    protected boolean findCapitalAhead(int marker) {
      boolean result = false;

      int lookAhead = marker + 1;
      while (lookAhead < codePoints.length) {
        final int cp = codePoints[lookAhead];
        if (Character.isLetter(cp)) {
          result = Character.isUpperCase(cp);
          break;
        }
        ++lookAhead;
      }

      return result;
    }

    /**
     * Utility method to determine whether there is a space or end of string
     * boundary at pos.
     */
    protected final boolean isEndBoundary(int pos) {
      return (pos >= codePoints.length || codePoints[pos] == '&' || 
              codePoints[pos] == ' ' || isWhitespace(codePoints[pos]));
    }

    /**
     * Get the index of the first non-white/non-Unicode space character at or after startPos.
     */
    public int getTrimStart(int startPos) {
      for (; startPos < codePoints.length; ++startPos) {
        if (!isWhitespace(codePoints[startPos])) break;
      }
      return startPos;
    }

    /**
     * Get the index of the last non-white/non-Unicode character before endPos.
     */
    public int getTrimEnd(int endPos) {
      for (; endPos > 0; --endPos) {
        if (!isWhitespace(codePoints[endPos - 1])) break;
      }
      return endPos;
    }

    /**
     * Determine whether the codepoint is whitespace.
     * <p>
     * Include all unicode and java whitespace chars, including non-breaking
     * spaces.
     */
    private final boolean isWhitespace(int cp) {
      return Character.isSpaceChar(cp) || Character.isWhitespace(cp);
    }
  }


  public void dump(PrintStream out, String[] args) {
    for (int i = 0; i < args.length; ++i) {
      final String arg = args[i];
      final String[] sentences = split(arg);
      
      for (int j = 0; j < sentences.length; ++j) {
        System.out.println(i + "," + j + ": " + sentences[j]);
      }
    }
  }

  public static void main(String[] args) {
    final SentenceSplitter splitter = new SentenceSplitter();
    splitter.dump(System.out, args);
  }
}
