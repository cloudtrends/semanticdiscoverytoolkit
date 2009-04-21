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
package org.sd.text.sentiment;


import org.sd.text.MultiTermFinder;
import org.sd.text.TermFinderLogicalResult;
import org.sd.util.logic.LogicalResult;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Sentiment analyzer based on keywords.
 * <p>
 * @author Spence Koehler
 */
public class KeywordSentimentAnalyzer implements SentimentAnalyzer {
  

  /**
   * Sentiment types that correspond to their mtf indexes.
   */
  private static final SentimentType[] SENTIMENT_TYPES = new SentimentType[] {
    SentimentType.POSITIVE,
    SentimentType.NEGATIVE,
    SentimentType.NEUTRAL,
  };


  private MultiTermFinder multiTermFinder;

  /**
   * Load with a multi-term finder (mtf) definition file.
   * <p>
   * Mtf definition file is assumed to be of the form:
   * <code>
   * %(or 0 1 2)
   *
   * #Terms 0: positive terms
   * $caseInsensitive full
   * \@positive-terms-file-name.txt
   *
   * #Terms 1: negative terms
   * $caseInsensitive full
   * \@negative-terms-file-name.txt
   *
   * #Terms 2: neutral terms
   * $caseInsensitive full
   * \@neutral-terms-file-name.txt
   *
   * #Terms 3: negating terms
   * $caseInsensitive full
   * \@negating-terms-file-name.txt
   * </code>
   *
   * NOTE: If a different format for the definition file is used, then a mapping
   *       from terms index number to sentiment type will need to be supplied!
   */
  public KeywordSentimentAnalyzer(File multiDefFile) throws IOException {
    this.multiTermFinder = MultiTermFinder.loadFromFile(multiDefFile);
  }

  /**
   * Get the sentiment expressed in the non-topic strings about the topic
   * strings according to the topicSplitter.
   * <p>
   * If the topicSplitter is null, then compute a general sentiment for
   * the textString as a whole.
   */
  public Sentiment getSentiment(String textString, TopicSplitter topicSplitter) {
    final String[] textStrings = topicSplitter == null ? new String[]{textString} : topicSplitter.split(textString);
    return getSentiment(textStrings);
  }

  /**
   * Get the sentiment expressed in the non-topic (ODD-indexed) strings about
   * the topic (EVEN-indexed) strings.
   */
  public Sentiment getSentiment(String[] textStrings) {
    SentimentType sentimentType = null; /*SentimentType.UNKNOWN;*/

    //
    // basic algorithm:
    //
    // (1) find the closest-to-topic positive, negative, or neutral in non-topic strings
    //     and assign sentiment accordingly.
    // (2) unless unknown, scan back for negating words. if found, flip sentiment type.
    //

    // find the closest match
    TermFinderLogicalResult closestMatch = null;
    int closestMatchStringIndex = -1;
    int closestDistance = -1;

    for (int i = 0; i < textStrings.length; i += 2) {
      String curTextString = textStrings[i];

      // search the current text string for a matching pattern
      for (List<LogicalResult<String>> matches = multiTermFinder.findFirstMatch(curTextString);
           matches != null && matches.size() > 0;
           matches = multiTermFinder.findFirstMatch(curTextString)) {

        final TermFinderLogicalResult match = (TermFinderLogicalResult)matches.get(0);  // gets <idx> assuming structure of "(and <idx> (not ...))"
        final int distance = computeDistance(textStrings, i, curTextString, match);

        // keep the match that is closest to a topic
        if (closestMatch == null || closestDistance < 0 || distance < closestDistance) {
          closestMatch = match;
          closestMatchStringIndex = i;
          closestDistance = distance;
        }

        // shorten the textString and search again to find something closer
        final int[] patternPos = match.getPatternPos();
        final int charPos = patternPos[0] + patternPos[1];
        if (charPos < curTextString.length()) {
          curTextString = curTextString.substring(charPos);
        }
        else break;
      }
    }

    // compute sentimentType of closestMatch
    if (closestMatch != null) {
      final int finderIndex = multiTermFinder.getFinderIndex(closestMatch.getTruthFunction());
      if (finderIndex >= 0 && finderIndex < SENTIMENT_TYPES.length) {
        sentimentType = SENTIMENT_TYPES[finderIndex];
      }

      // scan back for negating words
      // use multiTermFinder.evaluateLogicalExpression("3", textStrings[<up to and including closestMatchStringIndex>])
      for (int i = 0; i <= closestMatchStringIndex; i += 2) {
        final String curTextString = textStrings[i];
        final List<LogicalResult<String>> matches = multiTermFinder.evaluateLogicalExpression("3", curTextString);
        if (matches != null) {
          // found negating word! flip sentiment type!
          sentimentType = sentimentType.flip();
        }
      }
    }

    return new Sentiment(sentimentType);
  }

  private int computeDistance(String[] textStrings, int textStringIndex, String curTextString, TermFinderLogicalResult match) {
    int leftDistance = -1;
    int rightDistance = -1;

    final int[] patternPos = match.getPatternPos();
    final int strlen = curTextString.length();
    
    if (textStringIndex > 0) {
      // compute 'left' distance
      leftDistance = patternPos[0];
    }

    if (textStringIndex < textStrings.length - 1) {
      // compute 'right' distance
      final int charPos = patternPos[0] + patternPos[1];
      rightDistance = strlen - charPos;
    }

    // return the minimum (positive) distance
    int result = leftDistance;
    if (result < 0) {
      result = rightDistance;
    }
    else if (rightDistance >= 0) {
      result = Math.min(leftDistance, rightDistance);
    }

    return result;
  }
}
