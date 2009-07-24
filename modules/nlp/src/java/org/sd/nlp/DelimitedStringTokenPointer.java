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


import org.sd.util.DelimitedString;

/**
 * DelimitedString implementation of the token pointer interface.
 * <p>
 * This implementation is based on using util.DelimitedString for tokenization.
 *
 * @author Spence Koehler
 */
public class DelimitedStringTokenPointer implements TokenPointer {
  
  private DelimitedString.ContinuousSegment segment;
  private Lexicon lexicon;
  private int skipUpTo;
  private Categories categories;

  DelimitedStringTokenPointer(DelimitedString.ContinuousSegment segment, Lexicon lexicon, int skipUpTo) {
    this.segment = segment;
    this.lexicon = lexicon;
    this.skipUpTo = skipUpTo;
    this.categories = narrowToDefinedSegment();
  }

  private final Categories doLookup(DelimitedString.ContinuousSegment segment) {
    final StringWrapper.SubString subString = new StringWrapper(segment.getString()).getSubString(0);
    lexicon.lookup(subString);
    return subString.getCategories();
  }

  private final Categories narrowToDefinedSegment() {
    Categories result = null;
    int skipped = 0;

    do {
      result = doLookup(segment);
      while (segment.length() > 1 && (result == null || result.isEmpty())) {
        segment = segment.shorten();
        result = doLookup(segment);
      }
      if ((result == null || result.isEmpty()) && (skipped < skipUpTo)) {
        segment = segment.next();
        ++skipped;
      }
      else {
        break;
      }
    } while (skipped < skipUpTo);

    return result;
  }

  public DelimitedString.ContinuousSegment getSegment() {
    return segment;
  }

  public final String getString() {
    return segment.getString();
  }

  public final Categories getCategories() {
    return categories;
  }

  public final int getPosition() {
    return segment.getStartWordPosition();
  }

  // strategy is to shorten tokens until only one word is left.
  public final TokenPointer revise() {
    TokenPointer result = null;
    if (segment.length() > 1) {
      DelimitedString.ContinuousSegment nextSegment = segment.shorten();
      if (nextSegment != null) result = new DelimitedStringTokenPointer(nextSegment, lexicon, 0);
    }
    return result;
  }

  // strategy is to get the longest token, then revise by shortening.
  public final TokenPointer next(boolean allowSkip) {
    TokenPointer result = null;
    DelimitedString.ContinuousSegment nextSegment = segment.next();
    int newSkipLimit = allowSkip ? skipUpTo - nextSegment.getStartWordPosition() : 0;
    if (newSkipLimit < 0) newSkipLimit = 0;
    if (nextSegment != null) result = new DelimitedStringTokenPointer(nextSegment, lexicon, newSkipLimit);
    return result;
  }

  /**
   * Get the full unnormalized input up through the referenced token, using the
   * unnormalized input string as reference if necessary.
   */
  public String getInputThrough(String unnormalizedInput) {
    return segment.getInputThrough(unnormalizedInput);
  }

  /**
   * Get the full unnormalized input after the referenced token, using the
   * unnormalized input string as reference if necessary.
   */
  public String getInputBeyond(String unnormalizedInput) {
    return segment.getInputBeyond(unnormalizedInput);
  }

  /**
   * Determine whether this token pointer is guessable.
   * <p>
   * Note that calling this method can have side-effects on the instance.
   */
  public boolean isGuessable() {
    return (categories == null || categories.isEmpty()) && (segment.length() == 1);
  }
}
