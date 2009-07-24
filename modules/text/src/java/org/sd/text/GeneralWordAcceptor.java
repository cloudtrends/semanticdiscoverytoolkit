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
package org.sd.text;


import org.sd.text.WordGramSplitter.AcceptCode;
import org.sd.text.WordGramSplitter.WordAcceptor;
import org.sd.util.StringUtil;

import java.util.Set;
import java.util.HashSet;

/**
 * A general word acceptor to be used by a word gram splitter.
 * <p>
 * @author Spence Koehler
 */
public class GeneralWordAcceptor implements WordAcceptor {

  public static final int DEFAULT_MIN_TOKEN_LENGTH = 3;
  public static final boolean DEFAULT_DIGIT_ACCEPTANCE = false;


  private Set<String> stopwords;
  private int minTokenLength;
  private boolean acceptTokensWithDigits;

  public GeneralWordAcceptor() {
    this((Set<String>)null);
  }

  public GeneralWordAcceptor(Set<String> stopwords) {
    this(stopwords, DEFAULT_MIN_TOKEN_LENGTH, DEFAULT_DIGIT_ACCEPTANCE);
  }

  public GeneralWordAcceptor(Set<String> stopwords, int minTokenLength, boolean acceptTokensWithDigits) {
    this.stopwords = stopwords;
    init(minTokenLength, acceptTokensWithDigits);
  }

  public GeneralWordAcceptor(String[] stopwords) {
    this(stopwords, DEFAULT_MIN_TOKEN_LENGTH, DEFAULT_DIGIT_ACCEPTANCE);
  }

  public GeneralWordAcceptor(String[] stopwords, int minTokenLength, boolean acceptTokensWithDigits) {
    this.stopwords = new HashSet<String>();
    for (String stopword : stopwords) {
      this.stopwords.add(stopword);
    }
    init(minTokenLength, acceptTokensWithDigits);
  }

  private final void init(int minTokenLength, boolean acceptTokensWithDigits) {
    this.minTokenLength = minTokenLength;
    this.acceptTokensWithDigits = acceptTokensWithDigits;
  }


  /**
   * Accept a token if it
   * <ul>
   * <li>is N chars or longer</li>
   * <li>doesn't contain any digits (optionally)</li>
   * <li>is not a stopword</li>
   * </ul>
   */
  public final AcceptCode accept(String token) {
    if ("".equals(token) || (token == null)) return AcceptCode.REJECT_IGNORE;

    AcceptCode result = AcceptCode.ACCEPT;

    if (StringUtil.hasDigit(token) && !acceptTokensWithDigits) {
      result = AcceptCode.REJECT_SPLIT;
    }
    if (result == AcceptCode.ACCEPT && minTokenLength > 0 && token.length() < minTokenLength) {
      result = AcceptCode.REJECT_IGNORE;
    }
    if (result == AcceptCode.ACCEPT && stopwords != null && stopwords.contains(token)) {
      result = AcceptCode.REJECT_SPLIT;
    }

    return result;
  }
  
}
