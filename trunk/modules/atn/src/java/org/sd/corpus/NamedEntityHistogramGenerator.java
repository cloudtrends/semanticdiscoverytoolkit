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
package org.sd.corpus;


import java.io.File;
import java.io.IOException;
import org.sd.token.NamedEntitySegmentFinder;
import org.sd.token.StandardTokenizerOptions;
import org.sd.token.SegmentTokenizer;
import org.sd.token.Token;
import org.sd.token.Tokenizer;
import org.sd.xml.DataProperties;

/**
 * Histogram generator using parse output for tokenization.
 * <p>
 * @author Spence Koehler
 */
public class NamedEntityHistogramGenerator extends CorpusHistogramGenerator {
  
  public static final String BEGINNING_OF_STRING = "_B_";
  public static final String END_OF_STRING = "_E_";
  public static final String CONNECTOR_STRING = " | ";
  public static final String NUMBER_STRING = "NUM";


  //
  // Histogram Generator Config:
  //
  // <config>
  //   <fileSelector>
  //     <dirPattern>...</dirPattern>    <!-- regex match pattern; accept all if omitted -->
  //     <filePattern>...</filePattern>  <!-- file match pattern; accept all if omitted -->
  //   </fileSelector>
  //   <tokenizerOptions>
  //     ...
  //   </tokenizerOptions>
  // </config>
  //
  // Properties:
  //   fullEntity -- (default=true) true to include full entity text
  //   squashNumbers -- (default=true) true to substitute "NUM" for any string containing a number
  //   priorWord -- (default=false) true to include prior word to entity
  //   nextWord -- (default=false) true to include next word after entity
  //   firstEntityWord -- (default=false) true to include the entity's first word (replaces/overrides fullEntity)
  //   lastEntityWord -- (default=false) true to include entity's last word (replaces/overrides fullEntity)
  //   minEntityWords -- (default=2) specifies the min number of words necessary to include an entity
  //
  //   prefixOnly -- (default=false) true to setup for just entity prefix unigrams
  //   suffixOnly -- (default=false) true to setup for just entity suffix unigrams
  //   prefixBigrams -- (default=false) true to setup for prefix bigrams
  //   suffixBigrams -- (default=false) true to setup for suffix bigrams
  //

  private DataProperties topOptions;

  private boolean fullEntity;
  private boolean squashNumbers;
  private boolean priorWord;
  private boolean nextWord;
  private boolean firstEntityWord;
  private boolean lastEntityWord;
  private int minEntityWords;

  public NamedEntityHistogramGenerator(DataProperties topOptions, File histogramGeneratorConfig) throws IOException {
    super(histogramGeneratorConfig);
    this.topOptions = topOptions;

    this.fullEntity = topOptions.getBoolean("fullEntity", true);
    this.squashNumbers = topOptions.getBoolean("squashNumbers", true);
    this.priorWord = topOptions.getBoolean("priorWord", false);
    this.nextWord = topOptions.getBoolean("nextWord", false);
    this.firstEntityWord = topOptions.getBoolean("firstEntityWord", false);
    this.lastEntityWord = topOptions.getBoolean("lastEntityWord", false);
    this.minEntityWords = topOptions.getInt("minEntityWords", 2);

    if (topOptions.getBoolean("prefixOnly", false)) {
      this.fullEntity = false;
      this.priorWord = true;
      this.nextWord = false;
      this.firstEntityWord = false;
      this.lastEntityWord = false;
    }

    if (topOptions.getBoolean("suffixOnly", false)) {
      this.fullEntity = false;
      this.priorWord = false;
      this.nextWord = true;
      this.firstEntityWord = false;
      this.lastEntityWord = false;
    }

    if (topOptions.getBoolean("prefixBigrams", false)) {
      this.fullEntity = false;
      this.priorWord = true;
      this.nextWord = false;
      this.firstEntityWord = true;
      this.lastEntityWord = false;
    }

    if (topOptions.getBoolean("suffixBigrams", false)) {
      this.fullEntity = false;
      this.priorWord = false;
      this.nextWord = true;
      this.firstEntityWord = false;
      this.lastEntityWord = true;
    }
  }

