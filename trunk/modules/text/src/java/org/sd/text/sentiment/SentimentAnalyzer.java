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


/**
 * Interface for analyzing sentiment.
 * <p>
 * @author Spence Koehler
 */
public interface SentimentAnalyzer {

  /**
   * Get the sentiment expressed in the non-topic strings about the topic
   * strings according to the topicSplitter.
   * <p>
   * If the topicSplitter is null, then compute a general sentiment for
   * the textString as a whole.
   */
  public Sentiment getSentiment(String textString, TopicSplitter topicSplitter);

  /**
   * Get the sentiment expressed in the non-topic (ODD-indexed) strings about
   * the topic (EVEN-indexed) strings.
   */
  public Sentiment getSentiment(String[] textStrings);

}
