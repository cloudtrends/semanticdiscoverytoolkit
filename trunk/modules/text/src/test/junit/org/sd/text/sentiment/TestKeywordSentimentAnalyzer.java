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


import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.sd.io.FileUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * JUnit Tests for the KeywordSentimentAnalyzer class.
 * <p>
 * @author Spence Koehler
 */
public class TestKeywordSentimentAnalyzer extends TestCase {

  public TestKeywordSentimentAnalyzer(String name) {
    super(name);
  }
  

  private final void doTest(KeywordSentimentAnalyzer sentimentAnalyzer, TopicSplitter topicSplitter, String textString, SentimentType expectedSentimentType) {
    final Sentiment sentiment = sentimentAnalyzer.getSentiment(textString, topicSplitter);
    assertEquals(expectedSentimentType, sentiment.getType());
  }

  public void test1() throws IOException {
    final KeywordSentimentAnalyzer sentimentAnalyzer = new KeywordSentimentAnalyzer(FileUtil.getFile(this.getClass(), "resources/test-KeywordSentimentAnalyzer-1.txt"));
    final TopicSplitter topicSplitter = new MyTopicSplitter("bad boy");
    
    doTest(sentimentAnalyzer, topicSplitter, "He is a good bad boy.", SentimentType.POSITIVE);
    doTest(sentimentAnalyzer, topicSplitter, "He is not a good bad boy.", SentimentType.NEGATIVE);
    doTest(sentimentAnalyzer, topicSplitter, "He is not a bad bad boy.", SentimentType.POSITIVE);
    doTest(sentimentAnalyzer, topicSplitter, "He is an okay bad boy.", SentimentType.NEUTRAL);
    doTest(sentimentAnalyzer, topicSplitter, "He is not an okay bad boy.", SentimentType.NEGATIVE);
    doTest(sentimentAnalyzer, topicSplitter, "He is a good bad boy, not great but good.", SentimentType.POSITIVE);
    doTest(sentimentAnalyzer, topicSplitter, "The awful truth of the matter is that he is a good bad boy after all.", SentimentType.POSITIVE);
    doTest(sentimentAnalyzer, topicSplitter, "The awful truth of the matter is that the bad boy is good after all.", SentimentType.POSITIVE);
    doTest(sentimentAnalyzer, topicSplitter, "Great, great stuff.", SentimentType.POSITIVE);
    doTest(sentimentAnalyzer, topicSplitter, "That good for nothing bad boy!", SentimentType.NEGATIVE);
    doTest(sentimentAnalyzer, topicSplitter, "That bad boy is good for nothing.", SentimentType.NEGATIVE);

    doTest(sentimentAnalyzer, topicSplitter, "That bad boy gained banker's acceptance.", SentimentType.POSITIVE);
    doTest(sentimentAnalyzer, topicSplitter, "That bad boy did a bang-up job.", SentimentType.POSITIVE);
    doTest(sentimentAnalyzer, topicSplitter, "That bad boy did a bang up job.", null);
  }


  private static final class MyTopicSplitter implements TopicSplitter {
    private String splitTerm;

    public MyTopicSplitter(String splitTerm) {
      this.splitTerm = splitTerm;
    }

    public String[] split(String textString) {
      final List<String> result = new ArrayList<String>();

      for (int charPos = textString.indexOf(splitTerm); charPos >= 0 && charPos < textString.length();
           charPos = textString.indexOf(splitTerm)) {

        result.add(textString.substring(0, charPos).trim());
        result.add(splitTerm);

        textString = textString.substring(charPos + splitTerm.length()).trim();
      }

      if (textString.length() > 0) {
        result.add(textString);
      }

      return result.toArray(new String[result.size()]);
    }
  }


  public static Test suite() {
    TestSuite suite = new TestSuite(TestKeywordSentimentAnalyzer.class);
    return suite;
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}
