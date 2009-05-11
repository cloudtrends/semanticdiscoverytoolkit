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
		final String sentence = inputWrapper.getInput(startIndex, endIndex);
		final String trimmed = trimSentence(sentence);
		if (trimmed != null && !"".equals(trimmed)) {
			result.add(new SplitInfo(startIndex, endIndex, trimmed));
		}
	}

  protected InputWrapper buildInputWrapper(String paragraph) {
    return new SdInputWrapper(paragraph);
  }

  protected String trimSentence(String sentence) {
    return sentence.trim();
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

  }

	/**
	 * Container for a sentence and its original text boundaries.
	 */
	public static final class SplitInfo {
		/** Start index (inclusive.) */
		public final int startIndex;

		/** End index (exlusive.) */
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

    protected SdInputWrapper(String input) {
      super(input);

      this.codePoints = StringUtil.toCodePoints(input);

      this.lastSpacePos = -1;
      this.lastLetterPos = -1;
      this.lastCapPos = -1;
      this.lastDigitPos = -1;
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

      if (cp == '.' || cp == '!' || cp == '?' || cp == '|') {
        result = true;
      }
      else {
        if (cp == ' ') {
          lastSpacePos = index;
        }
        else if (Character.isDigit(cp)) {
          lastDigitPos = index;
        }
        else if (Character.isLetter(cp)) {
          this.lastLetterPos = index;

          if (Character.isUpperCase(cp)) {
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

        // check for acronym or abbrev
        // NOTE: added '!' to catch company names as non-sentence enders (like "Yahoo!")
        if (cp == '.' || cp == '!') {
          // '.' immediately follows a letter and last cap is after last space
          if (lastLetterPos == endPos - 1 && lastCapPos > lastSpacePos) {
            // NOTE: this will err when an acronmym or abbrev really is at the end of a sentence.
            //       it is assumed that cases like this will be relatively rare; if not, add more
            //       rules to cover.
            result = false;
          }
        }
      }
      // else, check for other valid end conditions

      return result;
    }

    /**
     * Utility method to determine whether there is a space or end of string
     * boundary at pos.
     */
    protected final boolean isEndBoundary(int pos) {
      return (pos >= codePoints.length || codePoints[pos] == ' ');
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
