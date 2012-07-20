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
 * Histogram generator for tokens immediately preceding a named entity.
 * <p>
 * @author Abe Sanderson
 */
public class NamedEntityPrefixBigramHistogramGenerator 
  extends CorpusHistogramGenerator 
{
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
  //

  private DataProperties topOptions;

  public NamedEntityPrefixBigramHistogramGenerator(DataProperties topOptions, 
                                             File histogramGeneratorConfig) 
    throws IOException 
  {
    super(histogramGeneratorConfig);
    this.topOptions = topOptions;
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

  private boolean isEntity(Token token)
  {
    boolean result = false;
    if (token.hasFeatures()) {
      final Object value = token.getFeatureValue(NamedEntitySegmentFinder.ENTITY_LABEL, null);
      if (value != null)
        result = true;
    }
    return result;
  }
  protected String[] generateHistogramStrings(Token token) {
    //todo: use super.getOptions to determine mapping functions for token categories
    String[] result = null;
    Token next = token.getNextToken();
    if(!isEntity(token) && next != null && isEntity(next))
    {
      String[] splits = next.getText().split(" ");
      String prefixWord = token.getText();
      String firstEntityWord = splits[0];
      
      StringBuilder builder = new StringBuilder();
      builder.append(prefixWord);
      builder.append(" START_ENTITY ");
      builder.append(firstEntityWord);
      result = new String[]{builder.toString()};
    }
    return result;
  }

  public static void main(String[] args) throws IOException {
    final NamedEntityPrefixBigramHistogramGenerator generator =
      (NamedEntityPrefixBigramHistogramGenerator)doMain(args, new InstanceBuilder() {
          public CorpusHistogramGenerator buildInstance(DataProperties options, String[] args, File histogramGeneratorConfig) throws IOException {
            return new NamedEntityPrefixBigramHistogramGenerator(options, histogramGeneratorConfig);
          }
        });
  }
}