  protected Tokenizer buildTokenizer(String line) {

    final DataProperties dataProperties = new DataProperties();
    dataProperties.set("segmentHardBoundaries", NamedEntitySegmentFinder.ENTITY_LABEL);
    dataProperties.set("segmentUnbreakables", NamedEntitySegmentFinder.ENTITY_LABEL);

    final NamedEntitySegmentFinder finder = new NamedEntitySegmentFinder(line);

    StandardTokenizerOptions tokenizerOptions = getTokenizerOptions();
    if (tokenizerOptions == null) {
      tokenizerOptions = NamedEntitySegmentFinder.DEFAULT_TOKENIZER_OPTIONS;
    }

    final Tokenizer result = new SegmentTokenizer(finder, tokenizerOptions, dataProperties);

    return result;
  }

  protected boolean isEntity(Token token) {
    boolean result = false;
    if (token.hasFeatures() && numWords(token.getText()) >= minEntityWords) {
      final Object value = token.getFeatureValue(NamedEntitySegmentFinder.ENTITY_LABEL, null);
      if (value != null) {
        result = true;
      }
    }
    return result;
  }

  protected int numWords(String text) {
    int result = 1;

    for (int spacePos = text.indexOf(' '); spacePos >= 0; spacePos = text.indexOf(' ', spacePos + 1)) {
      ++result;
    }

    return result;
  }

  protected boolean hasDigits(Token token) {
    return hasDigits(token.getText());
  }

  protected boolean hasDigits(String text) {
    boolean result = false;

    final int len = text.length();
    for (int i = 0; i < len; ++i) {
      final char c = text.charAt(i);
      if (c <= '9' && c >= '0') {
        result = true;
        break;
      }
    }

    return result;
  }

  protected String trim(String word) {
    final int len = word.length();

    int startIdx = 0;
    int endIdx = len;

    for (; startIdx < len; ++startIdx) {
      final char c = word.charAt(startIdx);
      if (c == '_' || Character.isLetterOrDigit(c)) break;
    }

    for (; endIdx > 0; --endIdx) {
      final char c = word.charAt(endIdx - 1);
      if (c == '_' || Character.isLetterOrDigit(c)) break;
    }

    return
      ((startIdx > 0 || endIdx < len) && (endIdx > startIdx)) ?
      word.substring(startIdx, endIdx) :
      word;
  }

  protected void addWord(StringBuilder result, String word, boolean isEntity) {
    if (result.length() > 0) result.append(CONNECTOR_STRING);
    if (!isEntity) {
      if (hasDigits(word)) word = NUMBER_STRING;
      else word = trim(word);
    }
    else {
      if (firstEntityWord || lastEntityWord) {
        final String[] words = word.split("\\s");
        word = trim(firstEntityWord ? words[0] : words[words.length - 1]);
      }
    }
    result.append(word);
  }


  protected String[] generateHistogramStrings(Token token) {
    //todo: use super.getOptions to determine mapping functions for token categories
    final String result = doGenerateHistogramStrings(token);
    return (result.length() == 0) ? null : new String[]{result};
  }

  protected String doGenerateHistogramStrings(Token token) {
    final StringBuilder result = new StringBuilder();

    final Token nextToken = (priorWord || nextWord) ? token.getNextToken() : null;
    final boolean nextIsEntity = nextToken != null ? isEntity(nextToken) : false;
    final boolean curIsEntity = isEntity(token);
    
    if (priorWord) {
      if (token.getStartIndex() == 0 && curIsEntity) {
        addWord(result, BEGINNING_OF_STRING, false);
      }

      if (nextIsEntity && !curIsEntity) {
        final String[] words = token.getText().split("\\s");
        addWord(result, words[words.length - 1], false);
      }
    }

    if (fullEntity || firstEntityWord || lastEntityWord) {
      if (priorWord) {
        if (token.getStartIndex() == 0 && curIsEntity) {
          addWord(result, token.getText(), true);
        }
        if (nextIsEntity && !curIsEntity) {
          addWord(result, nextToken.getText(), true);
        }
      }
      else if (curIsEntity) {
        addWord(result, token.getText(), true);
      }
    }

    if (nextWord && curIsEntity) {
      if (nextToken == null) {
        addWord(result, END_OF_STRING, false);
      }
      else if (!nextIsEntity) {
        final String[] words = nextToken.getText().split("\\s");
        addWord(result, words[0], false);
      }
    }

    return result.toString();
  }


  public static void main(String[] args) throws IOException {
    final NamedEntityHistogramGenerator generator =
      (NamedEntityHistogramGenerator)doMain(args, new InstanceBuilder() {
          public CorpusHistogramGenerator buildInstance(DataProperties options, String[] args, File histogramGeneratorConfig) throws IOException {
            return new NamedEntityHistogramGenerator(options, histogramGeneratorConfig);
          }
        });
  }
}
