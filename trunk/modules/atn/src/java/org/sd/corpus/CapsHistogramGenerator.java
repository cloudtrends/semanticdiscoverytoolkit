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
import org.sd.token.Token;
import org.sd.token.WordCharacteristics;
import org.sd.xml.DataProperties;
import org.sd.xml.DomElement;

/**
 * Histogram generator for capitalized tokens.
 * <p>
 * @author Spence Koehler
 */
public class CapsHistogramGenerator extends CorpusHistogramGenerator {
  
  //
  // Histogram Generator Config:
  //
  // <config reverse='true|false'>
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
  //   reverse
  //

  private boolean reverse;

  public CapsHistogramGenerator(DataProperties options, File histogramGeneratorConfig) throws IOException {
    super(histogramGeneratorConfig);

    final DomElement configElt = getOptions().getDomElement();
    this.reverse = configElt.getAttributeBoolean("reverse", false);

    if (options.hasProperty("reverse")) {  // override reverse
      this.reverse = options.getBoolean("reverse");
    }
  }

  public void setReverse(boolean reverse) {
    this.reverse = reverse;
  }

  public boolean reverse() {
    return reverse;
  }

  protected String[] generateHistogramStrings(Token token) {
    String[] result = null;
    
    final String text = token.getText();
    final WordCharacteristics wc = new WordCharacteristics(text);
    final boolean firstIsUpper = wc.firstIsUpper();

    if ((!reverse && firstIsUpper) || (reverse && !firstIsUpper)) {
      result = new String[]{text};
    }

    return result;
  }


  public static void main(String[] args) throws IOException {
    final CapsHistogramGenerator generator =
      (CapsHistogramGenerator)doMain(args, new InstanceBuilder() {
          public CorpusHistogramGenerator buildInstance(DataProperties options, String[] args, File histogramGeneratorConfig) throws IOException {
            return new CapsHistogramGenerator(options, histogramGeneratorConfig);
          }
        });
  }
}
