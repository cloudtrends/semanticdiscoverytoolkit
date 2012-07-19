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
import org.sd.atn.AtnParseBasedTokenizer;
import org.sd.atn.AtnParseRunner;
import org.sd.atn.ParseOutputCollector;
import org.sd.token.StandardTokenizerOptions;
import org.sd.token.Token;
import org.sd.token.Tokenizer;
import org.sd.xml.DataProperties;

/**
 * Histogram generator using parse output for tokenization.
 * <p>
 * @author Spence Koehler
 */
public class ParseHistogramGenerator extends CorpusHistogramGenerator {
  
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
  //   parseConfig
  //   supplementalConfig
  //   resourcesDir
  //

  private DataProperties parseOptions;
  private AtnParseRunner runner;

  public ParseHistogramGenerator(DataProperties parseOptions, File histogramGeneratorConfig) throws IOException {
    super(histogramGeneratorConfig);
    this.parseOptions = parseOptions;
    this.runner = new AtnParseRunner(parseOptions);
  }

  protected Tokenizer buildTokenizer(String line) {
    Tokenizer result = null;

    try {
      final ParseOutputCollector output = runner.parseInputString(line, parseOptions, null);

      if (output.hasParseResults()) {
        final StandardTokenizerOptions tokenizerOptions = getTokenizerOptions();

        if (tokenizerOptions != null) {
          // use histogram generator config's tokenizer options
          result = new AtnParseBasedTokenizer(null, null, output.getParseResults(), output.getInputContext(), tokenizerOptions);
        }
        else {
          // use final parser's tokenizer options
          result = output.getOutputTokenizer();
        }
      }
    }
    catch (IOException e) {
      throw new IllegalStateException(e);
    }

    return result;
  }

  protected String[] generateHistogramStrings(Token token) {
    //todo: use super.getOptions to determine mapping functions for token categories
    String[] result = null;

    if (token.hasFeatures()) {
      final Object value = token.getFeatureValue("personName", null);
      if (value != null) {
        result = new String[]{token.getText()};
      }
    }

    return result;
  }


  public static void main(String[] args) throws IOException {
    final ParseHistogramGenerator generator =
      (ParseHistogramGenerator)doMain(args, new InstanceBuilder() {
          public CorpusHistogramGenerator buildInstance(DataProperties options, String[] args, File histogramGeneratorConfig) throws IOException {
            return new ParseHistogramGenerator(options, histogramGeneratorConfig);
          }
        });
  }
}
