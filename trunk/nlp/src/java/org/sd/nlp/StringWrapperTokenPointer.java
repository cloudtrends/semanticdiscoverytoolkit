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


import java.io.PrintStream;

/**
 * StringWrapper implementation of the token pointer interface.
 * <p>
 * This implementation is based on using StringWrapper.SubString for tokenization.
 *
 * @author Spence Koehler
 */
public class StringWrapperTokenPointer implements TokenPointer {
  
  private static PrintStream DEBUG = null;

  private Lexicon lexicon;
  private StringWrapper.SubString subString;
  private int skipUpTo;
  private boolean dontShrinkDefined;

  private boolean narrowed;
  private boolean didRevising;
  private StringWrapper.SubString _revised;

  StringWrapperTokenPointer(Lexicon lexicon, StringWrapper.SubString subString, int skipUpTo) {
    this(lexicon, subString, skipUpTo, false);
  }

  StringWrapperTokenPointer(Lexicon lexicon, StringWrapper.SubString subString, int skipUpTo, boolean dontShrinkDefined) {
    this.lexicon = lexicon;
    this.subString = subString;
    this.skipUpTo = skipUpTo;
    this.dontShrinkDefined = dontShrinkDefined;
    this.narrowed = false;
    this.didRevising = false;
    this._revised = null;
  }

  protected final int getSkipUpTo() {
    return skipUpTo;
  }

  public static final void setDebug(PrintStream debug) {
    DEBUG = debug;
  }

  public static final PrintStream getDebug() {
    return DEBUG;
  }

  protected Lexicon getLexicon() {
    return lexicon;
  }

  public StringWrapper.SubString getSubString() {
    if (!narrowed) narrowToDefined();
    return subString;
  }

  /**
   * Get the referenced token's string.
   */
  public final String getString() {
    final StringWrapper.SubString subString = getSubString();
    return subString.originalSubString;
  }

  /**
   * Get the referenced token's categories.
   */
  public final Categories getCategories() {
    final StringWrapper.SubString subString = getSubString();
    return subString.getCategories();
  }

  /**
   * Get the (start word) position of this token within its string.
   */
  public int getPosition() {
    return subString.getStartWordPosition();
  }

  /**
   * Get another token pointer based on shortening or lengthening the
   * referenced token's size. This revision strategy must be correlated
   * with the "next" strategy.
   */
  public final TokenPointer revise() {
    TokenPointer result = null;

    final StringWrapper.SubString subString = getSubString();
    final StringWrapper.SubString shorter = getRevised();

    if (shorter != null) {
      result = buildRevisedTokenPointer(shorter);
    }
    return result;
  }

  /**
   * Get the next token pointer that immediately follows this one.
   * The strategy for building the next token (longest, shortest, etc.)
   * must be correlated with the "revise" strategy.
   *
   * @param allowSkip  if true, allow skipping text within limits established
   *                   on construction; otherwise, don't skip.
   */
  public final TokenPointer next(boolean allowSkip) {
    final StringWrapper.SubString subString = getSubString();
    TokenPointer result = null;
    
    final StringWrapper.SubString next = doGetNext(subString);
    if (next != null) {
      final int newSkipLimit = getNextSkipLimit(subString, allowSkip);
      result = buildNextTokenPointer(next, newSkipLimit);
    }
    return result;
  }

  /**
   * Get the full unnormalized input up through the referenced token, using the
   * unnormalized input string as reference if necessary.
   */
  public String getInputThrough(String unnormalizedInput) {
    String result = null;
    final StringWrapper.SubString input = subString.stringWrapper.getSubString(0, subString.endPos);
    if (input != null) result = input.originalSubString;
    return result;
  }

  /**
   * Get the full unnormalized input after the referenced token, using the
   * unnormalized input string as reference if necessary.
   */
  public String getInputBeyond(String unnormalizedInput) {
    String result = null;
    final StringWrapper.SubString input = subString.stringWrapper.getSubString(subString.endPos);
    if (input != null) result = input.originalSubString;
    return result;
  }

  /**
   * Determine whether this token pointer is guessable.
   * <p>
   * Note that calling this method can have side-effects on the instance.
   */
  public boolean isGuessable() {
    boolean result = false;

    if (dontShrinkDefined && subString.getCategories() != null) return result;

    // need to have tried all tokens, the smallest can't have had any def's
    StringWrapper.SubString revised = getRevised();

    if (revised == null) {
      // we've tried all tokens. get the smallest.
      revised = subString.stringWrapper.getShortestSubString(subString.startPos);
      if (revised.getCategories() == null) {
        result = true;

        // need to change the pointer to be the smallest if true.
        this.subString = revised;
      }
    }

    return result;
  }

  protected Categories doLookup(StringWrapper.SubString subString) {
    lexicon.lookup(subString);
    if (DEBUG != null /*&& subString.getCategories() != null*/) DEBUG.println(this.toString() + "> " + subString + ": def=" + subString.getCategories());
    return subString.getCategories();
  }

  // override this for different strategies
  protected TokenPointer buildRevisedTokenPointer(StringWrapper.SubString subString) {
    return new StringWrapperTokenPointer(lexicon, subString, 0, dontShrinkDefined);
  }

  // override this for different strategies
  protected TokenPointer buildNextTokenPointer(StringWrapper.SubString subString, int newSkipLimit) {
    return new StringWrapperTokenPointer(lexicon, subString, newSkipLimit, dontShrinkDefined);
  }

  private final int getNextSkipLimit(StringWrapper.SubString subString, boolean allowSkip) {
    final int newSkipLimit = allowSkip ? skipUpTo - subString.getStartWordPosition() : 0;
    return (newSkipLimit < 0) ? 0 : newSkipLimit;
  }

  // override this for different strategies
  protected StringWrapper.SubString doRevising(StringWrapper.SubString subString, int maxNumWords) {
    return (dontShrinkDefined && subString.getCategories() != null) ? null : subString.getShorterSubString();
  }

  protected StringWrapper.SubString doGetNext(StringWrapper.SubString subString) {
    return subString.getNextLongestSubString(lexicon.maxNumWords());
  }

  // override this for different strategies
  protected StringWrapper.SubString doNarrowing(StringWrapper.SubString subString, int maxNumWords) {
    StringWrapper.SubString smallest = subString;
    while (subString != null) {
      final Categories categories = doLookup(subString);
      if (categories != null) break;

      subString = doRevising(subString, maxNumWords);
      if (subString != null) smallest = subString;
    }

    return (subString == null) ? (subString == smallest ? null : smallest) : subString;
  }

  private final void narrowToDefined() {
    narrowed = true;
    int skipped = 0;
    StringWrapper.SubString narrowed = null;

    while (subString != null) {
      narrowed = doNarrowing(subString, lexicon.maxNumWords());

      if (skipped >= skipUpTo) break;
      if (narrowed != null && narrowed.getCategories() != null) break;

      subString = doGetNext(narrowed);
      if (DEBUG != null) DEBUG.println("StringWrapperTokenPointer> " + "skipping over '" + narrowed + "' to '" + subString + "'.");
      ++skipped;
    }

    this.subString = narrowed;
  }

  private final StringWrapper.SubString getRevised() {
    if (!didRevising) _revised = doRevising(subString, lexicon.maxNumWords());
    return _revised;
  }
}
