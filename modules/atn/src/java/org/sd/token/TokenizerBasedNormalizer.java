/*
    Copyright 2013 Semantic Discovery, Inc. (www.semanticdiscovery.com)

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
package org.sd.token;


import org.sd.nlp.Normalizer;
import org.sd.nlp.NormalizedString;

/**
 * An nlp.Normalizer implementation based on this package's tokenization.
 * <p>
 * @author Spence Koehler
 */
public class TokenizerBasedNormalizer implements Normalizer {
  
  private StandardTokenizerOptions tokenizerOptions;
  private boolean lowerCaseFlag;

  public TokenizerBasedNormalizer(StandardTokenizerOptions tokenizerOptions, boolean lowerCaseFlag) {
    this.tokenizerOptions = tokenizerOptions;
    this.lowerCaseFlag = lowerCaseFlag;
  }

  public StandardTokenizerOptions getTokenizerOptions() {
    return tokenizerOptions;
  }

  public boolean getLowerCaseFlag() {
    return lowerCaseFlag;
  }

  public NormalizedString normalize(String string) {
    return new TokenizerNormalizedString(this, new StandardTokenizer(string, tokenizerOptions), lowerCaseFlag);
  }
}
